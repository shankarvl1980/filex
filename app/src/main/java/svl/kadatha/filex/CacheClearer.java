package svl.kadatha.filex;

import android.app.Application;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CacheClearer {

    /**
     * Pref key storing last-cleared month as YYYYMM (e.g., 202508).
     */
    private static final String KEY_CACHE_CLEARED_YYYYMM = "cache_cleared_yyyymm";

    /**
     * Whether we decided to clear during this app run.
     */
    private static final AtomicBoolean sShouldClearThisRun = new AtomicBoolean(false);

    /**
     * Ensures perform happens only once per run.
     */
    private static final AtomicBoolean sPerformed = new AtomicBoolean(false);

    private CacheClearer() {
    }

    /**
     * Compute current YYYYMM (month is 1..12).
     */
    private static int currentYyyyMm() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month1 = cal.get(Calendar.MONTH) + 1; // 1..12
        return year * 100 + month1;
    }

    /**
     * Strict validator for YYYYMM. Resilient against tampering.
     */
    private static boolean isValidYyyyMm(int value) {
        if (value < 200001 || value > 999912) return false; // coarse sanity bounds
        int mm = value % 100;
        return mm >= 1 && mm <= 12;
    }

    /**
     * Reads last value safely; if invalid, resets to 0 in prefs and returns 0.
     * 0 means "unset".
     */
    private static int safeGetLastClearYyyyMm(TinyDB tinyDB) {
        // Safe: TinyDB#getInt(key, 0) already catches ClassCastException and writes back 0
        int stored = tinyDB.getInt(KEY_CACHE_CLEARED_YYYYMM, 0);

        if (stored == 0) return 0;               // unset or self-healed to 0
        if (isValidYyyyMm(stored)) return stored;

        // Invalid format/value → self-heal to 0
        tinyDB.putInt(KEY_CACHE_CLEARED_YYYYMM, 0);
        return 0;
    }


    /**
     * Call early (e.g., in Application#onCreate). Only decides; does NOT write or delete.
     * If last (validated) value is different from current YYYYMM (or unset), schedules a clear.
     */
    public static void decideForThisRun(TinyDB tinyDB) {
        int last = safeGetLastClearYyyyMm(tinyDB);
        int now = currentYyyyMm();
        if (last == 0 || last != now) {
            sShouldClearThisRun.set(true);
        }
    }

    /**
     * Call when the app is going to background/close (e.g., from ProcessLifecycleOwner.onStop()).
     * Performs deletions ONCE per run if previously decided, then writes current YYYYMM.
     */
    public static void performIfDecided(Application application, TinyDB tinyDB) {
        if (!sShouldClearThisRun.get()) return;
        if (!sPerformed.compareAndSet(false, true)) return;

        try {
            // ---- Your async deletions ----
            Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(application.getCacheDir());
            Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.PDF_CACHE_DIR);
            if (Global.SIZE_APK_ICON_LIST > 800) {
                Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.APK_ICON_DIR);
            }
            // --------------------------------

            // Mark as cleared for this month only after scheduling deletions
            tinyDB.putInt(KEY_CACHE_CLEARED_YYYYMM, currentYyyyMm());
            Global.print(application, "cleared cache");

        } catch (Throwable t) {
            // If something failed very early, let future attempts run again this process
            sPerformed.set(false);
            Global.print(application, "cache clear failed: " + t.getMessage());
        }
    }
}