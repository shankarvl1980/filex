package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class AppManagerActivity extends BaseActivity{

    private Context context;
    public FragmentManager fm;
    private TabLayout tabLayout;
    public ViewPager viewPager;
    private FloatingActionButton floatingActionButton;
    private AppManagerListFragment userAppListFragment,systemAppListFragment;
    private AppManagementFragmentAdapter adapter;
    public static final String USER_INSTALLED_APPS="user_installed_apps";
    public static final String SYSTEM_APPS="system_apps";
    public boolean search_toolbar_visible;
    public KeyBoardUtil keyBoardUtil;
    private Group search_toolbar;
    public EditText search_edittext;
    private final List<SearchFilterListener> searchFilterListeners=new ArrayList<>();



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);
        context=this;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        fm=getSupportFragmentManager();

        View containerLayout = findViewById(R.id.activity_app_manager_container_layout);
        keyBoardUtil=new KeyBoardUtil(containerLayout);
        search_toolbar=findViewById(R.id.app_manager_search_toolbar);
        search_edittext=findViewById(R.id.app_manager_search_view_edit_text);
        search_edittext.setMaxWidth(Integer.MAX_VALUE);
        search_edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!search_toolbar_visible)
                {
                    return;
                }
                for(SearchFilterListener listener:searchFilterListeners)
                {
                    if(listener!=null)
                    {
                        listener.onSearchFilter(s.toString());
                    }
                }
            }
        });

        ImageButton search_cancel_btn = findViewById(R.id.app_manager_search_view_cancel_button);
        search_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_visibility_searchbar(false);
            }
        });




        tabLayout=findViewById(R.id.activity_app_manager_tab_layout);
        viewPager=findViewById(R.id.activity_app_manager_viewpager);
        adapter=new AppManagementFragmentAdapter(fm);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.setupWithViewPager(viewPager);

        floatingActionButton=findViewById(R.id.floating_action_app_manager);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });


        adapter.startUpdate(viewPager);
        userAppListFragment= (AppManagerListFragment) adapter.instantiateItem(viewPager,0);
        systemAppListFragment= (AppManagerListFragment) adapter.instantiateItem(viewPager,1);
        adapter.finishUpdate(viewPager);

        Intent intent=getIntent();
        if(savedInstanceState==null)
        {
            if(intent!=null) on_intent(intent);
        }

    }

    private void on_intent(Intent intent)
    {

    }


    public void set_visibility_searchbar(boolean visible)
    {
        if(userAppListFragment.asyncTaskStatus!=AsyncTaskStatus.COMPLETED || systemAppListFragment.asyncTaskStatus!=AsyncTaskStatus.COMPLETED)
        {
            print(getString(R.string.please_wait));
            return;
        }
        search_toolbar_visible=visible;
        if(search_toolbar_visible)
        {
            ((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_edittext.requestFocus();
            userAppListFragment.clear_selection();
            systemAppListFragment.clear_selection();
        }
        else
        {
            ((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
            search_toolbar.setVisibility(View.GONE);
            search_edittext.setText("");
            search_edittext.clearFocus();
            userAppListFragment.clear_selection();
            systemAppListFragment.clear_selection();
            for(SearchFilterListener listener:searchFilterListeners)
            {
                if(listener!=null)
                {
                    listener.onSearchFilter(null);
                }
            }
        }

    }



    private class AppManagementFragmentAdapter extends FragmentPagerAdapter
    {

        public AppManagementFragmentAdapter(@NonNull FragmentManager fm) {
            super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position)
            {
                case 1:
                    Bundle bundle=new Bundle();
                    bundle.putString(SYSTEM_APPS,SYSTEM_APPS);
                    systemAppListFragment=new AppManagerListFragment();
                    systemAppListFragment.setArguments(bundle);
                    return systemAppListFragment;
                default:
                    return new AppManagerListFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position)
            {
                case 1:
                    return "System Apps";
                default:
                    return "User-installed Apps";
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(keyBoardUtil.getKeyBoardVisibility())
        {
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
        }
        else if(search_toolbar_visible)
        {
            set_visibility_searchbar(false);
        }
        else
        {
            userAppListFragment.clear_selection();
            systemAppListFragment.clear_selection();
            finish();
        }

    }


    interface SearchFilterListener
    {
        void onSearchFilter(String constraint);
    }

    public void addSearchFilterListener(SearchFilterListener listener)
    {
        searchFilterListeners.add(listener);
    }

    public void removeSearchFilterListener(SearchFilterListener listener)
    {
        searchFilterListeners.remove(listener);
    }

    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}