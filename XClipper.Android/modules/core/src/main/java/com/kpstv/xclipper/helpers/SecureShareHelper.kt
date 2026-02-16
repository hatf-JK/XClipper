package com.kpstv.xclipper.helpers

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kpstv.xclipper.R
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecureShareHelper {
    private const val ENCRYPTION_PREFIX = "XClipper:ENC:"
    private const val ITERATIONS = 10000
    private const val KEY_SIZE = 256
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16

    fun encrypt(text: String, passphrase: String): String? {
        try {
            val salt = ByteArray(SALT_SIZE)
            SecureRandom().nextBytes(salt)

            val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_SIZE + (IV_SIZE * 8))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val bytes = factory.generateSecret(spec).encoded
            
            val key = bytes.copyOfRange(0, 32)
            val iv = bytes.copyOfRange(32, 48)

            val secretKey = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            
            val encrypted = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
            
            val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)

            return "${ENCRYPTION_PREFIX}1:$saltB64:$ivB64:$cipherB64"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun decrypt(encryptedText: String, passphrase: String): String? {
        try {
            if (!encryptedText.startsWith(ENCRYPTION_PREFIX)) return null
            
            val parts = encryptedText.split(":")
            if (parts.size < 6) return null
            // parts[0] = XClipper, parts[1] = ENC, parts[2] = 1, parts[3] = Salt, parts[4] = IV, parts[5] = Cipher

            val salt = Base64.decode(parts[3], Base64.NO_WRAP)
            val iv = Base64.decode(parts[4], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[5], Base64.NO_WRAP)

            val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_SIZE + (IV_SIZE * 8))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val bytes = factory.generateSecret(spec).encoded

            val key = bytes.copyOfRange(0, 32)
            // val iv = bytes.copyOfRange(32, 48) // We use the IV from the payload, but we need to derive bytes to match logic? 
            // Wait, standard PBKDF2 derives Key. IV should be random.
            // In Windows implementation:
            // "byte[] iv = deriveBytes.GetBytes(IV_SIZE);"
            // "using (var deriveBytes = new Rfc2898DeriveBytes(passphrase, salt, ITERATIONS))"
            // Rfc2898DeriveBytes generates pseudo-random bytes based on password and salt.
            // If I generate IV from deriveBytes in Windows, I must do same here.
            // In Windows I did:
            // byte[] key = deriveBytes.GetBytes(KEY_SIZE);
            // byte[] iv = deriveBytes.GetBytes(IV_SIZE);
            // So IV AND Key are derived from Password+Salt.
            // My implementation in SecureShareHelper.encrypt above:
            // SecureRandom().nextBytes(salt) -> PBEKeySpec -> generateSecret -> bytes.
            // The bytes are Key + IV (32 + 16 = 48 bytes).
            // So my IV IS derived from password+salt.
            // Wait, standard practice is Random IV. 
            // In Windows `AdHocHelper`:
            // "rng.GetBytes(salt);" -> "deriveBytes.GetBytes(KEY)" -> "deriveBytes.GetBytes(IV)".
            // Yes, Windows derives IV from Password+Salt.
            // So Android must do the same.
            // `val bytes = factory.generateSecret(spec).encoded`
            // If I ask for 256 bit key, I get 32 bytes.
            // How do I get 48 bytes from `generateSecret`?
            // `PBEKeySpec` keyLength is in bits.
            // `KEY_SIZE` (256) + `IV_SIZE` (128) = 384 bits.
            // In `encrypt` above: `KEY_SIZE + (IV_SIZE * 8)` -> 256 + 128 = 384.
            // So `bytes` will be 48 bytes.
            // `key` = 0..31.
            // `iv` = 32..47.
            // So `ivSpec` uses this derived IV.
            // BUT, `salt` is random.
            // So `iv` is effectively random-ish (dependent on salt).
            // Wait, in `encrypt` I did `val ivSpec = IvParameterSpec(iv)`.
            // And then I encoded `iv` to B64 and put it in payload: `val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)`.
            // In `decrypt`, I decode `iv` from payload: `val iv = Base64.decode(parts[4], Base64.NO_WRAP)`.
            // AND I also derive `bytes` from password+salt.
            // I should use the DERIVED IV if it matches Windows logic?
            // Windows AdHocHelper.Decrypt:
            // "byte[] iv = Convert.FromBase64String(parts[4]);"
            // "using (var deriveBytes...)" -> "byte[] key = deriveBytes.GetBytes(KEY_SIZE);"
            // "aes.IV = iv;"
            // WAIT. In Windows, I put IV in payload: `Convert.ToBase64String(iv)`.
            // But `iv` came from `deriveBytes.GetBytes(IV_SIZE)`.
            // So the IV in payload IS the derived IV.
            // So I can use EITHER the payload IV OR re-derive it.
            // If I use payload IV, I don't need to derive it.
            // However, `PBEKeySpec` generates a single block of bytes.
            // Java `SecretKeyFactory` might not support arbitrary length generation like C# `Rfc2898DeriveBytes` stream.
            // `Rfc2898DeriveBytes` is a PRNG.
            // `PBKDF2WithHmacSHA1` might just hash it.
            // If I want compatibility, I must match exactly.
            // C# `Rfc2898DeriveBytes` uses PBKDF2.
            // If I use `PBEKeySpec(chars, salt, iter, 384)`, will it match C# `GetBytes(32); GetBytes(16)`?
            // Usually yes.
            // So:
            // 1. Derive 48 bytes.
            // 2. Key = first 32.
            // 3. IV = next 16.
            // 4. Use these.
            // 5. Ignore IV from payload? Or verify it?
            // The Windows code puts `iv` (derived) into payload.
            // And Windows Decrypt `byte[] iv = Convert.FromBase64String(parts[4])`. 
            // And DOES NOT call `deriveBytes.GetBytes(IV_SIZE)` in Decrypt!
            // "byte[] key = deriveBytes.GetBytes(KEY_SIZE);"
            // It stops there.
            // So Windows Decrypt uses Payload IV.
            // So Android Decrypt should use Payload IV.
            // But Android Decrypt `key` must be same.
            // Can I derive ONLY 32 bytes in Java?
            // Yes, `PBEKeySpec(..., 256)`.
            // But will `PBEKeySpec(..., 256)` produce same bytes as first 32 of `PBEKeySpec(..., 384)`?
            // Yes, PBKDF2 stream is deterministic. First N bytes are same.
            // So:
            // Decrypt Logic:
            // 1. Decode Salt from payload.
            // 2. Derive 32 bytes Key using Password + Salt.
            // 3. Decode IV from Payload.
            // 4. Decrypt using Key + IV.
            
            val decryptSpec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, 256)
            val decryptBytes = factory.generateSecret(decryptSpec).encoded
            val decryptKey = SecretKeySpec(decryptBytes, "AES")
            
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.DECRYPT_MODE, decryptKey, ivSpec)
            
            val decryptedBytes = cipher.doFinal(cipherBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun showPasswordDialog(context: Context, title: String, onPositive: (String) -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_text, null)
        val editText = view.findViewById<EditText>(R.id.et_text)
        
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) onPositive(text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
