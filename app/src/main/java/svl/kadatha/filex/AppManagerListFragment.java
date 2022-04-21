package svl.kadatha.filex;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private List<AppPOJO> appPOJOList,total_appPOJO_list;
    private AppListAdapter adapter;
    private Handler handler;
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public List<AppPOJO> app_selected_array=new ArrayList<>();
    private AppManagerActivity.SearchFilterListener searchFilterListener;
    private int num_all_app;
    private TextView empty_tv;
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    public static String BACKUP;
    public static String UNINSTALL;
    public static String CONTROL_PANEL;
    public static String PLAY_STORE;
    public static String SHARE;
    private String package_clicked_for_delete="";

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
        BACKUP=getString(R.string.backup);
        UNINSTALL=getString(R.string.uninstall);
        CONTROL_PANEL=getString(R.string.control_panel);
        PLAY_STORE=getString(R.string.play_store);
        SHARE=getString(R.string.share);

        new Thread(new Runnable() {
            @Override
            public void run() {
                asyncTaskStatus=AsyncTaskStatus.STARTED;
                int flags = PackageManager.GET_META_DATA |
                        PackageManager.GET_SHARED_LIBRARY_FILES |
                        PackageManager.GET_UNINSTALLED_PACKAGES;

                appPOJOList=new ArrayList<>();
                total_appPOJO_list=new ArrayList<>();
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
                            String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
                            if(publicsourcedir==null)
                            {
                                continue;
                            }
                            File file = new File(publicsourcedir);
                            String path=file.getAbsolutePath();
                            long size=file.length();
                            long date=file.lastModified();
                            extract_icon(package_name+".png",packageManager,packageInfo);
                            appPOJOList.add(new AppPOJO(name, package_name, path, size, date));

                        }
                    }
                    else
                    {
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1)
                        {
                            String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                            String package_name=packageInfo.packageName;
                            String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
                            if(publicsourcedir==null)
                            {
                                continue;
                            }
                            File file = new File(publicsourcedir);
                            String path=file.getAbsolutePath();
                            long size=file.length();
                            long date=file.lastModified();
                            extract_icon(package_name+".png",packageManager,packageInfo);
                            appPOJOList.add(new AppPOJO(name, package_name, path, size, date));

                        }
                    }

                }
                total_appPOJO_list=appPOJOList;
                asyncTaskStatus=AsyncTaskStatus.COMPLETED;
            }
        }).start();

        list_popupwindowpojos=new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v=inflater.inflate(R.layout.fragment_app_manager,container,false);
        app_count_textview=v.findViewById(R.id.fragment_app_list_number);
        recyclerView=v.findViewById(R.id.fragment_app_list_recyclerview);
        if(Global.FILE_GRID_LAYOUT)
        {
            GridLayoutManager glm = new GridLayoutManager(context, Global.GRID_COUNT);
            SpacesItemDecoration spacesItemDecoration=new SpacesItemDecoration(Global.ONE_DP);
            recyclerView.addItemDecoration(spacesItemDecoration);
            recyclerView.setLayoutManager(glm);
        }
        else
        {
            LinearLayoutManager llm = new LinearLayoutManager(context);
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

        empty_tv=v.findViewById(R.id.fragment_app_list_empty);
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
                    adapter=new AppListAdapter();
                    recyclerView.setAdapter(adapter);
                    progressBar.setVisibility(View.GONE);
                    num_all_app=total_appPOJO_list.size();
                    app_count_textview.setText(""+num_all_app);
                    if(num_all_app<=0)
                    {
                        recyclerView.setVisibility(View.GONE);
                        empty_tv.setVisibility(View.VISIBLE);
                        //enable_disable_buttons(false);
                    }
                    handler.removeCallbacks(this);
                }
            }
        });

        listPopWindow=new PopupWindow(context);
        ListView listView=new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapater(context,list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
        int listview_height = Global.GET_HEIGHT_LIST_VIEW(listView);
        listView.setOnItemClickListener(new ListPopupWindowClickListener());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        searchFilterListener=new AppManagerActivity.SearchFilterListener() {
            @Override
            public void onSearchFilter(String constraint) {
                adapter.getFilter().filter(constraint);
            }
        };
        ((AppManagerActivity)context).addSearchFilterListener(searchFilterListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(((AppManagerActivity)context).search_toolbar_visible)
        {
            ((AppManagerActivity)context).set_visibility_searchbar(false);
        }

        ((AppManagerActivity)context).removeSearchFilterListener(searchFilterListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listPopWindow.dismiss();
    }

    private void extract_icon(String file_with_package_name, PackageManager packageManager, PackageInfo packageInfo)
    {
        if(!Global.APK_ICON_PACKAGE_NAME_LIST.contains(file_with_package_name))
        {
            Drawable APKicon = packageInfo.applicationInfo.loadIcon(packageManager);
            Bitmap bitmap;
            if(APKicon instanceof BitmapDrawable)
            {
                bitmap=((BitmapDrawable)APKicon).getBitmap();
            }
            else
            {
                bitmap = Bitmap.createBitmap(APKicon.getIntrinsicWidth(),APKicon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                APKicon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                APKicon.draw(canvas);
            }

            File f=new File(Global.APK_ICON_DIR,file_with_package_name);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream=new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.PNG,100,fileOutputStream);
                fileOutputStream.close();
                Global.APK_ICON_PACKAGE_NAME_LIST.add(file_with_package_name);
            } catch (IOException e) {
                if(fileOutputStream!=null)
                {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ioException) {

                    }
                }
            }

        }

    }


    public void remove_app(String package_name)
    {
        for(AppPOJO a:appPOJOList)
        {
            if(a.getPackage_name().equals(package_name))
            {
                appPOJOList.remove(a);
                total_appPOJO_list.remove(a);
                break;
            }
        }
        num_all_app=total_appPOJO_list.size();
        clear_selection();
    }


    public void clear_selection()
    {
        app_selected_array=new ArrayList<>();
        mselecteditems=new SparseBooleanArray();
        if (adapter!=null) adapter.notifyDataSetChanged();
        if(num_all_app<=0)
        {
            recyclerView.setVisibility(View.GONE);
            empty_tv.setVisibility(View.VISIBLE);
        }
        app_count_textview.setText(""+num_all_app);
    }


    private class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.VH> implements Filterable
    {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(new AppsInstalledRecyclerViewLayout(context));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AppPOJO appPOJO=appPOJOList.get(position);
            boolean selected=mselecteditems.get(position,false);
            holder.v.setData(appPOJO,selected);
            holder.v.setSelected(selected);

        }

        @Override
        public int getItemCount() {
            return appPOJOList.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    appPOJOList=new ArrayList<>();
                    if(constraint==null || constraint.length()==0)
                    {
                        appPOJOList=total_appPOJO_list;
                    }
                    else
                    {
                        String pattern=constraint.toString().toLowerCase().trim();
                        for(int i=0;i<num_all_app;++i)
                        {
                            AppPOJO appPOJO=total_appPOJO_list.get(i);
                            if(appPOJO.getLowerName().contains(pattern))
                            {
                                appPOJOList.add(appPOJO);
                            }
                        }
                    }
                    return new FilterResults();
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {

                    int t=appPOJOList.size();
                    if(mselecteditems.size()>0)
                    {
                        clear_selection();
                    }
                    else
                    {
                        notifyDataSetChanged();
                    }
                    app_count_textview.setText(""+t);
                }
            };
        }

        private class VH extends RecyclerView.ViewHolder
        {
            final AppsInstalledRecyclerViewLayout v;
            int pos;
            public VH(@NonNull AppsInstalledRecyclerViewLayout itemView) {
                super(itemView);
                v=itemView;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pos=getBindingAdapterPosition();
                        if(mselecteditems.size()>0)
                        {
                            if (!mselecteditems.get(pos, false)) {
                                clear_selection();
                                mselecteditems.put(pos,true);
                                app_selected_array.add(appPOJOList.get(pos));
                                v.setSelected(true);
                                show_app_action_select_dialog(appPOJOList.get(pos));
                            }
                            else
                            {
                                clear_selection();
                            }
                        }
                        else
                        {
                            mselecteditems.put(pos,true);
                            app_selected_array.add(appPOJOList.get(pos));
                            v.setSelected(true);
                            show_app_action_select_dialog(appPOJOList.get(pos));
                        }
                    }
                });

            }
        }
    }

    private void show_app_action_select_dialog(AppPOJO appPOJO)
    {
        AppActionSelectDialog appActionSelectDialog=AppActionSelectDialog.getInstance(appPOJO.getName(),appPOJO.getPackage_name(),appPOJO.getSize());
        appActionSelectDialog.setAppActionSelectListener(new AppActionSelectDialog.AppActionSelectListener() {
            @Override
            public void onSelectType(String app_action) {
                if (BACKUP.equals(app_action)) {
                    MoveToCopyToProcedure(appPOJO.getPath());
                } else if (UNINSTALL.equals(app_action)) {
                    String app_pkg_name = appPOJO.getPackage_name();
                    if (package_clicked_for_delete.equals("")) {
                        package_clicked_for_delete = app_pkg_name;
                    }
                    Intent uninstall_intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                    uninstall_intent.setData(Uri.parse("package:" + app_pkg_name));
                    unInstallActivityResultLauncher.launch(uninstall_intent);
                } else if (CONTROL_PANEL.equals(app_action)) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", appPOJO.getPackage_name(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } else if (PLAY_STORE.equals(app_action)) {
                    final String appPackageName = appPOJO.getPackage_name();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                } else if (SHARE.equals(app_action)) {

                    try {
                        PackageManager pm = context.getPackageManager();
                        ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), 0);
                        File srcFile = new File(ai.publicSourceDir);
                        Uri uri= FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",new File(srcFile.getPath()));
                        FileIntentDispatch.sendUri(context, new ArrayList<>(Collections.singletonList(uri)));

                    } catch (Exception e) {
                        print(getString(R.string.could_not_perform_action));
                    }

                }
                clear_selection();
            }
        });
        appActionSelectDialog.show(((AppManagerActivity)context).fm,"");
    }


    private final ActivityResultLauncher<Intent> unInstallActivityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(isPackageExisted(package_clicked_for_delete))
            {
                print(getString(R.string.could_not_be_uninstalled));
            }
            else
            {
                remove_app(package_clicked_for_delete);
                print(getString(R.string.uninstalled));
            }

            package_clicked_for_delete="";
        }
    });

    private final ActivityResultLauncher<Intent>activityResultLauncher_file_select=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK)
            {
                Bundle bundle = result.getData().getBundleExtra("bundle");
                ArrayList<String> files_selected_array=new ArrayList<>(bundle.getStringArrayList("files_selected_array"));
                boolean cut=bundle.getBoolean("cut");
                String source_folder=bundle.getString("source_folder");
                String dest_folder=bundle.getString("dest_folder");
                FileObjectType sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
                FileObjectType destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
                if (sourceFileObjectType.equals(destFileObjectType) && source_folder.equals(dest_folder)) {
                    print(!cut ? getString(R.string.selected_files_have_been_copied) : getString(R.string.selected_filed_have_been_moved));
                }

                else
                {
                    PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder, files_selected_array, sourceFileObjectType,
                            destFileObjectType, dest_folder, cut);
                    pasteSetUpDialog.show(((AppManagerActivity)context).fm, "paste_dialog");

                }
            }
        }
    });



    public boolean isPackageExisted(String targetPackage){
        PackageManager pm=context.getPackageManager();
        try {
            PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private void MoveToCopyToProcedure(String file_path)
    {
        Bundle bundle=new Bundle();
        ArrayList<String>files_selected_array=new ArrayList<>();
        files_selected_array.add(file_path);
        bundle.putString("source_folder", new File(file_path).getParent());
        bundle.putStringArrayList("files_selected_array", files_selected_array);
        bundle.putSerializable("sourceFileObjectType", FileObjectType.FILE_TYPE);
        bundle.putBoolean("cut", false);

        Intent intent=new Intent(context,FileSelectorActivity.class);
        intent.putExtra("bundle",bundle);
        intent.putExtra(FileSelectorActivity.ACTION_SOUGHT,FileSelectorActivity.MOVE_COPY_REQUEST_CODE);
        activityResultLauncher_file_select.launch(intent);

    }


    private void send_uri(Uri uri,String subject)
    {
        Intent intent=new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM,uri);
        intent.putExtra(Intent.EXTRA_SUBJECT,subject);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chooser=Intent.createChooser(intent,"Select app");
        if(intent.resolveActivity(context.getPackageManager())!=null)
        {
            context.startActivity(chooser);
        }
    }


    private class ListPopupWindowClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
        {
            // TODO: Implement this method
            final Bundle bundle=new Bundle();
            final ArrayList<String> files_selected_array=new ArrayList<>();
            if (app_selected_array.size() < 1) {
                return;
            }

            switch(p3)
            {
                case 0:

                    break;
                case 1:
                    break;

                case 2:
                    for(AppPOJO app:app_selected_array)
                    {
                        files_selected_array.add(app.getPath());
                    }

                    PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,FileObjectType.FILE_TYPE);
                    propertiesDialog.show(((AppManagerActivity)context).getSupportFragmentManager(),"properties_dialog");
                    break;
                default:
                    break;

            }

            listPopWindow.dismiss();
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
                ((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                if(!((AppManagerActivity)context).search_toolbar_visible)
                {
                    ((AppManagerActivity) context).set_visibility_searchbar(true);
                }

            } else if (id == R.id.toolbar_btn_2) {
                ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AppManagerActivity) context).search_edittext.getWindowToken(), 0);
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

    public static class AppPOJO
    {
        private final String name;
        private final String lower_name;
        private final String package_name;
        private final String path;
        private final long sizeLong;
        private final String size;
        private final long dateLong;
        private final String date;


        AppPOJO(String app_name,String app_package,String app_path,long app_size_long,long app_date_long)
        {
            this.name=app_name;
            this.lower_name=app_name.toLowerCase();
            this.package_name=app_package;
            this.path=app_path;
            this.sizeLong=app_size_long;
            this.size=FileUtil.humanReadableByteCount(app_size_long,Global.BYTE_COUNT_BLOCK_1000);
            this.dateLong=app_date_long;
            this.date=Global.SDF.format(dateLong);
        }

        public String getName(){return this.name;}

        public String getLowerName(){ return this.lower_name;}

        public String getPackage_name(){ return this.package_name;}

        public String getPath() {return this.path;}

        public long getSizeLong(){return this.sizeLong;}

        public String getSize(){return this.size;}

        public long getDateLong(){ return this.dateLong;}

        public String getDate(){return this.date;}

    }

    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
