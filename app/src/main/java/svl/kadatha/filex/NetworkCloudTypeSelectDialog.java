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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NetworkCloudTypeSelectDialog extends DialogFragment {
    public static final String NETWORK = "network";
    public static final String CLOUD = "cloud";
    public static final String HOST = "host";
    private Context context;
    private String request_code, what_type_network_cloud;
    private final ArrayList<PickerItem> items = new ArrayList<>();
    private NetworkCloudRecyclerAdapter adapter;

    private NetworkCloudHostPickerDialogViewModel viewModel;

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
        TextView label = v.findViewById(R.id.network_cloud_label);
        TextView nothing_tv = v.findViewById(R.id.fragment_network_cloud_type_select_nothing_found_tv);
        label.setText(R.string.select_server);
        FrameLayout progress_bar = v.findViewById(R.id.fragment_network_cloud_type_select_progressbar);
        progress_bar.setVisibility(View.GONE);

        RecyclerView recyclerview = v.findViewById(R.id.fragment_network_cloud_type_recyclerView);
        recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        ViewGroup button_layout = v.findViewById(R.id.fragment_network_cloud_type_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button cancel = button_layout.findViewById(R.id.first_button);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(p1 -> dismissAllowingStateLoss());
        adapter = new NetworkCloudRecyclerAdapter(items);
        recyclerview.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(NetworkCloudHostPickerDialogViewModel.class);

        if (HOST.equals(what_type_network_cloud)) {
            items.clear();
            adapter.notifyDataSetChanged();
            viewModel.scanStandardPorts(requireContext().getApplicationContext());
        } else {
            items.clear();
            items.addAll(buildPickerItems());
            adapter.notifyDataSetChanged();
        }

        viewModel.scanHostAsyncTaskStatus.observe(getViewLifecycleOwner(), st -> {
            if (!HOST.equals(what_type_network_cloud)) return;
            if (st == AsyncTaskStatus.STARTED) {
                label.setText(R.string.scanning);
            } else if (st == AsyncTaskStatus.COMPLETED) {
                label.setText(R.string.select_server);
                if (items.isEmpty()) {
                    nothing_tv.setVisibility(View.VISIBLE);
                    recyclerview.setItemViewCacheSize(View.GONE);
                }
            }
        });

        viewModel.newResult.observe(getViewLifecycleOwner(), r -> {
            if (!HOST.equals(what_type_network_cloud) || r == null) return;
            int pos = items.size();
            items.add(PickerItem.forHost(r.display, r.host, r.port));
            adapter.notifyItemInserted(pos);
        });

        return v;
    }

    private List<PickerItem> buildPickerItems() {
        if (NETWORK.equals(what_type_network_cloud)) {
            List<String> networkTypes = Arrays.asList(getResources().getStringArray(R.array.network_types));
            List<PickerItem> out = new ArrayList<>(networkTypes.size());
            for (String s : networkTypes) out.add(new PickerItem(R.drawable.network_icon, s, null));
            return out;
        } else if (CLOUD.equals(what_type_network_cloud)) {
            List<String> cloudTypes = Arrays.asList(getResources().getStringArray(R.array.cloud_types));
            List<PickerItem> out = new ArrayList<>(cloudTypes.size());
            for (String s : cloudTypes) {
                FileObjectType fot = null;
                switch (s) {
                    case "Google Drive":
                        fot = FileObjectType.GOOGLE_DRIVE_TYPE;
                        break;
                    case "Drop Box":
                        fot = FileObjectType.DROP_BOX_TYPE;
                        break;
                    case "MediaFire":
                        fot = FileObjectType.MEDIA_FIRE_TYPE;
                        break;
                    case "Yandex":
                        fot = FileObjectType.YANDEX_TYPE;
                        break;
                }
                out.add(new PickerItem(R.drawable.cloud_icon, s, fot));
            }
            return out;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) viewModel.cancel(true);
    }


    private static class PickerItem implements Serializable {
        @DrawableRes
        final int iconResId;
        @NonNull
        final String title;

        final FileObjectType fileObjectType; // for cloud
        final String host; // for host picker
        final int port;    // for host picker

        PickerItem(@DrawableRes int iconResId, @NonNull String title, FileObjectType fileObjectType) {
            this.iconResId = iconResId;
            this.title = title;
            this.fileObjectType = fileObjectType;
            this.host = null;
            this.port = 0;
        }

        static PickerItem forHost(@NonNull String title, @NonNull String host, int port) {
            PickerItem p = new PickerItem(R.drawable.network_icon, title, null);
            // hack-free: create a separate constructor if you prefer
            // but keeping minimal changes:
            return new PickerItem(R.drawable.network_icon, title, null, host, port);
        }

        private PickerItem(@DrawableRes int iconResId, @NonNull String title, FileObjectType fileObjectType,
                           String host, int port) {
            this.iconResId = iconResId;
            this.title = title;
            this.fileObjectType = fileObjectType;
            this.host = host;
            this.port = port;
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

            if (HOST.equals(what_type_network_cloud)) {
                holder.imageview.setVisibility(View.GONE);
                holder.pdf_overlay_imageview.setVisibility(View.GONE);
            } else {
                holder.imageview.setVisibility(View.VISIBLE);
                holder.imageview.setImageDrawable(ContextCompat.getDrawable(context, item.iconResId));
            }
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
                    } else if (HOST.equals(what_type_network_cloud)) {
                        if (clicked.host != null && !clicked.host.isEmpty()) {
                            bundle.putString("host", clicked.host);
                            bundle.putInt("port", clicked.port);
                        }
                    }

                    getParentFragmentManager().setFragmentResult(request_code, bundle);
                    dismissAllowingStateLoss();
                });
            }
        }
    }
}
