package svl.kadatha.filex.network;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.EquallyDistributedButtonsWithTextLayout;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FileSelectorActivity;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.MainActivity;
import svl.kadatha.filex.NetworkCloudTypeSelectDialog;
import svl.kadatha.filex.PermissionsUtil;
import svl.kadatha.filex.R;

public class NetworkAccountsDetailsDialog extends DialogFragment {

    public final static String FTP = "ftp";
    public final static String SFTP = "sftp";
    public final static String WebDAV = "webdav";
    public final static String SMB = "smb";
    public final static String NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE = "network_account_input_details_request_code";
    public final static String ALL = "all";
    private final static String NETWORK_ACCOUNT_DELETE_REQUEST_CODE = "network_account_delete_request_code";
    private final static String NETWORK_ACCOUNT_RENAME_REQUEST_CODE = "network_account_rename_request_code";
    private final static String NETWORK_ACCOUNT_TYPE_REQUEST_CODE = "network_account_type_request_code";
    private Context context;
    private String type;
    private Toolbar bottom_toolbar;
    private RecyclerView network_account_list_recyclerview;
    private NetworkAccountPojoListAdapter networkAccountPojoListAdapter;
    private List<NetworkAccountPOJO> networkAccountPOJO_selected_for_delete = new ArrayList<>();
    private boolean toolbar_visible = true;
    private int scroll_distance;
    private int num_all_network_account;
    private Button delete_btn, disconnect_btn;
    private Button edit_btn;
    private PermissionsUtil permissionsUtil;
    private FrameLayout progress_bar;
    private TextView network_number_text_view, empty_network_account_list_tv;
    private NetworkAccountDetailsViewModel viewModel;
    private NetworkAccountPOJO connected_network_account_pojo = null;

    public static NetworkAccountsDetailsDialog getInstance(String type) {
        Bundle bundle = new Bundle();
        bundle.putString("type", type);
        NetworkAccountsDetailsDialog d = new NetworkAccountsDetailsDialog();
        d.setArguments(bundle);
        return d;
    }

    // New helper for all
    public static NetworkAccountsDetailsDialog getAllInstance() {
        return getInstance(ALL);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        permissionsUtil = new PermissionsUtil(context, (AppCompatActivity) context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        if (getArguments() != null) {
            type = getArguments().getString("type");
        }
        if (type == null) type = ALL;

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_network_account_list, container, false);
        progress_bar = v.findViewById(R.id.fragment_network_list_progressbar);
        TextView heading = v.findViewById(R.id.fragment_network_list_heading);
        if (type.equals(ALL)) {
            heading.setText(R.string.network_accounts); // add string, or use a fallback
        } else if (type.equals(NetworkAccountsDetailsDialog.FTP)) {
            heading.setText(R.string.ftp);
        } else if (type.equals(NetworkAccountsDetailsDialog.SFTP)) {
            heading.setText(R.string.sftp);
        } else if (type.equals(NetworkAccountsDetailsDialog.WebDAV)) {
            heading.setText(R.string.webdav);
        } else if (type.equals(NetworkAccountsDetailsDialog.SMB)) {
            heading.setText(R.string.smb);
        }
        network_number_text_view = v.findViewById(R.id.network_details_network_number);
        empty_network_account_list_tv = v.findViewById(R.id.network_details_empty);
        network_account_list_recyclerview = v.findViewById(R.id.fragment_network_account_recyclerview);
        network_account_list_recyclerview.setLayoutManager(new LinearLayoutManager(context));
        network_account_list_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        network_account_list_recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            final int threshold = 5;

            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (scroll_distance > threshold && toolbar_visible) {
                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    toolbar_visible = false;
                    scroll_distance = 0;
                } else if (scroll_distance < -threshold && !toolbar_visible) {
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                }

                if ((toolbar_visible && dy > 0) || (!toolbar_visible && dy < 0)) {
                    scroll_distance += dy;
                }
            }
        });

        Button scan_btn = v.findViewById(R.id.fragment_network_scan_btn);
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        FloatingActionButton floatingActionButton = v.findViewById(R.id.floating_action_network_list);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!viewModel.mselecteditems.isEmpty()) {
                    clear_selection();
                } else {
                    dismissAllowingStateLoss();
                }
            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(context, 4, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.document_add_icon, R.drawable.delete_icon, R.drawable.connect_icon, R.drawable.edit_icon};
        String[] titles = new String[]{getString(R.string.new_), getString(R.string.delete), getString(R.string.disconnect), getString(R.string.edit)};
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);
        bottom_toolbar = v.findViewById(R.id.fragment_network_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button add_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        delete_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        disconnect_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        edit_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_4);

        BottomToolbarClickListener bottomToolbarClickListener = new BottomToolbarClickListener();
        add_btn.setOnClickListener(bottomToolbarClickListener);
        delete_btn.setOnClickListener(bottomToolbarClickListener);
        disconnect_btn.setOnClickListener(bottomToolbarClickListener);
        edit_btn.setOnClickListener(bottomToolbarClickListener);

        viewModel = new ViewModelProvider(this).get(NetworkAccountDetailsViewModel.class);
        viewModel.type = type;
        viewModel.fetchNetworkAccountPojoList(type);

        if (!type.equals(ALL)) {
            if (type.equals(FTP)) {
                connected_network_account_pojo = NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO;
            } else if (type.equals(SFTP)) {
                connected_network_account_pojo = NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO;
            } else if (type.equals(WebDAV)) {
                connected_network_account_pojo = NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO;
            } else if (type.equals(SMB)) {
                connected_network_account_pojo = NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO;
            }

            if (connected_network_account_pojo != null) {
                progress_bar.setVisibility(View.VISIBLE);
                viewModel.testServiceConnection();
            } else {
                disconnect_btn.setAlpha(Global.DISABLE_ALFA);
                disconnect_btn.setEnabled(false);
            }
        } else {
            // In ALL mode, disconnect is driven by selection, not dialog-level type
            disconnect_btn.setAlpha(Global.DISABLE_ALFA);
            disconnect_btn.setEnabled(false);
        }


        int size = viewModel.mselecteditems.size();
        network_number_text_view.setText(size + "/" + num_all_network_account);
        enable_disable_buttons(size != 0, size);

        viewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    networkAccountPojoListAdapter = new NetworkAccountPojoListAdapter();
                    network_account_list_recyclerview.setAdapter(networkAccountPojoListAdapter);
                    num_all_network_account = viewModel.networkAccountPOJOList.size();
                    if (num_all_network_account == 0) {
                        network_account_list_recyclerview.setVisibility(View.GONE);
                        empty_network_account_list_tv.setVisibility(View.VISIBLE);
                    }
                    network_number_text_view.setText(viewModel.mselecteditems.size() + "/" + num_all_network_account);
                    updateDisconnectButtonState();
                }
            }
        });

        viewModel.deleteAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    num_all_network_account = viewModel.networkAccountPOJOList.size();
                    if (num_all_network_account == 0) {
                        network_account_list_recyclerview.setVisibility(View.GONE);
                        empty_network_account_list_tv.setVisibility(View.VISIBLE);
                    } else {
                        network_account_list_recyclerview.setVisibility(View.VISIBLE);
                        empty_network_account_list_tv.setVisibility(View.GONE);
                    }
                    clear_selection();
                    viewModel.deleteAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.networkConnectAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (viewModel.loggedInStatus) {
                        viewModel.networkConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                        String t = type.equals(ALL)
                                ? (viewModel.networkAccountPOJO != null ? viewModel.networkAccountPOJO.type : null)
                                : type;

                        if (t == null) return;
                        if (t.equals(FTP) && (NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        } else if (t.equals(SFTP) && (NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        } else if (t.equals(WebDAV) && (NetworkAccountDetailsViewModel.WEBDAV_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.WEBDAV_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        } else if (t.equals(SMB) && (NetworkAccountDetailsViewModel.SMB_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.SMB_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        }

                        initiateCreateFragmentTransactions();
                        dismissAllowingStateLoss();
                    } else {
                        viewModel.networkConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    }
                }
            }
        });

        viewModel.replaceAndConnectNetworkAccountAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (viewModel.loggedInStatus) {
                        viewModel.networkConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                        String t = type.equals(ALL)
                                ? (viewModel.networkAccountPOJO != null ? viewModel.networkAccountPOJO.type : null)
                                : type;

                        if (t == null) return;
                        if (t.equals(FTP) && (NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        } else if (t.equals(SFTP) && (NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        } else if (t.equals(WebDAV) && (NetworkAccountDetailsViewModel.WEBDAV_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.WEBDAV_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        } else if (t.equals(SMB) && (NetworkAccountDetailsViewModel.SMB_WORKING_DIR_PATH == null || NetworkAccountDetailsViewModel.SMB_WORKING_DIR_PATH.isEmpty())) {
                            Global.print(context, getString(R.string.server_could_not_be_connected));
                            return;
                        }
                        initiateCreateFragmentTransactions();
                        dismissAllowingStateLoss();
                    } else {
                        viewModel.replaceAndConnectNetworkAccountAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    }
                }
            }
        });

        viewModel.replaceNetworkAccountAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    num_all_network_account = viewModel.networkAccountPOJOList.size();
                    if (num_all_network_account == 0) {
                        network_account_list_recyclerview.setVisibility(View.GONE);
                        empty_network_account_list_tv.setVisibility(View.VISIBLE);
                    } else {
                        network_account_list_recyclerview.setVisibility(View.VISIBLE);
                        empty_network_account_list_tv.setVisibility(View.GONE);
                    }
                    clear_selection();
                    viewModel.replaceNetworkAccountAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.changeNetworkAccountDisplayAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    clear_selection();
                    viewModel.changeNetworkAccountDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.testServiceConnectionAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!viewModel.isNetworkConnected) {
                        disconnect_btn.setAlpha(Global.DISABLE_ALFA);
                        disconnect_btn.setEnabled(false);
                        connected_network_account_pojo = null;
                        clear_selection();
                    }
                    viewModel.testServiceConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    updateDisconnectButtonState();
                }
            }
        });

        viewModel.disconnectNetworkConnectionAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!type.equals(ALL)) {
                        connected_network_account_pojo = null;
                    }
                    clear_selection();
                    Global.print(context, getString(R.string.network_connection_disconnected));
                    updateDisconnectButtonState();
                    viewModel.disconnectNetworkConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(NETWORK_ACCOUNT_DELETE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(NETWORK_ACCOUNT_DELETE_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    networkAccountPOJO_selected_for_delete = new ArrayList<>();
                    networkAccountPOJO_selected_for_delete.addAll(viewModel.mselecteditems.values());
                    viewModel.deleteNetworkAccountPojo(networkAccountPOJO_selected_for_delete);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    if (result.getBoolean("whetherToConnect")) {
                        viewModel.replaceAndConnectNetworkAccount(result);
                    } else {
                        viewModel.replaceNetworkAccountPojoList(result);
                    }
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(NETWORK_ACCOUNT_RENAME_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(NETWORK_ACCOUNT_RENAME_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    String new_name = result.getString("new_name");
                    String host = result.getString("host");
                    int port = result.getInt("port");
                    String user_name = result.getString("user_name");
                    String rowType = result.getString("type");
                    if (rowType == null || rowType.isEmpty()) {
                        rowType = type;
                    }
                    if (new_name != null) {
                        viewModel.changeNetworkAccountPojoDisplay(host, port, user_name, new_name, rowType);
                    }
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(NETWORK_ACCOUNT_TYPE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(NETWORK_ACCOUNT_TYPE_REQUEST_CODE)) {
                    String type = result.getString("type");
                    NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = NetworkAccountDetailsInputDialog.getInstance(NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE, type, null);
                    networkAccountDetailsInputDialog.show(getParentFragmentManager(), "");
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
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_WIDTH);
        } else {
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_HEIGHT);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void initiateCreateFragmentTransactions() {
        String t = type.equals(ALL)
                ? (viewModel.networkAccountPOJO != null ? viewModel.networkAccountPOJO.type : null)
                : type;

        if (t == null) return;
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        if (appCompatActivity instanceof MainActivity) {
            ((MainActivity) context).storageRecyclerAdapter.notifyDataSetChanged();
            if (((MainActivity) context).recentDialogListener != null) {
                ((MainActivity) context).recentDialogListener.onMediaAttachedAndRemoved();
            }
            if (t.equals(FTP)) {
                ((MainActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH, FileObjectType.FTP_TYPE);
            } else if (t.equals(SFTP)) {
                ((MainActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH, FileObjectType.SFTP_TYPE);
            } else if (t.equals(WebDAV)) {
                ((MainActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.WEBDAV_WORKING_DIR_PATH, FileObjectType.WEBDAV_TYPE);
            } else if (t.equals(SMB)) {
                ((MainActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.SMB_WORKING_DIR_PATH, FileObjectType.SMB_TYPE);
            }
        } else if (appCompatActivity instanceof FileSelectorActivity) {
            if (((FileSelectorActivity) context).recentDialogListener != null) {
                ((FileSelectorActivity) context).recentDialogListener.onMediaAttachedAndRemoved();
            }
            if (t.equals(FTP)) {
                ((FileSelectorActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH, FileObjectType.FTP_TYPE);
            } else if (t.equals(SFTP)) {
                ((FileSelectorActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH, FileObjectType.SFTP_TYPE);
            } else if (t.equals(WebDAV)) {
                ((FileSelectorActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.WEBDAV_WORKING_DIR_PATH, FileObjectType.WEBDAV_TYPE);
            } else if (t.equals(SMB)) {
                ((FileSelectorActivity) context).createFragmentTransaction(NetworkAccountDetailsViewModel.SMB_WORKING_DIR_PATH, FileObjectType.SMB_TYPE);
            }
        }
    }

    public void clear_selection() {
        viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        if (networkAccountPojoListAdapter != null) {
            networkAccountPojoListAdapter.notifyDataSetChanged();
        }

        enable_disable_buttons(false, 0);
        network_number_text_view.setText(viewModel.mselecteditems.size() + "/" + num_all_network_account);
        updateDisconnectButtonState();
    }

    private void enable_disable_buttons(boolean enable, int selection_size) {
        if (enable) {
            delete_btn.setAlpha(Global.ENABLE_ALFA);
            if (selection_size == 1) {
                edit_btn.setAlpha(Global.ENABLE_ALFA);
            } else {
                edit_btn.setAlpha(Global.DISABLE_ALFA);
            }
        } else {
            delete_btn.setAlpha(Global.DISABLE_ALFA);
            edit_btn.setAlpha(Global.DISABLE_ALFA);
        }
        delete_btn.setEnabled(enable);
        edit_btn.setEnabled(enable && selection_size == 1);
    }

    private void updateDisconnectButtonState() {
        boolean enable = false;

        if (viewModel != null && viewModel.mselecteditems != null && !viewModel.mselecteditems.isEmpty()) {
            for (int i = 0; i < viewModel.mselecteditems.size(); i++) {
                NetworkAccountPOJO pojo = viewModel.mselecteditems.getValueAtIndex(i);
                if (NetworkAccountDetailsViewModel.isPojoConnected(pojo)) {
                    enable = true;
                    break;
                }
            }
        } else if (!type.equals(ALL)) {
            // single-type dialog behavior: enable if there is any active connection for that type
            enable = (connected_network_account_pojo != null);
        }

        disconnect_btn.setEnabled(enable);
        disconnect_btn.setAlpha(enable ? Global.ENABLE_ALFA : Global.DISABLE_ALFA);
    }

    private class NetworkAccountPojoListAdapter extends RecyclerView.Adapter<NetworkAccountPojoListAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.network_account_list_recyclerview_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NetworkAccountPOJO networkAccountPOJO = viewModel.networkAccountPOJOList.get(position);
            String display = networkAccountPOJO.display;
            String host = networkAccountPOJO.host;
            String user_name = networkAccountPOJO.user_name;
            String port = String.valueOf(networkAccountPOJO.port);
            holder.network_account_display.setText((display == null || display.isEmpty()) ? host : display);
            holder.network_account_host.setText(host + ":" + port);
            holder.network_account_user_name.setText(user_name);
            boolean item_selected = viewModel.mselecteditems.containsKey(position);
            holder.v.setSelected(item_selected);
            holder.network_account_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
            boolean connected = NetworkAccountDetailsViewModel.isPojoConnected(networkAccountPOJO);
            holder.green_dot.setVisibility(connected ? View.VISIBLE : View.INVISIBLE);

// Optional: show type in host line without changing layout
            holder.network_account_host.setText(networkAccountPOJO.type + " • " + host + ":" + port);

        }

        @Override
        public int getItemCount() {
            return viewModel.networkAccountPOJOList.size();
        }

        private class VH extends RecyclerView.ViewHolder {
            final View v;
            //final ImageView network_image;
            final ImageView network_account_select_indicator;
            final TextView network_account_display;
            final TextView network_account_host;
            final TextView network_account_user_name;
            final ImageView green_dot;
            int pos;

            VH(View view) {
                super(view);
                v = view;
                //network_image = v.findViewById(R.id.network_list_recyclerview_network_image);
                network_account_display = v.findViewById(R.id.network_list_recyclerview_display);
                network_account_host = v.findViewById(R.id.network_list_recyclerview_host);
                network_account_user_name = v.findViewById(R.id.network_list_recyclerview_user_name);
                network_account_select_indicator = v.findViewById(R.id.network_list_recyclerview_select_indicator);
                green_dot = v.findViewById(R.id.network_list_recyclerview_connected_indicator);

                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        pos = getBindingAdapterPosition();
                        int size = viewModel.mselecteditems.size();
                        if (size > 0) {
                            onLongClickProcedure(p, size);
                        } else {
                            if (!permissionsUtil.isNetworkConnected()) {
                                Global.print(context, getString(R.string.not_connected_to_network));
                                return;
                            }

                            progress_bar.setVisibility(View.VISIBLE);
                            NetworkAccountPOJO networkAccountPOJO = viewModel.networkAccountPOJOList.get(pos);
                            viewModel.connectNetworkAccount(networkAccountPOJO);
                        }
                    }
                });

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View p) {
                        onLongClickProcedure(p, viewModel.mselecteditems.size());
                        return true;
                    }
                });
            }

            private void onLongClickProcedure(View v, int size) {
                pos = getBindingAdapterPosition();
                if (viewModel.mselecteditems.containsKey(pos)) {
                    v.setSelected(false);
                    network_account_select_indicator.setVisibility(View.INVISIBLE);
                    viewModel.mselecteditems.remove(pos);
                    --size;
                    if (size >= 1) {
                        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                        toolbar_visible = true;
                        scroll_distance = 0;
                        enable_disable_buttons(true, size);
                    }

                    if (size == 0) {
                        enable_disable_buttons(false, size);
                    }
                } else {
                    v.setSelected(true);
                    network_account_select_indicator.setVisibility(View.VISIBLE);
                    viewModel.mselecteditems.put(pos, viewModel.networkAccountPOJOList.get(pos));

                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;

                    ++size;
                    enable_disable_buttons(true, size);
                }
                network_number_text_view.setText(size + "/" + num_all_network_account);
                updateDisconnectButtonState();
            }
        }
    }

    private class BottomToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.toolbar_btn_1) {
                clear_selection();
                if (type.equals(ALL)) {
                    NetworkCloudTypeSelectDialog networkCloudTypeSelectDialog = NetworkCloudTypeSelectDialog.getInstance(NetworkCloudTypeSelectDialog.NETWORK, NETWORK_ACCOUNT_TYPE_REQUEST_CODE,type);
                    networkCloudTypeSelectDialog.show(getParentFragmentManager(), "");
                } else {
                    NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = NetworkAccountDetailsInputDialog.getInstance(NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE, viewModel.type, null);
                    networkAccountDetailsInputDialog.show(getParentFragmentManager(), "");
                }
            } else if (id == R.id.toolbar_btn_2) {
                int s = viewModel.mselecteditems.size();
                if (s > 0) {
                    NetworkAccountPOJO networkAccountPOJO = viewModel.mselecteditems.getValueAtIndex(0);
                    String display = networkAccountPOJO.display;
                    DeleteNetworkAccountAlertDialog deleteNetworkAccountAlertDialog = DeleteNetworkAccountAlertDialog.getInstance(NETWORK_ACCOUNT_DELETE_REQUEST_CODE, (display == null || display.isEmpty()) ? networkAccountPOJO.host : display, s);
                    deleteNetworkAccountAlertDialog.show(getParentFragmentManager(), "");
                }
            } else if (id == R.id.toolbar_btn_3) {
                progress_bar.setVisibility(View.VISIBLE);
                if (type.equals(ALL)) {
                    // disconnect connected ones among selected rows
                    List<NetworkAccountPOJO> selected = new ArrayList<>();
                    selected.addAll(viewModel.mselecteditems.values());
                    viewModel.disconnectSelectedConnectedRows(selected);
                } else {
                    if (connected_network_account_pojo == null) {
                        progress_bar.setVisibility(View.GONE);
                        return;
                    }
                    viewModel.disconnectNetworkConnection();
                }

            } else if (id == R.id.toolbar_btn_4) {
                int s = viewModel.mselecteditems.size();
                if (s == 1) {
                    NetworkAccountPOJO selected = viewModel.networkAccountPOJOList.get(viewModel.mselecteditems.getKeyAtIndex(0));
                    String editType = selected.type;
                    NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = NetworkAccountDetailsInputDialog.getInstance(NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE, editType, selected);
                    networkAccountDetailsInputDialog.show(getParentFragmentManager(), "");

                }
                clear_selection();
            } else if (id == R.id.toolbar_btn_5) {
                dismissAllowingStateLoss();
            }
        }
    }
}