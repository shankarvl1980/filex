package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AppActionSelectDialog extends DialogFragment
{
    private Context context;
    private String app_name;
    private String package_name;
    private String app_size;
    private String version;
    private List<String> action_list;
    private Bundle bundle;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle=getArguments();
        app_name=bundle.getString("app_name");
        package_name=bundle.getString("package_name");
        app_size=bundle.getString("app_size");
        version=bundle.getString("version");
        String app_path = bundle.getString("app_path");
        action_list=new ArrayList<>(Arrays.asList(AppManagerListFragment.BACKUP,AppManagerListFragment.UNINSTALL,AppManagerListFragment.CONTROL_PANEL,AppManagerListFragment.PLAY_STORE));
    }

    public static AppActionSelectDialog getInstance(String app_name,String package_name,String app_size, String version,String app_path)
    {
        AppActionSelectDialog appActionSelectDialog=new AppActionSelectDialog();
        Bundle bundle=new Bundle();
        bundle.putString("app_name",app_name);
        bundle.putString("package_name",package_name);
        bundle.putString("app_size",app_size);
        bundle.putString("version",version);
        bundle.putString("app_path",app_path);
        appActionSelectDialog.setArguments(bundle);
        return appActionSelectDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method
        View v=inflater.inflate(R.layout.fragment_app_action,container,false);
        ImageView app_image_view=v.findViewById(R.id.fragment_app_action_app_image);
        TextView app_name_tv = v.findViewById(R.id.fragment_app_action_app_name);
        TextView package_name_tv = v.findViewById(R.id.fragment_app_action_package_name);
        TextView app_size_tv = v.findViewById(R.id.fragment_app_action_app_size);
        TextView app_version_tv=v.findViewById(R.id.fragment_app_action_app_version);

        String apk_icon_file_path=Global.APK_ICON_DIR.getAbsolutePath()+ File.separator+package_name+".png";
        GlideApp.with(context).load(apk_icon_file_path).placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(app_image_view);
        app_name_tv.setText(app_name);
        package_name_tv.setText(package_name);
        app_version_tv.setText(getString(R.string.version)+" "+  version);
        app_size_tv.setText(getString(R.string.size)+" "+app_size);

        RecyclerView app_action_recyclerview = v.findViewById(R.id.fragment_app_action_recyclerView);
        app_action_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        app_action_recyclerview.setLayoutManager(new LinearLayoutManager(context));
        app_action_recyclerview.setAdapter(new AppActionRecyclerViewAdapter());

        ViewGroup button_layout = v.findViewById(R.id.fragment_app_action_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button cancel = button_layout.findViewById(R.id.first_button);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View p1)
            {
                dismissAllowingStateLoss();
            }
        });
        return v;
    }


    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, GridLayout.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private class AppActionRecyclerViewAdapter extends RecyclerView.Adapter<AppActionRecyclerViewAdapter.VH>
    {
        @Override
        public AppActionRecyclerViewAdapter.VH onCreateViewHolder(ViewGroup p1, int p2)
        {
            // TODO: Implement this method
            View v=LayoutInflater.from(context).inflate(R.layout.working_dir_recyclerview_layout,p1,false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(AppActionRecyclerViewAdapter.VH p1, int p2)
        {
            // TODO: Implement this method
            p1.file_type_tv.setText(action_list.get(p2));
        }

        @Override
        public int getItemCount()
        {
            // TODO: Implement this method
            return action_list.size();
        }


        private class VH extends RecyclerView.ViewHolder
        {
            final View v;
            final TextView file_type_tv;
            int pos;
            VH(View vi)
            {
                super(vi);
                v=vi;
                file_type_tv=v.findViewById(R.id.working_dir_name);

                vi.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View p1)
                    {
                        pos=getBindingAdapterPosition();
                        bundle.putString("app_action",action_list.get(pos));
                        if(action_list.get(pos).equals(AppManagerListFragment.BACKUP))
                        {
                            ApkBackUpNameDialog apkBackUpNameDialog=ApkBackUpNameDialog.getInstance(bundle);
                            apkBackUpNameDialog.show(((AppManagerActivity)context).getSupportFragmentManager(),"");
                        }
                        else
                        {
                            ((AppManagerActivity)context).getSupportFragmentManager().setFragmentResult(AppManagerListFragment.APP_ACTION_REQUEST_CODE,bundle);
                        }

                        dismissAllowingStateLoss();
                    }
                });
            }
        }

    }

}
