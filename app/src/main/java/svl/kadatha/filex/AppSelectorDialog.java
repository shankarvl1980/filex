package svl.kadatha.filex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.List;

public class AppSelectorDialog extends DialogFragment
{
    private Context context;
    private RecyclerView app_recycler_view;
    private CheckBox remember_app_check_box;
    private String mime_type;
    private String file_type;
    private AsyncTaskStatus asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;

    private Intent intent;
    private Bundle bundle;
    private FrameLayout progress_bar;
    public AppSelectorViewModel viewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle=getArguments();

        if(bundle!=null)
        {
            Uri data = bundle.getParcelable("data");
            String file_path = bundle.getString("file_path");
            mime_type=bundle.getString("mime_type");
            for(MimePOJO mimePOJO:Global.MIME_POJOS)
            {
                if(mimePOJO.getMime_type().equals(mime_type))
                {
                    file_type=mimePOJO.getFile_type();
                    break;
                }
            }
            if(file_type==null) //in case of selection of 'Other' in FileTypeSelectDialog
            {
                file_type="Other";
            }
            boolean clear_top = bundle.getBoolean("clear_top");
            FileObjectType fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
            long file_size = bundle.getLong("file_size");
            intent=new Intent(Intent.ACTION_VIEW);
            FileIntentDispatch.SET_INTENT_FOR_VIEW(intent,mime_type, file_path,"", fileObjectType,clear_top,data);
        }

        if(savedInstanceState!=null)
        {
            asyncTaskStatus= (AsyncTaskStatus) savedInstanceState.get("asyncTaskStatus");
        }

    }

    public static AppSelectorDialog getInstance(Uri data, String file_path,String mime_type,boolean clear_top,FileObjectType fileObjectType,long file_size)
    {
        AppSelectorDialog appSelectorDialog=new AppSelectorDialog();
        Bundle bundle=new Bundle();
        bundle.putParcelable("data",data);
        bundle.putString("file_path",file_path);
        bundle.putString("mime_type",mime_type);
        bundle.putBoolean("clear_top",clear_top);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
        bundle.putLong("file_size",file_size);
        appSelectorDialog.setArguments(bundle);
        return  appSelectorDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_app_selector,container,false);
        app_recycler_view=v.findViewById(R.id.dialog_select_app_recyclerview);
        app_recycler_view.addItemDecoration(Global.DIVIDERITEMDECORATION);

        remember_app_check_box=v.findViewById(R.id.select_app_remember_choice_checkbox);
        remember_app_check_box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });

        ImageView contextualInfo_btn=v.findViewById(R.id.select_app_remember_choice_info_image);
        contextualInfo_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View popup_view=inflater.inflate(R.layout.popup_window,null);
                int width=600;
                int height=ViewGroup.LayoutParams.WRAP_CONTENT;
                boolean focusable=true;

                PopupWindow popupWindow=new PopupWindow(popup_view,width,height,focusable);
                popupWindow.setElevation(20);
                popupWindow.showAtLocation( contextualInfo_btn,Gravity.NO_GRAVITY, (int) contextualInfo_btn.getX()-400, (int) contextualInfo_btn.getY()-remember_app_check_box.getHeight());
            }
        });
        if(mime_type.equals("*/*"))
        {
            remember_app_check_box.setVisibility(View.GONE);
        }
        progress_bar=v.findViewById(R.id.fragment_app_selector_progressbar);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_app_selector_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));

        Button close_button = buttons_layout.findViewById(R.id.first_button);
        close_button.setText(R.string.close);
        close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissAllowingStateLoss();
            }
        });


        viewModel=new ViewModelProvider(this).get(AppSelectorViewModel.class);
        viewModel.populateAppList(intent);
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
                    AppRecyclerAdapter appRecyclerAdapter = new AppRecyclerAdapter(viewModel.appPOJOList);
                    app_recycler_view.setAdapter(appRecyclerAdapter);
                    app_recycler_view.setLayoutManager(new LinearLayoutManager(context));
                }
            }
        });

        return v;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Window window=getDialog().getWindow();
        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
        {
            window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);
        }
        else
        {
            window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_HEIGHT);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("asyncTaskStatus",asyncTaskStatus);
    }


    private class AppRecyclerAdapter extends RecyclerView.Adapter<AppSelectorDialog.AppRecyclerAdapter.ViewHolder>
    {
        final List<AvailableAppPOJO> appPOJOList;
        final DefaultAppDatabaseHelper defaultAppDatabaseHelper=new DefaultAppDatabaseHelper(context);
        AppRecyclerAdapter(List<AvailableAppPOJO> appPOJOList)
        {
            this.appPOJOList=appPOJOList;
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            final View v;
            final ImageView app_icon_image_view;
            final TextView app_name_text_view;

            ViewHolder(View v)
            {
                super(v);
                this.v=v;
                app_icon_image_view=v.findViewById(R.id.image_storage_dir);
                app_name_text_view=v.findViewById(R.id.text_storage_dir_name);

                v.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View p)
                    {
                        int pos=getBindingAdapterPosition();
                        AvailableAppPOJO appPOJO=appPOJOList.get(pos);
                        final String app_name=appPOJO.app_name;
                        final String app_package_name=appPOJO.app_package_name;
                        final String app_component_name=appPOJO.app_component_name;
                        if(file_type.equals("APK"))
                        {
                            bundle.putString("app_package_name",app_package_name);
                            bundle.putString("app_component_name",app_component_name);
                            bundle.putBoolean("remember_app_check_box",remember_app_check_box.isChecked());
                            AppInstallAlertDialog appInstallAlertDialog = AppInstallAlertDialog.getInstance(bundle);
                            AppCompatActivity appCompatActivity=(AppCompatActivity)context;
                            appInstallAlertDialog.show(getParentFragmentManager(),"");
                        }
                        else
                        {
//                            if(fileObjectType!=null && fileObjectType.equals(FileObjectType.USB_TYPE) && app_package_name.equals(Global.FILEX_PACKAGE) &&  file_size>Global.CACHE_FILE_MAX_LIMIT)
//                            {
//                                Global.print(context,context.getString(R.string.file_is_large_copy_to_device_storage));
//                                defaultAppDatabaseHelper.close();
//                                dismissAllowingStateLoss();
//                                return;
//                            }

                            if(Global.FILEX_PACKAGE.equals(app_package_name))
                            {
                                AppCompatActivity appCompatActivity=(AppCompatActivity)context;
                                if(appCompatActivity instanceof MainActivity)
                                {
                                    ((MainActivity)context).clear_cache=false;
                                }
                                else if(appCompatActivity instanceof StorageAnalyserActivity)
                                {
                                    ((StorageAnalyserActivity)context).clear_cache=false;
                                }
                                else if(appCompatActivity instanceof ArchiveViewActivity)
                                {
                                    ((ArchiveViewActivity)context).clear_cache=false;
                                }
                                else if(appCompatActivity instanceof InstaCropperActivity)
                                {
                                    ((InstaCropperActivity)context).clear_cache=false;
                                }
                            }

                            intent.setComponent(new ComponentName(app_package_name,app_component_name));
                            try {
                                context.startActivity(intent);
                            }
                            catch (Exception e){
                                Global.print(context,getString(R.string.exception_thrown));
                            }


                        }

                        if(remember_app_check_box.isChecked())
                        {
                            defaultAppDatabaseHelper.insert_row(mime_type,file_type,app_name,app_package_name,app_component_name);
                        }
                        defaultAppDatabaseHelper.close();
                        dismissAllowingStateLoss();

                    }
                });
            }
        }

        @Override
        public AppRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
        {
            View v= LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout,p1,false);
            return new AppRecyclerAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(AppRecyclerAdapter.ViewHolder p1, int p2)
        {
            AvailableAppPOJO appPOJO=appPOJOList.get(p2);
            GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath()+File.separator+appPOJO.app_package_name+".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(p1.app_icon_image_view);
            p1.app_name_text_view.setText(appPOJO.app_name);

        }

        @Override
        public int getItemCount()
        {
            return appPOJOList.size();
        }
    }



    public static class AvailableAppPOJO
    {
        final String app_name;
        final String app_package_name;
        final String app_component_name;

        AvailableAppPOJO(String app_name,String app_package_name,String app_component_name)
        {
            this.app_name=app_name;
            this.app_package_name=app_package_name;
            this.app_component_name=app_component_name;
        }
    }

}
