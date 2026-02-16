using System;
using System.Windows;
using System.Windows.Input;
using System.Windows.Controls;
using static Components.DefaultSettings;

namespace Components.UI
{
    public partial class LockWindow : Window
    {
        private bool _isAuthenticated = false;

        public LockWindow()
        {
            InitializeComponent();
            UnlockButton.Click += UnlockButton_Click;
            Loaded += (s, e) => {
                PinBox.Focus();
                Activate();
            };
        }

        private void UnlockButton_Click(object sender, RoutedEventArgs e)
        {
            AttemptUnlock();
        }

        private void PinBox_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                AttemptUnlock();
            }
        }

        private void AttemptUnlock()
        {
            if (PinBox.Password.Encrypt() == AppLockPassword)
            {
                _isAuthenticated = true;
                Close();
            }
            else
            {
                StatusText.Text = "Incorrect PIN/Password";
                PinBox.Password = "";
                PinBox.Focus();
            }
        }

        protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
        {
            if (!_isAuthenticated)
            {
                Application.Current.Shutdown();
            }
            base.OnClosing(e);
        }
    }
}
