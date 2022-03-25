package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

public class PdfViewFragment_view_container extends Fragment
{
    private Context context;
    private String file_path;
    private FilePOJO currently_shown_file;
    //private File pdf_file;
    private final List<Bitmap> list_pdf_pages=new ArrayList<>();
    private AsyncTaskStatus asyncTaskStatus;
    private Handler h;
    private Handler handler;
    private int image_selected_idx=0,previously_selected_image_idx=0;
    private PdfViewFragmentPagerAdapter pdf_fragment_pager_adapter;
    private PictureSelectorAdapter picture_selector_adapter;
    private final int s=0;
    //private TextView total_pages_tv;
    private int total_pages;
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    private List<FilePOJO> files_selected_for_delete;
    private List<FilePOJO> deleted_files;
    private String tree_uri_path="";
    private Uri tree_uri;
    private final int saf_request_code=268;
    private DeleteFileAsyncTask delete_file_async_task;
    private boolean asynctask_running;
    private Uri data;
    private ProgressBarFragment pbf;
    private LocalBroadcastManager localBroadcastManager;
    private RecyclerView recyclerview;
    private boolean toolbar_visible,is_menu_opened;
    private TextView title,current_page_tv;
    private Toolbar toolbar;
    private FloatingActionButton floating_back_button;
    private Runnable runnable;
    private ViewPager2 view_pager;
    private LinearLayoutManager lm;
    private SparseBooleanArray selected_item_sparseboolean;
    private int preview_image_offset;
    private int floating_button_height;
    private int recyclerview_height;
    //private List<Bitmap> bitmapList=new ArrayList<>();
    private AsyncTaskPdfPages asyncTaskPdfPages;
    private LinearLayout image_view_selector_butt;
    private String source_folder;
    private PdfRenderer pdfRenderer;
    private FileObjectType fileObjectType;
    private boolean fromThirdPartyApp;
    private double size_per_page_MB;
    private static final int SAFE_MEMORY_BUFFER=5;
    private PdfViewFragment_view_container pdfViewFragment_view_container;
    private PdfPageLoadListener pdfPageLoadListener;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
        localBroadcastManager=LocalBroadcastManager.getInstance(context);
        pdfViewFragment_view_container=this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        data=((PdfViewActivity)context).data;
        asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            file_path = bundle.getString("file_path");
            boolean fromArchiveView = bundle.getBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE);
            fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
        }

        if(fileObjectType==null || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
        {
            fileObjectType=FileObjectType.FILE_TYPE;
            fromThirdPartyApp = true;
        }

        source_folder=new File(file_path).getParent();
        if(fileObjectType==FileObjectType.USB_TYPE)
        {
            if(MainActivity.usbFileRoot!=null)
            {
                try {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path)),false);

                } catch (IOException e) {

                }
            }
        }
        else
        {
            currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,fileObjectType);
        }

        asyncTaskPdfPages = new AsyncTaskPdfPages();
        asyncTaskPdfPages.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        list_popupwindowpojos=new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
        DisplayMetrics displayMetrics=context.getResources().getDisplayMetrics();
        floating_button_height=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,146,displayMetrics);
        recyclerview_height= (int) getResources().getDimension(R.dimen.image_preview_dimen)+((int)+getResources().getDimension(R.dimen.layout_margin)*2);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method
        h=new Handler();
        handler=new Handler();
        Handler hand = new Handler();
        View v=inflater.inflate(R.layout.fragment_pdf_view_container,container,false);
        toolbar_visible=true;
        handler=new Handler();
        toolbar=v.findViewById(R.id.fragment_pdf_view_container_toolbar);
        title=v.findViewById(R.id.fragment_pdf_view_container_pdf_name);
        ImageView overflow = v.findViewById(R.id.fragment_pdf_view_container_overflow);
        overflow.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                is_menu_opened=true;
                listPopWindow.showAsDropDown(v,0,(Global.SIX_DP));
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
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> adapterview, View v, int p1,long p2)
            {
                final Bundle bundle=new Bundle();
                final ArrayList<String> files_selected_array=new ArrayList<>();

                switch(p1)
                {
                    case 0:
                        if(fromThirdPartyApp)
                        {
                            print(getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(currently_shown_file.getPath());
                        DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity=DeleteFileAlertDialogOtherActivity.getInstance(files_selected_array,fileObjectType);
                        deleteFileAlertDialogOtherActivity.setDeleteFileDialogListener(new DeleteFileAlertDialogOtherActivity.DeleteFileAlertDialogListener()
                        {
                            public void onSelectOK()
                            {
                                if(!asynctask_running)
                                {
                                    asynctask_running=true;
                                    files_selected_for_delete=new ArrayList<>();
                                    deleted_files=new ArrayList<>();
                                    files_selected_for_delete.add(currently_shown_file);
                                    delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,fileObjectType);
                                    delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }

                            }
                        });
                        deleteFileAlertDialogOtherActivity.show(((PdfViewActivity)context).fm,"deletefilealertotheractivity");
                        break;

                    case 1:
                        Uri src_uri=null;
                        if(fromThirdPartyApp)
                        {
                            src_uri=data;

                        }
                        else if(fileObjectType==FileObjectType.FILE_TYPE)
                        {
                            src_uri= FileProvider.getUriForFile(context, context.getPackageName()+".provider",new File(currently_shown_file.getPath()));
                        }
                        if(src_uri==null)
                        {
                            print(getString(R.string.not_able_to_process));
                            break;
                        }
                        ArrayList<Uri> uri_list=new ArrayList<>();
                        uri_list.add(src_uri);
                        FileIntentDispatch.sendUri(context,uri_list);

                        break;

                    case 2:
                        if(fromThirdPartyApp)
                        {
                            print(getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(currently_shown_file.getPath());
                        PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,FileObjectType.FILE_TYPE);
                        propertiesDialog.show(((PdfViewActivity)context).fm,"properties_dialog");
                        break;


                    default:
                        break;

                }
                listPopWindow.dismiss();
            }

        });

        listPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener()
        {
            public void onDismiss()
            {
                is_menu_opened=false;
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
            }

        });
        view_pager=v.findViewById(R.id.fragment_pdf_view_container_viewpager);
        floating_back_button=v.findViewById(R.id.fragment_pdf_view_floating_button);
        floating_back_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                ((PdfViewActivity)context).onBackPressed();
            }
        });
        current_page_tv=v.findViewById(R.id.fragment_pdf_view_container_current_view);
        image_view_selector_butt=v.findViewById(R.id.fragment_pdf_view_container_view_selector_recyclerview_group);
        recyclerview=v.findViewById(R.id.fragment_pdf_view_container_view_selector_recyclerview);
        new LinearSnapHelper().attachToRecyclerView(recyclerview);
        recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            public void onScrolled(RecyclerView rv, int dx,int dy)
            {
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
            }
        });
        int recyclerview_image_width = (int) getResources().getDimension(R.dimen.image_preview_dimen);
        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
        {
            recyclerview.setPadding(Global.SCREEN_HEIGHT/2- recyclerview_image_width /2,0,Global.SCREEN_HEIGHT/2- recyclerview_image_width /2,0);
        }
        else
        {
            recyclerview.setPadding(Global.SCREEN_WIDTH/2- recyclerview_image_width /2,0,Global.SCREEN_WIDTH/2- recyclerview_image_width /2,0);
        }

        preview_image_offset=(int)getResources().getDimension(R.dimen.layout_margin);
        selected_item_sparseboolean=new SparseBooleanArray();
        lm=new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false);

        view_pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                previously_selected_image_idx=image_selected_idx;
                image_selected_idx=position;
                current_page_tv.setText(image_selected_idx+1+"/"+total_pages);

            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                lm.scrollToPositionWithOffset(position,-preview_image_offset);
                selected_item_sparseboolean=new SparseBooleanArray();
                selected_item_sparseboolean.put(position,true);
                if(picture_selector_adapter!=null)
                {
                    picture_selector_adapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });


        runnable=new Runnable()
        {
            public void run()
            {
                if(!is_menu_opened)
                {
                    toolbar.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
                    floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
                    image_view_selector_butt.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));

                    //toolbar.setVisibility(View.GONE);
                    //recyclerview.setVisibility(View.GONE);
                    //floating_back_button.setVisibility(View.GONE);
                    toolbar_visible=false;
                }

            }
        };


        h.post(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus!=AsyncTaskStatus.COMPLETED)
                {
                    h.postDelayed(this,100);
                }
                else
                {
                    pdf_fragment_pager_adapter=new PdfViewFragmentPagerAdapter(pdfViewFragment_view_container);
                    view_pager.setAdapter(pdf_fragment_pager_adapter);
                    view_pager.setCurrentItem(image_selected_idx);
                    selected_item_sparseboolean.put(image_selected_idx,true);

                    picture_selector_adapter=new PictureSelectorAdapter(list_pdf_pages);
                    recyclerview.setLayoutManager(lm);
                    recyclerview.setAdapter(picture_selector_adapter);
                    lm.scrollToPositionWithOffset(image_selected_idx,-preview_image_offset);
                    current_page_tv.setText(image_selected_idx+1+"/"+total_pages);
                    handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
                    h.removeCallbacks(this);
                }
            }
        });

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                image_view_on_click_procedure();
            }
        });

        return v;
    }


    public static PdfViewFragment_view_container getNewInstance(String file_path, boolean fromArchiveView, FileObjectType fileObjectType)
    {
        PdfViewFragment_view_container pdfViewFragment=new PdfViewFragment_view_container();
        Bundle bundle=new Bundle();
        bundle.putString("file_path",file_path);
        bundle.putBoolean("fromArchiveView",fromArchiveView);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
        pdfViewFragment.setArguments(bundle);
        return  pdfViewFragment;
    }

    public void seekSAFPermission()
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent>activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Uri treeUri;
                treeUri = result.getData().getData();
                Global.ON_REQUEST_URI_PERMISSION(context, treeUri);

                boolean permission_requested = false;
                delete_file_async_task = new DeleteFileAsyncTask(files_selected_for_delete,fileObjectType);
                delete_file_async_task.executeOnExecutor(android.os.AsyncTask.THREAD_POOL_EXECUTOR);

            } else {
                //cancel_button.callOnClick();
                print(getString(R.string.permission_not_granted));
            }
        }
    });


    private boolean check_SAF_permission(String file_path,FileObjectType fileObjectType)
    {
        UriPOJO  uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
        if(uriPOJO!=null)
        {
            tree_uri_path=uriPOJO.get_path();
            tree_uri=uriPOJO.get_uri();
        }

        if(tree_uri_path.equals("")) {
            SAFPermissionHelperDialog safpermissionhelper = new SAFPermissionHelperDialog();
            safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener() {
                public void onOKBtnClicked() {
                    seekSAFPermission();
                }

                public void onCancelBtnClicked() {

                }
            });
            safpermissionhelper.show(((PdfViewActivity)context).fm, "saf_permission_dialog");
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
    }

    @Override
    public void onDestroy() {
        if(asyncTaskPdfPages!=null)
        {
            asyncTaskPdfPages.cancel(true);
        }
        super.onDestroy();
    }

    private void image_view_on_click_procedure()
    {
        if(toolbar_visible)
        {
            //disappear
            toolbar.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
            floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
            image_view_selector_butt.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));


            //toolbar.setVisibility(View.GONE);
            //recyclerview.setVisibility(View.GONE);
            //floating_back_button.setVisibility(View.GONE);

            is_menu_opened=false;
            toolbar_visible=false;
            handler.removeCallbacks(runnable);

        }
        else
        {
            //appear
            toolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
            floating_back_button.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
            image_view_selector_butt.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));

            //toolbar.setVisibility(View.VISIBLE);
            //recyclerview.setVisibility(View.VISIBLE);
            //floating_back_button.setVisibility(View.VISIBLE);
            toolbar_visible=true;
            handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
        }

    }


    public void onFragmentShown(PdfViewFragment pdfViewFragment) {
        if(pdfViewFragment!=null)
        {
            pdfPageLoadListener=pdfViewFragment;
            pdfViewFragment.setOnClickListener(new PdfViewFragment.OnClickListener() {
                @Override
                public void onClickView() {
                    image_view_on_click_procedure();
                }
            });
            new BitmapFetchAsyncTask(pdfViewFragment,image_selected_idx).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        }
    }


    private class PdfViewFragmentPagerAdapter extends FragmentStateAdapter
    {

        TouchImageView image_view;
        Bitmap bitmap;
        AsyncTaskStatus asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;

        public PdfViewFragmentPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        title.setText(currently_shown_file.getName());
        }


        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new PdfViewFragment();
        }


        @Override
        public int getItemCount() {
            return total_pages;
        }
    }



    private class BitmapFetchAsyncTask extends AsyncTask<Void,Void,Void>
    {
        PdfViewFragment pdfViewFragment;
        int position;
        Bitmap bitmap;

        BitmapFetchAsyncTask(PdfViewFragment pvf, int p) {
            pdfViewFragment=pvf;
            position=p;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbf=ProgressBarFragment.getInstance();
            pbf.show(((PdfViewActivity)context).fm,"");

        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(size_per_page_MB*3<(Global.AVAILABLE_MEMORY_MB()-SAFE_MEMORY_BUFFER)) {

                try {
                    bitmap=getBitmap(pdfRenderer,position);
                    pbf.dismissAllowingStateLoss();
                }
                catch (SecurityException e)
                {
                    ((PdfViewActivity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            print(getString(R.string.security_exception_thrown));
                        }
                    });
                    pbf.dismissAllowingStateLoss();
                }
                catch (OutOfMemoryError error)
                {
                    ((PdfViewActivity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            print(getString(R.string.outofmemory_exception_thrown));
                        }
                    });
                    pbf.dismissAllowingStateLoss();
                    ((PdfViewActivity)context).finish();
                }
                catch (Exception e)
                {
                    ((PdfViewActivity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            print(getString(R.string.exception_thrown));
                        }
                    });
                    pbf.dismissAllowingStateLoss();
                }

            }
                return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            pbf.dismissAllowingStateLoss();
            if(pdfPageLoadListener!=null)
            {
                pdfPageLoadListener.onRetrievePdfPage(bitmap);
            }

        }
    }


    private Bitmap getBitmap(PdfRenderer pdfRenderer, int i)
    {
        PdfRenderer.Page page= pdfRenderer.openPage(i);
        Bitmap bitmap=Bitmap.createBitmap(page.getWidth(),page.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap,0f,0f,null);
        page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        return bitmap;
    }


    private class PictureSelectorAdapter extends RecyclerView.Adapter<PictureSelectorAdapter.VH>
    {
        final List<Bitmap> picture_list;
        PictureSelectorAdapter(List<Bitmap>list)
        {
            picture_list=list;
        }

        @Override
        public PictureSelectorAdapter.VH onCreateViewHolder(ViewGroup parent, int p2)
        {
            // TODO: Implement this method
            View v=LayoutInflater.from(context).inflate(R.layout.pdf_page_selector_recyclerview_layout,parent,false);
            return new PictureSelectorAdapter.VH(v);
        }

        @Override
        public void onBindViewHolder(PictureSelectorAdapter.VH p1, int p2)
        {
            // TODO: Implement this method
            p1.textView.setText(p2+1+"");
            p1.v.setSelected(selected_item_sparseboolean.get(p2,false));

        }

        @Override
        public int getItemCount()
        {
            // TODO: Implement this method
            return picture_list.size();
        }

        private class VH extends RecyclerView.ViewHolder
        {
            final View v;
            final TextView textView;
            VH(View view)
            {
                super(view);
                v=view;
                textView=v.findViewById(R.id.pdf_page_selector_textview);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position=Integer.parseInt(textView.getText().toString());
                        view_pager.setCurrentItem(position-1);
                    }
                });

            }

        }
    }


    private class AsyncTaskPdfPages extends svl.kadatha.filex.AsyncTask<Void,Bitmap,Void>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            asyncTaskStatus=AsyncTaskStatus.STARTED;

        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            asyncTaskStatus=AsyncTaskStatus.COMPLETED;

        }


        @Override
        protected Void doInBackground(Void... voids) {
            try {

                long file_size;
                if(fromThirdPartyApp)
                {
                    pdfRenderer = new PdfRenderer(context.getContentResolver().openFileDescriptor(data,"r"));
                }
                else if(fileObjectType==FileObjectType.FILE_TYPE)
                {
                    pdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(new File(currently_shown_file.getPath()), ParcelFileDescriptor.MODE_READ_ONLY));
                }
                else if(fileObjectType==FileObjectType.USB_TYPE)
                {
                    pdfRenderer = new PdfRenderer(context.getContentResolver().openFileDescriptor(data,"r"));
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    pdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(new File(currently_shown_file.getPath()), ParcelFileDescriptor.MODE_READ_ONLY));
                }


                file_size=currently_shown_file.getSizeLong();
                if(file_size==0)
                {
                    file_size=Global.GET_URI_FILE_SIZE(data,context);
                }

                total_pages = pdfRenderer.getPageCount();
                //Log.d("shankar","file size "+file_size);
                if(file_size!=0)
                {
                    size_per_page_MB=(double)file_size/total_pages/1024/1024;
                    //  Log.d("shankar","pages "+total_pages);
                    // Log.d("shankar","size per page in MB "+size_per_page_MB);
                    //Log.d("shankar","size per page in MB *3 "+size_per_page_MB*3);
                }
                double availablememory=Global.AVAILABLE_MEMORY_MB();
                //Log.d("shankar","available memory "+availablememory);

                for(int i = 0; i< total_pages; ++i)
                {
                    if(isCancelled())
                    {
                        return null;
                    }

                    list_pdf_pages.add(null);

                }
            }
            catch (SecurityException e)
            {
                ((PdfViewActivity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        print(getString(R.string.security_exception_thrown)+" - "+getString(R.string.may_be_password_protected));
                    }
                });
                return null;
            }
            catch (IOException e) {
                ((PdfViewActivity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        print(getString(R.string.file_not_in_PDF_format_or_corrupted));
                    }
                });

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            asyncTaskStatus=AsyncTaskStatus.COMPLETED;
        }
    }

    private class DeleteFileAsyncTask extends svl.kadatha.filex.AsyncTask<Void,File,Boolean>
    {

        final List<FilePOJO> src_file_list;
        final List<String> deleted_file_name_list=new ArrayList<>();

        int counter_no_files;
        long counter_size_files;
        String current_file_name;
        boolean isFromInternal;
        String size_of_files_format;
        final FileObjectType fileObjectType;
        DeleteFileAsyncTask(List<FilePOJO> src_file_list, FileObjectType fileObjectType)
        {
            this.src_file_list=src_file_list;
            this.fileObjectType=fileObjectType;
        }

        @Override
        protected void onPreExecute()
        {
            // TODO: Implement this method
            pbf=ProgressBarFragment.getInstance();
            pbf.show(((PdfViewActivity)context).fm,"progressbar_dialog");
        }

        @Override
        protected void onCancelled(Boolean result)
        {
            // TODO: Implement this method
            super.onCancelled(result);
            if(deleted_files.size()>0)
            {
                pdf_fragment_pager_adapter.notifyDataSetChanged();
                picture_selector_adapter.notifyDataSetChanged();
                FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
                Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,PdfViewActivity.ACTIVITY_NAME);
                ((PdfViewActivity)context).finish();
            }

            pbf.dismissAllowingStateLoss();
            asynctask_running=false;

        }

        @Override
        protected Boolean doInBackground(Void...p)
        {
            // TODO: Implement this method
            boolean success;

            if(fileObjectType==FileObjectType.FILE_TYPE)
            {
                isFromInternal=FileUtil.isFromInternal(fileObjectType,src_file_list.get(0).getPath());
            }
            success=deleteFromFolder();
            return success;
        }


        @Override
        protected void onPostExecute(Boolean result)
        {
            // TODO: Implement this method

            super.onPostExecute(result);
            if(deleted_files.size()>0)
            {
                pdf_fragment_pager_adapter.notifyDataSetChanged();
                picture_selector_adapter.notifyDataSetChanged();
                FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
                Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,PdfViewActivity.ACTIVITY_NAME);
                ((PdfViewActivity)context).finish();

            }

            pbf.dismissAllowingStateLoss();
            asynctask_running=false;

        }


        private boolean deleteFromFolder()
        {
            boolean success=false;
            int iteration=0;
            int size=src_file_list.size();
            if(fileObjectType==FileObjectType.FILE_TYPE)
            {
                if(isFromInternal)
                {
                    for(int i=0;i<size;++i)
                    {
                        if(isCancelled())
                        {
                            return false;
                        }
                        FilePOJO filePOJO=src_file_list.get(i);
                        File f=new File(filePOJO.getPath());
                        current_file_name=f.getName();
                        success=FileUtil.deleteNativeDirectory(f);
                        if(success)
                        {
                            deleted_files.add(filePOJO);
                            deleted_file_name_list.add(current_file_name);
                        }
                        files_selected_for_delete.remove(filePOJO);
                    }

                }
                else
                {
                    if(check_SAF_permission(src_file_list.get(0).getPath(),fileObjectType))
                    {
                        for(int i=0;i<size;++i)
                        {
                            if(isCancelled())
                            {
                                return false;
                            }
                            FilePOJO filePOJO=src_file_list.get(i);
                            File file=new File(filePOJO.getPath());
                            current_file_name=file.getName();
                            success=FileUtil.deleteSAFDirectory(context,file.getAbsolutePath(),tree_uri,tree_uri_path);
                            if(success)
                            {
                                deleted_files.add(filePOJO);
                                deleted_file_name_list.add(current_file_name);
                            }
                            files_selected_for_delete.remove(filePOJO);
                        }
                    }

                }
            }
            else if(fileObjectType==FileObjectType.USB_TYPE)
            {
                for(int i=0;i<size;++i)
                {
                    if(isCancelled())
                    {
                        return false;
                    }
                    FilePOJO filePOJO=src_file_list.get(i);
                    UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,filePOJO.getPath());
                    current_file_name=f.getName();
                    success=FileUtil.deleteUsbDirectory(f);
                    if(success)
                    {
                        deleted_files.add(filePOJO);
                        deleted_file_name_list.add(current_file_name);
                    }
                    files_selected_for_delete.remove(filePOJO);
                }
            }

            return success;
        }

    }

    interface PdfPageLoadListener
    {
        void onRetrievePdfPage(Bitmap bitmap);
    }

    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

}



