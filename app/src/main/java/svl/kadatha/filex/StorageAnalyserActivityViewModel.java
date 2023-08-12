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
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        repositoryClass.hashmap_internal_directory_size =new HashMap<>();
        repositoryClass.hashmap_external_directory_size =new HashMap<>();
    }
}
