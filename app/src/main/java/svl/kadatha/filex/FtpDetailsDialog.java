package svl.kadatha.filex;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
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
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public List<FtpPOJO> ftpPOJO_selected_array=new ArrayList<>();
    private final List<FtpPOJO> ftpPJO_selected_for_delete=new ArrayList<>();
    private boolean toolbar_visible=true;
    private int scroll_distance;

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
        ftp_list_recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener()
        {

            final int threshold=5;
            public void onScrolled(RecyclerView rv, int dx, int dy)
            {
                super.onScrolled(rv,dx,dy);
                if(scroll_distance>threshold && toolbar_visible)
                {

                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    toolbar_visible=false;
                    scroll_distance=0;
                }
                else if(scroll_distance<-threshold && !toolbar_visible)
                {

                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible=true;
                    scroll_distance=0;
                }

                if((toolbar_visible && dy>0) || (!toolbar_visible && dy<0))
                {
                    scroll_distance+=dy;
                }

            }

        });





        Button add_btn=v.findViewById(R.id.fragment_ftp_add_btn);
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear_selection();
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

    public void clear_selection()
    {
        mselecteditems=new SparseBooleanArray();
        ftpPOJO_selected_array=new ArrayList<>();

        if(ftpListAdapter!=null)ftpListAdapter.notifyDataSetChanged();
        /*
        enable_disable_buttons(false);
        file_number_view.setText(mselecteditems.size()+"/"+num_all_audio_list);
        all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);

         */
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
            boolean item_selected=mselecteditems.get(position,false);
            holder.v.setSelected(item_selected);
            holder.ftp_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);


        }

        @Override
        public int getItemCount() {
            return ftpPOJOList.size();
        }

        private class VH extends RecyclerView.ViewHolder
        {
            final View v;
            final ImageView ftp_image;
            final ImageView ftp_select_indicator;
            final TextView ftp_display;
            final TextView ftp_server;
            int pos;
            VH(View view)
            {
                super(view);
                v=view;
                ftp_image=v.findViewById(R.id.ftp_list_recyclerview_image_ftp);
                ftp_display=v.findViewById(R.id.ftp_list_recyclerview_display);
                ftp_server=v.findViewById(R.id.ftp_list_recyclerview_server);
                ftp_select_indicator=v.findViewById(R.id.ftp_list_recyclerview_select_indicator);
                v.setOnClickListener(new View.OnClickListener()
                {

                    public void onClick(View p)
                    {
                        pos=getBindingAdapterPosition();
                        int size=mselecteditems.size();
                        if(size>0)
                        {

                            onLongClickProcedure(p,size);
                        }
                        else
                        {
                            ProgressBarFragment pbf=ProgressBarFragment.getInstance();
                            pbf.show(fragmentManager,"");

                     //to open detailfragment

                            pbf.dismissAllowingStateLoss();
                        }
                    }

                });


                view.setOnLongClickListener(new View.OnLongClickListener()
                {
                    public boolean onLongClick(View p)
                    {
                        onLongClickProcedure(p,mselecteditems.size());
                        return true;

                    }
                });
            }


            private void onLongClickProcedure(View v, int size)
            {
                pos=getBindingAdapterPosition();
                if(mselecteditems.get(pos,false))
                {
                    v.setSelected(false);
                    ftp_select_indicator.setVisibility(View.INVISIBLE);
                    ftpPOJO_selected_array.remove(ftpPOJOList.get(pos));
                    mselecteditems.delete(pos);
                    --size;
                    if(size>=1)
                    {
                        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                        toolbar_visible=true;
                        scroll_distance=0;
                        //enable_disable_buttons(true);
                    }

                    if(size==0)
                    {
                        //enable_disable_buttons(false);
                        //all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
                    }
                }
                else
                {
                    v.setSelected(true);
                    ftp_select_indicator.setVisibility(View.VISIBLE);
                    ftpPOJO_selected_array.add(ftpPOJOList.get(pos));
                    mselecteditems.put(pos,true);

                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible=true;
                    scroll_distance=0;
                    //enable_disable_buttons(true);
                    ++size;
                    /*
                    if(size==num_all_audio_list)
                    {
                        all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
                    }

                     */

                }
                //file_number_view.setText(size+"/"+num_all_audio_list);
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
                if(mselecteditems.size()>0)
                {

                    ftpPJO_selected_for_delete.addAll(ftpPOJO_selected_array);
                    clear_selection();
                    new DeleteFtpPOJOAsyncTask(ftpPJO_selected_for_delete).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }


            }
            else if(id==R.id.toolbar_btn_2)
            {

            }
            else if(id==R.id.toolbar_btn_3)
            {

            }
        }
    }

    private class DeleteFtpPOJOAsyncTask extends AsyncTask<Void, Void,Void>
    {
        private final List<FtpPOJO> ftpPOJOS_for_delete;

        DeleteFtpPOJOAsyncTask(List<FtpPOJO> list)
        {
            ftpPOJOS_for_delete=list;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBarFragment=ProgressBarFragment.getInstance();
            progressBarFragment.show(fragmentManager,"");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int size=ftpPOJOS_for_delete.size();
            for(int i=0;i<size;++i)
            {
                FtpPOJO ftpPOJO=ftpPOJOS_for_delete.get(i);
                int j=ftpDatabaseHelper.delete(ftpPOJO.server);
                if(j>0)
                {
                    ftpPOJOList.remove(ftpPOJO);
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            progressBarFragment.dismissAllowingStateLoss();
            ftpListAdapter.notifyDataSetChanged();
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