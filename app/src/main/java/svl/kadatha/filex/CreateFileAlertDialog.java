package svl.kadatha.filex;
import android.content.*;
import android.widget.*;

import android.view.*;
import android.view.animation.*;
import android.os.*;
import android.graphics.drawable.*;
import android.graphics.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

public class CreateFileAlertDialog extends DialogFragment
{

	private String [] type_file;
	private	Context context;
	private String parent_folder;
	private FileObjectType fileObjectType;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		//this.setRetainInstance(true);
		setCancelable(false);
		type_file=new String[]{getString(R.string.file),getString(R.string.folder)};
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			parent_folder=bundle.getString("parent_folder");
			fileObjectType=(FileObjectType) bundle.getSerializable("fileObjectType");
		}
	
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		

		View v=inflater.inflate(R.layout.fragment_create_file,container,false);
        ListView file_folder_listview = v.findViewById(R.id.file_type_ListView);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_file_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button close_button = buttons_layout.findViewById(R.id.first_button);
		close_button.setText(R.string.close);
		ArrayAdapter<String>adater=new ArrayAdapter<>(context,android.R.layout.simple_list_item_1,type_file);
		file_folder_listview.setAdapter(adater);
		file_folder_listview.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				public void onItemClick(AdapterView<?> p1, View p2,int p3,long p4)
				{
					CreateFileDialog createFileDialog=CreateFileDialog.getInstance(p3,parent_folder,fileObjectType);
					createFileDialog.show(((AppCompatActivity)context).getSupportFragmentManager(),null);
					dismissAllowingStateLoss();
				}
			});
			
		close_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				dismissAllowingStateLoss();
			}
		});
		return v;
		
	}

	public static CreateFileAlertDialog getInstance(String parent_folder,FileObjectType fileObjectType)
	{
		CreateFileAlertDialog createFileAlertDialog=new CreateFileAlertDialog();
		Bundle bundle=new Bundle();
		bundle.putString("parent_folder",parent_folder);
		bundle.putSerializable("fileObjectType",fileObjectType);
		createFileAlertDialog.setArguments(bundle);
		return createFileAlertDialog;
	}
	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		window.setLayout(Global.DIALOG_WIDTH, WindowManager.LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}


	
	/*
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	 */
	
}
