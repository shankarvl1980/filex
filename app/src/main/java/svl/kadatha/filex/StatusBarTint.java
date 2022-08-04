package svl.kadatha.filex;

import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class StatusBarTint 
{

// The public static function which can be called from other classes
	public static void darkenStatusBar(AppCompatActivity activity, int color)
	{

		Window window=activity.getWindow();
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.setStatusBarColor(darkenColor(ContextCompat.getColor(activity, color)));

	}


	// Code to darken the color supplied (mostly color of toolbar)
    private static int darkenColor(int color) 
	{
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }


}
 
