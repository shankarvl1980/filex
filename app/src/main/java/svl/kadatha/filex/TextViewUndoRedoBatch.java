package svl.kadatha.filex;


import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.widget.TextView;

import java.util.LinkedList;

public class TextViewUndoRedoBatch 
{
    private static final String TAG = TextViewUndoRedoBatch.class.getCanonicalName();
    private boolean mIsUndoOrRedo = true;

    private final EditHistory mEditHistory;

    private final EditTextChangeListener mChangeListener;

    private final TextView mTextView;
	
	private EditTextRedoUndoListener editTextRedoUndoListener;

    // =================================================================== //

    public TextViewUndoRedoBatch(TextView textView, Context context) {
        mTextView = textView;
        mEditHistory = new EditHistory();
        mChangeListener = new EditTextChangeListener();
        mTextView.addTextChangedListener(mChangeListener);

    }

    // =================================================================== //
	
	
	public void startListening()
	{
		mIsUndoOrRedo=false;
	
	}
	
	public void stopListening()
	{
		mIsUndoOrRedo=true;
	}

	public void setEditTextUndoRedoListener(EditTextRedoUndoListener listener)
	{
		editTextRedoUndoListener=listener;
	}
	
    public void disconnect() 
	{
        mTextView.removeTextChangedListener(mChangeListener);
    }
/*
    public void setMaxHistorySize(int maxHistorySize) 
	{
        mEditHistory.setMaxHistorySize(maxHistorySize);
    }
*/
    public void clearHistory() 
	{
        mEditHistory.clear();
    }

    public boolean getCanUndo() 
	{
        return (mEditHistory.mmPosition > 0);
    }

    public void undo() 
	{
        EditItem edit = mEditHistory.getPrevious();
        if (edit == null) 
		{
            return;
        }

        Editable text = mTextView.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmAfter != null ? edit.mmAfter.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.mmBefore);
        mIsUndoOrRedo = false;

        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class))
		{
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmBefore == null ? start : (start + edit.mmBefore.length()));
    }

    public boolean getCanRedo() 
	{
        return (mEditHistory.mmPosition < mEditHistory.mmHistory.size());
    }

    public void redo() 
	{
        EditItem edit = mEditHistory.getNext();
        if (edit == null) 
		{
            return;
        }

        Editable text = mTextView.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmBefore != null ? edit.mmBefore.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.mmAfter);
        mIsUndoOrRedo = false;

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) 
		{
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmAfter == null ? start
							   : (start + edit.mmAfter.length()));
    }

    public void storePersistentState(Bundle editor, String prefix) 
	{
        // Store hash code of text in the editor so that we can check if the
        // editor contents has changed.
        int size=mEditHistory.mmHistory.size();
        editor.putString(prefix + ".hash",
						 String.valueOf(mTextView.getText().toString().hashCode()));
        //editor.putInt(prefix + ".maxSize", mEditHistory.mmMaxHistorySize);
        editor.putInt(prefix + ".position", mEditHistory.mmPosition);
        editor.putInt(prefix + ".size", size);

        //int i = 0;

        for(int i=0;i<size;++i)
        //for (EditItem ei : mEditHistory.mmHistory)
        {
            EditItem ei=mEditHistory.mmHistory.get(i);
            String pre = prefix + "." + i;

            editor.putInt(pre + ".start", ei.mmStart);
            editor.putString(pre + ".before", ei.mmBefore.toString());
            editor.putString(pre + ".after", ei.mmAfter.toString());

            //++i;
        }
		//editor.commit();
    }

    public boolean restorePersistentState(Bundle sp, String prefix)
	throws IllegalStateException 
	{

        boolean ok = doRestorePersistentState(sp, prefix);
        if (!ok) 
		{
            mEditHistory.clear();
        }
		
        return ok;
    }

    private boolean doRestorePersistentState(Bundle sp, String prefix) 
	{
        String hash = sp.getString(prefix + ".hash", null);
        if (hash == null) 
		{
            // No state to be restored.
            return true;
        }

        mEditHistory.clear();
       // mEditHistory.mmMaxHistorySize = sp.getInt(prefix + ".maxSize", -1);

        int count = sp.getInt(prefix + ".size", -1);
        if (count == -1) 
		{
            return false;
        }

        for (int i = 0; i < count; ++i)
		{
            String pre = prefix + "." + i;

            int start = sp.getInt(pre + ".start", -1);
            String before = sp.getString(pre + ".before", null);
            String after = sp.getString(pre + ".after", null);

            if (start == -1 || before == null || after == null) 
			{
                return false;
            }
            mEditHistory.add(new EditItem(start, before, after));
        }

        mEditHistory.mmPosition = sp.getInt(prefix + ".position", -1);
        return mEditHistory.mmPosition != -1;
    }

	
    // =================================================================== //

    private static final class EditHistory
	{
        private int mmPosition = 0;
        //private int mmMaxHistorySize = -1;

        private final LinkedList<EditItem> mmHistory = new LinkedList<>();

        private void clear() 
		{
            mmPosition = 0;
            mmHistory.clear();
        }

        private void add(EditItem item) 
		{
            while (mmHistory.size() > mmPosition) 
			{
                mmHistory.removeLast();
            }
            mmHistory.add(item);
            mmPosition++;
/*
            if (mmMaxHistorySize >= 0) 
			{
                trimHistory();
            }
			*/
        }
/*
        private void setMaxHistorySize(int maxHistorySize) 
		{
            mmMaxHistorySize = maxHistorySize;
            if (mmMaxHistorySize >= 0) 
			{
                trimHistory();
            }
        }
	*/
/*
        private void trimHistory() 
		{
            while (mmHistory.size() > mmMaxHistorySize) 
			{
                mmHistory.removeFirst();
                mmPosition--;
            }

            if (mmPosition < 0) 
			{
                mmPosition = 0;
            }
        }
*/
        private EditItem getCurrent() 
		{
            if (mmPosition == 0) 
			{
                return null;
            }
            return mmHistory.get(mmPosition - 1);
        }

        private EditItem getPrevious() 
		{
            if (mmPosition == 0) 
			{
                return null;
            }
            mmPosition--;
            return mmHistory.get(mmPosition);
        }

        private EditItem getNext() 
		{
            if (mmPosition >= mmHistory.size()) 
			{
                return null;
            }

            EditItem item = mmHistory.get(mmPosition);
            mmPosition++;
            return item;
        }
    }

    private static final class EditItem
	{
        private int mmStart;
        private CharSequence mmBefore;
        private CharSequence mmAfter;

        public EditItem(int start, CharSequence before, CharSequence after) 
		{
            mmStart = start;
            mmBefore = before;
            mmAfter = after;
        }

        @Override
        public String toString() 
		{
            return "EditItem{" +
				"mmStart=" + mmStart +
				", mmBefore=" + mmBefore +
				", mmAfter=" + mmAfter +
				'}';
        }
    }

    enum ActionType 
	{
        INSERT, DELETE, PASTE, NOT_DEF
    }

    private final class EditTextChangeListener implements TextWatcher 
	{
        private CharSequence mBeforeChange;
        private CharSequence mAfterChange;
        private ActionType lastActionType = ActionType.NOT_DEF;
        private long lastActionTime = 0;

        public void beforeTextChanged(CharSequence s, int start, int count, int after) 
		{
            if (mIsUndoOrRedo) 
			{
                return;
            }

            mBeforeChange = s.subSequence(start, start + count);
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) 
		{
            if (mIsUndoOrRedo) 
			{
                return;
            }

            Log.d(Global.TAG,"textchanging listening");

            mAfterChange = s.subSequence(start, start + count);
            makeBatch(start);
			if(editTextRedoUndoListener!=null)
			{
				editTextRedoUndoListener.onEditTextChange();
			}
        }

        private void makeBatch(int start) 
		{
            ActionType at = getActionType();
            EditItem editItem = mEditHistory.getCurrent();
            if ((lastActionType != at || ActionType.PASTE == at || System.currentTimeMillis() - lastActionTime > 1000) || editItem == null) 
			{
                mEditHistory.add(new EditItem(start, mBeforeChange, mAfterChange));
            } 
			else 
			{
                if (at == ActionType.DELETE) 
				{
                    editItem.mmStart = start;
                    editItem.mmBefore = TextUtils.concat(mBeforeChange, editItem.mmBefore);
                } 
				else 
				{
                    editItem.mmAfter = TextUtils.concat(editItem.mmAfter, mAfterChange);
                }
            }
            lastActionType = at;
            lastActionTime = System.currentTimeMillis();
        }

        private ActionType getActionType() 
		{
            if (!TextUtils.isEmpty(mBeforeChange) && TextUtils.isEmpty(mAfterChange)) 
			{
                return ActionType.DELETE;
            } 
			else if (TextUtils.isEmpty(mBeforeChange) && !TextUtils.isEmpty(mAfterChange)) 
			{
                return ActionType.INSERT;
            } 
			else 
			{
                return ActionType.PASTE;
            }
        }

        public void afterTextChanged(Editable s) 
		{
        }
    }
	
	interface EditTextRedoUndoListener
	{
		void onEditTextChange();
	}
}
