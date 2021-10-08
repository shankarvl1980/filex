package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class DefaultAppsDialog extends DialogFragment
{
    private Context context;
    private List<DefaultAppPOJO> defaultAppPOJOS;
    private List<DefaultAppPOJO> selectedDefaultPOJOS;
    private SparseArray<String> mSelectedMimeType;
    private TableLayout tableLayout;
    private DefaultAppDatabaseHelper defaultAppDatabaseHelper;
    private int top_row_color,detail_row_color;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        defaultAppPOJOS=new ArrayList<>();
        selectedDefaultPOJOS=new ArrayList<>();
        defaultAppDatabaseHelper=new DefaultAppDatabaseHelper(getContext());
        defaultAppPOJOS=defaultAppDatabaseHelper.getDefaultAppPOJOList();
        top_row_color=getResources().getColor(R.color.light_tab_select_text_color);
        TypedValue typedValue=new TypedValue();
        Resources.Theme theme=context.getTheme();
        theme.resolveAttribute(R.attr.recycler_text_color,typedValue,true);
        detail_row_color=typedValue.data;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_default_app,container,false);
        tableLayout=v.findViewById(R.id.default_app_table_layout);

        mSelectedMimeType=new SparseArray<>();
        selectedDefaultPOJOS=new ArrayList<>();

        int size=defaultAppPOJOS.size();
        for(int i=-1;i<size;++i)
        {
            final TableRow itemView=(TableRow)LayoutInflater.from(context).inflate(R.layout.default_app_itemview_layout,tableLayout,false);
            TextView mime_type=itemView.findViewById(R.id.default_app_mime_type);
            TextView app_name=itemView.findViewById(R.id.default_app_name);
            ImageView app_icon=itemView.findViewById(R.id.default_app_image_view);
            final CheckBox checkBox=itemView.findViewById(R.id.default_app_checkbox);

            if(i==-1)
            {

                mime_type.setTextColor(top_row_color);
                app_name.setTextColor(top_row_color);
                mime_type.setText(R.string.file_type);
                app_name.setText(R.string.app_name);
                checkBox.setVisibility(View.GONE);
            }
            else
            {

                mime_type.setTextColor(detail_row_color);
                app_name.setTextColor(detail_row_color);
                final DefaultAppPOJO defaultAppPOJO=defaultAppPOJOS.get(i);
                mime_type.setText(defaultAppPOJO.file_type);
                app_name.setText(defaultAppPOJO.app_name);
                app_icon.setImageDrawable(defaultAppPOJO.app_icon);
                checkBox.setVisibility(View.VISIBLE);

                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos=tableLayout.indexOfChild(itemView);
                        if (((CheckBox)v).isChecked())
                        {
                            mSelectedMimeType.put(pos,defaultAppPOJO.mime_type);
                            selectedDefaultPOJOS.add(defaultAppPOJO);
                        }
                        else
                        {
                            mSelectedMimeType.delete(pos);
                            selectedDefaultPOJOS.remove(defaultAppPOJO);
                        }
                    }
                });

            }
            tableLayout.addView(itemView);

        }

        ViewGroup buttons_layout = v.findViewById(R.id.fragment_default_app_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button remove_button=buttons_layout.findViewById(R.id.first_button);
        remove_button.setText(R.string.remove);
        remove_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int size=mSelectedMimeType.size();
                if(size==0)
                {
                    print(getString(R.string.select_app));
                }
                else
                {
                    for(int i=0;i<size;++i)
                    {
                        tableLayout.removeViewAt((mSelectedMimeType.keyAt(i)-i));
                        defaultAppDatabaseHelper.delete_row(mSelectedMimeType.valueAt(i));
                    }
                    defaultAppPOJOS.removeAll(selectedDefaultPOJOS);
                    mSelectedMimeType.clear();
                    selectedDefaultPOJOS.clear();
                }

            }
        });
        Button close_button = buttons_layout.findViewById(R.id.second_button);
        close_button.setText(R.string.close);
        close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissAllowingStateLoss();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window=getDialog().getWindow();
        if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
        {
            window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);
        }
        else
        {
            window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_HEIGHT);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        int list_size=defaultAppPOJOS.size();
        for(int i=0;i<list_size;++i)
        {
            TableRow itemView=(TableRow) tableLayout.getChildAt(i+1);
            ((CheckBox)itemView.getChildAt(3)).setChecked(false);
        }

    }

    @Override
    public void onDestroyView() {

        if(getDialog()!=null && getRetainInstance())
        {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        defaultAppDatabaseHelper.close();
    }

    public static class DefaultAppPOJO
    {
        final String mime_type;
        final String file_type;
        final Drawable app_icon;
        final String app_name;
        final String app_package_name;

        DefaultAppPOJO(String mime_type,String file_type,Drawable app_icon,String app_name,String app_package_name)
        {
            this.mime_type=mime_type;
            this.file_type=file_type;
            this.app_icon=app_icon;
            this.app_name=app_name;
            this.app_package_name=app_package_name;
        }

    }

    /*
    private class DefaultAppsAdapater extends RecyclerView.Adapter<DefaultAppsAdapater.ViewHolder>
    {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.default_app_itemview_layout,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DefaultAppPOJO defaultAppPOJO=defaultAppPOJOS.get(position);
            holder.mime_type.setText(defaultAppPOJO.mime_type);
            holder.app_icon.setImageDrawable(defaultAppPOJO.app_icon);
            holder.app_name.setText(defaultAppPOJO.app_name);
        }

        @Override
        public int getItemCount() {
            return defaultAppPOJOS.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder
        {
            TextView mime_type,app_name;
            ImageView app_icon;
            CheckBox checkBox;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                mime_type=itemView.findViewById(R.id.default_app_mime_type);
                app_name=itemView.findViewById(R.id.default_app_name);
                app_icon=itemView.findViewById(R.id.default_app_image_view);
                checkBox=itemView.findViewById(R.id.default_app_checkbox);
            }
        }
    }

     */
    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
