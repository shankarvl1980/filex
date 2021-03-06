package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateFileDialog extends DialogFragment
{
    private EditText new_file_name_edittext;
	private Button okbutton;
    private DetailFragment df;
	private int file_type;
	private Context context;
	private InputMethodManager imm;
	private final int request_code=56;
	private String tree_uri_path="";
	private Uri tree_uri;
	private String parent_folder;
	private FileObjectType fileObjectType;
	private String other_file_permission;
	private final List<String> dest_file_names=new ArrayList<>();

	private CreateFileDialog(){}

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			file_type=bundle.getInt("file_type");
			parent_folder=bundle.getString("parent_folder");
			fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
		}


		List<FilePOJO> destFilePOJOs=Global.HASHMAP_FILE_POJO.get(fileObjectType+parent_folder);
		for(FilePOJO filePOJO:destFilePOJOs)
		{
			dest_file_names.add(filePOJO.getName());
		}


		other_file_permission=Global.GET_OTHER_FILE_PERMISSION(parent_folder);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		context=getContext();
		View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView file_label_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
		new_file_name_edittext=v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        TextView no_of_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView files_size_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
		no_of_files_textview.setVisibility(View.GONE);
		files_size_textview.setVisibility(View.GONE);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
		okbutton= buttons_layout.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);
		df=(DetailFragment)MainActivity.FM.findFragmentById(R.id.detail_fragment);
		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if(file_type==0)
		{
			dialog_heading_textview.setText(R.string.enter_file_name);
			file_label_textview.setText(R.string.file_name_colon);
		}
		else
		{
			dialog_heading_textview.setText(R.string.enter_folder_name);
			file_label_textview.setText(R.string.folder_name_colon);
		}
		okbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				boolean file_created=false;
				String new_name=new_file_name_edittext.getText().toString().trim();
				if(new_name.equals(""))
				{
					print(getString(R.string.enter_file_name));
					return;
				}
				if(CheckStringForSpecialCharacters.whetherStringContains(new_name))
				{
					print(getString(R.string.avoid_name_involving_special_characters));
					return;
				}

				String new_file_path =(parent_folder.endsWith(File.separator)) ? parent_folder+new_name : parent_folder+File.separator+new_name;
				File file=new File(new_file_path);

				boolean isWritable=FileUtil.isWritable(fileObjectType,new_file_path);
				if(!check_name_availability(new_file_path, isWritable,fileObjectType))
				{
					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
					return;
				}
				if(file_type==0)
				{
					if(isWritable)
					{
						file_created=FileUtil.createNativeNewFile(file);
					}
					else if(fileObjectType== FileObjectType.FILE_TYPE)
					{
						file_created=FileUtil.createSAFNewFile(context,parent_folder,new_name,tree_uri,tree_uri_path);
					}
					else if(fileObjectType== FileObjectType.USB_TYPE)
					{
						UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_folder);
						file_created=FileUtil.createUsbFile(parentUsbFile,new_name);

					}
					else if(fileObjectType==FileObjectType.ROOT_TYPE)
					{
						//file_created=RootUtils.EXECUTE(Arrays.asList(">",new_file_path));
						if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
						{
							file_created=FileUtil.createNativeNewFile(file);
						}
					}
                }
				else if(file_type==1)
				{
					if(isWritable)
					{
						file_created=FileUtil.mkdirNative(file);
					}
					else if(fileObjectType== FileObjectType.FILE_TYPE)
					{
						file_created=FileUtil.mkdirSAF(context,parent_folder,new_name,tree_uri,tree_uri_path);
					}
					else if(fileObjectType== FileObjectType.USB_TYPE)
					{
						UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_folder);
						file_created=FileUtil.mkdirUsb(parentUsbFile,new_name);
					}
					else if(fileObjectType==FileObjectType.ROOT_TYPE)
					{
						//file_created=RootUtils.EXECUTE(Arrays.asList("mkdir","-p",new_file_path));
						if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
						{
							file_created=FileUtil.mkdirNative(file);

						}

					}

				}

				if(file_created)
				{
					final FilePOJO filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(parent_folder, Collections.singletonList(new_name),fileObjectType,null);
					Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {


							df.clearSelectionAndNotifyDataSetChanged();
							int idx=df.filePOJO_list.indexOf(filePOJO);
							if(df.llm!=null)
							{
								df.llm.scrollToPositionWithOffset(idx,0);
							}
							else if(df.glm!=null)
							{
								df.glm.scrollToPositionWithOffset(idx,0);
							}


						}
					},500);
					
					print("'"+new_name+ "' "+getString(R.string.created));
				}
				else
				{
					print(getString(R.string.could_not_create));
				}
				Global.SET_OTHER_FILE_PERMISSION(other_file_permission,parent_folder);
				imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
				dismissAllowingStateLoss();

			}	
				

		});
			
		cancelbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
				dismissAllowingStateLoss();
			}
				
				
		});
		return v;
	}

	public static CreateFileDialog getInstance(int file_type, String parent_folder, FileObjectType fileObjectType)
	{
		CreateFileDialog createFileDialog=new CreateFileDialog();
		Bundle bundle=new Bundle();
		bundle.putInt("file_type",file_type);
		bundle.putString("parent_folder",parent_folder);
		bundle.putSerializable("fileObjectType",fileObjectType);
		createFileDialog.setArguments(bundle);
		return createFileDialog;
	}

	private boolean check_name_availability(String new_file_path, boolean isWritable, FileObjectType fileObjectType)
	{
		File new_file=new File(new_file_path);
		if(dest_file_names.contains(new_file.getName()))
		{
			print(getString(R.string.new_file_can_not_be_created_a_file_with_the_specified_name_exists));
			return false;
		}
		if(fileObjectType== FileObjectType.FILE_TYPE)
		{
			if(isWritable)
			{
				return true;
			}
			else
			{
				return check_SAF_permission(new_file_path,fileObjectType);
			}

		}
		else if(fileObjectType== FileObjectType.USB_TYPE)
		{

		}
		else if(fileObjectType==FileObjectType.ROOT_TYPE)
		{

			print(getString(R.string.root_access_not_avaialable));
			return false;
		}
		else
		{
			return check_SAF_permission(new_file_path,fileObjectType);

		}
		print(getString(R.string.could_not_create));
		return false;
	}

	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		new_file_name_edittext.requestFocus();
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
	}

	private boolean check_SAF_permission(String new_file_path,FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(new_file_path,fileObjectType);
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
			//saf_permission_requested=true;

			return false;
		}
		else
		{
			return true;
		}
	}

	public void seekSAFPermission()
	{
		((MainActivity)context).clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, request_code);
	}

	// @TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) 
	{
		if (requestCode == this.request_code && resultCode== Activity.RESULT_OK)
		{
			Uri treeUri;
			treeUri = resultData.getData();
			Global.ON_REQUEST_URI_PERMISSION(context,treeUri);

			//saf_permission_requested=false;
			okbutton.callOnClick();
		}
		else
		{
			print(getString(R.string.permission_not_granted));
		}
		
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		// TODO: Implement this method
		imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
		super.onCancel(dialog);
	}
	

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		// TODO: Implement this method
		imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
		super.onDismiss(dialog);
	}
	

	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}



	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
	
}
