package svl.kadatha.filex;
import android.app.Activity;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.Gallery.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.*;
import android.content.*;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

public class SAFPermissionHelperDialog extends DialogFragment
{

    //private SafPermissionHelperListener safPermissionHelperListener;
    private boolean forUSB;
	private Context context;
	private String tree_uri_path="";
	private Uri tree_uri;
	private String file_path;
	private FileObjectType fileObjectType;
	private Bundle bundle;
	private String request_code;

    //SAFPermissionHelperDialog(){}
    /*
	SAFPermissionHelperDialog(boolean forUSB)
	{
		this.forUSB=forUSB;
	}

     */

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
		//setRetainInstance(true);
		setCancelable(false);
		bundle=getArguments();
		request_code=bundle.getString("request_code");
		file_path=bundle.getString("file_path");
		fileObjectType=(FileObjectType) bundle.getSerializable("fileObjectType");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
        Context context = getContext();
		View v=inflater.inflate(R.layout.fragment_saf_permission_helper,container,false);
        ImageView imageView1 = v.findViewById(R.id.fragment_saf_permission_helper_imageview1);
        ImageView imageView2 = v.findViewById(R.id.fragment_saf_permission_helper_imageview2);
        ImageView imageView3 = v.findViewById(R.id.fragment_saf_permission_helper_imageview3);
        ImageView imageView4 = v.findViewById(R.id.fragment_saf_permission_helper_imageview4);
        TextView textView = v.findViewById(R.id.fragment_saf_permission_helper_tv);
		if(forUSB)
		{
			imageView1.setVisibility(View.GONE);
			imageView2.setVisibility(View.GONE);
			imageView3.setVisibility(View.GONE);
			imageView4.setVisibility(View.GONE);
			textView.setText(R.string.external_usb_permission_message);
		}

		ViewGroup buttons_layout = v.findViewById(R.id.fragment_saf_permission_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
		ok_button.setText(R.string.ok);
        Button cancel_button = v.findViewById(R.id.second_button);
		cancel_button.setText(R.string.cancel);
		ok_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				seekSAFPermission();
				/*
				if(safPermissionHelperListener!=null)
				{
					safPermissionHelperListener.onOKBtnClicked();
				}

				dismissAllowingStateLoss();

				 */
			}
		});
		
		cancel_button.setOnClickListener(new View.OnClickListener()
		{
			
			public void onClick(View v)
			{
				/*
				if(safPermissionHelperListener!=null)
				{
					safPermissionHelperListener.onCancelBtnClicked();
				}

				 */
				dismissAllowingStateLoss();
			}
		});
		
		return v;
	}
	
	public static SAFPermissionHelperDialog getInstance(String request_code, String file_path,FileObjectType fileObjectType)
	{
		SAFPermissionHelperDialog safPermissionHelperDialog=new SAFPermissionHelperDialog();
		Bundle bundle=new Bundle();
		bundle.putString("request_code",request_code);
		bundle.putString("file_path",file_path);
		bundle.putSerializable("fileObjectType",fileObjectType);
		safPermissionHelperDialog.setArguments(bundle);
		return safPermissionHelperDialog;
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

	/*
	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	 */

	private void seekSAFPermission()
	{
		AppCompatActivity appCompatActivity=(AppCompatActivity)context;
		if(appCompatActivity instanceof MainActivity)
		{
			((MainActivity)appCompatActivity).clear_cache=false;
		}
		else if(appCompatActivity instanceof VideoViewActivity)
		{
			((VideoViewActivity)appCompatActivity).clear_cache=false;
		}
		else if(appCompatActivity instanceof AudioPlayerActivity)
		{
			((AudioPlayerActivity)appCompatActivity).clear_cache=false;
		}
		else if(appCompatActivity instanceof FileEditorActivity)
		{
			((FileEditorActivity)appCompatActivity).clear_cache=false;
		}
		else if(appCompatActivity instanceof ImageViewActivity)
		{
			((ImageViewActivity)appCompatActivity).clear_cache=false;
		}
		else if(appCompatActivity instanceof PdfViewActivity)
		{
			((PdfViewActivity)appCompatActivity).clear_cache=false;
		}

		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		activityResultLauncher.launch(intent);
	}

	private final ActivityResultLauncher<Intent> activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
		@Override
		public void onActivityResult(ActivityResult result) {
			if (result.getResultCode()== Activity.RESULT_OK)
			{
				Uri treeUri;
				treeUri = result.getData().getData();
				Global.ON_REQUEST_URI_PERMISSION(context,treeUri);
				if(check_SAF_permission(file_path,fileObjectType))
				{
					bundle.putParcelable("tree_uri",tree_uri);
					bundle.putString("tree_uri_path",tree_uri_path);
					((AppCompatActivity)context).getSupportFragmentManager().setFragmentResult(request_code,bundle);
					dismissAllowingStateLoss();
				}
				else
				{
					Global.print(context,getString(R.string.permission_not_granted));
				}


			}
			else
			{
				Global.print(context,getString(R.string.permission_not_granted));
			}
		}
	});

	private boolean check_SAF_permission(String file_path,FileObjectType fileObjectType)
	{
		UriPOJO  uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}
		else
		{
			return false;
		}


		return !tree_uri_path.equals("");
	}


	
	/*
	public void set_safpermissionhelperlistener(SafPermissionHelperListener safpermissionlistener)
	{
		safPermissionHelperListener=safpermissionlistener;
	}
	
	interface SafPermissionHelperListener
	{
		void onOKBtnClicked();
		void onCancelBtnClicked();
	}

	 */
}
