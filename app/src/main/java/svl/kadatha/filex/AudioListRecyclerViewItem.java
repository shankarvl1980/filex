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

public class AudioListRecyclerViewItem extends ViewGroup
{
	private final Context context;
    private ImageView audioimageview, audio_select_indicator;
	public TextView titletextview, albumtextview, durationtextview,artisttextview;
	private int itemWidth,itemHeight, imageview_dimension;
	private final boolean whetherDialog;
	private final int select_indicator_offset=Global.TEN_DP*4;

    AudioListRecyclerViewItem(Context context, boolean whetherDialog)
	{
		super(context);
		this.context=context;
		this.whetherDialog=whetherDialog;
		init();
	}

	AudioListRecyclerViewItem(Context context, AttributeSet attr, boolean whetherDialog)
	{
		super(context,attr);
		this.context=context;
		this.whetherDialog=whetherDialog;
		init();
	}

	AudioListRecyclerViewItem(Context context, AttributeSet attr, int defStyle, boolean whetherDialog)
	{
		super(context,attr,defStyle);
		this.context=context;
		this.whetherDialog=whetherDialog;
		init();
	}

	private void init()
	{

		setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_recyclerview));
        View view = LayoutInflater.from(context).inflate(R.layout.audiolist_recyclerview_layout, this, true);
		audioimageview= view.findViewById(R.id.audio_image);
		audio_select_indicator=view.findViewById(R.id.audio_select_indicator);
		titletextview= view.findViewById(R.id.audio_file_title);
		albumtextview= view.findViewById(R.id.audio_file_album);
		durationtextview= view.findViewById(R.id.audio_file_duration);
		artisttextview= view.findViewById(R.id.audio_file_artist);

        int second_line_font_size;
        int first_line_font_size;
        if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
		{
			first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
			second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
			imageview_dimension =Global.IMAGEVIEW_DIMENSION_SMALL_LIST;

		}
		else if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
		{
			first_line_font_size =Global.FONT_SIZE_LARGE_FIRST_LINE;
			second_line_font_size =Global.FONT_SIZE_LARGE_DETAILS_LINE;
			imageview_dimension =Global.IMAGEVIEW_DIMENSION_LARGE_LIST;
		}
		else
		{
			first_line_font_size =Global.FONT_SIZE_MEDIUM_FIRST_LINE;
			second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
			imageview_dimension =Global.IMAGEVIEW_DIMENSION_MEDIUM_LIST;
		}

		titletextview.setTextSize(first_line_font_size);
		albumtextview.setTextSize(second_line_font_size);
		durationtextview.setTextSize(second_line_font_size);
		artisttextview.setTextSize(second_line_font_size);
		
		audioimageview.getLayoutParams().width= imageview_dimension;
		audioimageview.getLayoutParams().height= imageview_dimension;

		if(whetherDialog)
		{
			itemWidth =Global.DIALOG_WIDTH-Global.SIX_DP;

		}
		else
		{
			if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
			{
				itemWidth =Global.SCREEN_HEIGHT;
			}
			else
			{
				itemWidth =Global.SCREEN_WIDTH;
			}

		}

	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
	{

		int iconheight,maxHeight=0;
		int usedWidth=Global.FOURTEEN_DP;//Global.TEN_DP;


		measureChildWithMargins(audio_select_indicator,widthMeasureSpec,0,heightMeasureSpec,0);
		measureChildWithMargins(audioimageview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
		usedWidth+=imageview_dimension;
		iconheight=imageview_dimension;

		measureChildWithMargins(titletextview,widthMeasureSpec,usedWidth+Global.FOUR_DP+(Global.TEN_DP*2),heightMeasureSpec,0);
		maxHeight+=titletextview.getMeasuredHeight();

		measureChildWithMargins(albumtextview,widthMeasureSpec,usedWidth+Global.FOUR_DP+(Global.TEN_DP*2),heightMeasureSpec,0);
		maxHeight+=albumtextview.getMeasuredHeight();

		measureChildWithMargins(durationtextview,widthMeasureSpec,usedWidth+Global.FOUR_DP+(Global.TEN_DP*2),heightMeasureSpec,0);
		maxHeight+=durationtextview.getMeasuredHeight();
		
		
		measureChildWithMargins(artisttextview,widthMeasureSpec,usedWidth+Global.FOUR_DP+(Global.TEN_DP*2),heightMeasureSpec,0);
		maxHeight+=artisttextview.getMeasuredHeight();

		maxHeight=Math.max(iconheight,maxHeight);
		maxHeight+=Global.RECYCLERVIEW_ITEM_SPACING*2;
		itemHeight=maxHeight;
		setMeasuredDimension(widthMeasureSpec,maxHeight);

	}

	@Override
	protected void onLayout(boolean p1, int l, int t, int r, int b)
	{
		// TODO: Implement this method
		int x=Global.FOURTEEN_DP,y=Global.RECYCLERVIEW_ITEM_SPACING;

		View v=audioimageview;
		int d=(itemHeight-imageview_dimension)/2;
		v.layout(x,d,x+v.getMeasuredWidth(),d+v.getMeasuredHeight());
		x+=v.getMeasuredWidth()+Global.TEN_DP;

		v=audio_select_indicator;
		int a=itemWidth-select_indicator_offset;
		int file_select_indicator_height=v.getMeasuredHeight();
		int c=(itemHeight-file_select_indicator_height)/2;
		v.layout(a,c,a+v.getMeasuredWidth(),c+file_select_indicator_height);

		v=titletextview;
		v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
		y+=v.getMeasuredHeight();

		v=albumtextview;
		v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
		y+=v.getMeasuredHeight();

		v=durationtextview;
		v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
		y+=v.getMeasuredHeight();
		
		v=artisttextview;
		v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());

	}

	public void set_background_color(int color)
	{
		titletextview.setBackgroundColor(color);
		albumtextview.setBackgroundColor(color);
		durationtextview.setBackgroundColor(color);
		artisttextview.setBackgroundColor(color);
	}

	public void set_selected(boolean item_selected)
	{
		audio_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
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


	public void setData(String title,String album,String duration,String artist,boolean item_selected)
	{

		titletextview.setText(title);
		audio_select_indicator.setVisibility(item_selected ? VISIBLE : INVISIBLE);
		albumtextview.setText(album);
		durationtextview.setText(duration);
		artisttextview.setText(artist);
		audioimageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.audio_file_icon));
	}
	
	

}
