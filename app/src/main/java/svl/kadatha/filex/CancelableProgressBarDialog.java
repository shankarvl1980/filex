package svl.kadatha.filex;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.graphics.drawable.*;
import android.graphics.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;


public class CancelableProgressBarDialog extends DialogFragment
{
    private Context context;
	private TextView title;
	private String title_string="";
	//private ProgresBarFragmentCancelListener progresBarFragmentCancelListener;
	private Bundle bundle;
	private String request_code;


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
		setCancelable(false);
		//setRetainInstance(true);
		bundle=getArguments();
		request_code=bundle.getString("request_code");
	}

	public static CancelableProgressBarDialog getInstance(String request_code)
	{
		CancelableProgressBarDialog cancelableProgressBarDialog=new CancelableProgressBarDialog();
		Bundle bundle=new Bundle();
		bundle.putString("request_code",request_code);
		cancelableProgressBarDialog.setArguments(bundle);
		return cancelableProgressBarDialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		if(context==null)
		{
			dismissAllowingStateLoss();
			return null;

		}
		View v=inflater.inflate(R.layout.fragment_cancelable_progressbar,container,false);
		title=v.findViewById(R.id.fragment_cancelable_pbf_title);
        //ProgressBar pb = v.findViewById(R.id.fragment_cancelable_progressbar_pb);

        ViewGroup button_layout = v.findViewById(R.id.fragment_cancelable_pbf_button_layout);
		button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button cancel_button = button_layout.findViewById(R.id.first_button);
		cancel_button.setText(R.string.cancel);
		cancel_button.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View p)
			{

				((AppCompatActivity)context).getSupportFragmentManager().setFragmentResult(request_code,bundle);
				dismissAllowingStateLoss();
				/*
				if(progresBarFragmentCancelListener!=null)
				{
					progresBarFragmentCancelListener.on_cancel_progress();
					
				}

				 */
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
		window.setLayout(Global.DIALOG_WIDTH,WindowManager.LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		title.setText(title_string);
	}
	
	
/*
	@Override
	public void onDestroyView()
	{
		// TODO: Implement this method
		
		if(getDialog()!=null && getRetainInstance())
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

 */
	
	
	public void set_title(String heading)
	{
		title_string=heading;
		if(title!=null)
		{
			title.setText(title_string);
		}
	}
	/*
	public void setProgressBarCancelListener(ProgresBarFragmentCancelListener listener)
	{
		progresBarFragmentCancelListener=listener;
	}
	
	interface ProgresBarFragmentCancelListener
	{
		void on_cancel_progress();
	}

	 */

}
