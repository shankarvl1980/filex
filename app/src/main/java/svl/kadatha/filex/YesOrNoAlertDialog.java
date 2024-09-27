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
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class YesOrNoAlertDialog extends DialogFragment {
    private Bundle bundle;
    private String request_code;
    private int message;
    private Context context;

    public static YesOrNoAlertDialog getInstance(String request_code, int message, Bundle bundle) {
        YesOrNoAlertDialog ftpServerCloseAlertDialog = new YesOrNoAlertDialog();
        bundle.putString("request_code", request_code);
        bundle.putInt("message", message);
        ftpServerCloseAlertDialog.setArguments(bundle);
        return ftpServerCloseAlertDialog;
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
        request_code = bundle.getString("request_code");
        message = bundle.getInt("message");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: Implement this method
        View v = inflater.inflate(R.layout.fragment_archivereplace_confirmation, container, false);
        TextView confirmation_message_textview = v.findViewById(R.id.dialog_fragment_archive_replace_message);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_archive_replace_confirmation_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button yes_button = buttons_layout.findViewById(R.id.first_button);
        yes_button.setText(R.string.yes);
        Button no_button = buttons_layout.findViewById(R.id.second_button);
        no_button.setText(R.string.no);
        confirmation_message_textview.setText(message);

        yes_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getParentFragmentManager().setFragmentResult(request_code, bundle);
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
        // TODO: Implement this method
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
