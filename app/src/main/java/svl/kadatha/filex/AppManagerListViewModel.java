package svl.kadatha.filex;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AppManagerListViewModel extends AndroidViewModel {

    private boolean alreadyRun;
    private Future<?> future;
    public MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
    public List<AppManagerListFragment.AppPOJO> appPOJOList;
    private final Application application;
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public List<AppManagerListFragment.AppPOJO> app_selected_array=new ArrayList<>();

    public AppManagerListViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        future.cancel(true);
        boolean isCancelled = true;
    }

    public void populate(String app_type)
    {
        if(alreadyRun)return;
        alreadyRun=true;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future=executorService.submit(new Runnable() {
            @Override
            public void run() {
                int flags = PackageManager.GET_META_DATA |
                        PackageManager.GET_SHARED_LIBRARY_FILES |
                        PackageManager.GET_UNINSTALLED_PACKAGES;

                appPOJOList=new ArrayList<>();
                final PackageManager packageManager = application.getPackageManager();
                List<PackageInfo> packageInfos=packageManager.getInstalledPackages(flags);
                for (PackageInfo packageInfo : packageInfos)
                {
                    if(app_type.equals(AppManagerActivity.SYSTEM_APPS))
                    {
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
                        {
                            String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                            String package_name=packageInfo.packageName;
                            String version=packageInfo.versionName;
                            String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
                            if(publicsourcedir==null)
                            {
                                continue;
                            }
                            File file = new File(publicsourcedir);
                            String path=file.getAbsolutePath();
                            long size=file.length();
                            long date=file.lastModified();
                            AppManagerListFragment.extract_icon(package_name+".png",packageManager,packageInfo);
                            appPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date,version));

                        }
                    }
                    else
                    {
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1)
                        {
                            String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                            String package_name=packageInfo.packageName;
                            String version=packageInfo.versionName;
                            String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
                            if(publicsourcedir==null)
                            {
                                continue;
                            }
                            File file = new File(publicsourcedir);
                            String path=file.getAbsolutePath();
                            long size=file.length();
                            long date=file.lastModified();
                            AppManagerListFragment.extract_icon(package_name+".png",packageManager,packageInfo);
                            appPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date,version));

                        }
                    }

                }
                isFinished.postValue(true);
            }

        });

    }

}
