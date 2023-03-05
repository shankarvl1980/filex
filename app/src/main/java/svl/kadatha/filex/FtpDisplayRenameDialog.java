package svl.kadatha.filex;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

public class FtpDisplayRenameDialog extends DialogFragment {

    private Context context;
    private String server,user_name, display;
    private EditText new_ftp_name_edittext;
    private InputMethodManager imm;
    private FtpDatabaseHelper ftpDatabaseHelper;
    private String request_code;
    private Bundle bundle;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
        ftpDatabaseHelper=new FtpDatabaseHelper(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ftpDatabaseHelper.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle=getArguments();
        if(bundle!=null)
        {
            request_code=bundle.getString("request_code");
            server=bundle.getString("server");
            user_name=bundle.getString("user_name");
            display=bundle.getString("display");
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method

        View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
        dialog_message_textview.setVisibility(View.GONE);
        new_ftp_name_edittext=v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        TextView no_of_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView files_size_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
        no_of_files_textview.setVisibility(View.GONE);
        files_size_textview.setVisibility(View.GONE);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button okbutton = buttons_layout.findViewById(R.id.first_button);
        okbutton.setText(R.string.ok);
        Button cancelbutton = v.findViewById(R.id.second_button);
        cancelbutton.setText(R.string.cancel);

        dialog_heading_textview.setText(R.string.rename);
        new_ftp_name_edittext.setText(display);
        int l=display.lastIndexOf(".");
        if(l==-1)
        {
            l=display.length();
        }
        new_ftp_name_edittext.setSelection(0,l);

        imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

        okbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {

                final String new_name=new_ftp_name_edittext.getText().toString().trim();
                if(new_name.equals(display))
                {
                    imm.hideSoftInputFromWindow(new_ftp_name_edittext.getWindowToken(),0);
                    dismissAllowingStateLoss();
                    return;
                }
                /*
                if(new_name.equalsIgnoreCase(display))
                {
                    imm.hideSoftInputFromWindow(new_ftp_name_edittext.getWindowToken(),0);
                    dismissAllowingStateLoss();
                    print(getString(R.string.could_not_be_renamed));
                    return;
                }

                if(CheckStringForSpecialCharacters.whetherStringContains(new_name))
                {
                    print(getString(R.string.avoid_name_involving_special_characters));
                    return;
                }

                 */
                if(new_name.equals(""))
                {
                    Global.print(context,getString(R.string.enter_file_name));
                    return;
                }
                int i=ftpDatabaseHelper.change_display(server,user_name,new_name);
                if(i>0)
                {
                    bundle.putString("new_name",new_name);
                    ((AppCompatActivity)context).getSupportFragmentManager().setFragmentResult(request_code,bundle);
                }
                imm.hideSoftInputFromWindow(new_ftp_name_edittext.getWindowToken(),0);
                dismissAllowingStateLoss();
            }
        });

        cancelbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                imm.hideSoftInputFromWindow(new_ftp_name_edittext.getWindowToken(),0);
                dismissAllowingStateLoss();
            }
        });
        return v;
    }

    public static FtpDisplayRenameDialog getInstance(String request_code,String server,String user_name, String display)
    {
        FtpDisplayRenameDialog ftpDisplayRenameDialog=new FtpDisplayRenameDialog();
        Bundle bundle=new Bundle();
        bundle.putString("request_code",request_code);
        bundle.putString("server",server);
        bundle.putString("user_name",user_name);
        bundle.putString("display",display);
        ftpDisplayRenameDialog.setArguments(bundle);
        return ftpDisplayRenameDialog;
    }


    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, AbsListView.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        new_ftp_name_edittext.requestFocus();
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        // TODO: Implement this method
        super.onCancel(dialog);
        imm.hideSoftInputFromWindow(new_ftp_name_edittext.getWindowToken(),0);
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        // TODO: Implement this method
        imm.hideSoftInputFromWindow(new_ftp_name_edittext.getWindowToken(),0);
        ftpDatabaseHelper.close();
        super.onDismiss(dialog);
    }


}
