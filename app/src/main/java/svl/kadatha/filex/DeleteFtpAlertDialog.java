package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.util.ArrayList;

public class DeleteFtpAlertDialog extends DialogFragment {

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
        okButtonClickListener= (DeleteFileAlertDialog.OKButtonClickListener) context;
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
        bundle=getArguments();

        if(bundle!=null)
        {
            files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
            sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
            source_folder=bundle.getString("source_folder");
            boolean storage_analyser_delete = bundle.getBoolean("storage_analyser_delete");
            size=files_selected_array.size();
            fileCountSize=new DeleteFileAlertDialog.FileCountSize(files_selected_array,sourceFileObjectType);
            fileCountSize.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        other_file_permission=Global.GET_OTHER_FILE_PERMISSION(source_folder);
    }

    public static DeleteFileAlertDialog getInstance(ArrayList<String> files_selected_array, FileObjectType sourceFileObjectType, String source_folder, boolean storage_analyser_delete )
    {
        DeleteFileAlertDialog deleteFileAlertDialog=new DeleteFileAlertDialog();
        Bundle bundle=new Bundle();
        bundle.putStringArrayList("files_selected_array",files_selected_array);
        bundle.putSerializable("sourceFileObjectType",sourceFileObjectType);
        bundle.putString("source_folder",source_folder);
        bundle.putBoolean("storage_analyser_delete",storage_analyser_delete);
        deleteFileAlertDialog.setArguments(bundle);
        return deleteFileAlertDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method
        View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
        if(files_selected_array.size()==1)
        {
            dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_the_selected_file)+" '"+new File(files_selected_array.get(0)).getName()+"'");
        }
        else
        {
            dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_the_selected_files)+" "+files_selected_array.size()+" "+getString(R.string.files));
        }

        EditText new_file_name_edittext = v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        no_files_textview=v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        size_files_textview=v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        okbutton = buttons_layout.findViewById(R.id.first_button);
        okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
        cancelbutton.setText(R.string.cancel);
        dialog_heading_textview.setText(R.string.delete);
        new_file_name_edittext.setVisibility(View.GONE);

        okbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if(fileCountSize!=null)
                {
                    fileCountSize.cancel(true);
                }
                if(sourceFileObjectType== FileObjectType.FILE_TYPE)
                {
                    String file_path=files_selected_array.get(0);
                    if(!FileUtil.isWritable(sourceFileObjectType,file_path))
                    {
                        if (!check_SAF_permission(file_path, sourceFileObjectType)) return;

                    }
                    Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
                    if(emptyService==null)
                    {
                        print(getString(R.string.maximum_3_services_processed));
                        return;
                    }
                    start_delete_progress_activity(emptyService);

                }
                else if(sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
                {
                    for(int i=0;i<size;++i)
                    {
                        String file_path=files_selected_array.get(i);
                        if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,file_path))
                        {
                            whether_native_file_exists=true;
                        }
                        else
                        {
                            if (!check_SAF_permission(file_path, FileObjectType.FILE_TYPE)) return;
                        }

                    }

                    Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
                    if(emptyService==null)
                    {
                        print(getString(R.string.maximum_3_services_processed));
                        return;
                    }
                    start_delete_progress_activity(emptyService);
                }
                else if(sourceFileObjectType== FileObjectType.USB_TYPE)
                {
                    Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
                    if(emptyService==null)
                    {
                        print(getString(R.string.maximum_3_services_processed));
                        return;
                    }
                    start_delete_progress_activity(emptyService);
                }
                else if(sourceFileObjectType==FileObjectType.ROOT_TYPE)
                {
                    if(RootUtils.CAN_RUN_ROOT_COMMANDS())
                    {
                        Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
                        if(emptyService==null)
                        {
                            print(getString(R.string.maximum_3_services_processed));
                            return;
                        }
                        start_delete_progress_activity(emptyService);
                    }
                    else
                    {
                        print(getString(R.string.root_access_not_avaialable));
                        return;
                    }
                }
                else if(sourceFileObjectType== FileObjectType.FTP_TYPE)
                {
                    if(!Global.CHECK_FTP_SERVER_CONNECTED())
                    {
                        print(getString(R.string.ftp_server_is_not_connected));
                        return;
                    }
                    Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
                    if(emptyService==null)
                    {
                        print(getString(R.string.maximum_3_services_processed));
                        return;
                    }
                    start_delete_progress_activity(emptyService);
                }
                if(okButtonClickListener!=null) okButtonClickListener.deleteDialogOKButtonClick();
                dismissAllowingStateLoss();
            }

        });

        cancelbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if(fileCountSize!=null)
                {
                    fileCountSize.cancel(true);
                }
                dismissAllowingStateLoss();
            }
        });

        if(savedInstanceState!=null)
        {
            no_files_textview.setText(getString(R.string.total_files_colon)+" "+total_no_of_files);
            size_files_textview.setText(getString(R.string.size_colon)+" "+size_of_files_to_be_deleted);
        }
        return v;
    }


    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, AbsListView.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

}
