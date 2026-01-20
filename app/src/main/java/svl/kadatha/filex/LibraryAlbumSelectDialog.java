package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;


public class LibraryAlbumSelectDialog extends DialogFragment {
    private Context context;
    private String request_code, library_type;
    private Bundle bundle;
    private FrameLayout progress_bar;
    private LibraryAlbumSelectViewModel viewModel;

    public static LibraryAlbumSelectDialog getInstance(String request_code, String library_type) {
        LibraryAlbumSelectDialog libraryAlbumSelectDialog = new LibraryAlbumSelectDialog();
        Bundle bundle = new Bundle();
        bundle.putString("request_code", request_code);
        bundle.putString("library_type", library_type);
        libraryAlbumSelectDialog.setArguments(bundle);
        return libraryAlbumSelectDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle = getArguments();
        request_code = bundle.getString("request_code");
        library_type = bundle.getString("library_type");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library_filter, container, false);
        progress_bar = v.findViewById(R.id.fragment_library_filter_progressbar);
        TextView label_text_view = v.findViewById(R.id.fragment_library_filter_label);
        label_text_view.setText(R.string.select_album);
        RecyclerView library_recyclerview = v.findViewById(R.id.fragment_library_filter_recyclerView);
        library_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        library_recyclerview.setLayoutManager(new LinearLayoutManager(context));

        ViewGroup button_layout = v.findViewById(R.id.fragment_library_filter_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button cancel = button_layout.findViewById(R.id.first_button);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                dismissAllowingStateLoss();
            }
        });

        viewModel = new ViewModelProvider(this).get(LibraryAlbumSelectViewModel.class);
        viewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus.equals(AsyncTaskStatus.STARTED)) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else {
                    progress_bar.setVisibility(View.GONE);
                    library_recyclerview.setAdapter(new LibraryRecyclerViewAdapter());
                }
            }
        });

        if (library_type != null) {
            viewModel.fetchAlbumDirectories(library_type);
        } else {
            progress_bar.setVisibility(View.GONE);
        }
        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_WIDTH);
        } else {
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_HEIGHT);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    static class LibraryDirPOJO {
        private final String path;
        private final String name;
        private final boolean fromSDCard;

        public LibraryDirPOJO(String path, String name, boolean fromSDCard) {
            this.path = path;
            this.name = name;
            this.fromSDCard = fromSDCard;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public boolean isFromSDCard() {
            return fromSDCard;
        }
    }

    public static class LibraryAlbumRecyclerViewLayout extends ViewGroup {
        private final Context context;
        public int itemWidth, itemHeight;
        private ImageView imageView;
        private TextView albumNameTextView, pathTextView;
        private int imageview_dimension;
        private boolean isIconHeightMore;

        LibraryAlbumRecyclerViewLayout(Context context) {
            super(context);
            this.context = context;
            init();
        }

        private void init() {
            View view = LayoutInflater.from(context).inflate(R.layout.album_recyclerview_layout, this, true);
            albumNameTextView = view.findViewById(R.id.album_name);
            pathTextView = view.findViewById(R.id.album_path);
            imageView = view.findViewById(R.id.album_image_dir);

            setBackground(ContextCompat.getDrawable(context, R.drawable.select_detail_recyclerview));
            imageview_dimension = Global.THIRTY_DP;
            imageView.getLayoutParams().width = imageview_dimension;
            imageView.getLayoutParams().height = imageview_dimension;

            itemWidth = Global.DIALOG_WIDTH;
            int pad = getResources().getDimensionPixelSize(R.dimen.layout_margin);
            setPaddingRelative(pad, 0, pad, 0);
            setBackground(ContextCompat.getDrawable(context, R.drawable.select_drawer_storage_list));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int iconHeight;
            int maxHeight = 0;
            int usedWidth;

            if (Global.IS_TABLET) {
                usedWidth = Global.EIGHT_DP;
            } else {
                usedWidth = Global.SIX_DP;
            }
            measureChildWithMargins(imageView, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);

            usedWidth += imageview_dimension;
            iconHeight = imageview_dimension;


            measureChildWithMargins(albumNameTextView, widthMeasureSpec, usedWidth + (Global.EIGHT_DP * 2), heightMeasureSpec, 0);
            measureChildWithMargins(pathTextView, widthMeasureSpec, usedWidth + (Global.EIGHT_DP * 2), heightMeasureSpec, 0);
            maxHeight += albumNameTextView.getMeasuredHeight();
            maxHeight += pathTextView.getMeasuredHeight();

            isIconHeightMore = (iconHeight * 2 > maxHeight);
            maxHeight = Math.max(iconHeight, maxHeight);

            maxHeight += Global.TEN_DP;
            itemHeight = maxHeight;
            setMeasuredDimension(widthMeasureSpec, maxHeight);
        }

        @Override
        protected void onLayout(boolean p1, int l, int t, int r, int b) {
            int x = Global.EIGHT_DP;
            int y;
            int margin_offset_icon, top_offset;

            top_offset = (itemHeight - albumNameTextView.getMeasuredHeight() - pathTextView.getMeasuredHeight()) / 2;
            int d = isIconHeightMore ? (itemHeight - imageview_dimension) / 2 : top_offset + Global.SIX_DP;

            View v = imageView;
            int imageMeasuredWidth = v.getMeasuredWidth();
            int imageMeasuredHeight = v.getMeasuredHeight();
            int recyclerview_padding;
            if (Global.IS_TABLET) {
                recyclerview_padding = Global.TWELVE_DP;
            } else {
                recyclerview_padding = Global.EIGHT_DP;
            }
            margin_offset_icon = itemWidth - imageMeasuredWidth - Global.EIGHT_DP - Global.EIGHT_DP - recyclerview_padding;
            v.layout(margin_offset_icon, d, margin_offset_icon + imageMeasuredWidth, d + imageMeasuredHeight);

            // Then lay out the overlay_fileimageview at the bottom-right corner of fileimageview

            v = albumNameTextView;
            int measuredHeight = v.getMeasuredHeight();
            int measuredWidth = v.getMeasuredWidth();
            y = top_offset;
            v.layout(x, y, x + measuredWidth, y + measuredHeight);
            y += measuredHeight;

            v = pathTextView;
            measuredHeight = v.getMeasuredHeight();
            measuredWidth = v.getMeasuredWidth();
            v.layout(x, y, x + measuredWidth, y + measuredHeight);
        }

        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return p instanceof MarginLayoutParams;
        }

        /**
         * @return A set of default layout parameters when given a child with no layout parameters.
         */
        @Override
        protected LayoutParams generateDefaultLayoutParams() {
            return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }

        /**
         * @return A set of layout parameters created from attributes passed in XML.
         */
        @Override
        public LayoutParams generateLayoutParams(AttributeSet attrs) {
            return new MarginLayoutParams(context, attrs);
        }

        /**
         * Called when {@link #checkLayoutParams(LayoutParams)} fails.
         *
         * @return A set of valid layout parameters for this ViewGroup that copies appropriate/valid
         * attributes from the supplied, not-so-good-parameters.
         */
        @Override
        protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return generateDefaultLayoutParams();
        }

        public void setData(LibraryDirPOJO libraryDirPOJO) {
            if (libraryDirPOJO.getPath().equals("All")) {
                imageView.setImageDrawable(null);
            } else if (libraryDirPOJO.isFromSDCard()) {
                imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.sdcard_icon));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
            }

            albumNameTextView.setText(libraryDirPOJO.getName());
            pathTextView.setText(libraryDirPOJO.getPath());
        }
    }

    private class LibraryRecyclerViewAdapter extends RecyclerView.Adapter<LibraryRecyclerViewAdapter.VH> {

        @Override
        public LibraryRecyclerViewAdapter.VH onCreateViewHolder(ViewGroup p1, int p2) {
            return new VH(new LibraryAlbumRecyclerViewLayout(context));
        }

        @Override
        public void onBindViewHolder(LibraryRecyclerViewAdapter.VH p1, int p2) {
            LibraryAlbumSelectDialog.LibraryDirPOJO libraryDirPOJO = viewModel.libraryDirPOJOS.get(p2);
            p1.v.setData(libraryDirPOJO);
        }

        @Override
        public int getItemCount() {
            return viewModel.libraryDirPOJOS.size();
        }

        private class VH extends RecyclerView.ViewHolder {
            final LibraryAlbumRecyclerViewLayout v;
            final ImageView album_dir_image;
            final TextView album_name_tv, album_path_tv;
            int pos;

            VH(LibraryAlbumRecyclerViewLayout vi) {
                super(vi);
                v = vi;
                album_dir_image = v.findViewById(R.id.album_image_dir);
                album_name_tv = v.findViewById(R.id.album_name);
                album_path_tv = v.findViewById(R.id.album_path);

                vi.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p1) {
                        pos = getBindingAdapterPosition();
                        String parent_file_path = null;
                        if (pos != 0) {
                            parent_file_path = viewModel.libraryDirPOJOS.get(pos).getPath();
                            bundle.putString("parent_file_name", new File(parent_file_path).getName());
                        }


                        bundle.putString("parent_file_path", parent_file_path);
                        getParentFragmentManager().setFragmentResult(request_code, bundle);
                        dismissAllowingStateLoss();
                    }
                });
            }
        }
    }
}
