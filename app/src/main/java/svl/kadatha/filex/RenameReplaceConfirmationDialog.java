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
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class RenameReplaceConfirmationDialog extends DialogFragment {
    private String rename_file_name;
    private Context context;
    private Bundle bundle;

    public static RenameReplaceConfirmationDialog getInstance(String new_name) {
        RenameReplaceConfirmationDialog renameReplaceConfirmationDialog = new RenameReplaceConfirmationDialog();
        Bundle bundle = new Bundle();
        bundle.putString("rename_file_name", new_name);
        renameReplaceConfirmationDialog.setArguments(bundle);
        return renameReplaceConfirmationDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle = getArguments();
        rename_file_name = bundle.getString("rename_file_name");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_archive_replace_confirmation, container, false);
        TextView confirmation_message_textview = v.findViewById(R.id.dialog_fragment_archive_replace_message);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_archive_replace_confirmation_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button yes_button = buttons_layout.findViewById(R.id.first_button);
        yes_button.setText(R.string.yes);
        Button no_button = buttons_layout.findViewById(R.id.second_button);
        no_button.setText(R.string.no);
        confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it) + " '" + rename_file_name + "'");

        yes_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getParentFragmentManager().setFragmentResult(RenameFileDialog.REPLACEMENT_CONFIRMATION, bundle);
                dismissAllowingStateLoss();
            }

        });

        no_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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
