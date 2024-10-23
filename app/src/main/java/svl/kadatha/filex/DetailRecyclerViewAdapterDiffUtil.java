package svl.kadatha.filex;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class DetailRecyclerViewAdapterDiffUtil extends DiffUtil.Callback {
    final List<FilePOJO> old_list;
    final List<FilePOJO> new_list;

    DetailRecyclerViewAdapterDiffUtil(List<FilePOJO> old_l, List<FilePOJO> new_l) {
        old_list = old_l;
        new_list = new_l;
    }

    @Override
    public int getOldListSize() {
        return old_list != null ? old_list.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return new_list != null ? new_list.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int p1, int p2) {
        return old_list.get(p1).getName().equals(new_list.get(p2).getName());
    }

    @Override
    public boolean areContentsTheSame(int p1, int p2) {
        FilePOJO oldFilePOJO = old_list.get(p1);
        FilePOJO newFilePOJO = new_list.get(p2);
        if (oldFilePOJO.getIsDirectory()) {
            return oldFilePOJO.getSize().equals(newFilePOJO.getSize());
        } else {
            return oldFilePOJO.getSizeLong() == newFilePOJO.getSizeLong();
        }
    }

    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
