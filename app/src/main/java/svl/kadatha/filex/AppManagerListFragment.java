package svl.kadatha.filex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppManagerListFragment extends Fragment {

    private Context context;
    private String app_type="";
    private RecyclerView recyclerView;
    private FrameLayout progressBar;
    private TextView app_count_textview;
    private Toolbar bottom_toolbar;
    private boolean toolbar_visible=true;
    private int scroll_distance;
    public AsyncTaskStatus asyncTaskStatus;
    private List<AppPOJO> appPOJOList;
    private AppListAdapter adapter;
    private Handler handler;
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public List<AppPOJO> app_selected_array=new ArrayList<>();
    private LinearLayoutManager llm;
    private GridLayoutManager glm;


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
                int flags = PackageManager.GET_META_DATA |
                        PackageManager.GET_SHARED_LIBRARY_FILES |
                        PackageManager.GET_UNINSTALLED_PACKAGES;

                appPOJOList=new ArrayList<>();
                final PackageManager packageManager = context.getPackageManager();
                List<PackageInfo> packageInfos=packageManager.getInstalledPackages(flags);
                for (PackageInfo packageInfo : packageInfos)
                {
                    if(app_type.equals(AppManagerActivity.SYSTEM_APPS))
                    {
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
                        {
                            String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                            String package_name=packageInfo.packageName;
                            File file = new File(packageInfo.applicationInfo.publicSourceDir);
                            long size=file.length();
                            long date=file.lastModified();
                            extract_icon(package_name,packageManager,packageInfo);
                            appPOJOList.add(new AppPOJO(name,package_name,size,date));

                        }
                    }
                    else
                    {
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1)
                        {
                            String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                            String package_name=packageInfo.packageName;
                            File file = new File(packageInfo.applicationInfo.publicSourceDir);
                            long size=file.length();
                            long date=file.lastModified();
                            extract_icon(package_name,packageManager,packageInfo);
                            appPOJOList.add(new AppPOJO(name,package_name,size,date));

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
        if(Global.FILE_GRID_LAYOUT)
        {
            glm=new GridLayoutManager(context,Global.GRID_COUNT);
            SpacesItemDecoration spacesItemDecoration=new SpacesItemDecoration(Global.ONE_DP);
            recyclerView.addItemDecoration(spacesItemDecoration);
            recyclerView.setLayoutManager(glm);
        }
        else
        {
            llm=new LinearLayoutManager(context);
            recyclerView.setLayoutManager(llm);
        }
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
        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,2,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.search_icon,R.drawable.sort_icon};
        String [] titles={getString(R.string.search),getString(R.string.sort)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);
        bottom_toolbar.addView(tb_layout);

        Button search_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button sort_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);

        ToolBarClickListener toolBarClickListener = new ToolBarClickListener();

        search_btn.setOnClickListener(toolBarClickListener);
        sort_btn.setOnClickListener(toolBarClickListener);

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
                    Collections.sort(appPOJOList,FileComparator.AppPOJOComparate(Global.APP_MANAGER_SORT));
                    adapter=new AppListAdapter(appPOJOList);
                    recyclerView.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                    app_count_textview.setText(""+appPOJOList.size());
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

    public void clear_selection()
    {
        app_selected_array=new ArrayList<>();
        mselecteditems=new SparseBooleanArray();
        if (adapter!=null) adapter.notifyDataSetChanged();
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
            return new VH(new AppsInstalledRecyclerViewLayout(context));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AppPOJO appPOJO=appPOJOs.get(position);
            boolean selected=mselecteditems.get(position,false);
            holder.v.setData(appPOJO,selected);
            holder.v.setSelected(selected);

        }

        @Override
        public int getItemCount() {
            return appPOJOs.size();
        }

        private class VH extends RecyclerView.ViewHolder
        {
            AppsInstalledRecyclerViewLayout v;
            int pos;
            public VH(@NonNull AppsInstalledRecyclerViewLayout itemView) {
                super(itemView);
                v=itemView;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clear_selection();
                    }
                });

                v.appselect_indicator.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pos=getBindingAdapterPosition();
                        if(mselecteditems.size()>0)
                        {
                            if (!mselecteditems.get(pos, false)) {
                                clear_selection();
                                mselecteditems.put(pos,true);
                                app_selected_array.add(appPOJOs.get(pos));
                                v.setSelected(true);
                            }
                            else
                            {
                                clear_selection();
                            }
                        }
                        else
                        {
                            mselecteditems.put(pos,true);
                            app_selected_array.add(appPOJOs.get(pos));
                            v.setSelected(true);

                        }
                    }
                });

            }
        }
    }


    private class ToolBarClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View p1) {
            // TODO: Implement this method

            int id = p1.getId();
            clear_selection();
            if (id == R.id.toolbar_btn_1) {
                //((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                if (!((AppManagerActivity) context).search_toolbar_visible) {
                    //((AppManagerActivity) context).set_visibility_searchbar(true);
                }

            } else if (id == R.id.toolbar_btn_2) {
                //((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AppManagerActivity) context).search_edittext.getWindowToken(), 0);
                AppManagerSortDialog appManagerSortDialog=new AppManagerSortDialog();
                appManagerSortDialog.show(((AppManagerActivity)context).fm,"");
            }
        }

    }


/*
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
            GlideApp.with(context).load(Global.APK_ICON_DIR.getAbsolutePath()+ File.separator+appPOJO.getPackage()+".png").placeholder(R.drawable.apk_file_icon).error(R.drawable.apk_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(holder.app_image);
            holder.app_name.setText(appPOJO.getName());
            holder.app_package.setText(appPOJO.getPackage());
            holder.app_size.setText(appPOJO.getSize());

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

 */

    public class AppPOJO
    {
        private final String name;
        private final String lower_name;
        private final String package_name;
        private final long sizeLong;
        private final String size;
        private final long dateLong;
        private final String date;


        AppPOJO(String app_name,String app_package,long app_size_long,long app_date_long)
        {
            this.name=app_name;
            this.lower_name=app_name.toLowerCase();
            this.package_name=app_package;
            this.sizeLong=app_size_long;
            this.size=FileUtil.humanReadableByteCount(app_size_long,Global.BYTE_COUNT_BLOCK_1000);
            this.dateLong=app_date_long;
            this.date=Global.SDF.format(dateLong);
        }

        public String getName(){return this.name;}

        public String getLowerName(){ return this.lower_name;}

        public String getPackage_name(){ return this.package_name;}

        public long getSizeLong(){return this.sizeLong;}

        public String getSize(){return this.size;}

        public long getDateLong(){ return this.dateLong;}

        public String getDate(){return this.date;}

    }
}
