package svl.kadatha.filex;
import android.content.*;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class FragmentViewPager extends FragmentStatePagerAdapter
{
	final Context context;
	FragmentViewPager(FragmentManager fm, Context context)
	{
		super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		this.context=context;
	}

	@Override
	public Fragment getItem(int p1)
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
	public int getCount()
	{
		// TODO: Implement this method
		return 10;
	}


	
}
