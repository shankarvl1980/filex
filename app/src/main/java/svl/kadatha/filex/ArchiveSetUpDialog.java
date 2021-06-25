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
import android.widget.GridLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.util.ArrayList;


public class ArchiveSetUpDialog extends DialogFragment
{
	private Context context;
	private String folderclickselected;
	private View zipdialogview;
    private CheckBox create_folder_checkbox;
	private EditText zip_file_edittext,customdir_edittext;
    private RadioButton rb_current_dir,rb_custom_dir;
	private Button browsebutton;
	private Button okbutton;
	private String zip_file_path="";
    private String archive_action;
	private final ArrayList<String> files_selected_array=new ArrayList<>();
	private final ArrayList<String> zipentry_selected_array=new ArrayList<>();
	private final int saf_permission_request_code=112;
	private final int folder_select_request_code=1569;
	private boolean saf_permission_requested;
	private String tree_uri_path="";
	//private final String source_uri_path="";
	private Uri tree_uri,source_uri;
	private FileObjectType sourceFileObjectType;
	private FileObjectType current_dir_fileObjectType,custom_dir_fileObjectType,destFileObjectType;
	private InputMethodManager imm;
	private String first_file_name,parent_file_name,parent_file_path;
	public final static String ARCHIVE_ACTION_ZIP="archive-zip";
	public final static String ARCHIVE_ACTION_UNZIP="archive-unzip";

	private ArchiveSetUpDialog(){}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
			if(bundle.getStringArrayList("zipentry_selected_array")!=null)
			{
				zipentry_selected_array.addAll(bundle.getStringArrayList("zipentry_selected_array"));
			}
			sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
			archive_action=bundle.getString("archive_action");
			String first_file_path=files_selected_array.get(0);
			first_file_name = new File(first_file_path).getName();
			if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				parent_file_path=Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR();
				current_dir_fileObjectType=FileObjectType.FILE_TYPE;
			}
			else
			{
				current_dir_fileObjectType=sourceFileObjectType;
				parent_file_path=getParentFilePath(first_file_path);
			}

			String s=new File(parent_file_path).getName();
			parent_file_name =s.equals("0") ? first_file_name : s;
		}

		folderclickselected=Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR();
		custom_dir_fileObjectType=FileObjectType.FILE_TYPE;
		//DetailFragment df = (DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
	}

	public static ArchiveSetUpDialog getInstance(ArrayList<String>files_selected_array,ArrayList<String>zipentry_selected_array,FileObjectType sourceFileObjectType, String archive_action)
	{
		ArchiveSetUpDialog archiveSetUpDialog=new ArchiveSetUpDialog();
		Bundle bundle=new Bundle();
		bundle.putStringArrayList("files_selected_array", files_selected_array);
		bundle.putStringArrayList("zipentry_selected_array",zipentry_selected_array);
		bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
		bundle.putString("archive_action",archive_action);
		archiveSetUpDialog.setArguments(bundle);
		return archiveSetUpDialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		zipdialogview=inflater.inflate(R.layout.fragment_archive_setup,container,false);
		return zipdialogview;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);
		context=getContext();
		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        TextView dialog_heading = zipdialogview.findViewById(R.id.dialog_archive_heading);
        TextView outputfilename = zipdialogview.findViewById(R.id.dialog_archive_outputfilename);
		create_folder_checkbox=zipdialogview.findViewById(R.id.dialog_archive_checkbox);
		zip_file_edittext=zipdialogview.findViewById(R.id.dialog_archive_textview_zipname);
        RadioGroup rg = zipdialogview.findViewById(R.id.dialog_archive_rg);
		rb_current_dir=zipdialogview.findViewById(R.id.dialog_archive_rb_current_dir);
		rb_custom_dir=zipdialogview.findViewById(R.id.dialog_archive_rb_custom_dir);
		customdir_edittext=zipdialogview.findViewById(R.id.dialog_archive_edittext_customdir);
		browsebutton=zipdialogview.findViewById(R.id.dialog_archive_browse_button);
        ViewGroup buttons_layout = zipdialogview.findViewById(R.id.fragment_archive_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
		okbutton=zipdialogview.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
		Button cancelbutton = zipdialogview.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);
		browsebutton.setVisibility(View.GONE);
		customdir_edittext.setVisibility(View.GONE);

		rb_current_dir.setText(parent_file_path);
		rb_custom_dir.setText(R.string.choose_directory);
		customdir_edittext.setText(folderclickselected);
		rg.check(rb_current_dir.getId());
		zip_file_path=files_selected_array.get(0);

		create_folder_checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton p1,boolean p2)
			{
				if(p1.isChecked())
				{
					zip_file_edittext.setEnabled(true);
					zip_file_edittext.setAlpha(Global.ENABLE_ALFA);
				}
				else
				{
					zip_file_edittext.setEnabled(false);
					zip_file_edittext.setAlpha(Global.DISABLE_ALFA);
				}
			}
		});
	
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			public void onCheckedChanged(RadioGroup rg,int pos)
			{
				if(rb_custom_dir.isChecked())
				{
					customdir_edittext.setVisibility(View.VISIBLE);
					browsebutton.setVisibility(View.VISIBLE);
				}
				else
				{
					customdir_edittext.setVisibility(View.GONE);
					browsebutton.setVisibility(View.GONE);
				}
			}
		});
		
		browsebutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(rb_custom_dir.isChecked())
				{
					((MainActivity)context).clear_cache=false;
					Intent intent=new Intent(context,FileSelectorActivity.class);
					intent.putExtra(FileSelectorActivity.ACTION_SOUGHT,FileSelectorActivity.FOLDER_SELECT_REQUEST_CODE);
					startActivityForResult(intent,folder_select_request_code);
				}
			}
		});

		switch(archive_action)
		{
			case ARCHIVE_ACTION_ZIP:
			{
				create_folder_checkbox.setVisibility(View.GONE);
				dialog_heading.setText(R.string.archive);
				outputfilename.setText(R.string.output_file_colon);
				if(files_selected_array.size()==1)
				{
					zip_file_edittext.setText(first_file_name);
				}
				else
				{
					zip_file_edittext.setText(parent_file_name);
				}
				zip_file_edittext.setSelection(zip_file_edittext.getText().length());

				okbutton.setOnClickListener(new View.OnClickListener()
					{
						public void onClick(View v)
						{
							
							String zip_folder_name=zip_file_edittext.getText().toString().trim();
							if(zip_folder_name.equals(""))
							{
								print(getString(R.string.enter_zip_file_name));
								return;
							}
							if(CheckStringForSpecialCharacters.whetherStringContains(zip_folder_name))
							{
								print(getString(R.string.avoid_name_involving_special_characters));
								return;
							}

							String archivedestfolder=rb_current_dir.isChecked() ? rb_current_dir.getText().toString() : customdir_edittext.getText().toString();
							destFileObjectType=rb_current_dir.isChecked() ? current_dir_fileObjectType : custom_dir_fileObjectType;
							final String zip_folder_path=(archivedestfolder.endsWith(File.separator)) ? archivedestfolder+zip_folder_name : archivedestfolder+File.separator+zip_folder_name;

							if (!isFilePathValidExists(archivedestfolder, destFileObjectType)) {
								print(getString(R.string.directory_not_exist_not_valid));
								return;
							}

							if(!is_file_writable(archivedestfolder,destFileObjectType))
							{
								return;
							}

							final Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
							if(emptyService==null)
							{
								print(getString(R.string.maximum_3_services_processed));
								return;
							}
							Global.REMOVE_RECURSIVE_PATHS(files_selected_array,archivedestfolder,destFileObjectType,sourceFileObjectType);
							final Bundle bundle=new Bundle();
							bundle.putStringArrayList("files_selected_array",files_selected_array);
							bundle.putString("dest_folder",archivedestfolder);
							bundle.putString("zip_file_path",zip_file_path);
							bundle.putString("zip_folder_name",zip_folder_name);
							bundle.putString("archive_action",archive_action);
							bundle.putString("tree_uri_path",tree_uri_path);
							bundle.putParcelable("tree_uri",tree_uri);
							bundle.putString("source_folder",parent_file_path);
							bundle.putSerializable("sourceFileObjectType",sourceFileObjectType);
							bundle.putSerializable("destFileObjectType",destFileObjectType);

							if(whether_file_already_exists(zip_folder_path+".zip",destFileObjectType))
							{
								if(!isFilePathDirectory(zip_folder_path+".zip",destFileObjectType))
								{
									ArchiveReplaceConfirmationDialog archiveReplaceConfirmationDialog=new ArchiveReplaceConfirmationDialog();
									archiveReplaceConfirmationDialog.setArchiveReplaceDialogListener(new ArchiveReplaceConfirmationDialog.ArchiveReplaceDialogListener()
									{
										public void onYes()
										{
											files_selected_array.remove(zip_folder_path+".zip");
											bundle.putStringArrayList("files_selected_array",files_selected_array);
											Intent intent=new Intent(context,emptyService);
											intent.setAction(ARCHIVE_ACTION_ZIP);
											intent.putExtra("bundle",bundle);
											context.startActivity(intent);
											imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
											dismissAllowingStateLoss();

										}
									});
									archiveReplaceConfirmationDialog.setArguments(bundle);
									archiveReplaceConfirmationDialog.show(MainActivity.FM,null);

								}
								else
								{
									print(getString(R.string.a_directory_with_output_file_name_already_exists)+" '"+zip_folder_name+"'");
								}
							}
							else
							{
								Intent intent=new Intent(context,emptyService);
								intent.setAction(ARCHIVE_ACTION_ZIP);
								intent.putExtra("bundle",bundle);
								context.startActivity(intent);
								imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(),0);
								dismissAllowingStateLoss();
							}
						}

					});
					break;
			}
			
			case ARCHIVE_ACTION_UNZIP:
			{
				dialog_heading.setText(R.string.extract);
				outputfilename.setText(R.string.output_folder);
				int ext_index= first_file_name.lastIndexOf(".");
				zip_file_edittext.setText(first_file_name.substring(0,ext_index));
				zip_file_edittext.setSelection(zip_file_edittext.getText().length());
				okbutton.setOnClickListener(new View.OnClickListener()
				{

						public void onClick(View v) {
							String zip_output_folder = zip_file_edittext.getText().toString().trim();
							final Bundle bundle = new Bundle();
							bundle.putStringArrayList("files_selected_array", files_selected_array);
							bundle.putStringArrayList("zipentry_selected_array", zipentry_selected_array);
							bundle.putString("zip_file_path", zip_file_path);
							bundle.putString("archive_action",archive_action);
							bundle.putBoolean("archive_view", false);
							bundle.putString("source_folder",parent_file_path);
							bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);

							String zip_folder_path;
							String unarchivedestfolder=rb_current_dir.isChecked() ? rb_current_dir.getText().toString() : customdir_edittext.getText().toString();
							destFileObjectType=rb_current_dir.isChecked() ? current_dir_fileObjectType : custom_dir_fileObjectType;
							bundle.putSerializable("destFileObjectType", destFileObjectType); //put destfileobjecttype after deciding which one to put

							if (create_folder_checkbox.isChecked()) {
								if (zip_output_folder == null || zip_output_folder.equals("")) {
									print(getString(R.string.enter_output_folder_name));
									return;
								}

								if (CheckStringForSpecialCharacters.whetherStringContains(zip_output_folder)) {
									print(getString(R.string.avoid_name_involving_special_characters));
									return;
								}
								zip_folder_path=unarchivedestfolder.endsWith(File.separator) ? unarchivedestfolder+zip_output_folder : unarchivedestfolder+File.separator+zip_output_folder;
							}
							else
							{
								zip_folder_path=unarchivedestfolder;
								zip_output_folder=null;
							}


							if (!isFilePathValidExists(unarchivedestfolder, destFileObjectType)) {
								print(getString(R.string.directory_not_exist_not_valid));
								return;
							}

							if (!is_file_writable(unarchivedestfolder, destFileObjectType)) {
								return;
							}



							final Class emptyService = ArchiveDeletePasteServiceUtil.getEmptyService(context);
							if (emptyService == null) {
								print(getString(R.string.maximum_3_services_processed));
								return;
							}

							bundle.putString("tree_uri_path", tree_uri_path);
							bundle.putParcelable("tree_uri", tree_uri);
							bundle.putString("dest_folder", unarchivedestfolder);
							bundle.putString("zip_folder_name", zip_output_folder);
							if (create_folder_checkbox.isChecked() && whether_file_already_exists(zip_folder_path, destFileObjectType)) {
								if (isFilePathDirectory(zip_folder_path, destFileObjectType)) {
									ArchiveReplaceConfirmationDialog archiveReplaceConfirmationDialog = new ArchiveReplaceConfirmationDialog();
									archiveReplaceConfirmationDialog.setArchiveReplaceDialogListener(new ArchiveReplaceConfirmationDialog.ArchiveReplaceDialogListener() {
										public void onYes() {
											Intent intent = new Intent(context, emptyService);
											intent.setAction(ARCHIVE_ACTION_UNZIP);
											intent.putExtra("bundle", bundle);
											context.startActivity(intent);
											imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(), 0);
											dismissAllowingStateLoss();
										}
									});
									archiveReplaceConfirmationDialog.setArguments(bundle);
									archiveReplaceConfirmationDialog.show(MainActivity.FM, null);
								} else {
									print(getString(R.string.a_file_with_folder_name_exists_in_selected_directory));

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

		cancelbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(),0);
				dismissAllowingStateLoss();
			}
			
		});
	}

	private String getParentFilePath(String file_path)
	{
		return new File(file_path).getParent();

	}

	private boolean check_SAF_permission(String parent_file_path, FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}


		if(tree_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=new SAFPermissionHelperDialog();
			safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener()
			{
				public void onOKBtnClicked()
				{
					seekSAFPermission();
				}

				public void onCancelBtnClicked()
				{
					
				}
			});
			safpermissionhelper.show(MainActivity.FM,"saf_permission_dialog");
			saf_permission_requested=true;
			imm.hideSoftInputFromWindow(zip_file_edittext.getWindowToken(),0);
			return false;
		}
		else
		{
			return true;
		}
	}

	private boolean is_file_writable(String file_path,FileObjectType fileObjectType)
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
		else return fileObjectType == FileObjectType.USB_TYPE;

	}

	private boolean isFilePathDirectory(String file_path, FileObjectType fileObjectType)
	{

		if((fileObjectType==FileObjectType.FILE_TYPE) || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			return new File(file_path).isDirectory();
		}
		else  if(fileObjectType==FileObjectType.USB_TYPE)
		{
			return FileUtil.getUsbFile(MainActivity.usbFileRoot,file_path).isDirectory();
		}
		else
		{
			return false;
		}
	}

	private boolean whether_file_already_exists(String new_file_path,FileObjectType fileObjectType)
	{
		if(fileObjectType== FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			File new_file=new File(new_file_path);
			return new_file.exists();

		}
		else if(fileObjectType== FileObjectType.USB_TYPE)
		{
			UsbFile usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,new_file_path);
			return usbFile != null;

		}
		else if(fileObjectType==FileObjectType.ROOT_TYPE)
		{
			if(RootUtils.CAN_RUN_ROOT_COMMANDS())
			{
				return !RootUtils.WHETHER_FILE_EXISTS(new_file_path);
			}
			else
			{
				print(getString(R.string.root_access_not_avaialable));
				return false;
			}

		}
		else
		{
			if(check_SAF_permission(new_file_path,fileObjectType))
			{
				return FileUtil.exists(context, new_file_path, tree_uri, tree_uri_path);
			}
			else
			{
				return false;
			}
		}

	}


	private boolean isFilePathValidExists(String file_path,FileObjectType fileObjectType)
	{
		return whether_file_already_exists(file_path,fileObjectType);
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

	
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}
	

	public void seekSAFPermission()
	{
		((MainActivity)context).clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, saf_permission_request_code);
	}


	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) 
	{
		switch (requestCode)
		{
			case saf_permission_request_code:
				if (resultCode == Activity.RESULT_OK)
				{
					Uri treeUri;
					treeUri = resultData.getData();
					Global.ON_REQUEST_URI_PERMISSION(context, treeUri);

					saf_permission_requested = false;
					okbutton.callOnClick();
				}
				else
				{
					print(getString(R.string.permission_not_granted));
				}

				break;
			case folder_select_request_code:
				if (resultCode == Activity.RESULT_OK)
				{
					folderclickselected = resultData.getStringExtra("folderclickselected");
					custom_dir_fileObjectType = (FileObjectType) resultData.getSerializableExtra("destFileObjectType");
					customdir_edittext.setText(folderclickselected);
				}

				break;
		}

	}


	private void print(String msg)
	{
		android.widget.Toast.makeText(context,msg,android.widget.Toast.LENGTH_SHORT).show();
	}
}
