package svl.kadatha.filex;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;


public class AppInstallAlertDialog extends DialogFragment
{
    private Context context;
    private ImageView app_icon_image_view;
    private TextView app_name_tv,package_name_tv, version_tv,installed_version_tv, message_tv;
    private String file_path;
    private Bitmap app_icon;
    private String package_name, app_name, version, installed_version;
    private AppInstallDialogListener appInstallDialogListener;
    private AsyncTaskStatus asyncTaskStatus;
    private Handler handler;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle bundle=getArguments();
        asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;


        if(bundle!=null)
        {
            file_path=bundle.getString("file_path");
            if(file_path!=null)
            {
                new ApkArchiveInfoAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        handler=new Handler();
        View v=inflater.inflate(R.layout.fragment_apk_install_alert_dialog,container,false);
        app_icon_image_view=v.findViewById(R.id.fragment_apk_install_icon_imageview);
        app_name_tv=v.findViewById(R.id.fragment_apk_install_apk_name_tv);
        package_name_tv=v.findViewById(R.id.fragment_apk_install_apk_package_tv);
        version_tv=v.findViewById(R.id.fragment_apk_install_apk_version_tv);
        installed_version_tv=v.findViewById(R.id.fragment_apk_install_apk_installed_version_tv);
        message_tv=v.findViewById(R.id.fragment_apk_install_message);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    GlideApp.with(context).load(app_icon).placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(app_icon_image_view);
                    app_name_tv.setText(app_name);
                    package_name_tv.setText(package_name);
                    version_tv.setText(version);
                    installed_version_tv.setText(installed_version);

                    if(installed_version.equals(getString(R.string.na)))
                    {
                        message_tv.setText(R.string.do_you_install_a_new_app);
                    }
                    else
                    {
                        message_tv.setText(R.string.do_you_want_update_the_app);
                    }

                    handler.removeCallbacks(this);
                }
                else
                {
                    handler.postDelayed(this,25);
                }
            }
        });

        FrameLayout button_layout = v.findViewById(R.id.fragment_apk_install_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok,cancel;
        ok= button_layout.findViewById(R.id.first_button);
        ok.setText(getString(R.string.ok));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(appInstallDialogListener!=null)
                {
                    appInstallDialogListener.on_ok_click();

                }
                dismissAllowingStateLoss();
            }
        });
        cancel= button_layout.findViewById(R.id.second_button);
        cancel.setText(getString(R.string.cancel));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissAllowingStateLoss();
            }
        });
        return v;
    }

    public static AppInstallAlertDialog getInstance(String file_path)
    {
        AppInstallAlertDialog appInstallAlertDialog =new AppInstallAlertDialog();
        Bundle bundle=new Bundle();
        bundle.putString("file_path",file_path);
        appInstallAlertDialog.setArguments(bundle);
        return appInstallAlertDialog;

    }



    @Override
    public void onResume() {
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(getDialog() != null && getRetainInstance())
        {
            getDialog().setDismissMessage(null);
        }
    }

    private class ApkArchiveInfoAsyncTask extends AsyncTask<Void, Void, Void>
    {
        ProgressBarFragment progressBarFragment;
        PackageManager packageManager;
        FragmentManager fragmentManager;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            asyncTaskStatus=AsyncTaskStatus.STARTED;
            AppCompatActivity appCompatActivity=(AppCompatActivity)context;
            fragmentManager=appCompatActivity.getSupportFragmentManager();
            packageManager=appCompatActivity.getPackageManager();
            progressBarFragment=ProgressBarFragment.getInstance();
            progressBarFragment.show(fragmentManager,"");

        }

        @Override
        protected Void doInBackground(Void... voids) {
            PackageInfo packageInfo=packageManager.getPackageArchiveInfo(file_path,0);
            if(packageInfo!=null)
            {
                packageInfo.applicationInfo.publicSourceDir=file_path;
                Drawable app_icon_drawable=packageInfo.applicationInfo.loadIcon(packageManager);
                if(app_icon_drawable instanceof BitmapDrawable)
                {
                    app_icon=((BitmapDrawable) app_icon_drawable).getBitmap();
                }
                package_name=packageInfo.applicationInfo.packageName;
                app_name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                version=packageInfo.versionName;
            }
            if(package_name==null)
            {
                String na=getString(R.string.na);
                app_name=na;
                package_name=na;
                version=na;
                installed_version=na;
                return null;
            }

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
                installed_version=getString(R.string.na);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            asyncTaskStatus=AsyncTaskStatus.COMPLETED;
            progressBarFragment.dismissAllowingStateLoss();
        }
    }

    public void setAppInstallDialogListener(AppInstallDialogListener listener)
    {
        appInstallDialogListener=listener;
    }

    interface AppInstallDialogListener
    {
        void on_ok_click();
    }


    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}