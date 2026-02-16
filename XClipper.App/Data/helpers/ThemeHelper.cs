using System;
using System.Linq;
using System.Windows;
using static Components.DefaultSettings;

namespace Components
{
    public static class ThemeHelper
    {
        public static void ApplyTheme()
        {
            var dictionaries = Application.Current.Resources.MergedDictionaries;
            var colorsUri = new Uri("Styles/Colors.xaml", UriKind.Relative);
            var amoledUri = new Uri("Styles/AmoledColors.xaml", UriKind.Relative);

            var colorsDict = dictionaries.FirstOrDefault(d => d.Source != null && d.Source.OriginalString.EndsWith("Styles/Colors.xaml"));
            var amoledDict = dictionaries.FirstOrDefault(d => d.Source != null && d.Source.OriginalString.EndsWith("Styles/AmoledColors.xaml"));

            if (colorsDict != null) dictionaries.Remove(colorsDict);
            if (amoledDict != null) dictionaries.Remove(amoledDict);

            if (UseAmoledTheme)
            {
                dictionaries.Insert(0, new ResourceDictionary { Source = amoledUri });
            }
            else
            {
                dictionaries.Insert(0, new ResourceDictionary { Source = colorsUri });
            }
        }
    }
}
