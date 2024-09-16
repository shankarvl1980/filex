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
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;

public class FtpDetailsInputDialog extends DialogFragment {

    private Context context;
    private FtpDatabaseHelper ftpDatabaseHelper;
    private String original_server="",original_user_name="",server="",mode="",user_name="",password="",type="",encoding="",display="";
    private int port;
    private int anonymous;
    private TextView server_tv,port_tv,user_name_tv,password_tv,encoding_tv,display_tv;
    private RadioButton mode_active_radio_btn;
    private CheckBox anonymous_check_box;
    private boolean update,replace;
    private Bundle bundle;
    private static final String FTP_REPLACE_REQUEST_CODE="ftp_replace_request_code";

    public static FtpDetailsInputDialog getInstance(String request_code,String server,String user_name,String type)
    {
        FtpDetailsInputDialog ftpDetailsInputDialog=new FtpDetailsInputDialog();
        Bundle bundle=new Bundle();
        bundle.putString("request_code",request_code);
        bundle.putString("server",server);
        bundle.putString("user_name",user_name);
        bundle.putString("type",type);
        ftpDetailsInputDialog.setArguments(bundle);
        return ftpDetailsInputDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
        ftpDatabaseHelper=new FtpDatabaseHelper(context);
        PermissionsUtil permissionsUtil = new PermissionsUtil(context, (AppCompatActivity) context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ftpDatabaseHelper.close();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle=getArguments();
        if(bundle!=null)
        {
            String request_code = bundle.getString("request_code");
            original_server=bundle.getString("server");
            original_user_name=bundle.getString("user_name");
            type=bundle.getString("type");
            if(original_server==null)
            {
            }
            else
            {
                server=bundle.getString("server");
            }
            user_name=bundle.getString("user_name");
            if(original_server!=null && !original_server.isEmpty())
            {
                update=true;
                FtpDetailsDialog.FtpPOJO ftpPOJO= ftpDatabaseHelper.getFtpPOJO(original_server,original_user_name,type);
                if(ftpPOJO!=null)
                {
                    port=ftpPOJO.port;
                    mode=ftpPOJO.mode;
                    password=ftpPOJO.password;
                    type=ftpPOJO.type;
                    anonymous=ftpPOJO.anonymous ? 1 : 0;
                    encoding=ftpPOJO.encoding;
                    display=ftpPOJO.display;
                }

            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_ftp_details_input,container,false);
        TextView title_tv=v.findViewById(R.id.ftp_details_title);
        if(type.equals(FtpDetailsDialog.FTP)){
            if(update) {
                title_tv.setText(R.string.update_ftp_server);
            } else{
                title_tv.setText(R.string.new_ftp_server);
            }
        }
        else if(type.equals(FtpDetailsDialog.SFTP)){
            if(update) {
                title_tv.setText(R.string.update_sftp_server);
            } else{
                title_tv.setText(R.string.new_sftp_server);
            }
        }

        server_tv=v.findViewById(R.id.ftp_details_server);
        port_tv=v.findViewById(R.id.ftp_details_port);
        mode_active_radio_btn=v.findViewById(R.id.ftp_details_active_radio_btn);
        RadioButton mode_passive_radio_btn = v.findViewById(R.id.ftp_details_passive_radio_btn);
        user_name_tv=v.findViewById(R.id.ftp_details_user_name);
        password_tv=v.findViewById(R.id.ftp_details_pword);
        anonymous_check_box=v.findViewById(R.id.ftp_details_anonymous_check_box);
        //encoding_tv=v.findViewById(R.id.ftp_details_en);
        display_tv=v.findViewById(R.id.ftp_details_display);
        Button connect_button = v.findViewById(R.id.ftp_details_connect);
        connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOkButtonClick(true);
            }
        });

        server_tv.setText(server);
        port_tv.setText("21");
        if(mode.equals("active"))
        {
            mode_active_radio_btn.setChecked(true);
        }
        else
        {
            mode_passive_radio_btn.setChecked(true);
        }

        user_name_tv.setText(user_name);
        password_tv.setText(password);
        anonymous_check_box.setChecked(anonymous != 0);
        display_tv.setText(display);

        ViewGroup buttons_layout = v.findViewById(R.id.ftp_details_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
        ok_button.setText(R.string.ok);
        ok_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                onOkButtonClick(false);
            }
        });

        Button cancel_button = buttons_layout.findViewById(R.id.second_button);
        cancel_button.setText(R.string.cancel);
        cancel_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                dismissAllowingStateLoss();
            }
        });

        getParentFragmentManager().setFragmentResultListener(FTP_REPLACE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(FTP_REPLACE_REQUEST_CODE))
                {
                    replace=true;
                    boolean whetherToConnect=result.getBoolean("whetherToConnect");
                    onOkButtonClick(whetherToConnect);
                }
            }
        });
        return v;
    }

    private void onOkButtonClick(boolean whetherToConnect)
    {
        server=server_tv.getText().toString().trim();
        user_name=user_name_tv.getText().toString().trim();
        if(server.isEmpty() || port_tv.getText().toString().trim().isEmpty() || user_name.isEmpty())
        {
            Global.print(context,getString(R.string.server_port_username_fields_can_not_be_empty));
            return;
        }


        if(!server.matches("\\S+"))
        {
            Global.print(context,getString(R.string.server_address_should_not_contain_spaces));
            return;
        }

        port=Integer.parseInt(port_tv.getText().toString().trim());
        mode=mode_active_radio_btn.isChecked() ? "active" : "passive";
        password=password_tv.getText().toString().trim();
        anonymous=anonymous_check_box.isChecked() ? 1 : 0;
        display=display_tv.getText().toString().trim();
        bundle.putBoolean("whetherToConnect",whetherToConnect);
        if(!update && whetherFtpPOJOAlreadyExists(server,user_name,type) && !replace)
        {
            YesOrNoAlertDialog ftpServerCloseAlertDialog= YesOrNoAlertDialog.getInstance(FTP_REPLACE_REQUEST_CODE,R.string.ftp_setting_already_exists_want_to_replace_it,bundle);
            ftpServerCloseAlertDialog.show(getParentFragmentManager(),"");
        }
        else
        {
            bundle.putString("original_server",original_server);
            bundle.putString("original_user_name",original_user_name);
            bundle.putString("server",server);
            bundle.putInt("port",port);
            bundle.putString("mode",mode);
            bundle.putString("user_name",user_name);
            bundle.putString("password",password);
            bundle.putString("type",type);
            bundle.putBoolean("anonymous",anonymous != 0);
            bundle.putString("encoding",encoding);
            bundle.putString("display",display);
            bundle.putBoolean("update",update);
            bundle.putBoolean("replace",replace);


            dismissAllowingStateLoss();
            //request_code gets changed on yesornodialog orientation change. so request_code be hardcoded.
            getParentFragmentManager().setFragmentResult(FtpDetailsDialog.FTP_INPUT_DETAILS_REQUEST_CODE,bundle);
        }
    }
    private boolean whetherFtpPOJOAlreadyExists(String server,String user_name,String type)
    {
        FtpDetailsDialog.FtpPOJO ftpPOJO=ftpDatabaseHelper.getFtpPOJO(server,user_name,type);
        return ftpPOJO != null;
    }
    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, TableRow.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    }

}
