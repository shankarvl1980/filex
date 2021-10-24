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
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Collections;
import java.util.HashSet;

public class FtpDetailsInputDialog extends DialogFragment {

    private Context context;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setReenterTransition(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_ftp_details_input,container,false);
        ViewGroup buttons_layout = v.findViewById(R.id.ftp_details_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
        ok_button.setText(R.string.ok);
        ok_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {


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

    @Override
    public void onDestroy() {
        if(getDialog()!=null && getRetainInstance())
        {
            getDialog().setDismissMessage(null);
        }
        super.onDestroy();
    }
}
