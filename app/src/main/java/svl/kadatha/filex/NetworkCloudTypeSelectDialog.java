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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class NetworkCloudTypeSelectDialog extends DialogFragment {
    private Context context;
    public static final String NETWORK="network";
    public static final String CLOUD="cloud";
    private String request_code, what_type_network_cloud;

    private NetworkCloudTypeSelectDialog(){};

    public static NetworkCloudTypeSelectDialog getInstance(String what_type_network_cloud,String request_code) {
        NetworkCloudTypeSelectDialog networkCloudTypeSelectDialog = new NetworkCloudTypeSelectDialog();
        Bundle bundle = new Bundle();
        bundle.putString("what_type_network_cloud", what_type_network_cloud);
        bundle.putString("request_code", request_code);
        networkCloudTypeSelectDialog.setArguments(bundle);
        return networkCloudTypeSelectDialog;
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
        FrameLayout progress_bar = v.findViewById(R.id.fragment_network_cloud_type_select_progressbar);
        progress_bar.setVisibility(View.GONE);
        RecyclerView recyclerview = v.findViewById(R.id.fragment_network_cloud_type_recyclerView);
        recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        recyclerview.setLayoutManager(new LinearLayoutManager(context));
        recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));

        if(what_type_network_cloud.equals(NETWORK)){
            int[] network_icon_image_array = {R.drawable.network_icon, R.drawable.network_icon, R.drawable.network_icon, R.drawable.network_icon};
            List<String> network_types = Arrays.asList(getResources().getStringArray(R.array.network_types));
            recyclerview.setAdapter(new NetworkCloudRecyclerAdapter(network_types, network_icon_image_array));
        } else if(what_type_network_cloud.equals(CLOUD)){
            List<String> cloud_types = Arrays.asList(getResources().getStringArray(R.array.cloud_types));
            int[] cloud_icon_image_array = {R.drawable.cloud_icon, R.drawable.cloud_icon, R.drawable.cloud_icon};
            recyclerview.setAdapter(new NetworkCloudRecyclerAdapter(cloud_types, cloud_icon_image_array));
        }

        ViewGroup button_layout = v.findViewById(R.id.fragment_network_cloud_type_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button cancel = button_layout.findViewById(R.id.first_button);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                dismissAllowingStateLoss();
            }
        });

        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, GridLayout.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private class NetworkCloudRecyclerAdapter extends RecyclerView.Adapter<NetworkCloudRecyclerAdapter.ViewHolder> {
        final List<String> arraylist;
        final int[] icon_image_list;

        NetworkCloudRecyclerAdapter(List<String> arraylist, int[] icon_image_list) {
            this.arraylist = arraylist;
            this.icon_image_list = icon_image_list;
        }

        @Override
        public NetworkCloudRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout, p1, false);
            return new NetworkCloudRecyclerAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(NetworkCloudRecyclerAdapter.ViewHolder p1, int p2) {
            p1.textView_network_cloud.setText(arraylist.get(p2));
            p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, icon_image_list[p2]));
        }

        @Override
        public int getItemCount() {
            return arraylist.size();
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
                final int[] position = new int[1];
                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        position[0] = getBindingAdapterPosition();
                        String type=textView_network_cloud.getText().toString().toLowerCase(Locale.ROOT);
                        Bundle bundle=new Bundle();
                        bundle.putString("type",type);
                        getParentFragmentManager().setFragmentResult(request_code,bundle);
                        dismissAllowingStateLoss();
                    }
                });
            }
        }
    }
}
