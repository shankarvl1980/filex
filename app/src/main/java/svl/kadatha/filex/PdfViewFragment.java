package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PdfViewFragment extends Fragment {
    public static final int SAFE_MEMORY_BUFFER = 3;
    private static final String DELETE_FILE_REQUEST_CODE = "pdf_file_delete_request_code";
    public FrameLayout progress_bar;
    public FilteredFilePOJOViewModel viewModel;
    private Context context;
    private long availableHeapMemory;
    private Handler handler;
    private PdfViewPagerAdapter pdf_view_adapter;
    private PictureSelectorAdapter picture_selector_adapter;
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    private List<FilePOJO> files_selected_for_delete;
    private Uri data;
    private RecyclerView recyclerview;
    private boolean toolbar_visible, is_menu_opened;
    private TextView title, current_page_tv;
    private Toolbar toolbar;
    private FloatingActionButton floating_back_button;
    private Runnable runnable;
    private ViewPager view_pager;
    private LinearLayoutManager lm;
    private int preview_image_offset;
    private int floating_button_height;
    private int recyclerview_height;
    private LinearLayout toolbar_group;
    private AppCompatActivity activity;

    public static PdfViewFragment getNewInstance(String file_path, FileObjectType fileObjectType) {
        PdfViewFragment pdfViewFragment = new PdfViewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("file_path", file_path);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE, fileObjectType);
        pdfViewFragment.setArguments(bundle);
        return pdfViewFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        activity = (AppCompatActivity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list_popupwindowpojos = new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon, getString(R.string.delete), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 2));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.copy_icon, getString(R.string.copy_to), 3));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 4));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.redo_icon, getString(R.string.rotate), 5));

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float height = getResources().getDimension(R.dimen.floating_button_margin_bottom) + 56;
        floating_button_height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, displayMetrics);
        recyclerview_height = (int) getResources().getDimension(R.dimen.image_preview_dimen) + ((int) getResources().getDimension(R.dimen.layout_margin) * 2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_image_view, container, false);
        toolbar_visible = true;
        handler = new Handler();
        toolbar = v.findViewById(R.id.activity_picture_toolbar);
        title = v.findViewById(R.id.activity_picture_name);
        ImageView overflow = v.findViewById(R.id.activity_picture_overflow);
        overflow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_menu_opened = true;
                listPopWindow.showAsDropDown(v, 0, (Global.LIST_POPUP_WINDOW_DROP_DOWN_OFFSET));
            }
        });

        listPopWindow = new PopupWindow(context);
        ListView listView = new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context, list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popup_window_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.list_popup_background));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterview, View v, int p1, long p2) {
                final ArrayList<String> files_selected_array = new ArrayList<>();
                switch (p1) {
                    case 0:
                        if (viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE, files_selected_array, viewModel.fileObjectType);
                        deleteFileAlertDialogOtherActivity.show(getParentFragmentManager(), "deletefilealertotheractivity");
                        break;

                    case 1:
                        Uri src_uri = null;
                        if (viewModel.fromThirdPartyApp) {
                            src_uri = data;
                        } else {
                            src_uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                        if (src_uri == null) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        ArrayList<Uri> uri_list = new ArrayList<>();
                        uri_list.add(src_uri);
                        FileIntentDispatch.sendUri(context, uri_list);
                        break;
                    case 2:
                        Uri copy_uri = null;
                        if (viewModel.fromThirdPartyApp) {
                            copy_uri = data;
                        } else {
                            copy_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                        if (copy_uri == null) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        if (activity instanceof PdfViewActivity) {
                            ((PdfViewActivity) activity).clear_cache = false;
                        }

                        Intent copy_intent = new Intent(context, CopyToActivity.class);
                        copy_intent.setAction(Intent.ACTION_SEND);
                        copy_intent.putExtra(Intent.EXTRA_STREAM, copy_uri);
                        copy_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        copy_intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        copy_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(copy_intent);
                        } catch (Exception e) {
                            Global.print(context, getString(R.string.could_not_perform_action));
                        }
                        break;

                    case 3:
                        if (viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, FileObjectType.FILE_TYPE);
                        propertiesDialog.show(getParentFragmentManager(), "properties_dialog");
                        break;
                    case 4: // Rotate
                        int currentItem = view_pager.getCurrentItem();
                        View currentView = null;
                        for (int i = 0; i < view_pager.getChildCount(); i++) {
                            View child = view_pager.getChildAt(i);
                            if (child.getTag() != null && child.getTag().equals(currentItem)) {
                                currentView = child;
                                break;
                            }
                        }

                        if (currentView != null) {
                            TouchImageView imageView = currentView.findViewById(R.id.picture_viewpager_layout_imageview);
                            if (imageView != null) {
                                Drawable drawable = imageView.getDrawable();
                                if (drawable instanceof BitmapDrawable) {
                                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

                                    // Rotate the bitmap
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(90); // Rotate by 90 degrees
                                    Bitmap rotatedBitmap = Bitmap.createBitmap(
                                            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                                    // Set the rotated bitmap to the ImageView
                                    imageView.setImageBitmap(rotatedBitmap);

                                    // Reset zoom and center the image
                                    imageView.resetZoom();
                                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                                    // Force layout update
                                    imageView.requestLayout();
                                } else {
                                    Global.print(context, getString(R.string.could_not_be_rotated));
                                }
                            } else {
                                Global.print(context, getString(R.string.could_not_be_rotated));
                            }
                        } else {
                            Global.print(context, getString(R.string.could_not_be_rotated));
                        }
                        break;
                    default:
                        break;
                }
                listPopWindow.dismiss();
            }
        });

        listPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            public void onDismiss() {
                is_menu_opened = false;
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
            }
        });
        view_pager = v.findViewById(R.id.activity_picture_view_viewpager);
        floating_back_button = v.findViewById(R.id.floating_button_picture_fragment);
        floating_back_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        progress_bar = v.findViewById(R.id.activity_picture_progressbar);
        current_page_tv = v.findViewById(R.id.image_view_current_view);
        toolbar_group = v.findViewById(R.id.image_view_toolbar_group);
        recyclerview = v.findViewById(R.id.activity_picture_view_recyclerview);
        new LinearSnapHelper().attachToRecyclerView(recyclerview);
        recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
            }
        });
        int recyclerview_image_width = (int) getResources().getDimension(R.dimen.image_preview_dimen);
        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            recyclerview.setPadding(Global.SCREEN_HEIGHT / 2 - recyclerview_image_width / 2, 0, Global.SCREEN_HEIGHT / 2 - recyclerview_image_width / 2, 0);
        } else {
            recyclerview.setPadding(Global.SCREEN_WIDTH / 2 - recyclerview_image_width / 2, 0, Global.SCREEN_WIDTH / 2 - recyclerview_image_width / 2, 0);
        }

        preview_image_offset = (int) getResources().getDimension(R.dimen.layout_margin);
        lm = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);


        view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageSelected(int i) {
                lm.scrollToPositionWithOffset(i, -preview_image_offset);
                viewModel.mselecteditems = new IndexedLinkedHashMap<>();
                viewModel.mselecteditems.put(i, true);
                if (picture_selector_adapter != null) {
                    picture_selector_adapter.notifyDataSetChanged();
                }
            }

            public void onPageScrollStateChanged(int i) {

            }

            public void onPageScrolled(int i, float p2, int p3) {
                viewModel.previously_selected_image_idx = viewModel.image_selected_idx;
                viewModel.image_selected_idx = i;
                current_page_tv.setText(viewModel.image_selected_idx + 1 + "/" + viewModel.total_pages);
            }
        });

        runnable = new Runnable() {
            public void run() {
                if (!is_menu_opened) {
                    toolbar_group.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
                    floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
                    recyclerview.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));

                    //toolbar.setVisibility(View.GONE);
                    //recyclerview.setVisibility(View.GONE);
                    //floating_back_button.setVisibility(View.GONE);
                    toolbar_visible = false;
                }
            }
        };

        viewModel = new ViewModelProvider(this).get(FilteredFilePOJOViewModel.class);

        if (activity instanceof PdfViewActivity) {
            data = ((PdfViewActivity) activity).data;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            viewModel.file_path = bundle.getString("file_path");
            viewModel.fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
            if (viewModel.fileObjectType == null || viewModel.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                viewModel.fileObjectType = FileObjectType.FILE_TYPE;
                viewModel.fromThirdPartyApp = true;
            }
        }

        viewModel.initializePdfRenderer(viewModel.fileObjectType, viewModel.file_path, data, viewModel.fromThirdPartyApp);
        viewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (Global.whether_file_cached(viewModel.fileObjectType)) {
                        if (activity instanceof PdfViewActivity) {
                            ((PdfViewActivity) activity).data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                    }

                    pdf_view_adapter = new PdfViewPagerAdapter();
                    view_pager.setAdapter(pdf_view_adapter);
                    view_pager.setCurrentItem(viewModel.image_selected_idx);
                    viewModel.mselecteditems.put(viewModel.image_selected_idx, true);
                    picture_selector_adapter = new PictureSelectorAdapter(viewModel.total_pages);

                    recyclerview.setLayoutManager(lm);
                    recyclerview.setAdapter(picture_selector_adapter);
                    lm.scrollToPositionWithOffset(viewModel.image_selected_idx, -preview_image_offset);
                    current_page_tv.setText(viewModel.image_selected_idx + 1 + "/" + viewModel.total_pages);
                }
            }
        });


        DeleteFileOtherActivityViewModel deleteFileOtherActivityViewModel = new ViewModelProvider(PdfViewFragment.this).get(DeleteFileOtherActivityViewModel.class);
        deleteFileOtherActivityViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!deleteFileOtherActivityViewModel.deleted_files.isEmpty()) {
                        pdf_view_adapter.notifyDataSetChanged();
                        picture_selector_adapter.notifyDataSetChanged();
                        getActivity().finish();
                    }
                    deleteFileOtherActivityViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                image_view_on_click_procedure();
            }
        });


        getParentFragmentManager().setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(DELETE_FILE_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    Uri tree_uri = result.getParcelable("tree_uri");
                    String tree_uri_path = result.getString("tree_uri_path");
                    String source_folder = result.getString("source_folder");
                    files_selected_for_delete = new ArrayList<>();
                    files_selected_for_delete.add(viewModel.currently_shown_file);
                    deleteFileOtherActivityViewModel.deleteFilePOJO(source_folder, files_selected_for_delete, viewModel.fileObjectType, tree_uri, tree_uri_path);
                }
            }
        });

        getChildFragmentManager().setFragmentResultListener(PdfPasswordDialog.PASSWORD_REQUEST_CODE, this,
                (requestKey, result) -> {
                    String password = result.getString("password");
                });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        availableHeapMemory = Global.AVAILABLE_MEMORY_MB();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
    }


    private void image_view_on_click_procedure() {
        if (toolbar_visible) {
            //disappear
            toolbar_group.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
            floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
            recyclerview.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));


            //toolbar.setVisibility(View.GONE);
            //recyclerview.setVisibility(View.GONE);
            //floating_back_button.setVisibility(View.GONE);

            is_menu_opened = false;
            toolbar_visible = false;
            handler.removeCallbacks(runnable);
        } else {
            //appear
            toolbar_group.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
            floating_back_button.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
            recyclerview.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));

            //toolbar.setVisibility(View.VISIBLE);
            //recyclerview.setVisibility(View.VISIBLE);
            //floating_back_button.setVisibility(View.VISIBLE);
            toolbar_visible = true;
            handler.postDelayed(runnable, Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
        }
    }

    private Bitmap getBitmap(PdfRenderer pdfRenderer, int i) {

        PdfRenderer.Page page = pdfRenderer.openPage(i);
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();

        Bitmap bitmap = Global.scaleToFitHeight(Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888), Global.SCREEN_HEIGHT);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0f, 0f, null);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        return bitmap;
    }

    private class PdfViewPagerAdapter extends PagerAdapter {
        TouchImageView image_view;

        PdfViewPagerAdapter() {
            title.setText(viewModel.currently_shown_file.getName());
        }

        @Override
        public int getCount() {
            return viewModel.total_pages;
        }

        @Override
        public boolean isViewFromObject(View p1, Object p2) {
            return p1.equals(p2);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = LayoutInflater.from(context).inflate(R.layout.image_viewpager_layout, container, false);
            image_view = v.findViewById(R.id.picture_viewpager_layout_imageview);
            image_view.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image_view.setMaxZoom(6);
            image_view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    image_view_on_click_procedure();
                }
            });

            v.setTag(position);
            container.addView(v);
            new BitmapFetchAsyncTask(position).execute();
            return v;
        }


        @Override
        public int getItemPosition(Object object) {
            return POSITION_UNCHANGED;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    private class BitmapFetchAsyncTask extends AsyncTask<Void, Void, Void> {
        final int position;
        Bitmap bitmap;

        BitmapFetchAsyncTask(int p) {
            position = p;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (viewModel.size_per_page_MB * 5 < (availableHeapMemory - SAFE_MEMORY_BUFFER)) {

                try {
                    bitmap = getBitmap(viewModel.pdfRenderer, position);
                } catch (SecurityException e) {
                    Global.print_background_thread(context, getString(R.string.security_exception_thrown));
                } catch (OutOfMemoryError error) {
                    Global.print_background_thread(context, getString(R.string.outofmemory_exception_thrown));
                    getActivity().finish();
                } catch (Exception e) {
                    Global.print_background_thread(context, getString(R.string.exception_thrown));
                }
            } else {
                Global.print_background_thread(context, getString(R.string.outofmemory_exception_thrown));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            View view = view_pager.findViewWithTag(position);
            if (view != null) {
                FrameLayout frameLayout = (FrameLayout) view;
                ImageView imageView = frameLayout.findViewById(R.id.picture_viewpager_layout_imageview);
                try {
                    GlideApp.with(context).load(bitmap).placeholder(R.drawable.pdf_water_icon).error(R.drawable.pdf_water_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(imageView);
                    view.setTag(position);
                } catch (IllegalArgumentException e) {
                    cancel(true);
                }
            }
        }
    }

    private class PictureSelectorAdapter extends RecyclerView.Adapter<PictureSelectorAdapter.VH> {
        final int total_pages;

        PictureSelectorAdapter(int total_pages) {
            this.total_pages = total_pages;
        }

        @Override
        public PictureSelectorAdapter.VH onCreateViewHolder(ViewGroup parent, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.pdf_page_selector_recyclerview_layout, parent, false);
            return new PictureSelectorAdapter.VH(v);
        }

        @Override
        public void onBindViewHolder(PictureSelectorAdapter.VH p1, int p2) {
            p1.textView.setText(p2 + 1 + "");
            p1.v.setSelected(viewModel.mselecteditems.containsKey(p2));

        }

        @Override
        public int getItemCount() {
            return total_pages;
        }

        private class VH extends RecyclerView.ViewHolder implements View.OnClickListener {
            final View v;
            final TextView textView;

            VH(View view) {
                super(view);
                v = view;
                textView = v.findViewById(R.id.pdf_page_selector_textview);
                v.setOnClickListener(this);
            }

            @Override
            public void onClick(View p1) {
                if (progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.wait_ellipse));
                } else {
                    view_pager.setCurrentItem(getBindingAdapterPosition());
                }
            }
        }
    }
}



