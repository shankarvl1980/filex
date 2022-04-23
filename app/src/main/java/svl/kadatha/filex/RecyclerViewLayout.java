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

import java.io.File;

public class RecyclerViewLayout extends ViewGroup
{
	private final Context context;
    private ImageView fileimageview,overlay_fileimageview,file_select_indicator;
	private TextView filenametextview, filesubfilecounttextview, filepermissionstextView,filemoddatetextview,filepathtextview;
    private int imageview_dimension;
	public int itemWidth, itemHeight;
	private final boolean show_file_path,whether_file_selector_activity;
	private int select_indicator_offset_linear;
	private boolean file_grid_layout;
	private int grid_count;

	RecyclerViewLayout(Context context,boolean show_file_path,boolean whether_file_selector_activity)
	{
		super(context);
		this.context=context;
		this.show_file_path=show_file_path;
		this.whether_file_selector_activity=whether_file_selector_activity;
		init();
	}
	
	RecyclerViewLayout(Context context, AttributeSet attr, boolean show_file_path,boolean whether_file_selector_activity)
	{
		super(context,attr);
		this.context=context;
		this.show_file_path=show_file_path;
		this.whether_file_selector_activity=whether_file_selector_activity;
		init();
	}

	RecyclerViewLayout(Context context, AttributeSet attr, int defStyle, boolean show_file_path,boolean whether_file_selector_activity)
	{
		super(context,attr,defStyle);
		this.context=context;
		this.show_file_path=show_file_path;
		this.whether_file_selector_activity=whether_file_selector_activity;
		init();
	}
	

	public static void setIcon(Context context,FilePOJO filePOJO,ImageView fileimageview,ImageView overlay_fileimageview)
	{
		overlay_fileimageview.setVisibility(filePOJO.getOverlayVisibility());
		if(filePOJO.getType()==-1)
		{
			GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath()+ File.separator+filePOJO.getPackage_name()+".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(fileimageview);

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

	}


	private void init()
	{

        View view = LayoutInflater.from(context).inflate(R.layout.filedetail_recyclerview_layout, this, true);
		fileimageview= view.findViewById(R.id.image_file);
		overlay_fileimageview= view.findViewById(R.id.overlay_image_file);
		file_select_indicator=view.findViewById(R.id.file_select_indicator);
		filenametextview= view.findViewById(R.id.text_file_name);
		filesubfilecounttextview= view.findViewById(R.id.text_subfile_count);
		//filepermissionstextView=view.findViewById(R.id.text_file_permissions);
		filemoddatetextview= view.findViewById(R.id.text_file_moddate);
		filepathtextview=view.findViewById(R.id.text_file_path);

        int second_line_font_size;
        int first_line_font_size;

        if(whether_file_selector_activity)
		{
			file_grid_layout=FileSelectorActivity.FILE_GRID_LAYOUT;
			grid_count=FileSelectorActivity.GRID_COUNT;
			if(file_grid_layout)
			{
				setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_grid_recyclerview));
				if(FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
				{
					first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
					second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
					imageview_dimension=Global.IMAGEVIEW_DIMENSION_SMALL_GRID;

				}
				else if(FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
				{

					first_line_font_size =Global.FONT_SIZE_MEDIUM_FIRST_LINE;
					second_line_font_size =Global.FONT_SIZE_MEDIUM_DETAILS_LINE;
					imageview_dimension=Global.IMAGEVIEW_DIMENSION_LARGE_GRID;

				}
				else
				{
					first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
					second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
					imageview_dimension=Global.IMAGEVIEW_DIMENSION_MEDIUM_GRID;

				}

				MarginLayoutParams params= (MarginLayoutParams) filenametextview.getLayoutParams();
				params.setMargins(Global.FOUR_DP,0,Global.FOUR_DP,0);

				filenametextview.setMaxLines(2);
				filenametextview.setGravity(Gravity.CENTER);
				params= (MarginLayoutParams) filesubfilecounttextview.getLayoutParams();
				params.setMargins(Global.FOUR_DP,0,Global.FOUR_DP,0);

				filesubfilecounttextview.setGravity(Gravity.CENTER);

			}
			else
			{

				setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_recyclerview));
				if(FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
				{
					first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
					second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
					imageview_dimension=Global.IMAGEVIEW_DIMENSION_SMALL_LIST;

				}
				else if(FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
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

			}

		}
        else
		{
			file_grid_layout=Global.FILE_GRID_LAYOUT;
			grid_count=Global.GRID_COUNT;
			if(file_grid_layout)
			{
				setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_grid_recyclerview));
				if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
				{
					first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
					second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
					imageview_dimension=Global.IMAGEVIEW_DIMENSION_SMALL_GRID;

				}
				else if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
				{

					first_line_font_size =Global.FONT_SIZE_MEDIUM_FIRST_LINE;
					second_line_font_size =Global.FONT_SIZE_MEDIUM_DETAILS_LINE;
					imageview_dimension=Global.IMAGEVIEW_DIMENSION_LARGE_GRID;

				}
				else
				{
					first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
					second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
					imageview_dimension=Global.IMAGEVIEW_DIMENSION_MEDIUM_GRID;

				}

				MarginLayoutParams params= (MarginLayoutParams) filenametextview.getLayoutParams();
				params.setMargins(Global.FOUR_DP,0,Global.FOUR_DP,0);

				filenametextview.setMaxLines(2);
				filenametextview.setGravity(Gravity.CENTER);
				params= (MarginLayoutParams) filesubfilecounttextview.getLayoutParams();
				params.setMargins(Global.FOUR_DP,0,Global.FOUR_DP,0);

				filesubfilecounttextview.setGravity(Gravity.CENTER);

			}
			else
			{

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

			}

		}



		fileimageview.getLayoutParams().width=imageview_dimension;
		fileimageview.getLayoutParams().height=imageview_dimension;

		overlay_fileimageview.getLayoutParams().width=imageview_dimension;
		overlay_fileimageview.getLayoutParams().height=imageview_dimension;

		filenametextview.setTextSize(first_line_font_size);
		filesubfilecounttextview.setTextSize(second_line_font_size);
		//filepermissionstextView.setTextSize(second_line_font_size);
		filemoddatetextview.setTextSize(second_line_font_size);
		filepathtextview.setTextSize(second_line_font_size);

		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			itemWidth=Global.SCREEN_HEIGHT;

		}
		else
		{
			itemWidth=Global.SCREEN_WIDTH;

		}

		select_indicator_offset_linear=Global.TEN_DP*4; //around 40 dp which is about 1 & half of select indicator icon;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int iconheight;
		 int maxHeight=0;
		int usedWidth;

		if(file_grid_layout)
		{

			measureChildWithMargins(file_select_indicator,widthMeasureSpec,0,heightMeasureSpec,0);
			measureChildWithMargins(fileimageview,widthMeasureSpec,0,heightMeasureSpec,0);
			measureChildWithMargins(overlay_fileimageview,widthMeasureSpec,0,heightMeasureSpec,0);

			maxHeight+=imageview_dimension;

			measureChildWithMargins(filenametextview,widthMeasureSpec,0,heightMeasureSpec,0);
			maxHeight+=filenametextview.getMeasuredHeight();

			measureChildWithMargins(filesubfilecounttextview,widthMeasureSpec,0,heightMeasureSpec,0);
			maxHeight+=filesubfilecounttextview.getMeasuredHeight();

			maxHeight+=Global.FOUR_DP*2;
		}
		else
		{
			usedWidth=Global.TEN_DP;
			measureChildWithMargins(fileimageview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
			measureChildWithMargins(overlay_fileimageview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);

			usedWidth+=imageview_dimension;
			iconheight=imageview_dimension;

			measureChildWithMargins(file_select_indicator,widthMeasureSpec,0,heightMeasureSpec,0);

			measureChildWithMargins(filenametextview,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
			measureChildWithMargins(filepathtextview,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
			maxHeight+=filenametextview.getMeasuredHeight();
			if(show_file_path)
			{
				filepathtextview.setVisibility(VISIBLE);
				maxHeight+=filepathtextview.getMeasuredHeight();
			}


			measureChildWithMargins(filesubfilecounttextview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
			usedWidth+=filesubfilecounttextview.getMeasuredWidth()+Global.TEN_DP;

			measureChildWithMargins(filemoddatetextview,widthMeasureSpec,usedWidth+Global.TEN_DP,heightMeasureSpec,0);

			maxHeight+=filemoddatetextview.getMeasuredHeight();

			maxHeight=Math.max(iconheight,maxHeight);

			maxHeight+=Global.RECYCLERVIEW_ITEM_SPACING*2; //providing top and bottom margin of six dp

		}

		itemHeight=maxHeight;
		setMeasuredDimension(widthMeasureSpec,maxHeight);

	}
	
	@Override
	protected void onLayout(boolean p1, int l, int t, int r, int b)
	{
		// TODO: Implement this method
		int x=0,y=Global.FOUR_DP;

		if(file_grid_layout)
		{
			//int grid_count=;
			int grid_width=itemWidth/grid_count;
			x+=(grid_width-imageview_dimension)/2;

			View v=file_select_indicator;

			int a=grid_width-((grid_width-imageview_dimension)/2)-Global.SELECTOR_ICON_DIMENSION;
			v.layout(a,y,a+v.getMeasuredWidth(),y+v.getMeasuredHeight());

			v=fileimageview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());

			v=overlay_fileimageview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			y+=v.getMeasuredHeight();


			x=Global.FOUR_DP;
			v=filenametextview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			y+=v.getMeasuredHeight();



			v=filesubfilecounttextview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());

		}
		else
		{
			x=Global.TEN_DP;
			int margin_offset_icon, max_height_second_line;
			View v=fileimageview;
			y=Global.RECYCLERVIEW_ITEM_SPACING;
			int d=(itemHeight-imageview_dimension)/2;
			v.layout(x,d,x+v.getMeasuredWidth(),d+v.getMeasuredHeight());


			v=overlay_fileimageview;
			v.layout(x,d,x+v.getMeasuredWidth(),d+v.getMeasuredHeight());
			x+=v.getMeasuredWidth()+Global.TEN_DP;
			margin_offset_icon=x;

			v=file_select_indicator;
			int a=itemWidth-select_indicator_offset_linear;
			int file_select_indicator_height=v.getMeasuredHeight();
			int c=(itemHeight-file_select_indicator_height)/2;
			v.layout(a,c,a+v.getMeasuredWidth(),c+file_select_indicator_height);

			v=filenametextview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			y+=v.getMeasuredHeight();


			v=filesubfilecounttextview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			x+=v.getMeasuredWidth();
			max_height_second_line=v.getMeasuredHeight();

			v=filemoddatetextview;
			x=Math.max(x,itemWidth/2);
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			max_height_second_line=Math.max(max_height_second_line,v.getMeasuredHeight());

			v=filepathtextview;
			v.layout(margin_offset_icon,y+max_height_second_line,margin_offset_icon+v.getMeasuredWidth(),y+max_height_second_line+v.getMeasuredHeight());

		}


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
		fileimageview.setAlpha(filePOJO.getAlfa());
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
		filesubfilecounttextview.setText(filePOJO.getSize());
		filemoddatetextview.setText(filePOJO.getDate());
		filepathtextview.setText(context.getString(R.string.path_colon)+" "+filePOJO.getPath());
	}

	public void set_selected(boolean item_selected)
	{
		file_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
	}

}
