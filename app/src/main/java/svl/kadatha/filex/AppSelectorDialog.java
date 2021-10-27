package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppSelectorDialog extends DialogFragment
{
    private Context context;
    private RecyclerView app_recycler_view;
    private CheckBox remember_app_check_box;
    private String file_path,mime_type,file_type;

    private AsyncTaskStatus asyncTaskStatus;
    private List<AppPOJO> appPOJOList;
    private Intent intent;


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
        if(bundle!=null)
        {
            Uri data = bundle.getParcelable("data");
            file_path=bundle.getString("file_path");
            mime_type=bundle.getString("mime_type");
            for(MimePOJO mimePOJO:Global.MIME_POJOS)
            {
                if(mimePOJO.getMime_type().equals(mime_type))
                {
                    file_type=mimePOJO.getFile_type();
                    break;
                }
            }
            boolean clear_top = bundle.getBoolean("clear_top");
            boolean fromArchiveView = bundle.getBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE,false);
            FileObjectType fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
            intent=new Intent(Intent.ACTION_VIEW);
            FileIntentDispatch.SET_INTENT_FOR_VIEW(intent,mime_type,file_path,"",fileObjectType,fromArchiveView,clear_top,data);
        }

        asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
    }

    public static AppSelectorDialog getInstance(Uri data, String file_path,String mime_type,boolean clear_top,boolean fromArchiveView,FileObjectType fileObjectType)
    {
        AppSelectorDialog appSelectorDialog=new AppSelectorDialog();
        Bundle bundle=new Bundle();
        bundle.putParcelable("data",data);
        bundle.putString("file_path",file_path);
        bundle.putString("mime_type",mime_type);
        bundle.putBoolean("clear_top",clear_top);
        bundle.putBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE,fromArchiveView);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
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
                /* print("Default apps can be removed from 'Settings'"); */
            }
        });
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

        if(asyncTaskStatus!=AsyncTaskStatus.STARTED)
        {
            new AppListAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        return v;
    }

    @Override
    public void onResume()
    {
        // TODO: Implement this method
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
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }


    private static class AppPOJO
    {
        final Drawable app_icon;
        final String app_name;
        final String app_package_name;

        AppPOJO(Drawable app_icon,String app_name,String app_package_name)
        {
            this.app_icon=app_icon;
            this.app_name=app_name;
            this.app_package_name=app_package_name;
        }
    }

    private class AppListAsyncTask extends svl.kadatha.filex.AsyncTask<Void, Void, Void>
    {
        PackageManager packageManager;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            asyncTaskStatus=AsyncTaskStatus.STARTED;
            packageManager= context.getPackageManager();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(appPOJOList==null)
            {
                appPOJOList=new ArrayList<>();
                if(intent==null)
                {
                    return  null;
                }
                List<ResolveInfo> resolveInfoList=packageManager.queryIntentActivities(intent,0);
                int size=resolveInfoList.size();

                for(int i=0; i<size;++i)
                {
                    ResolveInfo resolveInfo=resolveInfoList.get(i);
                    String app_package_name=resolveInfo.activityInfo.packageName;
                    String app_name=resolveInfo.loadLabel(packageManager).toString();
                    Drawable app_icon=resolveInfo.loadIcon(packageManager);
                    appPOJOList.add(new AppPOJO(app_icon, app_name, app_package_name));

                }

            }
            else
            {
                return  null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            AppRecyclerAdapter appRecyclerAdapter = new AppRecyclerAdapter(appPOJOList);
            app_recycler_view.setAdapter(appRecyclerAdapter);
            app_recycler_view.setLayoutManager(new LinearLayoutManager(context));
            asyncTaskStatus=AsyncTaskStatus.COMPLETED;
        }
    }

    private class AppRecyclerAdapter extends RecyclerView.Adapter<AppSelectorDialog.AppRecyclerAdapter.ViewHolder>
    {
        final List<AppPOJO> appPOJOList;
        final DefaultAppDatabaseHelper defaultAppDatabaseHelper=new DefaultAppDatabaseHelper(context);
        AppRecyclerAdapter(List<AppPOJO> appPOJOList)
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
                        AppPOJO appPOJO=appPOJOList.get(pos);
                        final String app_name=appPOJO.app_name;
                        final String app_package_name=appPOJO.app_package_name;
                        if(file_type.equals("APK"))
                        {
                            AppInstallAlertDialogFragment appInstallAlertDialogFragment=AppInstallAlertDialogFragment.getInstance(file_path);
                            appInstallAlertDialogFragment.setAppInstallDialogListener(new AppInstallAlertDialogFragment.AppInstallDialogListener() {
                                @Override
                                public void on_ok_click() {
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
                                    }
                                    intent.setPackage(app_package_name);
                                    context.startActivity(intent);

                                    if(remember_app_check_box.isChecked())
                                    {
                                        defaultAppDatabaseHelper.insert_row(mime_type,file_type,app_name,app_package_name);
                                    }
                                    defaultAppDatabaseHelper.close();
                                    dismissAllowingStateLoss();
                                }

                            });

                            AppCompatActivity appCompatActivity=(AppCompatActivity)context;
                            appInstallAlertDialogFragment.show(appCompatActivity.getSupportFragmentManager(),"");
                            /*
                            if(appCompatActivity instanceof MainActivity)
                            {
                                appInstallAlertDialogFragment.show(MainActivity.FM,"");
                            }
                            else if(appCompatActivity instanceof StorageAnalyserActivity)
                            {
                                appInstallAlertDialogFragment.show(StorageAnalyserActivity.FM,"");
                            }

                             */


                        }
                        else
                        {
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
                            }
                            intent.setPackage(app_package_name);
                            context.startActivity(intent);

                            if(remember_app_check_box.isChecked())
                            {
                                defaultAppDatabaseHelper.insert_row(mime_type,file_type,app_name,app_package_name);
                            }
                            defaultAppDatabaseHelper.close();
                            dismissAllowingStateLoss();
                        }

                    }
                });
            }
        }

        @Override
        public AppRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
        {
            // TODO: Implement this method
            View v= LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout,p1,false);
            return new AppRecyclerAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(AppRecyclerAdapter.ViewHolder p1, int p2)
        {
            // TODO: Implement this method
            AppPOJO filePOJO=appPOJOList.get(p2);
            p1.app_icon_image_view.setImageDrawable(filePOJO.app_icon);
            p1.app_name_text_view.setText(filePOJO.app_name);

        }

        @Override
        public int getItemCount()
        {
            // TODO: Implement this method
            return appPOJOList.size();
        }
    }

    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
