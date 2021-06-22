package svl.kadatha.filex;
import android.content.res.Configuration;
import android.view.*;
import android.content.*;
import android.util.*;
import android.widget.*;

import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;

public class EquallyDistributedImageButtonsLayout extends ViewGroup
{
	private final Context context;
	private final int screen_width;
    private final int screen_height;
    private int child_count,toppadding;
	private LinearLayout.LayoutParams params;

	EquallyDistributedImageButtonsLayout(Context context, int screen_width, int screen_height)
	{
		super(context);
		this.context=context;
		this.child_count= 5;
		this.screen_width=screen_width;
		this.screen_height=screen_height;
		init();
	}
	
	EquallyDistributedImageButtonsLayout(Context context,AttributeSet attr,int child_count,int screen_width,int screen_height)
	{
		super(context,attr);
		this.context=context;
		this.child_count=child_count;
		this.screen_width=screen_width;
		this.screen_height=screen_height;
		init();
	}
	
	EquallyDistributedImageButtonsLayout(Context context,AttributeSet attr,int defStyle,int child_count,int screen_width,int screen_height)
	{
		super(context,attr,defStyle);
		this.context=context;
		this.child_count=child_count;
		this.screen_width=screen_width;
		this.screen_height=screen_height;
		init();
	}

	private void init()
	{
		setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT,ViewGroup.MarginLayoutParams.WRAP_CONTENT));
		LayoutInflater.from(context).inflate(R.layout.toobar_img_buttons_layout,this,true);


        int toolbar_width;
        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			toolbar_width =screen_height;
		}
		else
		{
			toolbar_width =screen_width;
		}

		int icon_dimension=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,getResources().getDisplayMetrics());

		toppadding=(Global.ACTION_BAR_HEIGHT-icon_dimension)/2;

		child_count=Math.min(child_count,getChildCount());
		int w= toolbar_width /child_count;

		params=new LinearLayout.LayoutParams(w,Global.ACTION_BAR_HEIGHT);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// TODO: Implement this method
		int widthUsed=0,maxHeight=0,heightUsed=0;

		for(int i=0; i<child_count; ++i)
		{
			View child=getChildAt(i);

			child.setLayoutParams(params);
			child.setPadding(0,toppadding,0,toppadding);

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
	
		int x=0,y=0;

		for(int i=0;i<child_count;++i)
		{
			View child=getChildAt(i);
			child.layout(x,y,x+child.getMeasuredWidth(),y+child.getMeasuredHeight());
			x+=child.getMeasuredWidth();
			child.setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_recyclerview));

		}

	}
	
	public void setResourceImageDrawables(@IdRes int[] drawables)
	{

		for(int i=0;i<child_count;++i)
		{
			ImageButton child=(ImageButton)getChildAt(i);
			child.setImageDrawable(ContextCompat.getDrawable(context,drawables[i]));

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
