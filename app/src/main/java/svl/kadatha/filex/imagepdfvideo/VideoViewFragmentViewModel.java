package svl.kadatha.filex.imagepdfvideo;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class VideoViewFragmentViewModel extends AndroidViewModel {


    public boolean play_mode, completed;
    public boolean wasPlaying;
    public int idx;
    public int position;
    public int orientation;
    public boolean setDisplay;
    public boolean prepared, stopped;

    public VideoViewFragmentViewModel(@NonNull Application application) {
        super(application);
    }
}
