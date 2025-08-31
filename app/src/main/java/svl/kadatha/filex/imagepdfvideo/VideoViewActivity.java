package svl.kadatha.filex.imagepdfvideo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import svl.kadatha.filex.BaseActivity;
import svl.kadatha.filex.FileIntentDispatch;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.R;
import svl.kadatha.filex.RealPathUtil;
import svl.kadatha.filex.StatusBarTint;
import svl.kadatha.filex.TinyDB;

public class VideoViewActivity extends BaseActivity {

    public static final String ACTIVITY_NAME = "VIDEO_VIEW_ACTIVITY";
    public Uri data;
    public int current_page_idx;
    public FileObjectType fileObjectType;
    public boolean fromThirdPartyApp;
    public String source_folder;
    public boolean clear_cache;
    public FilteredFilePOJOViewModel viewModel;
    TinyDB tinyDB;
    private FrameLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;
        setContentView(R.layout.activity_blank_view);
        root = findViewById(R.id.activity_blank_view_container);
        hideStatusBarKeepNavBar();
        applyBottomInsetOnly();
        tinyDB = new TinyDB(context);
        Intent intent = getIntent();
        on_intent(intent, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FilteredFilePOJOViewModel.class);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                clear_cache = false;
                remove();
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * Hides status bar, keeps nav bar visible
     */
    private void hideStatusBarKeepNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);   // we’ll handle insets ourselves
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars());     // hide only status bar
                c.show(WindowInsets.Type.navigationBars()); // keep nav bar
            }
        } else {
            // Legacy APIs 21‑29
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN   // allow drawing under status bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;         // hide status bar
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    /**
     * Gives the root view bottom padding equal to nav‑bar height (API‑safe)
     */
    private void applyBottomInsetOnly() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            // Height of navigation bar (0 if gesture nav hidden)
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, 0, 0, bottom);
            return WindowInsetsCompat.CONSUMED;   // we handled what we need
        });
        // Trigger first inset pass
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideStatusBarKeepNavBar();   // re‑enter fullscreen after transient bars
    }

    private void on_intent(Intent intent, Bundle savedInstanceState) {
        if (intent != null) {
            data = intent.getData();
            fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
            String file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
            boolean fromArchive = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
            if (file_path == null) {
                file_path = RealPathUtil.getLastSegmentPath(data);
            }

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.activity_blank_view_container, VideoViewContainerFragment.getNewInstance(file_path, fileObjectType, fromArchive), "").commit();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        on_intent(intent, null);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
        VideoViewContainerFragment videoViewContainerFragment = (VideoViewContainerFragment) getSupportFragmentManager().findFragmentById(R.id.activity_blank_view_container);
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache", clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache = savedInstanceState.getBoolean("clear_cache");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations() && clear_cache) {
            clearCache();
        }
    }

    public void clearCache() {
        Global.CLEAR_CACHE();
    }


    public interface VideoControlListener {
        void showControls(boolean autoHide);

        void hideControls();
    }

}
