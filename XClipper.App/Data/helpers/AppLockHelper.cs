using Components.UI;
using System;
using System.Windows;
using static Components.DefaultSettings;

namespace Components
{
    public static class AppLockHelper
    {
        private static long _lastDeactivatedTime = -1;
        private const long TIMEOUT_MS = 60 * 1000; // 1 minute

        public static void CheckLockOnStartup()
        {
            if (IsAppLockEnabled)
            {
                ShowLockScreen();
            }
        }

        public static void OnDeactivated()
        {
            if (IsAppLockEnabled)
            {
                _lastDeactivatedTime = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            }
        }

        public static void CheckLockOnResume()
        {
            if (IsAppLockEnabled && _lastDeactivatedTime != -1)
            {
                long currentTime = DateTimeOffset.Now.ToUnixTimeMilliseconds();
                if ((currentTime - _lastDeactivatedTime) > TIMEOUT_MS)
                {
                    ShowLockScreen();
                }
                _lastDeactivatedTime = -1; // Reset
            }
        }
        
        private static void ShowLockScreen()
        {
            // Ensure we are on UI thread
            Application.Current.Dispatcher.Invoke(() => {
                var lockWindow = new LockWindow();
                lockWindow.ShowDialog();
            });
        }
    }
}
