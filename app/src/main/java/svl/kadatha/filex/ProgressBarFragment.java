package svl.kadatha.filex;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

import androidx.fragment.app.DialogFragment;

public class ProgressBarFragment extends DialogFragment
{

	public static String TAG="progress_bar_fragment";


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
	}

	public static ProgressBarFragment newInstance() {

		return new ProgressBarFragment();
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

}
