package svl.kadatha.filex;
import java.util.*;
import android.os.*;

import androidx.annotation.MainThread;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;


public abstract class BaseQueuedAdapter < VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH>
{

    protected List<FilePOJO> mDataset;
    private final ArrayDeque<List<FilePOJO>> mPendingUpdates = new ArrayDeque<>();
    final Handler mHandler = new Handler(Looper.getMainLooper());
/*
    @MainThread
    public boolean hasPendingUpdates() 
	{
        return !mPendingUpdates.isEmpty();
    }
*/
    @MainThread
    public List<FilePOJO> peekLast() 
	{
        return mPendingUpdates.isEmpty() ? mDataset : mPendingUpdates.peekLast();
    }

    @MainThread
    public void update(final List<FilePOJO> items) 
	{
        mPendingUpdates.push(items);
        if (mPendingUpdates.size() == 1)
		{
			internalUpdate(items);
		}
		else
		{
			processQueue();
		}
            
    }

    private void internalUpdate(final List<FilePOJO> newList) 
	{
        new Thread(new Runnable() 
		{
				@Override
				public void run() 
				{
					final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DetailRecyclerViewAdapterDiffUtil(mDataset, newList), false);
					mHandler.post(new Runnable() 
					{
							@Override
							public void run() 
							{
								mDataset.clear();
								mDataset.addAll(newList);
								result.dispatchUpdatesTo(BaseQueuedAdapter.this);
								processQueue();
							}
						});
				}
			}).start();
    }

    @MainThread
    private void processQueue() 
	{
        mPendingUpdates.remove();
        if (!mPendingUpdates.isEmpty()) 
		{
            if (mPendingUpdates.size() > 1) 
			{
				List<FilePOJO> lastList = mPendingUpdates.pop();
                mPendingUpdates.clear();
                mPendingUpdates.add(lastList);
				internalUpdate(lastList);
            }
            
        }
    }
}
