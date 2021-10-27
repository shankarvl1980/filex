package svl.kadatha.filex;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class MainActivity extends BaseActivity implements MediaMountReceiver.MediaMountListener, DeleteFileAlertDialog.OKButtonClickListener
{
	public boolean archive_view;
	private boolean working_dir_open,library_or_search_shown;
	DrawerLayout drawerLayout;
    public Button rename,working_dir_add_btn,working_dir_remove_btn;
    public ImageButton parent_dir_imagebutton,all_select;
	private ImageView working_dir_expand_indicator,library_expand_indicator;
	TextView file_number_view;
	public static final String ACTIVITY_NAME="MAIN_ACTIVITY";

	Toolbar extract_toolbar,bottom_toolbar,paste_toolbar,actionmode_toolbar;
	ConstraintLayout search_toolbar;
	ViewGroup drawer;
    private RecyclerView workingDirListRecyclerView;
    private RecyclerView libraryRecyclerView;
	TextView current_dir_textview;
    Context context=this;
	private int countBackPressed=0;
	ViewPager viewPager;
	public TinyDB tinyDB;
	static File ARCHIVE_EXTRACT_DIR,ZIP_FILE;
	static List<String> LIBRARY_CATEGORIES=new ArrayList<>();

	private Group working_dir_button_layout;
	private final int folder_select_request_code=6926;
    static boolean SHOW_HIDDEN_FILE;
	public String toolbar_shown="bottom";
	static FilePOJO DRAWER_STORAGE_FILEPOJO_SELECTED;
	static LinkedList<FilePOJO> RECENTS=new LinkedList<>();

	
	private StorageRecyclerAdapter storageRecyclerAdapter;
	private WorkingDirRecyclerAdapter workingDirRecyclerAdapter;

	private ArrayList<String> working_dir_arraylist=new ArrayList<>();
    ActionModeListener actionModeListener;
	private PopupWindow listPopWindow;
	final ArrayList<ListPopupWindowPOJO> list_popupwindowpojos=new ArrayList<>();
	static final List<String>APK_ICON_PACKAGE_NAME_LIST=new ArrayList<>();
	static File APK_ICON_DIR;
	public static PackageManager PM;
	public PackageManager pm;
	public static int ARCHIVE_CACHE_DIR_LENGTH;
	public FragmentManager fm;
	public static FragmentManager FM;
	private String toolbar_shown_prior_archive="";
    private EditText search_view;
	public boolean search_toolbar_visible;
	private KeyBoardUtil keyBoardUtil;

    private LocalBroadcastManager localBroadcastManager;
	private OtherActivityBroadcastReceiver otherActivityBroadcastReceiver;
	private USBReceiver usbReceiver;
	private InputMethodManager imm;
	static UsbFile usbFileRoot;
	static FileSystem usbCurrentFs;
	private static final List<DetailFragmentCommunicationListener> DETAIL_FRAGMENT_COMMUNICATION_LISTENERS=new ArrayList<>();
	public boolean clear_cache;
	public RecentDialogListener recentDialogListener;
	private ListView listView;
    private MediaMountReceiver mediaMountReceiver;
	public static String SU="";
	public FloatingActionButton floating_button_back;
	public static FTPClient FTP_CLIENT;



	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		context=this;
		PermissionsUtil permissionUtil=new PermissionsUtil(context,MainActivity.this);
		permissionUtil.check_storage_permission();
		tinyDB=new TinyDB(context);


		setContentView(R.layout.main);
		fm=getSupportFragmentManager();
		FM=fm;
		pm=getPackageManager();
		PM=pm;
		localBroadcastManager=LocalBroadcastManager.getInstance(context);

		mediaMountReceiver=new MediaMountReceiver();
		mediaMountReceiver.setMediaMountListener(this);
		IntentFilter intentFilter=new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addDataScheme("file");
		context.registerReceiver(mediaMountReceiver, intentFilter);


		otherActivityBroadcastReceiver= new OtherActivityBroadcastReceiver();
		IntentFilter localBroadcastIntentFilter=new IntentFilter();
		localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION);
		localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);
		localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION);
		localBroadcastManager.registerReceiver(otherActivityBroadcastReceiver,localBroadcastIntentFilter);


		usbReceiver=new USBReceiver();
		IntentFilter usbIntentFilter=new IntentFilter();
		usbIntentFilter.addAction(UsbDocumentProvider.USB_ATTACH_BROADCAST);
		localBroadcastManager.registerReceiver(usbReceiver,usbIntentFilter);

		ARCHIVE_EXTRACT_DIR=new File(getFilesDir(),"Archive");
		APK_ICON_DIR=getExternalFilesDir(".apk_icons");
		APK_ICON_PACKAGE_NAME_LIST.addAll(Arrays.asList(APK_ICON_DIR.list()));

		//StatusBarTint.darkenStatusBar(this, R.color.light_toolbar_background);

		ARCHIVE_CACHE_DIR_LENGTH=MainActivity.ARCHIVE_EXTRACT_DIR.getAbsolutePath().length();
		
		drawerLayout=findViewById(R.id.drawer_layout);
		drawer=findViewById(R.id.drawer_navigation_layout);
		keyBoardUtil=new KeyBoardUtil(drawerLayout);
		imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		search_toolbar=findViewById(R.id.search_toolbar);

        ImageButton search_detailed_button = search_toolbar.findViewById(R.id.search_bar_detailed_search_button);
		search_detailed_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				set_visibility_searchbar(false);
				SearchDialog searchDialog=new SearchDialog();
				searchDialog.show(fm,"search_dialog");

			}
		});

        ImageButton search_cancel_button = search_toolbar.findViewById(R.id.search_bar_cancel_button);
		search_cancel_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				set_visibility_searchbar(false);
			}
		});
		search_view=search_toolbar.findViewById(R.id.search_bar_view);
		search_view.setMaxWidth(Integer.MAX_VALUE);
		search_view.addTextChangedListener(new TextWatcher() {
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
				DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
				if(df!=null && df.adapter!=null)
				{
					df.adapter.getFilter().filter(s.toString());
				}
			}
		});

		file_number_view=findViewById(R.id.detail_fragment_file_number);

		ImageButton home_button = findViewById(R.id.top_toolbar_home_button);
		parent_dir_imagebutton=findViewById(R.id.top_toolbar_parent_dir_imagebutton);
		current_dir_textview=findViewById(R.id.top_toolbar_current_dir_label);
		all_select=findViewById(R.id.detail_fragment_all_select);

        TopToolbarClickListener topToolbarClickListener = new TopToolbarClickListener();
		home_button.setOnClickListener(topToolbarClickListener);
		parent_dir_imagebutton.setOnClickListener(topToolbarClickListener);
		current_dir_textview.setOnClickListener(topToolbarClickListener);
		all_select.setOnClickListener(topToolbarClickListener);


        floating_button_back = findViewById(R.id.floating_action_button_back);
		floating_button_back.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View p1)
			{
				onbackpressed(false);
			}
		});

		
		EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(this,5,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] bottom_drawables ={R.drawable.document_add_icon,R.drawable.search_icon,R.drawable.refresh_icon,R.drawable.view_icon,R.drawable.exit_icon};
		String [] titles=new String[]{getString(R.string.new_),getString(R.string.search),getString(R.string.refresh),getString(R.string.view),getString(R.string.exit)};
		tb_layout.setResourceImageDrawables(bottom_drawables,titles);
		bottom_toolbar=findViewById(R.id.bottom_toolbar);
		bottom_toolbar.addView(tb_layout);
        Button create_file = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button search = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button refresh = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        Button view = bottom_toolbar.findViewById(R.id.toolbar_btn_4);
        Button finish = bottom_toolbar.findViewById(R.id.toolbar_btn_5);
        BottomToolbarClickListener bottomToolbarClickListener = new BottomToolbarClickListener();
		create_file.setOnClickListener(bottomToolbarClickListener);
		search.setOnClickListener(bottomToolbarClickListener);
		refresh.setOnClickListener(bottomToolbarClickListener);
		view.setOnClickListener(bottomToolbarClickListener);
		finish.setOnClickListener(bottomToolbarClickListener);
		
		
		tb_layout =new EquallyDistributedButtonsWithTextLayout(this,5,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] paste_drawables ={R.drawable.document_add_icon,R.drawable.refresh_icon,R.drawable.paste_icon,R.drawable.delete_icon,R.drawable.no_icon};
		titles=new String[]{getString(R.string.new_),getString(R.string.refresh),getString(R.string.paste),getString(R.string.delete),getString(R.string.cancel)};
		tb_layout.setResourceImageDrawables(paste_drawables,titles);
		paste_toolbar=findViewById(R.id.paste_toolbar);
		paste_toolbar.addView(tb_layout);
        Button paste_toolbar_create_file = paste_toolbar.findViewById(R.id.toolbar_btn_1);
        Button paste_toolbar_refresh = paste_toolbar.findViewById(R.id.toolbar_btn_2);
        Button paste = paste_toolbar.findViewById(R.id.toolbar_btn_3);
        Button paste_toolbar_delete = paste_toolbar.findViewById(R.id.toolbar_btn_4);
        Button paste_cancel = paste_toolbar.findViewById(R.id.toolbar_btn_5);

        PasteToolbarClickListener pasteToolbarClickListener = new PasteToolbarClickListener();
		paste_toolbar_create_file.setOnClickListener(pasteToolbarClickListener);
		paste_toolbar_refresh.setOnClickListener(pasteToolbarClickListener);
		paste.setOnClickListener(pasteToolbarClickListener);
		paste_cancel.setOnClickListener(pasteToolbarClickListener);
		paste_toolbar_delete.setOnClickListener(pasteToolbarClickListener);

		tb_layout =new EquallyDistributedButtonsWithTextLayout(this,1,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] extract_drawables ={R.drawable.extract_icon};
		titles=new String[]{getString(R.string.extract)};
		tb_layout.setResourceImageDrawables(extract_drawables,titles);
		extract_toolbar=findViewById(R.id.extract_toolbar);
		extract_toolbar.addView(tb_layout);
        Button extract = extract_toolbar.findViewById(R.id.toolbar_btn_1);
		extract.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
				Bundle bundle=new Bundle();
				ArrayList<String> files_selected_array=new ArrayList<>();
				ArrayList<String> zipentry_selected_array=new ArrayList<>();
				files_selected_array.add(ZIP_FILE.getAbsolutePath());
				int size=df.mselecteditemsFilePath.size();
				if(df.mselecteditemsFilePath.size()!=0)
				{
					List<File> file_list=new ArrayList<>();

					for(int i=0;i<size;++i)
					{
						file_list.add(new File(df.mselecteditemsFilePath.valueAt(i)));
					}
					recursivefilepath(zipentry_selected_array,file_list);
				}

				ArchiveSetUpDialog unziparchiveDialog=ArchiveSetUpDialog.getInstance(files_selected_array,zipentry_selected_array,df.fileObjectType,ArchiveSetUpDialog.ARCHIVE_ACTION_UNZIP);
				unziparchiveDialog.show(fm,null);
				df.clearSelectionAndNotifyDataSetChanged();

			}
		});
		
		tb_layout =new EquallyDistributedButtonsWithTextLayout(this,5,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] actionmode_drawables ={R.drawable.cut_icon,R.drawable.copy_icon,R.drawable.rename_icon,R.drawable.delete_icon,R.drawable.overflow_icon};
		titles=new String[]{getString(R.string.cut),getString(R.string.copy),getString(R.string.rename),getString(R.string.delete),getString(R.string.more)};
		tb_layout.setResourceImageDrawables(actionmode_drawables,titles);
		actionmode_toolbar=findViewById(R.id.actionmode_toolbar);
		actionmode_toolbar.addView(tb_layout);
        Button cut = actionmode_toolbar.findViewById(R.id.toolbar_btn_1);
        Button copy = actionmode_toolbar.findViewById(R.id.toolbar_btn_2);
		rename=actionmode_toolbar.findViewById(R.id.toolbar_btn_3);
        Button delete = actionmode_toolbar.findViewById(R.id.toolbar_btn_4);
        Button overflow = actionmode_toolbar.findViewById(R.id.toolbar_btn_5);
		
		
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.compress_icon,getString(R.string.compress)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.extract_icon,getString(R.string.extract)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.cut_icon,getString(R.string.move_to)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.copy_icon,getString(R.string.copy_to)));


		listPopWindow=new PopupWindow(context);
		listView=new ListView(context);
		listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapater(context,list_popupwindowpojos));
		listPopWindow.setContentView(listView);
		listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
		listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		listPopWindow.setFocusable(true);
		listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
        int listview_height = Global.GET_HEIGHT_LIST_VIEW(listView);

		actionModeListener=new ActionModeListener();
		cut.setOnClickListener(actionModeListener);
		copy.setOnClickListener(actionModeListener);
		delete.setOnClickListener(actionModeListener);
		rename.setOnClickListener(actionModeListener);
		overflow.setOnClickListener(actionModeListener);
		
		/*
		viewPager=(ViewPager)findViewById(R.id.view_pager);
		viewPager.setOffscreenPageLimit(10);
		FragmentViewPager viewPagerAdapter=new FragmentViewPager(getSupportFragmentManager(),context);
		viewPager.setAdapter(viewPagerAdapter);
		
*/
		SHOW_HIDDEN_FILE=tinyDB.getBoolean("show_hidden_file");

        RecyclerView storageDirListRecyclerView = findViewById(R.id.drawer_recyclerview);
		storageDirListRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);

		storageDirListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		storageRecyclerAdapter=new StorageRecyclerAdapter(Global.STORAGE_DIR);
		storageDirListRecyclerView.setAdapter(storageRecyclerAdapter);

        View working_dir_heading_layout = findViewById(R.id.working_dir_layout_background);
		working_dir_heading_layout.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(workingDirListRecyclerView.getVisibility()==View.GONE)
				{
					working_dir_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.right_arrow_drawer_icon));
					workingDirListRecyclerView.setVisibility(View.VISIBLE);
					working_dir_button_layout.setVisibility(View.VISIBLE);
					working_dir_open=true;

				}
				else
				{
					working_dir_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.down_arrow_drawer_icon));
					workingDirListRecyclerView.setVisibility(View.GONE);
					working_dir_button_layout.setVisibility(View.GONE);
					working_dir_open=false;
				}
			}
		});

		working_dir_expand_indicator=findViewById(R.id.working_dir_expand_indicator);
		workingDirListRecyclerView=findViewById(R.id.working_dir_recyclerview);
		workingDirListRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);
		working_dir_arraylist=tinyDB.getListString("working_dir_arraylist");
		workingDirRecyclerAdapter=new WorkingDirRecyclerAdapter(context,working_dir_arraylist);
		workingDirRecyclerAdapter.setOnItemClickListener(new WorkingDirRecyclerAdapter.ItemClickListener()
		{
			public void onItemClick(int pos,String item_name)
			{
				File f=new File(working_dir_arraylist.get(pos));
				drawerLayout.closeDrawer(drawer);
				if(!f.exists())
				{
					print(getString(R.string.directory_does_not_exist));
				}
				else
				{
					DRAWER_STORAGE_FILEPOJO_SELECTED=new FilePOJO(FileObjectType.FILE_TYPE,f.getName(),f.getName().toLowerCase(),null,f.getAbsolutePath(),true,0L,null,0L,null,R.drawable.folder_icon,null,0,0,0,0L,null,0,null);
				}

			}

		});
		workingDirListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		workingDirListRecyclerView.setAdapter(workingDirRecyclerAdapter);
		setRecyclerViewHeight(workingDirListRecyclerView);

		working_dir_button_layout=findViewById(R.id.working_dir_button_group);
		working_dir_add_btn=findViewById(R.id.working_dir_add_btn);
		working_dir_add_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				workingDirAdd();
			}
		});
		working_dir_remove_btn=findViewById(R.id.working_dir_remove_btn);
		working_dir_remove_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				workingDirRemove();
			}
		});



        View library_heading_layout = findViewById(R.id.library_layout_background);
		library_heading_layout.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(libraryRecyclerView.getVisibility()==View.GONE)
				{
					library_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.right_arrow_drawer_icon));
					libraryRecyclerView.setVisibility(View.VISIBLE);
					library_or_search_shown=true;
				}
				else
				{
					library_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.down_arrow_drawer_icon));
					libraryRecyclerView.setVisibility(View.GONE);
					library_or_search_shown=false;
				}
			}
		});

		library_expand_indicator=findViewById(R.id.library_expand_indicator);
		LIBRARY_CATEGORIES=Arrays.asList(getResources().getStringArray(R.array.library_categories));
		libraryRecyclerView=findViewById(R.id.library_recyclerview);
		libraryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		libraryRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);
		libraryRecyclerView.setAdapter(new LibraryRecyclerAdapter(LIBRARY_CATEGORIES));


        SwitchCompat switchHideFile = findViewById(R.id.switch_hide_file);
		switchHideFile.setChecked(MainActivity.SHOW_HIDDEN_FILE);

		switchHideFile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton cb, final boolean checked)
			{

				new Handler().postDelayed(new Runnable()
				{
					public void run()
					{
						MainActivity.SHOW_HIDDEN_FILE=checked;
						tinyDB.putBoolean("show_hidden_file",MainActivity.SHOW_HIDDEN_FILE);
						DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
						actionmode_finish(df,df.fileclickselected);
						if(df.fileObjectType==FileObjectType.FILE_TYPE || df.fileObjectType==FileObjectType.ROOT_TYPE)
						{
							fm.beginTransaction().detach(df).commit();
							fm.beginTransaction().attach(df).commit();
						}

					}
				},500);

			}
		});

		View audio_player_heading_layout = findViewById(R.id.audio_player_label_background);
		audio_player_heading_layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				clear_cache=false;
				final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
				pbf.show(fm,"");
				drawerLayout.closeDrawer(drawer);

				Handler h=new Handler();
				h.postDelayed(new Runnable() {
					@Override
					public void run() {
						DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
						actionmode_finish(df,df.fileclickselected);
						Intent intent=new Intent(context,AudioPlayerActivity.class);
						startActivity(intent);
						pbf.dismissAllowingStateLoss();

					}
				},500);

			}
		});

		View storage_analyser_heading_layout=findViewById(R.id.storage_analyser_label_background);
		storage_analyser_heading_layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				clear_cache=false;
				final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
				pbf.show(fm,"");
				drawerLayout.closeDrawer(drawer);

				Handler h=new Handler();
				h.postDelayed(new Runnable() {
					@Override
					public void run() {
						DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
						actionmode_finish(df,df.fileclickselected);
						Intent intent=new Intent(context,StorageAnalyserActivity.class);
						startActivity(intent);
						pbf.dismissAllowingStateLoss();

					}
				},500);
			}
		});

		View ftp_details_heading_layout=findViewById(R.id.ftp_label_background);
		ftp_details_heading_layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
				pbf.show(fm,"");
				drawerLayout.closeDrawer(drawer);

				Handler h=new Handler();
				h.postDelayed(new Runnable() {
					@Override
					public void run() {
						DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
						actionmode_finish(df,df.fileclickselected);

						FtpDetailsDialog ftpDetailsDialog=new FtpDetailsDialog();
						ftpDetailsDialog.show(fm,"ftp_dialog_fragment");
						pbf.dismissAllowingStateLoss();

					}
				},500);

			}
		});

		int drawer_width=(int)getResources().getDimension(R.dimen.drawer_width);
		tb_layout =new EquallyDistributedButtonsWithTextLayout(this,2,drawer_width,drawer_width);
		int[] drawer_end_drawables ={R.drawable.exit_drawer_icon,R.drawable.settings_drawer_icon};
		titles=new String[]{getString(R.string.exit),getString(R.string.settings)};
		tb_layout.setResourceImageDrawables(drawer_end_drawables,titles);
        ViewGroup drawer_end_butt = findViewById(R.id.drawer_end_butt_layout);
		drawer_end_butt.addView(tb_layout);
		int color=getResources().getColor(R.color.light_heading_text_color);
        Button exit = drawer_end_butt.findViewById(R.id.toolbar_btn_1);
		exit.setTextColor(color);
        Button settings = drawer_end_butt.findViewById(R.id.toolbar_btn_2);
		settings.setTextColor(color);
		DrawerEndButtButtonsClickListener drawerEndButtButtonsClickListener=new DrawerEndButtButtonsClickListener();
		exit.setOnClickListener(drawerEndButtButtonsClickListener);
		settings.setOnClickListener(drawerEndButtButtonsClickListener);

		drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener()
		{
			public void onDrawerOpened(View v)
			{
				DRAWER_STORAGE_FILEPOJO_SELECTED=null;
			}
			public void onDrawerClosed(View v)
			{
				if(DRAWER_STORAGE_FILEPOJO_SELECTED!=null)
				{
					FileObjectType fileObjectType=DRAWER_STORAGE_FILEPOJO_SELECTED.getFileObjectType();
					createFragmentTransaction(DRAWER_STORAGE_FILEPOJO_SELECTED.getPath(),fileObjectType);
					DRAWER_STORAGE_FILEPOJO_SELECTED=null;
				}
			}
			public void onDrawerStateChanged(int p){}
			public void onDrawerSlide(View v, float f){}
		});



		if(savedInstanceState==null)
		{
			createFragmentTransaction(Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR(),FileObjectType.FILE_TYPE);
			Intent intent=getIntent();
			if(intent!=null)
			{
				onNewIntent(intent);
			}
		}

		discoverDevice();
	}

	public void set_visibility_searchbar(boolean visible)
	{
		DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
		if(!df.filled_filePOJOs)
		{
			print(getString(R.string.please_wait));
			return;
		}
		search_toolbar_visible=visible;
		if(search_toolbar_visible)
		{
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
			search_toolbar.setVisibility(View.VISIBLE);
			search_view.requestFocus();
		}
		else
		{
			actionmode_finish(df, df.fileclickselected);
		}

	}
	public ArrayList<String> recursivefilepath(ArrayList<String> file_pathstring_array, List<File> file_array)
	{
		int size=file_array.size();
		for(int i=0;i<size;++i)
		{
			File f=file_array.get(i);
			if(f.isDirectory())
			{
				File[] inner_file_array=f.listFiles();
				if(inner_file_array.length==0)
				{
					file_pathstring_array.add(f.getAbsolutePath()+File.separator);
				}
				else
				{
					recursivefilepath(file_pathstring_array,Arrays.asList(inner_file_array));
				}
			}
			else
			{
				file_pathstring_array.add(f.getAbsolutePath());
			}
		}
		return file_pathstring_array;
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		// TODO: Implement this method
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		final List<String> permission_not_granted_list=new ArrayList<>();
		if(requestCode==PermissionsUtil.STORAGE_PERMISSIONS_REQUEST_CODE && grantResults.length>0)
		{
			for(int i=0;i<permissions.length;++i)
			{
				if(grantResults[i]!=PackageManager.PERMISSION_GRANTED)
				{
					permission_not_granted_list.add(permissions[i]);
				}
			}

		}

		if(grantResults.length==0 || !permission_not_granted_list.isEmpty())
		{
			for(String permission:permission_not_granted_list)
			{
				boolean show_rationale= false;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)
					{
						try {
							Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
							intent.addCategory("android.intent.category.DEFAULT");
							intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
							startActivityForResult(intent, PermissionsUtil.STORAGE_PERMISSIONS_REQUEST_CODE);
						} catch (Exception e) {
							Intent intent = new Intent();
							intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
							startActivityForResult(intent, PermissionsUtil.STORAGE_PERMISSIONS_REQUEST_CODE);
						}
						break;
					}
					else
					{
						show_rationale = shouldShowRequestPermissionRationale(permission);
					}

				}

				if(!show_rationale)
				{
					print(getString(R.string.seems_permissions_were_not_granted_goto_settings_grant_permissions_to_app));
					finish();
				}
				else if(permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE))
				{
					showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions),new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							switch (which)
							{
								case DialogInterface.BUTTON_POSITIVE:
									new PermissionsUtil(context,MainActivity.this).check_storage_permission();
									break;
								case DialogInterface.BUTTON_NEGATIVE:
									print(getString(R.string.permission_not_granted));
									finish();
									break;
							}
						}
					});
				}
			}

		}
		else
		{
			Global.STORAGE_DIR.clear();
			clearCache();
			Intent in=getIntent();
			finish();
			startActivity(in);

		}
	}

	private void showDialogOK(String message, DialogInterface.OnClickListener okListener)
	{
        new AlertDialog.Builder(context)
			.setMessage(message)
			.setPositiveButton(getString(R.string.ok), okListener)
			.setNegativeButton(getString(R.string.cancel), okListener)
			.create()
			.show();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		// TODO: Implement this method
		super.onNewIntent(intent);
		if(intent==null)
		{
			return;
		}

		String receivedAction=intent.getAction();
		Uri uri=intent.getData();
		if(receivedAction!=null && receivedAction.equals(Intent.ACTION_VIEW) &&  uri !=null)
		{
			ZIP_FILE=new File(PathUtil.getPath(context,uri));
			ZipFile zipfile;
			try
			{
				zipfile=new ZipFile(ZIP_FILE);
			}
			catch(IOException e)
			{
				print(getString(R.string.could_not_open_zipe_file));
				return;
			}

			ArchiveViewAsyncTask archiveViewAsyncTask=new ArchiveViewAsyncTask(zipfile);
			archiveViewAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}


	@Override
	protected void onStart()
	{
		// TODO: Implement this method
		super.onStart();
		clear_cache=true;
		Global.WORKOUT_AVAILABLE_SPACE();

	}

	@Override
	protected void onStop() {
		super.onStop();
		if(search_toolbar_visible)
		{
			set_visibility_searchbar(false);
		}
		if(!isChangingConfigurations() && clear_cache)
		{
			clearCache();
		}
	}

	public void clearCache()
	{
		Global.HASHMAP_FILE_POJO.clear();
		Global.HASHMAP_FILE_POJO_FILTERED.clear();
		Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION,localBroadcastManager,ACTIVITY_NAME);
	}

	public void clearCache(String file_path, FileObjectType fileObjectType)
	{
		FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_path),fileObjectType); //no need of broad cast here, as the method includes broadcast
	}

	public void broadcast_file_pojo_cache_removal(String file_path,FileObjectType fileObjectType)
	{
		Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION,localBroadcastManager,ACTIVITY_NAME,file_path,fileObjectType);
	}


	 @Override
	 public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData)
	 {
	 	super.onActivityResult(requestCode,resultCode,resultData);
	 	switch (requestCode)
		{
			case folder_select_request_code:
				if (resultCode == Activity.RESULT_OK)
		 			{
			 			Bundle bundle = resultData.getBundleExtra("bundle");
						ArrayList<String> files_selected_array=new ArrayList<>(bundle.getStringArrayList("files_selected_array"));
			 			boolean cut=bundle.getBoolean("cut");
						String source_folder=bundle.getString("source_folder");
						String dest_folder=bundle.getString("dest_folder");
						FileObjectType sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
						FileObjectType destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
						if (sourceFileObjectType.equals(destFileObjectType) && source_folder.equals(dest_folder)) {
							print(!cut ? getString(R.string.selected_files_have_been_copied) : getString(R.string.selected_filed_have_been_moved));
						}
						else
							{
							if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
							{
								//process to have only files with unique file names
								Set<String> file_name_set=new HashSet<>();
								Iterator<String> iterator=files_selected_array.iterator();
								while(iterator.hasNext())
								{
									String file_path=iterator.next();
									boolean inserted=file_name_set.add(new File(file_path).getName());
									if(!inserted) iterator.remove();
								}

							}

							PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder, files_selected_array, sourceFileObjectType,
									destFileObjectType, dest_folder, cut);
							pasteSetUpDialog.show(fm, "paste_dialog");

						}
		 			}
				break;
			case PermissionsUtil.STORAGE_PERMISSIONS_REQUEST_CODE:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					if (Environment.isExternalStorageManager())
					{
						Global.STORAGE_DIR.clear();
						clearCache();
						Intent in=getIntent();
						finish();
						startActivity(in);
					}
					else
					{
						showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions),new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								switch (which)
								{
									case DialogInterface.BUTTON_POSITIVE:
										try {
											Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
											intent.addCategory("android.intent.category.DEFAULT");
											intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
											startActivityForResult(intent, PermissionsUtil.STORAGE_PERMISSIONS_REQUEST_CODE);
										} catch (Exception e) {
											Intent intent = new Intent();
											intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
											startActivityForResult(intent, PermissionsUtil.STORAGE_PERMISSIONS_REQUEST_CODE);
										}
										break;
									case DialogInterface.BUTTON_NEGATIVE:
										print(getString(R.string.permission_not_granted));
										finish();
										break;
								}
							}
						});
					}
				}

		}

	 }

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);
		outState.putString("toolbar_shown",toolbar_shown);
		outState.putString("toolbar_shown_prior_archive",toolbar_shown_prior_archive);
		outState.putBoolean("archive_view",archive_view);
		outState.putBoolean("working_dir_open",working_dir_open);
		outState.putBoolean("library_or_search_shown",library_or_search_shown);
		outState.putSerializable("custom_dir_selected_hash_map",workingDirRecyclerAdapter.custom_dir_selected_hash_map);
		outState.putStringArrayList("custom_dir_selected_array",workingDirRecyclerAdapter.custom_dir_selected_array);
		outState.putBoolean("clear_cache",clear_cache);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onRestoreInstanceState(savedInstanceState);
		DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
		toolbar_shown=savedInstanceState.getString("toolbar_shown");
		switch (toolbar_shown) {
			case "actionmode":
				actionmode_toolbar.setVisibility(View.VISIBLE);
				bottom_toolbar.setVisibility(View.GONE);
				paste_toolbar.setVisibility(View.GONE);
				break;
			case "paste":
				paste_toolbar.setVisibility(View.VISIBLE);
				bottom_toolbar.setVisibility(View.GONE);
				actionmode_toolbar.setVisibility(View.GONE);
				break;
			case "extract":
				extract_toolbar.setVisibility(View.VISIBLE);
				parent_dir_imagebutton.setEnabled(false);
				parent_dir_imagebutton.setAlpha(Global.DISABLE_ALFA);
				bottom_toolbar.setVisibility(View.GONE);
				break;
		}
		toolbar_shown_prior_archive=savedInstanceState.getString("toolbar_shown_prior_archive");
		archive_view=savedInstanceState.getBoolean("archive_view");
		if(df.mselecteditems.size()>1)
		{
			rename.setEnabled(false);
			rename.setAlpha(Global.DISABLE_ALFA);
		}

		if(working_dir_open=savedInstanceState.getBoolean("working_dir_open"))
		{
			working_dir_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.right_arrow_drawer_icon));
			workingDirListRecyclerView.setVisibility(View.VISIBLE);
			working_dir_button_layout.setVisibility(View.VISIBLE);
		}

		workingDirRecyclerAdapter.custom_dir_selected_hash_map= (HashMap<Integer, Boolean>) savedInstanceState.getSerializable("custom_dir_selected_hash_map");
		workingDirRecyclerAdapter.custom_dir_selected_array=savedInstanceState.getStringArrayList("custom_dir_selected_array");

		if(library_or_search_shown=savedInstanceState.getBoolean("library_or_search_shown"))
		{
			library_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.right_arrow_drawer_icon));
			libraryRecyclerView.setVisibility(View.VISIBLE);
		}

		clear_cache=savedInstanceState.getBoolean("clear_cache");
	}

	public void createFragmentTransaction(String file_path,FileObjectType fileObjectType)
	{
		String fragment_tag;
		String existingFilePOJOkey="";
		DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
		if(df!=null)
		{
			fragment_tag=df.getTag();
			existingFilePOJOkey=df.fileObjectType+fragment_tag;
			actionmode_finish(df,file_path); //string provided to actionmode_finish method is file_path (which is clicked, not the existing file_path) to be created of fragemnttransaction
		}

		if(file_path.equals(DetailFragment.SEARCH_RESULT) || DetailFragment.TO_BE_MOVED_TO_FILE_POJO!=null)
		{
			fm.beginTransaction().replace(R.id.detail_fragment,DetailFragment.getInstance(fileObjectType),file_path)
					.addToBackStack(file_path).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

		}
		else if(!(fileObjectType+file_path).equals(existingFilePOJOkey))
		{
			fm.beginTransaction().replace(R.id.detail_fragment,DetailFragment.getInstance(fileObjectType),file_path)
					.addToBackStack(file_path).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

		}

	}


	@Override
	public void onBackPressed()
	{
		// TODO: Implement this method
		onbackpressed(true);

	}

	private void onbackpressed(boolean onBackPressed)
	{
		DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
		boolean drawerOpen=drawerLayout.isDrawerOpen(drawer);
		if(drawerOpen)
		{
			drawerLayout.closeDrawer(drawer);
		}
		else if(keyBoardUtil.getKeyBoardVisibility())
		{

			imm.hideSoftInputFromWindow(search_view.getWindowToken(),0);
		}
		else if(df.mselecteditems.size()>0)
		{
			actionmode_finish(df,df.fileclickselected);

		}
		else if(search_toolbar_visible)
		{
			set_visibility_searchbar(false);
		}
		else
		{
			if(df.getTag().equals(ARCHIVE_EXTRACT_DIR.getAbsolutePath()) && archive_view)
			{
				archive_exit();
			}
			int entry_count;
			if((entry_count=fm.getBackStackEntryCount())>1)
			{
				switch (toolbar_shown)
				{
					case "bottom":
						bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
						bottom_toolbar.setVisibility(View.VISIBLE);
						break;
					case "paste":
						paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
						paste_toolbar.setVisibility(View.VISIBLE);
						break;
					case "extract":
						extract_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
						extract_toolbar.setVisibility(View.VISIBLE);
						break;
				}

				fm.popBackStack();
				int frag=2;
				df= (DetailFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count-frag).getName());
				String df_tag=df.getTag();
				while(!new File(df_tag).exists() && !LIBRARY_CATEGORIES.contains(df_tag) &&  df.currentUsbFile == null) //!df_tag.equals(DetailFragment.SEARCH_RESULT) &&
				{
					fm.popBackStack();
					++frag;
					if(frag>entry_count) break;
					df= (DetailFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count-frag).getName());
					df_tag = df.getTag();
				}

				if(df_tag.equals(ARCHIVE_EXTRACT_DIR.getAbsolutePath()) && archive_view)
				{
					parent_dir_imagebutton.setEnabled(false);
					parent_dir_imagebutton.setAlpha(Global.DISABLE_ALFA);
				}
				countBackPressed=0;
				/*
				if((entry_count-frag)<1)
				{
					floating_button_back.setEnabled(false);
					floating_button_back.setAlpha(Global.DISABLE_ALFA);
				}

				 */
			}
			else
			{
				/*
				floating_button_back.setEnabled(false);
				floating_button_back.setAlpha(Global.DISABLE_ALFA);

				 */
				if(onBackPressed)
				{
					countBackPressed++;
					if(countBackPressed==1)
					{
						print(getString(R.string.press_again_to_close_app));
					}
					else
					{
						if(getCacheDir().exists())
						{
							new AsyncTaskDeleteDirectory(getCacheDir()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						}
						if(ARCHIVE_EXTRACT_DIR.exists())
						{
							new AsyncTaskDeleteDirectory(ARCHIVE_EXTRACT_DIR).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						}
						finish();
					}
				}

			}
		}
	}

	@Override
	protected void onPause()
	{
		// TODO: Implement this method
		super.onPause();
		imm.hideSoftInputFromWindow(search_view.getWindowToken(),0);
	}


	@Override
	protected void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		localBroadcastManager.unregisterReceiver(usbReceiver);
		localBroadcastManager.unregisterReceiver(otherActivityBroadcastReceiver);
		context.unregisterReceiver(mediaMountReceiver);
	}

	public void DeselectAllAndAdjustToolbars(DetailFragment df,String detailfrag_tag)
	{
		listPopWindow.dismiss();
		if((detailfrag_tag+File.separator).startsWith(ARCHIVE_EXTRACT_DIR.getAbsolutePath()+File.separator) &&  archive_view)
		{
			extract_toolbar.setVisibility(View.VISIBLE);
			extract_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
			paste_toolbar.setVisibility(View.GONE);
			bottom_toolbar.setVisibility(View.GONE);
			toolbar_shown="extract";
			if(detailfrag_tag.equals(ARCHIVE_EXTRACT_DIR.getAbsolutePath()))
			{
				parent_dir_imagebutton.setEnabled(false);
				parent_dir_imagebutton.setAlpha(Global.DISABLE_ALFA);
			}

		}
		else if(DetailFragment.CUT_SELECTED || DetailFragment.COPY_SELECTED)
		{
			if(archive_view)
			{
				archive_exit();   //experimental
			}
			paste_toolbar.setVisibility(View.VISIBLE);
			paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
			bottom_toolbar.setVisibility(View.GONE);
			extract_toolbar.setVisibility(View.GONE);
			toolbar_shown="paste";
			parent_dir_imagebutton.setEnabled(true);
			parent_dir_imagebutton.setAlpha(Global.ENABLE_ALFA);
		}
		else
		{
			parent_dir_imagebutton.setEnabled(true);
			parent_dir_imagebutton.setAlpha(Global.ENABLE_ALFA);

			archive_exit();
		}
		actionmode_toolbar.setVisibility(View.GONE);
		if(df!=null)
		{
			df.clearSelectionAndNotifyDataSetChanged();
			df.is_toolbar_visible=true;
			all_select.setImageResource(R.drawable.select_icon);
		}

	}

	public void actionmode_finish(DetailFragment df, String detailgfrag_tag)
	{
		DeselectAllAndAdjustToolbars(df,detailgfrag_tag);
		imm.hideSoftInputFromWindow(search_view.getWindowToken(),0);
		search_view.setText("");
		search_view.clearFocus();
		search_toolbar.setVisibility(View.GONE); //no need to call adapter.filter with null to refill filepjos as calling datasetchanged replenished df.adapter.filepojo list
		search_toolbar_visible=false;
		df.adapter.getFilter().filter(null);

	}

	public void archive_exit()
	{
		if(ARCHIVE_EXTRACT_DIR.exists())
		{
			new AsyncTaskDeleteDirectory(ARCHIVE_EXTRACT_DIR).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(MainActivity.ARCHIVE_EXTRACT_DIR.getAbsolutePath()),FileObjectType.FILE_TYPE);

		}

		if(toolbar_shown_prior_archive.equals("paste"))
		{
			paste_toolbar.setVisibility(View.VISIBLE);
			paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
			toolbar_shown=toolbar_shown_prior_archive;
			toolbar_shown_prior_archive="";
			bottom_toolbar.setVisibility(View.GONE);
		}
		else
		{
			bottom_toolbar.setVisibility(View.VISIBLE);
			bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
			toolbar_shown="bottom";
			paste_toolbar.setVisibility(View.GONE);
		}

		actionmode_toolbar.setVisibility(View.GONE);
		extract_toolbar.setVisibility(View.GONE);
		archive_view=false;
	}


	public void workingDirAdd()
	{
		DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
		if(working_dir_arraylist.size()>10)
		{
			print(getString(R.string.more_than_10_directories_cannot_be_added));
			return;
		}

		String file_path=df.fileclickselected;
		if(df.fileObjectType== FileObjectType.FILE_TYPE || df.fileObjectType==FileObjectType.ROOT_TYPE)
		{
			File file=new File(file_path);
			if(file.isDirectory() && !working_dir_arraylist.contains(file_path) && !StorageUtil.STORAGE_DIR.contains(file) && !archive_view)
			{
				int i=workingDirRecyclerAdapter.insert(file_path);
				workingDirListRecyclerView.scrollToPosition(i);
				tinyDB.putListString("working_dir_arraylist",working_dir_arraylist);
				setRecyclerViewHeight(workingDirListRecyclerView);
			}
		}
    }

	public void workingDirRemove()
	{
		if(working_dir_arraylist==null || working_dir_arraylist.size()==0)
		{
			return;
		}

		if(workingDirRecyclerAdapter.custom_dir_selected_array.size()<1)
		{
			print(getString(R.string.select_directories_by_long_pressing));
		}
		else
		{
			workingDirRecyclerAdapter.remove(workingDirRecyclerAdapter.custom_dir_selected_array);
			tinyDB.putListString("working_dir_arraylist",working_dir_arraylist);
			setRecyclerViewHeight(workingDirListRecyclerView);
		}

	}
	private void setRecyclerViewHeight(RecyclerView v)
	{
		int number_items=Math.min(5,v.getAdapter().getItemCount());
		v.getLayoutParams().height=number_items*Global.FOUR_DP*14;
	}

	public ArrayList<File> iterate_to_attach_file(SparseArray<String> file_list)
	{
		ArrayList<File> file_list_excluding_dir=new ArrayList<>();
		int size=file_list.size();
		for(int i=0;i<size;++i)
		{
			File f=new File(file_list.valueAt(i));
			if(!f.isDirectory())
			{
				file_list_excluding_dir.add(f);
			}
		}
		return file_list_excluding_dir;
	}


	@Override
	public void deleteDialogOKButtonClick() {
		final DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
		actionmode_finish(df,df.fileclickselected);
	}


	private class TopToolbarClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			// TODO: Implement this method
			final DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
			int id = v.getId();
			if (id == R.id.top_toolbar_home_button) {
				if (drawerLayout.isDrawerOpen(drawer)) {
					drawerLayout.closeDrawer(drawer);
				} else {
					drawerLayout.openDrawer(drawer);
				}
			} else if (id == R.id.top_toolbar_parent_dir_imagebutton) {

				File f = new File(df.fileclickselected);
				String parent_file_path = f.getParent();
				if (parent_file_path==null) return;
				if (df.fileObjectType == FileObjectType.FILE_TYPE) {

					File parent_file=new File(parent_file_path);
					if (parent_file != null && parent_file.list() != null) {
						createFragmentTransaction(parent_file.getAbsolutePath(),FileObjectType.FILE_TYPE);
					}
				} else if (df.fileObjectType == FileObjectType.USB_TYPE) {
					if (MainActivity.usbFileRoot == null) {
						return;
					}
					try {
						UsbFile usbFile = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(parent_file_path));
						createFragmentTransaction(usbFile.getAbsolutePath(),FileObjectType.USB_TYPE);
					} catch (IOException e) {

					}
				} else if(df.fileObjectType==FileObjectType.ROOT_TYPE)
				{
					createFragmentTransaction(parent_file_path,FileObjectType.ROOT_TYPE);
				}
			} else if (id == R.id.top_toolbar_current_dir_label) {
				RecentDialog recentDialogFragment = new RecentDialog();
				recentDialogFragment.show(fm, "recent_file_dialog");
			} else if (id == R.id.detail_fragment_all_select) {
				if (df.adapter == null) {
					return;
				}

				if (df.mselecteditems.size() < df.filePOJO_list.size()) {
					all_select.setImageResource(R.drawable.deselect_icon);
					df.adapter.selectAll();
				} else {
					all_select.setImageResource(R.drawable.select_icon);
					df.adapter.deselectAll();
				}
			}

		}

	}

	private class BottomToolbarClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View v)
		{

			DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
			int id = v.getId();
			if (id == R.id.toolbar_btn_1) {
				if(search_toolbar_visible)
				{
					set_visibility_searchbar(false);
				}
				if(df.fileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
				{
					print(getString(R.string.file_can_not_be_created_here));
					return;
				}
				CreateFileAlertDialog dialog = CreateFileAlertDialog.getInstance(df.fileclickselected,df.fileObjectType);
				dialog.show(fm, "create_file_dialog");
			} else if (id == R.id.toolbar_btn_2) {
				if(!search_toolbar_visible)
				{
					set_visibility_searchbar(true);
				}
				else
				{
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
				}

			} else if (id == R.id.toolbar_btn_3) {

				fm.beginTransaction().detach(df).commit();
				fm.beginTransaction().attach(df).commit();
				Global.WORKOUT_AVAILABLE_SPACE();
			} else if (id == R.id.toolbar_btn_4) {
				if(search_toolbar_visible)
				{
					set_visibility_searchbar(false);
				}
				ViewDialog viewDialog = new ViewDialog();
				viewDialog.show(fm, "view_dialog");
			} else if (id == R.id.toolbar_btn_5) {
				if(search_toolbar_visible)
				{
					set_visibility_searchbar(false);
				}
				if (getCacheDir().exists()) {
					new AsyncTaskDeleteDirectory(getCacheDir()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				if (ARCHIVE_EXTRACT_DIR.exists()) {
					new AsyncTaskDeleteDirectory(ARCHIVE_EXTRACT_DIR).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				finish();
			}
		}

	}

	private class ActionModeListener implements View.OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			imm.hideSoftInputFromWindow(search_view.getWindowToken(),0);
			final DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
			final Bundle bundle=new Bundle();
			final ArrayList<String> files_selected_array=new ArrayList<>();
			final ArrayList<Integer> files_selected_index_array=new ArrayList<>();
			int size;
			int id = v.getId();
			if (id == R.id.toolbar_btn_1) {
				DetailFragment.CUT_SELECTED = true;
				DetailFragment.COPY_SELECTED = false;
				DetailFragment.FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
				size = df.mselecteditemsFilePath.size();
				for (int i = 0; i < size; ++i) {
					DetailFragment.FILE_SELECTED_FOR_CUT_COPY.add(df.mselecteditemsFilePath.valueAt(i));
				}
				DetailFragment.CUT_COPY_FILE_OBJECT_TYPE = df.fileObjectType;
				DetailFragment.CUT_COPY_FILECLICKSELECTED=df.fileclickselected;
				actionmode_finish(df,df.fileclickselected);
			} else if (id == R.id.toolbar_btn_2) {
				DetailFragment.COPY_SELECTED = true;
				DetailFragment.CUT_SELECTED = false;
				DetailFragment.FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
				size = df.mselecteditemsFilePath.size();
				for (int i = 0; i < size; ++i) {
					DetailFragment.FILE_SELECTED_FOR_CUT_COPY.add(df.mselecteditemsFilePath.valueAt(i));
				}
				DetailFragment.CUT_COPY_FILE_OBJECT_TYPE = df.fileObjectType;
				DetailFragment.CUT_COPY_FILECLICKSELECTED=df.fileclickselected;
				actionmode_finish(df,df.fileclickselected);
			} else if (id == R.id.toolbar_btn_3) {
				FilePOJO filePOJO = df.filePOJO_list.get(df.mselecteditems.keyAt(0)); //take file pojo from df.adapter.filepojolist, not from df.filepojolist
				String parent_file_path = new File(filePOJO.getPath()).getParent();
				String existing_name = filePOJO.getName();
				boolean isDirectory = filePOJO.getIsDirectory();
				RenameFileDialog renameFileAlertDialog = RenameFileDialog.getInstance(parent_file_path,existing_name,isDirectory,filePOJO.getFileObjectType(),df.fileclickselected);
				renameFileAlertDialog.show(fm, "rename_dialog");
				actionmode_finish(df,df.fileclickselected);
			} else if (id == R.id.toolbar_btn_4) {
				size = df.mselecteditemsFilePath.size();
				for (int i = 0; i < size; ++i) {
					int key = df.mselecteditemsFilePath.keyAt(i);
					files_selected_array.add(df.mselecteditemsFilePath.get(key));
					files_selected_index_array.add(key);
				}

				DeleteFileAlertDialog deleteFileAlertDialog = DeleteFileAlertDialog.getInstance(files_selected_array,df.fileObjectType,df.fileclickselected,false);
				deleteFileAlertDialog.show(fm, "delete_dialog");

			} else if (id == R.id.toolbar_btn_5) {
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
						int size;
						switch (p3) {
							case 0:
								if ((df.fileObjectType == FileObjectType.FILE_TYPE) || (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE)) {
									ArrayList<File> file_list_excluding_dir;
									file_list_excluding_dir = iterate_to_attach_file(df.mselecteditemsFilePath);
									if (file_list_excluding_dir.size() == 0) {
										print(getString(R.string.directories_can_not_be_sent_select_one_file));
										actionmode_finish(df,df.fileclickselected);
										break;
									}
									FileIntentDispatch.sendFile(MainActivity.this, file_list_excluding_dir);
								}
                                actionmode_finish(df,df.fileclickselected);
								break;
							case 1:
								size = df.mselecteditemsFilePath.size();
								for (int i = 0; i < size; ++i) {
									files_selected_array.add(df.mselecteditemsFilePath.valueAt(i));
								}

								PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array,df.fileObjectType);
								propertiesDialog.show(fm, "properties_dialog");
								break;
							case 2:
								size = df.mselecteditemsFilePath.size();
								for (int i = 0; i < size; ++i) {
									files_selected_array.add(df.mselecteditemsFilePath.valueAt(i));
								}
								ArchiveSetUpDialog archiveSetUpDialog=ArchiveSetUpDialog.getInstance(files_selected_array,null,df.fileObjectType,ArchiveSetUpDialog.ARCHIVE_ACTION_ZIP);
								archiveSetUpDialog.show(fm, "zip_dialog");
								actionmode_finish(df,df.fileclickselected);
								break;
							case 3:
								if (df.mselecteditemsFilePath.size() != 1) {
									print(getString(R.string.select_only_a_zip_file));
									actionmode_finish(df,df.fileclickselected);
									break;
								}
								String path = df.mselecteditemsFilePath.valueAt(0);
								String file_name = new File(path).getName();
								String file_ext = "";
								int idx = file_name.lastIndexOf(".");
								if (idx != -1) {
									file_ext = file_name.substring(idx + 1);
								}
								if (file_ext.matches(("(?i)zip"))) {
									size = df.mselecteditemsFilePath.size();
									for (int i = 0; i < size; ++i) {
										files_selected_array.add(df.mselecteditemsFilePath.valueAt(i));
									}
									ArchiveSetUpDialog unarchiveSetUpDialog=ArchiveSetUpDialog.getInstance(files_selected_array,null,df.fileObjectType,ArchiveSetUpDialog.ARCHIVE_ACTION_UNZIP);
									unarchiveSetUpDialog.show(fm, "zip_dialog");
									actionmode_finish(df,df.fileclickselected);
								} else {
									print(getString(R.string.select_only_a_zip_file));
									actionmode_finish(df,df.fileclickselected);
								}
								break;
							case 4:
								MoveToCopyToProcedure(df,true);
								actionmode_finish(df,df.fileclickselected);
								break;
							case 5:
								MoveToCopyToProcedure(df,false);
								actionmode_finish(df,df.fileclickselected);
								break;
							default:
								break;

						}
						listPopWindow.dismiss();
					}
				});

				//listPopWindow.showAsDropDown(v,0,-(Global.ACTION_BAR_HEIGHT+listview_height+Global.FOUR_DP));
				listPopWindow.showAtLocation(actionmode_toolbar,Gravity.BOTTOM|Gravity.END,0,Global.ACTION_BAR_HEIGHT+Global.FOUR_DP+Global.NAVIGATION_BAR_HEIGHT);

			}
		}
	}

	private void MoveToCopyToProcedure(DetailFragment df, boolean cut)
	{
		clear_cache=false;
		Bundle bundle=new Bundle();
		ArrayList<String>files_selected_array=new ArrayList<>();
		int size = df.mselecteditemsFilePath.size();
		for (int i = 0; i < size; ++i) {
			files_selected_array.add(df.mselecteditemsFilePath.valueAt(i));
		}
		bundle.putString("source_folder", df.fileclickselected);
		bundle.putStringArrayList("files_selected_array", files_selected_array);
		bundle.putSerializable("sourceFileObjectType", df.fileObjectType);
		bundle.putBoolean("cut", cut);

		Intent intent=new Intent(context,FileSelectorActivity.class);
		intent.putExtra("bundle",bundle);
		intent.putExtra(FileSelectorActivity.ACTION_SOUGHT,FileSelectorActivity.MOVE_COPY_REQUEST_CODE);
		startActivityForResult(intent,folder_select_request_code);

	}


	private class PasteToolbarClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			// TODO: Implement this method
			final DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
			Bundle bundle=new Bundle();
			ArrayList<String> files_selected_array=new ArrayList<>();
			ArrayList<Integer> files_selected_index_array=new ArrayList<>();
			int size;
			int id = v.getId();
			if (id == R.id.toolbar_btn_1) {
				if(df.fileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
				{
					print(getString(R.string.file_can_not_be_created_here));
					return;
				}
				CreateFileAlertDialog dialog = CreateFileAlertDialog.getInstance(df.fileclickselected,df.fileObjectType);
				dialog.show(fm, "create_file_dialog");
			} else if (id == R.id.toolbar_btn_2) {
				fm.beginTransaction().detach(df).attach(df).commit();
			} else if (id == R.id.toolbar_btn_3) {
				actionmode_finish(df,df.fileclickselected);
				if(df.fileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
				{
					print(getString(R.string.files_can_not_be_pasted_here));
					return;
				}

				FileObjectType sourceFileObjectType = DetailFragment.CUT_COPY_FILE_OBJECT_TYPE;
				String source_folder = DetailFragment.CUT_COPY_FILECLICKSELECTED;
				if (sourceFileObjectType.equals(df.fileObjectType) && source_folder.equals(df.fileclickselected)) {
					print(DetailFragment.COPY_SELECTED ? getString(R.string.selected_files_have_been_copied) : getString(R.string.selected_filed_have_been_moved));
				} else  {
					if(DetailFragment.CUT_COPY_FILE_OBJECT_TYPE==FileObjectType.SEARCH_LIBRARY_TYPE)
					{
						//process to have only files with unique file names
						Set<String> file_name_set=new HashSet<>();
						Iterator<String> iterator=DetailFragment.FILE_SELECTED_FOR_CUT_COPY.iterator();
						while(iterator.hasNext())
						{
							String file_path=iterator.next();
							boolean inserted=file_name_set.add(new File(file_path).getName());
							if(inserted) files_selected_array.add(file_path);
						}

					}
					else
					{
						files_selected_array = new ArrayList<>(DetailFragment.FILE_SELECTED_FOR_CUT_COPY);
					}

					PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder, files_selected_array, sourceFileObjectType,
							df.fileObjectType, df.fileclickselected, DetailFragment.CUT_SELECTED);
					pasteSetUpDialog.show(fm, "paste_dialog");

				}
				DetailFragment.FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
				DetailFragment.CUT_COPY_FILE_OBJECT_TYPE = null;
				DetailFragment.CUT_COPY_FILECLICKSELECTED="";
				paste_pastecancel_view_procedure(df);
			} else if (id == R.id.toolbar_btn_4) {
				if (df.mselecteditems.size() > 0) {
					size = df.mselecteditemsFilePath.size();
					for (int i = 0; i < size; ++i) {
						int key = df.mselecteditemsFilePath.keyAt(i);
						files_selected_array.add(df.mselecteditemsFilePath.get(key));
						files_selected_index_array.add(key);
					}

					DeleteFileAlertDialog deleteFileAlertDialog = DeleteFileAlertDialog.getInstance(files_selected_array,df.fileObjectType,df.fileclickselected,false);
					deleteFileAlertDialog.show(fm, "delete_dialog");
				} else {
					print(getString(R.string.select_files_to_delete));
				}
			} else if (id == R.id.toolbar_btn_5) {
				DetailFragment.FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
				paste_pastecancel_view_procedure(df);
			}
			df.clearSelectionAndNotifyDataSetChanged();
		}
	}

	private void paste_pastecancel_view_procedure(DetailFragment df)
	{
		DetailFragment.CUT_SELECTED=false;
		DetailFragment.COPY_SELECTED=false;
		bottom_toolbar.setVisibility(View.VISIBLE);
		bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
		paste_toolbar.setVisibility(View.GONE);
		actionmode_toolbar.setVisibility(View.GONE);
		extract_toolbar.setVisibility(View.GONE);
		toolbar_shown="bottom";
		df.is_toolbar_visible=true;
	}

	private class DrawerEndButtButtonsClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View v) {
			int id = v.getId();
			if (id == R.id.toolbar_btn_1) {
				if (getCacheDir().exists()) {
					new AsyncTaskDeleteDirectory(getCacheDir()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				if (ARCHIVE_EXTRACT_DIR.exists()) {
					new AsyncTaskDeleteDirectory(ARCHIVE_EXTRACT_DIR).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				finish();
			} else if (id == R.id.toolbar_btn_2) {
				final ProgressBarFragment pbf = ProgressBarFragment.getInstance();
				pbf.show(fm, "");
				drawerLayout.closeDrawer(drawer);

				Handler h = new Handler();
				h.postDelayed(new Runnable() {
					@Override
					public void run() {
						PreferencesDialog preferencesDialog = new PreferencesDialog();
						preferencesDialog.show(fm, "preferences_dialog");
						pbf.dismissAllowingStateLoss();

					}
				}, 500);
			}
		}
	}

	private class StorageRecyclerAdapter extends RecyclerView.Adapter<StorageRecyclerAdapter.ViewHolder>
	{
		final List<FilePOJO> storage_dir_arraylist;
		StorageRecyclerAdapter(List<FilePOJO> storage_dir_arraylist)
		{
			this.storage_dir_arraylist=storage_dir_arraylist;
		}

		class ViewHolder extends RecyclerView.ViewHolder
		{
			final View v;
			final ImageView imageview;
			final TextView textView_storage_dir;

			ViewHolder(View v)
			{
				super(v);
				this.v=v;
				imageview=v.findViewById(R.id.image_storage_dir);
				textView_storage_dir=v.findViewById(R.id.text_storage_dir_name);

				v.setOnClickListener(new View.OnClickListener()
					{
						public void onClick(View p)
						{
							DRAWER_STORAGE_FILEPOJO_SELECTED=storage_dir_arraylist.get(getBindingAdapterPosition());
							drawerLayout.closeDrawer(drawer);
						}
					});
			}
		}

		@Override
		public StorageRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method

			View v=LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout,p1,false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(StorageRecyclerAdapter.ViewHolder p1, int p2)
		{
			// TODO: Implement this method

			FilePOJO filePOJO=storage_dir_arraylist.get(p2);
			String file_path=filePOJO.getPath();
			FileObjectType fileObjectType=filePOJO.getFileObjectType();

			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				if(Global.GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR().getPath().equals(file_path)) {
					p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
				}
				else
				{
					p1.imageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
				}
				p1.textView_storage_dir.setText(filePOJO.getName());

			}
			else if(fileObjectType== FileObjectType.USB_TYPE)
			{
				p1.imageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
				p1.textView_storage_dir.setText(DetailFragment.USB_FILE_PREFIX+ filePOJO.getName());
			}
			else if(fileObjectType==FileObjectType.ROOT_TYPE)
			{
				p1.imageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.device_icon));
				p1.textView_storage_dir.setText(R.string.root_directory);

			}

		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return storage_dir_arraylist.size();
		}
	}

	private class LibraryRecyclerAdapter extends RecyclerView.Adapter<LibraryRecyclerAdapter.ViewHolder>
	{
		final List<String> storage_dir_arraylist;
		LibraryRecyclerAdapter(List<String> storage_dir_arraylist)
		{
			this.storage_dir_arraylist=storage_dir_arraylist;
		}

		class ViewHolder extends RecyclerView.ViewHolder
		{
			final View v;
			final ImageView imageview;
			final ImageView overlay_imageview;
			final TextView textView_recent_dir;

			ViewHolder(View v)
			{
				super(v);
				this.v=v;
				imageview=v.findViewById(R.id.image_storage_dir);
				overlay_imageview=v.findViewById(R.id.overlay_image_storage_dir);
				textView_recent_dir=v.findViewById(R.id.text_storage_dir_name);
				imageview.setVisibility(View.GONE);
				overlay_imageview.setVisibility(View.GONE);
				v.setOnClickListener(new View.OnClickListener()
					{

						public void onClick(View p)
						{
							String name=storage_dir_arraylist.get(getBindingAdapterPosition());
							DRAWER_STORAGE_FILEPOJO_SELECTED=new FilePOJO(FileObjectType.SEARCH_LIBRARY_TYPE,name,name.toLowerCase(),null,name,false,0L,null,0L,null,R.drawable.folder_icon,null,0,0,0,0L,null,0,null);
							drawerLayout.closeDrawer(drawer);
						}
					});
			}
		}

		@Override
		public LibraryRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			View v=LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout,p1,false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(LibraryRecyclerAdapter.ViewHolder p1, int p2)
		{
			// TODO: Implement this method
			p1.textView_recent_dir.setText(storage_dir_arraylist.get(p2));
		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return storage_dir_arraylist.size();
		}

	}
	private class ArchiveViewAsyncTask1 extends svl.kadatha.filex.AsyncTask<Void,Void,Boolean>
	{
		final Uri data;
		boolean success;
		InputStream inStream;
		ZipInputStream zipInputStream;
		final ProgressBarFragment progressbarFragment=ProgressBarFragment.getInstance();
		ArchiveViewAsyncTask1(Uri data)
		{
			this.data=data;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressbarFragment.show(fm,"progressbar_dialog");
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			if(ARCHIVE_EXTRACT_DIR.exists())
			{
				FileUtil.deleteNativeDirectory(ARCHIVE_EXTRACT_DIR);
			}
			try {
				inStream=getContentResolver().openInputStream(data);
				zipInputStream=new ZipInputStream(inStream);
				ZipEntry zipEntry=zipInputStream.getNextEntry();
				while (zipEntry!=null){
					File f=new File(ARCHIVE_EXTRACT_DIR,zipEntry.getName());
					if(zipEntry.isDirectory() && !f.exists())
					{
						success=f.mkdirs();
					}
					else if(!zipEntry.isDirectory())
					{
						if(!f.getParentFile().exists())
						{
							success=f.getParentFile().mkdirs();
						}
						try
						{
							success=f.createNewFile();
						}
						catch(IOException e)
						{
							success=false;
						}
					}
					zipEntry=zipInputStream.getNextEntry();
				}

			} catch(IOException e){
				success=false;
			} finally {
				try {
					inStream.close();
					return success;
				}
				catch (IOException e){
					success=false;
				}
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			progressbarFragment.dismissAllowingStateLoss();
			archive_view=result;
			if(result)
			{
				toolbar_shown_prior_archive=toolbar_shown;
				createFragmentTransaction(ARCHIVE_EXTRACT_DIR.getAbsolutePath(),FileObjectType.FILE_TYPE);
			}

			else
			{
				if(ARCHIVE_EXTRACT_DIR.exists())
				{
					FileUtil.deleteNativeDirectory(ARCHIVE_EXTRACT_DIR);
				}

			}
		}
	}
	private class ArchiveViewAsyncTask extends svl.kadatha.filex.AsyncTask<Void, Void, Boolean>
	{
		final ZipFile zipfile;
		boolean success;
		final ProgressBarFragment progressbarFragment=ProgressBarFragment.getInstance();
		ArchiveViewAsyncTask(ZipFile zip_file)
		{
			this.zipfile=zip_file;
		}
		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			progressbarFragment.show(fm,"progressbar_dialog");
		}

		@Override
		protected Boolean doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			if(ARCHIVE_EXTRACT_DIR.exists())
			{
				FileUtil.deleteNativeDirectory(ARCHIVE_EXTRACT_DIR);
			}

			Enumeration<? extends ZipEntry> zip_entries=zipfile.entries();
			while(zip_entries.hasMoreElements())
			{
				ZipEntry zipentry=zip_entries.nextElement();
				File f=new File(ARCHIVE_EXTRACT_DIR,zipentry.getName());
				if(zipentry.isDirectory() && !f.exists())
				{
					success=f.mkdirs();
				}
				else if(!zipentry.isDirectory())
				{
					if(!f.getParentFile().exists())
					{
						success=f.getParentFile().mkdirs();
					}
					try
					{
						success=f.createNewFile();
					}
					catch(IOException e)
					{
						success=false;
					}
				}
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			progressbarFragment.dismissAllowingStateLoss();
			archive_view=result;
			if(result)
			{
				toolbar_shown_prior_archive=toolbar_shown;
				createFragmentTransaction(ARCHIVE_EXTRACT_DIR.getAbsolutePath(),FileObjectType.FILE_TYPE);
			}
			else
			{
				if(ARCHIVE_EXTRACT_DIR.exists())
				{
					FileUtil.deleteNativeDirectory(ARCHIVE_EXTRACT_DIR);
				}
			}
		}

	}


	public class AsyncTaskDeleteDirectory extends svl.kadatha.filex.AsyncTask<Void,Void,Boolean>
	{
		final File dir;
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		AsyncTaskDeleteDirectory(File f)
		{
			dir=f;
		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			pbf.show(((AppCompatActivity)context).getSupportFragmentManager(),"progressbar_dialog");
		}

		@Override
		protected Boolean doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			{
				FileUtil.deleteNativeDirectory(dir);
			}
			return !dir.exists();
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			pbf.dismissAllowingStateLoss();
		}
	}


	private void setupDevice() {
		boolean usb_path_added=false;
		if(usbFileRoot==null)
		{
			UsbMassStorageDevice device=UsbDocumentProvider.USB_MASS_STORAGE_DEVICES.get(0);
			try {
				device.init();
				usbCurrentFs = device.getPartitions().get(0).getFileSystem();
				usbFileRoot = usbCurrentFs.getRootDirectory();
				FileUtil.USB_CHUNK_SIZE=usbCurrentFs.getChunkSize();
				if(FileUtil.USB_CHUNK_SIZE==0)
				{
					FileUtil.USB_CHUNK_SIZE=FileUtil.BUFFER_SIZE;
				}

			} catch (IOException e) {

			}
		}

		for(FilePOJO filePOJO: Global.STORAGE_DIR)
		{
			if (filePOJO.getFileObjectType()== FileObjectType.USB_TYPE && filePOJO.getPath().equals(Global.USB_STORAGE_PATH)) {
				usb_path_added = true;
				break;
			}

		}

		if(!usb_path_added)
		{

			Global.USB_STORAGE_PATH=usbFileRoot.getAbsolutePath();
			Global.STORAGE_DIR.add(FilePOJOUtil.MAKE_FilePOJO(usbFileRoot,false));
			Global.WORKOUT_AVAILABLE_SPACE();
			storageRecyclerAdapter.notifyDataSetChanged();
		}
	}

	private void discoverDevice()
	{
		if(UsbDocumentProvider.USB_MASS_STORAGE_DEVICES.size()>0)
		{
			setupDevice();
		}
	}

	private class USBReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context p1, Intent intent)
		{
			// TODO: Implement this method
			String action = intent.getAction();
			if (UsbDocumentProvider.USB_ATTACH_BROADCAST.equals(action)) {

				if(intent.getBooleanExtra(UsbDocumentProvider.USB_ATTACHED,false))
				{
					setupDevice();
				}
				else
				{
					usbCurrentFs=null;
					usbFileRoot=null;

					Iterator<FilePOJO> iterator=Global.STORAGE_DIR.iterator();
					while(iterator.hasNext())
					{
						if(iterator.next().getFileObjectType()==FileObjectType.USB_TYPE)
						{
							iterator.remove();
						}
					}

					storageRecyclerAdapter.notifyDataSetChanged();

					int size=DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.size();
					for(int i=0;i<size;++i)
					{
						DetailFragmentCommunicationListener listener=DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.get(i);
						if(listener!=null)
						{
							listener.setUsbFileRootNull();
						}
					}

					Iterator<FilePOJO> iterator1=MainActivity.RECENTS.iterator();
					while (iterator1.hasNext())
					{
						if(iterator1.next().getFileObjectType()==FileObjectType.USB_TYPE)
						{
							iterator1.remove();
						}
					}

					FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""),FileObjectType.USB_TYPE);
				}
			}
			if(recentDialogListener!=null)
			{
				recentDialogListener.onMediaAttachedAndRemoved();
			}
		}
	}

	@Override
	public void onMediaMount(String action) {

		switch (action) {
			case "android.intent.action.MEDIA_MOUNTED":
				Global.STORAGE_DIR.clear();
				Global.STORAGE_DIR.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
				Global.WORKOUT_AVAILABLE_SPACE();
				storageRecyclerAdapter.notifyDataSetChanged();
				if (recentDialogListener != null) {
					recentDialogListener.onMediaAttachedAndRemoved();
				}
				break;

			case "android.intent.action.MEDIA_EJECT":
			case "android.intent.action.MEDIA_REMOVED":
			case "android.intent.action.MEDIA_BAD_REMOVAL":

				Global.STORAGE_DIR.clear();
				Global.STORAGE_DIR.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
				Global.WORKOUT_AVAILABLE_SPACE();
				storageRecyclerAdapter.notifyDataSetChanged();
				FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(Global.EXTERNAL_STORAGE_PATH), FileObjectType.FILE_TYPE);
				DetailFragment df=(DetailFragment)fm.findFragmentById(R.id.detail_fragment);
				if(df!=null) df.clearSelectionAndNotifyDataSetChanged();
				if (recentDialogListener != null) {
					recentDialogListener.onMediaAttachedAndRemoved();
				}
				break;
		}
	}

	private class OtherActivityBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
			String activity_name=intent.getStringExtra("activity_name");
			String file_path=intent.getStringExtra("file_path");
			FileObjectType fileObjectType= (FileObjectType) intent.getSerializableExtra("fileObjectType");
			switch (intent.getAction()) {

				case Global.LOCAL_BROADCAST_DELETE_FILE_ACTION:
					if (df != null) df.local_activity_delete = true;
					break;
				case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
					if (df != null) df.modification_observed = true;
					break;
				case Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION:
					int size = DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.size();
					for(int i=0;i<size;++i)
					{
						DetailFragmentCommunicationListener listener=DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.get(i);
						if(listener!=null)
						{
							listener.onFragmentCacheClear(file_path,fileObjectType);
						}
					}
					break;
			}
		}
	}


	interface DetailFragmentCommunicationListener
	{
		void onFragmentCacheClear(String file_path, FileObjectType fileObjectType);
		void setUsbFileRootNull();
	}

	public void addFragmentCommunicationListener(DetailFragmentCommunicationListener listener)
	{
		DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.add(listener);
	}

	public void removeFragmentCommunicationListener(DetailFragmentCommunicationListener listener)
	{
		DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.remove(listener);
	}


	interface RecentDialogListener
	{
		void onMediaAttachedAndRemoved();
	}

	private void print(String msg)
	{
		android.widget.Toast.makeText(this,msg,android.widget.Toast.LENGTH_SHORT).show();
	}

}
