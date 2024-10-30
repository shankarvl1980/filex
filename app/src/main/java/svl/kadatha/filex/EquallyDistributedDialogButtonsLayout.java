package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

public class EquallyDistributedDialogButtonsLayout extends ViewGroup {
    private final Context context;
    private final int number_of_buttons;
    private final int screen_width;
    private final int screen_height;
    private int child_count;
    private int margin;
    private LinearLayout.LayoutParams params;

    EquallyDistributedDialogButtonsLayout(Context context, int number_of_buttons, int screen_width, int screen_height) {

        super(context);
        this.context = context;
        this.number_of_buttons = number_of_buttons;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        init();
    }

    EquallyDistributedDialogButtonsLayout(Context context, AttributeSet attr, int number_of_buttons, int screen_width, int screen_height) {
        super(context, attr);
        this.context = context;
        this.number_of_buttons = number_of_buttons;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        init();
    }

    EquallyDistributedDialogButtonsLayout(Context context, android.util.AttributeSet attr, int defStyle, int number_of_buttons, int screen_width, int screen_height) {
        super(context, attr, defStyle);
        this.context = context;
        this.number_of_buttons = number_of_buttons;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        init();
    }

    private void init() {
        LayoutInflater.from(context).inflate(R.layout.dialog_buttons_layout, this, true);
        int dialog_width;
        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            dialog_width = screen_height;
        } else {
            dialog_width = screen_width;
        }

        margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        child_count = Math.min(number_of_buttons, getChildCount());
        int distance_between_buttons = (child_count - 1) * margin;
        int width_for_child = MeasureSpec.makeMeasureSpec((dialog_width - distance_between_buttons) / child_count, MeasureSpec.EXACTLY);
        params = new LinearLayout.LayoutParams(width_for_child, Global.BUTTON_HEIGHT);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width_used = 0, height_used = 0;

        for (int i = 0; i < child_count; ++i) {

            View v = getChildAt(i);
            v.setLayoutParams(params);
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            measureChildWithMargins(v, widthMeasureSpec, width_used, heightMeasureSpec, 0);
            width_used += v.getMeasuredWidth() + lp.getMarginStart() + lp.getMarginEnd();
            height_used = v.getMeasuredHeight();

        }
        setMeasuredDimension(widthMeasureSpec, height_used);
    }


    @Override
    protected void onLayout(boolean p1, int p2, int p3, int p4, int p5) {
        int x = 0, y = 0;
        int child_count = Math.min(number_of_buttons, getChildCount());
        for (int i = 0; i < child_count; ++i) {
            View v = getChildAt(i);
            v.layout(x, y, x + v.getMeasuredWidth(), v.getMeasuredHeight());
            x += v.getMeasuredWidth() + margin;
            v.setBackground(ContextCompat.getDrawable(context, R.drawable.select_dialog_button));
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(context, attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }
}
