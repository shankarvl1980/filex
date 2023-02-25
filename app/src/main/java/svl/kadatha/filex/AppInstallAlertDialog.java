package svl.kadatha.filex;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;


public class AppInstallAlertDialog extends DialogFragment
{
    private Context context;
    private ImageView app_icon_image_view;
    private TextView app_name_tv,package_name_tv, version_tv,installed_version_tv, message_tv;
    private String file_path;
    private boolean remember_app_check_box;
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
        viewModel=new ViewModelProvider(this).get(AppSelectorViewModel.class);
        if(bundle!=null)
        {
            Uri data = bundle.getParcelable("data");
            viewModel.app_package_name=bundle.getString("app_package_name");
            viewModel.app_component_name=bundle.getString("app_component_name");
            remember_app_check_box=bundle.getBoolean("remember_app_check_box",false);
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
            FileObjectType fileObjectType= (FileObjectType) bundle.getSerializable("fileObjectType");
            intent=new Intent(Intent.ACTION_VIEW);
            FileIntentDispatch.SET_INTENT_FOR_VIEW(intent,viewModel.mime_type,file_path,"",fileObjectType,fromArchiveView,clear_top,data);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_apk_install_alert_dialog,container,false);
        app_icon_image_view=v.findViewById(R.id.fragment_apk_install_icon_imageview);
        app_name_tv=v.findViewById(R.id.fragment_apk_install_apk_name_tv);
        package_name_tv=v.findViewById(R.id.fragment_apk_install_apk_package_tv);
        version_tv=v.findViewById(R.id.fragment_apk_install_apk_version_tv);
        installed_version_tv=v.findViewById(R.id.fragment_apk_install_apk_installed_version_tv);
        message_tv=v.findViewById(R.id.fragment_apk_install_message);
        progress_bar=v.findViewById(R.id.fragment_apk_install_progressbar);
        FrameLayout button_layout = v.findViewById(R.id.fragment_apk_install_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok,cancel;
        ok= button_layout.findViewById(R.id.first_button);
        ok.setText(getString(R.string.ok));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(progress_bar.getVisibility()==View.VISIBLE)
                {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }

                if (Global.FILEX_PACKAGE.equals(viewModel.app_package_name)) {
                    AppCompatActivity appCompatActivity = (AppCompatActivity) context;
                    if (appCompatActivity instanceof MainActivity) {
                        ((MainActivity) context).clear_cache = false;
                    } else if (appCompatActivity instanceof StorageAnalyserActivity) {
                        ((StorageAnalyserActivity) context).clear_cache = false;
                    }
                }

                intent.setComponent(new ComponentName(viewModel.app_package_name,viewModel.app_component_name));
                try {
                    context.startActivity(intent);
                }
                catch (ActivityNotFoundException e){
                    Global.print(context,getString(R.string.exception_thrown));
                }


                Global.APP_POJO_HASHMAP.clear();
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
        viewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                }

                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
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

}
