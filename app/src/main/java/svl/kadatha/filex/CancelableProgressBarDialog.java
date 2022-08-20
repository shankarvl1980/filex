package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;


public class CancelableProgressBarDialog extends DialogFragment
{
    private Context context;
	private TextView title;
	private String title_string="";
	private Bundle bundle;
	private String request_code;
	public static final String CPBD_CANCEL_REQUEST_CODE="cpdb_cancel_request_code";


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
		View v=inflater.inflate(R.layout.fragment_cancelable_progressbar,container,false);
		title=v.findViewById(R.id.fragment_cancelable_pbf_title);
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
			}
			
		});

		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(CPBD_CANCEL_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(CPBD_CANCEL_REQUEST_CODE))
				{
					dismissAllowingStateLoss();
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
		window.setLayout(Global.DIALOG_WIDTH,WindowManager.LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		title.setText(title_string);
	}
	

	public void set_title(String heading)
	{
		title_string=heading;
		if(title!=null)
		{
			title.setText(title_string);
		}
	}

}
