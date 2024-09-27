package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;

public class DeleteFileAlertDialogOtherActivity extends DialogFragment {
    private final static String SAF_PERMISSION_REQUEST_CODE = "delete_file_other_saf_permission_request_code";
    public String tree_uri_path = "";
    public Uri tree_uri;
    private TextView no_files_textview;
    private TextView size_files_textview;
    private ArrayList<String> files_selected_array = new ArrayList<>();
    private int total_no_of_files;
    private String size_of_files_to_be_deleted;
    private Context context;
    private FileObjectType fileObjectType;
    private int size;
    private Button okbutton;
    private String request_code;
    private Bundle bundle;
    private String source_folder;

    public static DeleteFileAlertDialogOtherActivity getInstance(String request_code, ArrayList<String> files_selected_array, FileObjectType fileObjectType) {
        DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = new DeleteFileAlertDialogOtherActivity();
        Bundle bundle = new Bundle();
        bundle.putString("request_code", request_code);
        bundle.putStringArrayList("files_selected_array", files_selected_array);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE, fileObjectType);
        deleteFileAlertDialogOtherActivity.setArguments(bundle);
        return deleteFileAlertDialogOtherActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        bundle = getArguments();
        request_code = bundle.getString("request_code");
        files_selected_array = bundle.getStringArrayList("files_selected_array");
        fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
        size = files_selected_array.size();
        source_folder = new File(files_selected_array.get(0)).getParent();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        View v = inflater.inflate(R.layout.fragment_create_rename_delete, container, false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
        if (files_selected_array.size() == 1) {
            dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_the_selected_file) + " '" + new File(files_selected_array.get(0)).getName() + "'");
        } else {
            dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_the_selected_files) + " " + files_selected_array.size() + " " + getString(R.string.files));
        }

        EditText new_file_name_edittext = v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        no_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        size_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        okbutton = buttons_layout.findViewById(R.id.first_button);
        okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
        cancelbutton.setText(R.string.cancel);
        dialog_heading_textview.setText(R.string.delete);
        new_file_name_edittext.setVisibility(View.GONE);

        ViewModelFileCount.ViewModelFileCountFactory factory = new ViewModelFileCount.ViewModelFileCountFactory(context, files_selected_array, fileObjectType);
        ViewModelFileCount viewModel = new ViewModelProvider(this, factory).get(ViewModelFileCount.class);
        //viewModel.countFile(files_selected_array.get(0),fileObjectType,files_selected_array,size,true);

        viewModel.total_no_of_files.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                no_files_textview.setText(getString(R.string.total_files) + " " + integer);
            }
        });

        viewModel.size_of_files_formatted.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {

                size_files_textview.setText(getString(R.string.size) + " " + s);
            }
        });


        okbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(fileObjectType, null)) {
                    Global.print(context, context.getString(R.string.wait_till_completion_on_going_operation_on_usb));
                    return;
                }

                if (fileObjectType == FileObjectType.FILE_TYPE) {
                    String file_path = files_selected_array.get(0);
                    if (!FileUtil.isWritable(fileObjectType, file_path)) {
                        if (!check_SAF_permission(file_path, fileObjectType)) return;
                    }
                } else if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                    for (int i = 0; i < size; ++i) {
                        String file_path = files_selected_array.get(i);
                        if (!FileUtil.isFromInternal(FileObjectType.FILE_TYPE, file_path)) {
                            if (!check_SAF_permission(file_path, FileObjectType.FILE_TYPE)) return;
                        }
                    }

                }

                bundle.putParcelable("tree_uri", tree_uri);
                bundle.putString("tree_uri_path", tree_uri_path);
                bundle.putString("source_folder", source_folder);
                getParentFragmentManager().setFragmentResult(request_code, bundle);
                dismissAllowingStateLoss();

            }

        });

        cancelbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }

        });


        getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE)) {
                    tree_uri = result.getParcelable("tree_uri");
                    tree_uri_path = result.getString("tree_uri_path");
                    okbutton.callOnClick();
                }
            }
        });


        if (savedInstanceState != null) {
            no_files_textview.setText(getString(R.string.total_files) + " " + total_no_of_files);
            size_files_textview.setText(getString(R.string.size) + " " + size_of_files_to_be_deleted);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private boolean check_SAF_permission(String file_path, FileObjectType fileObjectType) {
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path, fileObjectType);
        if (uriPOJO != null) {
            tree_uri_path = uriPOJO.get_path();
            tree_uri = uriPOJO.get_uri();
        }

        if (uriPOJO == null || tree_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE, file_path, fileObjectType);
            safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
            return false;
        } else {
            return true;
        }
    }
}
