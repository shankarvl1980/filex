package svl.kadatha.filex;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public abstract class DetailRecyclerViewAdapter extends RecyclerView.Adapter<DetailRecyclerViewAdapter.ViewHolder> implements Filterable {

    private final DetailFragment df;

    private CardViewClickListener cardViewClickListener;

    DetailRecyclerViewAdapter(Context context, DetailFragment df) {
        this.df = df;
        if (df.detailFragmentListener != null) {
            df.detailFragmentListener.setCurrentDirText(df.file_click_selected_name);
            df.detailFragmentListener.setFileNumberView(df.viewModel.mselecteditems.size() + "/" + df.file_list_size);
            if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                if(df.file_click_selected_name.equals(DetailFragment.SEARCH_RESULT)){
                    df.detailFragmentListener.setCurrentDirText(df.search_file_name);
                }
                df.detailFragmentListener.enableParentDirImageButton(false);
            } else if (df.fileObjectType == FileObjectType.FILE_TYPE) {
                File f = new File(df.fileclickselected);
                File parent_file = f.getParentFile();
                if (parent_file != null) {
                    df.detailFragmentListener.enableParentDirImageButton(true);
                } else {
                    df.detailFragmentListener.setCurrentDirText(context.getString(R.string.root_directory));
                    df.detailFragmentListener.enableParentDirImageButton(false);
                }
            } else {
                String parent_path = df.fileclickselected;
                if (parent_path.equals("/")) {
                    df.detailFragmentListener.setCurrentDirText(df.file_click_selected_name);
                    df.detailFragmentListener.enableParentDirImageButton(false);
                } else {
                    df.detailFragmentListener.enableParentDirImageButton(true);
                }
            }
        }
    }

    @Override
    public abstract ViewHolder onCreateViewHolder(ViewGroup p1, int p2);

    @Override
    public void onBindViewHolder(DetailRecyclerViewAdapter.ViewHolder p1, int p2) {
        FilePOJO file = df.filePOJO_list.get(p2);
        boolean selected = df.viewModel.mselecteditems.containsKey(p2);
        p1.view.setData(file, selected);
        p1.view.setSelected(selected);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults filterResults) {
                df.filePOJO_list = new ArrayList<>();
                if ((constraint == null || constraint.length() == 0) && df.viewModel.library_filter_path != null) {
                    for (int i = 0; i < df.totalFilePOJO_list_Size; ++i) {
                        FilePOJO filePOJO = df.totalFilePOJO_list.get(i);
                        if (new File(filePOJO.getPath()).getParent().equals(df.viewModel.library_filter_path)) {
                            df.filePOJO_list.add(filePOJO);
                        }
                    }
                } else if ((constraint == null || constraint.length() == 0) && df.viewModel.library_filter_path == null) {
                    df.filePOJO_list = df.totalFilePOJO_list;
                } else if (df.viewModel.library_filter_path == null) {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (int i = 0; i < df.totalFilePOJO_list_Size; ++i) {
                        FilePOJO filePOJO = df.totalFilePOJO_list.get(i);
                        if (filePOJO.getLowerName().contains(pattern)) {
                            df.filePOJO_list.add(filePOJO);
                        }
                    }
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (int i = 0; i < df.totalFilePOJO_list_Size; ++i) {
                        FilePOJO filePOJO = df.totalFilePOJO_list.get(i);
                        if (filePOJO.getLowerName().contains(pattern) && new File(filePOJO.getPath()).getParent().equals(df.viewModel.library_filter_path)) {
                            df.filePOJO_list.add(filePOJO);
                        }
                    }
                }

                df.file_list_size = df.filePOJO_list.size();
                if (!df.viewModel.mselecteditems.isEmpty()) {
                    deselectAll();
                } else {
                    notifyDataSetChanged();
                }
                if (df.file_list_size > 0) {
                    df.recyclerView.setVisibility(View.VISIBLE);
                    df.folder_empty.setVisibility(View.GONE);
                }
                if (df.detailFragmentListener != null) {
                    df.detailFragmentListener.setFileNumberView(df.viewModel.mselecteditems.size() + "/" + df.file_list_size);
                }
            }
        };
    }

    @Override
    public int getItemCount() {
        return df.filePOJO_list.size();
    }

    public void updateList(List<FilePOJO> newFilePOJOList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DetailRecyclerViewAdapterDiffUtil(df.filePOJO_list, newFilePOJOList));
        diffResult.dispatchUpdatesTo(this);
    }

    public void clear_cache_and_refresh(String file_path, FileObjectType fileObjectType) {
        if (df != null) {
            df.viewModel.mselecteditems = new IndexedLinkedHashMap<>();
            if (df.detailFragmentListener != null) {
                df.detailFragmentListener.clearCache(file_path, fileObjectType);
            }

            df.modification_observed = true;
        }
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    public void selectAll() {
        df.viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        int size = df.filePOJO_list.size();

        for (int i = 0; i < size; ++i) {
            df.viewModel.mselecteditems.put(i, df.filePOJO_list.get(i).getPath());
        }

        int s = df.viewModel.mselecteditems.size();

        if (df.detailFragmentListener != null) {
            df.detailFragmentListener.setFileNumberView(s + "/" + df.file_list_size);
            df.detailFragmentListener.onLongClickItem(s);
        }
        notifyDataSetChanged();
    }

    public void selectInterval() {
        int size = df.viewModel.mselecteditems.size();
        if (size < 2) {
            return;
        }
        int last_key = df.viewModel.mselecteditems.getKeyAtIndex(size - 1);
        int previous_to_last_key = df.viewModel.mselecteditems.getKeyAtIndex(size - 2);
        if (last_key == previous_to_last_key) {
            return;
        }
        int min = Math.min(last_key, previous_to_last_key);
        int max = Math.max(last_key, previous_to_last_key);
        if (max - min == 1) {
            return;
        }
        for (int i = min + 1; i < max; ++i) {
            df.viewModel.mselecteditems.put(i, df.filePOJO_list.get(i).getPath());
        }
        int s = df.viewModel.mselecteditems.size();

        if (df.detailFragmentListener != null) {
            df.detailFragmentListener.setFileNumberView(s + "/" + df.file_list_size);
            df.detailFragmentListener.onLongClickItem(s);
        }
        notifyDataSetChanged();
    }

    public void deselectAll() {
        if (df.detailFragmentListener != null) {
            df.detailFragmentListener.onDeselectAll(df);
        }
    }

    public void setCardViewClickListener(CardViewClickListener listener) {
        this.cardViewClickListener = listener;
    }


    interface CardViewClickListener {
        void onClick(FilePOJO filePOJO);

        void onLongClick(FilePOJO filePOJO, int size);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnLongClickListener {
        final RecyclerViewLayout view;
        int pos;

        ViewHolder(RecyclerViewLayout view) {
            super(view);
            this.view = view;
            this.view.setOnClickListener(this);
            this.view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View p1) {
            pos = getBindingAdapterPosition();
            int size = df.viewModel.mselecteditems.size();
            if (size > 0) {
                longClickMethod(p1, size);
            } else {
                if (cardViewClickListener != null) {
                    FilePOJO filePOJO = df.filePOJO_list.get(pos);
                    cardViewClickListener.onClick(filePOJO);
                }
            }
        }

        private void longClickMethod(View v, int size) {
            pos = getBindingAdapterPosition();
            if (df.viewModel.mselecteditems.containsKey(pos)) {
                df.viewModel.mselecteditems.remove(pos);
                v.setSelected(false);
                ((RecyclerViewLayout) v).set_selected(false);
                --size;

                if (cardViewClickListener != null) {
                    FilePOJO filePOJO = df.filePOJO_list.get(pos);
                    cardViewClickListener.onLongClick(filePOJO, size);
                }
            } else {
                df.viewModel.mselecteditems.put(pos, df.filePOJO_list.get(pos).getPath());
                v.setSelected(true);
                ((RecyclerViewLayout) v).set_selected(true);
                ++size;

                if (cardViewClickListener != null) {
                    FilePOJO filePOJO = df.filePOJO_list.get(pos);
                    cardViewClickListener.onLongClick(filePOJO, size);
                }
            }
            if (df.detailFragmentListener != null) {
                df.detailFragmentListener.setFileNumberView(size + "/" + df.file_list_size);
            }
        }

        @Override
        public boolean onLongClick(View p1) {
            longClickMethod(p1, df.viewModel.mselecteditems.size());
            return true;
        }
    }
}
