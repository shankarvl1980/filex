package svl.kadatha.filex;
import android.content.Context;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.view.WindowManager.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class ProgressBarFragment extends DialogFragment
{

	public static String TAG="progress_bar_fragment";
	private static ProgressBarFragment progressBarFragment;
	private Context context;
	private AppCompatActivity appCompatActivity;



	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		this.context=getContext();
		appCompatActivity=((AppCompatActivity) context);
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setCancelable(false);
	}

	public static ProgressBarFragment newInstance() {

		//Bundle args = new Bundle();
		//fragment.setArguments(args);
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
