package svl.kadatha.filex;

import android.util.SparseBooleanArray;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class AppManagerListFragmentViewModel extends ViewModel {

    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public List<AppManagerListFragment.AppPOJO> app_selected_array=new ArrayList<>();
}
