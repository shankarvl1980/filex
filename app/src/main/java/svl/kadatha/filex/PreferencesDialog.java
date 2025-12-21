package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import svl.kadatha.filex.usb.UsbDocumentProvider;

public class PreferencesDialog extends DialogFragment {
    public static String THEME;
    DetailFragment df;
    private Context context;
    private boolean byte_count_block_1000;
    private TinyDB tinyDB;
    private DefaultAppDatabaseHelper defaultAppDatabaseHelper;
    private boolean light_rb_checked, dark_rb_checked, system_rb_checked;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        df = (DetailFragment) getParentFragmentManager().findFragmentById(R.id.detail_fragment);
        tinyDB = new TinyDB(context);
        defaultAppDatabaseHelper = new DefaultAppDatabaseHelper(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_preferences, container, false);
        RadioButton light_rb = v.findViewById(R.id.preferences_rb_light);
        light_rb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!light_rb_checked) {
                    THEME = "light";
                    Global.THEME = PreferencesDialog.THEME;
                    tinyDB.putString("theme", Global.THEME);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    light_rb_checked = true;
                    dark_rb_checked = false;
                    system_rb_checked = false;
                }
            }
        });

        RadioButton dark_rb = v.findViewById(R.id.preferences_rb_dark);
        dark_rb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!dark_rb_checked) {
                    THEME = "dark";
                    Global.THEME = PreferencesDialog.THEME;
                    tinyDB.putString("theme", Global.THEME);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    dark_rb_checked = true;
                    light_rb_checked = false;
                    system_rb_checked = false;
                }
            }
        });

        RadioButton system_rb = v.findViewById(R.id.preferences_rb_system);
        system_rb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!system_rb_checked) {
                    THEME = "system";
                    Global.THEME = PreferencesDialog.THEME;
                    tinyDB.putString("theme", Global.THEME);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    system_rb_checked = true;
                    dark_rb_checked = false;
                    light_rb_checked = false;
                }
            }
        });

        THEME = Global.THEME;
        if (THEME != null) {
            switch (THEME) {
                case "system":
                    system_rb.setChecked(true);
                    system_rb_checked = true;
                    light_rb_checked = false;
                    dark_rb_checked = false;
                    break;
                case "light":
                    light_rb.setChecked(true);
                    light_rb_checked = true;
                    dark_rb_checked = false;
                    system_rb_checked = false;
                    break;
                case "dark":
                    dark_rb.setChecked(true);
                    dark_rb_checked = true;
                    light_rb_checked = false;
                    system_rb_checked = false;
                    break;
            }
        }


        Button remove_defaults_btn = v.findViewById(R.id.preferences_remove_all_defaults_btn);
        remove_defaults_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultAppDatabaseHelper.deleteTable();
                Global.print(context, getString(R.string.removed_default_apps_to_open_files));
            }
        });

        Button remove_selected_btn = v.findViewById(R.id.preferences_remove_select_defaults_btn);
        remove_selected_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DefaultAppsDialog defaultAppsDialog = new DefaultAppsDialog();
                defaultAppsDialog.show(getParentFragmentManager(), "");
            }
        });

        SwitchCompat switch_show_file_path = v.findViewById(R.id.preferences_switch_show_file_path);
        switch_show_file_path.setChecked(Global.SHOW_FILE_PATH);
        switch_show_file_path.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Global.SHOW_FILE_PATH = b;
                        tinyDB.putBoolean("show_file_path", Global.SHOW_FILE_PATH);
                        DetailFragment df = (DetailFragment) getParentFragmentManager().findFragmentById(R.id.detail_fragment);
                        if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                            getParentFragmentManager().beginTransaction().detach(df).commit();
                            getParentFragmentManager().beginTransaction().attach(df).commit();
                        }
                    }
                }, 500);
            }
        });

        SwitchCompat switch_show_usb = v.findViewById(R.id.preferences_switch_show_usb);
        switch_show_usb.setChecked(Global.RECOGNISE_USB);
        switch_show_usb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Global.RECOGNISE_USB = b;
                        tinyDB.putBoolean("recognise_usb", Global.RECOGNISE_USB);
                        if (Global.RECOGNISE_USB) {
                            Global.LOCAL_BROADCAST(UsbDocumentProvider.ACTION_USB_RECOGNITION_ENABLED, LocalBroadcastManager.getInstance(context), null);
                        }
                    }
                }, 500);
            }
        });

        ViewGroup buttons_layout = v.findViewById(R.id.preferences_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT));
        buttons_layout.setBackgroundResource(R.drawable.button_border);
        Button cancel_btn = buttons_layout.findViewById(R.id.first_button);
        cancel_btn.setText(R.string.close);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        defaultAppDatabaseHelper.close();
    }
}
