package svl.kadatha.filex.cloud;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.BaseActivity;
import svl.kadatha.filex.EquallyDistributedButtonsWithTextLayout;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.PermissionsUtil;
import svl.kadatha.filex.R;
import svl.kadatha.filex.network.DeleteNetworkAccountAlertDialog;
import svl.kadatha.filex.network.NetworkCloudTypeSelectDialog;

public class CloudAuthActivity extends BaseActivity {
    private static final String CLOUD_ACCOUNT_DELETE_REQUEST_CODE = "cloud_account_delete_request_code";
    private static final String CLOUD_ACCOUNT_TYPE_REQUEST_CODE = "cloud_account_type_request_code";
    private final List<CloudAccountPOJO> cloudAccountPOJO_selected_for_delete = new ArrayList<>();
    public boolean clear_cache;
    public CloudAuthActivityViewModel viewModel;
    private Context context;
    private PermissionsUtil permissionsUtil;
    private Toolbar bottom_toolbar;
    private RecyclerView cloud_account_list_recyclerview;
    private FrameLayout progress_bar;
    private TextView cloud_number_text_view, empty_cloud_account_list_tv;
    private Button delete_btn, disconnect_btn, edit_btn;
    private boolean toolbar_visible = true;
    private int scroll_distance;
    private int num_all_network_account = 0;
    private CloudAccountPojoListAdapter cloudAccountPojoListAdapter;
    private FileObjectType fileObjectType;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CloudAuthProvider provider = viewModel != null ? viewModel.getAuthProvider() : null;
        if (provider != null) {
            viewModel.oauthResultProcessingStatus.setValue(AsyncTaskStatus.STARTED);
            provider.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setContentView(R.layout.activity_cloud_auth);

        permissionsUtil = new PermissionsUtil(context, (AppCompatActivity) context);
        viewModel = new ViewModelProvider(this).get(CloudAuthActivityViewModel.class);

        // VM needs Activity to create providers (constructors require Activity)
        viewModel.attachProviderFactory(this::createProvider);

        setupToolbar();
        setupViews();
        setupRecycler();
        setupObservers();
        setupFragmentResultListeners();

        // OAuth return intent (if any)
        on_intent(getIntent());

        // Load all accounts
        viewModel.fetchCloudAccountPojoList();

        FloatingActionButton floatingActionButton = findViewById(R.id.floating_action_cloud_activity);
        floatingActionButton.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!viewModel.mselecteditems.isEmpty()) {
                    clear_selection();
                } else {
                    Intent data = new Intent();
                    data.putExtra("POP_TOP_FRAGMENT", viewModel.pop_up_top_fragment);
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });
    }

    private void setupToolbar() {
        EquallyDistributedButtonsWithTextLayout tb_layout =
                new EquallyDistributedButtonsWithTextLayout(context, 4, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);

        int[] bottom_drawables = {
                R.drawable.document_add_icon,
                R.drawable.delete_icon,
                R.drawable.connect_icon,
                R.drawable.edit_icon
        };
        String[] titles = new String[]{
                getString(R.string.new_),
                getString(R.string.delete),
                getString(R.string.disconnect),
                getString(R.string.edit)
        };
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);

        bottom_toolbar = findViewById(R.id.activity_cloud_bottom_toolbar);
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

        edit_btn.setVisibility(View.GONE);
        tb_layout.requestLayout();
        tb_layout.invalidate();
    }

    private void setupViews() {
        progress_bar = findViewById(R.id.activity_cloud_list_progressbar);
        cloud_number_text_view = findViewById(R.id.activity_cloud_details_number);
        empty_cloud_account_list_tv = findViewById(R.id.activity_cloud_details_empty);
        cloud_account_list_recyclerview = findViewById(R.id.activity_cloud_account_recyclerview);

        ((TextView) findViewById(R.id.activity_cloud_list_heading)).setText(R.string.cloud_accounts);

        enable_disable_buttons(false, 0, false);
        cloud_number_text_view.setText("0/0");
    }

    private void setupRecycler() {
        cloud_account_list_recyclerview.setLayoutManager(new LinearLayoutManager(context));
        cloud_account_list_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);

        cloud_account_list_recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            final int threshold = 5;

            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                if (scroll_distance > threshold && toolbar_visible) {
                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight())
                            .setInterpolator(new AccelerateInterpolator(1));
                    toolbar_visible = false;
                    scroll_distance = 0;
                } else if (scroll_distance < -threshold && !toolbar_visible) {
                    bottom_toolbar.animate().translationY(0)
                            .setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                }

                if ((toolbar_visible && dy > 0) || (!toolbar_visible && dy < 0)) {
                    scroll_distance += dy;
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.asyncTaskStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.STARTED) {
                progress_bar.setVisibility(View.VISIBLE);
            } else if (status == AsyncTaskStatus.COMPLETED) {
                progress_bar.setVisibility(View.GONE);
                cloudAccountPojoListAdapter = new CloudAccountPojoListAdapter();
                cloud_account_list_recyclerview.setAdapter(cloudAccountPojoListAdapter);
                num_all_network_account = (viewModel.cloudAccountPOJOList == null) ? 0 : viewModel.cloudAccountPOJOList.size();

                if (num_all_network_account == 0) {
                    cloud_account_list_recyclerview.setVisibility(View.GONE);
                    empty_cloud_account_list_tv.setVisibility(View.VISIBLE);
                } else {
                    cloud_account_list_recyclerview.setVisibility(View.VISIBLE);
                    empty_cloud_account_list_tv.setVisibility(View.GONE);
                }

                refreshToolbarFromSelection();
                cloud_number_text_view.setText(viewModel.mselecteditems.size() + "/" + num_all_network_account);
            }
        });

        viewModel.cloudAccountConnectionAsyncTaskStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.COMPLETED) {
                if (viewModel.connected) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("fileObjectType", fileObjectType);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_CONNECTED_TO_CLOUD_ACTION, LocalBroadcastManager.getInstance(context), bundle);
                    viewModel.pop_up_top_fragment=false;
                    viewModel.cloudAccountConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    finish();
                } else {
                    progress_bar.setVisibility(View.GONE);
                    viewModel.cloudAccountConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.cloudAccountStorageDirFillAsyncTaskStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.STARTED) {
                progress_bar.setVisibility(View.VISIBLE);
                return;
            }
            if (status == AsyncTaskStatus.COMPLETED) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("fileObjectType", fileObjectType);
                Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_CONNECTED_TO_CLOUD_ACTION, LocalBroadcastManager.getInstance(context), bundle);
                viewModel.pop_up_top_fragment=false;
                viewModel.cloudAccountStorageDirFillAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                finish();
            }
        });

        viewModel.deleteAsyncTaskStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.STARTED) {
                progress_bar.setVisibility(View.VISIBLE);
            } else if (status == AsyncTaskStatus.COMPLETED) {
                progress_bar.setVisibility(View.GONE);

                num_all_network_account = (viewModel.cloudAccountPOJOList == null) ? 0 : viewModel.cloudAccountPOJOList.size();
                if (num_all_network_account == 0) {
                    cloud_account_list_recyclerview.setVisibility(View.GONE);
                    empty_cloud_account_list_tv.setVisibility(View.VISIBLE);
                } else {
                    cloud_account_list_recyclerview.setVisibility(View.VISIBLE);
                    empty_cloud_account_list_tv.setVisibility(View.GONE);
                }

                clear_selection();
                viewModel.deleteAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            }
        });

        viewModel.disconnectCloudConnectionAsyncTaskStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.STARTED) {
                progress_bar.setVisibility(View.VISIBLE);
                return;
            }
            if (status == AsyncTaskStatus.COMPLETED) {
                clear_selection();
                if (cloudAccountPojoListAdapter != null){
                    cloudAccountPojoListAdapter.notifyDataSetChanged();
                }
                viewModel.pop_up_top_fragment=true;
                progress_bar.setVisibility(View.GONE);
                viewModel.disconnectCloudConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            }
        });

        viewModel.rowDisconnectAsyncTaskStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.STARTED) {
                progress_bar.setVisibility(View.VISIBLE);
                return;
            }
            if (status == AsyncTaskStatus.COMPLETED) {
                CloudAuthActivityViewModel.PendingConnect pc = viewModel.consumeRowPendingConnect();
                if (pc != null) {
                    startConnectFlow(pc.type, pc.account);
                    return;
                }
                progress_bar.setVisibility(View.GONE);
                viewModel.rowDisconnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            }
        });

        viewModel.logoutWhileAuthenticateAsyncTaskStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.COMPLETED) {
                if (cloudAccountPojoListAdapter != null) {
                    cloudAccountPojoListAdapter.notifyDataSetChanged();
                }
                viewModel.pop_up_top_fragment=true;
                viewModel.logoutWhileAuthenticateAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            }
        });

        viewModel.oauthResultProcessingStatus.observe(this, status -> {
            if (status == AsyncTaskStatus.STARTED) {
                progress_bar.setVisibility(View.VISIBLE);
            } else if (status == AsyncTaskStatus.COMPLETED) {
                progress_bar.setVisibility(View.GONE);
                viewModel.oauthResultProcessingStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            }
        });
    }

    private void setupFragmentResultListeners() {
        getSupportFragmentManager().setFragmentResultListener(CLOUD_ACCOUNT_DELETE_REQUEST_CODE, this,
                (requestKey, result) -> {
                    if (!CLOUD_ACCOUNT_DELETE_REQUEST_CODE.equals(requestKey)) return;

                    progress_bar.setVisibility(View.VISIBLE);
                    cloudAccountPOJO_selected_for_delete.clear();
                    cloudAccountPOJO_selected_for_delete.addAll(viewModel.mselecteditems.values());
                    viewModel.deleteCloudAccountPojo(cloudAccountPOJO_selected_for_delete);
                });

        getSupportFragmentManager().setFragmentResultListener(CLOUD_ACCOUNT_TYPE_REQUEST_CODE, this,
                (requestKey, result) -> {
                    if (!CLOUD_ACCOUNT_TYPE_REQUEST_CODE.equals(requestKey)) return;

                    fileObjectType = (FileObjectType) result.getSerializable("fileObjectType");
                    if (fileObjectType != null) {
                        authenticate(fileObjectType);
                    }
                });
    }

    private void on_intent(Intent intent) {
        if (intent == null) return;

        FileObjectType t = (FileObjectType) intent.getSerializableExtra("fileObjectType");
        if (t == null) return;

        // Make sure VM knows current type/provider context before provider handles response
        fileObjectType = t;

        CloudAuthProvider provider = createProvider(t);
        viewModel.setAuthProvider(provider);
        viewModel.fileObjectType = t;

        provider.handleAuthorizationResponse(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        on_intent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations() && clear_cache) {
            clearCache();
        }
    }

    public void clearCache() {
        Global.CLEAR_CACHE();
    }

    private void authenticate(@NonNull FileObjectType type) {
        CloudAuthProvider provider = createProvider(type);
        fileObjectType = type;

        viewModel.setAuthProvider(provider);
        viewModel.fileObjectType = type;

        viewModel.authenticate();
    }

    private CloudAuthProvider createProvider(@NonNull FileObjectType type) {
        switch (type) {
            case GOOGLE_DRIVE_TYPE:
                return new GoogleDriveAuthProvider(this);
            case DROP_BOX_TYPE:
                return new DropboxAuthProvider(this);
            case YANDEX_TYPE:
                return new YandexAuthProvider(this);
            case MEDIA_FIRE_TYPE:
                return new MediaFireAuthProvider(this);
            default:
                throw new IllegalStateException("Unsupported provider: " + type);
        }
    }

    private FileObjectType typeFromAccount(@NonNull CloudAccountPOJO a) {
        return FileObjectType.valueOf(a.type); // DB must store enum name
    }

    private void handleAccountClick(@NonNull CloudAccountPOJO account) {
        if (!permissionsUtil.isNetworkConnected()) {
            Global.print(context, getString(R.string.not_connected_to_network));
            return;
        }

        FileObjectType clickedType = typeFromAccount(account);
        fileObjectType = clickedType;

        // If already active for this type, do disconnect -> connect flow
        CloudAccountPOJO active = CloudAuthActivityViewModel.getActiveForType(clickedType);
        if (active != null) {
            viewModel.disconnectThenConnectRowClick(clickedType, account, createProvider(clickedType));
            return;
        }
        startConnectFlow(clickedType, account);
    }

    private void startConnectFlow(@NonNull FileObjectType type, @NonNull CloudAccountPOJO account) {
        fileObjectType = type;

        CloudAuthProvider provider = createProvider(type);
        viewModel.setAuthProvider(provider);
        viewModel.fileObjectType = type;
        viewModel.setCloudAccount(account);

        // 1) token ok -> populate
        if (provider.isAccessTokenValid(account)) {
            progress_bar.setVisibility(View.VISIBLE);
            CloudAuthActivityViewModel.setActive(type, account);
            viewModel.populateStorageDir(type, account);
            return;
        }

        // 2) refresh if supported
        if (provider.supportsRefresh() && account.refreshToken != null && !account.refreshToken.isEmpty()) {
            provider.refreshToken(account, new CloudAuthProvider.AuthCallback() {
                @Override
                public void onSuccess(CloudAccountPOJO updated) {
                    progress_bar.setVisibility(View.VISIBLE);
                    viewModel.saveAccount(updated);
                    viewModel.setCloudAccount(updated);
                    CloudAuthActivityViewModel.setActive(type, updated);
                    viewModel.populateStorageDir(type, updated);
                }

                @Override
                public void onError(Exception e) {
                    progress_bar.setVisibility(View.GONE);
                    viewModel.fileObjectType = type;
                    viewModel.setAuthProvider(provider);
                    viewModel.authenticate();
                }
            });
            return;
        }

        // 3) full auth
        progress_bar.setVisibility(View.GONE);
        viewModel.fileObjectType = type;
        viewModel.setAuthProvider(provider);
        viewModel.authenticate();
    }

    private boolean hasAnyConnectedSelection() {
        int s = viewModel.mselecteditems.size();
        if (s == 0) return false;

        for (int i = 0; i < s; i++) {
            CloudAccountPOJO pojo = viewModel.mselecteditems.getValueAtIndex(i);
            if (CloudAuthActivityViewModel.isPojoConnected(pojo)) {
                return true;
            }
        }
        return false;
    }

    private void refreshToolbarFromSelection() {
        int size = viewModel.mselecteditems.size();
        boolean hasConnected = (size > 0) && hasAnyConnectedSelection();

        enable_disable_buttons(size > 0, size, hasConnected);
        cloud_number_text_view.setText(size + "/" + num_all_network_account);
    }


    private void enable_disable_buttons(boolean enable, int selection_size, boolean enableDisconnect) {
        if (enable) {
            delete_btn.setAlpha(Global.ENABLE_ALFA);
            edit_btn.setAlpha(selection_size == 1 ? Global.ENABLE_ALFA : Global.DISABLE_ALFA);
        } else {
            delete_btn.setAlpha(Global.DISABLE_ALFA);
            edit_btn.setAlpha(Global.DISABLE_ALFA);
        }
        delete_btn.setEnabled(enable);
        edit_btn.setEnabled(enable && selection_size == 1);

        // disconnect only if at least one selected item is connected
        disconnect_btn.setEnabled(enable && enableDisconnect);
        disconnect_btn.setAlpha((enable && enableDisconnect) ? Global.ENABLE_ALFA : Global.DISABLE_ALFA);
    }

    public void clear_selection() {
        viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        if (cloudAccountPojoListAdapter != null) cloudAccountPojoListAdapter.notifyDataSetChanged();
        enable_disable_buttons(false, 0, false);
        cloud_number_text_view.setText("0/" + num_all_network_account);
    }

    private class CloudAccountPojoListAdapter extends RecyclerView.Adapter<CloudAccountPojoListAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.network_account_list_recyclerview_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CloudAccountPOJO cloudAccountPOJO = viewModel.cloudAccountPOJOList.get(position);

            String display = cloudAccountPOJO.displayName;
            String user_id = cloudAccountPOJO.userId;

            holder.cloud_account_display.setText((display == null || display.isEmpty()) ? user_id : display);
            String type_display = "";
            if ("GOOGLE_DRIVE_TYPE".equals(cloudAccountPOJO.type)) {
                type_display = "Google Drive";
                holder.cloud_imageView.setImageResource(R.drawable.google_drive_icon);
            } else if ("DROP_BOX_TYPE".equals(cloudAccountPOJO.type)) {
                type_display = "Dropbox";
                holder.cloud_imageView.setImageResource(R.drawable.dropbox_icon);
            } else if ("YANDEX_TYPE".equals(cloudAccountPOJO.type)) {
                type_display = "Yandex";
                holder.cloud_imageView.setImageResource(R.drawable.cloud_icon);
            } else if ("MEDIA_FIRE_TYPE".equals(cloudAccountPOJO.type)) {
                type_display = "MediaFire";
                holder.cloud_imageView.setImageResource(R.drawable.cloud_icon);
            }

            holder.host.setText(type_display);
            holder.cloud_account_user_id.setText(user_id);

            boolean item_selected = viewModel.mselecteditems.containsKey(position);
            holder.v.setSelected(item_selected);
            holder.cloud_account_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);

            boolean connected = CloudAuthActivityViewModel.isPojoConnected(cloudAccountPOJO);
            holder.green_dot.setVisibility(connected ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getItemCount() {
            return viewModel.cloudAccountPOJOList == null ? 0 : viewModel.cloudAccountPOJOList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final View v;
            final ImageView cloud_imageView, cloud_account_select_indicator;
            final TextView cloud_account_display, host;
            final TextView cloud_account_user_id;
            final ImageView green_dot;

            VH(View view) {
                super(view);
                v = view;
                cloud_imageView = v.findViewById(R.id.network_list_recyclerview_network_image);
                cloud_account_display = v.findViewById(R.id.network_list_recyclerview_display);
                cloud_account_user_id = v.findViewById(R.id.network_list_recyclerview_user_name);
                host = v.findViewById(R.id.network_list_recyclerview_host);
                cloud_account_select_indicator = v.findViewById(R.id.network_list_recyclerview_select_indicator);
                green_dot = v.findViewById(R.id.network_list_recyclerview_connected_indicator);

                v.setOnClickListener(p -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    int size = viewModel.mselecteditems.size();
                    if (size > 0) {
                        onLongClickProcedure(v);
                    } else {
                        CloudAccountPOJO cloudAccountPOJO = viewModel.cloudAccountPOJOList.get(pos);
                        handleAccountClick(cloudAccountPOJO);
                    }
                });

                v.setOnLongClickListener(p -> {
                    onLongClickProcedure(v);
                    return true;
                });
            }

            private void onLongClickProcedure(View row) {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                int size = viewModel.mselecteditems.size();

                if (viewModel.mselecteditems.containsKey(pos)) {
                    row.setSelected(false);
                    cloud_account_select_indicator.setVisibility(View.INVISIBLE);
                    viewModel.mselecteditems.remove(pos);
                    size--;
                } else {
                    row.setSelected(true);
                    cloud_account_select_indicator.setVisibility(View.VISIBLE);
                    viewModel.mselecteditems.put(pos, viewModel.cloudAccountPOJOList.get(pos));
                    size++;
                }

                bottom_toolbar.setVisibility(View.VISIBLE);
                bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                toolbar_visible = true;
                scroll_distance = 0;

                enable_disable_buttons(size > 0, size, hasAnyConnectedSelection());
                cloud_number_text_view.setText(size + "/" + num_all_network_account);
            }
        }
    }

    private class BottomToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.toolbar_btn_1) {
                clear_selection();
                NetworkCloudTypeSelectDialog dialog =
                        NetworkCloudTypeSelectDialog.getInstance(NetworkCloudTypeSelectDialog.CLOUD, CLOUD_ACCOUNT_TYPE_REQUEST_CODE, null);
                dialog.show(getSupportFragmentManager(), "");

            } else if (id == R.id.toolbar_btn_2) {
                int s = viewModel.mselecteditems.size();
                if (s > 0) {
                    CloudAccountPOJO pojo = viewModel.mselecteditems.getValueAtIndex(0);
                    String display = pojo.displayName;
                    DeleteNetworkAccountAlertDialog alert =
                            DeleteNetworkAccountAlertDialog.getInstance(
                                    CLOUD_ACCOUNT_DELETE_REQUEST_CODE,
                                    (display == null || display.isEmpty()) ? pojo.userId : display,
                                    s
                            );
                    alert.show(getSupportFragmentManager(), "");
                }

            } else if (id == R.id.toolbar_btn_3) {
                int s = viewModel.mselecteditems.size();
                if (s == 0) return;

                progress_bar.setVisibility(View.VISIBLE);
                viewModel.disconnectSelectedConnectedAccountsFromToolbar();
            } else if (id == R.id.toolbar_btn_4) {
                clear_selection();
            }
        }
    }
}
