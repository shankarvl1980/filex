package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.HashMap;

public class StorageAnalyserActivityViewModel extends AndroidViewModel {


    public StorageAnalyserActivityViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Global.HASHMAP_INTERNAL_DIRECTORY_SIZE=new HashMap<>();
        Global.HASHMAP_EXTERNAL_DIRECTORY_SIZE=new HashMap<>();
    }
}
