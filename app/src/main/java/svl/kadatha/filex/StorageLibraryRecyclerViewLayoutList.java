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

public class StorageLibraryRecyclerViewLayoutList extends ViewGroup {
    private final Context context;
    public int itemWidth, itemHeight;
    private ImageView fileimageview, play_overlay_imageview, pdf_overlay_imageview;
    private TextView filenametextview;
    private int imageview_dimension;
    private boolean isIconHeightMore;

    StorageLibraryRecyclerViewLayoutList(Context context, int itemWidth) {
        super(context);
        this.context = context;
        this.itemWidth = itemWidth;
        init();
    }

    private void init() {
        View view = LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout, this, true);
        fileimageview = view.findViewById(R.id.image_storage_dir);
        play_overlay_imageview = view.findViewById(R.id.play_overlay_image_storage_dir);
        pdf_overlay_imageview = view.findViewById(R.id.pdf_overlay_image_storage_dir);
        filenametextview = view.findViewById(R.id.text_storage_dir_name);

        int overlay_image_dimension;
        imageview_dimension = fileimageview.getWidth();

        overlay_image_dimension = imageview_dimension / 2 - Global.TWO_DP;

        setBackground(ContextCompat.getDrawable(context, R.drawable.select_detail_recyclerview));

        play_overlay_imageview.getLayoutParams().width = overlay_image_dimension;
        play_overlay_imageview.getLayoutParams().height = overlay_image_dimension;

        pdf_overlay_imageview.getLayoutParams().width = overlay_image_dimension;
        pdf_overlay_imageview.getLayoutParams().height = overlay_image_dimension;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int iconHeight = 0;
        int maxHeight = 0;
        int usedWidth = 0;

        //usedWidth = Global.FOURTEEN_DP;
        measureChildWithMargins(fileimageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        measureChildWithMargins(play_overlay_imageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        measureChildWithMargins(pdf_overlay_imageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);

        usedWidth += imageview_dimension;
        iconHeight = imageview_dimension;

        measureChildWithMargins(filenametextview, widthMeasureSpec, usedWidth + Global.FOUR_DP + (Global.TEN_DP * 2), heightMeasureSpec, 0);

        maxHeight += filenametextview.getMeasuredHeight();

        maxHeight = Math.max(iconHeight, maxHeight);
        isIconHeightMore = (iconHeight == maxHeight);

        //maxHeight += Global.RECYCLERVIEW_ITEM_SPACING;//providing top and bottom margin of six dp
        itemHeight = maxHeight;
        setMeasuredDimension(widthMeasureSpec, maxHeight);
    }

    @Override
    protected void onLayout(boolean p1, int l, int t, int r, int b) {
        int x = 0;
        int y;
        int top_offset;

        top_offset = (itemHeight - filenametextview.getMeasuredHeight() - Global.FOUR_DP) / 2;
        int d = isIconHeightMore ? (itemHeight - imageview_dimension) / 2 : top_offset + Global.SIX_DP;

        View v = fileimageview;
        int fileMeasuredWidth = v.getMeasuredWidth();
        int fileMeasuredHeight = v.getMeasuredHeight();
        v.layout(x, d, x + fileMeasuredWidth, d + fileMeasuredHeight);

        // Then lay out the overlay_fileimageview at the bottom-right corner of fileimageview
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

    public void setData(FilePOJO filePOJO, boolean item_selected) {
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
}
