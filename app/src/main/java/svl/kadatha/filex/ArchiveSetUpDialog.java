package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.asynctasks.ArchiveAsyncTask;
import svl.kadatha.filex.asynctasks.UnarchiveAsyncTask;


public class ArchiveSetUpDialog extends DialogFragment {
    public final static String ARCHIVE_ACTION_ZIP = ArchiveAsyncTask.TASK_TYPE;
    public final static String ARCHIVE_ACTION_UNZIP = UnarchiveAsyncTask.TASK_TYPE;
    private final static String ARCHIVE_REPLACE_REQUEST_CODE = "archive_replace_request_code";
    private final static String SAF_PERMISSION_REQUEST_CODE = "archive_set_up_saf_permission_request_code";
    private final ArrayList<String> files_selected_array = new ArrayList<>();
    private final ArrayList<String> zipentry_selected_array = new ArrayList<>();
    private Context context;
    private CheckBox create_folder_checkbox;
    private EditText zip_file_edittext, customdir_edittext;
    private RadioButton rb_current_dir, rb_custom_dir;
    private Button browsebutton;
    private Button okbutton;
    private String zip_file_path = "";
    private String archive_action;
    private String tree_uri_path = "";
    private Uri tree_uri;
    private FileObjectType sourceFileObjectType;
    private FileObjectType current_dir_fileObjectType, destFileObjectType;
    private InputMethodManager imm;
    private String first_file_name, parent_file_name, parent_file_path;
    private Class emptyService;
    private ArchiveSetUpViewModel viewModel;
    private FrameLayout progress_bar;
    private TextView destFileObjectType_text_view;
    private final ActivityResultLauncher<Intent> activityResultLauncher_file_select = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                viewModel.folderclickselected = intent.getStringExtra("folderclickselected");
                viewModel.custom_dir_fileObjectType = (FileObjectType) intent.getSerializableExtra("destFileObjectType");
                customdir_edittext.setText(viewModel.folderclickselected);
                destFileObjectType_text_view.setText(getDestFileObjectType());
            }
        }
    });

    public static ArchiveSetUpDialog getInstance(ArrayList<String> files_selected_array, ArrayList<String> zipentry_selected_array, FileObjectType sourceFileObjectType, String archive_action) {
        ArchiveSetUpDialog archiveSetUpDialog = new ArchiveSetUpDialog();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("files_selected_array", files_selected_array);
        bundle.putStringArrayList("zipentry_selected_array", zipentry_selected_array);
        bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
        bundle.putString("archive_action", archive_action);
        archiveSetUpDialog.setArguments(bundle);
        return archiveSetUpDialog;
    }

    public static boolean isFilePathDirectory(String file_path, FileObjectType fileObjectType, List<FilePOJO> filePOJOs) {
        if ((fileObjectType == FileObjectType.FILE_TYPE) || fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
            return new File(file_path).isDirectory();
        } else {
            File f = new File(file_path);
            String file_name = f.getName();
            if (filePOJOs != null) {
                if (filePOJOs.isEmpty()) return true; //folder is blank, so folder can be created
                for (FilePOJO filePOJO : filePOJOs) {
                    if (filePOJO.getName().equals(file_name)) {
                        return filePOJO.getIsDirectory();
                    }
                }
            } else {
                return true;
            }
            return true;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle bundle = getArguments();
        if (bundle != null) {
            files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
            if (bundle.getStringArrayList("zipentry_selected_array") != null) {
                zipentry_selected_array.addAll(bundle.getStringArrayList("zipentry_selected_array"));
            }
            sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
            archive_action = bundle.getString("archive_action");
            String first_file_path = files_selected_array.get(0);
            first_file_name = new File(first_file_path).getName();
            if (sourceFileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                parent_file_path = Global.INTERNAL_PRIMARY_STORAGE_PATH;
                current_dir_fileObjectType = FileObjectType.FILE_TYPE;
            } else {
                current_dir_fileObjectType = sourceFileObjectType;
                parent_file_path = getParentFilePath(first_file_path);
            }

            String s = new File(parent_file_path).getName();
            parent_file_name = s.equals("0") ? first_file_name : s;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View zipdialogview = inflater.inflate(R.layout.fragment_archive_setup, container, false);
        TextView dialog_heading = zipdialogview.findViewById(R.id.dialog_archive_heading);
        TextView outputfilename = zipdialogview.findViewById(R.id.dialog_archive_outputfilename);
        TextView zip_name_suffix = zipdialogview.findViewById(R.id.dialog_archive_textview_zip_suffix);
        if (archive_action.equals(ARCHIVE_ACTION_UNZIP)) zip_name_suffix.setVisibility(View.GONE);
        create_folder_checkbox = zipdialogview.findViewById(R.id.dialog_archive_checkbox);
        zip_file_edittext = zipdialogview.findViewById(R.id.dialog_archive_textview_zipname);
        destFileObjectType_text_view = zipdialogview.findViewById(R.id.dialog_archive_destination_file_object_type);
        RadioGroup rg = zipdialogview.findViewById(R.id.dialog_archive_rg);
        rb_current_dir = zipdialogview.findViewById(R.id.dialog_archive_rb_current_dir);
        rb_custom_dir = zipdialogview.findViewById(R.id.dialog_archive_rb_custom_dir);
        customdir_edittext = zipdialogview.findViewById(R.id.dialog_archive_edittext_customdir);
        browsebutton = zipdialogview.findViewById(R.id.dialog_archive_browse_button);
        progress_bar = zipdialogview.findViewById(R.id.fragment_archive_setup_progressbar);
        progress_bar.setVisibility(View.GONE);
        ViewGroup buttons_layout = zipdialogview.findViewById(R.id.fragment_archive_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        okbutton = buttons_layout.findViewById(R.id.first_button);
        okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
        cancelbutton.setText(R.string.cancel);
        browsebutton.setVisibility(View.GONE);
        customdir_edittext.setVisibility(View.GONE);

        rb_current_dir.setText(parent_file_path);
        rb_custom_dir.setText(R.string.choose_directory);
        rg.check(rb_current_dir.getId());
        zip_file_path = files_selected_array.get(0);

        viewModel = new ViewModelProvider(this).get(ArchiveSetUpViewModel.class);

        viewModel.isRecursiveFilesRemoved.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    viewModel.isRecursiveFilesRemoved.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    if (viewModel.files_selected_array.isEmpty()) {
                        Global.print(context, getString(R.string.could_not_perform_action));
                        dismissAllowingStateLoss();
                        return;
                    }

                    String zip_folder_name = zip_file_edittext.getText().toString().trim();
                    if (zip_folder_name.isEmpty()) {
                        Global.print(context, getString(R.string.enter_zip_file_name));
                        return;
                    }
                    if (CheckString.whetherStringContainsSpecialCharacters(zip_folder_name)) {
                        Global.print(context, getString(R.string.avoid_name_involving_special_characters));
                        return;
                    }

                    String archivedestfolder = rb_current_dir.isChecked() ? rb_current_dir.getText().toString() : customdir_edittext.getText().toString();
                    destFileObjectType = rb_current_dir.isChecked() ? current_dir_fileObjectType : viewModel.custom_dir_fileObjectType;
                    final String zip_folder_path = Global.CONCATENATE_PARENT_CHILD_PATH(archivedestfolder, zip_folder_name);
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                    viewModel.destFilePOJOs = repositoryClass.hashmap_file_pojo.get(destFileObjectType + archivedestfolder);

                    if (!is_file_writable(archivedestfolder, destFileObjectType)) {
                        return;
                    }

                    emptyService = ArchiveDeletePasteServiceUtil.getEmptyService(context);
                    if (emptyService == null) {
                        Global.print(context, getString(R.string.maximum_3_services_processed));
                        return;
                    }

                    final Bundle bundle = new Bundle();
                    bundle.putStringArrayList("files_selected_array", viewModel.files_selected_array);
                    bundle.putString("dest_folder", archivedestfolder);
                    bundle.putString("zip_file_path", zip_file_path);
                    bundle.putString("zip_folder_name", zip_folder_name);
                    bundle.putString("archive_action", archive_action);
                    bundle.putString("tree_uri_path", tree_uri_path);
                    bundle.putParcelable("tree_uri", tree_uri);
                    bundle.putString("source_folder", parent_file_path);
                    bundle.putString("zip_folder_path", zip_folder_path);
                    bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
                    bundle.putSerializable("destFileObjectType", destFileObjectType);

                    if (Global.WHETHER_FILE_ALREADY_EXISTS(destFileObjectType, zip_folder_path + ".zip", viewModel.destFilePOJOs)) {
                        if (isFilePathDirectory(zip_folder_path + ".zip", destFileObjectType, viewModel.destFilePOJOs)) {
                            Global.print(context, getString(R.string.a_directory_with_output_file_name_already_exists) + " '" + zip_folder_name + "'");

                        } else {
                            ArchiveReplaceConfirmationDialog archiveReplaceConfirmationDialog = ArchiveReplaceConfirmationDialog.getInstance(ARCHIVE_REPLACE_REQUEST_CODE, bundle);
                            archiveReplaceConfirmationDialog.show(getParentFragmentManager(), null);

                        }
                    } else {
                        Intent intent = new Intent(context, emptyService);
                        intent.setAction(ARCHIVE_ACTION_ZIP);
                        intent.putExtra("bundle", bundle);
                        context.startActivity(intent);
                        imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
                        dismissAllowingStateLoss();
                    }
                }
            }
        });

        customdir_edittext.setText(viewModel.folderclickselected);
        create_folder_checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton p1, boolean p2) {
                if (p1.isChecked()) {
                    zip_file_edittext.setEnabled(true);
                    zip_file_edittext.setAlpha(Global.ENABLE_ALFA);
                } else {
                    zip_file_edittext.setEnabled(false);
                    zip_file_edittext.setAlpha(Global.DISABLE_ALFA);
                }
            }
        });

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup rg, int pos) {
                if (rb_custom_dir.isChecked()) {
                    customdir_edittext.setVisibility(View.VISIBLE);
                    browsebutton.setVisibility(View.VISIBLE);
                } else {
                    customdir_edittext.setVisibility(View.GONE);
                    browsebutton.setVisibility(View.GONE);
                }
                destFileObjectType_text_view.setText(getDestFileObjectType());
            }
        });

        browsebutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (rb_custom_dir.isChecked()) {
                    AppCompatActivity appCompatActivity = (AppCompatActivity) context;
                    if (appCompatActivity instanceof MainActivity) {
                        ((MainActivity) context).clear_cache = false;
                    } else if (appCompatActivity instanceof ArchiveViewActivity) {
                        ((ArchiveViewActivity) context).clear_cache = false;
                    }

                    Intent intent = new Intent(context, FileSelectorActivity.class);
                    intent.putExtra(FileSelectorActivity.ACTION_SOUGHT, FileSelectorActivity.FOLDER_SELECT_REQUEST_CODE);
                    activityResultLauncher_file_select.launch(intent);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(ARCHIVE_REPLACE_REQUEST_CODE, ArchiveSetUpDialog.this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(ARCHIVE_REPLACE_REQUEST_CODE)) {
                    String archive_action = result.getString("archive_action");
                    emptyService = ArchiveDeletePasteServiceUtil.getEmptyService(context);
                    if (archive_action.equals(ARCHIVE_ACTION_ZIP)) {
                        String zip_folder_path = result.getString("zip_folder_path");
                        viewModel.files_selected_array.remove(zip_folder_path + ".zip");
                        result.putStringArrayList("files_selected_array", viewModel.files_selected_array);
                        Intent intent = new Intent(context, emptyService);
                        intent.setAction(ARCHIVE_ACTION_ZIP);
                        intent.putExtra("bundle", result);
                        context.startActivity(intent);
                        imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
                        dismissAllowingStateLoss();
                    } else if (archive_action.equals(ARCHIVE_ACTION_UNZIP)) {
                        Intent intent = new Intent(context, emptyService);
                        intent.setAction(ARCHIVE_ACTION_UNZIP);
                        intent.putExtra("bundle", result);
                        context.startActivity(intent);
                        imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
                        dismissAllowingStateLoss();
                    }

                }
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


        switch (archive_action) {
            case ARCHIVE_ACTION_ZIP: {
                create_folder_checkbox.setVisibility(View.GONE);
                dialog_heading.setText(R.string.archive);
                outputfilename.setText(R.string.output_file);
                if (files_selected_array.size() == 1) {
                    zip_file_edittext.setText(first_file_name);
                } else {
                    zip_file_edittext.setText(parent_file_name);
                }
                zip_file_edittext.setSelection(zip_file_edittext.getText().length());

                okbutton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (progress_bar.getVisibility() == View.VISIBLE) {
                            Global.print(context, getString(R.string.please_wait));
                            return;
                        }
                        destFileObjectType = rb_current_dir.isChecked() ? current_dir_fileObjectType : viewModel.custom_dir_fileObjectType;
                        if (destFileObjectType == FileObjectType.USB_TYPE) {
                            Global.print(context, getString(R.string.not_supported));
                            return;
                        }

                        if (sourceFileObjectType == FileObjectType.ROOT_TYPE || destFileObjectType == FileObjectType.ROOT_TYPE) {
                            if (!RootUtils.canRunRootCommands()) {
                                Global.print(context, getString(R.string.root_access_not_avaialable));
                                return;
                            }
                        }

                        if (!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(null, destFileObjectType)) {
                            Global.print(context, context.getString(R.string.wait_till_completion_on_going_operation_on_usb));
                            return;
                        }

                        progress_bar.setVisibility(View.VISIBLE);
                        String archivedestfolder = rb_current_dir.isChecked() ? rb_current_dir.getText().toString() : customdir_edittext.getText().toString();
                        viewModel.removeRecursiveFiles(files_selected_array, archivedestfolder, destFileObjectType, sourceFileObjectType);
                    }
                });
                break;
            }

            case ARCHIVE_ACTION_UNZIP: {
                dialog_heading.setText(R.string.extract);
                outputfilename.setText(R.string.output_folder);
                int ext_index = first_file_name.lastIndexOf(".");
                zip_file_edittext.setText(first_file_name.substring(0, ext_index));
                zip_file_edittext.setSelection(zip_file_edittext.getText().length());
                okbutton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (progress_bar.getVisibility() == View.VISIBLE) {
                            Global.print(context, getString(R.string.please_wait));
                            return;
                        }
                        destFileObjectType = rb_current_dir.isChecked() ? current_dir_fileObjectType : viewModel.custom_dir_fileObjectType;
                        if (destFileObjectType == FileObjectType.USB_TYPE || destFileObjectType == FileObjectType.FTP_TYPE || destFileObjectType == FileObjectType.SFTP_TYPE) {
                            Global.print(context, getString(R.string.not_supported));
                            return;
                        }

                        if (sourceFileObjectType == FileObjectType.ROOT_TYPE || destFileObjectType == FileObjectType.ROOT_TYPE) {
                            if (!RootUtils.canRunRootCommands()) {
                                Global.print(context, getString(R.string.root_access_not_avaialable));
                                return;
                            }
                        }

                        if (!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(null, destFileObjectType)) {
                            Global.print(context, context.getString(R.string.wait_till_completion_on_going_operation_on_usb));
                            return;
                        }
                        String zip_output_folder = zip_file_edittext.getText().toString().trim();
                        final Bundle bundle = new Bundle();
                        bundle.putStringArrayList("files_selected_array", files_selected_array);
                        bundle.putStringArrayList("zipentry_selected_array", zipentry_selected_array);
                        bundle.putString("zip_file_path", zip_file_path);
                        bundle.putString("archive_action", archive_action);
                        bundle.putBoolean("archive_view", false);
                        bundle.putString("source_folder", parent_file_path);
                        bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);

                        String zip_folder_path;
                        String unarchivedestfolder = rb_current_dir.isChecked() ? rb_current_dir.getText().toString() : customdir_edittext.getText().toString();

                        bundle.putSerializable("destFileObjectType", destFileObjectType); //put destfileobjecttype after deciding which one to put

                        if (create_folder_checkbox.isChecked()) {
                            if (zip_output_folder == null || zip_output_folder.isEmpty()) {
                                Global.print(context, getString(R.string.enter_output_folder_name));
                                return;
                            }

                            if (CheckString.whetherStringContainsSpecialCharacters(zip_output_folder)) {
                                Global.print(context, getString(R.string.avoid_name_involving_special_characters));
                                return;
                            }
                            zip_folder_path = Global.CONCATENATE_PARENT_CHILD_PATH(unarchivedestfolder, zip_output_folder);
                            RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                            viewModel.destFilePOJOs = repositoryClass.hashmap_file_pojo.get(destFileObjectType + unarchivedestfolder); //only if your crating new folder store unarchive files
                        } else {
                            zip_folder_path = unarchivedestfolder;
                            zip_output_folder = null;
                        }


                        if (!is_file_writable(unarchivedestfolder, destFileObjectType)) {
                            return;
                        }

                        emptyService = ArchiveDeletePasteServiceUtil.getEmptyService(context);
                        if (emptyService == null) {
                            Global.print(context, getString(R.string.maximum_3_services_processed));
                            return;
                        }

                        bundle.putString("tree_uri_path", tree_uri_path);
                        bundle.putParcelable("tree_uri", tree_uri);
                        bundle.putString("dest_folder", unarchivedestfolder);
                        bundle.putString("zip_folder_name", zip_output_folder);
                        if (create_folder_checkbox.isChecked() && Global.WHETHER_FILE_ALREADY_EXISTS(destFileObjectType, zip_folder_path, viewModel.destFilePOJOs)) {
                            if (isFilePathDirectory(zip_folder_path, destFileObjectType, viewModel.destFilePOJOs)) {
                                ArchiveReplaceConfirmationDialog archiveReplaceConfirmationDialog = ArchiveReplaceConfirmationDialog.getInstance(ARCHIVE_REPLACE_REQUEST_CODE, bundle);
                                archiveReplaceConfirmationDialog.show(getParentFragmentManager(), null);

                            } else {
                                Global.print(context, getString(R.string.a_file_with_folder_name_exists_in_selected_directory));
                            }
                        } else {
                            Intent intent = new Intent(context, emptyService);
                            intent.setAction(ArchiveSetUpDialog.ARCHIVE_ACTION_UNZIP);
                            intent.putExtra("bundle", bundle);
                            context.startActivity(intent);
                            imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
                            dismissAllowingStateLoss();
                        }
                    }
                });
                break;
            }
        }

        cancelbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
                dismissAllowingStateLoss();
            }

        });
        destFileObjectType_text_view.setText(getDestFileObjectType());
        return zipdialogview;
    }

    private String getDestFileObjectType() {
        FileObjectType fileObjectType = rb_current_dir.isChecked() ? current_dir_fileObjectType : viewModel.custom_dir_fileObjectType;
        return Global.GET_FileObjectType(fileObjectType);
    }

    private String getParentFilePath(String file_path) {
        return new File(file_path).getParent();
    }

    private boolean check_SAF_permission(String parent_file_path, FileObjectType fileObjectType) {
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path, fileObjectType);
        if (uriPOJO != null) {
            tree_uri_path = uriPOJO.get_path();
            tree_uri = uriPOJO.get_uri();
        }

        if (uriPOJO == null || tree_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE, parent_file_path, fileObjectType);
            safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
            imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
            return false;
        } else {
            return true;
        }
    }

    private boolean is_file_writable(String file_path, FileObjectType fileObjectType) {
        if (fileObjectType == FileObjectType.FILE_TYPE) {
            boolean isWritable;
            isWritable = FileUtil.isWritable(fileObjectType, file_path);
            if (isWritable) {
                return true;
            } else {
                return check_SAF_permission(file_path, fileObjectType);
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
