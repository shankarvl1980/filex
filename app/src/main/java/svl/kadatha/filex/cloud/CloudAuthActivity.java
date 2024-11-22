package svl.kadatha.filex.cloud;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import svl.kadatha.filex.BaseActivity;
import svl.kadatha.filex.FileIntentDispatch;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.R;
import svl.kadatha.filex.RealPathUtil;
import svl.kadatha.filex.imagepdfvideo.ImageViewFragment;

public class CloudAuthActivity extends BaseActivity {
    public boolean clear_cache;
    public CloudAccountViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;
        setContentView(R.layout.activity_cloud_auth);
        viewModel = new ViewModelProvider(this).get(CloudAccountViewModel.class);
    }


    private void on_intent(Intent intent, Bundle savedInstanceState) {
        if (intent != null) {

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
