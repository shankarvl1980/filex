package svl.kadatha.filex;

import android.content.Context;
import android.util.DisplayMetrics;

public final class Dimens {
    private static float density;       // dp → px
    private static float scaledDensity; // sp → px

    private Dimens() {
    }

    public static void init(Context context) {
        // Use Application context to avoid leaking Activity
        Context app = context.getApplicationContext();
        DisplayMetrics dm = app.getResources().getDisplayMetrics();
        density = dm.density;
        scaledDensity = dm.scaledDensity;
    }

    public static int px(int dp) {
        // Rounds to nearest int, consistent with getDimensionPixelSize()
        return Math.round(dp * density);
    }

    public static int spPx(int sp) {
        return Math.round(sp * scaledDensity);
    }

    public static int dpFromPx(int px) {
        return Math.round(px / density);
    }
}

