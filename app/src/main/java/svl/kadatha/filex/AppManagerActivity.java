package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

public class AppManagerActivity extends BaseActivity{

    private Context context;
    public FragmentManager fm;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton floatingActionButton;
    private AppManagerListFragment userAppListFragment,systemAppListFragment;
    private AppManagementFragmentAdapter adapter;
    public static final String USER_INSTALLED_APPS="user_installed_apps";
    public static final String SYSTEM_APPS="system_apps";
    public boolean search_toolbar_visible;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);
        context=this;
        fm=getSupportFragmentManager();
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
        finish();
    }
}
