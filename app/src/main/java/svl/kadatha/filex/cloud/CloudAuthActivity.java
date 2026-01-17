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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
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
import svl.kadatha.filex.network.NetworkAccountsDetailsDialog;

public class CloudAuthActivity extends BaseActivity {

    private final List<CloudAccountPOJO> cloudAccountPOJO_selected_for_delete = new ArrayList<>();
    public boolean clear_cache;
    public CloudAuthActivityViewModel viewModel;
    public ActivityResultLauncher<Intent> mediaFireAuthLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (viewModel.authProvider instanceof MediaFireAuthProvider) {
                //((MediaFireAuthProvider) viewModel.authProvider).handleAuthResult();
            }
        }
    });
    private FileObjectType fileObjectType;
    private Toolbar bottom_toolbar;
    private RecyclerView cloud_account_list_recyclerview;
    private boolean toolbar_visible = true;
    private int scroll_distance;
    private int num_all_network_account;
    private Button delete_btn, disconnect_btn;
    private Button edit_btn;
    private PermissionsUtil permissionsUtil;
    private FrameLayout progress_bar;
    private TextView cloud_number_text_view, empty_cloud_account_list_tv;
    private Context context;
    private CloudAccountPOJO connected_cloud_account_pojo;
    private CloudAccountPojoListAdapter cloudAccountPojoListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_cloud_auth);
        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(context, 4, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.document_add_icon, R.drawable.delete_icon, R.drawable.connect_icon, R.drawable.edit_icon};
        String[] titles = new String[]{getString(R.string.new_), getString(R.string.delete), getString(R.string.disconnect), getString(R.string.edit)};
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
        progress_bar = findViewById(R.id.activity_cloud_list_progressbar);
        permissionsUtil = new PermissionsUtil(context, (AppCompatActivity) context);
        viewModel = new ViewModelProvider(this).get(CloudAuthActivityViewModel.class);
        Intent intent = getIntent();
        on_intent(intent, savedInstanceState);
        viewModel.fetchCloudAccountPojoList(fileObjectType.toString());

        viewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    cloudAccountPojoListAdapter = new CloudAccountPojoListAdapter();
                    cloud_account_list_recyclerview.setAdapter(cloudAccountPojoListAdapter);
                    num_all_network_account = viewModel.cloudAccountPOJOList.size();
                    if (num_all_network_account == 0) {
                        cloud_account_list_recyclerview.setVisibility(View.GONE);
                        empty_cloud_account_list_tv.setVisibility(View.VISIBLE);
                    }
                    cloud_number_text_view.setText(viewModel.mselecteditems.size() + "/" + num_all_network_account);
                    progress_bar.setVisibility(View.GONE);
                }
            }
        });

        viewModel.cloudAccountConnectionAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    if (viewModel.connected) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("fileObjectType", fileObjectType);
                        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_CONNECTED_TO_CLOUD_ACTION, LocalBroadcastManager.getInstance(context), bundle);
                        viewModel.cloudAccountConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                        finish();
                    } else {
                        progress_bar.setVisibility(View.GONE);
                    }
                }
            }
        });

        viewModel.cloudAccountStorageDirFillAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("fileObjectType", fileObjectType);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_CONNECTED_TO_CLOUD_ACTION, LocalBroadcastManager.getInstance(context), bundle);
                    viewModel.cloudAccountStorageDirFillAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    finish();
                }
            }
        });

        TextView heading = findViewById(R.id.activity_cloud_list_heading);
        if (fileObjectType.equals(FileObjectType.GOOGLE_DRIVE_TYPE)) {
            heading.setText(R.string.google_drive);
        } else if (fileObjectType.equals(FileObjectType.DROP_BOX_TYPE)) {
            heading.setText(R.string.drop_box);
        } else if (fileObjectType.equals(FileObjectType.YANDEX_TYPE)) {
            heading.setText(R.string.yandex);
        }
        cloud_number_text_view = findViewById(R.id.activity_cloud_details_number);
        empty_cloud_account_list_tv = findViewById(R.id.activity_cloud_details_empty);
        cloud_account_list_recyclerview = findViewById(R.id.activity_cloud_account_recyclerview);
        cloud_account_list_recyclerview.setLayoutManager(new LinearLayoutManager(context));
        cloud_account_list_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        cloud_account_list_recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        FloatingActionButton floatingActionButton = findViewById(R.id.floating_action_cloud_activity);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
    private void on_intent(Intent intent, Bundle savedInstanceState) {
        if (intent != null) {
            fileObjectType = (FileObjectType) intent.getSerializableExtra("fileObjectType");
            if (viewModel.authProvider != null) {
                viewModel.authProvider.handleAuthorizationResponse(intent);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        on_intent(intent, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (viewModel != null && viewModel.authProvider != null) {
            viewModel.authProvider.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache", clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache = savedInstanceState.getBoolean("clear_cache");
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

    private void authenticate() {
        switch (fileObjectType) {
            case GOOGLE_DRIVE_TYPE:
                viewModel.setAuthProvider(new GoogleDriveAuthProvider(this));
                break;
            case ONE_DRIVE_TYPE:
                break;
            case DROP_BOX_TYPE:
                viewModel.setAuthProvider(new DropboxAuthProvider(this));
                break;
            case MEDIA_FIRE_TYPE:
                viewModel.setAuthProvider(new MediaFireAuthProvider(this));
                break;
            case BOX_TYPE:
                break;
            case NEXT_CLOUD_TYPE:
                break;
            case YANDEX_TYPE:
                viewModel.setAuthProvider(new YandexAuthProvider(this));
                break;
        }
        viewModel.fileObjectType = fileObjectType;
        viewModel.authenticate();
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

    public void clear_selection() {
        viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        if (cloudAccountPojoListAdapter != null) {
            cloudAccountPojoListAdapter.notifyDataSetChanged();
        }

        enable_disable_buttons(false, 0);
        cloud_number_text_view.setText(viewModel.mselecteditems.size() + "/" + num_all_network_account);
    }

    private CloudAuthProvider createProvider(FileObjectType type) {
        switch (type) {
            case GOOGLE_DRIVE_TYPE:
                return new GoogleDriveAuthProvider(this);
            case DROP_BOX_TYPE:
                return new DropboxAuthProvider(this);
            case YANDEX_TYPE:
                return new YandexAuthProvider(this);
            // TODO others
            default:
                throw new IllegalStateException("Unsupported provider: " + type);
        }
    }

    private void handleAccountClick(@NonNull CloudAccountPOJO account) {

        if (!permissionsUtil.isNetworkConnected()) {
            Global.print(context, getString(R.string.not_connected_to_network));
            return;
        }

        progress_bar.setVisibility(View.VISIBLE);

        CloudAuthProvider provider; // optional if already set
        // Better: always create/set the right provider for this fileObjectType
        viewModel.setAuthProvider(createProvider(fileObjectType));
        provider = viewModel.authProvider;

        // 1) Token valid -> connect directly
        if (provider.isAccessTokenValid(account)) {
            viewModel.populateStorageDir(fileObjectType, account);
            return;
        }

        // 2) Try refresh (if supported + refreshToken exists)
        if (provider.supportsRefresh() && account.refreshToken != null && !account.refreshToken.isEmpty()) {
            provider.refreshToken(account, new CloudAuthProvider.AuthCallback() {
                @Override
                public void onSuccess(CloudAccountPOJO updated) {
                    viewModel.saveAccount(updated);
                    viewModel.populateStorageDir(fileObjectType, account);
                }

                @Override
                public void onError(Exception e) {
                    // refresh failed -> OAuth again (reuse your activity method)
                    authenticate(); // <-- your existing method
                }
            });

        } else {
            // no refresh path -> OAuth again
            authenticate(); // <-- your existing method
        }
    }

    private class CloudAccountPojoListAdapter extends RecyclerView.Adapter<CloudAccountPojoListAdapter.VH> {
        @NonNull
        @Override
        public CloudAccountPojoListAdapter.VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CloudAccountPojoListAdapter.VH(LayoutInflater.from(context).inflate(R.layout.network_account_list_recyclerview_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CloudAccountPojoListAdapter.VH holder, int position) {
            CloudAccountPOJO cloudAccountPOJO = viewModel.cloudAccountPOJOList.get(position);
            String display = cloudAccountPOJO.displayName;
            String user_id = cloudAccountPOJO.userId;
            holder.cloud_account_display.setText((display == null || display.isEmpty()) ? "" : display);
            holder.cloud_account_display.setText(user_id);
            boolean item_selected = viewModel.mselecteditems.containsKey(position);
            holder.v.setSelected(item_selected);
            holder.cloud_account_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
            if (connected_cloud_account_pojo != null) {
                holder.green_dot.setVisibility(cloudAccountPOJO.displayName.equals(connected_cloud_account_pojo.displayName) && cloudAccountPOJO.userId.equals(connected_cloud_account_pojo.userId) ? View.VISIBLE : View.INVISIBLE);
            } else {
                holder.green_dot.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return viewModel.cloudAccountPOJOList.size();
        }

        private class VH extends RecyclerView.ViewHolder {
            final View v;
            //final ImageView cloud_image;
            final ImageView cloud_account_select_indicator;
            final TextView cloud_account_display;
            final TextView cloud_account_user_id;
            final ImageView green_dot;
            int pos;

            VH(View view) {
                super(view);
                v = view;
                //cloud_image = v.findViewById(R.id.network_list_recyclerview_network_image);
                cloud_account_display = v.findViewById(R.id.network_list_recyclerview_display);
                cloud_account_user_id = v.findViewById(R.id.network_list_recyclerview_user_name);
                cloud_account_select_indicator = v.findViewById(R.id.network_list_recyclerview_select_indicator);
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

                            CloudAccountPOJO cloudAccountPOJO = viewModel.cloudAccountPOJOList.get(pos);
                            handleAccountClick(cloudAccountPOJO);
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
                    cloud_account_select_indicator.setVisibility(View.INVISIBLE);
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
                    cloud_account_select_indicator.setVisibility(View.VISIBLE);
                    viewModel.mselecteditems.put(pos, viewModel.cloudAccountPOJOList.get(pos));

                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;

                    ++size;
                    enable_disable_buttons(true, size);
                }
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
                authenticate();
//                NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = NetworkAccountDetailsInputDialog.getInstance(NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE, viewModel.type, null);
//                networkAccountDetailsInputDialog.show(getFragmentManager(), "");

            } else if (id == R.id.toolbar_btn_2) {
                int s = viewModel.mselecteditems.size();
                if (s > 0) {
                    CloudAccountPOJO cloudAccountPOJO = viewModel.mselecteditems.getValueAtIndex(0);
                    String display = cloudAccountPOJO.displayName;
//                    DeleteNetworkAccountAlertDialog deleteNetworkAccountAlertDialog = DeleteNetworkAccountAlertDialog.getInstance(NETWORK_ACCOUNT_DELETE_REQUEST_CODE, (display == null || display.isEmpty()) ? networkAccountPOJO.host : display, s);
//                    deleteNetworkAccountAlertDialog.show(getParentFragmentManager(), "");
                }
            } else if (id == R.id.toolbar_btn_3) {
                if (connected_cloud_account_pojo == null) {
                    return;
                }
                progress_bar.setVisibility(View.VISIBLE);
                //viewModel.disconnectNetworkConnection();
            } else if (id == R.id.toolbar_btn_4) {
                int s = viewModel.mselecteditems.size();
                if (s == 1) {
//                    NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = NetworkAccountDetailsInputDialog.getInstance(NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE, viewModel.type, viewModel.networkAccountPOJOList.get(viewModel.mselecteditems.getKeyAtIndex(0)));
//                    networkAccountDetailsInputDialog.show(getParentFragmentManager(), "");
                }
                clear_selection();
            } else if (id == R.id.toolbar_btn_5) {
                finish();
            }
        }
    }
}
