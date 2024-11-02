package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

/**
 * Created by Yashar on 3/11/2017.
 */

public class InstaCropperActivity extends BaseActivity {

    public static final String EXTRA_OUTPUT = MediaStore.EXTRA_OUTPUT;
    public static final String EXTRA_PREFERRED_RATIO = "preferred_ratio";
    public static final String EXTRA_MINIMUM_RATIO = "minimum_ratio";
    public static final String EXTRA_MAXIMUM_RATIO = "maximum_ratio";
    public static final String EXTRA_WIDTH_SPEC = "width_spec";
    public static final String EXTRA_HEIGHT_SPEC = "height_spec";
    public static final String EXTRA_OUTPUT_QUALITY = "output_quality";
    public static final String EXTRA_FILE_NAME = "file_name";
    public static final String ACTIVITY_NAME = "INSTA_CROPPER_ACTIVITY";
    private static final int DEFAULT_OUTPUT_QUALITY = 50;
    public boolean clear_cache;
    public ConstraintLayout progress_bar;
    private InstaCropperView mInstaCropper;
    private int mWidthSpec;
    private int mHeightSpec;
    private Uri mOutputUri;
    private String file_name;
    private final InstaCropperView.BitmapCallback mBitmapCallback = new InstaCropperView.BitmapCallback() {

        @Override
        public void onBitmapReady(final Bitmap bitmap) {
            if (bitmap == null) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            try (OutputStream os = getContentResolver().openOutputStream(mOutputUri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            } catch (IOException e) {

            }
            Intent data = new Intent();
            data.setData(mOutputUri);
            data.putExtra(EXTRA_FILE_NAME, file_name);
            setResult(RESULT_OK, data);
            finish();
        }
    };

    public static Intent getIntent(Context context, Uri src, Uri dst, String file_name, int maxWidth, int outputQuality) {
        return getIntent(
                context,
                src,
                dst,
                file_name,
                View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                outputQuality
        );
    }

    public static Intent getIntent(Context context, Uri src, Uri dst, String file_name, int widthSpec, int heightSpec, int outputQuality) {
        return getIntent(
                context,
                src,
                dst,
                file_name,
                InstaCropperView.DEFAULT_RATIO,
                InstaCropperView.DEFAULT_MINIMUM_RATIO,
                InstaCropperView.DEFAULT_MAXIMUM_RATIO,
                widthSpec,
                heightSpec,
                outputQuality
        );
    }

    public static Intent getIntent(Context context, Uri src, Uri dst, String file_name,
                                   float preferredRatio, float minimumRatio, float maximumRatio,
                                   int widthSpec, int heightSpec, int outputQuality) {
        Intent intent = new Intent(context, InstaCropperActivity.class);

        intent.setData(src);

        intent.putExtra(EXTRA_OUTPUT, dst);
        intent.putExtra(EXTRA_FILE_NAME, file_name);

        intent.putExtra(EXTRA_PREFERRED_RATIO, preferredRatio);
        intent.putExtra(EXTRA_MINIMUM_RATIO, minimumRatio);
        intent.putExtra(EXTRA_MAXIMUM_RATIO, maximumRatio);

        intent.putExtra(EXTRA_WIDTH_SPEC, widthSpec);
        intent.putExtra(EXTRA_HEIGHT_SPEC, heightSpec);
        intent.putExtra(EXTRA_OUTPUT_QUALITY, outputQuality);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instacropper);
        Toolbar toolbar = findViewById(R.id.crop_toolbar);
        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 2, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.wallpaper_icon, R.drawable.cancel_icon};
        String[] titles = new String[]{getString(R.string.set_wallpaper), getString(R.string.cancel)};
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);

        toolbar.addView(tb_layout);
        Button crop_button = toolbar.findViewById(R.id.toolbar_btn_1);
        crop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInstaCropper.crop(mWidthSpec, mHeightSpec, mBitmapCallback);
            }
        });

        Button cancel_button = toolbar.findViewById(R.id.toolbar_btn_2);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mInstaCropper = findViewById(R.id.instacropper);
        progress_bar = findViewById(R.id.instacropper_activity_progressbar);
        progress_bar.setVisibility(View.GONE);

        Intent intent = getIntent();

        Uri uri = intent.getData();

        float defaultRatio = intent.getFloatExtra(EXTRA_PREFERRED_RATIO, InstaCropperView.DEFAULT_RATIO);
        float minimumRatio = intent.getFloatExtra(EXTRA_MINIMUM_RATIO, InstaCropperView.DEFAULT_MINIMUM_RATIO);
        float maximumRatio = intent.getFloatExtra(EXTRA_MAXIMUM_RATIO, InstaCropperView.DEFAULT_MAXIMUM_RATIO);

        mInstaCropper.setRatios(defaultRatio, minimumRatio, maximumRatio);
        mInstaCropper.setImageUri(uri);

        mWidthSpec = intent.getIntExtra(EXTRA_WIDTH_SPEC, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mHeightSpec = intent.getIntExtra(EXTRA_HEIGHT_SPEC, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int mOutputQuality = intent.getIntExtra(EXTRA_OUTPUT_QUALITY, DEFAULT_OUTPUT_QUALITY);

        mOutputUri = intent.getParcelableExtra(EXTRA_OUTPUT);
        file_name = intent.getStringExtra(EXTRA_FILE_NAME);
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