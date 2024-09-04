package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class RotatableImageView extends FrameLayout {
    private TouchImageView touchImageView;
    private float currentRotation = 0f;
    private boolean isZooming = false;

    public RotatableImageView(Context context) {
        super(context);
        init(context);
    }

    public RotatableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        touchImageView = new TouchImageView(context);
        addView(touchImageView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        touchImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 2) {
                    isZooming = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (isZooming) {
                        isZooming = false;
                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                adjustSize();
                            }
                        });
                    }
                }
                return false; // Allow TouchImageView to handle the event
            }
        });
    }

    public void rotate90Degrees() {
        currentRotation = (currentRotation + 90) % 360;
        touchImageView.setRotation(currentRotation);
        post(new Runnable() {
            @Override
            public void run() {
                adjustSize();
                centerImage();
            }
        });
    }

    private void adjustSize() {
        float scale = touchImageView.getCurrentZoom();
        Drawable drawable = touchImageView.getDrawable();
        if (drawable == null) return;

        int imageWidth = drawable.getIntrinsicWidth();
        int imageHeight = drawable.getIntrinsicHeight();

        int newWidth, newHeight;

        if (currentRotation == 90 || currentRotation == 270) {
            newWidth = (int) (imageHeight * scale);
            newHeight = (int) (imageWidth * scale);
        } else {
            newWidth = (int) (imageWidth * scale);
            newHeight = (int) (imageHeight * scale);
        }

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = newWidth;
        layoutParams.height = newHeight;
        setLayoutParams(layoutParams);
    }

    private void centerImage() {
        touchImageView.post(new Runnable() {
            @Override
            public void run() {
                Drawable drawable = touchImageView.getDrawable();
                if (drawable == null) return;

                int viewWidth = getWidth();
                int viewHeight = getHeight();
                int drawableWidth = drawable.getIntrinsicWidth();
                int drawableHeight = drawable.getIntrinsicHeight();

                float widthRatio = (float) viewWidth / drawableWidth;
                float heightRatio = (float) viewHeight / drawableHeight;
                float scale = Math.min(widthRatio, heightRatio);

                Matrix matrix = new Matrix();
                matrix.setScale(scale, scale);
                matrix.postRotate(currentRotation, viewWidth / 2f, viewHeight / 2f);

                float translateX = (viewWidth - drawableWidth * scale) / 2f;
                float translateY = (viewHeight - drawableHeight * scale) / 2f;
                matrix.postTranslate(translateX, translateY);

                touchImageView.setImageMatrix(matrix);
            }
        });
    }

    // Delegate methods to TouchImageView as needed
    public void setImageResource(int resId) {
        touchImageView.setImageResource(resId);
        post(new Runnable() {
            @Override
            public void run() {
                centerImage();
            }
        });
    }

    public void setImageBitmap(Bitmap bitmap) {
        touchImageView.setImageBitmap(bitmap);
        post(new Runnable() {
            @Override
            public void run() {
                centerImage();
            }
        });
    }

    public void setScaleType(ImageView.ScaleType scaleType) {
        touchImageView.setScaleType(scaleType);
    }

    public void setMaxZoom(float maxZoom) {
        touchImageView.setMaxZoom(maxZoom);
    }

    // Add other necessary methods...
}