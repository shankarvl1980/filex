package svl.kadatha.filex;

import android.content.Context;
import android.view.ViewGroup;

public class DetailRecyclerViewDuplicateAdapterList extends DetailRecyclerViewAdapter {

    private final Context context;
    private final boolean show_file_path;
    private final DetailFragment df;

    DetailRecyclerViewDuplicateAdapterList(Context context, boolean show_file_path, DetailFragment df) {
        super(context);
        this.context = context;
        this.show_file_path = show_file_path;
        this.df = df;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
        return new ViewHolder(new RecyclerViewLayoutList(context, show_file_path));
    }

    @Override
    public void onBindViewHolder(DetailRecyclerViewAdapter.ViewHolder p1, int p2) {
        FilePOJO file = df.filePOJO_list.get(p2);
        boolean selected = df.viewModel.mselecteditems.containsKey(p2);
        p1.view.setData(file, selected);
        p1.view.setWhetherExternal(file);
        p1.view.setSelected(selected);
        String next_file_name = "";
        String next_file_checksum = "";

        try {
            next_file_name = df.filePOJO_list.get(p2 + 1).getName();
            next_file_checksum = df.filePOJO_list.get(p2 + 1).getChecksum();
        } catch (IndexOutOfBoundsException e) {

        }
        boolean whetherToShowDivider = !(next_file_name.equals(file.getName()) && next_file_checksum.equals(file.getChecksum()));
        p1.view.setDivider(whetherToShowDivider);

    }

}
