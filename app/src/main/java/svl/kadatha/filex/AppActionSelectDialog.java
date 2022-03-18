package svl.kadatha.filex;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.content.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AppActionSelectDialog extends DialogFragment
{
    private Context context;
    private AppActionSelectListener appActionSelectListener;
    private AppManagerListFragment.AppPOJO appPOJO;
    private TextView app_name_tv,package_name_tv,app_size_tv;
    private String app_name,package_name,app_size;
    private final List<String> action_list=new ArrayList<>(Arrays.asList(AppManagerListFragment.BACKUP,AppManagerListFragment.UNINSTALL,AppManagerListFragment.PROPERTIES,AppManagerListFragment.PLAY_STORE,AppManagerListFragment.SHARE));


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
        setRetainInstance(true);
        Bundle bundle=getArguments();
        app_name=bundle.getString("app_name");
        package_name=bundle.getString("package_name");
        app_size=bundle.getString("app_size");

    }

    public static AppActionSelectDialog getInstance(String app_name,String package_name,String app_size)
    {
        AppActionSelectDialog appActionSelectDialog=new AppActionSelectDialog();
        Bundle bundle=new Bundle();
        bundle.putString("app_name",app_name);
        bundle.putString("package_name",package_name);
        bundle.putString("app_size",app_size);
        appActionSelectDialog.setArguments(bundle);
        return appActionSelectDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method
        View v=inflater.inflate(R.layout.fragment_app_action,container,false);
        app_name_tv=v.findViewById(R.id.fragment_app_action_app_name);
        package_name_tv=v.findViewById(R.id.fragment_app_action_package_name);
        app_size_tv=v.findViewById(R.id.fragment_app_action_app_size);


        app_name_tv.setText(app_name);
        package_name_tv.setText(package_name);
        app_size_tv.setText(app_size);

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
                if(appActionSelectListener!=null)
                {
                    appActionSelectListener.onSelectType("cancel");
                }
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

        WindowManager.LayoutParams params=window.getAttributes();
        int height=params.height;
        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
        {
            window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);

        }
        else
        {
            window.setLayout(Global.DIALOG_WIDTH,Math.min(height,Global.DIALOG_HEIGHT));

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
                        if(appActionSelectListener!=null)
                        {
                            appActionSelectListener.onSelectType(action_list.get(pos));
                        }
                        dismissAllowingStateLoss();
                    }
                });
            }
        }

    }


    interface AppActionSelectListener
    {
        void onSelectType(String app_action);
    }

    public void setAppActionSelectListener(AppActionSelectListener listener)
    {
        appActionSelectListener=listener;
    }

}
