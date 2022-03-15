package svl.kadatha.filex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import java.io.FileOutputStream;
import java.io.IOException;
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
                List<PackageInfo> packageInfos=packageManager.getInstalledPackages(0);
                for (PackageInfo packageInfo : packageInfos)
                {
                    if(app_type.equals(AppManagerActivity.SYSTEM_APPS))
                    {
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        {
                            String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                            String package_name=packageInfo.packageName;
                            File file = new File(packageInfo.applicationInfo.publicSourceDir);
                            long size=file.length();
                            String size_formatted=FileUtil.humanReadableByteCount(size,false);
                            extract_icon(package_name,packageManager,packageInfo);
                            appPOJOList.add(new AppPOJO(name,package_name,size_formatted));

                        }
                    }
                    else
                    {
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) != 0)
                        {
                            String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                            String package_name=packageInfo.packageName;
                            File file = new File(packageInfo.applicationInfo.publicSourceDir);
                            long size=file.length();
                            String size_formatted=FileUtil.humanReadableByteCount(size,false);
                            extract_icon(package_name,packageManager,packageInfo);
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

    private void extract_icon(String file_with_package_name,PackageManager packageManager,PackageInfo packageInfo)
    {
        if(!Global.APK_ICON_PACKAGE_NAME_LIST.contains(file_with_package_name))
        {
            Drawable APKicon = packageInfo.applicationInfo.loadIcon(packageManager);
            if(APKicon instanceof BitmapDrawable)
            {
                Bitmap bm=((BitmapDrawable)APKicon).getBitmap();
                File f=new File(Global.APK_ICON_DIR,file_with_package_name);
                try {
                    FileOutputStream fileOutputStream=new FileOutputStream(f);
                    bm.compress(Bitmap.CompressFormat.PNG,100,fileOutputStream);
                    fileOutputStream.close();
                    Global.APK_ICON_PACKAGE_NAME_LIST.add(file_with_package_name);
                } catch (IOException e) {

                }

            }

        }

    }


    private class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.VH>
    {

        List<AppPOJO> appPOJOs;
        AppListAdapter(List<AppPOJO> list)
        {
            appPOJOs=list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.app_manager_recycler_layout,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {



            AppPOJO appPOJO=appPOJOs.get(position);
            /*
            if(appPOJO==null)
            {
                Log.d("shankar","apppojo is null");
            }
            else
            {
                Log.d("shankar","apppojo is not null "+appPOJO.getApp_name());
                Log.d("shankar","apppojo is not null "+appPOJO.app_package);
            }
            Log.d("shankar","size is "+appPOJOList.size());

             */
            GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath()+ File.separator+appPOJO.getApp_package()+".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(holder.app_image);
            holder.app_name.setText(appPOJO.getApp_name());
            holder.app_package.setText(appPOJO.getApp_package());
            holder.app_size.setText(appPOJO.getApp_size());

        }

        @Override
        public int getItemCount() {
            return appPOJOs.size();
        }

        private class VH extends RecyclerView.ViewHolder
        {
            View v;
            ImageView app_image;
            TextView app_name,app_package,app_size;

            int imageview_dimension,first_line_font_size,second_line_font_size;

            public VH(@NonNull View itemView) {
                super(itemView);
                v=itemView;
                app_image=v.findViewById(R.id.app_manager_app_image);
                app_name=v.findViewById(R.id.app_manager_app_name);
                app_package=v.findViewById(R.id.app_manager_app_package);
                app_size=v.findViewById(R.id.app_manager_size);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });

                if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
                {
                    first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
                    second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
                    imageview_dimension=Global.IMAGEVIEW_DIMENSION_SMALL_LIST;

                }
                else if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
                {
                    first_line_font_size =Global.FONT_SIZE_LARGE_FIRST_LINE;
                    second_line_font_size =Global.FONT_SIZE_LARGE_DETAILS_LINE;
                    imageview_dimension=Global.IMAGEVIEW_DIMENSION_LARGE_LIST;
                }
                else
                {
                    first_line_font_size =Global.FONT_SIZE_MEDIUM_FIRST_LINE;
                    second_line_font_size =Global.FONT_SIZE_MEDIUM_DETAILS_LINE;
                    imageview_dimension=Global.IMAGEVIEW_DIMENSION_MEDIUM_LIST;
                }

                app_name.setTextSize(first_line_font_size);
                app_package.setTextSize(second_line_font_size);
                app_size.setTextSize(second_line_font_size);

            }
        }
    }

    private class AppPOJO
    {
        private final String app_name;
        private final String app_package;
        private final String app_size;

        AppPOJO(String app_name,String app_package,String app_size)
        {
            this.app_name=app_name;
            this.app_package=app_package;
            this.app_size=app_size;
        }

        public String getApp_name()
        {
            return this.app_name;
        }

        public String getApp_package()
        {
            return this.app_package;
        }

        public String getApp_size()
        {
            return this.app_size;
        }

    }
}
