package svl.kadatha.filex;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

public class ListPopupWindowPOJO {
    final int resource_id;
    final String menu_name;
    final int id;

    public ListPopupWindowPOJO(int resource_id, String menu_name, int id) {
        this.resource_id = resource_id;
        this.menu_name = menu_name;
        this.id = id;
    }

    public static class PopupWindowAdapter extends ArrayAdapter<ListPopupWindowPOJO> {
        final Context context;
        final List<ListPopupWindowPOJO> list;

        public PopupWindowAdapter(Context context, List<ListPopupWindowPOJO> list) {
            super(context, R.layout.list_popupwindow_layout, list);
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v;
            ViewHolder vh;
            if (convertView == null) {
                v = LayoutInflater.from(context).inflate(R.layout.list_popupwindow_layout, parent, false);
                vh = new ViewHolder();
                vh.imageView = v.findViewById(R.id.list_popupwindow_layout_iv);
                vh.textView = v.findViewById(R.id.list_popupwindow_tv);
                v.setTag(vh);
            } else {
                v = convertView;
                vh = (ViewHolder) convertView.getTag();
            }
            ListPopupWindowPOJO listPopupWindowPOJO = list.get(position);
            vh.imageView.setImageDrawable(ContextCompat.getDrawable(context, listPopupWindowPOJO.resource_id));
            vh.textView.setText(listPopupWindowPOJO.menu_name);
            return v;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        public static class ViewHolder {
            ImageView imageView;
            TextView textView;
            View divider;
        }
    }
}
