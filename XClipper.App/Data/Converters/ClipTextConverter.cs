using ClipboardManager.models;
using System;
using System.Globalization;
using System.Windows.Data;

namespace Components
{
    public class ClipTextConverter : IValueConverter
    {
        public static ClipTextConverter Instance = new ClipTextConverter();

        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is TableCopy clip)
            {
                if (clip.IsMasked)
                {
                    return "******"; 
                }
                if (clip.IsAdHocEncrypted)
                {
                    return "🔒 Encrypted Content";
                }
                // Return the requested property (Text or LongText)
                if (parameter as string == "LongText")
                    return clip.LongText;
                return clip.Text;
            }
            return "";
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
