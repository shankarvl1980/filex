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

import java.io.File;

public class StorageAnalyserRecyclerViewLayout extends ViewGroup
{
    private final Context context;
    private ImageView fileimageview,overlay_fileimageview,file_select_indicator;
    private TextView filenametextview, filesizetextview,filesizepercentagetextview,filesubfilecounttextview, itemlinebackground,itemlineforeground;
    private int imageview_dimension;
    private int itemWidth;
    private int itemHeight;
    private final boolean show_file_path;

    StorageAnalyserRecyclerViewLayout(Context context,boolean show_file_path)
    {
        super(context);
        this.context=context;
        this.show_file_path=show_file_path;
        init();
    }

    StorageAnalyserRecyclerViewLayout(Context context, AttributeSet attr, boolean show_file_path)
    {
        super(context,attr);
        this.context=context;
        this.show_file_path=show_file_path;
        init();
    }

    StorageAnalyserRecyclerViewLayout(Context context, AttributeSet attr, int defStyle, boolean show_file_path)
    {
        super(context,attr,defStyle);
        this.context=context;
        this.show_file_path=show_file_path;
        init();
    }



    private void init()
    {

        View view = LayoutInflater.from(context).inflate(R.layout.storage_analyser_recyclerview_layout, this, true);

        fileimageview= view.findViewById(R.id.analyser_image_file);
        overlay_fileimageview= view.findViewById(R.id.analyser_overlay_image_file);
        file_select_indicator=view.findViewById(R.id.analyser_file_select_indicator);
        filenametextview= view.findViewById(R.id.analyser_text_file_name);
        filesizetextview=view.findViewById(R.id.analyser_text_size);
        filesizepercentagetextview=view.findViewById(R.id.analyser_text_size_percentage);
        filesubfilecounttextview= view.findViewById(R.id.analyser_text_subfile_count);
        itemlinebackground=view.findViewById(R.id.analyser_item_background);
        itemlineforeground=view.findViewById(R.id.analyser_item_foreground);

        int second_line_font_size;
        int first_line_font_size;
        setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_recyclerview));

        if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
        {
            first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
            second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
            imageview_dimension=Global.IMAGEVIEW_DIMENSION_SMALL_LIST;

        }
        else if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
        {
            first_line_font_size =Global.FONT_SIZE_LARGE_FIRST_LINE;
            second_line_font_size =Global.FONT_SIZE_LARGE_DETAILS_LINE;
            imageview_dimension=Global.IMAGEVIEW_DIMENSION_LARGE_LIST;
        }
        else
        {
            first_line_font_size =Global.FONT_SIZE_MEDIUM_FIRST_LINE;
            second_line_font_size =Global.FONT_SIZE_MEDIUM_DETAILS_LINE;
            imageview_dimension=Global.IMAGEVIEW_DIMENSION_MEDIUM_LIST;
        }


        fileimageview.getLayoutParams().width=imageview_dimension;
        fileimageview.getLayoutParams().height=imageview_dimension;

        overlay_fileimageview.getLayoutParams().width=imageview_dimension;
        overlay_fileimageview.getLayoutParams().height=imageview_dimension;

        filenametextview.setTextSize(first_line_font_size);
        filesizetextview.setTextSize(second_line_font_size);
        filesizepercentagetextview.setTextSize(second_line_font_size);
        filesubfilecounttextview.setTextSize(second_line_font_size);

        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
        {
            itemWidth=Global.SCREEN_HEIGHT;

        }
        else
        {
            itemWidth=Global.SCREEN_WIDTH;

        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {

        int iconheight,maxHeight=0;
        int usedWidth;

        usedWidth=Global.FOURTEEN_DP;//Global.TEN_DP;
        measureChildWithMargins(fileimageview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
        measureChildWithMargins(overlay_fileimageview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);

        usedWidth+=imageview_dimension;
        iconheight=imageview_dimension;

        measureChildWithMargins(file_select_indicator,widthMeasureSpec,0,heightMeasureSpec,0);

        measureChildWithMargins(filenametextview,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
        measureChildWithMargins(itemlinebackground,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
        measureChildWithMargins(itemlineforeground,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
        maxHeight+=filenametextview.getMeasuredHeight();


        measureChildWithMargins(filesizetextview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
        usedWidth+=filesubfilecounttextview.getMeasuredWidth()+Global.TEN_DP;

        measureChildWithMargins(filesizepercentagetextview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
        usedWidth+=filesizepercentagetextview.getMeasuredWidth();

        measureChildWithMargins(filesubfilecounttextview,widthMeasureSpec,usedWidth+Global.TEN_DP,heightMeasureSpec,0);

        maxHeight+=filesubfilecounttextview.getMeasuredHeight();

        maxHeight=Math.max(iconheight,maxHeight);

        maxHeight+=Global.FOUR_DP*2; //providing top and bottom margin of six dp

        itemHeight=maxHeight;
        setMeasuredDimension(widthMeasureSpec,maxHeight);

        ViewGroup.MarginLayoutParams params=(ViewGroup.MarginLayoutParams) getLayoutParams();
        params.setMargins(0,Global.TWO_DP,0,Global.TWO_DP);

    }

    @Override
    protected void onLayout(boolean p1, int l, int t, int r, int b)
    {
        // TODO: Implement this method
        int x,y=0;

        x=Global.FOURTEEN_DP;//Global.TEN_DP;
        View v=fileimageview;
        int top_for_icon=(itemHeight-imageview_dimension)/2;
        v.layout(x,top_for_icon,x+v.getMeasuredWidth(),top_for_icon+v.getMeasuredHeight());


        v=overlay_fileimageview;
        v.layout(x,top_for_icon,x+v.getMeasuredWidth(),top_for_icon+v.getMeasuredHeight());
        x+=v.getMeasuredWidth()+Global.TEN_DP;


        v=itemlinebackground;
        v.layout(x,y,itemWidth-Global.TEN_DP,y+itemHeight);

        v=itemlineforeground;
        v.layout(x,y,itemWidth-Global.TEN_DP,y+itemHeight);

        v=file_select_indicator;
        int a=(itemWidth-imageview_dimension)/2;
        a+=a/2-(v.getMeasuredWidth()/2)+imageview_dimension-Global.FOUR_DP;
        int file_select_indicator_height=v.getMeasuredHeight();
        int c=(itemHeight-file_select_indicator_height)/2;
        v.layout(a,c,a+v.getMeasuredWidth(),c+file_select_indicator_height);




        v=filenametextview;
        v.layout(x,y,itemWidth-Global.TEN_DP,y+v.getMeasuredHeight());
        y+=v.getMeasuredHeight();


        v=filesizetextview;
        v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
        x+=v.getMeasuredWidth();

        v=filesizepercentagetextview;
        x=(itemWidth+imageview_dimension+Global.TEN_DP-v.getMeasuredWidth())/2;
        v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());


        v=filesubfilecounttextview;
        x=itemWidth-v.getMeasuredWidth()-Global.TEN_DP;
        v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());



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

    public void setData(FilePOJO filePOJO ,boolean item_selected)
    {

        overlay_fileimageview.setVisibility(filePOJO.getOverlayVisibility());
        file_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
        if(filePOJO.getType()==-1)
        {
            GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath()+File.separator+filePOJO.getPackage_name()+".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(fileimageview);

        }
        else if(filePOJO.getType()==0)
        {
            GlideApp.with(context).load(filePOJO.getPath()).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(fileimageview);
        }
        else
        {
            GlideApp.with(context).clear(fileimageview);
            fileimageview.setImageDrawable(ContextCompat.getDrawable(context,filePOJO.getType()));
        }

        filenametextview.setText(filePOJO.getName());
        if(filePOJO.getIsDirectory())
        {
            filesizetextview.setText(filePOJO.getTotalSize());
            filesizepercentagetextview.setText(filePOJO.getTotalSizePercentage());
            filesubfilecounttextview.setText("("+filePOJO.getTotalFiles()+")");
        }
        else
        {
            filesizetextview.setText(filePOJO.getSize());
            filesizepercentagetextview.setText(filePOJO.getTotalSizePercentage());
            filesubfilecounttextview.setText("");
        }
        itemlineforeground.getBackground().setLevel((int) (filePOJO.getTotalSizePercentageDouble()*100));

    }

    public void set_selected(boolean item_selected)
    {
        file_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
    }

}
