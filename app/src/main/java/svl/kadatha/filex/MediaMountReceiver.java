package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class MediaMountReceiver extends BroadcastReceiver {

    private final List<MediaMountListener> mediaMountListeners=new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mediaMountListeners!=null)
        {
            for(MediaMountListener mediaMountListener:mediaMountListeners)
            {
                mediaMountListener.onMediaMount(intent.getAction());
            }
        }
    }

    interface MediaMountListener
    {
        void onMediaMount(String action);
    }

    public void addMediaMountListener(MediaMountListener listener)
    {
        mediaMountListeners.add(listener);
    }

    public void removeMediaMountListener(MediaMountListener listener)
    {
        mediaMountListeners.remove(listener);
    }
}
