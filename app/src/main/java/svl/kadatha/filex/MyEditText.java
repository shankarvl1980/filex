package svl.kadatha.filex;


import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.view.*;

import androidx.appcompat.widget.AppCompatEditText;


/**
 *  This is a thin veneer over EditText, with copy/paste/spell-check removed.
 */
public class MyEditText extends AppCompatEditText
{
    private final Context context;
	private boolean isPasteMenuToBeShown;
	
	private Rect rect,line_background_rect;
    private Paint paint;
	private Paint line_background_paint;
	private boolean drawnFlag;
	
	private OnKeyBoardDownListener onKeyBoardDownListener;
	
    public MyEditText(Context context)
    {
        super(context);
        this.context = context;
		//init();
    }

    public MyEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
		//init();
    }

    public MyEditText(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
      	this.context=context;
		//init();
    }

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event)
	{
		// TODO: Implement this method
		if(keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_UP && onKeyBoardDownListener!=null)
		{
			onKeyBoardDownListener.onKeyDown();
			return false;
		}
		return super.dispatchKeyEvent(event);
	}
	
	
	/*
	private void init()
	{
        setDrawingCacheEnabled(true);
		drawnFlag=false;
		rect = new Rect();
		line_background_rect=new Rect();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.GRAY);
        paint.setTextSize(FileEditorActivity.LINE_NUMBER_SIZE);
		paint.setTypeface(Typeface.MONOSPACE);
		line_background_paint=new Paint(Paint.ANTI_ALIAS_FLAG);
		line_background_paint.setStyle(Paint.Style.FILL);
		line_background_paint.setColor(Color.LTGRAY);
		line_background_paint.setAlpha(100);
	
	}

	
    @Override
    protected void onDraw(Canvas canvas) {
        
		if(drawnFlag)
		{
			return;
		}
		int baseline=0;
		int lineCount = getLineCount();
		int lineNumber = 1;
		int ln;
		Layout layout = getLayout();
		
		for (int i = 0; i < lineCount; ++i) 
		{
			baseline=getLineBounds(i, null);
			if (i == 0) 
			{
				canvas.drawText(""+lineNumber, rect.left, baseline, paint);
	
				++lineNumber;
			}
			else if (getText().charAt(layout.getLineStart(i) - 1) == '\n' ) 
			{
	
				canvas.drawText(""+lineNumber, rect.left, baseline, paint);

				++lineNumber;
			}
		
		}
		

		int selectionStart = Selection.getSelectionStart(getText());


		if (!(selectionStart == -1)) 
		{
			ln=layout.getLineForOffset(selectionStart);
		}
		else
		{
			ln=-1;
		}

		if(ln!=-1)
		{

			getLineBounds(ln,line_background_rect);
			canvas.drawRect(line_background_rect,line_background_paint);

		}
		
		if(lineCount<100)
		{
			setPadding(FileEditorActivity.LINE_NUMBER_SIZE*2,getPaddingTop(),getPaddingRight(),getPaddingBottom());
			//canvas.drawRect(rect.left,rect.top,40,rect.bottom,line_background_paint);
		}
		else if(lineCount>99 && lineCount<1000)
		{
			setPadding(FileEditorActivity.LINE_NUMBER_SIZE*2,getPaddingTop(),getPaddingRight(),getPaddingBottom());
		}
		else if(lineCount>999 && lineCount<10000)
		{
			setPadding(FileEditorActivity.LINE_NUMBER_SIZE*3,getPaddingTop(),getPaddingRight(),getPaddingBottom());
		}
		else if(lineCount>9999 && lineCount<100000)
		{
			setPadding(FileEditorActivity.LINE_NUMBER_SIZE*4,getPaddingTop(),getPaddingRight(),getPaddingBottom());
		}
		
       
		super.onDraw(canvas);
		
    }
	
*/
	 
	 /*
    boolean canPaste()
    {
		return isPasteMenuToBeShown;
    }
*/
    /** This is a replacement method for the base TextView class' method of the same name. This method
     * is used in hidden class android.widget.Editor to determine whether the PASTE/REPLACE popup
     * appears when triggered from the text insertion handle. Returning false forces this window
     * to never appear.
     * @return false
     */
	 /*
    @Override
    public boolean isSuggestionsEnabled()
    {
        return isPasteMenuToBeShown;
    }
	
	public void setPasteMenuToBeShown(boolean isPasteMenuToBeShown)
	{
		this.isPasteMenuToBeShown=isPasteMenuToBeShown;
		//clearFocus();
		
		
	}
*/

/*
	@Overrideú
	public int getSelectionStart() {

		for (StackTraceElement element : Thread.currentThread().getStackTrace()) 
		{
			
			if ((element.getMethodName().equals("canPaste")|| element.getMethodName().equals("canCut")) && !isPasteMenuToBeShown) 
			{
				return -1;
			}
		}
		return super.getSelectionStart();
	}
	*/
	
	
	
	public interface OnKeyBoardDownListener
	{
		void onKeyDown();
	}
	
	public void setOnKeyBoardDownListener(OnKeyBoardDownListener listener)
	{
		onKeyBoardDownListener=listener;
	}
} 
