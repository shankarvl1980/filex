package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CopyToActivity extends BaseActivity{

    private Context context;
    private String tree_uri_path="";
    private Uri tree_uri;
    private FileObjectType destFileObjectType;
    private InputMethodManager imm;
    private EditText file_name_edit_text;
    private Button browse_button;
    public boolean clear_cache;
    private EditText destination_folder_edittext;
    private TextView destination_fileObject_text_view;
    private String folderclickselected;
    private List<Uri> data_list=new ArrayList<>();
    private final static String ARCHIVE_REPLACE_REQUEST_CODE="activity_copy_to_replace_request_code";
    private final static String SAF_PERMISSION_REQUEST_CODE="activity_copy_to_saf_permission_request_code";
    private final static String COPY_TO_ACTION="copy_to";
    private Class emptyService;
    private Button ok_button;
    private boolean first_start;
    private CopyToActivityViewModel viewModel;
    private FileDuplicationViewModel fileDuplicationViewModel;
    private final ArrayList<String> file_name_list=new ArrayList<>();
    private FrameLayout progress_bar;
    public final static String DUPLICATE_FILE_NAMES_REQUEST_CODE="copy_to_duplicate_file_names_request_code";
    private ArrayList<String> overwritten_file_path_list;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.activity_copy_to);
        setFinishOnTouchOutside(false);
        file_name_edit_text=findViewById(R.id.activity_copy_to_filename);
        destination_folder_edittext=findViewById(R.id.activity_copy_to_destination_folder);
        destination_fileObject_text_view=findViewById(R.id.activity_copy_to_destination_file_object_type);
        browse_button=findViewById(R.id.activity_copy_to_browse_button);
        browse_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                clear_cache=false;
                Intent intent=new Intent(context,FileSelectorActivity.class);
                intent.putExtra(FileSelectorActivity.ACTION_SOUGHT,FileSelectorActivity.FOLDER_SELECT_REQUEST_CODE);
                activityResultLauncher_file_select.launch(intent);
            }
        });

        progress_bar=findViewById(R.id.copy_to_progressbar);
        progress_bar.setVisibility(View.GONE);

        ViewGroup buttons_layout = findViewById(R.id.activity_copy_to_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(this,2,Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        ok_button = buttons_layout.findViewById(R.id.first_button);
        ok_button.setText(R.string.ok);
        Button cancel_button = buttons_layout.findViewById(R.id.second_button);
        cancel_button.setText(R.string.cancel);

        viewModel=new ViewModelProvider(this).get(CopyToActivityViewModel.class);
        fileDuplicationViewModel=new ViewModelProvider(this).get(FileDuplicationViewModel.class);
        fileDuplicationViewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                }

                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    if(fileDuplicationViewModel.sourceFileObjectType==FileObjectType.FTP_TYPE && fileDuplicationViewModel.destFileObjectType==FileObjectType.FTP_TYPE)
                    {
                        Global.print(context,context.getString(R.string.not_supported));
                    }
                    else if(fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty())
                    {
                        overwritten_file_path_list=fileDuplicationViewModel.overwritten_file_path_list;
                        launchService();
                    }
                    else
                    {
                        FileReplaceConfirmationDialog fileReplaceConfirmationDialog = FileReplaceConfirmationDialog.getInstance(fileDuplicationViewModel.source_folder,fileDuplicationViewModel.sourceFileObjectType,
                                fileDuplicationViewModel.dest_folder,fileDuplicationViewModel.destFileObjectType,fileDuplicationViewModel.files_selected_array,data_list,fileDuplicationViewModel.cut);
                        fileReplaceConfirmationDialog.show(getSupportFragmentManager(), "paste_dialog");
                    }
                    fileDuplicationViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);

                }
            }
        });
        ok_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {

                if(data_list==null)
                {
                    Global.print(context,getString(R.string.could_not_perform_action)+" - "+"Uri is null");
                    return;
                }

                if(data_list.size()!=1){
                    file_name_edit_text.setText("");
                    file_name_edit_text.setEnabled(false);
                }
//                else {
//                    file_name_edit_text.setEnabled(true);
//                }

                String file_name=file_name_edit_text.getText().toString().trim();
                if(CheckString.whetherStringContainsSpecialCharacters(file_name))
                {
                    Global.print(context,getString(R.string.avoid_name_involving_special_characters));
                    return;
                }


                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                viewModel.destFilePOJOs=repositoryClass.hashmap_file_pojo.get(destFileObjectType+folderclickselected);
                final String full_path=Global.CONCATENATE_PARENT_CHILD_PATH(folderclickselected,file_name);

                if(!is_file_writable(folderclickselected,destFileObjectType))
                {
                    return;
                }

                if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(null,destFileObjectType))
                {
                    Global.print(context,getString(R.string.wait_till_completion_on_going_operation_on_usb));
                    return;
                }

                if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_FTP(null,destFileObjectType))
                {
                    Global.print(context,getString(R.string.wait_till_current_service_on_ftp_finishes));
                    return;
                }

                emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
                if(emptyService==null)
                {
                    Global.print(context,getString(R.string.maximum_3_services_processed));
                    return;
                }
                progress_bar.setVisibility(View.VISIBLE);
                fileDuplicationViewModel.checkForExistingFileWithSameName("",FileObjectType.SEARCH_LIBRARY_TYPE,folderclickselected,destFileObjectType,file_name_list,false,false);
            }

        });

        cancel_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                imm.hideSoftInputFromWindow(file_name_edit_text.getWindowToken(),0);
                finish();
            }
        });

        if(savedInstanceState==null)first_start=true;
        Intent intent=getIntent();
        try {
            on_intent(intent,savedInstanceState);
        } catch (Exception e) {
            Global.print(context,getString(R.string.could_not_perform_action));
        }
        imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

        getSupportFragmentManager().setFragmentResultListener(DUPLICATE_FILE_NAMES_REQUEST_CODE, CopyToActivity.this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(DUPLICATE_FILE_NAMES_REQUEST_CODE))
                {
                    overwritten_file_path_list=result.getStringArrayList("overwritten_file_path_list");
                    data_list=result.getParcelableArrayList("data_list");
                    launchService();
                }
            }
        });


//        getSupportFragmentManager().setFragmentResultListener(ARCHIVE_REPLACE_REQUEST_CODE, CopyToActivity.this, new FragmentResultListener() {
//            @Override
//            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
//                if(requestKey.equals(ARCHIVE_REPLACE_REQUEST_CODE))
//                {
//                    emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
//                    if(emptyService==null)
//                    {
//                        Global.print(context,getString(R.string.maximum_3_services_processed));
//                        return;
//                    }
//                    Intent intent=new Intent(context,emptyService);
//                    intent.setAction(COPY_TO_ACTION);
//                    intent.putExtra("bundle",result);
//                    context.startActivity(intent);
//                    imm.hideSoftInputFromWindow(file_name_edit_text.getWindowToken(), 0);
//                    finish();
//                }
//            }
//        });

        getSupportFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE))
                {
                    tree_uri=result.getParcelable("tree_uri");
                    tree_uri_path=result.getString("tree_uri_path");
                    ok_button.callOnClick();
                }
            }
        });
    }

    private void launchService(){
        emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
        if(emptyService==null)
        {
            Global.print(context,getString(R.string.maximum_3_services_processed));
            return;
        }
        String file_name=file_name_edit_text.getText().toString().trim();
        Bundle bundle=new Bundle();
        bundle.putParcelableArrayList("data_list", (ArrayList<? extends Parcelable>) data_list);
        bundle.putStringArrayList("overwritten_file_path_list",overwritten_file_path_list);
        bundle.putString("dest_folder",folderclickselected);
        bundle.putString("file_name",file_name);
        bundle.putString("new_name",file_name);
        bundle.putString("tree_uri_path",tree_uri_path);
        bundle.putParcelable("tree_uri",tree_uri);
        bundle.putSerializable("destFileObjectType",destFileObjectType);

        Intent intent=new Intent(context,emptyService);
        intent.setAction(COPY_TO_ACTION);
        intent.putExtra("bundle",bundle);
        context.startActivity(intent);
        imm.hideSoftInputFromWindow(file_name_edit_text.getWindowToken(),0);
        finish();

    }

    @Override
    protected void onNewIntent(Intent intent)  {
        super.onNewIntent(intent);
        try {
            on_intent(intent,null);
        } catch (Exception e) {
            Global.print(context,getString(R.string.could_not_perform_action));
        }
    }

    @SuppressWarnings("RedundantThrows")
    private void on_intent(Intent intent, Bundle savedInstanceState) throws Exception
    {
        if(intent!=null)
        {
            Bundle bundle = intent.getExtras();
            String action=intent.getAction();
            if(action.equals(Intent.ACTION_SEND_MULTIPLE))
            {
                data_list = (List<Uri>) bundle.get(Intent.EXTRA_STREAM);
            }
            else if(action.equals(Intent.ACTION_SEND))
            {
                data_list.add((Uri)bundle.get(Intent.EXTRA_STREAM));
            }

            for(Uri data:data_list){
                file_name_list.add(getFileNameOfUri(context,data));
            }

            if(savedInstanceState==null)
            {
                if(data_list!=null && !data_list.isEmpty())
                {
                    if(data_list.size()==1){
                        String f_name=file_name_list.get(0);
                        file_name_edit_text.setText(f_name==null ? "" : f_name);
                    }
                    else {
                        file_name_edit_text.setEnabled(false);
                    }

                    browse_button.callOnClick();
                }

            }
        }
    }

    public static String getFileNameOfUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(index);
                }
            }
            catch (Exception e){
                result=uri.getLastPathSegment();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    @Override
    protected void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        getWindow().setLayout(Global.DIALOG_WIDTH, AbsListView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onStart()
    {
        // TODO: Implement this method
        super.onStart();
        if(first_start)
        {
                first_start=false;
        }
        else {
            clear_cache=true;
        }

        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isFinishing() && !isChangingConfigurations() && clear_cache)
        {
            clearCache();
        }
    }

    public void clearCache()
    {
        Global.CLEAR_CACHE();
    }

    public void clearCache(String file_path, FileObjectType fileObjectType)
    {
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_path),fileObjectType); //no need of broad cast here, as the method includes broadcast
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("folderclickselected",folderclickselected);
        outState.putSerializable("destFileObjectType",destFileObjectType);
        outState.putBoolean("clear_cache",clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        folderclickselected=savedInstanceState.getString("folderclickselected");
        destFileObjectType= (FileObjectType) savedInstanceState.getSerializable("destFileObjectType");
        clear_cache=savedInstanceState.getBoolean("clear_cache");
    }

    private boolean check_SAF_permission(String parent_file_path, FileObjectType fileObjectType)
    {
        UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path,fileObjectType);
        if(uriPOJO!=null)
        {
            tree_uri_path=uriPOJO.get_path();
            tree_uri=uriPOJO.get_uri();
        }

        if(uriPOJO==null || tree_uri_path.equals(""))
        {
            SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,parent_file_path,fileObjectType);
            safpermissionhelper.show(getSupportFragmentManager(),"saf_permission_dialog");
            imm.hideSoftInputFromWindow(file_name_edit_text.getWindowToken(),0);
            return false;
        }
        else
        {
            return true;
        }
    }

    private boolean is_file_writable(String file_path,FileObjectType fileObjectType)  //copied from ArchiveSetUpDialog
    {
        if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            boolean isWritable;
            isWritable=FileUtil.isWritable(fileObjectType,file_path);
            if(isWritable)
            {
                return true;
            }
            else
            {
                return check_SAF_permission(file_path,fileObjectType);
            }
        }
        return true;

    }

    private final ActivityResultLauncher<Intent> activityResultLauncher_file_select=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK)
            {
                Intent intent=result.getData();
                folderclickselected = intent.getStringExtra("folderclickselected");
                destFileObjectType = (FileObjectType) intent.getSerializableExtra("destFileObjectType");
                destination_folder_edittext.setText(folderclickselected);
                destination_fileObject_text_view.setText(Global.GET_FileObjectType(destFileObjectType));
            }
        }
    });
}
