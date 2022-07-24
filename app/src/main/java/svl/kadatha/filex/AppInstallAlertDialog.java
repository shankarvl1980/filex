package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.List;


public class AppInstallAlertDialog extends DialogFragment
{
    private Context context;
    private ImageView app_icon_image_view;
    private TextView app_name_tv,package_name_tv, version_tv,installed_version_tv, message_tv;
    private String file_path;
    //private String mime_type,file_type,app_package_name;

    //private String package_name, app_name, version, installed_version;
    //private AppInstallDialogListener appInstallDialogListener;
    private boolean remember_app_check_box;
    private AsyncTaskStatus asyncTaskStatus;
    private Handler handler;
    private Intent intent;
    private AppSelectorViewModel viewModel;
    private FrameLayout progress_bar;



    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle bundle=getArguments();
        asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
        viewModel=new ViewModelProvider(this).get(AppSelectorViewModel.class);
        if(bundle!=null)
        {
            Uri data = bundle.getParcelable("data");
            viewModel.app_package_name=bundle.getString("app_check_name");
            remember_app_check_box=bundle.getBoolean("remember_app_check_box");
            file_path=bundle.getString("file_path");
            viewModel.mime_type=bundle.getString("mime_type");
            for(MimePOJO mimePOJO:Global.MIME_POJOS)
            {
                if(mimePOJO.getMime_type().equals(viewModel.mime_type))
                {
                    viewModel.file_type=mimePOJO.getFile_type();
                    break;
                }
            }
            boolean clear_top = bundle.getBoolean("clear_top");
            boolean fromArchiveView = bundle.getBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE,false);
            FileObjectType fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
            intent=new Intent(Intent.ACTION_VIEW);
            FileIntentDispatch.SET_INTENT_FOR_VIEW(intent,viewModel.mime_type,file_path,"",fileObjectType,fromArchiveView,clear_top,data);
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
       /*
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {

                    handler.removeCallbacks(this);
                }
                else
                {
                    handler.postDelayed(this,25);
                }
            }
        });

        */

        progress_bar=v.findViewById(R.id.fragment_apk_install_progressbar);
        FrameLayout button_layout = v.findViewById(R.id.fragment_apk_install_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok,cancel;
        ok= button_layout.findViewById(R.id.first_button);
        ok.setText(getString(R.string.ok));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final DefaultAppDatabaseHelper defaultAppDatabaseHelper=new DefaultAppDatabaseHelper(context);

                if (Global.FILEX_PACKAGE.equals(viewModel.app_package_name)) {
                    AppCompatActivity appCompatActivity = (AppCompatActivity) context;
                    if (appCompatActivity instanceof MainActivity) {
                        ((MainActivity) context).clear_cache = false;
                    } else if (appCompatActivity instanceof StorageAnalyserActivity) {
                        ((StorageAnalyserActivity) context).clear_cache = false;
                    }
                }
                intent.setPackage(viewModel.app_package_name);
                context.startActivity(intent);

                if (remember_app_check_box) {
                    defaultAppDatabaseHelper.insert_row(viewModel.mime_type, viewModel.file_type, viewModel.app_name, viewModel.app_package_name);
                }
                defaultAppDatabaseHelper.close();
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


        viewModel.getApkArchiveInfo(file_path);
        viewModel.isFinished.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean)
                {
                    String apk_icon_file_path=Global.APK_ICON_DIR.getAbsolutePath()+ File.separator+viewModel.package_name+".png";
                    GlideApp.with(context).load(apk_icon_file_path).placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(app_icon_image_view);
                    app_name_tv.setText(viewModel.app_name);
                    package_name_tv.setText(viewModel.package_name);
                    version_tv.setText(viewModel.version);
                    installed_version_tv.setText(viewModel.installed_version);

                    if(viewModel.installed_version.equals(getString(R.string.na)))
                    {
                        message_tv.setText(R.string.do_you_install_a_new_app);
                    }
                    else
                    {
                        message_tv.setText(R.string.do_you_want_update_the_app);
                    }
                    progress_bar.setVisibility(View.GONE);
                }

            }
        });

        return v;
    }

    public static AppInstallAlertDialog getInstance(Bundle bundle)
    {
        AppInstallAlertDialog appInstallAlertDialog =new AppInstallAlertDialog();
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

    /*
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(getDialog() != null && getRetainInstance())
        {
            getDialog().setDismissMessage(null);
        }
    }

     */

    /*
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
            progressBarFragment=ProgressBarFragment.newInstance();
            progressBarFragment.show(fragmentManager,"");

        }

        @Override
        protected Void doInBackground(Void... voids) {
            PackageInfo packageInfo=packageManager.getPackageArchiveInfo(file_path,0);
            if(packageInfo!=null)
            {
                packageInfo.applicationInfo.publicSourceDir=file_path;
                viewModel.package_name=packageInfo.applicationInfo.packageName;
                viewModel.app_name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                viewModel.version=packageInfo.versionName;
            }
            if(viewModel.package_name==null)
            {
                String na=getString(R.string.na);
                viewModel.app_name=na;
                viewModel.package_name=na;
                viewModel.version=na;
                viewModel.installed_version=na;
                return null;
            }

            List<PackageInfo> packageInfoList=packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            int size=packageInfoList.size();
            for(int i=0; i<size; ++i)
            {
                PackageInfo pi=packageInfoList.get(i);
                if(viewModel.package_name.equals(pi.packageName))
                {
                    viewModel.installed_version=pi.versionName;
                    break;
                }

            }
            if(viewModel.installed_version==null)
            {
                viewModel.installed_version=getString(R.string.na);
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

     */

    /*
    public void setAppInstallDialogListener(AppInstallDialogListener listener)
    {
        appInstallDialogListener=listener;
    }

    interface AppInstallDialogListener
    {
        void on_ok_click();
    }

     */

}
