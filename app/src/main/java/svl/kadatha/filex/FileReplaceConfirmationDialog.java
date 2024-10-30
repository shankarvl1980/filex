package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileReplaceConfirmationDialog extends DialogFragment {
    FileDuplicationViewModel fileDuplicationViewModel;
    ArrayList<String> files_selected_array;
    ArrayList<Uri> data_list;
    private CheckBox apply_all_checkbox;
    private Context context;
    private FrameLayout progress_bar;
    private boolean cut;
    private String source_folder;
    private String dest_folder;
    private FileObjectType sourceFileObjectType, destFileObjectType;
    private String file_name;

    public static FileReplaceConfirmationDialog getInstance(String source_folder, FileObjectType sourceFileObjectType, String dest_folder, FileObjectType destFileObjectType,
                                                            ArrayList<String> files_selected_array, List<Uri> data_list,String file_name, boolean cut_selected) {
        FileReplaceConfirmationDialog fileReplaceConfirmationDialog = new FileReplaceConfirmationDialog();
        Bundle bundle = new Bundle();
        bundle.putString("source_folder", source_folder);
        bundle.putStringArrayList("files_selected_array", files_selected_array);
        bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
        bundle.putSerializable("destFileObjectType", destFileObjectType);
        bundle.putString("dest_folder", dest_folder);
        bundle.putParcelableArrayList("data_list", (ArrayList<? extends Parcelable>) data_list);
        bundle.putString("file_name",file_name);
        bundle.putBoolean("cut", cut_selected);
        fileReplaceConfirmationDialog.setArguments(bundle);
        return fileReplaceConfirmationDialog;
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

        Bundle bundle = getArguments();
        if (bundle != null) {
            files_selected_array = bundle.getStringArrayList("files_selected_array");
            cut = bundle.getBoolean("cut");
            source_folder = bundle.getString("source_folder");
            dest_folder = bundle.getString("dest_folder");
            sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
            destFileObjectType = (FileObjectType) bundle.getSerializable("destFileObjectType");
            data_list = bundle.getParcelableArrayList("data_list");
            file_name=bundle.getString("file_name");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_replace_confirmation, container, false);
        TextView confirmation_message_textview = v.findViewById(R.id.dialog_fragment_replace_message);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_replace_confirmation_button_layout);

        Button replace_button = null,rename_button,skip_button;

        if(sourceFileObjectType.equals(destFileObjectType) && source_folder.equals(dest_folder)){
            buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
            rename_button = buttons_layout.findViewById(R.id.first_button);
            rename_button.setText(R.string.rename);
            skip_button = buttons_layout.findViewById(R.id.second_button);
            skip_button.setText(R.string.skip);
        } else{
            buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 3, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
            replace_button = buttons_layout.findViewById(R.id.first_button);
            replace_button.setText(R.string.replace);
            rename_button = buttons_layout.findViewById(R.id.second_button);
            rename_button.setText(R.string.rename);
            skip_button = buttons_layout.findViewById(R.id.third_button);
            skip_button.setText(R.string.skip);
        }

        apply_all_checkbox = v.findViewById(R.id.dialog_fragment_apply_all_confirmation_CheckBox);
        progress_bar = v.findViewById(R.id.file_replacement_dialog_progressbar);

        if(replace_button!=null){
            replace_button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (progress_bar.getVisibility() == View.VISIBLE) {
                        Global.print(context, getString(R.string.please_wait));
                        return;
                    }

                    if (apply_all_checkbox.isChecked()) {
                        progress_bar.setVisibility(View.VISIBLE);
                        fileDuplicationViewModel.filterFileSelectedArray(context, FileOperationMode.REPLACE, true, data_list);
                    } else {
                        String replacing_file_path=fileDuplicationViewModel.source_duplicate_file_path_array.remove(0);
                        fileDuplicationViewModel.overwritten_file_path_list.add(fileDuplicationViewModel.destination_duplicate_file_path_array.remove(0));
                        fileDuplicationViewModel.sourceFileDestNameMap.put(replacing_file_path,new File(replacing_file_path).getName());
                        Uri uri=fileDuplicationViewModel.duplicateUriDestNameMap.removeAtIndex(0);
                        if(uri!=null) fileDuplicationViewModel.uriDestNameMap.put(uri,new File(replacing_file_path).getName());
                        if (fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty()) {
                            progress_bar.setVisibility(View.VISIBLE);
                            fileDuplicationViewModel.filterFileSelectedArray(context, FileOperationMode.REPLACE, false, data_list);
                        } else {
                            confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it) + " '" + new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName() + "'");
                        }
                    }
                }
            });

        }

        rename_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                if (apply_all_checkbox.isChecked()) {
                    progress_bar.setVisibility(View.VISIBLE);
                    fileDuplicationViewModel.filterFileSelectedArray(context, FileOperationMode.RENAME, true, data_list);
                } else {
                    fileDuplicationViewModel.source_duplicate_file_path_array.remove(0);
                    fileDuplicationViewModel.destination_duplicate_file_path_array.remove(0);
                    if (fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty()) {
                        progress_bar.setVisibility(View.VISIBLE);
                        fileDuplicationViewModel.filterFileSelectedArray(context, FileOperationMode.RENAME, false, data_list);
                    } else {
                        confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it) + " '" + new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName() + "'");
                    }
                }
            }
        });

        skip_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                if (apply_all_checkbox.isChecked()) {
                    progress_bar.setVisibility(View.VISIBLE);
                    fileDuplicationViewModel.filterFileSelectedArray(context, FileOperationMode.SKIP, true, data_list);
                } else {
                    fileDuplicationViewModel.not_to_be_replaced_files_path_array.add(fileDuplicationViewModel.source_duplicate_file_path_array.remove(0));
                    fileDuplicationViewModel.files_selected_array.removeAll(fileDuplicationViewModel.not_to_be_replaced_files_path_array);
                    removeNotTobeCopiedUris(context, data_list, fileDuplicationViewModel.not_to_be_replaced_files_path_array);
                    fileDuplicationViewModel.destination_duplicate_file_path_array.remove(0);
                    if (fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty()) {
                        progress_bar.setVisibility(View.VISIBLE);
                        fileDuplicationViewModel.filterFileSelectedArray(context, FileOperationMode.SKIP, false, data_list);
                    } else {
                        confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it) + " '" + new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName() + "'");
                    }
                }
            }
        });

        fileDuplicationViewModel = new ViewModelProvider(this).get(FileDuplicationViewModel.class);
        if (data_list != null && !data_list.isEmpty()) {
            fileDuplicationViewModel.checkForExistingFileWithSameNameUri(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,data_list,file_name,false,true);
        } else{
            fileDuplicationViewModel.checkForExistingFileWithSameName(source_folder, sourceFileObjectType, dest_folder, destFileObjectType, files_selected_array, cut, true);
        }

        fileDuplicationViewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty()) {
                        confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it) + " '" + new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName() + "'");
                    }
                }
            }
        });

        fileDuplicationViewModel.filterSelectedArrayAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (getActivity() instanceof CopyToActivity) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("uriDestNameMap", fileDuplicationViewModel.uriDestNameMap);
                        bundle.putStringArrayList("overwritten_file_path_list", fileDuplicationViewModel.overwritten_file_path_list);
                        getParentFragmentManager().setFragmentResult(CopyToActivity.DUPLICATE_FILE_NAMES_REQUEST_CODE, bundle);
                    } else {
                        PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder, sourceFileObjectType, dest_folder, destFileObjectType,
                                fileDuplicationViewModel.sourceFileDestNameMap, fileDuplicationViewModel.overwritten_file_path_list, cut);
                        pasteSetUpDialog.show(getParentFragmentManager(), "paste_dialog");
                    }
                    dismissAllowingStateLoss();
                }
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

    private static void removeNotTobeCopiedUris(Context context, List<Uri> data_list, List<String> file_path_list) {
        if (data_list == null || data_list.isEmpty() || file_path_list.isEmpty()) return;
        Iterator<Uri> iterator = data_list.iterator();
        while (iterator.hasNext()) {
            String name = CopyToActivity.getFileNameOfUri(context, iterator.next());
            for (String f_name : file_path_list) {
                if (name.equals(f_name)) {
                    iterator.remove();
                    break;
                }
            }
        }
    }
}
