using System;
using System.Threading.Tasks;
using System.Windows;
using System.Diagnostics;
using static Components.DefaultSettings;
using static Components.Constants;

#nullable enable

namespace Components
{
    public interface IFirebaseDataListener
    {
        void OnFirebaseDataChange();
    }

    public static class FirebaseHelper
    {
        // Re-adding extension methods for compatibility with FirebaseSingletonV2
        public static string EncryptBase64(this string text, string password) => Core.EncryptBase64(text, password);
        public static string DecryptBase64(this string text, string password) => Core.DecryptBase64(text, password);

        public static async Task InitializeService(IFirebaseBinderV2? binder = null)
        {
            if (binder != null)
                FirebaseSingletonV2.GetInstance.BindUI(binder);
            
            if (BindDatabase)
            {
                await FirebaseSingletonV2.GetInstance.Initialize();
                // MainHelper.ToggleCurrentQRData(); // Assuming MainHelper exists
            } 
            else 
            {
                DeInitializeService();
            }
        }

        public static void DeInitializeService()
        {
            if (FirebaseSingletonV2.GetInstance.isInitialized())
            {
                FirebaseSingletonV2.GetInstance.Deinitialize();
                // MainHelper.ToggleCurrentQRData();
            }
        }

        public static void AddContent(string data)
        {
            _ = FirebaseSingletonV2.GetInstance.AddClip(data);
        }

        public static void UpdateContent(string oldData, string newData)
        {
            _ = FirebaseSingletonV2.GetInstance.UpdateData(oldData, newData);
        }

        // Methods below are kept for compatibility if called from UI but stubbed or redirected
        
        public static void ShowSurpassMessage()
        {
             Process.Start(ACTION_NOT_COMPLETE_WIKI);
        }
    }
}
