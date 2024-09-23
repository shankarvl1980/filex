package svl.kadatha.filex;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NetworkAccountsDetailsDialog extends DialogFragment {

    private Context context;
    public final static String FTP="ftp";
    public final static String SFTP="sftp";
    public final static String WebDAV="webdav";
    public final static String SMB="smb";
    private String type;
    private Toolbar bottom_toolbar;
    private RecyclerView ftp_list_recyclerview;
    private NetworkAccountPojoListAdapter networkAccountPojoListAdapter;
    private List<NetworkAccountsDetailsDialog.NetworkAccountPOJO> networkAccountPOJO_selected_for_delete=new ArrayList<>();
    private boolean toolbar_visible=true;
    private int scroll_distance;
    private int num_all_ftp;
    private Button delete_btn,disconnect_btn;
    private Button edit_btn;
    private PermissionsUtil permissionsUtil;
    private FrameLayout progress_bar;
    private TextView ftp_number_text_view,empty_ftp_list_tv;
    private NetworkAccountDetailsViewModel viewModel;
    private final static String FTP_DELETE_REQUEST_CODE="ftp_delete_request_code";
    public final static String FTP_INPUT_DETAILS_REQUEST_CODE="ftp_input_details_request_code";
    private final static String FTP_RENAME_REQUEST_CODE="ftp_rename_request_code";


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        permissionsUtil=new PermissionsUtil(context,(AppCompatActivity)context );
    }

    public static NetworkAccountsDetailsDialog getInstance(String type){
        Bundle bundle=new Bundle();
        bundle.putString("type",type);
        NetworkAccountsDetailsDialog networkAccountsDetailsDialog =new NetworkAccountsDetailsDialog();
        networkAccountsDetailsDialog.setArguments(bundle);
        return networkAccountsDetailsDialog;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        if(getArguments()!=null)
        {
            type=getArguments().getString("type");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_ftp_list,container,false);
        progress_bar=v.findViewById(R.id.fragment_ftp_list_progressbar);
        TextView heading=v.findViewById(R.id.fragment_ftp_list_heading);
        if(type.equals(NetworkAccountsDetailsDialog.FTP)){
            heading.setText(R.string.ftp);
        }
        else if(type.equals(NetworkAccountsDetailsDialog.SFTP)){
            heading.setText(R.string.sftp);
        }
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

                if(!viewModel.mselecteditems.isEmpty())
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
        disconnect_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        edit_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);

        BottomToolbarClickListener bottomToolbarClickListener=new BottomToolbarClickListener();
        add_btn.setOnClickListener(bottomToolbarClickListener);
        delete_btn.setOnClickListener(bottomToolbarClickListener);
        disconnect_btn.setOnClickListener(bottomToolbarClickListener);
        edit_btn.setOnClickListener(bottomToolbarClickListener);

        viewModel=new ViewModelProvider(this).get(NetworkAccountDetailsViewModel.class);
        viewModel.type=type;
        viewModel.fetchNetworkAccountPojoList(type);
        if(viewModel.networkAccountPOJO!=null){
            progress_bar.setVisibility(View.VISIBLE);
            viewModel.testServiceConnection();
        }
        else{
            disconnect_btn.setAlpha(Global.DISABLE_ALFA);
            disconnect_btn.setEnabled(false);
        }

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
                    networkAccountPojoListAdapter =new NetworkAccountPojoListAdapter();
                    ftp_list_recyclerview.setAdapter(networkAccountPojoListAdapter);
                    num_all_ftp=viewModel.networkAccountPOJOList.size();
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
                    num_all_ftp = viewModel.networkAccountPOJOList.size();
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

        viewModel.networkConnectAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
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
                        viewModel.networkConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                        if(type.equals(FTP) && NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH.isEmpty()){
                            Global.print(context,getString(R.string.server_could_not_be_connected));
                            return;
                        } else if(type.equals(SFTP) && NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH.isEmpty()){
                            Global.print(context,getString(R.string.server_could_not_be_connected));
                            return;
                        }
                        initiateCreateFragmentTransactions();
                        dismissAllowingStateLoss();
                    }
                    else {
                        viewModel.networkConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    }
                }
            }
        });

        viewModel.replaceAndConnectNetworkAccountAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
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
                        viewModel.networkConnectAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                        if(type.equals(FTP) && NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH.isEmpty()){
                            Global.print(context,getString(R.string.server_could_not_be_connected));
                            return;
                        } else if(type.equals(SFTP) && NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH.isEmpty()){
                            Global.print(context,getString(R.string.server_could_not_be_connected));
                            return;
                        }
                        initiateCreateFragmentTransactions();
                        dismissAllowingStateLoss();
                    }
                    else {
                        viewModel.replaceAndConnectNetworkAccountAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    }
                }
            }
        });


        viewModel.replaceNetworkAccountAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    progress_bar.setVisibility(View.GONE);
                    num_all_ftp = viewModel.networkAccountPOJOList.size();
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
                    viewModel.replaceNetworkAccountAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.changeNetworkAccountDisplayAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
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
                    viewModel.changeNetworkAccountDisplayAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.testServiceConnectionAsyncTaskStatus.observe(this,new Observer<AsyncTaskStatus>(){
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if(asyncTaskStatus==AsyncTaskStatus.STARTED){
                    progress_bar.setVisibility(View.VISIBLE);
                }
                else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED){
                    progress_bar.setVisibility(View.GONE);
                    if(!viewModel.isNetworkConnected){
                        disconnect_btn.setAlpha(Global.DISABLE_ALFA);
                        disconnect_btn.setEnabled(false);
                    }
                    viewModel.testServiceConnectionAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(FTP_DELETE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(FTP_DELETE_REQUEST_CODE))
                {
                    progress_bar.setVisibility(View.VISIBLE);
                    networkAccountPOJO_selected_for_delete=new ArrayList<>();
                    networkAccountPOJO_selected_for_delete.addAll(viewModel.mselecteditems.values());
                    viewModel.deleteNetworkAccountPojo(networkAccountPOJO_selected_for_delete);
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
                        if(type.equals(FTP)){
                            viewModel.replaceAndConnectNetworkAccount(result);
                        }
                    }
                    else {
                        viewModel.replaceNetworkAccountPojoList(result);
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
                    int port=result.getInt("port");
                    String user_name=result.getString("user_name");
                    if(new_name!=null)viewModel.changeNetworkAccountPojoDisplay(server,port,user_name,new_name,type);
                }
            }
        });

        return v;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(R.attr.dialog_recyclerview_background));
    }

    private void initiateCreateFragmentTransactions(){
        AppCompatActivity appCompatActivity= (AppCompatActivity) getActivity();
        if(appCompatActivity instanceof MainActivity){
            ((MainActivity)context).storageRecyclerAdapter.notifyDataSetChanged();
            if(((MainActivity)context).recentDialogListener!=null){
                ((MainActivity)context).recentDialogListener.onMediaAttachedAndRemoved();
            }
            if(type.equals(FTP)){
                ((MainActivity)context).createNewFragmentTransaction(NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH,FileObjectType.FTP_TYPE);
            }else if(type.equals(SFTP)){
                ((MainActivity)context).createNewFragmentTransaction(NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH,FileObjectType.SFTP_TYPE);
            }
        }
        else if(appCompatActivity instanceof FileSelectorActivity){
            ///((FileSelectorActivity)context).storageRecyclerAdapter.notifyDataSetChanged();
            if(((FileSelectorActivity)context).recentDialogListener!=null){
                ((FileSelectorActivity)context).recentDialogListener.onMediaAttachedAndRemoved();
            }
            if(type.equals(FTP)){
                ((FileSelectorActivity)context).createNewFragmentTransaction(NetworkAccountDetailsViewModel.FTP_WORKING_DIR_PATH,FileObjectType.FTP_TYPE);
            } else if(type.equals(SFTP)){
                ((FileSelectorActivity)context).createNewFragmentTransaction(NetworkAccountDetailsViewModel.SFTP_WORKING_DIR_PATH,FileObjectType.SFTP_TYPE);
            }
        }
    }

    public void clear_selection()
    {
        viewModel.mselecteditems=new IndexedLinkedHashMap<>();

        if(networkAccountPojoListAdapter !=null) networkAccountPojoListAdapter.notifyDataSetChanged();
        enable_disable_buttons(false,0);
        ftp_number_text_view.setText(viewModel.mselecteditems.size()+"/"+num_all_ftp);
    }


    private class NetworkAccountPojoListAdapter extends RecyclerView.Adapter<NetworkAccountPojoListAdapter.VH>
    {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(context).inflate(R.layout.ftp_list_recyclerview_layout,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NetworkAccountsDetailsDialog.NetworkAccountPOJO networkAccountPOJO=viewModel.networkAccountPOJOList.get(position);
            String display=networkAccountPOJO.display;
            String server=networkAccountPOJO.server;
            String user_name=networkAccountPOJO.user_name;
            holder.ftp_display.setText((display==null || display.isEmpty()) ? server : display);
            holder.ftp_server.setText(server);
            holder.ftp_user_name.setText(getString(R.string.user)+" - "+user_name);
            boolean item_selected=viewModel.mselecteditems.containsKey(position);
            holder.v.setSelected(item_selected);
            holder.ftp_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getItemCount() {
            return viewModel.networkAccountPOJOList.size();
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
                            NetworkAccountPOJO networkAccountPOJO=viewModel.networkAccountPOJOList.get(pos);
                            if(type.equals(FTP)){
                                viewModel.connectNetworkAccount(networkAccountPOJO);
                            } else if(type.equals(SFTP)){
                                viewModel.connectNetworkAccount(networkAccountPOJO);
                            }
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
                    }
                }
                else
                {
                    v.setSelected(true);
                    ftp_select_indicator.setVisibility(View.VISIBLE);
                    viewModel.mselecteditems.put(pos,viewModel.networkAccountPOJOList.get(pos));

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
                edit_btn.setAlpha(Global.ENABLE_ALFA);
            }
            else
            {
                edit_btn.setAlpha(Global.DISABLE_ALFA);
            }
        }
        else
        {
            delete_btn.setAlpha(Global.DISABLE_ALFA);
            edit_btn.setAlpha(Global.DISABLE_ALFA);
        }
        delete_btn.setEnabled(enable);
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
                NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = NetworkAccountDetailsInputDialog.getInstance(FTP_INPUT_DETAILS_REQUEST_CODE,viewModel.type,null);
                networkAccountDetailsInputDialog.show(getParentFragmentManager(),"");
            }
            else if(id==R.id.toolbar_btn_2)
            {
                int s=viewModel.mselecteditems.size();
                if(s>0)
                {
                    NetworkAccountPOJO networkAccountPOJO=viewModel.mselecteditems.getValueAtIndex(0);
                    String display=networkAccountPOJO.display;
                    DeleteFtpAlertDialog deleteFtpAlertDialog=DeleteFtpAlertDialog.getInstance(FTP_DELETE_REQUEST_CODE,(display==null || display.isEmpty()) ? networkAccountPOJO.server : display,s);
                    deleteFtpAlertDialog.show(getParentFragmentManager(),"");
                }
            }
            else if(id==R.id.toolbar_btn_3)
            {
                if(viewModel.networkAccountPOJO==null)return;
                switch (type){
                    case FTP:

                        FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(viewModel.networkAccountPOJO);
                        ftpClientRepository.shutdown();
                        break;
                    case SFTP:
                        SftpChannelRepository sftpChannelRepository=SftpChannelRepository.getInstance(viewModel.networkAccountPOJO);
                        sftpChannelRepository.shutdown();
                        break;
                }

                disconnect_btn.setAlpha(Global.DISABLE_ALFA);
                disconnect_btn.setEnabled(false);
                Global.print(context, "ftp connection disconnected");
            }
            else if(id==R.id.toolbar_btn_4)
            {
                int s=viewModel.mselecteditems.size();
                if(s==1)
                {
                    NetworkAccountPOJO tobe_replaced_ftp=viewModel.mselecteditems.getValueAtIndex(0);
                    String ftp_server=tobe_replaced_ftp.server;
                    String ftp_user_name=tobe_replaced_ftp.user_name;
                    NetworkAccountDetailsInputDialog networkAccountDetailsInputDialog = NetworkAccountDetailsInputDialog.getInstance(FTP_INPUT_DETAILS_REQUEST_CODE,viewModel.type,viewModel.networkAccountPOJOList.get(viewModel.mselecteditems.getKeyAtIndex(0)));
                    networkAccountDetailsInputDialog.show(getParentFragmentManager(),"");
                }
                clear_selection();
            }
            else if(id==R.id.toolbar_btn_5)
            {
                dismissAllowingStateLoss();
            }
        }
    }


    public static class NetworkAccountPOJO implements Parcelable {
        final String server;
        final int port;
        final String user_name;
        final String password;
        final String encoding;
        final String display;
        final String type;

        // FTP-specific fields
        final String mode;
        final boolean anonymous;
        final boolean useFTPS;

        // SFTP-specific fields
        final String privateKeyPath;
        final String privateKeyPassphrase;
        final String knownHostsPath;

        // WebDAV-specific fields
        final String basePath;
        final boolean useHTTPS;

        // SMB-specific fields
        final String domain;
        final String shareName;
        final String smbVersion;

        // Constructor
        public NetworkAccountPOJO(String server, int port, String user_name, String password,
                                  String encoding, String display, String type,
                                  String mode, boolean anonymous, boolean useFTPS,
                                  String privateKeyPath, String privateKeyPassphrase, String knownHostsPath,
                                  String basePath, boolean useHTTPS,
                                  String domain, String shareName, String smbVersion) {
            this.server = server;
            this.port = port;
            this.user_name = user_name;
            this.password = password;
            this.encoding = encoding;
            this.display = display;
            this.type = type;
            this.mode = mode;
            this.anonymous = anonymous;
            this.useFTPS = useFTPS;
            this.privateKeyPath = privateKeyPath;
            this.privateKeyPassphrase = privateKeyPassphrase;
            this.knownHostsPath = knownHostsPath;
            this.basePath = basePath;
            this.useHTTPS = useHTTPS;
            this.domain = domain;
            this.shareName = shareName;
            this.smbVersion = smbVersion;
        }

        // Copy Constructor for deep copy
        public NetworkAccountPOJO(NetworkAccountPOJO other) {
            this.server = other.server != null ? new String(other.server) : null;
            this.port = other.port;
            this.user_name = other.user_name != null ? new String(other.user_name) : null;
            this.password = other.password != null ? new String(other.password) : null;
            this.encoding = other.encoding != null ? new String(other.encoding) : null;
            this.display = other.display != null ? new String(other.display) : null;
            this.type = other.type != null ? new String(other.type) : null;
            this.mode = other.mode != null ? new String(other.mode) : null;
            this.anonymous = other.anonymous;
            this.useFTPS = other.useFTPS;
            this.privateKeyPath = other.privateKeyPath != null ? new String(other.privateKeyPath) : null;
            this.privateKeyPassphrase = other.privateKeyPassphrase != null ? new String(other.privateKeyPassphrase) : null;
            this.knownHostsPath = other.knownHostsPath != null ? new String(other.knownHostsPath) : null;
            this.basePath = other.basePath != null ? new String(other.basePath) : null;
            this.useHTTPS = other.useHTTPS;
            this.domain = other.domain != null ? new String(other.domain) : null;
            this.shareName = other.shareName != null ? new String(other.shareName) : null;
            this.smbVersion = other.smbVersion != null ? new String(other.smbVersion) : null;
        }

        // Method to return a deep copy
        public NetworkAccountPOJO deepCopy() {
            return new NetworkAccountPOJO(this);
        }

        // Parcelable implementation
        protected NetworkAccountPOJO(Parcel in) {
            server = in.readString();
            port = in.readInt();
            user_name = in.readString();
            password = in.readString();
            encoding = in.readString();
            display = in.readString();
            type = in.readString();
            mode = in.readString();
            anonymous = in.readByte() != 0;
            useFTPS = in.readByte() != 0;
            privateKeyPath = in.readString();
            privateKeyPassphrase = in.readString();
            knownHostsPath = in.readString();
            basePath = in.readString();
            useHTTPS = in.readByte() != 0;
            domain = in.readString();
            shareName = in.readString();
            smbVersion = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(server);
            dest.writeInt(port);
            dest.writeString(user_name);
            dest.writeString(password);
            dest.writeString(encoding);
            dest.writeString(display);
            dest.writeString(type);
            dest.writeString(mode);
            dest.writeByte((byte) (anonymous ? 1 : 0));
            dest.writeByte((byte) (useFTPS ? 1 : 0));
            dest.writeString(privateKeyPath);
            dest.writeString(privateKeyPassphrase);
            dest.writeString(knownHostsPath);
            dest.writeString(basePath);
            dest.writeByte((byte) (useHTTPS ? 1 : 0));
            dest.writeString(domain);
            dest.writeString(shareName);
            dest.writeString(smbVersion);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<NetworkAccountPOJO> CREATOR = new Creator<NetworkAccountPOJO>() {
            @Override
            public NetworkAccountPOJO createFromParcel(Parcel in) {
                return new NetworkAccountPOJO(in);
            }

            @Override
            public NetworkAccountPOJO[] newArray(int size) {
                return new NetworkAccountPOJO[size];
            }
        };
    }
}