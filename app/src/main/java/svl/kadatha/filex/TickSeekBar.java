package svl.kadatha.filex;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;

public class TickSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {

    private int tickCount = 3;
    private Paint tickPaint;
    private Paint backgroundPaint;
    private int seekBarColor = Color.GRAY;
    private float seekBarLineWidth = 4f;
    private int tickColor = Color.BLACK;
    private float tickWidth = 2f;

    public TickSeekBar(Context context) {
        super(context);
        init(null, 0);
    }

    public TickSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public TickSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TickSeekBar, defStyle, 0);
            tickCount = a.getInteger(R.styleable.TickSeekBar_tickCount, 3);
            seekBarColor = a.getColor(R.styleable.TickSeekBar_seekBarColor, Color.GRAY);
            seekBarLineWidth = a.getDimension(R.styleable.TickSeekBar_seekBarLineWidth, 4f);
            tickColor = a.getColor(R.styleable.TickSeekBar_tickColor, Color.BLACK);
            tickWidth = a.getDimension(R.styleable.TickSeekBar_tickWidth, 2f);
            a.recycle();
        }

        tickPaint.setColor(tickColor);
        tickPaint.setStrokeWidth(tickWidth);

        backgroundPaint.setColor(seekBarColor);
        backgroundPaint.setStrokeWidth(seekBarLineWidth);

        // Set a custom drawable as the progress drawable
        setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the background line
        canvas.drawLine(getPaddingLeft(), getHeight() / 2f,
                getWidth() - getPaddingRight(), getHeight() / 2f, backgroundPaint);

        // Draw the ticks
        drawTicks(canvas);

        // Draw the thumb
        super.onDraw(canvas);
    }

    private void drawTicks(Canvas canvas) {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight();

        for (int i = 0; i < tickCount; i++) {
            float x = getPaddingLeft() + (width * i / (float)(tickCount - 1));
            canvas.drawLine(x, height * 0.25f, x, height * 0.75f, tickPaint);
        }
    }

    public void setTickCount(int count) {
        tickCount = count;
        invalidate();
    }

    public void setSeekBarColor(int color) {
        seekBarColor = color;
        backgroundPaint.setColor(seekBarColor);
        invalidate();
    }

    public void setSeekBarLineWidth(float width) {
        seekBarLineWidth = width;
        backgroundPaint.setStrokeWidth(seekBarLineWidth);
        invalidate();
    }

    public void setTickColor(int color) {
        tickColor = color;
        tickPaint.setColor(tickColor);
        invalidate();
    }

    public void setTickWidth(float width) {
        tickWidth = width;
        tickPaint.setStrokeWidth(tickWidth);
        invalidate();
    }
}