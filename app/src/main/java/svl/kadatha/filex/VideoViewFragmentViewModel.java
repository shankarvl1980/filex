package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class VideoViewFragmentViewModel extends AndroidViewModel {

   // public MediaPlayer mp;
    public boolean playmode,completed;
    public boolean firststart,surfaceCreated;
    public boolean wasPlaying;
    public int idx;
    public int position;
    public int orientation;
    public boolean setDisplay;
    public VideoViewFragmentViewModel(@NonNull Application application) {
        super(application);
       // mp=new MediaPlayer();
    }

}
