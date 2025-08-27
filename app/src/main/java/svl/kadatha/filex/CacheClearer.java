package svl.kadatha.filex;

import android.app.Application;

public final class CacheClearer {
    private static final String KEY_CACHE_CLEARED_YYYYMM = "cache_cleared_yyyymm";
    private static final java.util.concurrent.atomic.AtomicBoolean sShouldClearThisRun =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicBoolean sPerformed =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static int currentYyyyMm() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month1 = cal.get(java.util.Calendar.MONTH) + 1; // 1..12
        return year * 100 + month1;                         // e.g., 202508
    }

    /** Call once at startup. */
    public static void decideForThisRun(TinyDB tinyDB) {
        final int last = tinyDB.getInt(KEY_CACHE_CLEARED_YYYYMM); // returns 0 if missing
        final int now  = currentYyyyMm();
        // If never set (0) or different month, plan to clear
        if (last == 0 || last != now) {
            sShouldClearThisRun.set(true);
        }
    }

    /** Call on app background/close. */
    public static boolean performIfDecided(android.app.Application application, TinyDB tinyDB) {
        if (!sShouldClearThisRun.get()) return false;
        if (!sPerformed.compareAndSet(false, true)) return false;

        // Your async deletions
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(application.getCacheDir());
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.PDF_CACHE_DIR);
        if (Global.SIZE_APK_ICON_LIST > 800) {
            Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.APK_ICON_DIR);
        }

        // Mark cleared only after scheduling deletions
        tinyDB.putInt(KEY_CACHE_CLEARED_YYYYMM, currentYyyyMm());
        Global.print(application, "cleared cache");
        return true;
    }
}
