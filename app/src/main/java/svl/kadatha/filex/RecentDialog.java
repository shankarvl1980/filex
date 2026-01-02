package svl.kadatha.filex;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.LinkedList;
import java.util.Locale;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import svl.kadatha.filex.usb.UsbDocumentProvider;

public class RecentDialog extends DialogFragment implements MainActivity.RecentDialogListener {
    public static final int RECENT_SIZE = 30;
    private final static String SAF_PERMISSION_REQUEST_CODE = "recent_dialog_saf_permission_request_code";
    private final LinkedList<FilePOJO> root_dir_linkedlist = new LinkedList<>();
    private Context context;
    private Uri tree_uri;
    private String tree_uri_path = "";
    private RecentRecyclerAdapter rootdirrecycleradapter, recentRecyclerAdapter;
    private RecyclerView recent_recyclerview;
    private TextView recent_label;
    private FilePOJO clicked_filepojo;

    public static void ADD_FILE_POJO_TO_RECENT(FilePOJO filePOJO) {
        if (!MainActivity.RECENT.isEmpty()) {
            if ((!MainActivity.RECENT.getFirst().getPath().equals(filePOJO.getPath()))) {
                if (MainActivity.RECENT.size() >= RECENT_SIZE) {
                    MainActivity.RECENT.removeLast();
                }
                MainActivity.RECENT.addFirst(filePOJO);
            }
        } else {
            MainActivity.RECENT.addFirst(filePOJO);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).recentDialogListener = this;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).recentDialogListener = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        root_dir_linkedlist.addAll(RepositoryClass.getRepositoryClass().storage_dir); ////adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recent, container, false);
        RecyclerView root_dir_recyclerview = v.findViewById(R.id.dialog_recent_root_dir_recycler_view);
        recent_recyclerview = v.findViewById(R.id.dialog_recent_recycler_view);
        recent_label = v.findViewById(R.id.recent_label);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_recent_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button recent_clear_button = buttons_layout.findViewById(R.id.first_button);
        recent_clear_button.setText(R.string.clear);
        Button close_button = buttons_layout.findViewById(R.id.second_button);
        close_button.setText(R.string.close);

        rootdirrecycleradapter = new RecentRecyclerAdapter(root_dir_linkedlist, true);
        root_dir_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        root_dir_recyclerview.setAdapter(rootdirrecycleradapter);
        root_dir_recyclerview.setLayoutManager(new LinearLayoutManager(context));

        recentRecyclerAdapter = new RecentRecyclerAdapter(MainActivity.RECENT, false);
        recent_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        recent_recyclerview.setAdapter(recentRecyclerAdapter);
        recent_recyclerview.setLayoutManager(new LinearLayoutManager(context));

        recent_clear_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                recentRecyclerAdapter.clear_recents();
                MainActivity.RECENT = new LinkedList<>();
            }
        });

        close_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }

        });

        getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE)) {
                    tree_uri = result.getParcelable("tree_uri");
                    tree_uri_path = result.getString("tree_uri_path");
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            if (!Global.IS_TABLET) {
                recent_label.setVisibility(View.GONE);
                recent_recyclerview.setAdapter(null);
            }
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_WIDTH);
        } else {
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_HEIGHT);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void file_open_intent_dispatch(final String file_path, final FileObjectType fileObjectType, String file_name, long file_size) {
        int idx = file_name.lastIndexOf(".");
        String file_ext = "";
        if (idx > 0) {
            file_ext = file_name.substring(idx + 1);
        }

        if (file_ext.isEmpty() || Global.NO_APPS_FOR_RECOGNISED_FILE_EXT(context, file_ext)) {
            FileTypeSelectDialog fileTypeSelectFragment = FileTypeSelectDialog.getInstance(file_path, fileObjectType, tree_uri, tree_uri_path, false, file_size);
            fileTypeSelectFragment.show(getParentFragmentManager(), "");
        } else {
            if (Global.APK_EXT_SET.contains(file_ext.toLowerCase(Locale.ROOT))) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!getActivity().getPackageManager().canRequestPackageInstalls()) {
                        Intent unknown_package_install_intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        unknown_package_install_intent.setData(Uri.parse(String.format("package:%s", Global.FILEX_PACKAGE)));
                        activityResultLauncher_unknown_package_install_permission.launch(unknown_package_install_intent);
                        return;
                    }
                }
            }

            if (fileObjectType == FileObjectType.USB_TYPE) {
                if (file_size > Global.CACHE_FILE_MAX_LIMIT) {
                    Global.print(context, context.getString(R.string.file_is_large_copy_to_device_storage));
                    return;
                }

                if (!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(fileObjectType, null)) {
                    Global.print(context, context.getString(R.string.wait_till_completion_on_going_operation_on_usb));
                    return;
                }
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, false, file_size, false);

            } else if (Global.whether_file_cached(fileObjectType)) {
                if (file_size > Global.CACHE_FILE_MAX_LIMIT) {
                    Global.print(context, context.getString(R.string.file_is_large_copy_to_device_storage));
                    return;
                }
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, false, file_size, false);
            } else if (fileObjectType == FileObjectType.FILE_TYPE) {
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, false, file_size, false);
            }
        }
    }

    @Override
    public void onMediaAttachedAndRemoved() {
        root_dir_linkedlist.clear();
        root_dir_linkedlist.addAll(RepositoryClass.getRepositoryClass().storage_dir); //adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
        rootdirrecycleradapter.notifyDataSetChanged();
        recentRecyclerAdapter.notifyDataSetChanged();
    }

    private void discoverDevice() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        int pending_intent_flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            for (UsbMassStorageDevice massStorageDevice : UsbMassStorageDevice.getMassStorageDevices(getContext())) {
                if (device.equals(massStorageDevice.getUsbDevice())) {
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                            UsbDocumentProvider.ACTION_USB_PERMISSION), pending_intent_flag);
                    usbManager.requestPermission(device, permissionIntent);
                    break;
                }
            }
        }
    }

    private class RecentRecyclerAdapter extends RecyclerView.Adapter<RecentRecyclerAdapter.ViewHolder> {
        final boolean storage_dir;
        LinkedList<FilePOJO> dir_linkedlist;

        RecentRecyclerAdapter(LinkedList<FilePOJO> dir_linkedlist, boolean storage_dir) {
            this.dir_linkedlist = dir_linkedlist;
            this.storage_dir = storage_dir;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new RecentRecyclerViewLayoutList(context, Global.DIALOG_WIDTH));
        }

        @Override
        public void onBindViewHolder(RecentRecyclerAdapter.ViewHolder p1, int p2) {
            FilePOJO filePOJO = dir_linkedlist.get(p2);
            if (filePOJO == null) {
                return;
            }
            if (storage_dir) {
                FileObjectType fileObjectType = filePOJO.getFileObjectType();
                String space = "";
                SpacePOJO spacePOJO = Global.SPACE_ARRAY.get(fileObjectType + filePOJO.getPath());
                if (spacePOJO != null) {
                    space = " (" + spacePOJO.getUsedSpaceReadable() + "/" + spacePOJO.getTotalSpaceReadable() + ")";
                }
                if (fileObjectType == FileObjectType.FILE_TYPE) {
                    if (Global.GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR().getPath().equals(filePOJO.getPath())) {
                        p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
                    } else {
                        p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.sdcard_icon));
                    }
                    p1.textView_recent_dir.setText(filePOJO.getName() + space);
                } else if (fileObjectType == FileObjectType.USB_TYPE) {
                    p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.usb_icon));
                    p1.textView_recent_dir.setText(DetailFragment.USB_FILE_PREFIX + filePOJO.getName() + space);
                } else if (fileObjectType == FileObjectType.ROOT_TYPE) {
                    if (filePOJO.getPath().equals(File.separator)) {
                        p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
                        p1.textView_recent_dir.setText(R.string.root_directory);
                    }
                } else if (fileObjectType == FileObjectType.FTP_TYPE) {
                    p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                    p1.textView_recent_dir.setText(DetailFragment.FTP_FILE_PREFIX + filePOJO.getName() + space);
                } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
                    p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                    p1.textView_recent_dir.setText(DetailFragment.SFTP_FILE_PREFIX + filePOJO.getName() + space);
                } else if (fileObjectType == FileObjectType.WEBDAV_TYPE) {
                    p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                    p1.textView_recent_dir.setText(DetailFragment.WEBDAV_FILE_PREFIX + filePOJO.getName() + space);
                } else if (fileObjectType == FileObjectType.SMB_TYPE) {
                    p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                    p1.textView_recent_dir.setText(DetailFragment.SMB_FILE_PREFIX + filePOJO.getName() + space);
                }
            } else {
                p1.view.setData(filePOJO);
            }
        }

        @Override
        public int getItemCount() {
            return dir_linkedlist.size();
        }

        public void clear_recents() {
            dir_linkedlist = new LinkedList<>();
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final RecentRecyclerViewLayoutList view;
            final ImageView fileimageview;
            final ImageView play_overlay_imageview, pdf_overlay_imageview;
            final TextView textView_recent_dir;
            int pos;

            ViewHolder(RecentRecyclerViewLayoutList view) {
                super(view);
                this.view = view;
                fileimageview = view.findViewById(R.id.recent_image_storage_dir);
                play_overlay_imageview = view.findViewById(R.id.recent_play_overlay_image_storage_dir);
                pdf_overlay_imageview = view.findViewById(R.id.recent_pdf_overlay_image_storage_dir);
                textView_recent_dir = view.findViewById(R.id.recent_text_storage_dir_name);

                this.view.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        pos = getBindingAdapterPosition();
                        final FilePOJO filePOJO = dir_linkedlist.get(pos);
                        if (filePOJO == null) {
                            return;
                        }
                        clicked_filepojo = filePOJO;
                        if (filePOJO.getIsDirectory()) {
                            AppCompatActivity activity = (AppCompatActivity) getActivity();
                            if (activity instanceof MainActivity) {
                                ((MainActivity) activity).createFragmentTransaction(filePOJO.getPath(), filePOJO.getFileObjectType());
                            }
                        } else {
                            file_open_intent_dispatch(filePOJO.getPath(), filePOJO.getFileObjectType(), filePOJO.getName(), filePOJO.getSizeLong());
                        }

                        ADD_FILE_POJO_TO_RECENT(filePOJO);
                        dismissAllowingStateLoss();
                    }
                });
            }
        }
    }    private final ActivityResultLauncher<Intent> activityResultLauncher_unknown_package_install_permission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (clicked_filepojo != null) {
                    file_open_intent_dispatch(clicked_filepojo.getPath(), clicked_filepojo.getFileObjectType(), clicked_filepojo.getName(), clicked_filepojo.getSizeLong());
                }
                clicked_filepojo = null;
            } else {
                Global.print(context, getString(R.string.permission_not_granted));
            }
        }
    });


}
