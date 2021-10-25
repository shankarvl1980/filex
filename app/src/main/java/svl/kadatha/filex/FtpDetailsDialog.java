package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FtpDetailsDialog extends DialogFragment {

    private Context context;
    private Toolbar bottom_toolbar;
    private FragmentManager fragmentManager;
    private List<FtpPOJO> ftpPOJOList;
    private RecyclerView ftp_list_recyclerview;
    private AsyncTaskStatus asyncTaskStatus;
    private ProgressBarFragment progressBarFragment;
    private FtpDatabaseHelper ftpDatabaseHelper;
    private FtpListAdapter ftpListAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
        ftpDatabaseHelper=new FtpDatabaseHelper(context);
        asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
        progressBarFragment=ProgressBarFragment.getInstance();
        progressBarFragment.show(fragmentManager,"");
        new Thread(new Runnable() {
            @Override
            public void run() {
                asyncTaskStatus=AsyncTaskStatus.STARTED;
                ftpPOJOList=ftpDatabaseHelper.getFtpPOJOlist();
                asyncTaskStatus=AsyncTaskStatus.COMPLETED;
            }
        }).start();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_ftp_list,container,false);
        ftp_list_recyclerview=v.findViewById(R.id.fragment_ftp_recyclerview);
        ftp_list_recyclerview.setLayoutManager(new LinearLayoutManager(context));
        ftp_list_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        Button add_btn=v.findViewById(R.id.fragment_ftp_add_btn);
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FtpDetailsInputDialog ftpDetailsInputDialog=FtpDetailsInputDialog.getInstance(null);
                ftpDetailsInputDialog.setFtpDatabaseModificationListener(new FtpDetailsInputDialog.FtpDatabaseModificationListener() {
                    @Override
                    public void onInsert(FtpPOJO ftpPOJO) {
                        ftpPOJOList.add(ftpPOJO);
                        ftpListAdapter.notifyDataSetChanged();
                    }

                });
                ftpDetailsInputDialog.show(fragmentManager,"");
            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,3,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.delete_icon,R.drawable.rename_icon,R.drawable.edit_icon};
        String [] titles=new String[]{getString(R.string.delete),getString(R.string.rename),getString(R.string.edit)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);
        bottom_toolbar=v.findViewById(R.id.fragment_ftp_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button delete_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button rename_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button edit_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        BottomToolbarClickListener bottomToolbarClickListener=new BottomToolbarClickListener();
        delete_btn.setOnClickListener(bottomToolbarClickListener);
        rename_btn.setOnClickListener(bottomToolbarClickListener);
        edit_btn.setOnClickListener(bottomToolbarClickListener);
        Handler handler=new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    ftpListAdapter=new FtpListAdapter(ftpPOJOList);
                    ftp_list_recyclerview.setAdapter(ftpListAdapter);
                    if(progressBarFragment!=null && progressBarFragment.getDialog()!=null)
                    {
                        progressBarFragment.dismissAllowingStateLoss();
                    }
                    handler.removeCallbacks(this);
                }
                else
                {
                    handler.postDelayed(this,50);
                }
            }
        });
        return v;
    }

    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(R.attr.dialog_recyclerview_background));

    }

    @Override
    public void onDestroy() {
        if(getDialog()!=null && getRetainInstance())
        {
            getDialog().setDismissMessage(null);
        }
        super.onDestroy();
        ftpDatabaseHelper.close();
    }



    private class FtpListAdapter extends RecyclerView.Adapter<FtpListAdapter.VH>
    {

        FtpListAdapter(List<FtpPOJO> ftpPOJOS)
        {

        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.ftp_list_recyclerview_layout,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FtpPOJO ftpPOJO=ftpPOJOList.get(position);
            String display=ftpPOJO.display;
            String server=ftpPOJO.server;
            holder.ftp_display.setText((display==null || display.equals("")) ? server : display);
            holder.ftp_server.setText(server);

        }

        @Override
        public int getItemCount() {
            return ftpPOJOList.size();
        }

        private class VH extends RecyclerView.ViewHolder
        {
            View v;
            ImageView ftp_image;
            TextView ftp_display, ftp_server;

            VH(View view)
            {
                super(view);
                v=view;
                ftp_image=v.findViewById(R.id.ftp_list_recyclerview_image_ftp);
                ftp_display=v.findViewById(R.id.ftp_list_recyclerview_display);
                ftp_server=v.findViewById(R.id.ftp_list_recyclerview_server);

                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        }
    }

    private class BottomToolbarClickListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v) {
            int id=v.getId();
            if(id==R.id.toolbar_btn_1)
            {

            }
            else if(id==R.id.toolbar_btn_2)
            {

            }
            else if(id==R.id.toolbar_btn_3)
            {

            }
        }
    }

    public static class FtpPOJO
    {
        final String server,mode,user_name,password,encoding,display;
        final int port;
        final boolean anonymous;

        FtpPOJO(String server,int port,String mode, String user_name,String password,boolean anonymous,String encoding, String display)
        {
            this.server=server;
            this.port=port;
            this.mode=mode;
            this.user_name=user_name;
            this.password=password;
            this.anonymous=anonymous;
            this.encoding=encoding;
            this.display=display;
        }
    }

    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}