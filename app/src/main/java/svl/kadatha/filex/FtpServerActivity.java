package svl.kadatha.filex;

import static svl.kadatha.filex.Global.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.ftpserver.ftp.FsService;
import svl.kadatha.filex.ftpserver.ftp.FsSettings;
import svl.kadatha.filex.ftpserver.server.FtpUser;
import timber.log.Timber;

public class FtpServerActivity extends BaseActivity {
    private Context context;
    private TextView ftp_url_description_text_view,ftp_url_text_view;
    private TextView ftp_switch_label;
    private EditText port_host,user_name_host,password_host,chroot_host;

    private SwitchCompat ftp_start_stop_switch;
    private final Handler mHandler = new Handler();
    private Button set_button;
    private FtpServerViewModel viewModel;


    private static final String FTP_SERVER_CLOSE_REQUEST_CODE="ftp_server_close_request_code";
    private static final String FTP_SERVER_DETAILS_SET_REQUEST_CODE="ftp_server_details_set_request_code";
    private static final boolean[] alreadyNotificationWarned=new boolean[1];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context=this;
        setContentView(R.layout.activity_ftp_server);

        viewModel=new ViewModelProvider(this).get(FtpServerViewModel.class);
        TextView connection_status_tv = findViewById(R.id.ftp_server_connection_status);
        ftp_switch_label=findViewById(R.id.ftp_server_switch_label);
        CheckBox allow_anonymous_checkbox = findViewById(R.id.ftp_server_anonymous_check_box);
        allow_anonymous_checkbox.setChecked(FtpServerViewModel.ALLOW_ANONYMOUS);
        allow_anonymous_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                FtpServerViewModel.ALLOW_ANONYMOUS=b;
            }
        });

        ftp_url_description_text_view=findViewById(R.id.ftp_server_host_url_description);
        ftp_url_text_view=findViewById(R.id.ftp_server_host_url);
        port_host=findViewById(R.id.ftp_server_port);
        user_name_host=findViewById(R.id.ftp_server_user_name);
        password_host=findViewById(R.id.ftp_server_pword);
        chroot_host=findViewById(R.id.ftp_server_chroot);

        ftp_start_stop_switch=findViewById(R.id.ftp_server_ftp_switch);
        set_button=findViewById(R.id.ftp_server_credential_set_button);
        set_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username=user_name_host.getText().toString();
                String password=password_host.getText().toString();
                String directory=chroot_host.getText().toString();

                FtpServerSetDetailsDialog ftpServerSetDetailsDialog=FtpServerSetDetailsDialog.getInstance(FTP_SERVER_DETAILS_SET_REQUEST_CODE,FtpServerViewModel.PORT, username, password,directory);
                ftpServerSetDetailsDialog.show(getSupportFragmentManager(),"");
            }
        });

        if(FtpServerViewModel.PORT ==0)
        {
            FtpServerViewModel.PORT=2525;
            port_host.setText(""+FtpServerViewModel.PORT);
        }
        else {
            port_host.setText(""+FtpServerViewModel.PORT);
        }


        if(viewModel.user_name!=null && !viewModel.user_name.equals(""))
        {
            user_name_host.setText(viewModel.user_name);
        }
        else
        {
            user_name_host.setText(getString(R.string.username_default));
        }

        if(viewModel.password!=null && !viewModel.password.equals(""))
        {
            password_host.setText(viewModel.password);
        }
        else
        {
            password_host.setText(getString(R.string.password_default));
        }

        if(viewModel.chroot !=null && !viewModel.chroot.equals(""))
        {
            chroot_host.setText(viewModel.chroot);
        }
        else
        {
            chroot_host.setText(viewModel.chroot_list.get(0));
        }

        validateInput(true);


        Global.WARN_NOTIFICATIONS_DISABLED(context,FsNotification.CHANNEL_ID,alreadyNotificationWarned);
        updateRunningState();

//        ftp_start_stop_switch.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                return !validateInput(!set_port_pwd_group_checkbox.isChecked());
//
//            }
//        });

        ftp_start_stop_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if(b)
                {
                    validateInput(true);
                    FsService.start();
                }
                else
                {
                    FsService.stop();
                }
            }
        });

//        set_port_pwd_group_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if(b)
//                {
//                    user_password_group.setVisibility(View.GONE);
//                    port_password_edit_group.setVisibility(View.VISIBLE);
//                }
//                else
//                {
//                    user_password_group.setVisibility(View.VISIBLE);
//                    port_password_edit_group.setVisibility(View.GONE);
//                    imm.hideSoftInputFromWindow(port_input.getWindowToken(),0);
//                    imm.hideSoftInputFromWindow(user_input.getWindowToken(),0);
//                    imm.hideSoftInputFromWindow(password_input.getWindowToken(),0);
//                }
//                ftp_url_text_view.setVisibility(View.GONE);
//            }
//        });


        FrameLayout button_layout = findViewById(R.id.ftp_server_button_layout);
        button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT));
        Button close;
        close= button_layout.findViewById(R.id.first_button);
        close.setText(getString(R.string.close));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        getSupportFragmentManager().setFragmentResultListener(FTP_SERVER_DETAILS_SET_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                port_host.setText(""+FtpServerViewModel.PORT);
                user_name_host.setText(viewModel.user_name);
                password_host.setText(viewModel.password);
                chroot_host.setText(viewModel.chroot);
            }
        });
        getSupportFragmentManager().setFragmentResultListener(FTP_SERVER_CLOSE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(FTP_SERVER_CLOSE_REQUEST_CODE))
                {
                    finish();
                }
            }
        });
        disable_ftp_username_pwd_tv(true);
    }


    @Override
    public void onResume() {
        super.onResume();

        updateRunningState();

        Timber.tag(TAG).d("onResume: Registering the FTP server actions");
        IntentFilter filter = new IntentFilter();
        filter.addAction(FsService.ACTION_STARTED);
        filter.addAction(FsService.ACTION_STOPPED);
        filter.addAction(FsService.ACTION_FAILEDTOSTART);
        registerReceiver(mFsActionsReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.tag(TAG).d("onPause: Unregistering the FTPServer actions");
        unregisterReceiver(mFsActionsReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        if(FsService.isRunning())
        {
            YesOrNoAlertDialog ftpServerCloseAlertDialog= YesOrNoAlertDialog.getInstance(FTP_SERVER_CLOSE_REQUEST_CODE,R.string.want_to_stop_ftp_server_service,new Bundle());
            ftpServerCloseAlertDialog.show(getSupportFragmentManager(),"");
        }
        else
        {
            super.onBackPressed();
        }

    }

    private boolean validateInput(boolean correctIfNotValid) {

        int port=0;
        try {
            port = Integer.parseInt(port_host.getText().toString());
        } catch (Exception e) {
            Timber.tag(TAG).d("Error parsing port number! Moving on...");
            port= Integer.parseInt(getString(R.string.portnumber_default));
            FtpServerViewModel.PORT=port;
        }

        String username=user_name_host.getText().toString();
        String password=password_host.getText().toString();
        String directory=chroot_host.getText().toString();

        if(correctIfNotValid)
        {
            if (port <= 0 || 65535 < port) {
                port= Integer.parseInt(getString(R.string.portnumber_default));
                FtpServerViewModel.PORT=port;
                port_host.setText(String.valueOf(port));
            }
            if (!username.matches("[a-zA-Z0-9]+")) {
                username=getString(R.string.username_default);
                user_name_host.setText(username);
            }
            if (!password.matches("[a-zA-Z0-9]+")) {
                password=getString(R.string.password_default);
                password_host.setText(password);
            }

            if (directory==null) {
                if(viewModel.chroot_list.size()>0)
                {
                    chroot_host.setText(viewModel.chroot_list.get(0));
                }

            }
        }

        if (port <= 0 || 65535 < port) {
            Global.print(context,getString(R.string.port_validation_error));
            return false;
        }
        if (!username.matches("[a-zA-Z0-9]+")) {
            Global.print(context,getString(R.string.username_validation_error));
            return false;
        }
        if (!password.matches("[a-zA-Z0-9]+")) {
            Global.print(context,getString(R.string.password_validation_error));
            return false;
        }

        if (directory==null) {
            Global.print(context,getString(R.string.select_directory));
            return false;
        }

        FtpServerViewModel.PORT =port;
        viewModel.user_name=username;
        viewModel.password=password;
        viewModel.chroot=directory;
        FtpServerViewModel.FTP_USER=new FtpUser(username,password,directory);

        port_host.setText(String.valueOf(port));
        user_name_host.setText(viewModel.user_name);
        password_host.setText(viewModel.password);
        chroot_host.setText(viewModel.chroot);

        return true;
    }


    private void updateRunningState() {


        boolean service_started = FsService.isRunning();
        if (FsService.isRunning()) {

            // Fill in the FTP server address
            service_started =true;
            ftp_start_stop_switch.setChecked(true);
            set_button.setEnabled(false);
            set_button.setAlpha(Global.DISABLE_ALFA);
            //user_password_group.setVisibility(View.VISIBLE);
            //port_password_edit_group.setVisibility(View.GONE);
            //disable_ftp_username_pwd_tv(true);

            InetAddress address = FsService.getLocalInetAddress();
            if (address == null) {
                Timber.tag(TAG).d("Unable to retrieve wifi ip address");
                ftp_switch_label.setText(R.string.running_summary_failed_to_get_ip_address);
                return;
            }
            String ipText = "ftp://" + address.getHostAddress() + ":"
                    + FsSettings.getPortNumber() + "/";
            ftp_switch_label.setText(R.string.server_service_started);
            ftp_url_description_text_view.setVisibility(View.VISIBLE);
            ftp_url_text_view.setVisibility(View.VISIBLE);
            ftp_url_text_view.setText(ipText);
            //set_port_pwd_group_checkbox.setVisibility(View.INVISIBLE);

        } else {
            ftp_start_stop_switch.setChecked(false);
            service_started =false;
            ftp_switch_label.setText(R.string.running_summary_stopped);
            set_button.setEnabled(true);
            set_button.setAlpha(Global.ENABLE_ALFA);

//            if(set_port_pwd_group_checkbox.isChecked())
//            {
//                user_password_group.setVisibility(View.GONE);
//                port_password_edit_group.setVisibility(View.VISIBLE);
//            }
//            else
//            {
//                user_password_group.setVisibility(View.VISIBLE);
//                port_password_edit_group.setVisibility(View.GONE);
//            }
            //disable_ftp_username_pwd_tv(false);
//            user_name_host.setText(viewModel.user_name);
//            password_host.setText(viewModel.password);
//            chroot_host.setText(viewModel.chroot);
//            set_port_pwd_group_checkbox.setVisibility(View.VISIBLE);
            ftp_url_description_text_view.setVisibility(View.GONE);
            ftp_url_text_view.setVisibility(View.GONE);
        }
    }


    /**
     * This receiver will check FTPServer.ACTION* messages and will update the button,
     * running_state, if the server is running and will also display at what url the
     * server is running.
     */
    final BroadcastReceiver mFsActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.tag(TAG).d("action received: " + intent.getAction());
            if (intent.getAction() == null) {
                return;
            }
            // remove all pending callbacks
            mHandler.removeCallbacksAndMessages(null);
            // action will be ACTION_STARTED or ACTION_STOPPED
            updateRunningState();
            // or it might be ACTION_FAILEDTOSTART
            if (intent.getAction().equals(FsService.ACTION_FAILEDTOSTART)) {
                ftp_start_stop_switch.setChecked(false);
                mHandler.postDelayed(
                        () -> ftp_switch_label.setText(R.string.running_summary_failed),
                        100);
                mHandler.postDelayed(
                        () -> ftp_switch_label.setText(R.string.running_summary_stopped),
                        5000);
            }
        }
    };


    private void disable_ftp_username_pwd_tv(boolean disable)
    {
        if(disable)
        {
            port_host.clearFocus();
            user_name_host.clearFocus();
            password_host.clearFocus();
            chroot_host.clearFocus();
            port_host.setShowSoftInputOnFocus(false);
            user_name_host.setShowSoftInputOnFocus(false);
            password_host.setShowSoftInputOnFocus(false);
            chroot_host.setShowSoftInputOnFocus(false);
        }
        else
        {
            port_host.setShowSoftInputOnFocus(true);
            user_name_host.setShowSoftInputOnFocus(true);
            password_host.setShowSoftInputOnFocus(true);
            chroot_host.setShowSoftInputOnFocus(true);
        }

        port_host.setLongClickable(!disable);
        port_host.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return disable;
            }
        });

        user_name_host.setLongClickable(!disable);
        user_name_host.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent me)
            {
                return disable;
            }
        });

        password_host.setLongClickable(!disable);
        password_host.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent me)
            {
                return disable;
            }
        });

        chroot_host.setLongClickable(!disable);
        chroot_host.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent me)
            {
                return disable;
            }
        });
    }
}
