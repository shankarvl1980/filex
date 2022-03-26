package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class PdfViewFragment extends Fragment implements PdfViewFragment_view_container.PdfPageLoadListener {

    private Context context;
    public TouchImageView touchImageView;
    private OnClickListener onClickListener;
    public boolean page_set;

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
        touchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onClickListener!=null)
                {
                    onClickListener.onClickView();
                }
            }
        });
        return v;
        
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        page_set=false;
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v!=null && v.getViewTreeObserver().isAlive()) {
            v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    // Let our parent know we are laid out
                    if ( getActivity() instanceof PdfFragmentShownListener ) {
                        ((PdfFragmentShownListener) getActivity()).onFragmentShown(PdfViewFragment.this);
                    }
                }
            });
        }
    }

    @Override
    public void onRetrievePdfPage(Bitmap bitmap) {
        GlideApp.with(context).load(bitmap).placeholder(R.drawable.pdf_water_icon).error(R.drawable.pdf_water_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(touchImageView);
        page_set=true;
    }

    interface OnClickListener
    {
        void onClickView();
    }

    public void setOnClickListener(OnClickListener listener)
    {
        onClickListener=listener;
    }

    interface PdfFragmentShownListener
    {
        void onFragmentShown(PdfViewFragment pvf);
    }
}
