package svl.kadatha.filex;

import android.content.Context;
import android.view.ViewGroup;

public class DetailRecyclerViewAdapterGrid extends DetailRecyclerViewAdapter {

    private final Context context;
    private final boolean show_file_path;

    DetailRecyclerViewAdapterGrid(Context context, DetailFragment df, boolean show_file_path) {
        super(context, df);
        this.context = context;
        this.show_file_path = show_file_path;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
        return new ViewHolder(new RecyclerViewLayoutGrid(context, show_file_path));
    }
}
