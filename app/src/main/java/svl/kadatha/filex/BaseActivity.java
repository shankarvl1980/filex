package svl.kadatha.filex;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public abstract class BaseActivity extends AppCompatActivity {

    private Context context;
    TinyDB tinyDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        tinyDB = new TinyDB(context);

        Global.GET_SCREEN_DIMENSIONS(context);
        Global.GET_URI_PERMISSIONS_LIST(context);
        Global.GET_IMAGE_VIEW_DIMENSIONS(context);
        Global.GET_PREFERENCES(tinyDB);
        Global.GET_ACTION_BAR_HEIGHT(context);
        Global.GET_STORAGE_DIR(context);
        Global.GET_NAVIGATION_BAR_HEIGHT(context);

       switch (Global.THEME)
       {

           case "light":
               AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
               break;
           case "dark":
               AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
           case "system":
               AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode());
               break;
       }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Global.GET_SCREEN_DIMENSIONS(context);

    }

}
