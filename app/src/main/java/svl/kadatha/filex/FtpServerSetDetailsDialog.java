package svl.kadatha.filex;

import static svl.kadatha.filex.Global.TAG;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import svl.kadatha.filex.ftpserver.server.FtpUser;
import timber.log.Timber;

public class FtpServerSetDetailsDialog extends DialogFragment {

    private Context context;
    private EditText port_input, user_input, password_input;
    private int port;
    String username, password, directory;
    private Spinner chroot_spinner;

    private InputMethodManager imm;
    private FtpServerViewModel viewModel;
    private String request_code;


    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            request_code=bundle.getString("request_code");
            port=bundle.getInt("port");
            username=bundle.getString("username");
            password=bundle.getString("password");
            directory=bundle.getString("directory");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_ftp_server_set_dialog,container,false);

        port_input=v.findViewById(R.id.ftp_server_port_input);
        user_input=v.findViewById(R.id.ftp_server_user_input);
        password_input=v.findViewById(R.id.ftp_server_pword_input);
        chroot_spinner=v.findViewById(R.id.ftp_server_chroot_spinner_input);

        viewModel=new ViewModelProvider(requireActivity()).get(FtpServerViewModel.class);
        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,viewModel.chroot_list);
        chroot_spinner.setAdapter(arrayAdapter);
        chroot_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                viewModel.chroot=viewModel.chroot_list.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ViewGroup buttons_layout = v.findViewById(R.id.ftp_server_credential_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
        ok_button.setText(R.string.ok);
        Button cancel_button = buttons_layout.findViewById(R.id.second_button);
        cancel_button.setText(R.string.cancel);

        ok_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if(validateInput(false))
                {
                    getParentFragmentManager().setFragmentResult(request_code,new Bundle());
                    dismissAllowingStateLoss();
                }

            }

        });

        cancel_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                dismissAllowingStateLoss();
            }
        });

        port_input.setText(String.valueOf(port));
        user_input.setText(username);
        password_input.setText(password);
        if(viewModel.chroot_list.size()>0)
        {
            for(int i=0; i<viewModel.chroot_list.size();++i)
            {
                String path=viewModel.chroot_list.get(i);
                if(path.equals(directory))
                {
                    chroot_spinner.setSelection(i);
                    break;
                }
            }

        }


        return v;
    }

    public static FtpServerSetDetailsDialog getInstance(String request_code, int port, String username, String password, String directory)
    {
        FtpServerSetDetailsDialog ftpServerSetDetailsDialog=new FtpServerSetDetailsDialog();
        Bundle bundle=new Bundle();
        bundle.putString("request_code",request_code);
        bundle.putInt("port",FtpServerViewModel.PORT);
        bundle.putString("username",username);
        bundle.putString("password",password);
        bundle.putString("directory",directory);
        ftpServerSetDetailsDialog.setArguments(bundle);
        return ftpServerSetDetailsDialog;
    }

    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, GridLayout.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private boolean validateInput(boolean correctIfNotValid) {

        int port=0;
        try {
            port = Integer.parseInt(port_input.getText().toString());
        } catch (Exception e) {
            Timber.tag(TAG).d("Error parsing port number! Moving on...");
            port= Integer.parseInt(getString(R.string.portnumber_default));
            FtpServerViewModel.PORT=port;
        }
        String username=user_input.getText().toString();
        String password=password_input.getText().toString();
        if(chroot_spinner.getSelectedItem()!=null)
        {
            directory=chroot_spinner.getSelectedItem().toString();
        }

        if(correctIfNotValid)
        {
            if (port <= 0 || 65535 < port) {
                port= Integer.parseInt(getString(R.string.portnumber_default));
                FtpServerViewModel.PORT=port;
                port_input.setText(port+"");
            }
            if (!username.matches("[a-zA-Z0-9]+")) {
                username=getString(R.string.username_default);
                user_input.setText(username);
            }
            if (!password.matches("[a-zA-Z0-9]+")) {
                password=getString(R.string.password_default);
                password_input.setText(password);
            }

            if (directory==null) {
                if(viewModel.chroot_list.size()>0)
                {
                    chroot_spinner.setSelection(0);
                    directory=chroot_spinner.getSelectedItem().toString();
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


        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imm.hideSoftInputFromWindow(port_input.getWindowToken(),0);
        imm.hideSoftInputFromWindow(user_input.getWindowToken(),0);
        imm.hideSoftInputFromWindow(password_input.getWindowToken(),0);
    }



}
