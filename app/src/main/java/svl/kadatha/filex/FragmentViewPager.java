package svl.kadatha.filex;
import android.content.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FragmentViewPager extends FragmentStateAdapter
{
	final Context context;
	FragmentViewPager(Context context)
	{
		super((AppCompatActivity)context);
		this.context=context;
	}

	@Override
	public Fragment createFragment(int p1)
	{
		// TODO: Implement this method
		//if(p1==0)
		{
		
			return new DetailFragment();
			
		}
		//else
		//{
			//((MainActivity)context).createFragmentTransaction(DetailFragment.FILECLICKSELECTED);
		//}
		
		
	}

	@Override
	public int getItemCount()
	{
		// TODO: Implement this method
		return 10;
	}


	
}
