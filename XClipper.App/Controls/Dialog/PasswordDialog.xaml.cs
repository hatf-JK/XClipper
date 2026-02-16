using System.Windows;

namespace Components.Controls.Dialog
{
    public partial class PasswordDialog : Window
    {
        public static string Password;
        public PasswordDialog()
        {
            InitializeComponent();
            Loaded += (s, e) => pbText.Focus();
        }

        private void CancelButton_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
            Close();
        }
        private void OKButton_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = true;
            Password = pbText.Password;
            Close();
        }

        public class Builder
        {
            private PasswordDialog dialog = new PasswordDialog();
            public Builder SetTitle(string value)
            {
                dialog.Title = value;
                return this;
            }
            public Builder SetTopMost(bool value)
            {
                dialog.Topmost = value;
                return this;
            }
            public Builder SetMessage(string value)
            {
                dialog.tbMsg.Text = value;
                return this;
            }
            public string? Show()
            {
                if (dialog.ShowDialog() == true)
                    return Password;
                else 
                    return null;
            }
        }
    }
}
