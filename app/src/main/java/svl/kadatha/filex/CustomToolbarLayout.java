package svl.kadatha.filex;
import android.content.res.Configuration;
import android.view.*;
import android.content.*;
import android.util.*;
import android.widget.*;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

public class CustomToolbarLayout extends ViewGroup
{
	private final Context context;
	private final int resource_layout_id;
    private int viewCount;
    private int top_padding;
	private final int screen_width;
    private final int screen_height;
	LinearLayout.LayoutParams params;

	CustomToolbarLayout(Context context, @LayoutRes int layout_id, int screen_width, int screen_height)
	{
		super(context);
		this.context=context;
		resource_layout_id=layout_id;
		this.screen_width=screen_width;
		this.screen_height=screen_height;
		init();
	}
	CustomToolbarLayout(Context context,AttributeSet attr,@LayoutRes int layout_id,int screen_width,int screen_height)
	{
		super(context,attr);
		this.context=context;
		resource_layout_id=layout_id;
		this.screen_width=screen_width;
		this.screen_height=screen_height;
		init();
	}
	CustomToolbarLayout(Context context,AttributeSet attr,int defStyle, @LayoutRes int layout_id,int screen_width,int screen_height)
	{
		super(context,attr,defStyle);
		this.context=context;
		resource_layout_id=layout_id;
		this.screen_width=screen_width;
		this.screen_height=screen_height;
		init();
	}

	private void init()
	{
		setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT,ViewGroup.MarginLayoutParams.WRAP_CONTENT));
		LayoutInflater.from(context).inflate(resource_layout_id,this,true);

        int toolbar_width;
        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			toolbar_width =screen_height;
		}
		else
		{
			toolbar_width =screen_width;
		}
		viewCount=getChildCount();
		int icon_dimension=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,30,getResources().getDisplayMetrics());
		top_padding=(Global.ACTION_BAR_HEIGHT-icon_dimension)/2;
		int w= toolbar_width /viewCount;
		params=new LinearLayout.LayoutParams(w,Global.ACTION_BAR_HEIGHT);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// TODO: Implement this method
		int widthUsed=0,maxHeight=0,heightUsed=0;

		for(int i=0; i<viewCount; ++i)
		{
			View child=getChildAt(i);
			child.setLayoutParams(params);
			child.setPadding(0,top_padding,0,top_padding);
			measureChildWithMargins(child,widthMeasureSpec,widthUsed,Global.ACTION_BAR_HEIGHT,heightUsed);
			widthUsed+=child.getMeasuredWidth()+child.getPaddingStart()+child.getPaddingEnd();
			heightUsed=child.getMeasuredHeight()+child.getPaddingTop()+child.getPaddingBottom();
			maxHeight=Math.max(maxHeight,heightUsed);
		}
		
		//maxHeight+=getPaddingTop()+getPaddingBottom();
		setMeasuredDimension(widthMeasureSpec,Global.ACTION_BAR_HEIGHT);
		
	}
/*
	@Override
	protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed)
	{
		// TODO: Implement this method
		MarginLayoutParams lp=(MarginLayoutParams)child.getLayoutParams();
		widthUsed+=widthUsed+lp.leftMargin+lp.rightMargin;
		heightUsed+=heightUsed+lp.topMargin+lp.topMargin;
		int childWidthMeasureSpec=getChildMeasureSpec(parentWidthMeasureSpec,widthUsed,lp.width);
		int childHeightMeasureSpec=getChildMeasureSpec(parentHeightMeasureSpec,heightUsed,lp.height);
		measureChild(child,childWidthMeasureSpec,childHeightMeasureSpec);
		//super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
		
	}
	*/
	
	@Override
	protected void onLayout(boolean p1, int p2, int p3, int p4, int p5)
	{
		// TODO: Implement this method
		int child_count=getChildCount();
		int x=0,y=0;
		
		for(int i=0;i<child_count;++i)
		{
			View child=getChildAt(i);
			child.layout(x,y,x+child.getMeasuredWidth(),y+child.getMeasuredHeight());
			x+=child.getMeasuredWidth();
			child.setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_recyclerview));

		}
		
	}
	
	
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p)
	{
		// TODO: Implement this method
		return p instanceof MarginLayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams()
	{
		// TODO: Implement this method
		return new MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT,MarginLayoutParams.WRAP_CONTENT);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p)
	{
		// TODO: Implement this method
		return generateDefaultLayoutParams();
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs)
	{
		// TODO: Implement this method
		return new MarginLayoutParams(context,attrs);
	}
	

}
