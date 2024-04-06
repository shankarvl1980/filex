package svl.kadatha.filex;

import android.content.Context;
import android.view.ViewGroup;

public abstract class RecyclerViewLayout extends ViewGroup {

    public RecyclerViewLayout(Context context) {
        super(context);
    }

    abstract void setData(FilePOJO filePOJO , boolean item_selected);
    abstract void set_selected(boolean item_selected);

    abstract void setDivider(boolean set);

    abstract void setWhetherExternal(FilePOJO filePOJO);
}
