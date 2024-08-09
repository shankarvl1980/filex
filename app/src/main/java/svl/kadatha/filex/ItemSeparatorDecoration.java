package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;


import android.util.TypedValue;

import androidx.recyclerview.widget.GridLayoutManager;

public class ItemSeparatorDecoration extends RecyclerView.ItemDecoration {
    private final int spacingPx;
    private final boolean includeEdge;
    private int spanCount = 1;
    private boolean isGrid = false;

    public ItemSeparatorDecoration(Context context, int spacingDp, boolean includeEdge) {
        this.spacingPx = dpToPx(context, spacingDp);
        this.includeEdge = includeEdge;
    }

    private int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            if (!isGrid) {
                isGrid = true;
                spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
            }
        } else {
            isGrid = false;
            spanCount = 1;
        }

        int position = parent.getChildAdapterPosition(view);
        int column = position % spanCount;

        if (includeEdge) {
            outRect.left = spacingPx - column * spacingPx / spanCount;
            outRect.right = (column + 1) * spacingPx / spanCount;
            if (position < spanCount) {
                outRect.top = spacingPx;
            }
            outRect.bottom = spacingPx;
        } else {
            outRect.left = column * spacingPx / spanCount;
            outRect.right = spacingPx - (column + 1) * spacingPx / spanCount;
            if (position >= spanCount) {
                outRect.top = spacingPx;
            }
        }
    }
}

