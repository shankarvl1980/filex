package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

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
    private VideoViewContainerFragment videoViewContainerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        Context context = this;
        setContentView(R.layout.activity_blank_view);
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

    private void on_intent(Intent intent, Bundle savedInstanceState) {
        if (intent != null) {
            data = intent.getData();
            fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
            String file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
            boolean fromArchive=intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE,false);
            if (file_path == null) file_path = RealPathUtil.getLastSegmentPath(data);

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.activity_blank_view_container, VideoViewContainerFragment.getNewInstance(file_path, fileObjectType,fromArchive), "").commit();
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
        // TODO: Implement this method
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        // TODO: Implement this method
        super.onStart();
        clear_cache = true;
        videoViewContainerFragment = (VideoViewContainerFragment) getSupportFragmentManager().findFragmentById(R.id.activity_blank_view_container);
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


    public void onClickFragment() {
        if (videoViewContainerFragment != null) {
            videoViewContainerFragment.onVideoViewClick();
        }
    }

}
