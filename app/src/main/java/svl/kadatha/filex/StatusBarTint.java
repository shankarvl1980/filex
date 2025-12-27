package svl.kadatha.filex;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class StatusBarTint {
    private static final String SCRIM_TAG = "status_bar_scrim_view";

    private StatusBarTint() {
    }

    /**
     * Resolve color from attr, darken it a bit (e.g. 0.85f), then tint using a scrim.
     */
    public static void tintFromAttrWithScrim(@NonNull Activity activity, @AttrRes int colorAttr) {
        int base = resolveAttrColor(activity, colorAttr);
        int dark = darkenColor(base, 0.8f);
        tintWithScrim(activity, dark);
    }

    /**
     * Tint status bar area using a top overlay scrim. No icon changes.
     */
    public static void tintWithScrim(@NonNull Activity activity, @ColorInt int color) {

        Window window = activity.getWindow();
        ViewGroup decor = (ViewGroup) window.getDecorView();

        // Reuse or create the scrim view
        View scrim = decor.findViewWithTag(SCRIM_TAG);
        if (scrim == null) {
            scrim = new View(activity);
            scrim.setTag(SCRIM_TAG);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0);
            // DecorView is a FrameLayout, so gravity works:
            decor.addView(scrim, new ViewGroup.MarginLayoutParams(lp));
            // Make sure it sits on top
            scrim.bringToFront();

            // Keep the scrim height in sync with status bar insets
            View finalScrim = scrim;
            ViewCompat.setOnApplyWindowInsetsListener(finalScrim, (v, insets) -> {
                int h = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                ViewGroup.LayoutParams p = v.getLayoutParams();
                if (p.height != h) {
                    p.height = h;
                    v.setLayoutParams(p);
                }
                v.setVisibility(h > 0 ? View.VISIBLE : View.GONE);
                return insets;
            });
        }

        scrim.setBackgroundColor(color);
        // Request insets so the listener above runs and sizes the scrim correctly
        ViewCompat.requestApplyInsets(scrim);
    }

    /**
     * Resolve a theme color attribute to a concrete color int.
     */
    @ColorInt
    public static int resolveAttrColor(@NonNull Activity activity, @AttrRes int attr) {
        TypedValue tv = new TypedValue();
        boolean ok = activity.getTheme().resolveAttribute(attr, tv, true);
        if (!ok) return 0;
        if (tv.resourceId != 0) {
            // Works on minSdk 21 (and below)
            return ContextCompat.getColor(activity, tv.resourceId);
        }
        return tv.data;
    }

    /**
     * Darken a color by scaling its HSL lightness.
     */
    @ColorInt
    public static int darkenColor(@ColorInt int color, float factor) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        hsl[2] = clamp01(hsl[2] * factor);
        return ColorUtils.HSLToColor(hsl);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
