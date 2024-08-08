package svl.kadatha.filex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

public class PdfPasswordDialog extends DialogFragment {
    public static final String PASSWORD_REQUEST_CODE = "pdf_password_request_code";
    private EditText passwordEditText;

    public static PdfPasswordDialog newInstance() {
        return new PdfPasswordDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_password_dialog, container, false);
        passwordEditText = view.findViewById(R.id.dialog_fragment_pdf_password_input);
        ViewGroup buttons_layout = view.findViewById(R.id.fragment_pdf_password_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(getContext(), 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));

        Button okButton = buttons_layout.findViewById(R.id.first_button);
        okButton.setText(R.string.ok);
        Button cancelButton = buttons_layout.findViewById(R.id.second_button);
        cancelButton.setText(R.string.cancel);

        okButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            Bundle result = new Bundle();
            result.putString("password", password);
            getParentFragmentManager().setFragmentResult(PASSWORD_REQUEST_CODE, result);
            dismiss();
});

cancelButton.setOnClickListener(v -> dismiss());

        return view;
    }
}
