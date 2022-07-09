package svl.kadatha.filex;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.TableRow.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;


public class AppManagerSortDialog extends DialogFragment
{

    private TinyDB tinyDB;
    private ImageButton name_asc_btn,name_desc_btn,date_asc_btn,date_desc_btn,size_asc_btn,size_desc_btn;
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
        tinyDB=new TinyDB(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setCancelable(false);
        //this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // TODO: Implement this method

        View v= inflater.inflate(R.layout.fragment_storage_analyser_sort,container,false);
        name_asc_btn=v.findViewById(R.id.storage_analyser_name_asc);
        name_desc_btn=v.findViewById(R.id.storage_analyser_name_desc);
        date_asc_btn=v.findViewById(R.id.storage_analyser_date_asc);
        date_desc_btn=v.findViewById(R.id.storage_analyser_date_desc);
        size_asc_btn=v.findViewById(R.id.storage_analyser_size_asc);
        size_desc_btn=v.findViewById(R.id.storage_analyser_size_desc);

        SortButtonClickListener sortButtonClickListener = new SortButtonClickListener();
        name_asc_btn.setOnClickListener(sortButtonClickListener);
        name_desc_btn.setOnClickListener(sortButtonClickListener);
        date_asc_btn.setOnClickListener(sortButtonClickListener);
        date_desc_btn.setOnClickListener(sortButtonClickListener);
        size_asc_btn.setOnClickListener(sortButtonClickListener);
        size_desc_btn.setOnClickListener(sortButtonClickListener);


        ViewGroup buttons_layout = v.findViewById(R.id.storage_analyser_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button close_button = buttons_layout.findViewById(R.id.first_button);
        close_button.setText(R.string.close);
        close_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                dismissAllowingStateLoss();
            }
        });


        set_selection();

        return v;
    }

    private void set_selection()
    {
        switch(Global.APP_MANAGER_SORT)
        {

            case "d_name_asc":
            case "f_name_asc":
                name_asc_btn.setSelected(true);

                name_desc_btn.setSelected(false);
                date_asc_btn.setSelected(false);
                date_desc_btn.setSelected(false);
                size_asc_btn.setSelected(false);
                size_desc_btn.setSelected(false);
                break;


            case "d_name_desc":
            case "f_name_desc":
                name_desc_btn.setSelected(true);

                name_asc_btn.setSelected(false);
                date_asc_btn.setSelected(false);
                date_desc_btn.setSelected(false);
                size_asc_btn.setSelected(false);
                size_desc_btn.setSelected(false);
                break;

            case "d_date_asc":
            case "f_date_asc":
                date_asc_btn.setSelected(true);

                name_asc_btn.setSelected(false);
                name_desc_btn.setSelected(false);
                date_desc_btn.setSelected(false);
                size_asc_btn.setSelected(false);
                size_desc_btn.setSelected(false);
                break;

            case "d_date_desc":
            case "f_date_desc":
                date_desc_btn.setSelected(true);

                name_asc_btn.setSelected(false);
                name_desc_btn.setSelected(false);
                date_asc_btn.setSelected(false);
                size_asc_btn.setSelected(false);
                size_desc_btn.setSelected(false);
                break;

            case "d_size_asc":
            case "f_size_asc":
                size_asc_btn.setSelected(true);

                name_asc_btn.setSelected(false);
                name_desc_btn.setSelected(false);
                date_asc_btn.setSelected(false);
                date_desc_btn.setSelected(false);
                size_desc_btn.setSelected(false);
                break;


            default:
                size_desc_btn.setSelected(true);

                name_asc_btn.setSelected(false);
                name_desc_btn.setSelected(false);
                date_asc_btn.setSelected(false);
                date_desc_btn.setSelected(false);
                size_asc_btn.setSelected(false);
                break;
        }
    }

    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    }


/*
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

 */

    private class SortButtonClickListener implements View.OnClickListener
    {

        @Override
        public void onClick(View button)
        {
            // TODO: Implement this method
            String selected_sort;
            int id = button.getId();
            if(id==R.id.storage_analyser_name_asc){
                selected_sort = "d_name_asc";
            } else if (id == R.id.storage_analyser_name_desc) {
                selected_sort = "d_name_desc";
            } else if (id == R.id.storage_analyser_date_asc) {
                selected_sort = "d_date_asc";
            } else if (id == R.id.storage_analyser_date_desc) {
                selected_sort = "d_date_desc";
            } else if(id==R.id.storage_analyser_size_asc) {
                selected_sort="d_size_asc";
            } else {
                selected_sort="d_size_desc";
            }

            if(!selected_sort.equals(Global.APP_MANAGER_SORT))
            {
                ViewPager viewPager=((AppManagerActivity)context).viewPager;
                AppManagerListFragment appManagerListFragment=(AppManagerListFragment) viewPager.getAdapter().instantiateItem(viewPager,1);//fragmentManager.findFragmentById(R.id.app_manager_list_container);
                if(appManagerListFragment!=null && appManagerListFragment.adapter!=null)
                {
                    Global.APP_MANAGER_SORT=selected_sort;
                    set_selection();
                    viewPager.getAdapter().notifyDataSetChanged();
                    tinyDB.putString("app_manager_sort",Global.APP_MANAGER_SORT);
                }
                else
                {
                    Global.print(context,getString(R.string.wait_ellipse));
                }

            }
        }

    }

}

