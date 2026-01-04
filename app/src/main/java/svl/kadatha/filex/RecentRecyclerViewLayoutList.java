package svl.kadatha.filex;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;

public class RecentRecyclerViewLayoutList extends ViewGroup {
    public final int itemWidth;
    private final Context context;
    public int itemHeight;
    public ImageView fileimageview, play_overlay_imageview, pdf_overlay_imageview;
    public TextView filenametextview;
    private int imageview_dimension;
    private boolean isIconHeightMore;

    RecentRecyclerViewLayoutList(Context context, int itemWidth) {
        super(context);
        this.context = context;
        this.itemWidth = itemWidth;
        init();
    }

    private void init() {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_dir_recyclerview_layout, this, true);
        fileimageview = view.findViewById(R.id.recent_image_storage_dir);
        play_overlay_imageview = view.findViewById(R.id.recent_play_overlay_image_storage_dir);
        pdf_overlay_imageview = view.findViewById(R.id.recent_pdf_overlay_image_storage_dir);
        filenametextview = view.findViewById(R.id.recent_text_storage_dir_name);

        int overlay_image_dimension;
        imageview_dimension = Global.THIRTY_FOUR_DP;

        overlay_image_dimension = imageview_dimension / 2 - Global.TWO_DP;

        setBackground(ContextCompat.getDrawable(context, R.drawable.select_detail_recyclerview));

        play_overlay_imageview.getLayoutParams().width = overlay_image_dimension;
        play_overlay_imageview.getLayoutParams().height = overlay_image_dimension;

        pdf_overlay_imageview.getLayoutParams().width = overlay_image_dimension;
        pdf_overlay_imageview.getLayoutParams().height = overlay_image_dimension;

        int pad = getResources().getDimensionPixelSize(R.dimen.layout_margin);
        setPaddingRelative(pad, 0, pad, 0);
        setBackground(ContextCompat.getDrawable(context, R.drawable.select_drawer_storage_list));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int iconHeight;
        int maxHeight = 0;

        int usedWidth;
        if(Global.IS_TABLET){
            usedWidth =Global.EIGHT_DP;
        } else{
            usedWidth = Global.SIX_DP;
        }
        measureChildWithMargins(fileimageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        measureChildWithMargins(play_overlay_imageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        measureChildWithMargins(pdf_overlay_imageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);

        usedWidth += imageview_dimension + Global.FOUR_DP;
        iconHeight = Global.THIRTY_FOUR_DP;

        measureChildWithMargins(filenametextview, widthMeasureSpec, usedWidth + (Global.FOUR_DP * 2), heightMeasureSpec, 0);
        maxHeight += filenametextview.getMeasuredHeight();

        if (iconHeight * 2 > maxHeight) {
            isIconHeightMore = true;
        }
        maxHeight = Math.max(iconHeight, maxHeight);
        maxHeight = Math.max(maxHeight, Global.FORTY_DP);

        maxHeight += Global.TEN_DP;
        itemHeight = maxHeight;
        setMeasuredDimension(widthMeasureSpec, maxHeight);
    }

    @Override
    protected void onLayout(boolean p1, int l, int t, int r, int b) {
        int x = Global.EIGHT_DP;
        int y;
        int top_offset;

        top_offset = (itemHeight - filenametextview.getMeasuredHeight()) / 2;
        int d = isIconHeightMore ? (itemHeight - imageview_dimension) / 2 : top_offset + Global.EIGHT_DP;

        View v = fileimageview;
        int fileMeasuredWidth = v.getMeasuredWidth();
        int fileMeasuredHeight = v.getMeasuredHeight();
        v.layout(x, d, x + fileMeasuredWidth, d + fileMeasuredHeight);

        v = play_overlay_imageview;
        int overlayMeasuredWidth = v.getMeasuredWidth();
        int overlayMeasuredHeight = v.getMeasuredHeight();

        int overlayX = x + fileMeasuredWidth - overlayMeasuredWidth;
        int overlayY = d + fileMeasuredHeight - overlayMeasuredHeight;
        v.layout(overlayX, overlayY, overlayX + overlayMeasuredWidth, overlayY + overlayMeasuredHeight);

        v = pdf_overlay_imageview;
        v.layout(overlayX, overlayY, overlayX + overlayMeasuredWidth, overlayY + overlayMeasuredHeight);

        x += fileMeasuredWidth + Global.TEN_DP;

        v = filenametextview;
        y = top_offset;
        v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
    }


    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
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
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    public void setData(FilePOJO filePOJO) {
        play_overlay_imageview.setVisibility(filePOJO.getPlayOverlayVisibility());
        pdf_overlay_imageview.setVisibility(filePOJO.getPdfOverlayVisibility());
        fileimageview.setAlpha(filePOJO.getAlfa());
        if (filePOJO.getType() == 0) {
            GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath() + File.separator + filePOJO.getPackage_name() + ".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(fileimageview);
        } else if (filePOJO.getType() < 0) {
            if (filePOJO.getType() == -3) {
                GlideApp.with(context).load(filePOJO.getPath()).signature(new ObjectKey(filePOJO.getDateLong())).placeholder(R.drawable.pdf_file_icon).error(R.drawable.pdf_file_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(fileimageview);
            } else {
                GlideApp.with(context).load(filePOJO.getPath()).signature(new ObjectKey(filePOJO.getDateLong())).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(fileimageview);
            }
        } else {
            GlideApp.with(context).clear(fileimageview);
            fileimageview.setImageDrawable(ContextCompat.getDrawable(context, filePOJO.getType()));
        }
        String displayText;
        switch (filePOJO.getFileObjectType()) {
            case USB_TYPE:
                displayText = DetailFragment.USB_FILE_PREFIX + filePOJO.getPath();
                break;
            case FTP_TYPE:
                displayText = DetailFragment.FTP_FILE_PREFIX + filePOJO.getPath();
                break;
            case SFTP_TYPE:
                displayText = DetailFragment.SFTP_FILE_PREFIX + filePOJO.getPath();
                break;
            case WEBDAV_TYPE:
                displayText = DetailFragment.WEBDAV_FILE_PREFIX + filePOJO.getPath();
                break;
            case SMB_TYPE:
                displayText = DetailFragment.SMB_FILE_PREFIX + filePOJO.getPath();
                break;
            default:
                displayText = filePOJO.getPath();
                break;
        }
        filenametextview.setText(displayText);
    }

    public void setData(int drawable, String text) {
        fileimageview.setImageDrawable(ContextCompat.getDrawable(context, drawable));
        filenametextview.setText(text);
    }
}
