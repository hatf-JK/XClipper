using System.Windows;

namespace Components
{
    public partial class InputPasswordWindow : Window
    {
        public string Password { get; private set; }

        public InputPasswordWindow(string title, string message, bool showConfirm = false)
        {
            InitializeComponent();
            TitleText.Text = title;
            MessageText.Text = message;
            
            if (showConfirm)
            {
                ConfirmPassword.Visibility = Visibility.Visible;
                Height = 260; 
            }
            InputPassword.Focus();
        }

        private void BtnOk_Click(object sender, RoutedEventArgs e)
        {
            if (ConfirmPassword.Visibility == Visibility.Visible)
            {
                if (InputPassword.Password != ConfirmPassword.Password)
                {
                    MessageBox.Show("Passwords do not match.", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }
            }

            if (string.IsNullOrEmpty(InputPassword.Password))
            {
                MessageBox.Show("Password cannot be empty.", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                return;
            }

            Password = InputPassword.Password;
            DialogResult = true;
            Close();
        }

        private void BtnCancel_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
            Close();
        }
    }
}
