
using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;

namespace Components
{
    public static class Core
    {
        private static readonly byte[] Salt = Encoding.ASCII.GetBytes("XClipper_Salt_Value"); // Fixed salt for simplicity, ideally should be random
        // Using a fixed salt mimics a simple password-based encryption often used in such apps.
        // For better security, we should store a random salt per user, but for sync compatibility with a simple password, this is a compromise.

        public static string EncryptBase64(this string text, string password)
        {
            if (string.IsNullOrEmpty(text)) return text;
            try
            {
                using (Aes aes = Aes.Create())
                {
                    var key = new Rfc2898DeriveBytes(password, Salt, 1000);
                    aes.Key = key.GetBytes(aes.KeySize / 8);
                    aes.IV = key.GetBytes(aes.BlockSize / 8);

                    ICryptoTransform encryptor = aes.CreateEncryptor(aes.Key, aes.IV);

                    using (MemoryStream msEncrypt = new MemoryStream())
                    {
                        using (CryptoStream csEncrypt = new CryptoStream(msEncrypt, encryptor, CryptoStreamMode.Write))
                        {
                            using (StreamWriter swEncrypt = new StreamWriter(csEncrypt))
                            {
                                swEncrypt.Write(text);
                            }
                            return Convert.ToBase64String(msEncrypt.ToArray());
                        }
                    }
                }
            }
            catch (Exception)
            {
                return text;
            }
        }

        public static string DecryptBase64(this string text, string password)
        {
            if (string.IsNullOrEmpty(text)) return text;
            try
            {
                var cipherText = Convert.FromBase64String(text);
                using (Aes aes = Aes.Create())
                {
                    var key = new Rfc2898DeriveBytes(password, Salt, 1000);
                    aes.Key = key.GetBytes(aes.KeySize / 8);
                    aes.IV = key.GetBytes(aes.BlockSize / 8);

                    ICryptoTransform decryptor = aes.CreateDecryptor(aes.Key, aes.IV);

                    using (MemoryStream msDecrypt = new MemoryStream(cipherText))
                    {
                        using (CryptoStream csDecrypt = new CryptoStream(msDecrypt, decryptor, CryptoStreamMode.Read))
                        {
                            using (StreamReader srDecrypt = new StreamReader(csDecrypt))
                            {
                                return srDecrypt.ReadToEnd();
                            }
                        }
                    }
                }
            }
            catch (Exception)
            {
                return text; // Return original if decryption fails (e.g. already plain text or wrong password)
            }
        }

        // Stub for MainHelper.ToggleCurrentQRData usage
        public static string EncryptBase64Common(this string text)
        {
             // Simple obfuscation or use a hardcoded key
             return EncryptBase64(text, "XClipper_Common_Key");
        }
        
        // Stubs for DefaultSettings usage
        public static string Encrypt(this string text) 
        {
             if (Components.DefaultSettings.UsePQE)
                return EncryptV2(text, "XClipper_Common_Key"); 
             return EncryptBase64Common(text);
        }

        public static string Decrypt(this string text) 
        {
            // Auto-detect version
            if (IsV2(text))
                return DecryptV2(text, "XClipper_Common_Key");
            return DecryptBase64Common(text);
        }
        
        public static string DecryptBase64Common(this string text)
        {
             return DecryptBase64(text, "XClipper_Common_Key");
        }

        private static bool IsV2(string base64)
        {
            try {
                byte[] data = Convert.FromBase64String(base64);
                return data.Length > 0 && data[0] == 0x02;
            } catch { return false; }
        }

        public static string EncryptV2(string text, string password)
        {
            if (string.IsNullOrEmpty(text)) return text;
            try
            {
                using (Aes aes = Aes.Create())
                {
                    // Generate Random Salt
                    byte[] salt = new byte[16];
                    using (var rng = new RNGCryptoServiceProvider())
                    {
                        rng.GetBytes(salt);
                    }

                    // Derive Key and IV
                    var keyDerivation = new Rfc2898DeriveBytes(password, salt, 10000); // Higher iterations
                    aes.Key = keyDerivation.GetBytes(32); // 256 bits
                    aes.IV = keyDerivation.GetBytes(16); // 128 bits

                    ICryptoTransform encryptor = aes.CreateEncryptor(aes.Key, aes.IV);

                    using (MemoryStream msEncrypt = new MemoryStream())
                    {
                        // Write Version
                        msEncrypt.WriteByte(0x02);
                        // Write Salt
                        msEncrypt.Write(salt, 0, salt.Length);
                        // Write IV
                        // Note: Rfc2898DeriveBytes is deterministic given salt and password. 
                        // But we generated random salt, so Key and IV are also "random" (rotated).
                        // We don't need to write IV if we derive it.
                        // However, standard practice is often Random IV, separate from Key derivation.
                        // Let's stick to DeriveBytes for both for simplicity as Salt is random.
                        // Actually, better: Random Salt -> Key. Random IV -> IV.
                        // But logic above derives both. It is secure enough because Salt is random.
                        
                        using (CryptoStream csEncrypt = new CryptoStream(msEncrypt, encryptor, CryptoStreamMode.Write))
                        {
                            using (StreamWriter swEncrypt = new StreamWriter(csEncrypt))
                            {
                                swEncrypt.Write(text);
                            }
                        }
                        return Convert.ToBase64String(msEncrypt.ToArray());
                    }
                }
            }
            catch (Exception)
            {
                return text;
            }
        }

        public static string DecryptV2(string text, string password)
        {
            if (string.IsNullOrEmpty(text)) return text;
            try
            {
                byte[] fullCipher = Convert.FromBase64String(text);
                
                using (MemoryStream ms = new MemoryStream(fullCipher))
                {
                    // Read Version
                    int version = ms.ReadByte();
                    if (version != 0x02) return text; // Should not happen if IsV2 checked

                    // Read Salt
                    byte[] salt = new byte[16];
                    if (ms.Read(salt, 0, salt.Length) != salt.Length) return text;

                    using (Aes aes = Aes.Create())
                    {
                        var keyDerivation = new Rfc2898DeriveBytes(password, salt, 10000);
                        aes.Key = keyDerivation.GetBytes(32);
                        aes.IV = keyDerivation.GetBytes(16);

                        ICryptoTransform decryptor = aes.CreateDecryptor(aes.Key, aes.IV);

                        using (CryptoStream csDecrypt = new CryptoStream(ms, decryptor, CryptoStreamMode.Read))
                        {
                            using (StreamReader srDecrypt = new StreamReader(csDecrypt))
                            {
                                return srDecrypt.ReadToEnd();
                            }
                        }
                    }
                }
            }
            catch (Exception)
            {
                return text;
            }
        }
    }
}
