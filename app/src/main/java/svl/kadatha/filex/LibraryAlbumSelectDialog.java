package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;


public class LibraryAlbumSelectDialog extends DialogFragment
{
    private Context context;
    private String request_code,library_type;
    private Bundle bundle;
    private FrameLayout progress_bar;
    private LibraryAlbumSelectViewModel viewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle = getArguments();
        request_code=bundle.getString("request_code");
        library_type= bundle.getString("library_type");

    }

    public static LibraryAlbumSelectDialog getInstance(String request_code,String library_type)
    {
        LibraryAlbumSelectDialog libraryAlbumSelectDialog=new LibraryAlbumSelectDialog();
        Bundle bundle=new Bundle();
        bundle.putString("request_code",request_code);
        bundle.putString("library_type",library_type);
        libraryAlbumSelectDialog.setArguments(bundle);
        return libraryAlbumSelectDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method
        View v=inflater.inflate(R.layout.fragment_library_filter,container,false);
        progress_bar=v.findViewById(R.id.fragment_library_filter_progressbar);
        TextView label_text_view = v.findViewById(R.id.fragment_library_filter_label);
        label_text_view.setText(R.string.select_album);
        RecyclerView library_recyclerview = v.findViewById(R.id.fragment_library_filter_recyclerView);
        library_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        library_recyclerview.setLayoutManager(new LinearLayoutManager(context));

        ViewGroup button_layout = v.findViewById(R.id.fragment_library_filter_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button cancel = button_layout.findViewById(R.id.first_button);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View p1)
            {
                dismissAllowingStateLoss();
            }
        });

        viewModel=new ViewModelProvider(this).get(LibraryAlbumSelectViewModel.class);
        viewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus.equals(AsyncTaskStatus.STARTED))
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else
                {
                    progress_bar.setVisibility(View.GONE);
                    library_recyclerview.setAdapter(new LibraryRecyclerViewAdapter());
                }
            }
        });

        if(library_type!=null)
        {
            viewModel.fetchAlbumDirectories(library_type);
        }
        else
        {
            progress_bar.setVisibility(View.GONE);
        }

        return v;
    }


    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
        {
            window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);
        }
        else
        {
            window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_HEIGHT);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private class LibraryRecyclerViewAdapter extends RecyclerView.Adapter<LibraryRecyclerViewAdapter.VH>
    {

        @Override
        public LibraryRecyclerViewAdapter.VH onCreateViewHolder(ViewGroup p1, int p2)
        {
            // TODO: Implement this method
            View v=LayoutInflater.from(context).inflate(R.layout.album_recyclerview_layout,p1,false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(LibraryRecyclerViewAdapter.VH p1, int p2)
        {
            // TODO: Implement this method
            if(viewModel.libraryDirPOJOS.get(p2).getPath().equals("All"))
            {
                p1.album_dir_image.setImageDrawable(null);
            }
            else if(viewModel.libraryDirPOJOS.get(p2).isFromSDCard())
            {
                p1.album_dir_image.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
            }
            else
            {
                p1.album_dir_image.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.device_icon));
            }

            p1.album_name_tv.setText(viewModel.libraryDirPOJOS.get(p2).getName());
            p1.album_path_tv.setText(context.getString(R.string.path)+" "+viewModel.libraryDirPOJOS.get(p2).getPath());
        }

        @Override
        public int getItemCount()
        {
            // TODO: Implement this method
            return viewModel.libraryDirPOJOS.size();
        }


        private class VH extends RecyclerView.ViewHolder
        {
            final View v;
            final ImageView album_dir_image;
            final TextView album_name_tv, album_path_tv;
            int pos;
            VH(View vi)
            {
                super(vi);
                v=vi;
                album_dir_image=v.findViewById(R.id.album_image_dir);
                album_name_tv=v.findViewById(R.id.album_name);
                album_path_tv=v.findViewById(R.id.album_path);

                vi.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View p1)
                    {
                        pos=getBindingAdapterPosition();
                        String parent_file_path=null;
                        if(pos!=0)
                        {
                            parent_file_path=viewModel.libraryDirPOJOS.get(pos).getPath();
                            bundle.putString("parent_file_name",new File(parent_file_path).getName());
                        }


                        bundle.putString("parent_file_path",parent_file_path);
                        ((AppCompatActivity)context).getSupportFragmentManager().setFragmentResult(request_code,bundle);
                        dismissAllowingStateLoss();
                    }
                });
            }
        }

    }

    static class LibraryDirPOJO
    {
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

}
