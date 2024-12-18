package svl.kadatha.filex.appmanager;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableRow.LayoutParams;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

import svl.kadatha.filex.EquallyDistributedDialogButtonsLayout;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.R;
import svl.kadatha.filex.TinyDB;


public class AppManagerSortDialog extends DialogFragment {
    private TinyDB tinyDB;
    private ImageButton name_asc_btn, name_desc_btn, date_asc_btn, date_desc_btn, size_asc_btn, size_desc_btn;
    private Context context;
    private RadioButton list_rb, grid_rb;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        tinyDB = new TinyDB(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_storage_analyser_sort, container, false);
        RadioGroup rg = v.findViewById(R.id.storage_analyser_dialog_view_layout_rg);
        list_rb = v.findViewById(R.id.storage_analyser_view_rb_list);
        grid_rb = v.findViewById(R.id.storage_analyser_dialog_rb_grid);
        if (AppManagerActivity.FILE_GRID_LAYOUT) {
            grid_rb.setChecked(true);
        } else {
            list_rb.setChecked(true);
        }

        ViewPager viewPager = ((AppManagerActivity) context).viewPager;

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (list_rb.isChecked()) {
                    AppManagerActivity.FILE_GRID_LAYOUT = false;
                } else if (grid_rb.isChecked()) {
                    AppManagerActivity.FILE_GRID_LAYOUT = true;
                }

                viewPager.getAdapter().notifyDataSetChanged();
                tinyDB.putBoolean("app_manager_file_grid_layout", AppManagerActivity.FILE_GRID_LAYOUT);
            }
        });

        name_asc_btn = v.findViewById(R.id.storage_analyser_name_asc);
        name_desc_btn = v.findViewById(R.id.storage_analyser_name_desc);
        date_asc_btn = v.findViewById(R.id.storage_analyser_date_asc);
        date_desc_btn = v.findViewById(R.id.storage_analyser_date_desc);
        size_asc_btn = v.findViewById(R.id.storage_analyser_size_asc);
        size_desc_btn = v.findViewById(R.id.storage_analyser_size_desc);

        SortButtonClickListener sortButtonClickListener = new SortButtonClickListener();
        name_asc_btn.setOnClickListener(sortButtonClickListener);
        name_desc_btn.setOnClickListener(sortButtonClickListener);
        date_asc_btn.setOnClickListener(sortButtonClickListener);
        date_desc_btn.setOnClickListener(sortButtonClickListener);
        size_asc_btn.setOnClickListener(sortButtonClickListener);
        size_desc_btn.setOnClickListener(sortButtonClickListener);


        ViewGroup buttons_layout = v.findViewById(R.id.storage_analyser_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button close_button = buttons_layout.findViewById(R.id.first_button);
        close_button.setText(R.string.close);
        close_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });


        set_selection();

        return v;
    }

    private void set_selection() {
        switch (Global.APP_MANAGER_SORT) {

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
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    }

    private class SortButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View button) {
            String selected_sort;
            int id = button.getId();
            if (id == R.id.storage_analyser_name_asc) {
                selected_sort = "d_name_asc";
            } else if (id == R.id.storage_analyser_name_desc) {
                selected_sort = "d_name_desc";
            } else if (id == R.id.storage_analyser_date_asc) {
                selected_sort = "d_date_asc";
            } else if (id == R.id.storage_analyser_date_desc) {
                selected_sort = "d_date_desc";
            } else if (id == R.id.storage_analyser_size_asc) {
                selected_sort = "d_size_asc";
            } else {
                selected_sort = "d_size_desc";
            }

            if (!selected_sort.equals(Global.APP_MANAGER_SORT)) {
                ViewPager viewPager = ((AppManagerActivity) context).viewPager;
                AppManagerListFragment appManagerListFragment = (AppManagerListFragment) viewPager.getAdapter().instantiateItem(viewPager, 1);//fragmentManager.findFragmentById(R.id.app_manager_list_container);
                if (appManagerListFragment != null && appManagerListFragment.progress_bar.getVisibility() == View.GONE) {
                    Global.APP_MANAGER_SORT = selected_sort;
                    set_selection();
                    viewPager.getAdapter().notifyDataSetChanged();
                    tinyDB.putString("app_manager_sort", Global.APP_MANAGER_SORT);
                } else {
                    Global.print(context, getString(R.string.wait_ellipse));
                }

            }
        }

    }

}

