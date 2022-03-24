package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class PdfViewFragment extends Fragment implements PdfViewFragment_view_container.PdfPageLoadListener {

    private Context context;
    public TouchImageView touchImageView;
    private OnCreateViewListener onCreateViewListener;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.pdf_viewpager_layout,container,false);
        touchImageView=v.findViewById(R.id.pdf_viewpager_layout_imageview);
        touchImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        touchImageView.setMaxZoom(6);
        if(onCreateViewListener!=null)
        {
            onCreateViewListener.onCreateView();
        }

        return v;
        
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onRetrievePdfPage(Bitmap bitmap) {
        GlideApp.with(context).load(bitmap).placeholder(R.drawable.pdf_file_icon).error(R.drawable.pdf_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(touchImageView);
    }

    interface OnCreateViewListener
    {
        void onCreateView();
    }

    public void setOnCreateViewListener(OnCreateViewListener listener)
    {
        onCreateViewListener=listener;
    }
}
