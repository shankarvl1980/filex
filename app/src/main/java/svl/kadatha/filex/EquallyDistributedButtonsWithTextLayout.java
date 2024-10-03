package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;

public class EquallyDistributedButtonsWithTextLayout extends ViewGroup {
    private final Context context;
    private final int screen_width;
    private final int screen_height;
    private int child_count, toppadding;

    EquallyDistributedButtonsWithTextLayout(Context context, int child_count, int screen_width, int screen_height) {
        super(context);
        this.context = context;
        this.child_count = child_count;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        init();
    }

    EquallyDistributedButtonsWithTextLayout(Context context, AttributeSet attr, int child_count, int screen_width, int screen_height) {
        super(context, attr);
        this.context = context;
        this.child_count = child_count;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        init();
    }

    EquallyDistributedButtonsWithTextLayout(Context context, AttributeSet attr, int defStyle, int child_count, int screen_width, int screen_height) {
        super(context, attr, defStyle);
        this.context = context;
        this.child_count = child_count;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        init();
    }

    private void init() {
        setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT));
        LayoutInflater.from(context).inflate(R.layout.toolbar_buttons_layout, this, true);

        int toolbar_width;
        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            toolbar_width = screen_height;
        } else {
            toolbar_width = screen_width;
        }

        int icon_dimension = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        toppadding = (Global.ACTION_BAR_HEIGHT - icon_dimension) / 4;
        child_count = Math.min(child_count, getChildCount());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: Implement this method
        int widthUsed = 0, maxHeight = 0, heightUsed = 0;
        int visibleChildCount = 0;

        // First, count the visible children
        for (int i = 0; i < child_count; ++i) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                visibleChildCount++;
            }
        }

        // Calculate width per child
        int toolbar_width = (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) ? screen_height : screen_width;
        int w = toolbar_width / visibleChildCount;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, Global.ACTION_BAR_HEIGHT);

        for (int i = 0; i < child_count; ++i) {
            Button child = (Button) getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                child.setLayoutParams(params);
                child.setPadding(0, toppadding, 0, toppadding);
                child.setTextSize((float) Global.ACTION_BAR_HEIGHT_IN_DP / 4);
                measureChildWithMargins(child, widthMeasureSpec, widthUsed, Global.ACTION_BAR_HEIGHT, heightUsed);
                widthUsed += child.getMeasuredWidth() + child.getPaddingStart() + child.getPaddingEnd();
                heightUsed = child.getMeasuredHeight() + child.getPaddingTop() + child.getPaddingBottom();
                maxHeight = Math.max(maxHeight, heightUsed);
            }
        }
        setMeasuredDimension(widthMeasureSpec, Global.ACTION_BAR_HEIGHT);
    }

    @Override
    protected void onLayout(boolean p1, int p2, int p3, int p4, int p5) {
        int x = 0, y = 0;

        for (int i = 0; i < child_count; ++i) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE){
                child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
                x += child.getMeasuredWidth();
                child.setBackground(ContextCompat.getDrawable(context, R.drawable.select_icon_selection));
            }
        }
    }

    public void setResourceImageDrawables(@IdRes int[] drawables, String[] titles) {
        for (int i = 0; i < child_count; ++i) {
            Button child = (Button) getChildAt(i);
            child.setCompoundDrawablesWithIntrinsicBounds(0, drawables[i], 0, 0);
            child.setText(titles[i]);
        }
    }

    public void setResourceImageDrawables(@IdRes int[] drawables, String[] titles, int color) {
        for (int i = 0; i < child_count; ++i) {
            Button child = (Button) getChildAt(i);
            child.setTextColor(color);
            child.setCompoundDrawablesWithIntrinsicBounds(0, drawables[i], 0, 0);
            child.setText(titles[i]);
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(context, attrs);
    }
}
