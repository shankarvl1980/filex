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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;


public class CleanStorageDialog extends DialogFragment {
    private Context context;
    private DetailFragmentListener detailFragmentListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        AppCompatActivity activity = (AppCompatActivity) context;
        if (activity instanceof DetailFragmentListener) {
            detailFragmentListener = (DetailFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        detailFragmentListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_clean_storage, container, false);
        Button large_files_btn = v.findViewById(R.id.clean_storage_large_files_button);
        large_files_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailFragmentListener != null) {
                    detailFragmentListener.createFragmentTransaction("Large Files", FileObjectType.SEARCH_LIBRARY_TYPE);
                }
                dismissAllowingStateLoss();
            }
        });

        Button large_files_scan_btn = v.findViewById(R.id.clean_storage_large_files_rescan_button);
        large_files_scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailFragmentListener != null) {
                    detailFragmentListener.rescanLargeDuplicateFilesLibrary("large");
                }
            }
        });


        Button duplicate_files_btn = v.findViewById(R.id.clean_storage_duplicate_files_button);
        duplicate_files_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailFragmentListener != null) {
                    detailFragmentListener.createFragmentTransaction("Duplicate Files", FileObjectType.SEARCH_LIBRARY_TYPE);
                }
                dismissAllowingStateLoss();
            }
        });

        Button duplicate_files_scan_btn = v.findViewById(R.id.clean_storage_duplicate_files_rescan_button);
        duplicate_files_scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailFragmentListener != null) {
                    detailFragmentListener.rescanLargeDuplicateFilesLibrary("duplicate");
                }
            }
        });


        ViewGroup buttons_layout = v.findViewById(R.id.fragment_clean_storage_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button close_button = buttons_layout.findViewById(R.id.first_button);
        close_button.setText(R.string.close);
        close_button.setOnClickListener(new View.OnClickListener() {
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


