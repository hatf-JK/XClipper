using System;
using System.Linq;
using static Components.DefaultSettings;
using Components.Data.helpers;

namespace Components
{
    public static class AutoDeleteHelper
    {
        public static void RunAutoDelete()
        {
            if (!AutoDeleteEnabled) return;

            try
            {
                var cutoffDate = DateTime.Now.AddDays(-AutoDeletePeriod);
                var clipsToDelete = DatabaseHelper.GetClips()
                    .Where(c => !string.IsNullOrEmpty(c.Date) && DateTime.Parse(c.Date) < cutoffDate)
                    .ToList();

                if (clipsToDelete.Count > 0)
                {
                    foreach (var clip in clipsToDelete)
                    {
                        DatabaseHelper.DeleteClip(clip.Id);
                    }
                    LogHelper.Log($"Auto-Deleted {clipsToDelete.Count} clips older than {AutoDeletePeriod} days.");
                }
            }
            catch (Exception ex)
            {
                LogHelper.Log("AutoDelete Error: " + ex.Message);
            }
        }
    }
}
