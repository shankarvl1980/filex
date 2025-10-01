package svl.kadatha.filex.texteditor;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Minimal base: no line-number logic, no gutter.
 * Holds a configured EditText and exposes the common API used by your Activity.
 * Subclasses must build their own view hierarchy (including any gutters/scroll containers).
 */
public abstract class BaseLineNumberedEditText extends LinearLayout {

    protected EditText editText;
    protected int startingLineNumber = 1; // page-based numbering start; subclass can use it

    public BaseLineNumberedEditText(Context context) {
        super(context);
        // Subclasses will build the tree; we just create a configured EditText for them.
        configureCommonEditText(context);
    }

    public BaseLineNumberedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        configureCommonEditText(context);
    }

    /** Common EditText configuration shared by subclasses */
    protected void configureCommonEditText(Context context) {

    }

    // ------------------- Public API (kept identical to your Activity’s expectations) -------------------

    /** Keep the helper since you used it elsewhere */
    public static int spToPx(Context context, float sp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    /** Page-aware content setter. Subclasses are responsible for using startingLineNumber as needed. */
    public void setContent(String content, int pageNumber, int linesPerPage) {

    }

    public EditText getEditText() {
        return editText;
    }

    public String getContent() {
        Editable e = editText.getText();
        return e == null ? "" : e.toString();
    }

    public void setEditable(boolean editable) {
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);

    }

    public void setTextSize(float sizeSp) {

    }

    /** Subclasses override this to refresh line numbers / gutter width after text or numbering changes */
    protected void onContentOrNumberingChanged() {
        // default no-op
    }

    /** Utility */
    protected int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
