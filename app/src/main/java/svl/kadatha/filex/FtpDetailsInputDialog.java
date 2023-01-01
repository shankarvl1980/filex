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
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

public class FtpDetailsInputDialog extends DialogFragment {

    private Context context;
    private FtpDatabaseHelper ftpDatabaseHelper;
    private String original_server="",server="",mode="",user_name="",password="",encoding,display="";
    private int port;
    private int anonymous;
    private TextView server_tv,port_tv,user_name_tv,password_tv,encoding_tv,display_tv;
    private RadioButton mode_active_radio_btn;
    private RadioButton anonymous_radio_btn;
    private boolean update;
    private PermissionsUtil permissionsUtil;
    private String request_code;
    Bundle bundle;


    public static FtpDetailsInputDialog getInstance(String request_code,String server)
    {
        FtpDetailsInputDialog ftpDetailsInputDialog=new FtpDetailsInputDialog();
        Bundle bundle=new Bundle();
        bundle.putString("request_code",request_code);
        bundle.putString("server",server);
        ftpDetailsInputDialog.setArguments(bundle);
        return ftpDetailsInputDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
        ftpDatabaseHelper=new FtpDatabaseHelper(context);
        permissionsUtil=new PermissionsUtil(context,(AppCompatActivity)context);
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
            request_code=bundle.getString("request_code");
            original_server=bundle.getString("server");
            server=bundle.getString("server");
            if(original_server!=null && !original_server.equals(""))
            {
                update=true;
                FtpDetailsDialog.FtpPOJO ftpPOJO= ftpDatabaseHelper.getFtpPOJO(server);
                if(ftpPOJO!=null)
                {
                    port=ftpPOJO.port;
                    mode=ftpPOJO.mode;
                    user_name=ftpPOJO.user_name;
                    password=ftpPOJO.password;
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
        server_tv=v.findViewById(R.id.ftp_details_server);
        port_tv=v.findViewById(R.id.ftp_details_port);
        mode_active_radio_btn=v.findViewById(R.id.ftp_details_active_radio_btn);
        RadioButton mode_passive_radio_btn = v.findViewById(R.id.ftp_details_passive_radio_btn);
        user_name_tv=v.findViewById(R.id.ftp_details_user_name);
        password_tv=v.findViewById(R.id.ftp_details_pword);
        anonymous_radio_btn=v.findViewById(R.id.ftp_details_anonymous_radio_btn);
        //encoding_tv=v.findViewById(R.id.ftp_details_e);
        display_tv=v.findViewById(R.id.ftp_details_display);

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
        anonymous_radio_btn.setChecked(anonymous != 0);
        display_tv.setText(display);
        ViewGroup buttons_layout = v.findViewById(R.id.ftp_details_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
        ok_button.setText(R.string.ok);
        ok_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                server=server_tv.getText().toString().trim();
                user_name=user_name_tv.getText().toString().trim();
                if(server.equals("") || port_tv.getText().toString().trim().equals("")|| user_name.equals(""))
                {
                    Global.print(context,getString(R.string.server_port_username_fields_can_not_be_empty));
                }
                else if(!permissionsUtil.isNetworkConnected())
                {
                    Global.print(context,getString(R.string.not_connected_to_network));
                }
                else
                {
                    port=Integer.parseInt(port_tv.getText().toString().trim());
                    mode=mode_active_radio_btn.isChecked() ? "active" : "passive";
                    password=password_tv.getText().toString().trim();
                    anonymous=anonymous_radio_btn.isChecked() ? 1 : 0;
                    display=display_tv.getText().toString().trim();

                    long row_number;
                    if(update)
                    {
                        row_number=ftpDatabaseHelper.update(original_server,server,port,mode,user_name,password, anonymous != 0,encoding,display);
                        bundle.putString("original_server",original_server);
                        bundle.putString("server",server);
                    }
                    else
                    {
                        row_number=ftpDatabaseHelper.insert(server,port,mode,user_name,password, anonymous != 0,encoding,display);
                        bundle.putString("server",server);
                    }

                    if(row_number>0)// && ftpDatabaseModificationListener!=null)
                    {

                        //FtpDetailsDialog.FtpPOJO ftpPOJO=new FtpDetailsDialog.FtpPOJO(server,port,mode,user_name,password, anonymous != 0,encoding,display);
                        //ftpDatabaseModificationListener.onInsert(ftpPOJO);
                        ((AppCompatActivity)context).getSupportFragmentManager().setFragmentResult(request_code,bundle);
                    }

                    dismissAllowingStateLoss();
                }


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
        return v;
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
