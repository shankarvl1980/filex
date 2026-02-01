package svl.kadatha.filex.imagepdfvideo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import svl.kadatha.filex.BaseActivity;
import svl.kadatha.filex.FileIntentDispatch;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.R;
import svl.kadatha.filex.RealPathUtil;

public class PdfViewActivity extends BaseActivity {

    public static final String ACTIVITY_NAME = "PDF_VIEW_ACTIVITY";
    public Uri data;
    public boolean clear_cache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank_view);
        Intent intent = getIntent();
        on_intent(intent, savedInstanceState);
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
            FileObjectType fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
            boolean fromArchive = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
            String file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
            if (file_path == null) {
                file_path = RealPathUtil.getLastSegmentPath(data);
            }
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.activity_blank_view_container, PdfViewFragment.getNewInstance(file_path, fileObjectType, fromArchive), "pdf_view_fragment").commit();
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
}