package svl.kadatha.filex;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AppSelectorViewModel extends AndroidViewModel {
    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);

    public List<AppSelectorDialog.AvailableAppPOJO> appPOJOList;
    public String file_path,mime_type,file_type,app_package_name,app_component_name;
    public String package_name, app_name, version, installed_version;

    public AppSelectorViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public void populateAppList(Intent intent)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                appPOJOList=new ArrayList<>();
                PackageManager packageManager=application.getPackageManager();
                List<ResolveInfo> resolveInfoList=packageManager.queryIntentActivities(intent,0);
                int size=resolveInfoList.size();

                for(int i=0; i<size;++i)
                {
                    ResolveInfo resolveInfo=resolveInfoList.get(i);
                    String app_package_name=resolveInfo.activityInfo.packageName;
                    String app_component_name=resolveInfo.activityInfo.name;
                    String app_name=resolveInfo.loadLabel(packageManager).toString();
                    String file_with_package_name=app_package_name+".png";
                    if(!Global.APK_ICON_PACKAGE_NAME_LIST.contains(file_with_package_name))
                    {
                        Drawable APKicon = resolveInfo.loadIcon(packageManager);
                        Bitmap bitmap;
                        if(APKicon instanceof BitmapDrawable)
                        {
                            bitmap=((BitmapDrawable)APKicon).getBitmap();
                        }
                        else
                        {
                            bitmap = Bitmap.createBitmap(APKicon.getIntrinsicWidth(),APKicon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            APKicon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                            APKicon.draw(canvas);
                        }

                        File f=new File(Global.APK_ICON_DIR,file_with_package_name);
                        FileOutputStream fileOutputStream=null;
                        try {
                            fileOutputStream=new FileOutputStream(f);
                            bitmap.compress(Bitmap.CompressFormat.PNG,100,fileOutputStream);
                            fileOutputStream.close();
                            Global.APK_ICON_PACKAGE_NAME_LIST.add(file_with_package_name);
                        } catch (IOException e) {
                            if(fileOutputStream!=null)
                            {
                                try {
                                    fileOutputStream.close();
                                } catch (IOException ioException) {

                                }
                            }
                        }

                    }
                    appPOJOList.add(new AppSelectorDialog.AvailableAppPOJO(app_name, app_package_name,app_component_name));
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public void getApkArchiveInfo(String file_path)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {
                PackageManager packageManager=application.getPackageManager();
                PackageInfo packageInfo=packageManager.getPackageArchiveInfo(file_path,0);
                if(packageInfo!=null)
                {
                    packageInfo.applicationInfo.publicSourceDir=file_path;
                    package_name=packageInfo.applicationInfo.packageName;
                    app_name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                    version=packageInfo.versionName;
                }
                if(package_name==null)
                {
                    String na=application.getString(R.string.na);
                    app_name=na;
                    package_name=na;
                    version=na;
                    installed_version=na;
                }
                else
                {
                    List<PackageInfo> packageInfoList=packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
                    int size=packageInfoList.size();
                    for(int i=0; i<size; ++i)
                    {
                        PackageInfo pi=packageInfoList.get(i);
                        if(package_name.equals(pi.packageName))
                        {
                            installed_version=pi.versionName;
                            break;
                        }

                    }
                    if(installed_version==null)
                    {
                        installed_version=application.getString(R.string.na);
                    }

                }
               asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
