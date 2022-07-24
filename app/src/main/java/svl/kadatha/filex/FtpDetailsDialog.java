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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
    private List<FtpPOJO> ftpPJO_selected_for_delete=new ArrayList<>();
    private boolean toolbar_visible=true;
    private int scroll_distance;
    private Button delete_btn,rename_btn,edit_btn;
    private Handler handler;
    private PermissionsUtil permissionsUtil;
    private final static String FTP_DELETE_REQUEST_CODE="ftp_delete_request_code";


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();
        ftpDatabaseHelper=new FtpDatabaseHelper(context);
        permissionsUtil=new PermissionsUtil(context,(AppCompatActivity)context );
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ftpDatabaseHelper.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        //this.setRetainInstance(true);
        setCancelable(false);
        handler=new Handler();
        asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
        progressBarFragment=ProgressBarFragment.newInstance();
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


        Button scan_btn=v.findViewById(R.id.fragment_ftp_scan_btn);
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,4,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.document_add_icon,R.drawable.delete_icon,R.drawable.rename_icon,R.drawable.edit_icon};
        String [] titles=new String[]{getString(R.string.new_),getString(R.string.delete),getString(R.string.rename),getString(R.string.edit)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);
        bottom_toolbar=v.findViewById(R.id.fragment_ftp_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button add_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        delete_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        rename_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        edit_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);
        BottomToolbarClickListener bottomToolbarClickListener=new BottomToolbarClickListener();
        add_btn.setOnClickListener(bottomToolbarClickListener);
        delete_btn.setOnClickListener(bottomToolbarClickListener);
        rename_btn.setOnClickListener(bottomToolbarClickListener);
        edit_btn.setOnClickListener(bottomToolbarClickListener);
        Handler handler=new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    ftpListAdapter=new FtpListAdapter();
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

        int size=mselecteditems.size();
        enable_disable_buttons(size != 0, size);

        fragmentManager.setFragmentResultListener(FTP_DELETE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(FTP_DELETE_REQUEST_CODE))
                {
                    ftpPJO_selected_for_delete=new ArrayList<>();
                    ftpPJO_selected_for_delete.addAll(ftpPOJO_selected_array);
                    clear_selection();
                    new DeleteFtpPOJOAsyncTask(ftpPJO_selected_for_delete).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    /*
    @Override
    public void onDestroyView() {
        if(getDialog()!=null && getRetainInstance())
        {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

     */

    public void clear_selection()
    {
        mselecteditems=new SparseBooleanArray();
        ftpPOJO_selected_array=new ArrayList<>();

        if(ftpListAdapter!=null)ftpListAdapter.notifyDataSetChanged();
        enable_disable_buttons(false,0);
        /*
        file_number_view.setText(mselecteditems.size()+"/"+num_all_audio_list);
        all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
         */
    }


    private class FtpListAdapter extends RecyclerView.Adapter<FtpListAdapter.VH>
    {

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
                            if(!permissionsUtil.isNetworkConnected())
                            {
                                Global.print(context,getString(R.string.not_connected_to_network));
                                return;
                            }
                            progressBarFragment=ProgressBarFragment.newInstance();
                            progressBarFragment.show(fragmentManager,"");

                            FtpPOJO ftpPOJO=ftpPOJOList.get(pos);

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.FTP_CLIENT=new FTPClient();
                                        boolean status;
                                        try {
                                            MainActivity.FTP_CLIENT.connect(ftpPOJO.server,ftpPOJO.port);
                                            if(FTPReply.isPositiveCompletion(MainActivity.FTP_CLIENT.getReplyCode()))
                                            {
                                                status=MainActivity.FTP_CLIENT.login(ftpPOJO.user_name,ftpPOJO.password);
                                                if(status)
                                                {
                                                    MainActivity.FTP_CLIENT.setFileType(FTP.BINARY_FILE_TYPE);
                                                    //if(ftpPOJO.mode.equals("passive"))
                                                    {
                                                        MainActivity.FTP_CLIENT.enterLocalPassiveMode();
                                                    }
                                                    String path=MainActivity.FTP_CLIENT.printWorkingDirectory();

                                                    Iterator<FilePOJO> iterator=Global.STORAGE_DIR.iterator();
                                                    while(iterator.hasNext())
                                                    {
                                                        if(iterator.next().getFileObjectType()==FileObjectType.FTP_TYPE)
                                                        {
                                                            iterator.remove();
                                                        }
                                                    }
                                                    Global.STORAGE_DIR.add(FilePOJOUtil.MAKE_FilePOJO(FileObjectType.FTP_TYPE,path));

                                                    Iterator<FilePOJO> iterator1=MainActivity.RECENTS.iterator();
                                                    while (iterator1.hasNext())
                                                    {
                                                        if(iterator1.next().getFileObjectType()==FileObjectType.FTP_TYPE)
                                                        {
                                                            iterator1.remove();
                                                        }
                                                    }

                                                    FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""),FileObjectType.FTP_TYPE);


                                                    handler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            ((MainActivity)context).storageRecyclerAdapter.notifyDataSetChanged();
                                                            ((MainActivity)context).createFragmentTransaction(path,FileObjectType.FTP_TYPE);
                                                        }
                                                    });


                                                    dismissAllowingStateLoss();
                                                }

                                                else
                                                {
                                                    handler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Global.print(context,getString(R.string.server_could_not_be_connected));
                                                        }
                                                    });
                                                    progressBarFragment.dismissAllowingStateLoss();
                                                }

                                            }
                                            progressBarFragment.dismissAllowingStateLoss();
                                        } catch (IOException e) {
                                                handler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Global.print(context,getString(R.string.server_could_not_be_connected));
                                                    }
                                                });
                                                progressBarFragment.dismissAllowingStateLoss();
                                        }

                                    }
                                }).start();
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
                        enable_disable_buttons(true,size);
                    }

                    if(size==0)
                    {
                        enable_disable_buttons(false,size);
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

                    ++size;
                    enable_disable_buttons(true,size);
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


    private void enable_disable_buttons(boolean enable, int selection_size)
    {

        if(enable)
        {
            delete_btn.setAlpha(Global.ENABLE_ALFA);
            if(selection_size==1)
            {
                rename_btn.setAlpha(Global.ENABLE_ALFA);
                edit_btn.setAlpha(Global.ENABLE_ALFA);
            }
            else
            {
                rename_btn.setAlpha(Global.DISABLE_ALFA);
                edit_btn.setAlpha(Global.DISABLE_ALFA);
            }

        }
        else
        {
            delete_btn.setAlpha(Global.DISABLE_ALFA);
            rename_btn.setAlpha(Global.DISABLE_ALFA);
            edit_btn.setAlpha(Global.DISABLE_ALFA);
        }
        delete_btn.setEnabled(enable);
        rename_btn.setEnabled(enable && selection_size==1);
        edit_btn.setEnabled(enable && selection_size==1);

    }


    private class BottomToolbarClickListener implements View.OnClickListener
    {

        @Override
        public void onClick(View v) {
            int id=v.getId();
            if(id==R.id.toolbar_btn_1)
            {
                clear_selection();
                FtpDetailsInputDialog ftpDetailsInputDialog=FtpDetailsInputDialog.getInstance(null);
                ftpDetailsInputDialog.setFtpDatabaseModificationListener(new FtpDetailsInputDialog.FtpDatabaseModificationListener() {
                    @Override
                    public void onInsert(FtpPOJO ftpPOJO) {
                        Iterator<FtpPOJO> iterator=ftpPOJOList.iterator();
                        while(iterator.hasNext())
                        {
                            if(iterator.next().server.equals(ftpPOJO.server))
                            {
                                iterator.remove();
                                break;
                            }
                        }
                        ftpPOJOList.add(ftpPOJO);
                        ftpListAdapter.notifyDataSetChanged();
                    }

                });
                ftpDetailsInputDialog.show(fragmentManager,"");
            }
            if(id==R.id.toolbar_btn_2)
            {
                int s=mselecteditems.size();
                if(s>0)
                {
                    FtpPOJO ftpPOJO=ftpPOJO_selected_array.get(0);
                    String display=ftpPOJO.display;
                    DeleteFtpAlertDialog deleteFtpAlertDialog=DeleteFtpAlertDialog.getInstance(FTP_DELETE_REQUEST_CODE,(display==null || display.equals("")) ? ftpPOJO.server : display,s);
                    /*
                    deleteFtpAlertDialog.setDeleteFtpAlertDialogListener(new DeleteFtpAlertDialog.DeleteFtpAlertDialogListener() {
                        @Override
                        public void onOkClick() {
                            ftpPJO_selected_for_delete=new ArrayList<>();
                            ftpPJO_selected_for_delete.addAll(ftpPOJO_selected_array);
                            clear_selection();
                            new DeleteFtpPOJOAsyncTask(ftpPJO_selected_for_delete).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        @Override
                        public void onCancelClick() {
                            clear_selection();
                        }
                    });

                     */
                    deleteFtpAlertDialog.show(fragmentManager,"");
                }


            }
            else if(id==R.id.toolbar_btn_3)
            {
                int s=mselecteditems.size();
                if(s==1)
                {
                    FtpPOJO ftpPOJO=ftpPOJO_selected_array.get(0);
                    int idx=ftpPOJOList.indexOf(ftpPOJO);
                    FtpDisplayRenameDialog ftpDisplayRenameDialog=FtpDisplayRenameDialog.getInstance(ftpPOJO.server,ftpPOJO.display);
                    ftpDisplayRenameDialog.setFtpRenameListener(new FtpDisplayRenameDialog.FtpRenameListener() {
                        @Override
                        public void onRenameFtp(String new_name) {
                                ftpPOJO.display=new_name;
                                ftpListAdapter.notifyItemChanged(idx);
                        }
                    });
                    ftpDisplayRenameDialog.show(fragmentManager,"");
                }
                clear_selection();
            }
            else if(id==R.id.toolbar_btn_4)
            {
                int s=mselecteditems.size();
                if(s==1)
                {
                    FtpPOJO tobe_replaced_ftp=ftpPOJO_selected_array.get(0);
                    String ftp_server=tobe_replaced_ftp.server;
                    int idx=ftpPOJOList.indexOf(tobe_replaced_ftp);
                    FtpDetailsInputDialog ftpDetailsInputDialog=FtpDetailsInputDialog.getInstance(ftp_server);
                    ftpDetailsInputDialog.setFtpDatabaseModificationListener(new FtpDetailsInputDialog.FtpDatabaseModificationListener() {
                        @Override
                        public void onInsert(FtpPOJO ftpPOJO) {

                            Iterator<FtpPOJO> iterator=ftpPOJOList.iterator();
                            while(iterator.hasNext())
                            {
                                if(iterator.next().server.equals(ftp_server))
                                {

                                    iterator.remove();
                                    break;
                                }
                            }
                            iterator=ftpPOJOList.iterator();
                            while(iterator.hasNext())
                            {
                                if(iterator.next().server.equals(ftpPOJO.server))
                                {

                                    iterator.remove();
                                    break;
                                }
                            }
                            int max_idx=ftpPOJOList.size();
                            ftpPOJOList.add(Math.min(idx, max_idx),ftpPOJO);
                            ftpListAdapter.notifyDataSetChanged();
                        }

                    });
                    ftpDetailsInputDialog.show(fragmentManager,"");
                }

                clear_selection();
            }
        }
    }

    private class DeleteFtpPOJOAsyncTask extends AsyncTask<Void, Void,Void>
    {
        private final List<FtpPOJO> ftpPOJOS_for_delete;
        private List<String> ftp_deleted_server;

        DeleteFtpPOJOAsyncTask(List<FtpPOJO> list)
        {
            ftpPOJOS_for_delete=list;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBarFragment=ProgressBarFragment.newInstance();
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
        final String server;
        final String mode;
        final String user_name;
        final String password;
        final String encoding;
        String display;
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

}