package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

public class DeleteFtpAlertDialog extends DialogFragment {

    private Context context;
    private String ftp_display;
    private int ftp_selected_size;
    private String request_code;
    private TextView textView;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle bundle=getArguments();

        if(bundle!=null)
        {
            request_code=bundle.getString("request_code");
            ftp_display=bundle.getString("ftp_display");
            ftp_selected_size=bundle.getInt("ftp_selected_size");

        }

    }

    public static DeleteFtpAlertDialog getInstance(String request_code,String ftp_display, int ftp_selected_size)
    {
        DeleteFtpAlertDialog deleteFtpAlertDialog=new DeleteFtpAlertDialog();
        Bundle bundle=new Bundle();
        bundle.putString("request_code",request_code);
        bundle.putString("ftp_display",ftp_display);
        bundle.putInt("ftp_selected_size",ftp_selected_size);
        deleteFtpAlertDialog.setArguments(bundle);
        return deleteFtpAlertDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method
        View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
        TextView number_tv = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView size_tv = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
        number_tv.setVisibility(View.GONE);
        size_tv.setVisibility(View.GONE);
        if(ftp_selected_size==1)
        {
            dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_ftp_details)+" '"+ftp_display+"'");
        }
        else
        {
            dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_ftp_details)+" "+ftp_selected_size+" "+getString(R.string.ftp_server_details));
        }

        EditText new_file_name_edittext = v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);

        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button okbutton = buttons_layout.findViewById(R.id.first_button);
        okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
        cancelbutton.setText(R.string.cancel);
        dialog_heading_textview.setText(R.string.delete);
        new_file_name_edittext.setVisibility(View.GONE);

        okbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                ((AppCompatActivity)context).getSupportFragmentManager().setFragmentResult(request_code,null);
                dismissAllowingStateLoss();
            }

        });

        cancelbutton.setOnClickListener(new View.OnClickListener()
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
        window.setLayout(Global.DIALOG_WIDTH, AbsListView.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
