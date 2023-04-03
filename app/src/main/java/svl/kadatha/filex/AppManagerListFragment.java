package svl.kadatha.filex;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

public class AppManagerListFragment extends Fragment {

    private Context context;
    private String app_type;
    public RecyclerView recyclerView;
    public FrameLayout progress_bar;
    private TextView app_count_textview;
    private Toolbar bottom_toolbar;
    private boolean toolbar_visible=true;
    private int scroll_distance;
    public AsyncTaskStatus asyncTaskStatus;
    public List<AppPOJO> appPOJOList,total_appPOJO_list;
    public AppListAdapter adapter;

    private AppManagerActivity.SearchFilterListener searchFilterListener;
    public int num_all_app;
    private TextView empty_tv;
    private String tree_uri_path="";
    private Uri tree_uri;
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    public static String BACKUP;
    public static String UNINSTALL;
    public static String CONTROL_PANEL;
    public static String PLAY_STORE;
    public static String SHARE;
    private String package_clicked_for_delete="";
    private AppManagerListViewModel viewModel;
    private AppManagerListFragmentViewModel appManagerListFragmentViewModel;
    public static final String APP_ACTION_REQUEST_CODE="app_action_request_code";
    private final static String SAF_PERMISSION_REQUEST_CODE="back_up_apk_saf_permission_request_code";
    private final static String APK_REPLACEMENT_REQUEST_CODE="apk_replace_request_code";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            app_type=bundle.getString(AppManagerActivity.APP_TYPE);
        }
        BACKUP=getString(R.string.backup);
        UNINSTALL=getString(R.string.uninstall);
        CONTROL_PANEL=getString(R.string.control_panel);
        PLAY_STORE=getString(R.string.play_store);
        SHARE=getString(R.string.share);

        list_popupwindowpojos=new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete),1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send),2));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties),3));

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v=inflater.inflate(R.layout.fragment_app_manager,container,false);
        app_count_textview=v.findViewById(R.id.fragment_app_list_number);
        recyclerView=v.findViewById(R.id.fragment_app_list_recyclerview);
        if(AppManagerActivity.FILE_GRID_LAYOUT)
        {
            GridLayoutManager glm = new GridLayoutManager(context, Global.GRID_COUNT);
            recyclerView.setLayoutManager(glm);
            int top_padding=recyclerView.getPaddingTop();
            int bottom_padding=recyclerView.getPaddingBottom();
            recyclerView.setPadding(Global.RECYCLERVIEW_ITEM_SPACING,top_padding,Global.RECYCLERVIEW_ITEM_SPACING,bottom_padding);
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
        progress_bar=v.findViewById(R.id.fragment_app_list_progressbar);

        bottom_toolbar=v.findViewById(R.id.fragment_app_list_bottom_toolbar);
        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,3,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.search_icon,R.drawable.view_icon,R.drawable.scan_icon};
        String [] titles={getString(R.string.search),getString(R.string.view),getString(R.string.rescan)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);
        bottom_toolbar.addView(tb_layout);

        Button search_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button view_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button refresh_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);

        ToolBarClickListener toolBarClickListener = new ToolBarClickListener();

        search_btn.setOnClickListener(toolBarClickListener);
        view_btn.setOnClickListener(toolBarClickListener);
        refresh_btn.setOnClickListener(toolBarClickListener);

        viewModel= new ViewModelProvider(requireActivity()).get(AppManagerListViewModel.class);
        viewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
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
                    if(app_type.equals(AppManagerActivity.SYSTEM_APPS))
                    {
                        appPOJOList=viewModel.systemAppPOJOList;
                    }
                    else if(app_type.equals(AppManagerActivity.USER_INSTALLED_APPS))
                    {
                        appPOJOList=viewModel.userAppPOJOList;
                    }

                    total_appPOJO_list=appPOJOList;
                    Collections.sort(appPOJOList,FileComparator.AppPOJOComparate(Global.APP_MANAGER_SORT));
                    adapter=new AppListAdapter();
                    recyclerView.setAdapter(adapter);
                    num_all_app=total_appPOJO_list.size();
                    app_count_textview.setText(""+num_all_app);
                    if(num_all_app<=0)
                    {
                        recyclerView.setVisibility(View.GONE);
                        empty_tv.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        appManagerListFragmentViewModel=new ViewModelProvider(this).get(AppManagerListFragmentViewModel.class);

        listPopWindow=new PopupWindow(context);
        ListView listView=new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context,list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
        int listview_height = Global.GET_HEIGHT_LIST_VIEW(listView);
        listView.setOnItemClickListener(new ListPopupWindowClickListener());

        ((AppManagerActivity)context).getSupportFragmentManager().setFragmentResultListener(APP_ACTION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(!requestKey.equals(APP_ACTION_REQUEST_CODE)) return;
                String app_action=result.getString("app_action");
                String app_path=result.getString("app_path");
                String package_name=result.getString("package_name");
                if (BACKUP.equals(app_action)) {
                    MoveToCopyToProcedure(app_path,result);
                } else if (UNINSTALL.equals(app_action)) {
                    if (package_clicked_for_delete.equals("")) {
                        package_clicked_for_delete = package_name;
                    }
                    Intent uninstall_intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                    uninstall_intent.setData(Uri.parse("package:" + package_name));
                    unInstallActivityResultLauncher.launch(uninstall_intent);
                } else if (CONTROL_PANEL.equals(app_action)) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", package_name, null);
                    intent.setData(uri);
                    startActivity(intent);
                } else if (PLAY_STORE.equals(app_action)) {
                    final String appPackageName = package_name;
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                } else if (SHARE.equals(app_action)) {

                    Uri uri= FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",new File(app_path));
                    FileIntentDispatch.sendUri(context, new ArrayList<>(Collections.singletonList(uri)));
                    /*
                    try {
                        PackageManager pm = context.getPackageManager();
                        ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), 0);
                        File srcFile = new File(ai.publicSourceDir);
                        Uri uri= FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",new File(srcFile.getPath()));
                        FileIntentDispatch.sendUri(context, new ArrayList<>(Collections.singletonList(uri)));

                    } catch (Exception e) {
                        Global.print(context,getString(R.string.could_not_perform_action));
                    }

                     */

                }
                clear_selection();
            }
        });


        ((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(APK_REPLACEMENT_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(APK_REPLACEMENT_REQUEST_CODE))
                {
                    back_up(result);
                }
            }
        });

        ((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE))
                {
                    tree_uri=result.getParcelable("tree_uri");
                    tree_uri_path=result.getString("tree_uri_path");
                    back_up(result);
                }

            }
        });

        viewModel.isBackedUp.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
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
                    viewModel.isBackedUp.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    Global.print(context,getString(R.string.copied_apk_file));
                }
            }
        });

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


    public static void extract_icon(String file_with_package_name, PackageManager packageManager, PackageInfo packageInfo)
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
        Iterator<AppPOJO> iterator=viewModel.userAppPOJOList.iterator();
        while (iterator.hasNext())
        {
            AppPOJO appPOJO=iterator.next();
            if(appPOJO.package_name.equals(package_name))
            {
                iterator.remove();
                break;
            }
        }

        Iterator<AppPOJO> iterator1=viewModel.systemAppPOJOList.iterator();
        while (iterator1.hasNext())
        {
            AppPOJO appPOJO=iterator1.next();
            if(appPOJO.package_name.equals(package_name))
            {
                iterator1.remove();
                break;
            }
        }
        ((AppManagerActivity)context).refresh_fragment_on_uninstall();
    }


    public void clear_selection()
    {
        appManagerListFragmentViewModel.app_selected_array=new ArrayList<>();
        appManagerListFragmentViewModel.mselecteditems=new SparseBooleanArray();
        if (adapter!=null)
        {
            adapter.notifyDataSetChanged();
        }

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
            if(AppManagerActivity.FILE_GRID_LAYOUT)
            {
                return new VH(new AppInstalledRecyclerViewLayoutGrid(context));
            }else
            {
                return new VH(new AppInstalledRecyclerViewLayoutList(context));
            }

        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AppPOJO appPOJO=appPOJOList.get(position);
            boolean selected=appManagerListFragmentViewModel.mselecteditems.get(position,false);
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
                    if(appManagerListFragmentViewModel.mselecteditems.size()>0)
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
            final AppInstalledRecyclerViewLayout v;
            int pos;
            public VH(@NonNull AppInstalledRecyclerViewLayout itemView) {
                super(itemView);
                v=itemView;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pos=getBindingAdapterPosition();
                        if(appManagerListFragmentViewModel.mselecteditems.size()>0)
                        {
                            if (!appManagerListFragmentViewModel.mselecteditems.get(pos, false)) {
                                clear_selection();
                                appManagerListFragmentViewModel.mselecteditems.put(pos,true);
                                appManagerListFragmentViewModel.app_selected_array.add(appPOJOList.get(pos));
                                v.setSelected(true);
                                //show_app_action_select_dialog(appPOJOList.get(pos));
                            }
                            else
                            {
                                clear_selection();
                            }
                        }
                        else
                        {
                            appManagerListFragmentViewModel.mselecteditems.put(pos,true);
                            appManagerListFragmentViewModel.app_selected_array.add(appPOJOList.get(pos));
                            v.setSelected(true);
                            //show_app_action_select_dialog(appPOJOList.get(pos));
                        }
                        show_app_action_select_dialog(appPOJOList.get(pos));
                    }
                });

            }
        }
    }

    private void show_app_action_select_dialog(AppPOJO appPOJO)
    {
        if(((AppManagerActivity)context).search_toolbar_visible)
        {
            ((AppManagerActivity)context).set_visibility_searchbar(false);
        }
        AppActionSelectDialog appActionSelectDialog=AppActionSelectDialog.getInstance(appPOJO.getName(),appPOJO.getPackage_name(),appPOJO.getSize(),appPOJO.getVersion(),appPOJO.getPath());
        appActionSelectDialog.show(((AppManagerActivity)context).fm,"");
    }


    private final ActivityResultLauncher<Intent> unInstallActivityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(isPackageExisted(package_clicked_for_delete))
            {
                Global.print(context,getString(R.string.could_not_be_uninstalled));
            }
            else
            {
                remove_app(package_clicked_for_delete);
                Global.print(context,getString(R.string.uninstalled));
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
                String dest_folder=bundle.getString("dest_folder");
                FileObjectType destFileObjectType= (FileObjectType) bundle.getSerializable("destFileObjectType");
                String new_name=bundle.getString("new_name");
                File file=new File(dest_folder,new_name);
                String file_path=file.getAbsolutePath();
                bundle.putString("file_path",dest_folder);
                bundle.putSerializable("fileObjectType",destFileObjectType);
                if(whether_file_already_exists(file_path,destFileObjectType,bundle))
                {
                    ArchiveReplaceConfirmationDialog archiveReplaceConfirmationDialog=ArchiveReplaceConfirmationDialog.getInstance(APK_REPLACEMENT_REQUEST_CODE,bundle);
                    archiveReplaceConfirmationDialog.show(((AppCompatActivity)context).getSupportFragmentManager(),null);
                }
                else
                {
                    back_up(bundle);
                }

            }
        }
    });


    private void back_up(Bundle bundle)
    {
        String app_path=bundle.getString("app_path");
        String dest_folder=bundle.getString("dest_folder");
        FileObjectType destFileObjectType= (FileObjectType) bundle.getSerializable("destFileObjectType");
        String new_name=bundle.getString("new_name");
        bundle.putString("file_path",dest_folder);
        bundle.putSerializable("fileObjectType",destFileObjectType);
        if(is_file_writable(dest_folder,destFileObjectType,bundle))
        {
            progress_bar.setVisibility(View.VISIBLE);
            viewModel.back_up(new ArrayList<>(Collections.singletonList(app_path)),dest_folder,destFileObjectType,Collections.singletonList(new_name),tree_uri,tree_uri_path);
        }
    }

    private boolean whether_file_already_exists(String new_file_path,FileObjectType fileObjectType,Bundle bundle)
    {
        if(fileObjectType== FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
        {
            File new_file=new File(new_file_path);
            return new_file.exists();

        }
        else if(fileObjectType== FileObjectType.USB_TYPE)
        {
            UsbFile usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,new_file_path);
            return usbFile != null;

        }
        else if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            if(RootUtils.CAN_RUN_ROOT_COMMANDS())
            {
                return !RootUtils.WHETHER_FILE_EXISTS(new_file_path);
            }
            else
            {
                Global.print(context,getString(R.string.root_access_not_avaialable));
                return false;
            }

        }
        else
        {
            if(check_SAF_permission(new_file_path,fileObjectType,bundle))
            {
                return FileUtil.exists(context, new_file_path, tree_uri, tree_uri_path);
            }
            else
            {
                return false;
            }
        }

    }

    private boolean is_file_writable(String file_path,FileObjectType fileObjectType,Bundle bundle)
    {
        if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            boolean isWritable;
            isWritable=FileUtil.isWritable(fileObjectType,file_path);
            if(isWritable)
            {
                return true;
            }
            else
            {
                return check_SAF_permission(file_path,fileObjectType,bundle);
            }
        }
        else if(fileObjectType==FileObjectType.FTP_TYPE)
        {
            return false;
        }
        else return fileObjectType == FileObjectType.USB_TYPE;

    }

    private boolean check_SAF_permission(String new_file_path,FileObjectType fileObjectType,Bundle bundle)
    {
        UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(new_file_path,fileObjectType);
        if(uriPOJO!=null)
        {
            tree_uri_path=uriPOJO.get_path();
            tree_uri=uriPOJO.get_uri();
        }

        if(uriPOJO==null || tree_uri_path.equals(""))
        {
            SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,bundle);
            safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
            return false;
        }
        else
        {
            return true;
        }
    }


    public boolean isPackageExisted(String targetPackage){
        PackageManager pm=context.getPackageManager();
        try {
            PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private void MoveToCopyToProcedure(String file_path, Bundle bundle)
    {
        ((AppManagerActivity)context).clear_cache=false;
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
            if (appManagerListFragmentViewModel.app_selected_array.size() < 1) {
                return;
            }

            switch(p3)
            {
                case 0:

                    break;
                case 1:
                    break;

                case 2:
                    for(AppPOJO app:appManagerListFragmentViewModel.app_selected_array)
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

                if(!((AppManagerActivity)context).search_toolbar_visible)
                {
                    ((AppManagerActivity) context).set_visibility_searchbar(true);
                }
                else
                {
                    ((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }

            } else if (id == R.id.toolbar_btn_2) {
                ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AppManagerActivity) context).search_edittext.getWindowToken(), 0);
                AppManagerSortDialog appManagerSortDialog=new AppManagerSortDialog();
                appManagerSortDialog.show(((AppManagerActivity)context).fm,"");
            }
            else if(id==R.id.toolbar_btn_3)
            {
                if(progress_bar.getVisibility()==View.VISIBLE || !Global.APP_POJO_HASHMAP.containsKey("system"))
                {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }



                Global.APP_POJO_HASHMAP.clear();
//                //progress_bar.setVisibility(View.VISIBLE);
//                ViewPager viewPager=((AppManagerActivity)context).viewPager;
//                viewPager.getAdapter().notifyDataSetChanged();
//                //progress_bar.setVisibility(View.VISIBLE);
                ((AppManagerActivity)context).refresh_adapter();
////                viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
////
////
////                viewModel.populateApps();
           }
        }

    }


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
        private final String version;


        AppPOJO(String app_name,String app_package,String app_path,long app_size_long,long app_date_long, String version)
        {
            this.name=app_name;
            this.lower_name=app_name.toLowerCase();
            this.package_name=app_package;
            this.path=app_path;
            this.sizeLong=app_size_long;
            this.size=FileUtil.humanReadableByteCount(app_size_long);
            this.dateLong=app_date_long;
            this.date=Global.SDF.format(dateLong);
            this.version=version;
        }

        public String getName(){return this.name;}

        public String getLowerName(){ return this.lower_name;}

        public String getPackage_name(){ return this.package_name;}

        public String getPath() {return this.path;}

        public long getSizeLong(){return this.sizeLong;}

        public String getSize(){return this.size;}

        public long getDateLong(){ return this.dateLong;}

        public String getDate(){return this.date;}

        public String getVersion(){return this.version;}

    }

}
