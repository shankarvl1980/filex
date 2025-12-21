package svl.kadatha.filex;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.LinkedList;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import svl.kadatha.filex.usb.UsbDocumentProvider;

public class FileSelectorRecentDialog extends DialogFragment implements FileSelectorActivity.RecentDialogListener, StorageAnalyserActivity.RecentDialogListener {
    public static final int RECENT_SIZE = 30;
    public static final String FILE_SELECTOR = "file_selector";
    public static final String STORAGE_ANALYSER = "storage_analyser";
    private final LinkedList<FilePOJO> root_dir_linkedlist = new LinkedList<>();
    private Context context;
    private RecentRecyclerAdapter rootdirrecycleradapter, recentRecyclerAdapter;
    private String activity_catering = FILE_SELECTOR;
    private RecyclerView recent_recyclerview;
    private TextView recent_label;
    private AppCompatActivity activity;

    public static void ADD_FILE_POJO_TO_RECENT(FilePOJO filePOJO, String activity_catering) {
        if (activity_catering.equals(STORAGE_ANALYSER)) {
            if (!StorageAnalyserActivity.RECENT.isEmpty()) {
                if ((!StorageAnalyserActivity.RECENT.getFirst().getPath().equals(filePOJO.getPath()))) {
                    if (StorageAnalyserActivity.RECENT.size() >= RECENT_SIZE) {
                        StorageAnalyserActivity.RECENT.removeLast();
                    }

                    StorageAnalyserActivity.RECENT.addFirst(filePOJO);
                }
            } else {
                StorageAnalyserActivity.RECENT.addFirst(filePOJO);
            }
        } else {
            if (!FileSelectorActivity.RECENT.isEmpty()) {
                if ((!FileSelectorActivity.RECENT.getFirst().getPath().equals(filePOJO.getPath()))) {
                    if (FileSelectorActivity.RECENT.size() >= RECENT_SIZE) {
                        FileSelectorActivity.RECENT.removeLast();
                    }

                    FileSelectorActivity.RECENT.addFirst(filePOJO);
                }
            } else {
                FileSelectorActivity.RECENT.addFirst(filePOJO);
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        activity = (AppCompatActivity) context;

        if (activity instanceof StorageAnalyserActivity) {
            activity_catering = STORAGE_ANALYSER;
            ((StorageAnalyserActivity) context).recentDialogListener = this;
        } else if (activity instanceof FileSelectorActivity) {
            activity_catering = FILE_SELECTOR;
            ((FileSelectorActivity) context).recentDialogListener = this;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activity instanceof StorageAnalyserActivity) {
            ((StorageAnalyserActivity) context).recentDialogListener = null;
        } else if (activity instanceof FileSelectorActivity) {
            ((FileSelectorActivity) context).recentDialogListener = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        if (activity_catering.equals(STORAGE_ANALYSER)) {
            root_dir_linkedlist.addAll(((StorageAnalyserActivity) activity).getFilePOJO_list()); ////adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
        } else if (activity_catering.equals(FILE_SELECTOR)) {
            root_dir_linkedlist.addAll(((FileSelectorActivity) activity).getFilePOJO_list()); ////adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
        }
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
        if (activity_catering.equals(STORAGE_ANALYSER)) {
            recentRecyclerAdapter = new RecentRecyclerAdapter(StorageAnalyserActivity.RECENT, false);
        } else {
            recentRecyclerAdapter = new RecentRecyclerAdapter(FileSelectorActivity.RECENT, false);
        }

        recent_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        recent_recyclerview.setAdapter(recentRecyclerAdapter);
        recent_recyclerview.setLayoutManager(new LinearLayoutManager(context));

        recent_clear_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                recentRecyclerAdapter.clear_recent();
                if (activity_catering.equals(STORAGE_ANALYSER)) {
                    StorageAnalyserActivity.RECENT = new LinkedList<>();
                } else {
                    FileSelectorActivity.RECENT = new LinkedList<>();
                }
            }
        });

        close_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissAllowingStateLoss();
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

    @Override
    public void onMediaAttachedAndRemoved() {
        root_dir_linkedlist.clear();
        if (activity_catering.equals(STORAGE_ANALYSER)) {
            root_dir_linkedlist.addAll(((StorageAnalyserActivity) context).storage_filePOJO_list); //adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
        } else {
            root_dir_linkedlist.addAll(((FileSelectorActivity) context).storage_filePOJO_list); //adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
        }
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
        private static final int VT_CENTER = 0;       // existing centered layout
        private static final int VT_TOP = 1;  // new top layout
        final boolean storage_dir;
        private final int iconHeightPx;
        LinkedList<FilePOJO> dir_linkedlist;

        RecentRecyclerAdapter(LinkedList<FilePOJO> dir_linkedlist, boolean storage_dir) {
            this.dir_linkedlist = dir_linkedlist;
            this.storage_dir = storage_dir;
            iconHeightPx = Global.THIRTY_FOUR_DP;
        }

        @Override
        public int getItemViewType(int position) {
            // Storage dir must always be centered (your requirement)
            if (storage_dir) return VT_CENTER;

            FilePOJO filePOJO = dir_linkedlist.get(position);
            if (filePOJO == null) return VT_CENTER;

            // Build the same display text you set in onBind (IMPORTANT!)
            String displayText;
            switch (filePOJO.getFileObjectType()) {
                case USB_TYPE:
                    displayText = DetailFragment.USB_FILE_PREFIX + filePOJO.getPath();
                    break;
                case FTP_TYPE:
                    displayText = DetailFragment.FTP_FILE_PREFIX + filePOJO.getPath();
                    break;
                case SFTP_TYPE:
                    displayText = DetailFragment.SFTP_FILE_PREFIX + filePOJO.getPath();
                    break;
                case WEBDAV_TYPE:
                    displayText = DetailFragment.WEBDAV_FILE_PREFIX + filePOJO.getPath();
                    break;
                case SMB_TYPE:
                    displayText = DetailFragment.SMB_FILE_PREFIX + filePOJO.getPath();
                    break;
                default:
                    displayText = filePOJO.getPath();
                    break;
            }

            // We need available text width. Use a conservative approximation from RecyclerView width.
            // This works well in practice and avoids measuring in onBind.
            int rvWidth = recent_recyclerview.getWidth(); // keep a reference to recyclerView in adapter or pass it
            if (rvWidth <= 0) return VT_CENTER; // first layout pass fallback

            int horizontalPadding = 2 * Global.FOUR_DP;
            int iconAndMargins = iconHeightPx + (2 * Global.FOUR_DP);
            int availableTextWidth = rvWidth - horizontalPadding - iconAndMargins;

            if (availableTextWidth <= 0) return VT_CENTER;

            int textHeight = measureTextHeightPx(displayText, availableTextWidth);

            // If text is taller than icon => top align
            return (textHeight > iconHeightPx) ? VT_TOP : VT_CENTER;
        }

        private int measureTextHeightPx(CharSequence text, int widthPx) {
            TextPaint paint = new TextPaint();
            paint.setAntiAlias(true);

            // Match your TextView text size: 17sp
            float textSizePx = 17f * context.getResources().getDisplayMetrics().scaledDensity;
            paint.setTextSize(textSizePx);

            StaticLayout layout;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                layout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, widthPx)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .build();
            } else {
                layout = new StaticLayout(text, paint, widthPx, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
            return layout.getHeight();
        }

        @Override
        public RecentRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int layout = (viewType == VT_TOP) ? R.layout.storage_dir_recyclerview_layout_top : R.layout.storage_dir_recyclerview_layout;
            View itemview = LayoutInflater.from(context).inflate(layout, parent, false);
            return new RecentRecyclerAdapter.ViewHolder(itemview);
        }

        @Override
        public void onBindViewHolder(RecentRecyclerAdapter.ViewHolder p1, int p2) {
            FilePOJO filePOJO = dir_linkedlist.get(p2);
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
                FileSelectorRecyclerViewLayoutList.setIcon(context, filePOJO, p1.fileimageview, p1.play_overlay_imageview, p1.pdf_overlay_imageview);
                if (filePOJO.getFileObjectType() == FileObjectType.USB_TYPE) {
                    p1.textView_recent_dir.setText(DetailFragment.USB_FILE_PREFIX + filePOJO.getPath());
                } else if (filePOJO.getFileObjectType() == FileObjectType.FTP_TYPE) {
                    p1.textView_recent_dir.setText(DetailFragment.FTP_FILE_PREFIX + filePOJO.getPath());
                } else if (filePOJO.getFileObjectType() == FileObjectType.SFTP_TYPE) {
                    p1.textView_recent_dir.setText(DetailFragment.SFTP_FILE_PREFIX + filePOJO.getPath());
                } else if (filePOJO.getFileObjectType() == FileObjectType.WEBDAV_TYPE) {
                    p1.textView_recent_dir.setText(DetailFragment.WEBDAV_FILE_PREFIX + filePOJO.getPath());
                } else if (filePOJO.getFileObjectType() == FileObjectType.SMB_TYPE) {
                    p1.textView_recent_dir.setText(DetailFragment.SMB_FILE_PREFIX + filePOJO.getPath());
                } else {
                    p1.textView_recent_dir.setText(filePOJO.getPath());
                }
            }
        }

        @Override
        public int getItemCount() {
            return dir_linkedlist.size();
        }

        public void clear_recent() {
            dir_linkedlist = new LinkedList<>();
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View view;
            final ImageView fileimageview;
            final ImageView play_overlay_imageview, pdf_overlay_imageview;
            final TextView textView_recent_dir;
            int pos;

            ViewHolder(View view) {
                super(view);
                this.view = view;
                fileimageview = view.findViewById(R.id.image_storage_dir);
                play_overlay_imageview = view.findViewById(R.id.play_overlay_image_storage_dir);
                pdf_overlay_imageview = view.findViewById(R.id.pdf_overlay_image_storage_dir);
                textView_recent_dir = view.findViewById(R.id.text_storage_dir_name);

                this.view.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        pos = getBindingAdapterPosition();
                        final FilePOJO filePOJO = dir_linkedlist.get(pos);
                        if (filePOJO.getIsDirectory()) {
                            if (activity_catering.equals(STORAGE_ANALYSER)) {
                                ((StorageAnalyserActivity) context).createFragmentTransaction(filePOJO.getPath(), filePOJO.getFileObjectType());
                            } else {
                                ((FileSelectorActivity) context).createFragmentTransaction(filePOJO.getPath(), filePOJO.getFileObjectType());
                            }

                            ADD_FILE_POJO_TO_RECENT(filePOJO, activity_catering);
                        }
                        dismissAllowingStateLoss();
                    }
                });
            }
        }
    }
}
