package svl.kadatha.filex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppListFragment extends Fragment {

    private Context context;
    private String app_type="";
    private RecyclerView recyclerView;
    private FrameLayout progressBar;
    private TextView app_count_textview;
    private Toolbar bottom_toolbar;
    private boolean toolbar_visible=true;
    private int scroll_distance;
    private AsyncTaskStatus asyncTaskStatus;
    private List<AppPOJO> appPOJOList;
    private AppListAdapter adapter;
    private Handler handler;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setRetainInstance(true);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            app_type=bundle.getString(AppManagerActivity.SYSTEM_APPS);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                asyncTaskStatus=AsyncTaskStatus.STARTED;
                appPOJOList=new ArrayList<>();
                final PackageManager packageManager = context.getPackageManager();
                /*
                List<ApplicationInfo> installedApplications =
                        packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

                for (ApplicationInfo appInfo : installedApplications)
                {
                    if(app_type.equals(AppManagerActivity.SYSTEM_APPS))
                    {
                        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        {
                            // IS A SYSTEM APP
                            String name=appInfo.name;
                            String package_name=appInfo.packageName;

                            String size;
                            File file = new File(applicationInfo.publicSourceDir);
                            int size = file.length();
                            appPOJOList.add(new AppPOJO(name,package_name,size));

                        }
                    }
                    else
                    {
                        if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                        {
                            // APP WAS INSTALL AS AN UPDATE TO A BUILD-IN SYSTEM APP
                        }
                    }

                }

                 */
                List<PackageInfo> packageInfos=packageManager.getInstalledPackages(0);
                for (PackageInfo packageInfo : packageInfos)
                {
                    if(app_type.equals(AppManagerActivity.SYSTEM_APPS))
                    {
                        //if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        {
                            // IS A SYSTEM APP
                            String name=packageInfo.versionName;
                            String package_name=packageInfo.packageName;
                            long size=0;
                            String size_formatted=FileUtil.humanReadableByteCount(size,false);
                            appPOJOList.add(new AppPOJO(name,package_name,size_formatted));

                        }
                    }
                    else
                    {
                        //if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                        {
                            // APP WAS INSTALL AS AN UPDATE TO A BUILD-IN SYSTEM APP
                            String name=packageInfo.versionName;
                            String package_name=packageInfo.packageName;
                            long size=0;
                            String size_formatted=FileUtil.humanReadableByteCount(size,false);
                            appPOJOList.add(new AppPOJO(name,package_name,size_formatted));

                        }
                    }

                }


                asyncTaskStatus=AsyncTaskStatus.COMPLETED;
            }
        }).start();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v=inflater.inflate(R.layout.fragment_app_manager,container,false);
        app_count_textview=v.findViewById(R.id.fragment_app_list_number);
        recyclerView=v.findViewById(R.id.fragment_app_list_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            final int threshold=5;
            public void onScrolled(RecyclerView rv, int dx, int dy)
            {
                super.onScrolled(rv,dx,dy);
                if(scroll_distance>threshold && toolbar_visible)
                {
                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    toolbar_visible=false;
                    scroll_distance=0;
                }
                else if(scroll_distance<-threshold && !toolbar_visible)
                {

                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible=true;
                    scroll_distance=0;
                }

                if((toolbar_visible && dy>0) || (!toolbar_visible && dy<0))
                {
                    scroll_distance+=dy;
                }

            }


        });

        progressBar=v.findViewById(R.id.fragment_app_list_progressbar);

        bottom_toolbar=v.findViewById(R.id.fragment_app_list_bottom_toolbar);

        handler=new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus!=AsyncTaskStatus.COMPLETED)
                {
                    handler.postDelayed(this,500);
                }
                else
                {
                    adapter=new AppListAdapter(appPOJOList);
                    recyclerView.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                    handler.removeCallbacks(this);
                }
            }
        });


        return v;
    }


    private class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.VH>
    {

        List<AppPOJO> appPOJOList;
        AppListAdapter(List<AppPOJO> list)
        {
            appPOJOList=list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.app_manager_recycler_layout,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AppPOJO appPOJO=appPOJOList.get(position);
            GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath()+ File.separator+appPOJO.app_package+".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(holder.app_image);
            holder.app_name.setText(appPOJO.app_name);
            holder.app_package.setText(appPOJO.app_package);
            holder.app_size.setText(appPOJO.app_size);

        }

        @Override
        public int getItemCount() {
            return appPOJOList.size();
        }

        private class VH extends RecyclerView.ViewHolder
        {
            View v;
            ImageView app_image;
            TextView app_name,app_package,app_size;
            public VH(@NonNull View itemView) {
                super(itemView);
                v=itemView;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
            }
        }
    }

    private class AppPOJO
    {
        String app_name;
        String app_package;
        String app_size;

        AppPOJO(String app_name,String app_package,String app_size)
        {
            this.app_name=app_name;
            this.app_package=app_package;
            this.app_size=app_size;
        }

    }
}
