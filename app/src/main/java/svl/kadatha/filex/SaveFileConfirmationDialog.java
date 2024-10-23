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
import android.widget.EditText;
import android.widget.Gallery.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class SaveFileConfirmationDialog extends DialogFragment {
    private Context context;
    private String request_code;
    private boolean whether_closing;
    private Bundle bundle;

    public static SaveFileConfirmationDialog getInstance(String request_code, boolean whether_closing) {
        SaveFileConfirmationDialog dialog = new SaveFileConfirmationDialog();
        Bundle bundle = new Bundle();
        bundle.putString("request_code", request_code);
        bundle.putBoolean("whether_closing", whether_closing);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bundle = getArguments();
        request_code = bundle.getString("request_code");
        whether_closing = bundle.getBoolean("whether_closing");
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_rename_delete, container, false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);


        EditText new_file_name_edittext = v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        TextView no_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView size_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
        ViewGroup buttons_layout = v.findViewById((R.id.fragment_create_rename_delete_button_layout));
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button okbutton = buttons_layout.findViewById(R.id.first_button);
        okbutton.setText(R.string.yes);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
        cancelbutton.setText(R.string.no);

        dialog_heading_textview.setText(R.string.save);
        dialog_message_textview.setText(R.string.file_has_been_modified_do_you_want_save_the_file);
        new_file_name_edittext.setVisibility(View.GONE);
        no_files_textview.setVisibility(View.GONE);
        size_files_textview.setVisibility(View.GONE);

        okbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (whether_closing) {
                    bundle.putBoolean("to_close", true);
                } else {
                    bundle.putBoolean("next_action", true);
                }
                getParentFragmentManager().setFragmentResult(request_code, bundle);
                dismissAllowingStateLoss();
            }
        });

        cancelbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (whether_closing) {
                    bundle.putBoolean("to_close", false);
                } else {
                    bundle.putBoolean("next_action", false);
                }
                getParentFragmentManager().setFragmentResult(request_code, bundle);
                dismissAllowingStateLoss();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
