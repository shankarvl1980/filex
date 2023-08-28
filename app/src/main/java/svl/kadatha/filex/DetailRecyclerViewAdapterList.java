package svl.kadatha.filex;

import android.content.Context;
import android.view.ViewGroup;

public class DetailRecyclerViewAdapterList extends DetailRecyclerViewAdapter{

    private final Context context;
    private final boolean show_file_path;

    DetailRecyclerViewAdapterList(Context context,boolean show_file_path) {
        super(context);
        this.context=context;
        this.show_file_path=show_file_path;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
        return new ViewHolder(new RecyclerViewLayoutList(context,show_file_path));
    }
}
