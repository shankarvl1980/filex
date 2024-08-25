package svl.kadatha.filex;

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

import svl.kadatha.filex.asynctasks.DeleteAsyncTask;

public class DeleteFileAlertDialog extends DialogFragment
{

    private TextView no_files_textview;
    private TextView size_files_textview;
    private final ArrayList<String>files_selected_array=new ArrayList<>();
	private Context context;
    private String tree_uri_path="";
	private Uri tree_uri;
	private int size=0;
	private Bundle bundle;
	private FileObjectType sourceFileObjectType;
	private Button okbutton;
    private OKButtonClickListener okButtonClickListener;
	private String source_folder;
	private final static String SAF_PERMISSION_REQUEST_CODE="delete_file_saf_permission_request_code";

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		okButtonClickListener= (OKButtonClickListener) context;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		okButtonClickListener=null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
		bundle=getArguments();

		if(bundle!=null)
		{
			files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
			sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
			source_folder=bundle.getString("source_folder");
			size=files_selected_array.size();
		}
		String other_file_permission = Global.GET_OTHER_FILE_PERMISSION(source_folder);
	}

	public static DeleteFileAlertDialog getInstance(ArrayList<String> files_selected_array, FileObjectType sourceFileObjectType, String source_folder,boolean storage_analyser_delete )
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

		ViewModelFileCount viewModel = new ViewModelProvider(this).get(ViewModelFileCount.class);
		viewModel.countFile(source_folder,sourceFileObjectType,files_selected_array,size,true);

		viewModel.total_no_of_files.observe(this, new Observer<Integer>() {
			@Override
			public void onChanged(Integer integer) {
				no_files_textview.setText(getString(R.string.total_files)+" "+integer);
			}
		});

		viewModel.size_of_files_formatted.observe(this, new Observer<String>() {
			@Override
			public void onChanged(String s) {

				size_files_textview.setText(getString(R.string.size)+" "+s);
			}
		});


		okbutton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(sourceFileObjectType,null))
					{
						Global.print(context,getString(R.string.wait_till_completion_on_going_operation_on_usb));
						return;
					}

					if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_FTP(sourceFileObjectType,null))
					{
						Global.print(context,getString(R.string.wait_till_current_service_on_ftp_finishes));
						return;
					}

					DeleteFileAlertDialog.this.getViewModelStore().clear();
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
							Global.print(context,getString(R.string.maximum_3_services_processed));
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
							}
							else
							{
								if (!check_SAF_permission(file_path, FileObjectType.FILE_TYPE)) return;
							}

						}

						Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
						if(emptyService==null)
						{
							Global.print(context,getString(R.string.maximum_3_services_processed));
							return;
						}
						start_delete_progress_activity(emptyService);
					}
					else if(sourceFileObjectType== FileObjectType.USB_TYPE)
					{
						Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
						if(emptyService==null)
						{
							Global.print(context,getString(R.string.maximum_3_services_processed));
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
								Global.print(context,getString(R.string.maximum_3_services_processed));
								return;
							}
							start_delete_progress_activity(emptyService);
						}
						else
						{
							Global.print(context,getString(R.string.root_access_not_avaialable));
							return;
						}
					}
					else if(sourceFileObjectType== FileObjectType.FTP_TYPE)
					{
						Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
						if(emptyService==null)
						{
							Global.print(context,getString(R.string.maximum_3_services_processed));
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
					DeleteFileAlertDialog.this.getViewModelStore().clear();
					dismissAllowingStateLoss();
				}
			});

		getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE))
				{
					tree_uri=result.getParcelable("tree_uri");
					tree_uri_path=result.getString("tree_uri_path");
					okbutton.callOnClick();
				}

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
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}
	
	private void start_delete_progress_activity(Class service)
	{
		bundle.putString("source_uri_path",tree_uri_path);
		bundle.putParcelable("source_uri",tree_uri);
		Intent intent=new Intent(context,service);
		intent.setAction(DeleteAsyncTask.TASK_TYPE);
		intent.putExtra("bundle",bundle);
		context.startActivity(intent);

	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		//Global.SET_OTHER_FILE_PERMISSION(other_file_permission,source_folder);
	}

	private boolean check_SAF_permission(String file_path,FileObjectType fileObjectType)
	{
		UriPOJO  uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}

		if(uriPOJO==null || tree_uri_path.equals("")) {
			SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,file_path,fileObjectType);
			safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}


	interface OKButtonClickListener
	{
		void deleteDialogOKButtonClick();
	}

}
