package svl.kadatha.filex;
import android.os.*;
import android.view.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.view.WindowManager.LayoutParams;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentFactory;

public class ProgressBarFragment extends DialogFragment
{

	private static ProgressBarFragment progressBarFragment;

	private ProgressBarFragment(){}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setCancelable(false);

	}


	public static ProgressBarFragment getInstance()
	{
		if(progressBarFragment==null)
		{
			return new ProgressBarFragment();
		}
		else
		{
			return progressBarFragment;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		
		
		return inflater.inflate(R.layout.fragment_progressbar,container,false);
	}

	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window w=getDialog().getWindow();
		w.clearFlags(LayoutParams.FLAG_DIM_BEHIND);
		w.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
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
	

}
