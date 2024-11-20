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
import android.widget.LinearLayout;

public class LineNumberedEditText extends LinearLayout {
    private static final int LINE_NUMBER_TEXT_SIZE = 10; // sp
    private EditText editText;
    private LineNumberView lineNumberView;
    private int startingLineNumber = 1;


    public LineNumberedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public static int spToPx(Context context, float sp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                context.getResources().getDisplayMetrics()
        );
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
        LayoutParams editTextParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
        editTextParams.setMargins(dpToPx(context, 4), 0, 0, 0);
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

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private class LineNumberView extends View {
        private final Paint paint;
        private int[] lineStartIndexes;

        public LineNumberView(Context context) {
            super(context);
            paint = new Paint();
            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LINE_NUMBER_TEXT_SIZE, getResources().getDisplayMetrics()));
            paint.setColor(Color.GRAY);
            paint.setTextAlign(Paint.Align.RIGHT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Layout layout = editText.getLayout();
            if (layout != null && lineStartIndexes != null) {
                int scrollY = editText.getScrollY();
                int firstVisibleLine = layout.getLineForVertical(scrollY);
                int lastVisibleLine = layout.getLineForVertical(scrollY + getHeight()) + 1;

                for (int i = 0; i < lineStartIndexes.length; i++) {
                    int lineStartIndex = lineStartIndexes[i];
                    int layoutLine = layout.getLineForOffset(lineStartIndex);

                    if (layoutLine >= firstVisibleLine && layoutLine <= lastVisibleLine) {
                        int baseline = layout.getLineBaseline(layoutLine) - scrollY;
                        int lineNumber = startingLineNumber + i;
                        String lineNumberStr = String.valueOf(lineNumber);

                        float y = baseline - paint.ascent();
                        canvas.drawText(lineNumberStr, getWidth(), y, paint);
                    }
                }
            }
        }

        public void updateLineNumbers() {
            String text = editText.getText().toString();
            lineStartIndexes = getLineStartIndexes(text);
            invalidate();
        }

        private int[] getLineStartIndexes(String text) {
            java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
            indexes.add(0);  // First line always starts at index 0
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    indexes.add(i + 1);  // Start of next line is after the newline
                }
            }

            // Convert ArrayList<Integer> to int[] without using streams
            int[] result = new int[indexes.size()];
            for (int i = 0; i < indexes.size(); i++) {
                result[i] = indexes.get(i);
            }
            return result;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int maxNumber = startingLineNumber + (lineStartIndexes != null ? lineStartIndexes.length - 1 : 999);
            setMeasuredDimension(
                    (int) (paint.measureText(String.valueOf(maxNumber)) + dpToPx(getContext(), 6)),
                    MeasureSpec.getSize(heightMeasureSpec)
            );
        }

        public void setStartingLineNumber(int startingLineNumber) {
            LineNumberedEditText.this.startingLineNumber = startingLineNumber;
            invalidate();
        }
    }

}