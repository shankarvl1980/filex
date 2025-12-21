package svl.kadatha.filex.appmanager;

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

import java.io.File;

import svl.kadatha.filex.GlideApp;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.R;

public class AppInstalledRecyclerViewLayoutGrid extends AppInstalledRecyclerViewLayout {
    private final Context context;
    public int itemWidth, itemHeight;
    private ImageView appimageview, appselect_indicator;
    private TextView appnametextview, apppackagenametextview, appversiontextview, appsizetextview, appdatetextview;
    private int imageview_dimension;
    private int select_indicator_offset_linear;

    AppInstalledRecyclerViewLayoutGrid(Context context) {
        super(context);
        this.context = context;
        init();
    }

    private void init() {
        View view = LayoutInflater.from(context).inflate(R.layout.app_manager_recycler_layout, this, true);
        appimageview = view.findViewById(R.id.app_manager_app_image);
        //appselect_indicator=view.findViewById(R.id.app_manager_select_indicator);
        appnametextview = view.findViewById(R.id.app_manager_app_name);
        apppackagenametextview = view.findViewById(R.id.app_manager_app_package);
        appversiontextview = view.findViewById(R.id.app_manager_app_version);
        appsizetextview = view.findViewById(R.id.app_manager_app_size);
        appdatetextview = view.findViewById(R.id.app_manager_app_date);

        int second_line_font_size;
        int first_line_font_size;

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

        appnametextview.setMaxLines(2);
        appnametextview.setGravity(Gravity.CENTER);

        appsizetextview.setGravity(Gravity.CENTER);

        setBackground(ContextCompat.getDrawable(context, R.drawable.select_detail_recyclerview));

        appimageview.getLayoutParams().width = imageview_dimension;
        appimageview.getLayoutParams().height = imageview_dimension;

        appnametextview.setTextSize(first_line_font_size);
        apppackagenametextview.setTextSize(second_line_font_size);
        appversiontextview.setTextSize(second_line_font_size);
        appsizetextview.setTextSize(second_line_font_size);
        appdatetextview.setTextSize(second_line_font_size);

        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            itemWidth = Global.SCREEN_HEIGHT;
        } else {
            itemWidth = Global.SCREEN_WIDTH;
        }

        //select_indicator_offset_linear=Global.TEN_DP*4; //around 40 dp which is about 1 & half of select indicator icon;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxHeight = 0;
        int usedWidth;

        //measureChildWithMargins(appselect_indicator,widthMeasureSpec,0,heightMeasureSpec,0);
        measureChildWithMargins(appimageview, widthMeasureSpec, 0, heightMeasureSpec, 0);

        maxHeight += imageview_dimension;
        usedWidth = Global.FOUR_DP * 2;

        measureChildWithMargins(appnametextview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        maxHeight += appnametextview.getMeasuredHeight();

        measureChildWithMargins(appsizetextview, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
        maxHeight += appsizetextview.getMeasuredHeight();

        maxHeight += Global.RECYCLERVIEW_ITEM_SPACING * 2 + Global.TWELVE_DP; //providing top and bottom margin of six dp
        itemHeight = maxHeight;
        setMeasuredDimension(widthMeasureSpec, maxHeight);
    }

    @Override
    protected void onLayout(boolean p1, int l, int t, int r, int b) {
        int x = 0, y = Global.RECYCLERVIEW_ITEM_SPACING + Global.SIX_DP;
        int measuredHeight;
        int measuredWidth;
        int grid_count = Global.GRID_COUNT;
        int grid_width = (itemWidth - (Global.RECYCLERVIEW_ITEM_SPACING * 2)) / grid_count; //Deducting twenty dp because, recyclerview is added start and end padding of ten dp
        x += (grid_width - imageview_dimension) / 2;

        View v;

        v = appimageview;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        v.layout(x, y, x + measuredWidth, y + measuredHeight);
        y += measuredHeight + Global.TWO_DP;

        x = Global.FOUR_DP;
        v = appnametextview;
        measuredHeight = v.getMeasuredHeight();
        measuredWidth = v.getMeasuredWidth();
        v.layout(x, y, x + measuredWidth, y + measuredHeight);
        y += measuredHeight + Global.TWO_DP;

        v = appsizetextview;
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

    public void setData(AppManagerListFragment.AppPOJO appPOJO, boolean item_selected) {
        //appselect_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
        String apk_icon_file_path = Global.APK_ICON_DIR.getAbsolutePath() + File.separator + appPOJO.getPackage_name() + ".png";
        GlideApp.with(context).load(apk_icon_file_path).placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(appimageview);
        appnametextview.setText(appPOJO.getName());
        apppackagenametextview.setText(appPOJO.getPackage_name());
        appversiontextview.setText(context.getString(R.string.version) + " " + appPOJO.getVersion());
        appsizetextview.setText(appPOJO.getSize());
        appdatetextview.setText(appPOJO.getDate());
    }
}
