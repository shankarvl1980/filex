package svl.kadatha.filex.imagepdfvideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.CopyToActivity;
import svl.kadatha.filex.DeleteFileAlertDialogOtherActivity;
import svl.kadatha.filex.DeleteFileOtherActivityViewModel;
import svl.kadatha.filex.FileIntentDispatch;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.GlideApp;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.ListPopupWindowPOJO;
import svl.kadatha.filex.PropertiesDialog;
import svl.kadatha.filex.R;
import svl.kadatha.filex.SAFPermissionHelperDialog;
import svl.kadatha.filex.UriPOJO;
import svl.kadatha.filex.instacrop.InstaCropperActivity;

public class
ImageViewFragment extends Fragment {
    private static final String DELETE_FILE_REQUEST_CODE = "image_file_delete_request_code";
    private final static String SAF_PERMISSION_REQUEST_CODE = "image_view_fragment_saf_permission_request_code";
    public FilteredFilePOJOViewModel viewModel;
    public FrameLayout progress_bar;
    private ViewPager view_pager;
    private Context context;
    private final ActivityResultLauncher<Intent> activityResultLauncher_crop_request = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                progress_bar.setVisibility(View.VISIBLE);
                viewModel.setWallPaper(result, context.getExternalCacheDir());
            } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                Global.print(context, getString(R.string.could_not_be_set_as_wallpaper));
            }
        }
    });
    private ImageViewPagerAdapter image_view_adapter;
    private LinearLayoutManager lm;
    private PictureSelectorAdapter picture_selector_adapter;
    private int preview_image_offset;
    private Handler handler;
    private Runnable runnable;
    private boolean is_menu_opened;
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    private TextView title;
    private List<FilePOJO> files_selected_for_delete;
    private Uri data;
    private int floating_button_height;
    private int recyclerview_height;
    private FloatingActionButton floating_back_button;
    private boolean toolbar_visible;
    private LinearLayout toolbar_group;
    private TextView current_image_tv;
    private AppCompatActivity activity;
    private String tree_uri_path;
    private Uri tree_uri;
    private RecyclerView recyclerview;


    public static ImageViewFragment getNewInstance(String file_path, FileObjectType fileObjectType, boolean fromArchive) {
        ImageViewFragment frag = new ImageViewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("file_path", file_path);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE, fileObjectType);
        bundle.putBoolean("fromArchive", fromArchive);
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        activity = (AppCompatActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list_popupwindowpojos = new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon, getString(R.string.delete), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 2));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.copy_icon, getString(R.string.copy_to), 3));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 4));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.wallpaper_icon, getString(R.string.set_wallpaper), 5));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.redo_icon, getString(R.string.rotate), 6));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.undo_icon, getString(R.string.rotate), 7));

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
        Toolbar toolbar = v.findViewById(R.id.activity_picture_toolbar);
        title = v.findViewById(R.id.activity_picture_name);
        ImageView overflow = v.findViewById(R.id.activity_picture_overflow);
        overflow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_menu_opened = true;
                listPopWindow.showAsDropDown(v, 0, Global.LIST_POPUP_WINDOW_DROP_DOWN_OFFSET);
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
                        if (viewModel.fromArchive || viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE, files_selected_array, viewModel.fileObjectType);
                        deleteFileAlertDialogOtherActivity.show(getParentFragmentManager(), "deletefilealertotheractivity");
                        break;
                    case 1:
                        Uri src_uri;
                        if (viewModel.fromThirdPartyApp) {
                            src_uri = data;
                        } else {
                            src_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
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
                        Uri copy_uri;
                        if (viewModel.fromThirdPartyApp) {
                            copy_uri = data;
                        } else {
                            copy_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                        if (copy_uri == null) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }

                        if (activity instanceof ImageViewActivity) {
                            ((ImageViewActivity) activity).clear_cache = false;
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
                        if (viewModel.fromArchive || viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, viewModel.fileObjectType);
                        propertiesDialog.show(getParentFragmentManager(), "properties_dialog");
                        break;
                    case 4:
                        Uri uri;
                        if (viewModel.fromThirdPartyApp) {
                            uri = data;
                        } else {
                            uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                        if (uri == null) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        if (context == null) {
                            context = getContext();
                        }

                        if (activity instanceof ImageViewActivity) {
                            ((ImageViewActivity) activity).clear_cache = false;
                        }
                        File tempFile = new File(context.getExternalCacheDir(), viewModel.currently_shown_file.getName());
                        Intent intent = InstaCropperActivity.getIntent(context, uri, FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", tempFile), viewModel.currently_shown_file.getName(), Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT, 100);
                        activityResultLauncher_crop_request.launch(intent);
                        break;
                    case 5:
                        handleImageRotation(90);
                        break;
                    case 6:
                        handleImageRotation(-90);
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

        toolbar_group = v.findViewById(R.id.image_view_toolbar_group);
        current_image_tv = v.findViewById(R.id.image_view_current_view_number);
        progress_bar = v.findViewById(R.id.activity_picture_progressbar);

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
                viewModel.file_selected_idx = i;
                current_image_tv.setText(viewModel.file_selected_idx + 1 + "/" + viewModel.total_images);
                viewModel.currently_shown_file = viewModel.album_file_pojo_list.get(i);
                title.setText(viewModel.currently_shown_file.getName());
            }
        });

        runnable = new Runnable() {
            public void run() {
                if (!is_menu_opened) {
                    toolbar_group.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
                    floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
                    recyclerview.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));

                    //toolbar_group.setVisibility(View.GONE);
                    //recyclerview.setVisibility(View.GONE);
                    //floating_back_button.setVisibility(View.GONE);
                    toolbar_visible = false;
                }
            }
        };


        viewModel = new ViewModelProvider(this).get(FilteredFilePOJOViewModel.class);
        if (activity instanceof ImageViewActivity) {
            data = ((ImageViewActivity) activity).data;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            viewModel.file_path = bundle.getString("file_path");
            viewModel.fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
            viewModel.fromArchive = bundle.getBoolean("fromArchive");
            if (viewModel.fileObjectType == null || viewModel.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                viewModel.fileObjectType = FileObjectType.FILE_TYPE;
                viewModel.fromThirdPartyApp = true;
            }
        }

        viewModel.getAlbumFromCurrentFolder(Global.IMAGE_REGEX, false);
        viewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (Global.whether_file_cached(viewModel.fileObjectType)) {
                        if (activity instanceof ImageViewActivity) {
                            ((ImageViewActivity) activity).data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                    }
                    image_view_adapter = new ImageViewPagerAdapter(viewModel.album_file_pojo_list);
                    view_pager.setAdapter(image_view_adapter);
                    view_pager.setCurrentItem(viewModel.file_selected_idx);
                    current_image_tv.setText(viewModel.file_selected_idx + 1 + "/" + viewModel.total_images);
                    viewModel.mselecteditems.put(viewModel.file_selected_idx, true);
                    picture_selector_adapter = new PictureSelectorAdapter(viewModel.album_file_pojo_list);
                    lm = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                    recyclerview.setLayoutManager(lm);
                    recyclerview.setAdapter(picture_selector_adapter);
                    lm.scrollToPositionWithOffset(viewModel.file_selected_idx, -preview_image_offset);
                }
            }
        });


        DeleteFileOtherActivityViewModel deleteFileOtherActivityViewModel = new ViewModelProvider(ImageViewFragment.this).get(DeleteFileOtherActivityViewModel.class);
        deleteFileOtherActivityViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!deleteFileOtherActivityViewModel.deleted_files.isEmpty()) {
                        viewModel.album_file_pojo_list.removeAll(deleteFileOtherActivityViewModel.deleted_files);
                        viewModel.total_images = viewModel.album_file_pojo_list.size();
                        image_view_adapter.notifyDataSetChanged();
                        picture_selector_adapter.notifyDataSetChanged();
                        if (viewModel.album_file_pojo_list.isEmpty()) {
                            getActivity().finish();
                        }
                    }
                    deleteFileOtherActivityViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.isRotated.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    image_view_adapter.notifyDataSetChanged();
                    picture_selector_adapter.notifyDataSetChanged();
                    progress_bar.setVisibility(View.GONE);
                    viewModel.isRotated.setValue(AsyncTaskStatus.NOT_YET_STARTED);
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
                    files_selected_for_delete = new ArrayList<>();
                    files_selected_for_delete.add(viewModel.currently_shown_file);
                    deleteFileOtherActivityViewModel.deleteFilePOJO(viewModel.source_folder, files_selected_for_delete, viewModel.fileObjectType, tree_uri, tree_uri_path);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE)) {
                    tree_uri = result.getParcelable("tree_uri");
                    tree_uri_path = result.getString("tree_uri_path");
                    if (!FileUtil.isWritable(viewModel.currently_shown_file.getFileObjectType(), viewModel.currently_shown_file.getPath())) {
                        if (!check_SAF_permission(viewModel.currently_shown_file.getPath(), viewModel.currently_shown_file.getFileObjectType())) {
                            return;
                        }
                    } else {
                        progress_bar.setVisibility(View.VISIBLE);
                        viewModel.rotate(viewModel.rotation_degrees, tree_uri, tree_uri_path);
                    }
                }
            }
        });


        viewModel.hasWallPaperSet.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    viewModel.hasWallPaperSet.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });
        return v;
    }

    private boolean check_SAF_permission(String file_path, FileObjectType fileObjectType) {
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path, fileObjectType);
        if (uriPOJO != null) {
            tree_uri_path = uriPOJO.get_path();
            tree_uri = uriPOJO.get_uri();
        }

        if (uriPOJO == null || tree_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE, file_path, fileObjectType);
            safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
            return false;
        } else {
            return true;
        }
    }

    private void handleImageRotation(int degrees) {
        viewModel.rotation_degrees = degrees;
        if (viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
            rotateImageInMemory(degrees);
        } else {
            rotateImageFile(degrees);
        }
    }

    private void rotateImageInMemory(int degrees) {
        TouchImageView currentView = (TouchImageView) ((ViewGroup) view_pager.getChildAt(0)).getChildAt(view_pager.getCurrentItem());
        if (currentView != null) {
            TouchImageView imageView = currentView.findViewById(R.id.picture_viewpager_layout_imageview);
            if (imageView != null) {
                Drawable drawable = imageView.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

                    // Rotate the bitmap
                    Matrix matrix = new Matrix();
                    matrix.postRotate(degrees);
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
    }

    private void rotateImageFile(int degrees) {
        if (!FileUtil.isWritable(viewModel.currently_shown_file.getFileObjectType(), viewModel.currently_shown_file.getPath())) {
            if (!check_SAF_permission(viewModel.currently_shown_file.getPath(), viewModel.currently_shown_file.getFileObjectType())) {
                listPopWindow.dismiss();
                return;
            }
        }
        progress_bar.setVisibility(View.VISIBLE);
        viewModel.rotate(degrees, tree_uri, tree_uri_path);
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


            //toolbar_group.setVisibility(View.GONE);
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

    private class ImageViewPagerAdapter extends PagerAdapter {
        final List<FilePOJO> albumList;
        TouchImageView image_view;
        FilePOJO f;

        ImageViewPagerAdapter(List<FilePOJO> albumList) {
            this.albumList = albumList;
            title.setText(viewModel.currently_shown_file.getName());
        }

        @Override
        public int getCount() {
            return albumList.size();
        }

        @Override
        public boolean isViewFromObject(View p1, Object p2) {
            return p1.equals(p2);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = LayoutInflater.from(context).inflate(R.layout.image_viewpager_layout, container, false);
            image_view = v.findViewById(R.id.picture_viewpager_layout_imageview);
            image_view.setMaxZoom(6);
            image_view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    image_view_on_click_procedure();
                }
            });

            f = albumList.get(position);
            if (viewModel.fromThirdPartyApp) {
                if (activity instanceof ImageViewActivity) {
                    data = ((ImageViewActivity) activity).data;
                    GlideApp.with(context).load(data).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(image_view);
                }
            } else if (f.getFileObjectType() == FileObjectType.FILE_TYPE) {
                GlideApp.with(context)
                        .load(new File(f.getPath()))
                        .signature(new ObjectKey(new File(f.getPath()).lastModified()))
                        .placeholder(R.drawable.picture_icon)
                        .error(R.drawable.picture_icon)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .dontAnimate()
                        .into(image_view);
            } else if (f.getFileObjectType() == FileObjectType.USB_TYPE) {
                GlideApp.with(context).load(data).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(image_view);
            } else if (f.getFileObjectType() == FileObjectType.ROOT_TYPE) {
                GlideApp.with(context)
                        .load(new File(f.getPath()))
                        .signature(new ObjectKey(new File(f.getPath()).lastModified()))
                        .placeholder(R.drawable.picture_icon)
                        .error(R.drawable.picture_icon)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .dontAnimate()
                        .into(image_view);
            }
            v.setTag(position);
            container.addView(v);
            return v;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return albumList.get(position).getName();
        }
    }


    private class PictureSelectorAdapter extends RecyclerView.Adapter<PictureSelectorAdapter.VH> {
        final List<FilePOJO> picture_list;

        PictureSelectorAdapter(List<FilePOJO> list) {
            picture_list = list;
        }

        @Override
        public PictureSelectorAdapter.VH onCreateViewHolder(ViewGroup parent, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.image_selector_recyclerview_layout, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(PictureSelectorAdapter.VH p1, int p2) {
            FilePOJO f = picture_list.get(p2);
            if (viewModel.fromThirdPartyApp) {
                if (activity instanceof ImageViewActivity) {
                    data = ((ImageViewActivity) activity).data; //on rotation oncreateview is created first, data is null in oncreate
                    GlideApp.with(context).load(data).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(p1.imageview);
                }
            } else if (f.getFileObjectType() == FileObjectType.FILE_TYPE) {
                GlideApp.with(context)
                        .load(new File(f.getPath()))
                        .signature(new ObjectKey(new File(f.getPath()).lastModified()))
                        .placeholder(R.drawable.picture_icon)
                        .error(R.drawable.picture_icon)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .dontAnimate()
                        .into(p1.imageview);
            } else if (f.getFileObjectType() == FileObjectType.USB_TYPE) {
                GlideApp.with(context).load(data).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(p1.imageview);
            } else if (f.getFileObjectType() == FileObjectType.ROOT_TYPE) {
                GlideApp.with(context)
                        .load(new File(f.getPath()))
                        .signature(new ObjectKey(new File(f.getPath()).lastModified()))
                        .placeholder(R.drawable.picture_icon)
                        .error(R.drawable.picture_icon)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .dontAnimate()
                        .into(p1.imageview);
            }
            p1.v.setSelected(viewModel.mselecteditems.containsKey(p2));
        }

        @Override
        public int getItemCount() {
            return picture_list.size();
        }

        private class VH extends RecyclerView.ViewHolder implements View.OnClickListener {
            final View v;
            final ImageView imageview;

            VH(View view) {
                super(view);
                v = view;
                imageview = v.findViewById(R.id.picture_viewpager_layout_imageview);
                v.setOnClickListener(this);
            }

            @Override
            public void onClick(View p1) {
                view_pager.setCurrentItem(getBindingAdapterPosition());
            }
        }
    }
}
