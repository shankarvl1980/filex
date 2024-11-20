package svl.kadatha.filex;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ObservableScrollView extends ScrollView {
    private ScrollViewListener scrollViewListener;

    public ObservableScrollView(Context context) {
        super(context);
    }

    public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (scrollViewListener != null) {
            scrollViewListener.onScrollChange(this, l, t, oldl, oldt);
        }
    }

    public void setScrollViewListener(ScrollViewListener listener) {
        scrollViewListener = listener;
    }

    public interface ScrollViewListener {
        void onScrollChange(ObservableScrollView scrollview, int x, int y, int oldx, int oldy);
    }


}
