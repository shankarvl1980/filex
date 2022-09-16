package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Yashar on 3/11/2017.
 */

public class InstaCropperActivity extends AppCompatActivity {

    private static final int DEFAULT_OUTPUT_QUALITY = 50;

    public static final String EXTRA_OUTPUT = MediaStore.EXTRA_OUTPUT;

    public static final String EXTRA_PREFERRED_RATIO = "preferred_ratio";
    public static final String EXTRA_MINIMUM_RATIO = "minimum_ratio";
    public static final String EXTRA_MAXIMUM_RATIO = "maximum_ratio";

    public static final String EXTRA_WIDTH_SPEC = "width_spec";
    public static final String EXTRA_HEIGHT_SPEC = "height_spec";

    public static final String EXTRA_OUTPUT_QUALITY = "output_quality";

    public static final String EXTRA_FILE_NAME="file_name";

    private boolean clear_cache;

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
        intent.putExtra(EXTRA_FILE_NAME,file_name);

        intent.putExtra(EXTRA_PREFERRED_RATIO, preferredRatio);
        intent.putExtra(EXTRA_MINIMUM_RATIO, minimumRatio);
        intent.putExtra(EXTRA_MAXIMUM_RATIO, maximumRatio);

        intent.putExtra(EXTRA_WIDTH_SPEC, widthSpec);
        intent.putExtra(EXTRA_HEIGHT_SPEC, heightSpec);
        intent.putExtra(EXTRA_OUTPUT_QUALITY, outputQuality);

        return intent;
    }

    private InstaCropperView mInstaCropper;

    private int mWidthSpec;
    private int mHeightSpec;
    private int mOutputQuality;

    private Uri mOutputUri;
    private String file_name;
    public static final String ACTIVITY_NAME="INSTA_CROPPER_ACTIVITY";
    public FrameLayout progress_bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instacropper);
        Toolbar toolbar = findViewById(R.id.crop_toolbar);
        ImageButton crop_button = findViewById(R.id.crop_imagebutton);
        crop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInstaCropper.crop(mWidthSpec,mHeightSpec,mBitmapCallback);
            }
        });

        mInstaCropper = findViewById(R.id.instacropper);
        progress_bar=findViewById(R.id.instacropper_activity_progressbar);
        progress_bar.setVisibility(View.GONE);
        //LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = getIntent();

        Uri uri = intent.getData();

        float defaultRatio = intent.getFloatExtra(EXTRA_PREFERRED_RATIO, InstaCropperView.DEFAULT_RATIO);
        float minimumRatio = intent.getFloatExtra(EXTRA_MINIMUM_RATIO, InstaCropperView.DEFAULT_MINIMUM_RATIO);
        float maximumRatio = intent.getFloatExtra(EXTRA_MAXIMUM_RATIO, InstaCropperView.DEFAULT_MAXIMUM_RATIO);

        mInstaCropper.setRatios(defaultRatio, minimumRatio, maximumRatio);
        mInstaCropper.setImageUri(uri);

        mWidthSpec = intent.getIntExtra(EXTRA_WIDTH_SPEC, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mHeightSpec = intent.getIntExtra(EXTRA_HEIGHT_SPEC, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mOutputQuality = intent.getIntExtra(EXTRA_OUTPUT_QUALITY, DEFAULT_OUTPUT_QUALITY);

        mOutputUri = intent.getParcelableExtra(EXTRA_OUTPUT);
        file_name=intent.getStringExtra(EXTRA_FILE_NAME);
    }


    private final InstaCropperView.BitmapCallback mBitmapCallback = new InstaCropperView.BitmapCallback() {

        @Override
        public void onBitmapReady(final Bitmap bitmap) {
            if (bitmap == null) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            try {
                OutputStream os = getContentResolver().openOutputStream(mOutputUri);

                bitmap.compress(Bitmap.CompressFormat.JPEG, mOutputQuality, os);

                os.flush();
                os.close();

                Intent data = new Intent();
                data.setData(mOutputUri);
                data.putExtra(EXTRA_FILE_NAME,file_name);
                setResult(RESULT_OK, data);
            } catch (IOException e)
            {
                setResult(RESULT_CANCELED);
            }
            finish();
        }

    };


    @Override
    protected void onStart()
    {
        // TODO: Implement this method
        super.onStart();
        clear_cache=true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache",clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache=savedInstanceState.getBoolean("clear_cache");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isFinishing() && !isChangingConfigurations() && clear_cache)
        {
            clearCache();
        }
    }

    public void clearCache()
    {
        Global.CLEAR_CACHE();
    }

}