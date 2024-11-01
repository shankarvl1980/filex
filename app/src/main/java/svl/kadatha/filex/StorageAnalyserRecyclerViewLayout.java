package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
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

public class StorageAnalyserRecyclerViewLayout extends ViewGroup {
    private final Context context;
    private final boolean show_file_path;
    private ImageView fileimageview, overlay_fileimageview, file_select_indicator;
    private TextView filenametextview, filesizetextview, filesizepercentagetextview, filesubfilecounttextview, itemlinebackground, itemlineforeground;
    private View item_separator;
    private int imageview_dimension;
    private int itemWidth;
    private int itemHeight;

    StorageAnalyserRecyclerViewLayout(Context context, boolean show_file_path) {
        super(context);
        this.context = context;
        this.show_file_path = show_file_path;
        init();
    }

    StorageAnalyserRecyclerViewLayout(Context context, AttributeSet attr, boolean show_file_path) {
        super(context, attr);
        this.context = context;
        this.show_file_path = show_file_path;
        init();
    }

    StorageAnalyserRecyclerViewLayout(Context context, AttributeSet attr, int defStyle, boolean show_file_path) {
        super(context, attr, defStyle);
        this.context = context;
        this.show_file_path = show_file_path;
        init();
    }


    private void init() {

        View view = LayoutInflater.from(context).inflate(R.layout.storage_analyser_recyclerview_layout, this, true);

        fileimageview = view.findViewById(R.id.analyser_image_file);
        overlay_fileimageview = view.findViewById(R.id.analyser_overlay_image_file);
        file_select_indicator = view.findViewById(R.id.analyser_file_select_indicator);
        filenametextview = view.findViewById(R.id.analyser_text_file_name);
        filesizetextview = view.findViewById(R.id.analyser_text_size);
        filesizepercentagetextview = view.findViewById(R.id.analyser_text_size_percentage);
        filesubfilecounttextview = view.findViewById(R.id.analyser_text_subfile_count);
        itemlinebackground = view.findViewById(R.id.analyser_item_background);
        itemlineforeground = view.findViewById(R.id.analyser_item_foreground);
        item_separator = view.findViewById(R.id.analyser_item_separator);

        int second_line_font_size;
        int first_line_font_size;
        setBackground(ContextCompat.getDrawable(context, R.drawable.select_detail_recyclerview));

        if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 0) {
            first_line_font_size = Global.FONT_SIZE_SMALL_FIRST_LINE;
            second_line_font_size = Global.FONT_SIZE_SMALL_DETAILS_LINE;
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_SMALL_LIST;

        } else if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 2) {
            first_line_font_size = Global.FONT_SIZE_LARGE_FIRST_LINE;
            second_line_font_size = Global.FONT_SIZE_LARGE_DETAILS_LINE;
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_LARGE_LIST;
        } else {
            first_line_font_size = Global.FONT_SIZE_MEDIUM_FIRST_LINE;
            second_line_font_size = Global.FONT_SIZE_MEDIUM_DETAILS_LINE;
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_MEDIUM_LIST;
        }


        fileimageview.getLayoutParams().width = imageview_dimension;
        fileimageview.getLayoutParams().height = imageview_dimension;

        int overlay_image_dimension = imageview_dimension / 2 - Global.TWO_DP;
        overlay_fileimageview.getLayoutParams().width = overlay_image_dimension;
        overlay_fileimageview.getLayoutParams().height = overlay_image_dimension;

        filenametextview.setTextSize(first_line_font_size);
        filesizetextview.setTextSize(second_line_font_size);
        filesizepercentagetextview.setTextSize(second_line_font_size);
        filesubfilecounttextview.setTextSize(second_line_font_size);

        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            itemWidth = Global.SCREEN_HEIGHT;

        } else {
            itemWidth = Global.SCREEN_WIDTH;

        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int iconheight, maxHeight = 0;
        int usedWidth;

        usedWidth = Global.FOURTEEN_DP;
        measureChildWithMargins(fileimageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        measureChildWithMargins(overlay_fileimageview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);

        usedWidth += imageview_dimension;
        iconheight = imageview_dimension;

        measureChildWithMargins(file_select_indicator, widthMeasureSpec, 0, heightMeasureSpec, 0);

        measureChildWithMargins(filenametextview, widthMeasureSpec, usedWidth + Global.TEN_DP * 2, heightMeasureSpec, 0);
        measureChildWithMargins(itemlinebackground, widthMeasureSpec, usedWidth + Global.TEN_DP * 2, heightMeasureSpec, 0);
        measureChildWithMargins(itemlineforeground, widthMeasureSpec, usedWidth + Global.TEN_DP * 2, heightMeasureSpec, 0);
        maxHeight += filenametextview.getMeasuredHeight();


        measureChildWithMargins(filesizetextview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        usedWidth += filesubfilecounttextview.getMeasuredWidth() + Global.TEN_DP;

        measureChildWithMargins(filesizepercentagetextview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        usedWidth += filesizepercentagetextview.getMeasuredWidth();

        measureChildWithMargins(filesubfilecounttextview, widthMeasureSpec, usedWidth + Global.TEN_DP, heightMeasureSpec, 0);

        maxHeight += filesubfilecounttextview.getMeasuredHeight();

        measureChildWithMargins(item_separator, widthMeasureSpec, Global.TEN_DP * 2, heightMeasureSpec, 0);
        maxHeight += item_separator.getMeasuredHeight();

        maxHeight = Math.max(iconheight, maxHeight);

        maxHeight += Global.RECYCLERVIEW_ITEM_SPACING * 2; //providing top and bottom margin of four dp

        itemHeight = maxHeight;
        setMeasuredDimension(widthMeasureSpec, maxHeight);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
        params.setMargins(0, Global.TWO_DP, 0, Global.TWO_DP);

    }

    @Override
    protected void onLayout(boolean p1, int l, int t, int r, int b) {
        int x = Global.FOURTEEN_DP, y = 0;

        int top_for_icon = (itemHeight - imageview_dimension) / 2;

        // Lay out the fileimageview first
        View v = fileimageview;
        int fileMeasuredHeight = v.getMeasuredHeight();
        int fileMeasuredWidth = v.getMeasuredWidth();
        v.layout(x, top_for_icon, x + fileMeasuredWidth, top_for_icon + fileMeasuredHeight);

        // Then lay out the overlay_fileimageview at the bottom-right corner of fileimageview
        v = overlay_fileimageview;
        int overlayMeasuredHeight = v.getMeasuredHeight();
        int overlayMeasuredWidth = v.getMeasuredWidth();

        int overlayX = x + fileMeasuredWidth - overlayMeasuredWidth;
        int overlayY = top_for_icon + fileMeasuredHeight - overlayMeasuredHeight;
        v.layout(overlayX, overlayY, overlayX + overlayMeasuredWidth, overlayY + overlayMeasuredHeight);

        x += fileMeasuredWidth + Global.TEN_DP;

        // Layout itemlinebackground
        v = itemlinebackground;
        v.layout(x, y, itemWidth - Global.TEN_DP, y + itemHeight);

        // Layout itemlineforeground
        v = itemlineforeground;
        v.layout(x, y, itemWidth - Global.TEN_DP, y + itemHeight);

        // Layout file_select_indicator
        v = file_select_indicator;
        int measuredHeight = v.getMeasuredHeight();
        int measuredWidth = v.getMeasuredWidth();
        int a = (itemWidth - imageview_dimension) / 2;
        a += a / 2 - (measuredWidth / 2) + imageview_dimension - Global.FOUR_DP;
        int file_select_indicator_height = measuredHeight;
        int c = (itemHeight - file_select_indicator_height) / 2;
        v.layout(a, c, a + measuredWidth, c + file_select_indicator_height);

        // Layout filenametextview
        v = filenametextview;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        y = (itemHeight - measuredHeight - filesubfilecounttextview.getMeasuredHeight()) / 2;
        v.layout(x, y, itemWidth - Global.TEN_DP, y + measuredHeight);
        y += measuredHeight;

        // Layout filesizetextview
        v = filesizetextview;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        v.layout(x, y, x + measuredWidth, y + measuredHeight);
        int tempX = x + measuredWidth;

        // Layout filesizepercentagetextview
        v = filesizepercentagetextview;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        int centerX = (itemWidth + imageview_dimension + Global.TEN_DP - measuredWidth) / 2;
        v.layout(centerX, y, centerX + measuredWidth, y + measuredHeight);

        // Layout filesubfilecounttextview
        v = filesubfilecounttextview;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        int endX = itemWidth - measuredWidth - Global.TEN_DP;
        v.layout(endX, y, endX + measuredWidth, y + measuredHeight);
        y += measuredHeight;

        // Layout item_separator
        v = item_separator;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        v.layout(Global.FOURTEEN_DP, itemHeight - measuredHeight, Global.FOURTEEN_DP + measuredWidth, itemHeight);
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

    public void setData(FilePOJO filePOJO, boolean item_selected) {
        overlay_fileimageview.setVisibility(filePOJO.getOverlayVisibility());
        file_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
        if (filePOJO.getType() == 0) {
            GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath() + File.separator + filePOJO.getPackage_name() + ".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(fileimageview);
        } else if (filePOJO.getType() < 0) {
            GlideApp.with(context).load(filePOJO.getPath()).signature(new ObjectKey(filePOJO.getDateLong())).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(fileimageview);
        } else {
            GlideApp.with(context).clear(fileimageview);
            fileimageview.setImageDrawable(ContextCompat.getDrawable(context, filePOJO.getType()));
        }

        filenametextview.setText(filePOJO.getName());
        if (filePOJO.getIsDirectory()) {
            filesizetextview.setText(filePOJO.getTotalSize());
            filesizepercentagetextview.setText(filePOJO.getTotalSizePercentage());
            filesubfilecounttextview.setText("(" + filePOJO.getTotalFiles() + ")");
        } else {
            filesizetextview.setText(filePOJO.getSize());
            filesizepercentagetextview.setText(filePOJO.getTotalSizePercentage());
            filesubfilecounttextview.setText("");
        }

        if (filePOJO.getTotalSizePercentageDouble() == 0) {
            itemlineforeground.getBackground().setLevel(0);
        } else {
            itemlineforeground.getBackground().setLevel((int) (filePOJO.getTotalSizePercentageDouble() * 100));
        }

    }

    public void set_selected(boolean item_selected) {
        file_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
    }

    void setDivider(boolean set) {

        if (set) {
            item_separator.setVisibility(VISIBLE);
        } else {
            item_separator.setVisibility(GONE);
        }
    }

    void setWhetherExternal(FilePOJO filePOJO) {
        String path_category = filePOJO.getWhetherExternal() ? context.getString(R.string.sd_card) : context.getString(R.string.internal);
        filesizetextview.setText(filePOJO.getSize() + "   " + path_category);
    }

}
