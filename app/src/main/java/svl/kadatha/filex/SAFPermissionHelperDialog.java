package svl.kadatha.filex;
import android.os.*;
import android.view.*;
import android.widget.Gallery.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.*;
import android.content.*;

import androidx.fragment.app.DialogFragment;

public class SAFPermissionHelperDialog extends DialogFragment
{

    private SafPermissionHelperListener safPermissionHelperListener;
    private boolean forUSB;

    SAFPermissionHelperDialog(){}
    SAFPermissionHelperDialog(boolean forUSB)
	{
		this.forUSB=forUSB;
	}
    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setCancelable(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		//return super.onCreateView(inflater, container, savedInstanceState);
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
				if(safPermissionHelperListener!=null)
				{
					safPermissionHelperListener.onOKBtnClicked();
				}
				dismissAllowingStateLoss();
			}
		});
		
		cancel_button.setOnClickListener(new View.OnClickListener()
		{
			
			public void onClick(View v)
			{
				if(safPermissionHelperListener!=null)
				{
					safPermissionHelperListener.onCancelBtnClicked();
				}
				dismissAllowingStateLoss();
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

	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}
	
	public void set_safpermissionhelperlistener(SafPermissionHelperListener safpermissionlistener)
	{
		safPermissionHelperListener=safpermissionlistener;
	}
	
	interface SafPermissionHelperListener
	{
		void onOKBtnClicked();
		void onCancelBtnClicked();
	}
}
