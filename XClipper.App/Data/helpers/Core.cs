
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
        public static string Encrypt(this string text) => EncryptBase64Common(text);
        public static string Decrypt(this string text) => DecryptBase64Common(text);
        
        public static string DecryptBase64Common(this string text)
        {
             return DecryptBase64(text, "XClipper_Common_Key");
        }
    }
}
