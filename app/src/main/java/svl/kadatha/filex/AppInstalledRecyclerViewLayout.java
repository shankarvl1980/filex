package svl.kadatha.filex;

import android.content.Context;
import android.view.ViewGroup;

public abstract class AppInstalledRecyclerViewLayout extends ViewGroup {

    public AppInstalledRecyclerViewLayout(Context context) {
        super(context);
    }

    public abstract void setData(AppManagerListFragment.AppPOJO appPOJO, boolean item_selected);

}
