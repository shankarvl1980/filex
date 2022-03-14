package svl.kadatha.filex;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AppListFragment extends Fragment {

    private Context context;
    private String app_type;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView app_count_textview;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setReenterTransition(true);
        this.context=context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            app_type=bundle.getString(AppManagerActivity.SYSTEM_APPS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_app_manager,container,false);
        app_count_textview=v.findViewById(R.id.fragment_app_list_number);
        recyclerView=v.findViewById(R.id.fragment_app_list_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        progressBar=v.findViewById(R.id.fragment_app_list_progressbar);
        return v;
    }
}
