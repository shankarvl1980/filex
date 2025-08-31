package svl.kadatha.filex;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public abstract class BaseActivity extends AppCompatActivity {

    TinyDB tinyDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above
            getWindow().setDecorFitsSystemWindows(true);
        } else {
            // For Android 10 and below
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        Context context = this;
        tinyDB = new TinyDB(context);

        Global.GET_SCREEN_DIMENSIONS(context);
        Global.GET_URI_PERMISSIONS_LIST(context);
        Global.GET_IMAGE_VIEW_DIMENSIONS(context);
        Global.GET_PREFERENCES(tinyDB);
        Global.GET_ACTION_BAR_HEIGHT(context);
        Global.GET_STORAGE_DIR(context);

        switch (Global.THEME) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode());
                break;
        }
        StatusBarTint.tintFromAttrWithScrim(this, R.attr.toolbar_background);
    }
}
