package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaMountReceiver extends BroadcastReceiver {

    private MediaMountListener mediaMountListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mediaMountListener!=null)
        {
            mediaMountListener.onMediaMount(intent.getAction());
        }
    }

    interface MediaMountListener
    {
        void onMediaMount(String action);
    }

    public void setMediaMountListener(MediaMountListener listener)
    {
        mediaMountListener=listener;
    }
}
