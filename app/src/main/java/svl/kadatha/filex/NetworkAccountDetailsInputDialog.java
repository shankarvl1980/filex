package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;

public class NetworkAccountDetailsInputDialog extends DialogFragment {

    private static final String NETWORK_ACCOUNT_REPLACE_REQUEST_CODE = "network_account_replace_request_code";
    private final ActivityResultLauncher<Intent> activityResultLauncher_file_select = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                private_key_path_tv.setText(intent.getStringExtra("filepathclickselected"));
            }
        }
    });
    private Context context;
    private NetworkAccountsDatabaseHelper networkAccountsDatabaseHelper;
    private String original_host = "", original_user_name = "", host = "", user_name = "", password = "", type = "", encoding = "", display = "";
    private int original_port, port;
    //ftp specific fields
    private String mode = "";
    private boolean anonymous, useFTPS;
    //sftp specific fields
    private String privateKeyPath = "", privateKeyPassphrase = "", knownHostsPath = "";
    //webDAV specific fields
    private boolean useHTTPS;
    private String basePath = "";
    //smb specific fields
    private String domain = "", shareName = "", smbVersion = "";
    private Spinner smbVersion_spinner;
    private TextView server_tv, port_tv, user_name_tv, password_tv, encoding_tv, display_tv, private_key_path_tv, private_key_passphrase_tv, known_hosts_path_tv;
    private TextView base_path_tv, domain_tv, share_name_tv;
    private RadioButton mode_active_radio_btn;
    private CheckBox anonymous_check_box, useFTPS_check_box, useHTTPS_check_box;
    private boolean update, replace;
    private Bundle bundle;

    public static NetworkAccountDetailsInputDialog getInstance(String request_code, String type, NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO) {
        NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = new NetworkAccountDetailsInputDialog();
        Bundle bundle = new Bundle();
        bundle.putString("request_code", request_code);
        bundle.putParcelable("networkAccountPOJO", networkAccountPOJO);
        bundle.putString("type", type);
        networkAccountDetailsInputDialog.setArguments(bundle);
        return networkAccountDetailsInputDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        networkAccountsDatabaseHelper = new NetworkAccountsDatabaseHelper(context);
        PermissionsUtil permissionsUtil = new PermissionsUtil(context, (AppCompatActivity) context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        networkAccountsDatabaseHelper.close();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle = getArguments();
        if (bundle != null) {
            String request_code = bundle.getString("request_code");
            type = bundle.getString("type");
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = bundle.getParcelable("networkAccountPOJO");

            if (networkAccountPOJO != null) {
                original_host = networkAccountPOJO.host;
                original_port = networkAccountPOJO.port;
                original_user_name = networkAccountPOJO.user_name;
                host = networkAccountPOJO.host;
                port = networkAccountPOJO.port;
                user_name = networkAccountPOJO.user_name;
            }

            if (original_host != null && !original_host.isEmpty()) {
                update = true;
                if (networkAccountPOJO != null) {
                    mode = networkAccountPOJO.mode;
                    password = networkAccountPOJO.password;
                    type = networkAccountPOJO.type;
                    anonymous = networkAccountPOJO.anonymous;
                    encoding = networkAccountPOJO.encoding;
                    display = networkAccountPOJO.display;
                    useFTPS = networkAccountPOJO.useFTPS;
                    privateKeyPath = networkAccountPOJO.privateKeyPath;
                    privateKeyPassphrase = networkAccountPOJO.privateKeyPassphrase;
                    knownHostsPath = networkAccountPOJO.knownHostsPath;
                    basePath = networkAccountPOJO.basePath;
                    useHTTPS = networkAccountPOJO.useHTTPS;
                    domain = networkAccountPOJO.domain;
                    shareName = networkAccountPOJO.shareName;
                    smbVersion = networkAccountPOJO.smbVersion;
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_network_details_input, container, false);
        LinearLayout ftp_group = v.findViewById(R.id.fragment_network_details_input_ftp_group);
        LinearLayout sftp_group = v.findViewById(R.id.fragment_network_details_input_sftp_group);
        LinearLayout webdav_group = v.findViewById(R.id.fragment_network_details_input_webdav_group);
        LinearLayout smb_group = v.findViewById(R.id.fragment_network_details_input_smb_group);
        LinearLayout port_container = v.findViewById(R.id.fragment_network_account_port_container);
        switch (type) {
            case NetworkAccountsDetailsDialog.FTP:
                ftp_group.setVisibility(View.VISIBLE);
                break;
            case NetworkAccountsDetailsDialog.SFTP:
                sftp_group.setVisibility(View.VISIBLE);
                break;
            case NetworkAccountsDetailsDialog.WebDAV:
                webdav_group.setVisibility(View.VISIBLE);
                break;
            case NetworkAccountsDetailsDialog.SMB:
                smb_group.setVisibility(View.VISIBLE);
                port_container.setVisibility(View.GONE);
                break;
        }

        TextView title_tv = v.findViewById(R.id.fragment_network_details_input_heading);
        if (type.equals(NetworkAccountsDetailsDialog.FTP)) {
            if (update) {
                title_tv.setText(R.string.update_ftp_client);
            } else {
                title_tv.setText(R.string.new_ftp_client);
            }
        } else if (type.equals(NetworkAccountsDetailsDialog.SFTP)) {
            if (update) {
                title_tv.setText(R.string.update_sftp_client);
            } else {
                title_tv.setText(R.string.new_sftp_client);
            }
        } else if (type.equals(NetworkAccountsDetailsDialog.WebDAV)) {
            if (update) {
                title_tv.setText(R.string.update_webdav_client);
            } else {
                title_tv.setText(R.string.new_webdav_client);
            }
        } else if (type.equals(NetworkAccountsDetailsDialog.SMB)) {
            if (update) {
                title_tv.setText(R.string.update_smb_client);
            } else {
                title_tv.setText(R.string.new_smb_client);
            }
        }

        server_tv = v.findViewById(R.id.fragment_network_details_input_server);
        port_tv = v.findViewById(R.id.fragment_network_details_input_port);
        mode_active_radio_btn = v.findViewById(R.id.fragment_network_details_input_mode_active);
        RadioButton mode_passive_radio_btn = v.findViewById(R.id.fragment_network_details_input_mode_passive);
        user_name_tv = v.findViewById(R.id.fragment_network_details_input_username);
        password_tv = v.findViewById(R.id.fragment_network_details_input_password);
        anonymous_check_box = v.findViewById(R.id.fragment_network_details_input_anonymous);
        encoding_tv = v.findViewById(R.id.fragment_network_details_input_encoding);
        display_tv = v.findViewById(R.id.fragment_network_details_input_display);
        useFTPS_check_box = v.findViewById(R.id.fragment_network_details_input_use_ftps);
        private_key_path_tv = v.findViewById(R.id.fragment_network_details_input_private_key_path);
        private_key_passphrase_tv = v.findViewById(R.id.fragment_network_details_input_private_key_passphrase);
        known_hosts_path_tv = v.findViewById(R.id.fragment_network_details_input_known_hosts_path);
        base_path_tv = v.findViewById(R.id.fragment_network_details_input_base_path);
        useHTTPS_check_box = v.findViewById(R.id.fragment_network_details_input_use_https);
        domain_tv = v.findViewById(R.id.fragment_network_details_input_domain);
        share_name_tv = v.findViewById(R.id.fragment_network_details_input_share_name);
        smbVersion_spinner = v.findViewById(R.id.fragment_network_details_input_smb_version);
        ArrayAdapter<CharSequence> smbVersionArrayAdapter = ArrayAdapter.createFromResource(context, R.array.smb_versions, android.R.layout.simple_spinner_item);

        server_tv.setText(host);
        if (port == 0) {
            if (type.equals(NetworkAccountsDetailsDialog.FTP)) {
                port = 21;
            } else if (type.equals(NetworkAccountsDetailsDialog.SFTP)) {
                port = 22;
            } else if (type.equals(NetworkAccountsDetailsDialog.WebDAV)) {
                if (useHTTPS) port = 443;
                else port = 80;
            }
        }
        port_tv.setText(String.valueOf(port));

        if (mode == null || mode.isEmpty()) mode = "passive";
        if (mode.equals("active")) {
            mode_active_radio_btn.setChecked(true);
        } else {
            mode_passive_radio_btn.setChecked(true);
        }

        user_name_tv.setText(user_name);
        password_tv.setText(password);
        encoding_tv.setText(encoding);
        anonymous_check_box.setChecked(anonymous);
        display_tv.setText(display);
        useFTPS_check_box.setChecked(useFTPS);
        private_key_path_tv.setText(privateKeyPath);
        private_key_passphrase_tv.setText(privateKeyPassphrase);
        known_hosts_path_tv.setText(knownHostsPath);
        base_path_tv.setText(basePath);
        useHTTPS_check_box.setChecked(useHTTPS);
        domain_tv.setText(domain);
        share_name_tv.setText(shareName);
        if (smbVersion == null || smbVersion.isEmpty()) smbVersion = "SMB2";
        smbVersion_spinner.setSelection(smbVersionArrayAdapter.getPosition(smbVersion));

        Button private_key_select_button = v.findViewById(R.id.fragment_network_details_input_select_private_key);
        private_key_select_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppCompatActivity appCompatActivity = (AppCompatActivity) context;
                if (appCompatActivity instanceof MainActivity) {
                    ((MainActivity) context).clear_cache = false;
                }

                Intent intent = new Intent(context, FileSelectorActivity.class);
                intent.putExtra(FileSelectorActivity.ACTION_SOUGHT, FileSelectorActivity.FILE_PATH_REQUEST_CODE);
                activityResultLauncher_file_select.launch(intent);
            }
        });

        useHTTPS_check_box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) port = 443;
                else port = 80;
                port_tv.setText(String.valueOf(port));
            }
        });

        ViewGroup buttons_layout = v.findViewById(R.id.fragment_network_details_input_button_container);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
        ok_button.setText(R.string.ok);
        ok_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onOkButtonClick(false);
            }
        });

        Button cancel_button = buttons_layout.findViewById(R.id.second_button);
        cancel_button.setText(R.string.cancel);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });

        getParentFragmentManager().setFragmentResultListener(NETWORK_ACCOUNT_REPLACE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(NETWORK_ACCOUNT_REPLACE_REQUEST_CODE)) {
                    replace = true;
                    boolean whetherToConnect = result.getBoolean("whetherToConnect");
                    onOkButtonClick(whetherToConnect);
                }
            }
        });
        return v;
    }

    private void onOkButtonClick(boolean whetherToConnect) {
        host = server_tv.getText().toString().trim();
        user_name = user_name_tv.getText().toString().trim();
        if (host.isEmpty() || port_tv.getText().toString().trim().isEmpty() || user_name.isEmpty()) {
            Global.print(context, getString(R.string.enter_host_port_username_fields));
            return;
        }

        if (!host.matches("\\S+")) {
            Global.print(context, getString(R.string.host_address_should_not_contain_spaces));
            return;
        }

        if (type.equals(NetworkAccountsDetailsDialog.SMB) && share_name_tv.getText().toString().trim().isEmpty()) {
            Global.print(context, getString(R.string.enter_share_name));
            return;
        }

        port = Integer.parseInt(port_tv.getText().toString().trim());
        mode = mode_active_radio_btn.isChecked() ? "active" : "passive";
        password = password_tv.getText().toString().trim();
        anonymous = anonymous_check_box.isChecked();
        display = display_tv.getText().toString().trim();
        encoding = encoding_tv.getText().toString().trim();
        useFTPS = useFTPS_check_box.isChecked();
        useHTTPS = useHTTPS_check_box.isChecked();
        privateKeyPath = private_key_passphrase_tv.getText().toString().trim();
        privateKeyPassphrase = private_key_passphrase_tv.getText().toString().trim();
        knownHostsPath = known_hosts_path_tv.getText().toString().trim();
        basePath = base_path_tv.getText().toString().trim();
        domain = domain_tv.getText().toString().trim();
        shareName = share_name_tv.getText().toString().trim();
        if (smbVersion_spinner.getSelectedItem() != null) {
            smbVersion = smbVersion_spinner.getSelectedItem().toString();
        } else {
            smbVersion = "SMB2";
        }


        bundle.putBoolean("whetherToConnect", whetherToConnect);
        if (!update && whetherFtpPOJOAlreadyExists(host, user_name, type) && !replace) {
            YesOrNoAlertDialog ftpServerCloseAlertDialog = YesOrNoAlertDialog.getInstance(NETWORK_ACCOUNT_REPLACE_REQUEST_CODE, R.string.network_account_setting_already_exists_want_to_replace_it, bundle);
            ftpServerCloseAlertDialog.show(getParentFragmentManager(), "");
        } else {
            bundle.putString("original_host", original_host);
            bundle.putInt("original_port", original_port);
            bundle.putString("original_user_name", original_user_name);
            bundle.putBoolean("update", update);
            bundle.putBoolean("replace", replace);
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = new NetworkAccountsDetailsDialog.NetworkAccountPOJO(
                    host, port, user_name, password, encoding, display, type, mode, anonymous, useFTPS, privateKeyPath, privateKeyPassphrase, knownHostsPath, basePath, useHTTPS, domain, shareName, smbVersion);
            bundle.putParcelable("networkAccountPOJO", networkAccountPOJO);

            dismissAllowingStateLoss();
            //request_code gets changed on yesornodialog orientation change. so request_code be hardcoded.
            getParentFragmentManager().setFragmentResult(NetworkAccountsDetailsDialog.NETWORK_ACCOUNT_INPUT_DETAILS_REQUEST_CODE, bundle);
        }
    }

    private boolean whetherFtpPOJOAlreadyExists(String host, String user_name, String type) {
        NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO = networkAccountsDatabaseHelper.getNetworkAccountPOJO(host, port, user_name, type);
        return networkAccountPOJO != null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, TableRow.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}