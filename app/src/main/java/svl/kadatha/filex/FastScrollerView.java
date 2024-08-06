package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class FastScrollerView extends View {
    private RecyclerView recyclerView;
    private Drawable thumbDrawable;
    private int thumbHeight;
    private boolean isDragging;
    private boolean isThumbVisible = false;
    private Handler hideHandler = new Handler();
    private static final long HIDE_DELAY = 2000; // 2 seconds

    public FastScrollerView(Context context) {
        super(context);
        init(null);
    }

    public FastScrollerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        thumbHeight = 100; // Default height
        isDragging = false;
        setDefaultThumbDrawable();
    }

    private void setDefaultThumbDrawable() {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(Color.GRAY);
        shape.setAlpha(100);
        thumbDrawable = shape;
    }

    public void setThumbDrawable(Drawable drawable) {
        if (drawable != null) {
            this.thumbDrawable = drawable;
            invalidate();
        }
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (recyclerView != null && thumbDrawable != null && isThumbVisible) {
            updateThumbPosition();
            thumbDrawable.draw(canvas);
        }
    }

    private void updateThumbPosition() {
        float scrollExtent = recyclerView.computeVerticalScrollExtent();
        float scrollRange = recyclerView.computeVerticalScrollRange();
        float scrollOffset = recyclerView.computeVerticalScrollOffset();

        float thumbY = scrollOffset / (scrollRange - scrollExtent) * (getHeight() - thumbHeight);
        thumbDrawable.setBounds(0, (int)thumbY, getWidth(), (int)(thumbY + thumbHeight));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (recyclerView == null) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                showThumb();
                updateThumbFromTouch(event.getY());
                isDragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    showThumb();
                    updateThumbFromTouch(event.getY());
                    scrollRecyclerView(event.getY());
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                scheduleHideThumb();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void updateThumbFromTouch(float touchY) {
        float scrollFraction = touchY / getHeight();
        float thumbY = scrollFraction * (getHeight() - thumbHeight);
        thumbDrawable.setBounds(0, (int)thumbY, getWidth(), (int)(thumbY + thumbHeight));
        invalidate();
    }

    private void scrollRecyclerView(float touchY) {
        float scrollFraction = touchY / getHeight();
        int targetPosition = (int) (scrollFraction * recyclerView.getAdapter().getItemCount());
        recyclerView.scrollToPosition(targetPosition);
    }
    private void showThumb() {
        isThumbVisible = true;
        hideHandler.removeCallbacks(hideRunnable);
        invalidate();
    }

    private void scheduleHideThumb() {
        hideHandler.postDelayed(hideRunnable, HIDE_DELAY);
    }

    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            isThumbVisible = false;
            invalidate();
        }
    };
}

