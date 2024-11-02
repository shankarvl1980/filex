package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class VideoViewFragmentViewModel extends AndroidViewModel {


    public boolean playmode, completed;
    public boolean surfaceCreated;
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
