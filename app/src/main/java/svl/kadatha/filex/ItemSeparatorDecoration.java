package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ItemSeparatorDecoration extends RecyclerView.ItemDecoration {
    private final int spacingPx;
    private int spanCount = 1;

    public ItemSeparatorDecoration(Context context, int spacingDp, RecyclerView recyclerView) {
        this.spacingPx = dpToPx(context, spacingDp);
        setupLayoutManager(recyclerView.getLayoutManager());
    }

    private int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    private void setupLayoutManager(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            spanCount = gridLayoutManager.getSpanCount();
        } else {
            spanCount = 1; // Default span count for non-grid layouts
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int column = position % spanCount;

        outRect.left = column * spacingPx / spanCount;
        outRect.right = spacingPx - (column + 1) * spacingPx / spanCount;
        if (position >= spanCount) {
            outRect.top = spacingPx;
        }
    }
}

//
//        if (includeEdge) {
//outRect.left = spacingPx - column * spacingPx / spanCount;
//outRect.right = (column + 1) * spacingPx / spanCount;
//            if (position < spanCount) {
//outRect.top = spacingPx;
//            }
//outRect.bottom = spacingPx;
//        } else {
//outRect.left = column * spacingPx / spanCount;
//outRect.right = spacingPx - (column + 1) * spacingPx / spanCount;
//            if (position >= spanCount) {
//outRect.top = spacingPx;
//            }
//                    }