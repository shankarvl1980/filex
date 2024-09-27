package svl.kadatha.filex;


import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.appcompat.widget.AppCompatEditText;


/**
 * This is a thin veneer over EditText, with copy/paste/spell-check removed.
 */
public class MyEditText extends AppCompatEditText {
    private final Context context;
    private boolean isPasteMenuToBeShown;

    private Rect rect, line_background_rect;
    private Paint paint;
    private Paint line_background_paint;
    private boolean drawnFlag;

    private OnKeyBoardDownListener onKeyBoardDownListener;

    public MyEditText(Context context) {
        super(context);
        this.context = context;
        //init();
    }

    public MyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        //init();
    }

    public MyEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        //init();
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // TODO: Implement this method
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && onKeyBoardDownListener != null) {
            onKeyBoardDownListener.onKeyDown();
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    public void setOnKeyBoardDownListener(OnKeyBoardDownListener listener) {
        onKeyBoardDownListener = listener;
    }

    /**
     * This is a replacement method for the base TextView class' method of the same name. This method
     * is used in hidden class android.widget.Editor to determine whether the PASTE/REPLACE popup
     * appears when triggered from the text insertion handle. Returning false forces this window
     * to never appear.
     *
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



    public interface OnKeyBoardDownListener {
        void onKeyDown();
    }
} 
