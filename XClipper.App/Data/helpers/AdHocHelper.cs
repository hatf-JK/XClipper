using System;
using System.Security.Cryptography;
using System.Text;
using System.IO;

namespace Components
{
    public static class AdHocHelper
    {
        private const string ENCRYPTION_PREFIX = "XClipper:ENC:";
        private const int ITERATIONS = 10000;
        private const int SALT_SIZE = 16;
        private const int IV_SIZE = 16;
        private const int KEY_SIZE = 32; // 256 bit

        public static bool IsAdHocEncrypted(string text)
        {
            return !string.IsNullOrEmpty(text) && text.StartsWith(ENCRYPTION_PREFIX);
        }

        public static string Encrypt(string plainText, string passphrase)
        {
            if (string.IsNullOrEmpty(plainText) || string.IsNullOrEmpty(passphrase))
                return null;

            try
            {
                // Generate random salt
                byte[] salt = new byte[SALT_SIZE];
                using (var rng = new RNGCryptoServiceProvider())
                {
                    rng.GetBytes(salt);
                }

                // Derive key and IV
                using (var deriveBytes = new Rfc2898DeriveBytes(passphrase, salt, ITERATIONS))
                {
                    byte[] key = deriveBytes.GetBytes(KEY_SIZE);
                    byte[] iv = deriveBytes.GetBytes(IV_SIZE);

                    using (var aes = Aes.Create())
                    {
                        aes.Key = key;
                        aes.IV = iv;
                        aes.Mode = CipherMode.CBC;
                        aes.Padding = PaddingMode.PKCS7;

                        using (var encryptor = aes.CreateEncryptor())
                        using (var ms = new MemoryStream())
                        {
                            using (var cs = new CryptoStream(ms, encryptor, CryptoStreamMode.Write))
                            using (var sw = new StreamWriter(cs))
                            {
                                sw.Write(plainText);
                            }
                            
                            byte[] cipherText = ms.ToArray();
                            
                            // Format: XClipper:ENC:1:[SaltBase64]:[IVBase64]:[CipherBase64]
                            return $"{ENCRYPTION_PREFIX}1:{Convert.ToBase64String(salt)}:{Convert.ToBase64String(iv)}:{Convert.ToBase64String(cipherText)}";
                        }
                    }
                }
            }
            catch (Exception)
            {
                return null;
            }
        }

        public static string Decrypt(string encryptedText, string passphrase)
        {
            if (!IsAdHocEncrypted(encryptedText) || string.IsNullOrEmpty(passphrase))
                return null;

            try
            {
                // Parse format
                // XClipper:ENC:Version:Salt:IV:Cipher
                var parts = encryptedText.Split(':');
                if (parts.Length < 6) return null;

                int version = int.Parse(parts[2]); // Currently unused but good for future
                if (version != 1) return null; // Only support v1 for now

                byte[] salt = Convert.FromBase64String(parts[3]);
                byte[] iv = Convert.FromBase64String(parts[4]);
                byte[] cipherBytes = Convert.FromBase64String(parts[5]);

                using (var deriveBytes = new Rfc2898DeriveBytes(passphrase, salt, ITERATIONS))
                {
                    byte[] key = deriveBytes.GetBytes(KEY_SIZE);

                    using (var aes = Aes.Create())
                    {
                        aes.Key = key;
                        aes.IV = iv;
                        aes.Mode = CipherMode.CBC;
                        aes.Padding = PaddingMode.PKCS7;

                        using (var decryptor = aes.CreateDecryptor())
                        using (var ms = new MemoryStream(cipherBytes))
                        using (var cs = new CryptoStream(ms, decryptor, CryptoStreamMode.Read))
                        using (var sr = new StreamReader(cs))
                        {
                            return sr.ReadToEnd();
                        }
                    }
                }
            }
            catch (Exception)
            {
                // Decryption failed (wrong password or data corruption)
                return null;
            }
        }
    }
}
