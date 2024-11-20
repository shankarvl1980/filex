package svl.kadatha.filex.appmanager;

import androidx.lifecycle.ViewModel;

import svl.kadatha.filex.IndexedLinkedHashMap;

public class AppManagerListFragmentViewModel extends ViewModel {

    public IndexedLinkedHashMap<Integer, AppManagerListFragment.AppPOJO> mselecteditems = new IndexedLinkedHashMap<>();

}
