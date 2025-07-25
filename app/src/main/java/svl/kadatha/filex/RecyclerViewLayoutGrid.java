package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;

public class RecyclerViewLayoutGrid extends RecyclerViewLayout {
    private final Context context;
    public int itemWidth, itemHeight;
    private ImageView fileimageview, play_overlay_imageview, pdf_overlay_imageview, file_select_indicator;
    private TextView filenametextview, filesubfilecounttextview, filepermissionstextView, filemoddatetextview, filepathtextview;
    private View item_separator;
    private int imageview_dimension;

    RecyclerViewLayoutGrid(Context context, boolean show_file_path) {
        super(context);
        this.context = context;
        init();
    }


    public static void setIcon(Context context, FilePOJO filePOJO, ImageView fileimageview, ImageView play_overlay_imageview, ImageView pdf_overlay_imageview) {
        play_overlay_imageview.setVisibility(filePOJO.getPlayOverlayVisibility());
        pdf_overlay_imageview.setVisibility(filePOJO.getPdfOverlayVisibility());
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
    }


    private void init() {
        View view = LayoutInflater.from(context).inflate(R.layout.filedetail_recyclerview_layout, this, true);
        fileimageview = view.findViewById(R.id.image_file);
        play_overlay_imageview = view.findViewById(R.id.play_overlay_image_file);
        pdf_overlay_imageview = view.findViewById(R.id.pdf_overlay_image_file);
        file_select_indicator = view.findViewById(R.id.file_select_indicator);
        filenametextview = view.findViewById(R.id.text_file_name);
        filesubfilecounttextview = view.findViewById(R.id.text_subfile_count);
        //filepermissionstextView=view.findViewById(R.id.text_file_permissions);
        filemoddatetextview = view.findViewById(R.id.text_file_moddate);
        filepathtextview = view.findViewById(R.id.text_file_path);
        item_separator = view.findViewById(R.id.item_separator);

        int second_line_font_size;
        int first_line_font_size;
        int overlay_image_dimension;

        if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 0) {
            first_line_font_size = Global.FONT_SIZE_SMALL_FIRST_LINE;
            second_line_font_size = Global.FONT_SIZE_SMALL_DETAILS_LINE;
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_SMALL_GRID;
        } else if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 2) {
            first_line_font_size = Global.FONT_SIZE_MEDIUM_FIRST_LINE;
            second_line_font_size = Global.FONT_SIZE_MEDIUM_DETAILS_LINE;
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_LARGE_GRID;
        } else {
            first_line_font_size = Global.FONT_SIZE_SMALL_FIRST_LINE;
            second_line_font_size = Global.FONT_SIZE_SMALL_DETAILS_LINE;
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_MEDIUM_GRID;
        }

        filenametextview.setMaxLines(2);
        filenametextview.setGravity(Gravity.CENTER);

        filesubfilecounttextview.setGravity(Gravity.CENTER);
        overlay_image_dimension = imageview_dimension / 2 - Global.FOURTEEN_DP;

        setBackground(ContextCompat.getDrawable(context, R.drawable.select_detail_recyclerview));

        fileimageview.getLayoutParams().width = imageview_dimension;
        fileimageview.getLayoutParams().height = imageview_dimension;

        play_overlay_imageview.getLayoutParams().width = overlay_image_dimension;
        play_overlay_imageview.getLayoutParams().height = overlay_image_dimension;

        pdf_overlay_imageview.getLayoutParams().width = overlay_image_dimension;
        pdf_overlay_imageview.getLayoutParams().height = overlay_image_dimension;

        filenametextview.setTextSize(first_line_font_size);
        filesubfilecounttextview.setTextSize(second_line_font_size);
        //filepermissionstextView.setTextSize(second_line_font_size);
        filemoddatetextview.setTextSize(second_line_font_size);
        filepathtextview.setTextSize(second_line_font_size);

        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            itemWidth = Global.SCREEN_HEIGHT;
        } else {
            itemWidth = Global.SCREEN_WIDTH;
        }

        int select_indicator_offset_linear = Global.TEN_DP * 4; //around 40 dp which is about 1 & half of select indicator icon;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int iconheight;
        int maxHeight = 0;
        int usedWidth;

        measureChildWithMargins(file_select_indicator, widthMeasureSpec, 0, heightMeasureSpec, 0);
        measureChildWithMargins(fileimageview, widthMeasureSpec, 0, heightMeasureSpec, 0);
        measureChildWithMargins(play_overlay_imageview, widthMeasureSpec, 0, heightMeasureSpec, 0);
        measureChildWithMargins(pdf_overlay_imageview, widthMeasureSpec, 0, heightMeasureSpec, 0);

        maxHeight += imageview_dimension;
        usedWidth = Global.FOUR_DP * 2;

        measureChildWithMargins(filenametextview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        maxHeight += filenametextview.getMeasuredHeight();

        measureChildWithMargins(filesubfilecounttextview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        maxHeight += filesubfilecounttextview.getMeasuredHeight();

        //measureChildWithMargins(item_separator,widthMeasureSpec,0,heightMeasureSpec,0);
        //maxHeight+=item_separator.getMeasuredHeight();

        maxHeight += Global.RECYCLERVIEW_ITEM_SPACING * 2;//providing top and bottom margin of six dp
        itemHeight = maxHeight;
        setMeasuredDimension(widthMeasureSpec, maxHeight);
    }

    @Override
    protected void onLayout(boolean p1, int l, int t, int r, int b) {
        int x = 0, y = Global.RECYCLERVIEW_ITEM_SPACING;

        int grid_width = (itemWidth - (Global.RECYCLERVIEW_ITEM_SPACING * 2)) / Global.GRID_COUNT; // Deducting padding
        x += (grid_width - imageview_dimension) / 2;

        View v = file_select_indicator;
        int measuredHeight = v.getMeasuredHeight();
        int measuredWidth = v.getMeasuredWidth();
        int a = grid_width - ((grid_width - imageview_dimension) / 2) - Global.SELECTOR_ICON_DIMENSION;
        v.layout(a, y, a + measuredWidth, y + measuredHeight);

        v = fileimageview;
        int fileMeasuredHeight = v.getMeasuredHeight();
        int fileMeasuredWidth = v.getMeasuredWidth();
        v.layout(x, y, x + fileMeasuredWidth, y + fileMeasuredHeight);

        // Then lay out the overlay_fileimageview at the bottom-right corner of fileimageview
        v = play_overlay_imageview;
        int overlayMeasuredHeight = v.getMeasuredHeight();
        int overlayMeasuredWidth = v.getMeasuredWidth();

        int overlayX = x + fileMeasuredWidth - overlayMeasuredWidth;
        int overlayY = y + fileMeasuredHeight - overlayMeasuredHeight;
        v.layout(overlayX, overlayY, overlayX + overlayMeasuredWidth, overlayY + overlayMeasuredHeight);

        v = pdf_overlay_imageview;
        v.layout(overlayX, overlayY, overlayX + overlayMeasuredWidth, overlayY + overlayMeasuredHeight);

        y += fileMeasuredHeight;


        x = Global.FOUR_DP;
        v = filenametextview;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        v.layout(x, y, x + measuredWidth, y + measuredHeight);
        y += measuredHeight;


        v = filesubfilecounttextview;
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

    public void setData(FilePOJO filePOJO, boolean item_selected) {
        play_overlay_imageview.setVisibility(filePOJO.getPlayOverlayVisibility());
        pdf_overlay_imageview.setVisibility(filePOJO.getPdfOverlayVisibility());
        fileimageview.setAlpha(filePOJO.getAlfa());
        file_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
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

        filenametextview.setText(filePOJO.getName());
        filesubfilecounttextview.setText(filePOJO.getSize());
        filemoddatetextview.setText(filePOJO.getDate());
        filepathtextview.setText(filePOJO.getPath());
    }

    public void set_selected(boolean item_selected) {
        file_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    void setDivider(boolean set) {
        if (set) {
            item_separator.setVisibility(VISIBLE);
        } else {
            item_separator.setVisibility(GONE);
        }
    }

    @Override
    void setWhetherExternal(FilePOJO filePOJO) {

    }
}
