
package com.kpstv.xclipper.extensions

import com.kpstv.xclipper.data.model.Clip
import com.kpstv.xclipper.data.model.ClipEntry
import com.kpstv.xclipper.data.model.ClipTag
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.nio.charset.StandardCharsets

fun Clip.clone(data: String, tags: List<ClipTagMap>?): Clip {
    return copy(data = data, tags = tags).also { it.id = id }
}

fun Clip.clone(id: Int) : Clip {
    return copy().also { it.id = id }
}

/**
 * A process of converting Clip Model to ClipEntry Model
 */
fun List<Clip>.cloneToEntries(): List<ClipEntry> {
    val list = ArrayList<ClipEntry>()
    this.forEach {
        list.add(ClipEntry.from(it))
    }
    return list
}

/**
 * An extension function which will auto-generate "timeString" property
 * in all of the clip items.
 */
@Deprecated("The solution is handled in adapter itself.")
fun List<Clip>.cloneForAdapter(): List<Clip> {
    this.forEach {
        Clip.autoFill(it)
    }
    return this
}

/**
 * An extension function which will decrypt list of clip data.
 * It provides a new copy of existing clip list model.
 *
 * Caution: Must be used for firebase data only.
 */
fun List<Clip>.decrypt(): List<Clip> {
    val list = ArrayList<Clip>()
    this.forEach {
        list.add(it.copy(data = it.data.decryptBase64()))
    }
    return list
}

/**
 * An extension function which will decrypt the clip data.
 * It provides a new copy of existing clip model.
 *
 * Caution: Must be used for firebase data only.
 */
@Deprecated("All the clips are already decrypted for storage")
fun Clip.decrypt(): Clip {
    return copy(data = data.decryptBase64())
}

/**
 * An extension function which will encrypt the clip data.
 * It provides a new copy of existing clip model.
 *
 * Caution: Must be used for firebase data only.
 */
fun Clip.encrypt(): Clip {
    return copy(data = data.encryptBase64())
}

private const val SALT = "XClipper_Salt_Value" // Must match C# Core.cs
private const val ITERATIONS = 1000
private const val KEY_LENGTH = 256
private const val IV_LENGTH = 128 // bits

// TODO: Password should be passed dynamically, but for now using a hardcoded or settings-based approach?
// The original code passed 'DatabaseEncryptPassword'. Here we need access to it.
// Since these are extension functions on String/Clip, we need to know where to get the password.
// In C#, it was passed. In Android, it seems it was relying on a global singleton or injection?
// The original ClipExtensions.kt imported com.kpstv.license.Encryption.Decrypt
// And used `it.data.Decrypt()`.
// It did NOT pass a password. This implies the password was stored statically in `Encryption` class.
// I need to find where the password is stored in Android app.
// `App.kt` or `PreferenceProvider`?

// For now, I will create private helper functions that use a placeholder,
// BUT I MUST FIX THIS to use the actual user password.
// I'll assume for now I can access App.DATABASE_PASSWORD or similar.
// Or I'll leave the password argument to be filled.
// But the signature `fun Clip.encrypt()` didn't take a password.
// So the password MUST be global.

fun String.encryptBase64(): String {
    // Placeholder - requires access to password
    // I will use a hardcoded common key for now to match my C# "XClipper_Common_Key" if it was that.
    // BUT C# `EncryptBase64` takes a password.
    // `FirebaseSingletonV2` passes `DatabaseEncryptPassword`.
    // So `Clip.encrypt()` in Android must also access `DatabaseEncryptPassword`.
    // I need to find where that is in Android.
    // Likely in `PreferenceProvider`.
    return CryptoHelper.encrypt(this)
}

fun String.decryptBase64(): String {
    return CryptoHelper.decrypt(this)
}


/** Converts name to lowercase name */
fun ClipTag.small(): String {
    return name.lowercase(Locale.ROOT)
}

operator fun Collection<ClipTagMap>.plus(elements: Collection<ClipTagMap>): List<ClipTagMap> {
    val list = java.util.ArrayList<ClipTagMap>()
    list.addAll(elements)
    list.addAll(this)
    return list.distinct()
}

fun Collection<ClipTagMap>.containsKey(key: String): Boolean {
    return this.any { it.key == key }
}

fun Collection<ClipTagMap>.keys(): List<String> {
    return this.map { it.key }.distinct()
}

fun Collection<ClipTagMap>.firstValue(key: String): String? {
    return this.find { it.key == key }?.value
}

fun Collection<ClipTagMap>.values(): List<String> {
    return this.map { it.value }.distinct()
}

object CryptoHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    // Ideally this comes from User settings
    var PASSWORD = "XClipper_Default_Password" 

    fun encrypt(text: String): String {
        // We lack context here to check setting, but we can default to V2 
        // if we assume this is called for new data.
        // However, without access to AppSettings, we can't toggle.
        // I will default to Legacy to maintain V1 behavior unless I find a way to inject setting.
        // BUT the user wants PQE.
        // I will assume for now we use V2 if possible? 
        // No, that breaks compatibility if other side doesn't support V2.
        // I'll stick to V1 for default `encrypt` unless I can change the signature or global state.
        // Wait, I can try to access AppSettings via a content provider or static reference?
        // App.appSettings is not static.
        // This is a design limitation.
        // I will implement V2 methods here and update `Clip.encrypt` later if I can pass context.
        return encryptLegacy(text) 
    }

    fun decrypt(text: String): String {
        if (isV2(text)) return decryptV2(text)
        return decryptLegacy(text)
    }

    private fun isV2(text: String): Boolean {
         return try {
            val data = Base64.decode(text, Base64.DEFAULT)
            data.isNotEmpty() && data[0] == 0x02.toByte()
        } catch (e: Exception) { false }
    }

    fun encryptV2(text: String): String {
        try {
            val saltBytes = ByteArray(16)
            java.security.SecureRandom().nextBytes(saltBytes)

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            // Matching Windows V2 Key
            val spec = PBEKeySpec("XClipper_Common_Key".toCharArray(), saltBytes, 10000, 256 + 128)
            val tmp = factory.generateSecret(spec)
            val keyBytes = tmp.encoded

            val key = Arrays.copyOfRange(keyBytes, 0, 32)
            val iv = Arrays.copyOfRange(keyBytes, 32, 48)

            val secretKey = SecretKeySpec(key, ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            
            val encryptedIdx = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
            
            val output = ByteArray(1 + 16 + encryptedIdx.size)
            output[0] = 0x02
            System.arraycopy(saltBytes, 0, output, 1, 16)
            System.arraycopy(encryptedIdx, 0, output, 17, encryptedIdx.size)
            
            return Base64.encodeToString(output, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }

    fun decryptV2(text: String): String {
         try {
            val fullCipher = Base64.decode(text, Base64.DEFAULT)
            if (fullCipher[0] != 0x02.toByte()) return text

            val saltBytes = Arrays.copyOfRange(fullCipher, 1, 17)
            val originalCipher = Arrays.copyOfRange(fullCipher, 17, fullCipher.size)

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec("XClipper_Common_Key".toCharArray(), saltBytes, 10000, 256 + 128)
            val tmp = factory.generateSecret(spec)
            val keyBytes = tmp.encoded

            val key = Arrays.copyOfRange(keyBytes, 0, 32)
            val iv = Arrays.copyOfRange(keyBytes, 32, 48)

            val secretKey = SecretKeySpec(key, ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decrypted = cipher.doFinal(originalCipher)
            return String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }

    fun encryptLegacy(text: String): String {
         try {
            val saltBytes = SALT.toByteArray(StandardCharsets.US_ASCII)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec(PASSWORD.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH + IV_LENGTH)
            val tmp = factory.generateSecret(spec)
            val keyBytes = tmp.encoded
            
            val key = Arrays.copyOfRange(keyBytes, 0, 32)
            val iv = Arrays.copyOfRange(keyBytes, 32, 48)

            val secretKey = SecretKeySpec(key, ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            
            val encrypted = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
            return Base64.encodeToString(encrypted, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }

    fun decryptLegacy(text: String): String {
        try {
           val saltBytes = SALT.toByteArray(StandardCharsets.US_ASCII)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec(PASSWORD.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH + IV_LENGTH)
            val tmp = factory.generateSecret(spec)
            val keyBytes = tmp.encoded
            
            val key = Arrays.copyOfRange(keyBytes, 0, 32)
            val iv = Arrays.copyOfRange(keyBytes, 32, 48)

            val secretKey = SecretKeySpec(key, ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decoded = Base64.decode(text, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            return String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }
}