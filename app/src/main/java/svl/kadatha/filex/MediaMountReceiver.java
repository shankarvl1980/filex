package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaMountReceiver extends BroadcastReceiver {

    private MediaMountListener mediaMountListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d("shankar","on media mount and dismount broadcast receive and action - "+intent.getAction());
        if(mediaMountListener!=null)
        {
            mediaMountListener.onMediaMount(intent.getAction());
        }
        //Log.d("shankar","mouth point of sd card - ");

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
