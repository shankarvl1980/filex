package svl.kadatha.filex.texteditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;

/**
 * Composite editor with a pinned line-number gutter on the left and an EditText on the right.
 * - Horizontal scrolling is INTERNAL: only the EditText scrolls horizontally; gutter stays pinned.
 * - Vertical scrolling is expected to be owned by an OUTER ScrollView (parent layout), so the
 * inner EditText disables its own vertical scrollbar to avoid nested-Y scroll conflicts.
 * <p>
 * Public helpers expose wrapping toggle and horizontal scroll state for saving/restoring.
 */
public class LineNumberedEditTextNoWrap extends BaseLineNumberedEditText {
    // --- Tunables ---
    private static final int LINE_NUMBER_TEXT_SIZE = 10; // sp  // size of line numbers
    // editor text size
    private static final int GUTTER_RIGHT_PAD_DP = 4;      // pad inside gutter at right edge
    private static final int GUTTER_MIN_WIDTH_DP = 24;     // floor to avoid jittery narrow gutter
    private static final int GAP_BETWEEN_GUTTER_AND_TEXT_DP = 4; // tiny left gap before text

    // --- UI ---
    private LineNumberView lineNumberView;
    private HorizontalScrollView hScroll;

    // --- State ---
    private int startingLineNumber = 1;

    public LineNumberedEditTextNoWrap(Context context) {
        super(context);
        init(context);
    }

    public LineNumberedEditTextNoWrap(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // =========================
    // Public API
    // =========================

    /**
     * Sets content and recomputes line numbers with an explicit page context.
     */
    public void setContent(String content, int pageNumber, int linesPerPage) {
        editText.setText(content);
        startingLineNumber = ((Math.max(1, pageNumber) - 1) * Math.max(1, linesPerPage)) + 1;
        lineNumberView.setStartingLineNumber(startingLineNumber);
        lineNumberView.updateLineNumbers();
    }

    /**
     * Returns the current editor text.
     */
    public String getContent() {
        Editable e = editText.getText();
        return e == null ? "" : e.toString();
    }

    /**
     * Access the inner EditText for advanced operations (selection, IME, etc.).
     */
    public EditText getEditText() {
        return editText;
    }

    /**
     * Enable/disable editing without breaking selection/clipboard behavior.
     */
    public void setEditable(boolean editable) {
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
        editText.setClickable(editable);
        editText.setCursorVisible(editable);
        editText.setShowSoftInputOnFocus(editable);
    }

    /**
     * Set editor text size in SP; gutter recomputes width accordingly.
     */
    public void setTextSize(float sizeSp) {
        editText.setTextSize(sizeSp);
        lineNumberView.requestLayout();
        lineNumberView.invalidate();
    }

    /**
     * Toggle soft wrapping.
     * wrap = true  → lines wrap; horizontal scroll disabled
     * wrap = false → no wrap; horizontal scroll enabled
     */
    public void setSoftWrap(boolean wrap) {
        editText.setHorizontallyScrolling(!wrap);
        // When wrapping, content width becomes viewport width; HSView stops scrolling naturally.
    }

    /**
     * Current internal horizontal scroll X (for state save/restore).
     */
    public int getHorizontalScrollX() {
        return hScroll.getScrollX();
    }

    /**
     * Jump to a specific horizontal position.
     */
    public void scrollToX(int x) {
        hScroll.scrollTo(Math.max(0, x), 0);
    }

    /**
     * Smoothly scroll to a specific horizontal position.
     */
    public void smoothScrollToX(int x) {
        hScroll.smoothScrollTo(Math.max(0, x), 0);
    }


    // =========================
    // Internal setup
    // =========================

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);

        // 1) Gutter (left, never in any horizontal scroller)
        lineNumberView = new LineNumberView(context);
        LayoutParams gutterParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        addView(lineNumberView, gutterParams);

        // 2) Horizontal scroller (right) → contains ONLY the EditText
        hScroll = new HorizontalScrollView(context);
        hScroll.setFillViewport(true);
        hScroll.setHorizontalScrollBarEnabled(true);
        hScroll.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);

        editText = new EditText(context);
        editText.setGravity(Gravity.TOP | Gravity.START);
        editText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setSingleLine(false);
        editText.setHorizontallyScrolling(true);      // default: no wrap, allow horizontal scroll
        editText.setHorizontalScrollBarEnabled(true);
        editText.setVerticalScrollBarEnabled(false);  // outer ScrollView handles vertical
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setTextSize(16f);

        // Tiny visual gap between gutter and text
        int leftGap = dpToPx(context, GAP_BETWEEN_GUTTER_AND_TEXT_DP);
        editText.setPadding(
                editText.getPaddingLeft() + leftGap,
                editText.getPaddingTop(),
                editText.getPaddingRight(),
                editText.getPaddingBottom()
        );


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                lineNumberView.updateLineNumbers();
            }
        });

        // Put editor inside HSView
        HorizontalScrollView.LayoutParams insideParams =
                new HorizontalScrollView.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        hScroll.addView(editText, insideParams);

        // Add HSView to the right; it should take remaining width
        LayoutParams rightParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        addView(hScroll, rightParams);
    }

    public int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    // =========================
    // Gutter view (line numbers)
    // =========================

    private class LineNumberView extends View {
        private final Paint paint;
        private int[] lineStartIndexes;

        LineNumberView(Context context) {
            super(context);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    LINE_NUMBER_TEXT_SIZE,
                    getResources().getDisplayMetrics()));
            paint.setColor(Color.GRAY);
            paint.setTextAlign(Paint.Align.RIGHT);


            // Convenience: tap gutter to focus editor
            setOnClickListener(v -> editText.requestFocus());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Layout layout = editText.getLayout();
            if (layout == null || lineStartIndexes == null) return;

            // Map layout coordinates (content space) to this gutter view's canvas
            int scrollY = editText.getScrollY();
            int padTop = editText.getTotalPaddingTop();

            int firstVisibleLine = layout.getLineForVertical(scrollY);
            int lastVisibleLine = layout.getLineForVertical(scrollY + getHeight());

            float xRight = getWidth() - dpToPx(getContext(), GUTTER_RIGHT_PAD_DP);

            for (int i = 0; i < lineStartIndexes.length; i++) {
                int off = lineStartIndexes[i];
                int layoutLine = layout.getLineForOffset(off);
                if (layoutLine < firstVisibleLine || layoutLine > lastVisibleLine) continue;

                // Baseline in content coords
                int baselineContent = layout.getLineBaseline(layoutLine);

                // Convert to gutter canvas coords
                float y = baselineContent + padTop - scrollY;

                canvas.drawText(
                        String.valueOf(startingLineNumber + i),
                        xRight,
                        y,
                        paint
                );
            }
        }


        void updateLineNumbers() {
            Editable editable = editText.getText();
            if (editable == null || editable.length() == 0) {
                lineStartIndexes = new int[]{0};
            } else {
                String text = editable.toString();
                // Collect start offsets of all logical lines (split by '\n')
                java.util.ArrayList<Integer> idx = new java.util.ArrayList<>();
                idx.add(0);
                for (int i = 0; i < text.length(); i++) {
                    if (text.charAt(i) == '\n') idx.add(i + 1);
                }
                lineStartIndexes = new int[idx.size()];
                for (int i = 0; i < idx.size(); i++) lineStartIndexes[i] = idx.get(i);
            }
            requestLayout(); // digit count may change → width change
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Compute max line number currently visible/possible
            int lineCount = (lineStartIndexes != null ? lineStartIndexes.length : 1000);
            int maxNumber = startingLineNumber + Math.max(0, lineCount - 1);
            int digits = String.valueOf(maxNumber).length();

            float charW = paint.measureText("0"); // approximate per-digit width
            int pad = dpToPx(getContext(), GUTTER_RIGHT_PAD_DP) + dpToPx(getContext(), 4);
            int desired = (int) (digits * charW) + pad;
            int min = dpToPx(getContext(), GUTTER_MIN_WIDTH_DP);

            int width = resolveSize(Math.max(desired, min), widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(width, height);
        }

        void setStartingLineNumber(int starting) {
            startingLineNumber = Math.max(1, starting);
            invalidate();
        }
    }
}
