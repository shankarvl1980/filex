package svl.kadatha.filex;

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
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class LineNumberedEditText extends LinearLayout {
    private EditText editText;
    private LineNumberView lineNumberView;
    private int startingLineNumber = 1;
    private static final int LINE_NUMBER_TEXT_SIZE = 10; // sp

    public LineNumberedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);

        lineNumberView = new LineNumberView(context);
        addView(lineNumberView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        editText = new EditText(context);
        editText.setGravity(Gravity.TOP);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setSingleLine(false);
        editText.setHorizontalScrollBarEnabled(false);
        editText.setTypeface(Typeface.MONOSPACE);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                lineNumberView.updateLineNumbers();
            }
        });
        LayoutParams editTextParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
        editTextParams.setMargins(dpToPx(context, 5), 0, 0, 0);  // Add left margin
        addView(editText, editTextParams);

        setTextSize(16f); // Default size, can be changed later
    }

    public void setContent(String content, int pageNumber, int linesPerPage) {
        editText.setText(content);
        startingLineNumber = ((pageNumber - 1) * linesPerPage) + 1;
        lineNumberView.setStartingLineNumber(startingLineNumber);
        lineNumberView.updateLineNumbers();
    }

    public EditText getEditText() {
        return editText;
    }

    public String getContent() {
        return editText.getText().toString();
    }

    public void setShowSoftInputOnFocus(boolean show) {
        editText.setShowSoftInputOnFocus(show);
    }

    public void setEditable(boolean editable) {
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
        editText.setClickable(editable);
        editText.setCursorVisible(editable);
        editText.setShowSoftInputOnFocus(editable);
    }

    public void setTextSize(float size) {
        editText.setTextSize(size);
        lineNumberView.updateLineNumbers();
    }

    public void setTextSize(int unit, float size) {
        editText.setTextSize(unit, size);
        lineNumberView.updateLineNumbers();
    }

    private class LineNumberView extends View {
        private Paint paint;
        private int lastLineCount = 0;
        private int startingLineNumber = 1;

        public LineNumberView(Context context) {
            super(context);
            paint = new Paint();
            paint.setTextSize(dpToPx(context, LINE_NUMBER_TEXT_SIZE));
            paint.setColor(Color.GRAY);
            paint.setTextAlign(Paint.Align.RIGHT);
        }

        public void setStartingLineNumber(int startingLineNumber) {
            this.startingLineNumber = startingLineNumber;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Layout layout = editText.getLayout();
            if (layout != null) {
                int lineCount = layout.getLineCount();
                int actualLineNumber = startingLineNumber;
                for (int i = 0; i < lineCount; i++) {
                    int baseline = layout.getLineBaseline(i);
                    if (i == 0 || layout.getLineStart(i) == 0 || editText.getText().charAt(layout.getLineStart(i) - 1) == '\n') {
                        //canvas.drawText(String.valueOf(actualLineNumber), getWidth() - dpToPx(getContext(), 5), baseline, paint);
                        float yPosition = baseline - paint.ascent();
                        canvas.drawText(String.valueOf(actualLineNumber), getWidth() - dpToPx(getContext(), 5), yPosition, paint);
                        actualLineNumber++;
                    }
                }
            }
        }

        public void updateLineNumbers() {
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int maxNumber = startingLineNumber + Math.max(99, editText.getLineCount() - 1);  // Assume at least 100 lines
            setMeasuredDimension(
                    (int) (paint.measureText(String.valueOf(maxNumber)) + dpToPx(getContext(), 10)),
                    MeasureSpec.getSize(heightMeasureSpec)
            );
        }
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}