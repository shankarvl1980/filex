package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
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

import androidx.annotation.Nullable;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfViewFragment_single_view extends Fragment
{
    private Context context;
    private String file_path;
    private File pdf_file;
    private final List<Bitmap> list_pdf_pages=new ArrayList<>();
    private AsyncTaskStatus asyncTaskStatus;
    private Handler h,handler,hand;
    private int image_selected_idx=0,previously_selected_image_idx=0;
    private PdfViewPagerAdapter pdf_view_adapter;
    private PictureSelectorAdapter picture_selector_adapter;
    //private PdfPageViewRecyclerViewAdapter pdfPageViewRecyclerViewAdapter;
    private int s=0;
    //private TextView total_pages_tv;
    private int total_pages;
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    private List<File> files_selected_for_delete;
    private List<File> deleted_files;
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
    private ViewPager view_pager;
    private LinearLayoutManager lm;
    private SparseBooleanArray selected_item_sparseboolean;
    private int preview_image_offset;
    private int floating_button_height;
    private int recyclerview_height;
    //private List<Bitmap> bitmapList=new ArrayList<>();
    private AsyncTaskPdfPages asyncTaskPdfPages;
    private LinearLayout image_view_selector_butt;
    private PdfRenderer pdfRenderer;
    private FileObjectType fileObjectType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        context=getContext();
        localBroadcastManager=LocalBroadcastManager.getInstance(context);
        data=((PdfViewActivity)getContext()).data;
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
            boolean fromThirdPartyApp = true;
        }

        pdf_file=new File(file_path);
        asyncTaskPdfPages = new AsyncTaskPdfPages();
        asyncTaskPdfPages.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        list_popupwindowpojos=new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
        DisplayMetrics displayMetrics=context.getResources().getDisplayMetrics();
        floating_button_height=(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,146,displayMetrics);
        recyclerview_height=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,68,displayMetrics);



    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method
        context=getContext();
        localBroadcastManager=LocalBroadcastManager.getInstance(context);
        h=new Handler();
        handler=new Handler();
        hand=new Handler();
        View v=inflater.inflate(R.layout.fragment_image_view,container,false);
        toolbar_visible=true;
        handler=new Handler();
        toolbar=v.findViewById(R.id.activity_picture_toolbar);
        title=v.findViewById(R.id.activity_picture_name);
        ImageView overflow = v.findViewById(R.id.activity_picture_overflow);
        overflow.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                is_menu_opened=true;
                listPopWindow.showAsDropDown(v,0,(Global.ONE_DP*4));
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

                        if(!pdf_file.exists())
                        {
                            print(getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(pdf_file.getAbsolutePath());
                        DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity=DeleteFileAlertDialogOtherActivity.getInstance(files_selected_array,FileObjectType.FILE_TYPE);
                        deleteFileAlertDialogOtherActivity.setDeleteFileDialogListener(new DeleteFileAlertDialogOtherActivity.DeleteFileAlertDialogListener()
                        {
                            public void onSelectOK()
                            {
                                if(!asynctask_running)
                                {
                                    asynctask_running=true;
                                    files_selected_for_delete=new ArrayList<>();
                                    deleted_files=new ArrayList<>();
                                    files_selected_for_delete.add(pdf_file);
                                    delete_file_async_task=new DeleteFileAsyncTask();
                                    delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }

                            }
                        });
                        deleteFileAlertDialogOtherActivity.show(((PdfViewActivity)context).fm,"deletefilealertotheractivity");
                        break;

                    case 1:
                        Uri src_uri;
                        if(pdf_file.exists())
                        {
                            src_uri= FileProvider.getUriForFile(context, context.getPackageName()+".provider",pdf_file);
                        }
                        else
                        {
                            src_uri=data;
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
                        if(!pdf_file.exists())
                        {
                            print(getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(pdf_file.getAbsolutePath());
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
        view_pager=v.findViewById(R.id.activity_picture_view_viewpager);
        floating_back_button=v.findViewById(R.id.floating_button_picture_fragment);
        floating_back_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                ((PdfViewActivity)context).onBackPressed();
            }

        });
        current_page_tv=v.findViewById(R.id.image_view_current_view);
        image_view_selector_butt=v.findViewById(R.id.image_view_selector_recyclerview_group);
        recyclerview=v.findViewById(R.id.activity_picture_view_recyclerview);
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
        lm.scrollToPositionWithOffset(image_selected_idx,-preview_image_offset);

        view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            public void onPageSelected(int i)
            {
                lm.scrollToPositionWithOffset(i,-preview_image_offset);
                selected_item_sparseboolean=new SparseBooleanArray();
                selected_item_sparseboolean.put(i,true);
                picture_selector_adapter.notifyDataSetChanged();

            }

            public void onPageScrollStateChanged(int i)
            {

            }

            public void onPageScrolled(int i,float p2, int p3)
            {
                previously_selected_image_idx=image_selected_idx;
                image_selected_idx=i;
                current_page_tv.setText(image_selected_idx+1+"/"+total_pages);

                //currently_shown_file=album_list.get(i);
                //title.setText(currently_shown_file.getName());
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
        handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);


        h.post(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED && list_pdf_pages.size()<1)
                {
                    h.postDelayed(this,25);
                }
                else
                {
                    pdf_view_adapter=new PdfViewPagerAdapter();
                    view_pager.setAdapter(pdf_view_adapter);
                    //view_pager.setCurrentItem(image_selected_idx);
                    selected_item_sparseboolean.put(image_selected_idx,true);
                    picture_selector_adapter=new PictureSelectorAdapter(list_pdf_pages);

                    recyclerview.setLayoutManager(lm);
                    recyclerview.setAdapter(picture_selector_adapter);
                    s=list_pdf_pages.size();
                    start_polling();
                    h.removeCallbacks(this);
                }
            }
        });
        current_page_tv.setText(image_selected_idx+1+"/"+total_pages);
        return v;
    }



    private void start_polling()
    {
        hand.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    //bitmapList=new ArrayList<>();
                    //bitmapList=list_pdf_pages;
                    pdf_view_adapter.notifyDataSetChanged();

                    picture_selector_adapter.notifyItemRangeChanged(s,s=list_pdf_pages.size());
                    hand.removeCallbacks(this);
                }
                else
                {
                    //bitmapList=new ArrayList<>();
                    //bitmapList=list_pdf_pages;
                    pdf_view_adapter.notifyDataSetChanged();

                    picture_selector_adapter.notifyItemRangeChanged(s,s=list_pdf_pages.size());
                    hand.postDelayed(this,50);
                }

            }
        },50);
    }


    public static PdfViewFragment_view_pager getNewInstance(String file_path)
    {
        PdfViewFragment_view_pager pdfViewFragment=new PdfViewFragment_view_pager();
        Bundle bundle=new Bundle();
        bundle.putString("file_path",file_path);
        pdfViewFragment.setArguments(bundle);
        return  pdfViewFragment;
    }

    public void seekSAFPermission()
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, saf_request_code);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData)
    {
        super.onActivityResult(requestCode,resultCode,resultData);
        if (requestCode == saf_request_code) {
            if (resultCode == Activity.RESULT_OK) {
                Uri treeUri;
                // Get Uri from Storage Access Framework.
                treeUri = resultData.getData();
                Global.ON_REQUEST_URI_PERMISSION(context, treeUri);

                boolean permission_requested = false;
                delete_file_async_task = new DeleteFileAsyncTask();
                delete_file_async_task.executeOnExecutor(android.os.AsyncTask.THREAD_POOL_EXECUTOR);

            } else {
                //cancel_button.callOnClick();
                print(getString(R.string.permission_not_granted));
            }
        }
    }

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
    public void onDestroy() {
        if(asyncTaskPdfPages!=null)
        {
            asyncTaskPdfPages.cancel(true);
        }
        super.onDestroy();
    }

    private class PdfViewPagerAdapter extends PagerAdapter
    {

        TouchImageView image_view;
        Bitmap bitmap;
        PdfViewPagerAdapter()
        {
            //bitmapList=bitmap_list;
            title.setText(pdf_file.getName());
        }

        @Override
        public int getCount()
        {
            // TODO: Implement this method

            return total_pages;//bitmapList.size();

        }

        @Override
        public boolean isViewFromObject(View p1, Object p2)
        {
            // TODO: Implement this method
            return p1.equals(p2);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            // TODO: Implement this method


            View v=LayoutInflater.from(context).inflate(R.layout.image_viewpager_layout,container,false);
            image_view=v.findViewById(R.id.picture_viewpager_layout_imageview);
            image_view.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image_view.setMaxZoom(6);
            image_view.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    //if(toolbar.getGlobalVisibleRect(new Rect()))
                    if(toolbar_visible)
                    {
                        //disappear
                        toolbar.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
                        floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
                        image_view_selector_butt.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));


                        //toolbar.setVisibility(View.GONE);
                        //recyclerview.setVisibility(View.GONE);
                        //floating_back_button.setVisibility(View.GONE);
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
            });


            boolean big_file = true;
            if(big_file)
            {
                bitmap=getBitmap(pdfRenderer,position);
            }
            else
            {
                if(position<list_pdf_pages.size()) bitmap=list_pdf_pages.get(position);
            }

            GlideApp.with(context).load(bitmap).placeholder(R.drawable.pdf_file_icon).error(R.drawable.pdf_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(image_view);
            container.addView(v);
            return v;
        }


        @Override
        public int getItemPosition(Object object)
        {
            // TODO: Implement this method
            return POSITION_UNCHANGED;
        }



        @Override
        public void destroyItem(ViewGroup container, int position, Object object)
        {
            // TODO: Implement this method
            container.removeView((View)object);
        }


        public float getAspectRatio()
        {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inJustDecodeBounds=true;
            int width=options.outWidth;
            int height=options.outHeight;
            return (float) (width/height);
        }
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
            View v=LayoutInflater.from(context).inflate(R.layout.image_selector_recyclerview_layout,parent,false);
            return new PictureSelectorAdapter.VH(v);
        }

        @Override
        public void onBindViewHolder(PictureSelectorAdapter.VH p1, int p2)
        {
            // TODO: Implement this method
            Bitmap bitmap=picture_list.get(p2);
            GlideApp.with(context).load(bitmap).error(R.drawable.pdf_file_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(p1.imageview);
            p1.v.setSelected(selected_item_sparseboolean.get(p2,false));

        }

        @Override
        public int getItemCount()
        {
            // TODO: Implement this method
            return picture_list.size();
        }

        private class VH extends RecyclerView.ViewHolder implements View.OnClickListener
        {
            final View v;
            final ImageView imageview;
            VH(View view)
            {
                super(view);
                v=view;
                imageview=v.findViewById(R.id.picture_viewpager_layout_imageview);
                v.setOnClickListener(this);
            }

            @Override
            public void onClick(View p1)
            {
                // TODO: Implement this method
                view_pager.setCurrentItem(getBindingAdapterPosition());
            }
        }
    }


    private class AsyncTaskPdfPages extends svl.kadatha.filex.AsyncTask<Void,Bitmap,Void>
    {
        // CancelableProgressBarDialog cancelableProgressBarDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            asyncTaskStatus=AsyncTaskStatus.STARTED;
         /*
            cancelableProgressBarDialog=new CancelableProgressBarDialog();
            cancelableProgressBarDialog.setProgressBarCancelListener(new CancelableProgressBarDialog.ProgresBarFragmentCancelListener() {
                @Override
                public void on_cancel_progress() {
                    cancel(true);
                }
            });
            cancelableProgressBarDialog.show(((PdfViewActivity)context).fm,"");

          */
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            asyncTaskStatus=AsyncTaskStatus.COMPLETED;
            //cancelableProgressBarDialog.dismissAllowingStateLoss();
        }


        @Override
        protected Void doInBackground(Void... voids) {
            try {


                if(pdf_file.exists())
                {
                    pdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(pdf_file, ParcelFileDescriptor.MODE_READ_ONLY));
                }
                else
                {
                    pdfRenderer = new PdfRenderer(context.getContentResolver().openFileDescriptor(data,"r"));
                }

                total_pages = pdfRenderer.getPageCount();
                int display_dpi=context.getResources().getDisplayMetrics().densityDpi;
                float width=(float) Global.SCREEN_WIDTH;
                for(int i = 0; i< total_pages; ++i)
                {
                    if(isCancelled())
                    {
                        return null;
                    }

                    list_pdf_pages.add(getBitmap(pdfRenderer,i));
                    //publishProgress(bitmap);
                }
            } catch (IOException e) {

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
            //bitmapList.add(values[0]);
            //total_pages_tv.setText(current_page+"/"+total_pages);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            asyncTaskStatus=AsyncTaskStatus.COMPLETED;
            //cancelableProgressBarDialog.dismissAllowingStateLoss();
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


    private class DeleteFileAsyncTask extends svl.kadatha.filex.AsyncTask<Void,File,Boolean>
    {

        final List<File> src_file_list=new ArrayList<>();
        int counter_no_files;
        long counter_size_files;
        String current_file_name;
        boolean isFromInternal;
        String size_of_files_format;
        final ProgressBarFragment pbf=ProgressBarFragment.getInstance();

        DeleteFileAsyncTask()
        {
            src_file_list.addAll(files_selected_for_delete);
        }

        @Override
        protected void onPreExecute()
        {
            // TODO: Implement this method
            pbf.show(((VideoViewActivity)context).fm,"progressbar_dialog");
        }

        @Override
        protected void onCancelled(Boolean result)
        {
            // TODO: Implement this method
            super.onCancelled(result);

            if(deleted_files.size()>0)
            {

                pdf_view_adapter.notifyDataSetChanged();
                picture_selector_adapter.notifyDataSetChanged();
                Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,PdfViewActivity.ACTIVITY_NAME);
            }

            pbf.dismissAllowingStateLoss();
            asynctask_running=false;
        }

        @Override
        protected Boolean doInBackground(Void...p)
        {
            // TODO: Implement this method
            boolean success;
            isFromInternal=FileUtil.isFromInternal(fileObjectType,files_selected_for_delete.get(0).getAbsolutePath());
            success=deleteFromFolder();
            return success;
        }




        private boolean deleteFromFolder()
        {
            boolean success=false;
            int iteration=0;
            int size=src_file_list.size();
            if(isFromInternal)
            {
                for(int i=0;i<size;++i)
                {
                    File f=src_file_list.get(i);
                    current_file_name=f.getName();
                    success=deleteNativeDirectory(f);
                    if(success)
                    {
                        deleted_files.add(f);
                    }
                    files_selected_for_delete.remove(f);
                }

            }
            else
            {

                if(check_SAF_permission(files_selected_for_delete.get(0).getAbsolutePath(),fileObjectType))
                {
                    for(int i=0;i<size;++i)
                    {
                        File file=src_file_list.get(i);
                        if(isCancelled())
                        {
                            return false;
                        }
                        current_file_name=src_file_list.get(iteration).getName();
                        publishProgress(file);
                        success=FileUtil.deleteSAFDirectory(context,file.getAbsolutePath(),tree_uri,tree_uri_path);
                        if(success)
                        {
                            deleted_files.add(file);
                        }
                        files_selected_for_delete.remove(file);
                    }

                }

            }
            return success;
        }


        public boolean deleteNativeDirectory(final File folder)
        {
            boolean success=false;

            if (folder.isDirectory())            //Check if folder file is a real folder
            {
                if(isCancelled())
                {
                    return false;
                }

                File[] list = folder.listFiles(); //Storing all file name within array
                int size=list.length;
                if (list != null)                //Checking list value is null or not to check folder containts atleast one file
                {
                    for (int i = 0; i < size; ++i)
                    {
                        if(isCancelled())
                        {
                            return false;
                        }

                        File tmpF = list[i];
                        if (tmpF.isDirectory())   //if folder  found within folder remove that folder using recursive method
                        {
                            success=deleteNativeDirectory(tmpF);
                        }

                        else
                        {

                            counter_no_files++;
                            counter_size_files+=tmpF.length();
                            size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files,Global.BYTE_COUNT_BLOCK_1000);
                            publishProgress(tmpF);
                            success=tmpF.delete(); //else delete file
                        }

                    }
                }

                if(folder.exists())  //delete empty folder
                {
                    counter_no_files++;
                    publishProgress(folder);
                    success=folder.delete();
                }

            }
            else
            {
                if(isCancelled())
                {
                    return false;
                }

                counter_no_files++;
                counter_size_files+=folder.length();
                size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files,Global.BYTE_COUNT_BLOCK_1000);
                publishProgress(folder);
                success=folder.delete();
            }

            return success;
        }


        @Override
        protected void onPostExecute(Boolean result)
        {
            // TODO: Implement this method

            super.onPostExecute(result);
            if(deleted_files.size()>0)
            {
                pdf_view_adapter.notifyDataSetChanged();
                picture_selector_adapter.notifyDataSetChanged();
                Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,PdfViewActivity.ACTIVITY_NAME);

                if(!pdf_file.exists())
                {
                    ((PdfViewActivity)context).finish();
                }

            }
            pbf.dismissAllowingStateLoss();
            asynctask_running=false;

        }

    }


    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

}



