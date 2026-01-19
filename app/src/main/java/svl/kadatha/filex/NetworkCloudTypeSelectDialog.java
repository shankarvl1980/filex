package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NetworkCloudTypeSelectDialog extends DialogFragment {
    private Context context;
    public static final String NETWORK = "network";
    public static final String CLOUD = "cloud";
    private String request_code, what_type_network_cloud;

    private NetworkCloudTypeSelectDialog() {}

    public static NetworkCloudTypeSelectDialog getInstance(String what_type_network_cloud, String request_code) {
        NetworkCloudTypeSelectDialog dialog = new NetworkCloudTypeSelectDialog();
        Bundle bundle = new Bundle();
        bundle.putString("what_type_network_cloud", what_type_network_cloud);
        bundle.putString("request_code", request_code);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle bundle = getArguments();
        request_code = bundle.getString("request_code");
        what_type_network_cloud = bundle.getString("what_type_network_cloud");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_network_cloud_type_select, container, false);
        TextView label=v.findViewById(R.id.network_cloud_label);
        label.setText("Select Server");
        FrameLayout progress_bar = v.findViewById(R.id.fragment_network_cloud_type_select_progressbar);
        progress_bar.setVisibility(View.GONE);

        RecyclerView recyclerview = v.findViewById(R.id.fragment_network_cloud_type_recyclerView);
        recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));

        // Build a single list of items (icon + title + optional FileObjectType)
        List<PickerItem> items = buildPickerItems();
        recyclerview.setAdapter(new NetworkCloudRecyclerAdapter(items));

        ViewGroup button_layout = v.findViewById(R.id.fragment_network_cloud_type_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button cancel = button_layout.findViewById(R.id.first_button);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(p1 -> dismissAllowingStateLoss());

        return v;
    }

    private List<PickerItem> buildPickerItems() {
        if (NETWORK.equals(what_type_network_cloud)) {
            List<String> networkTypes = Arrays.asList(getResources().getStringArray(R.array.network_types));
            List<PickerItem> items = new ArrayList<>(networkTypes.size());
            for (String s : networkTypes) {
                // icon per item; can be different later if you want
                items.add(new PickerItem(R.drawable.network_icon, s, null));
            }
            return items;
        } else if (CLOUD.equals(what_type_network_cloud)) {
            List<String> cloudTypes = Arrays.asList(getResources().getStringArray(R.array.cloud_types));
            List<PickerItem> items = new ArrayList<>(cloudTypes.size());

            // TODO: map each cloud type to the correct FileObjectType.
            // Right now it's placeholder "null" like your original code.
            // Example:
            // items.add(new PickerItem(R.drawable.cloud_icon, "Google Drive", FileObjectType.GOOGLE_DRIVE));

            for (String s : cloudTypes) {
                FileObjectType fileObjectType = null;
                switch (s) {
                    case "Google Drive":
                        fileObjectType = FileObjectType.GOOGLE_DRIVE_TYPE;
                        break;
                    case "Drop Box":
                        fileObjectType = FileObjectType.DROP_BOX_TYPE;
                        break;
                    case "MediaFire":
                        fileObjectType = FileObjectType.MEDIA_FIRE_TYPE;
                        break;
                    case "Yandex":
                        fileObjectType = FileObjectType.YANDEX_TYPE;
                        break;
                    default:
                }
                items.add(new PickerItem(R.drawable.cloud_icon, s, fileObjectType));
            }
            return items;
        }

        return new ArrayList<>();
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, GridLayout.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    /**
     * Small POJO for the adapter.
     * - iconResId + title are displayed
     * - fileObjectType is optional and used only on click (cloud case)
     */
    private static class PickerItem implements Serializable {
        @DrawableRes final int iconResId;
        @NonNull final String title;
        final FileObjectType fileObjectType; // may be null

        PickerItem(@DrawableRes int iconResId, @NonNull String title, FileObjectType fileObjectType) {
            this.iconResId = iconResId;
            this.title = title;
            this.fileObjectType = fileObjectType;
        }
    }

    private class NetworkCloudRecyclerAdapter extends RecyclerView.Adapter<NetworkCloudRecyclerAdapter.ViewHolder> {
        final List<PickerItem> items;

        NetworkCloudRecyclerAdapter(List<PickerItem> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PickerItem item = items.get(position);
            holder.textView_network_cloud.setText(item.title);
            holder.imageview.setImageDrawable(ContextCompat.getDrawable(context, item.iconResId));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View v;
            final ImageView imageview;
            final ImageView play_overlay_imageview, pdf_overlay_imageview;
            final TextView textView_network_cloud;

            ViewHolder(View v) {
                super(v);
                this.v = v;

                imageview = v.findViewById(R.id.image_storage_dir);
                play_overlay_imageview = v.findViewById(R.id.play_overlay_image_storage_dir);
                pdf_overlay_imageview = v.findViewById(R.id.pdf_overlay_image_storage_dir);
                textView_network_cloud = v.findViewById(R.id.text_storage_dir_name);

                play_overlay_imageview.setVisibility(View.GONE);

                v.setOnClickListener(p -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    PickerItem clicked = items.get(pos);
                    Bundle bundle = new Bundle();

                    if (NETWORK.equals(what_type_network_cloud)) {
                        // Keep exactly your old behavior: "type" from displayed string
                        String type = clicked.title.toLowerCase(Locale.ROOT);
                        bundle.putString("type", type);
                    } else if (CLOUD.equals(what_type_network_cloud)) {
                        // Only cloud needs fileObjectType in bundle
                        bundle.putSerializable("fileObjectType", clicked.fileObjectType);
                        // If you also want the selected cloud label, uncomment:
                        // bundle.putString("cloudTypeLabel", clicked.title);
                    }

                    getParentFragmentManager().setFragmentResult(request_code, bundle);
                    dismissAllowingStateLoss();
                });
            }
        }
    }
}
