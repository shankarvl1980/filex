package svl.kadatha.filex;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FtpDetailsDialog extends DialogFragment {

    private Context context;
    private Toolbar bottom_toolbar;
    private RecyclerView ftp_list_recyclerview;
    private FtpListAdapter ftpListAdapter;
    private List<FtpDetailsDialog.FtpPOJO> ftpPJO_selected_for_delete=new ArrayList<>();
    private boolean toolbar_visible=true;
    private int scroll_distance;
    private int num_all_ftp;
    private Button delete_btn;
    private Button edit_btn;
    private PermissionsUtil permissionsUtil;
    private FrameLayout progress_bar;
    private TextView ftp_number_text_view,empty_ftp_list_tv;
    private FtpDetailsViewModel viewModel;
    private final static String FTP_DELETE_REQUEST_CODE="ftp_delete_request_code";
    public final static String FTP_INPUT_DETAILS_REQUEST_CODE="ftp_input_details_request_code";
    //private final static String FTP_INPUT_DETAILS_REQUEST_CODE_NON_NULL="ftp_input_details_request_code_non_null";
    private final static String FTP_RENAME_REQUEST_CODE="ftp_rename_request_code";


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        permissionsUtil=new PermissionsUtil(context,(AppCompatActivity)context );
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_ftp_list,container,false);
        progress_bar=v.findViewById(R.id.fragment_ftp_list_progressbar);
        ftp_number_text_view=v.findViewById(R.id.ftp_details_ftp_number);
        empty_ftp_list_tv=v.findViewById(R.id.ftp_details_empty);
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

        FloatingActionButton floatingActionButton = v.findViewById(R.id.floating_action_ftp_list);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(viewModel.mselecteditems.size()>0)
                {
                    clear_selection();
                }
                else
                {
                    dismissAllowingStateLoss();
                }

            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,4,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.document_add_icon,R.drawable.delete_icon,R.drawable.connect_icon,R.drawable.edit_icon};
        String [] titles=new String[]{getString(R.string.new_),getString(R.string.delete),getString(R.string.disconnect),getString(R.string.edit)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);
        bottom_toolbar=v.findViewById(R.id.fragment_ftp_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button add_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        delete_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button disconnect_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        edit_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);


        BottomToolbarClickListener bottomToolbarClickListener=new BottomToolbarClickListener();
        add_btn.setOnClickListener(bottomToolbarClickListener);
        delete_btn.setOnClickListener(bottomToolbarClickListener);
        disconnect_btn.setOnClickListener(bottomToolbarClickListener);
        edit_btn.setOnClickListener(bottomToolbarClickListener);

        viewModel=new ViewModelProvider(this).get(FtpDetailsViewModel.class);
        viewModel.fetchFtpPojoList();

        int size=viewModel.mselecteditems.size();
        ftp_number_text_view.setText(size+"/"+num_all_ftp);
        enable_disable_buttons(size != 0, size);

        viewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                    ftpListAdapter=new FtpListAdapter();
                    ftp_list_recyclerview.setAdapter(ftpListAdapter);
                    num_all_ftp=viewModel.ftpPOJOList.size();
                    if(num_all_ftp==0)
                    {
                        ftp_list_recyclerview.setVisibility(View.GONE);
                        empty_ftp_list_tv.setVisibility(View.VISIBLE);
                    }
                    ftp_number_text_view.setText(viewModel.mselecteditems.size()+"/"+num_all_ftp);
                }
            }
        });

        viewModel.deleteAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                    num_all_ftp = viewModel.ftpPOJOList.size();
                    if (num_all_ftp == 0) {
                        ftp_list_recyclerview.setVisibility(View.GONE);
                        empty_ftp_list_tv.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        ftp_list_recyclerview.setVisibility(View.VISIBLE);
                        empty_ftp_list_tv.setVisibility(View.GONE);
                    }
                    clear_selection();
                    viewModel.deleteAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.ftpConnectAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                    if(viewModel.loggedInStatus)
                    {
                        viewModel.loggedInStatus=false;
                        viewModel.ftpConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                        if(FtpDetailsViewModel.FTP_WORKING_DIR_PATH!=null)
                        {
                            ((MainActivity)context).storageRecyclerAdapter.notifyDataSetChanged();
                            ((MainActivity)context).createFragmentTransaction(FtpDetailsViewModel.FTP_WORKING_DIR_PATH,FileObjectType.FTP_TYPE);
                            dismissAllowingStateLoss();
                        }
                        else {
                            Global.print(context,getString(R.string.server_could_not_be_connected));
                        }

                    }
                    else {
                        viewModel.ftpConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    }

                }
            }
        });

        viewModel.replaceAndConnectFtpAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                    if(viewModel.loggedInStatus)
                    {
                        viewModel.loggedInStatus=false;
                        viewModel.ftpConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                        if(FtpDetailsViewModel.FTP_WORKING_DIR_PATH!=null)
                        {
                            ((MainActivity)context).storageRecyclerAdapter.notifyDataSetChanged();
                            ((MainActivity)context).createFragmentTransaction(FtpDetailsViewModel.FTP_WORKING_DIR_PATH,FileObjectType.FTP_TYPE);
                            dismissAllowingStateLoss();
                        }
                        else {
                            Global.print(context,getString(R.string.server_could_not_be_connected));
                        }


                    }
                    else {
                        viewModel.replaceAndConnectFtpAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    }

                }
            }
        });

        viewModel.replaceFtpAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                    num_all_ftp = viewModel.ftpPOJOList.size();
                    if (num_all_ftp == 0) {
                        ftp_list_recyclerview.setVisibility(View.GONE);
                        empty_ftp_list_tv.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        ftp_list_recyclerview.setVisibility(View.VISIBLE);
                        empty_ftp_list_tv.setVisibility(View.GONE);
                    }
                    clear_selection();
                    viewModel.replaceFtpAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.changeFtpDisplayAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                    clear_selection();
                    viewModel.changeFtpDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });



        getParentFragmentManager().setFragmentResultListener(FTP_DELETE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(FTP_DELETE_REQUEST_CODE))
                {
                    progress_bar.setVisibility(View.VISIBLE);
                    ftpPJO_selected_for_delete=new ArrayList<>();
                    ftpPJO_selected_for_delete.addAll(viewModel.mselecteditems.values());
                    viewModel.deleteFtpPojo(ftpPJO_selected_for_delete);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(FTP_INPUT_DETAILS_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(FTP_INPUT_DETAILS_REQUEST_CODE))
                {
                    progress_bar.setVisibility(View.VISIBLE);
                    if(result.getBoolean("whetherToConnect"))
                    {
                        viewModel.replaceAndConnectFtpPojoList(result);
                    }
                    else {
                        viewModel.replaceFtpPojoList(result);
                    }

                }
            }
        });


        getParentFragmentManager().setFragmentResultListener(FTP_RENAME_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(FTP_RENAME_REQUEST_CODE))
                {
                    progress_bar.setVisibility(View.VISIBLE);
                    String new_name=result.getString("new_name");
                    String server=result.getString("server");
                    String user_name=result.getString("user_name");
                    if(new_name!=null)viewModel.changeFtpPojoDisplay(server,user_name,new_name);
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


    public void clear_selection()
    {
        viewModel.mselecteditems=new IndexedLinkedHashMap<>();
        //viewModel.ftpPOJO_selected_array=new ArrayList<>();

        if(ftpListAdapter!=null)ftpListAdapter.notifyDataSetChanged();
        enable_disable_buttons(false,0);

        ftp_number_text_view.setText(viewModel.mselecteditems.size()+"/"+num_all_ftp);
        //all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
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
            FtpPOJO ftpPOJO=viewModel.ftpPOJOList.get(position);
            String display=ftpPOJO.display;
            String server=ftpPOJO.server;
            String user_name=ftpPOJO.user_name;
            holder.ftp_display.setText((display==null || display.equals("")) ? server : display);
            holder.ftp_server.setText(server);
            holder.ftp_user_name.setText(getString(R.string.user)+" - "+user_name);
            boolean item_selected=viewModel.mselecteditems.containsKey(position);
            holder.v.setSelected(item_selected);
            holder.ftp_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getItemCount() {
            return viewModel.ftpPOJOList.size();
        }

        private class VH extends RecyclerView.ViewHolder
        {
            final View v;
            final ImageView ftp_image;
            final ImageView ftp_select_indicator;
            final TextView ftp_display;
            final TextView ftp_server;
            final TextView ftp_user_name;
            int pos;
            VH(View view)
            {
                super(view);
                v=view;
                ftp_image=v.findViewById(R.id.ftp_list_recyclerview_image_ftp);
                ftp_display=v.findViewById(R.id.ftp_list_recyclerview_display);
                ftp_server=v.findViewById(R.id.ftp_list_recyclerview_server);
                ftp_user_name=v.findViewById(R.id.ftp_list_recyclerview_user_name);
                ftp_select_indicator=v.findViewById(R.id.ftp_list_recyclerview_select_indicator);
                v.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View p)
                    {
                        pos=getBindingAdapterPosition();
                        int size=viewModel.mselecteditems.size();
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
                            if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_FTP(FileObjectType.FTP_TYPE,FileObjectType.FTP_TYPE))
                            {
                                Global.print(context,getString(R.string.wait_till_current_service_on_ftp_finishes));
                                return;
                            }
                            progress_bar.setVisibility(View.VISIBLE);
                            FtpPOJO ftpPOJO=viewModel.ftpPOJOList.get(pos);
                            viewModel.connectFtp(ftpPOJO);
                        }
                    }
                });


                view.setOnLongClickListener(new View.OnLongClickListener()
                {
                    public boolean onLongClick(View p)
                    {
                        onLongClickProcedure(p,viewModel.mselecteditems.size());
                        return true;
                    }
                });
            }


            private void onLongClickProcedure(View v, int size)
            {
                pos=getBindingAdapterPosition();
                if(viewModel.mselecteditems.containsKey(pos))
                {
                    v.setSelected(false);
                    ftp_select_indicator.setVisibility(View.INVISIBLE);
                    //viewModel.ftpPOJO_selected_array.remove(viewModel.ftpPOJOList.get(pos));
                    viewModel.mselecteditems.remove(pos);
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
                    //viewModel.ftpPOJO_selected_array.add(viewModel.ftpPOJOList.get(pos));
                    viewModel.mselecteditems.put(pos,viewModel.ftpPOJOList.get(pos));

                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible=true;
                    scroll_distance=0;

                    ++size;
                    enable_disable_buttons(true,size);

                }
                ftp_number_text_view.setText(size+"/"+num_all_ftp);
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
                //rename_btn.setAlpha(Global.ENABLE_ALFA);
                edit_btn.setAlpha(Global.ENABLE_ALFA);
            }
            else
            {
                //rename_btn.setAlpha(Global.DISABLE_ALFA);
                edit_btn.setAlpha(Global.DISABLE_ALFA);
            }
        }
        else
        {
            delete_btn.setAlpha(Global.DISABLE_ALFA);
            //rename_btn.setAlpha(Global.DISABLE_ALFA);
            edit_btn.setAlpha(Global.DISABLE_ALFA);
        }
        delete_btn.setEnabled(enable);
        //rename_btn.setEnabled(enable && selection_size==1);
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
                FtpDetailsInputDialog ftpDetailsInputDialog=FtpDetailsInputDialog.getInstance(FTP_INPUT_DETAILS_REQUEST_CODE,null,null);
                ftpDetailsInputDialog.show(getParentFragmentManager(),"");
            }
            else if(id==R.id.toolbar_btn_2)
            {
                int s=viewModel.mselecteditems.size();
                if(s>0)
                {
                    FtpPOJO ftpPOJO=viewModel.mselecteditems.getValueAtIndex(0);
                    String display=ftpPOJO.display;
                    DeleteFtpAlertDialog deleteFtpAlertDialog=DeleteFtpAlertDialog.getInstance(FTP_DELETE_REQUEST_CODE,(display==null || display.equals("")) ? ftpPOJO.server : display,s);
                    deleteFtpAlertDialog.show(getParentFragmentManager(),"");
                }
            }
            else if(id==R.id.toolbar_btn_3)
            {
                FtpClientRepository.getInstance().disconnect_ftp_clients();
                Global.print(context, "ftp connection disconnected");

            }
            else if(id==R.id.toolbar_btn_4)
            {
                int s=viewModel.mselecteditems.size();
                if(s==1)
                {
                    FtpPOJO tobe_replaced_ftp=viewModel.mselecteditems.getValueAtIndex(0);
                    String ftp_server=tobe_replaced_ftp.server;
                    String ftp_user_name=tobe_replaced_ftp.user_name;
                    FtpDetailsInputDialog ftpDetailsInputDialog=FtpDetailsInputDialog.getInstance(FTP_INPUT_DETAILS_REQUEST_CODE,ftp_server,ftp_user_name);
                    ftpDetailsInputDialog.show(getParentFragmentManager(),"");
                }

                clear_selection();
            }
            else if(id==R.id.toolbar_btn_5)
            {
                dismissAllowingStateLoss();
            }
        }
    }


    public static class FtpPOJO
    {
        final String server;
        final String mode;
        final String user_name;
        final String password;
        final String type;
        final String encoding;
        final String display;
        final int port;
        final boolean anonymous;

        FtpPOJO(String server,int port,String mode, String user_name,String password,String type,boolean anonymous,String encoding, String display)
        {
            this.server=server;
            this.port=port;
            this.mode=mode;
            this.user_name=user_name;
            this.password=password;
            this.type=type;
            this.anonymous=anonymous;
            this.encoding=encoding;
            this.display=display;
        }
    }

}