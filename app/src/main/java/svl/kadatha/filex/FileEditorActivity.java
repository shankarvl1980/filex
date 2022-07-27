package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import me.jahnen.libaums.core.fs.UsbFile;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class FileEditorActivity extends BaseActivity implements FileEditorSettingsDialog.EOL_ChangeListener, SaveFileConfirmationDialog.SaveFileListener
{
    File file;
//	private FilePOJO currently_shown_file;
	public boolean fromArchiveView,fromThirdPartyApp;
	private String source_folder;
	FileSaveServiceConnection serviceConnection;
	private boolean fileServiceBound;
	private List<FilePOJO> files_selected_for_delete;
	//private List<FilePOJO> deleted_files;
	//private DeleteFileAsyncTask delete_file_async_task;
	private boolean asynctask_running;
	private boolean permission_requested;
	private String tree_uri_path="";
	private Uri tree_uri;
	EditText filetext_container_edittext;
	private Button edit_button,undo_button,redo_button,save_button,up_button,down_button;
    private boolean edit_mode;
	static boolean NOT_WRAP=true;
	static final int EOL_N = 0;
	static final int EOL_R = 1;
	static final int EOL_RN = 2;
	//int eol,altered_eol;
	static float FILE_EDITOR_TEXT_SIZE;
//	private TextViewUndoRedoBatch textViewUndoRedo;
	private TextView file_name;
	private final int request_code=876;
	private final String preference_name="undoredo";
	private SaveFileConfirmationDialog saveConfirmationAlertDialog;
    private svl.kadatha.filex.ObservableScrollView scrollview;
	private FileEditorSettingsDialog fileEditorSettingsDialog;
//	private FileOpenAsyncTask fileOpenAsyncTask;
//	private boolean updated=true,to_be_closed_after_save;
	private Context context;
//	private String action_after_save="";
	static int LINE_NUMBER_SIZE;
    TinyDB tinyDB;
//	private CancelableProgressBarDialog cpbf;
	private final LinkedHashMap<Integer, Long> page_pointer_hashmap=new LinkedHashMap<>();
//	private int current_page=0;
//	private long current_page_end_point=0L;
//	private boolean file_start,file_end;
	boolean isWritable,isFileBig;
//	private File temporary_file_for_save;
	private ProgressBarFragment pbf;
	private Class emptyService;
//    private String file_path;
	private Uri data;
//	private boolean file_loading_started;
	private KeyBoardUtil keyBoardUtil;
//	private boolean file_format_supported=true;
	public static final int BUFFER_SIZE=8192;
	public FileObjectType fileObjectType;
	private PopupWindow listPopWindow;
	public FragmentManager fm;
	private LocalBroadcastManager localBroadcastManager;
	private InputMethodManager imm;
	public static final String ACTIVITY_NAME="FILE_EDITOR_ACTIVITY";
	public boolean clear_cache;
	private static final String CANCEL_PROGRESS_REQUEST_CODE="file_editor_cancel_progress_request_code";
	private static final String DELETE_FILE_REQUEST_CODE="text_file_delete_request_code";
	private final static String SAF_PERMISSION_REQUEST_CODE="file_editor_saf_permission_request_code";
	private FrameLayout progress_bar;
	public FileEditorViewModel viewModel;

    @Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		
		context=this;
		fm=getSupportFragmentManager();
		localBroadcastManager = LocalBroadcastManager.getInstance(context);
		tinyDB=new TinyDB(context);

		NOT_WRAP=tinyDB.getBoolean("file_editor_not_wrap");
		if(NOT_WRAP)
		{
			setContentView(R.layout.activity_file_editor_horizontal_scroll);
		}
		else
		{
			setContentView(R.layout.activity_file_editor);
		}
		viewModel=new ViewModelProvider(this).get(FileEditorViewModel.class);

		viewModel.eol=viewModel.altered_eol=EOL_N;
		FILE_EDITOR_TEXT_SIZE=tinyDB.getFloat("file_editor_text_size");
		if(FILE_EDITOR_TEXT_SIZE<=0 || FILE_EDITOR_TEXT_SIZE> FileEditorSettingsDialog.MAX_TEXT_SIZE)
		{
			FILE_EDITOR_TEXT_SIZE=14F;
			tinyDB.putFloat("file_editor_text_size",FILE_EDITOR_TEXT_SIZE);
		}
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		LINE_NUMBER_SIZE=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10,getResources().getDisplayMetrics());
		fileEditorSettingsDialog=new FileEditorSettingsDialog();

		scrollview=findViewById(R.id.file_editor_scrollview);
		keyBoardUtil=new KeyBoardUtil(scrollview);

		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        Toolbar top_toolbar = findViewById(R.id.file_editor_top_toolbar);
		progress_bar=findViewById(R.id.file_editor_progressbar);
        FloatingActionButton floating_back_button = findViewById(R.id.file_editor_floating_action_button_back);
		floating_back_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
		
		EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(this,6,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] bottom_drawables ={R.drawable.edit_icon,R.drawable.undo_icon,R.drawable.redo_icon,R.drawable.save_icon,R.drawable.up_caret_icon,R.drawable.down_caret_icon};
		String [] titles={getString(R.string.edit),getString(R.string.undo),getString(R.string.redo),getString(R.string.save),getString(R.string.up),getString(R.string.down)};
		tb_layout.setResourceImageDrawables(bottom_drawables,titles);

        Toolbar bottom_toolbar = findViewById(R.id.file_editor_bottom_toolbar);
		bottom_toolbar.addView(tb_layout);
		edit_button= bottom_toolbar.findViewById(R.id.toolbar_btn_1);
		undo_button= bottom_toolbar.findViewById(R.id.toolbar_btn_2);
		redo_button= bottom_toolbar.findViewById(R.id.toolbar_btn_3);
		save_button= bottom_toolbar.findViewById(R.id.toolbar_btn_4);
		up_button= bottom_toolbar.findViewById(R.id.toolbar_btn_5);
		down_button= bottom_toolbar.findViewById(R.id.toolbar_btn_6);
		file_name=findViewById(R.id.file_editor_file_name_textview);
        ImageButton overflow = findViewById(R.id.file_editor_overflow_btn);
		overflow.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View p1)
			{
				listPopWindow.showAsDropDown(p1,0,Global.SIX_DP);
			}
		});

        ArrayList<ListPopupWindowPOJO> list_popupwindowpojos = new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.settings_icon,getString(R.string.settings)));


		listPopWindow=new PopupWindow(context);
		ListView listView=new ListView(context);
		listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapater(context, list_popupwindowpojos));
		listPopWindow.setContentView(listView);
		listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
		listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		listPopWindow.setFocusable(true);
		listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterview, View v, int p1,long p2)
			{

				final Bundle bundle=new Bundle();
				final ArrayList<String> files_selected_array=new ArrayList<>();

				switch(p1)
				{
					case 0:

						if(fromThirdPartyApp)
						{
							Global.print(context,getString(R.string.not_able_to_process));
							break;
						}
						files_selected_array.add(viewModel.currently_shown_file.getPath());
						DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity=DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE,files_selected_array,fileObjectType);
						/*
						deleteFileAlertDialogOtherActivity.setDeleteFileDialogListener(new DeleteFileAlertDialogOtherActivity.DeleteFileAlertDialogListener()
						{
							public void onSelectOK()
							{
								if(!asynctask_running)
								{
									asynctask_running=true;
									files_selected_for_delete=new ArrayList<>();
									deleted_files=new ArrayList<>();
									files_selected_for_delete.add(currently_shown_file);
									delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,fileObjectType);
									delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
								}

							}
						});

						 */
						deleteFileAlertDialogOtherActivity.show(fm,"deletefilealertotheractivity");
						break;

					case 1:
						Uri src_uri=null;
						if(fromThirdPartyApp)
						{
							src_uri=data;
						}
						else if(fileObjectType==FileObjectType.FILE_TYPE)
						{
							src_uri= FileProvider.getUriForFile(context, context.getPackageName()+".provider",new File(viewModel.currently_shown_file.getPath()));
						}
						if(src_uri==null)
						{
							Global.print(context,getString(R.string.not_able_to_process));
							break;
						}
						ArrayList<Uri> uri_list=new ArrayList<>();
						uri_list.add(src_uri);
						FileIntentDispatch.sendUri(context,uri_list);

						break;

					case 2:
						if(fromThirdPartyApp)
						{
							Global.print(context,getString(R.string.not_able_to_process));
							break;
						}
						files_selected_array.add(viewModel.currently_shown_file.getPath());
						PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,fileObjectType);
						propertiesDialog.show(fm,"properties_dialog");
						break;

					case 3:
						fileEditorSettingsDialog.show(fm,"file_editor_overflow");
						break;
					default:
						break;

				}
				listPopWindow.dismiss();

			}


		});


		//cpbf=CancelableProgressBarDialog.getInstance(CANCEL_PROGRESS_REQUEST_CODE);
		fm.setFragmentResultListener(CANCEL_PROGRESS_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(CANCEL_PROGRESS_REQUEST_CODE))
				{
					//fileOpenAsyncTask.cancel(true);
					viewModel.cancel(true);
				}
			}
		});

		viewModel.isReadingFinished.observe(FileEditorActivity.this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				if(aBoolean)
				{
					if(!viewModel.fileRead)
					{
						viewModel.file_start=viewModel.file_end=true;
						Global.print(context,getString(R.string.file_could_not_be_opened));
					}
					viewModel.file_format_supported=viewModel.fileRead;
					viewModel.textViewUndoRedo.stopListening();
					viewModel.textViewUndoRedo.clearHistory();
					undo_button.setEnabled(false);
					undo_button.setAlpha(Global.DISABLE_ALFA);
					redo_button.setEnabled(false);
					redo_button.setAlpha(Global.DISABLE_ALFA);

					if(viewModel.file_start)
					{
						up_button.setEnabled(false);
						up_button.setAlpha(Global.DISABLE_ALFA);

					}
					else
					{
						up_button.setEnabled(true);
						up_button.setAlpha(Global.ENABLE_ALFA);
					}

					if(viewModel.file_end)
					{
						down_button.setEnabled(false);
						down_button.setAlpha(Global.DISABLE_ALFA);
					}
					else
					{
						down_button.setEnabled(true);
						down_button.setAlpha(Global.ENABLE_ALFA);
					}


					{

						filetext_container_edittext.setText(viewModel.stringBuilder.toString());
						scrollview.smoothScrollTo(0,0);

					}

					viewModel.textViewUndoRedo.startListening();
//			cpbf.dismissAllowingStateLoss();
					progress_bar.setVisibility(View.GONE);
//					viewModel.file_loading_started=false;


					progress_bar.setVisibility(View.GONE);
				}
			}
		});

		Intent intent=getIntent();
		if(intent!=null)
		{
			on_intent(intent, savedInstanceState);
		}

		scrollview.setScrollViewListener(new ObservableScrollView.ScrollViewListener()
		{
			boolean visible=true;
			final int threshold=5;
			int scroll_distance=0;
			int dy=0;
			public void onScrollChange(ObservableScrollView v, int old_scrollX, int old_scrollY, int scrollX, int scrollY)
			{
				dy=scrollY-old_scrollY;

				if(scroll_distance>threshold && !visible)
				{
					visible=true;
					scroll_distance=0;
				}
				else if(scroll_distance<-threshold && visible)
				{
					scroll_distance=0;
				}
				if((visible && dy<0) || (!visible && dy>0))
				{
					scroll_distance+=dy;
				}

			}
			
		});

        BottomToolbarListener bottomToolbarListener = new BottomToolbarListener();
		edit_button.setOnClickListener(bottomToolbarListener);
		undo_button.setOnTouchListener(new RepeatListener(400,101, bottomToolbarListener));
		redo_button.setOnTouchListener(new RepeatListener(400,101, bottomToolbarListener));
		save_button.setOnClickListener(bottomToolbarListener);
		up_button.setOnClickListener(bottomToolbarListener);
		down_button.setOnClickListener(bottomToolbarListener);


		filetext_container_edittext=findViewById(R.id.textfile_edittext);
		filetext_container_edittext.setTextSize(FILE_EDITOR_TEXT_SIZE);

		viewModel.textViewUndoRedo=new TextViewUndoRedoBatch(filetext_container_edittext,context);
		viewModel.textViewUndoRedo.setEditTextUndoRedoListener(new TextViewUndoRedoBatch.EditTextRedoUndoListener()
		{
			public void onEditTextChange()
			{

				undo_button.setEnabled(true);
				undo_button.setAlpha(Global.ENABLE_ALFA);
				save_button.setEnabled(true);
				save_button.setAlpha(Global.ENABLE_ALFA);
				viewModel.updated=false;

				redo_button.setEnabled(false);
				redo_button.setAlpha(Global.DISABLE_ALFA);

			}
		});

		onClick_edit_button();
/*
		fm.setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(DELETE_FILE_REQUEST_CODE))
				{
					if(!asynctask_running)
					{
						asynctask_running=true;
						files_selected_for_delete=new ArrayList<>();
						deleted_files=new ArrayList<>();
						files_selected_for_delete.add(currently_shown_file);
						delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,fileObjectType);
						delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
				}
			}
		});

 */

		fm.setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(DELETE_FILE_REQUEST_CODE))
				{
					progress_bar.setVisibility(View.VISIBLE);
					Uri tree_uri=result.getParcelable("tree_uri");
					String tree_uri_path=result.getString("tree_uri_path");

					files_selected_for_delete=new ArrayList<>();
					files_selected_for_delete.add(viewModel.currently_shown_file);
					DeleteFileOtherActivityViewModel deleteFileOtherActivityViewModel=new ViewModelProvider(FileEditorActivity.this).get(DeleteFileOtherActivityViewModel.class);
					deleteFileOtherActivityViewModel.deleteFilePOJO(files_selected_for_delete,fileObjectType,tree_uri,tree_uri_path);
					deleteFileOtherActivityViewModel.isFinished.observe(FileEditorActivity.this, new Observer<Boolean>() {
						@Override
						public void onChanged(Boolean aBoolean) {
							if(aBoolean)
							{
								if(deleteFileOtherActivityViewModel.deleted_files.size()>0)
								{
									FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleteFileOtherActivityViewModel.deleted_file_name_list,fileObjectType);
									Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,ACTIVITY_NAME);
									clear_cache=false;
									finish();
								}
								progress_bar.setVisibility(View.GONE);
							}
						}
					});

				}
			}
		});

		fm.setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE))
				{
					tree_uri=result.getParcelable("tree_uri");
					tree_uri_path=result.getString("tree_uri_path");
					start_file_save_service();
				}

			}
		});

	}

	private void on_intent(Intent intent, @Nullable Bundle savedInstanceState)
	{
		data=intent.getData();
        fromArchiveView = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
		fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
		viewModel.file_path=intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
		if(viewModel.file_path==null) viewModel.file_path=PathUtil.getPath(context,data);

		if(fileObjectType==null || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			fileObjectType=FileObjectType.FILE_TYPE;
			fromThirdPartyApp=true;
		}


		source_folder=new File(viewModel.file_path).getParent();
		if(fileObjectType==FileObjectType.USB_TYPE)
		{
			if(MainActivity.usbFileRoot!=null)
			{
				try {
					viewModel.currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(viewModel.file_path)),false);

				} catch (IOException e) {

				}
			}
		}
		else
		{
			viewModel.currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(viewModel.file_path),false,false,fileObjectType);
		}


		file=new File(viewModel.file_path);
		isWritable=FileUtil.isWritable(fileObjectType,viewModel.file_path);

		if(savedInstanceState==null)
		{
			if(data!=null)
			{
				if(file!=null)
				{
					file_name.setText(file.getName());
				}
				viewModel.eol=viewModel.altered_eol=getEOL(data);

				if(!openFile(viewModel.current_page_end_point))
				{
					clear_cache=false;
					finish();
				}

				if(file.exists())
				{
					long internal_available_space,external_available_space,file_size;
					file_size=file.length();
					for(FilePOJO filePOJO:Global.STORAGE_DIR)
					{
						if(filePOJO.getFileObjectType()!=FileObjectType.FILE_TYPE)
						{
							continue;
						}
						if(!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(filePOJO.getPath()))))
						{
							continue;
						}

						//if(filePOJO.getPath().endsWith("/0"))
						{
							internal_available_space=new File(Global.INTERNAL_PRIMARY_STORAGE_PATH).getUsableSpace();
							if(file_size*2.5>internal_available_space)
							{
								isFileBig=true;
							}
							else
							{
								viewModel.temporary_file_for_save=getExternalFilesDir("file_save_temp");
								isFileBig=false;
								break;
							}
						}
						// the following is for attempting to put cache in sd card
						/*
						else if(isFileBig || temporary_file_for_save==null)
						{
							for(UriPOJO uriPOJO:Global.URI_PERMISSION_LIST)
							{
								if(filePOJO.getPath()+File.separator.startsWith(uriPOJO.get_path()+File.separator))
								{
									external_available_space=new File(filePOJO.getPath()).getUsableSpace();
									if(file_size*2.5>external_available_space)
									{
										isFileBig=true;
									}
									else
									{
										File[] external_volumes=ContextCompat.getExternalFilesDirs(context,null);
										if(external_volumes.length>=2)
										{
											temporary_file_for_save=external_volumes[1];
											isFileBig=false;
											break;
										}
										else
										{
											isFileBig=true;
										}

									}
								}
							}

						}

						 */

					}
				}

			}
		}

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		on_intent(intent, null);
	}

	@Override
	protected void onStart()
	{
		// TODO: Implement this method
		super.onStart();
		if(emptyService!=null && serviceConnection!=null)
		{
			Intent file_save_service_intent=new Intent(context,emptyService);
			bindService(file_save_service_intent,serviceConnection,Context.BIND_AUTO_CREATE);
			fileServiceBound=true;
		}

		clear_cache=true;
		Global.WORKOUT_AVAILABLE_SPACE();
	}

	@Override
	protected void onStop()
	{
		// TODO: Implement this method
		super.onStop();
		if(serviceConnection!=null)
		{
			unbindService(serviceConnection);
			fileServiceBound=false;
		}

		if(!isFinishing() && !isChangingConfigurations() && clear_cache)
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



	@Override
	protected void onDestroy() {
		super.onDestroy();
		listPopWindow.dismiss(); // to avoid memory leak on orientation change
	}

	private boolean check_SAF_permission(String file_path, FileObjectType fileObjectType)
	{
		UriPOJO  uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}


		if(tree_uri_path.equals("")) {
			SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,file_path,fileObjectType);
			/*
			safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener() {
				public void onOKBtnClicked() {
					seekSAFPermission();
				}

				public void onCancelBtnClicked() {

				}
			});

			 */
			safpermissionhelper.show(fm, "saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}


	private void go_previous()
	{
		if(viewModel.file_start)// || viewModel.file_loading_started)
		{
			return;
		}
		if(data!=null)
		{
			file=new File(viewModel.file_path);
			long prev_page_end_point=0L;
			viewModel.current_page=viewModel.current_page-2;
			if(viewModel.current_page<=0)
			{
				viewModel.current_page=0;
			}
			else
			{
				prev_page_end_point=viewModel.page_pointer_hashmap.get(viewModel.current_page);
				viewModel.current_page--;
			}
			openFile(prev_page_end_point);
		}
	}

	private void go_next()
	{
		if(viewModel.file_end)// || viewModel.file_loading_started)
		{
			return;
		}

		if(data!=null)
		{
			file=new File(viewModel.file_path);
			if(viewModel.current_page!=0 )
			{
				viewModel.current_page_end_point=viewModel.page_pointer_hashmap.get(viewModel.current_page);
				openFile(viewModel.current_page_end_point);
			}

		}
	}

	private boolean openFile(long pointer)
	{
		try
		{
			ParcelFileDescriptor pfd=getContentResolver().openFileDescriptor(data,"r");
			FileDescriptor fd=pfd.getFileDescriptor();

			progress_bar.setVisibility(View.VISIBLE);
			viewModel.isReadingFinished.setValue(false);
			viewModel.openFile(file,new FileInputStream(fd),pointer, false);
//			fileOpenAsyncTask=new FileOpenAsyncTask(new FileInputStream(fd),pointer, false);
//			fileOpenAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			return true;
		}
		catch(FileNotFoundException e)
		{
			Global.print(context,getString(R.string.file_not_found));
			return false;
		}
		catch (IllegalArgumentException e)
		{
			Global.print(context,getString(R.string.file_could_not_be_opened));
			return false;
		}

	}

	@Override
	public void onEOLchanged(int eol)
	{
		// TODO: Implement this method
		if(!viewModel.file_format_supported)
		{
			return;
		}
		if(viewModel.eol!=eol)
		{
			save_button.setEnabled(true);
			save_button.setAlpha(Global.ENABLE_ALFA);
			viewModel.updated=false;
		}
	}


	@Override
	public void onBackPressed()
	{
		// TODO: Implement this method
		file_close_procedure();
	}

	@Override
	public void next_action(boolean save) {
		if (save) {
			start_file_save_service();
		} else {
			viewModel.updated = true;
			go_previous();
		}
	}

	@Override
	public void on_being_closed(boolean to_close_after_save) {
		if(to_close_after_save)
		{
			viewModel.to_be_closed_after_save=to_close_after_save;
			start_file_save_service();
		}
		else
		{
			clear_cache=false;
			finish();
		}
	}

	private class BottomToolbarListener implements View.OnClickListener
	{
		@Override
		public void onClick(View p1)
		{
			// TODO: Implement this method
			if(progress_bar.getVisibility()==View.VISIBLE)
			{
				Global.print(context,getString(R.string.please_wait));
				return;
			}

			int id = p1.getId();
			if (id == R.id.toolbar_btn_1) {
				if (fromArchiveView || fromThirdPartyApp) {
					Global.print(context,getString(R.string.cant_edit_this_file));
					return;
				}

				if (isFileBig) {
					Global.print(context,getString(R.string.file_is_big) + ", " + getString(R.string.cant_edit_this_file));
					return;
				}

				onClick_edit_button();
			} else if (id == R.id.toolbar_btn_2) {
				if (viewModel.textViewUndoRedo.getCanUndo()) {
					viewModel.textViewUndoRedo.undo();
					save_button.setEnabled(true);
					save_button.setAlpha(Global.ENABLE_ALFA);
					viewModel.updated = false;
					if (!viewModel.textViewUndoRedo.getCanUndo()) {
						undo_button.setEnabled(false);
						undo_button.setAlpha(Global.DISABLE_ALFA);

						save_button.setEnabled(false);
						save_button.setAlpha(Global.DISABLE_ALFA);
						viewModel.updated = true;
					}

					if (viewModel.textViewUndoRedo.getCanRedo()) {
						redo_button.setEnabled(true);
						redo_button.setAlpha(Global.ENABLE_ALFA);
					}
				}
			} else if (id == R.id.toolbar_btn_3) {
				if (viewModel.textViewUndoRedo.getCanRedo()) {
					viewModel.textViewUndoRedo.redo();
					viewModel.updated = false;
					if (!viewModel.textViewUndoRedo.getCanRedo()) {
						redo_button.setEnabled(false);
						redo_button.setAlpha(Global.DISABLE_ALFA);
					}

					if (viewModel.textViewUndoRedo.getCanUndo()) {
						undo_button.setEnabled(true);
						undo_button.setAlpha(Global.ENABLE_ALFA);
						save_button.setEnabled(true);
						save_button.setAlpha(Global.ENABLE_ALFA);
					}
				}
			} else if (id == R.id.toolbar_btn_4) {
				edit_mode = false;
				start_file_save_service();
			} else if (id == R.id.toolbar_btn_5) {
				if (!viewModel.updated) {

					saveConfirmationAlertDialog = SaveFileConfirmationDialog.getInstance(false);
					/*
					saveConfirmationAlertDialog.setSaveFileListener(new SaveFileConfirmationDialog.SaveFileListener() {
						public void next_action(boolean save) {
							if (save) {
								start_file_save_service();
							} else {
								updated = true;
								go_previous();
							}
						}
					});

					 */
					saveConfirmationAlertDialog.show(fm, "saveconfirmationalert_dialog");
				} else {
					go_previous();
				}
			} else if (id == R.id.toolbar_btn_6) {
				if (!viewModel.updated) {

					saveConfirmationAlertDialog = SaveFileConfirmationDialog.getInstance(false);
					/*
					saveConfirmationAlertDialog.setSaveFileListener(new SaveFileConfirmationDialog.SaveFileListener() {
						public void next_action(boolean save) {
							if (save) {
								start_file_save_service();
							} else {
								updated = true;
								go_next();
							}
						}
					});

					 */
					saveConfirmationAlertDialog.show(fm, "saveconfirmationalert_dialog");

				} else {
					go_next();
				}
			}
		}

	}

	private void file_close_procedure()
	{
		if(keyBoardUtil.getKeyBoardVisibility())
		{
			imm.hideSoftInputFromWindow(filetext_container_edittext.getWindowToken(),0);
		}
		else if(!viewModel.updated)
		{

			saveConfirmationAlertDialog=SaveFileConfirmationDialog.getInstance(true);
			/*
			saveConfirmationAlertDialog.setSaveFileListener(new SaveFileConfirmationDialog.SaveFileListener()
				{
					public void next_action(boolean to_close_after_save)
					{
						if(to_close_after_save)
						{
							to_be_closed_after_save=to_close_after_save;
							start_file_save_service();
						}
						else
						{
							clear_cache=false;
							finish();
						}
					}
				});

			 */
			saveConfirmationAlertDialog.show(fm,"saveconfirmationalert_dialog");
		}
		else
		{
			viewModel.textViewUndoRedo.disconnect();
			clear_cache=false;
			finish();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);
		outState.putBoolean("edit_mode",edit_button.isSelected());
		outState.putString("file_name",file_name.getText().toString());
//		outState.putString("file_path",file_path);
//		outState.putBoolean("updated",updated);
//		outState.putBoolean("to_be_closed_after_save",to_be_closed_after_save);
//		outState.putInt("eol",eol);
//		outState.putInt("altered_eol",altered_eol);
//		outState.putSerializable("page_pointer_hashmap",page_pointer_hashmap);
//		outState.putInt("current_page",current_page);
//		outState.putLong("current_page_end_point",current_page_end_point);
//		outState.putBoolean("file_start",file_start);
//		outState.putBoolean("file_end",file_end);
//		outState.putString("action_after_save",action_after_save);
//		outState.putBoolean("file_format_supported",file_format_supported);
//		outState.putSerializable("temporary_file_for_save",temporary_file_for_save);
//		viewModel.textViewUndoRedo.storePersistentState(outState,preference_name);

		outState.putBoolean("clear_cache",clear_cache);

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onRestoreInstanceState(savedInstanceState);
		edit_mode=savedInstanceState.getBoolean("edit_mode");
		file_name.setText(savedInstanceState.getString("file_name"));
//		file_path=savedInstanceState.getString("file_path");
//		updated=savedInstanceState.getBoolean("updated");
//		to_be_closed_after_save=savedInstanceState.getBoolean("to_be_closed_after_save");
//		eol=savedInstanceState.getInt("eol");
//		altered_eol=savedInstanceState.getInt("altered_eol");
//		Serializable serializable = savedInstanceState.getSerializable("page_pointer_hashmap");
//		try {
//			page_pointer_hashmap=(LinkedHashMap<Integer,Long>)serializable;
//		} catch (ClassCastException e) {
//			clear_cache=false;
//			finish();
//		}
//
//		current_page=savedInstanceState.getInt("current_page");
//		current_page_end_point=savedInstanceState.getLong("current_page_end_point");
//		file_start=savedInstanceState.getBoolean("file_start");
//		file_end=savedInstanceState.getBoolean("file_end");
//		action_after_save=savedInstanceState.getString("action_after_save");
//		file_format_supported=savedInstanceState.getBoolean("file_format_supported");
//		temporary_file_for_save= (File) savedInstanceState.getSerializable("temporary_file_for_save");
//		textViewUndoRedo.restorePersistentState(savedInstanceState,preference_name);
//		textViewUndoRedo.startListening();
		onClick_edit_button();
		if(viewModel.file_start)
		{
			up_button.setEnabled(false);
			up_button.setAlpha(Global.DISABLE_ALFA);
		}
		if(viewModel.file_end)
		{
			down_button.setEnabled(false);
			down_button.setAlpha(Global.DISABLE_ALFA);
		}

		clear_cache=savedInstanceState.getBoolean("clear_cache");

	}

	/*
	public void seekSAFPermission()
	{
		clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		activityResultLauncher.launch(intent);
	}

	private final ActivityResultLauncher<Intent> activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
		@Override
		public void onActivityResult(ActivityResult result) {
			if (result.getResultCode()== RESULT_OK)
			{
				Uri treeUri;
				treeUri = result.getData().getData();
				Global.ON_REQUEST_URI_PERMISSION(context,treeUri);

				start_file_save_service();
			}
			else
			{
				Global.print(context,getString(R.string.permission_not_granted));
			}
		}
	});

	 */


	private void onClick_edit_button()
	{
		if(!viewModel.file_format_supported)
		{
			return;
		}

		if(edit_mode)
		{
			if(viewModel.textViewUndoRedo.getCanUndo())
			{
				undo_button.setEnabled(true);
				save_button.setEnabled(!viewModel.updated);
			}
			if(viewModel.textViewUndoRedo.getCanRedo())
			{
				redo_button.setEnabled(true);
			}

			// API 21
			filetext_container_edittext.setShowSoftInputOnFocus(true);
			filetext_container_edittext.requestFocus();
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

		}

		else
		{
			filetext_container_edittext.clearFocus();
			save_button.setEnabled(false);
			undo_button.setEnabled(false);
			redo_button.setEnabled(false);

			// API 21
			filetext_container_edittext.setShowSoftInputOnFocus(false);

			imm.hideSoftInputFromWindow(filetext_container_edittext.getWindowToken(),0);

		}
		edit_button.setSelected(edit_mode);
		setAlfaFileEditMenuItem();
		edit_mode=!edit_mode;
		filetext_container_edittext.setLongClickable(!edit_mode);
		filetext_container_edittext.setOnTouchListener(new View.OnTouchListener()
		{
			public boolean onTouch(View v, MotionEvent me)
			{
				return edit_mode;
			}
		});

	}

	private void setAlfaFileEditMenuItem()
	{
		edit_button.setAlpha(edit_button.isEnabled() ? Global.ENABLE_ALFA:Global.DISABLE_ALFA);
		save_button.setAlpha(save_button.isEnabled() ? Global.ENABLE_ALFA:Global.DISABLE_ALFA);
		undo_button.setAlpha(undo_button.isEnabled() ? Global.ENABLE_ALFA:Global.DISABLE_ALFA);
		redo_button.setAlpha(redo_button.isEnabled() ? Global.ENABLE_ALFA:Global.DISABLE_ALFA);
	}



	private void start_file_save_service()
	{

		if(!file.exists() || viewModel.temporary_file_for_save==null)
		{
			return;
		}

		long prev_page_end_point=0L;
		if(viewModel.current_page>1)
		{
			prev_page_end_point=viewModel.page_pointer_hashmap.get(viewModel.current_page-1);
		}
		Bundle bundle=new Bundle();
		bundle.putBoolean("isWritable",isWritable);
		bundle.putString("file_path",viewModel.file_path);
		bundle.putString("content",filetext_container_edittext.getText().toString());

		bundle.putInt("eol",viewModel.eol);
		bundle.putInt("altered_eol",viewModel.altered_eol);
		bundle.putLong("prev_page_end_point",prev_page_end_point);
		bundle.putLong("current_page_end_point",viewModel.current_page_end_point);
		bundle.putSerializable("page_pointer_hashmap",viewModel.page_pointer_hashmap);
		bundle.putString("temporary_file_path",viewModel.temporary_file_for_save.getAbsolutePath());
		bundle.putInt("current_page",viewModel.current_page);

		if (!isWritable) {
			if(!check_SAF_permission(viewModel.file_path,fileObjectType))
			{
				return;
			}
		}
		bundle.putString("tree_uri_path",tree_uri_path);
		bundle.putParcelable("tree_uri",tree_uri);
		pbf=ProgressBarFragment.newInstance();
		pbf.show(fm,"");
		emptyService=getEmptyService();
		if(emptyService==null)
		{
			Global.print(context,getString(R.string.maximum_2_services_only_be_processed_at_a_time));
			return;
		}
		serviceConnection=new FileSaveServiceConnection(emptyService);
		Intent file_save_service_intent=new Intent(context,emptyService);
		bindService(file_save_service_intent,serviceConnection,Context.BIND_AUTO_CREATE);

		file_save_service_intent.putExtra("bundle",bundle);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
		{
			startForegroundService(file_save_service_intent);
		}
		else
		{
			startService(file_save_service_intent);
		}

	}

	private class FileSaveServiceConnection implements ServiceConnection
	{

		final Class service;
		FileSaveServiceConnection(Class service)
		{
			this.service=service;
		}

		@Override
		public void onServiceConnected(ComponentName p1, IBinder binder)
		{
			// TODO: Implement this method
			switch(service.getName())
			{
				case "svl.kadatha.filex.FileSaveService1":
					final FileSaveService1 fileSaveService1=((FileSaveService1.FileSaveServiceBinder)binder).getService();
					if(fileSaveService1!=null)
					{
						fileSaveService1.setServiceCompletionListener(new FileSaveService1.FileSaveServiceCompletionListener()
						{
								public void onServiceCompletion(boolean result)
								{
									save_button.setEnabled(false);
									save_button.setAlpha(Global.DISABLE_ALFA);
									viewModel.current_page_end_point=fileSaveService1.page_pointer_hashmap.get(viewModel.current_page);
									viewModel.page_pointer_hashmap=fileSaveService1.page_pointer_hashmap;
									viewModel.updated=result;

									if(result)
									{
										viewModel.eol=viewModel.altered_eol;
										Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,ACTIVITY_NAME);
									}
									else
									{
										Global.print(context,getString(R.string.file_could_not_be_saved));
									}

									if(viewModel.to_be_closed_after_save)
									{
										clear_cache=false;
										finish();
									}
									else if(viewModel.action_after_save.equals("go_previous"))
									{
										go_previous();
									}
									else if(viewModel.action_after_save.equals("go_next"))
									{
										go_next();
									}

									if(pbf!=null)
									{
										pbf.dismissAllowingStateLoss();
									}
								}
						});
						fileServiceBound=true;
					}


					break;

				case "svl.kadatha.filex.FileSaveService2":
					final FileSaveService2 fileSaveService2=((FileSaveService2.FileSaveServiceBinder)binder).getService();
					if(fileSaveService2!=null)
					{
						fileSaveService2.setServiceCompletionListener(new FileSaveService2.FileSaveServiceCompletionListener()
							{
								public void onServiceCompletion(boolean result)
								{
									save_button.setEnabled(false);
									save_button.setAlpha(Global.DISABLE_ALFA);
									viewModel.current_page_end_point=fileSaveService2.page_pointer_hashmap.get(viewModel.current_page);
									viewModel.page_pointer_hashmap=fileSaveService2.page_pointer_hashmap;
									viewModel.updated=result;

									if(result)
									{
										viewModel.eol=viewModel.altered_eol;
										Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,ACTIVITY_NAME);
									}
									else
									{
										Global.print(context,getString(R.string.file_could_not_be_saved));
									}

									if(viewModel.to_be_closed_after_save)
									{
										clear_cache=false;
										finish();
									}
									else if(viewModel.action_after_save.equals("go_previous"))
									{
										go_previous();
									}
									else if(viewModel.action_after_save.equals("go_next"))
									{
										go_next();
									}

									if(pbf!=null)
									{
										pbf.dismissAllowingStateLoss();
									}
								}
							});
						fileServiceBound=true;
					}
					break;
				default:
					fileServiceBound=false;
					break;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName p1)
		{
			// TODO: Implement this method
			fileServiceBound=false;
		}

	}

/*
	private class FileOpenAsyncTask extends svl.kadatha.filex.AsyncTask<Void,StringBuilder,Boolean>
	{

		BufferedReader bufferedReader;
		final StringBuilder stringBuilder=new StringBuilder();
		long file_pointer;
		final FileInputStream fileInputStream;
		final boolean go_back;

		FileOpenAsyncTask (FileInputStream fileInputStream,long file_pointer, boolean go_back)
		{
			viewModel.file_loading_started=true;
			this.fileInputStream=fileInputStream;
			this.file_pointer=file_pointer;
			this.go_back=go_back;
		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
//			cpbf.set_title(getString(R.string.opening_the_file));
//			cpbf.show(fm,"progress_dialog");
			progress_bar.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			//cpbf.dismissAllowingStateLoss();
			progress_bar.setVisibility(View.GONE);
			clear_cache=false;
			finish();
		}

		@Override
		protected Boolean doInBackground(Void...f)
		{
			// TODO: Implement this method
			viewModel.file_start= file_pointer == 0L;
			try
			{

				FileChannel fc=fileInputStream.getChannel();
				fc.position(file_pointer);

				ByteBuffer buf=ByteBuffer.allocate(BUFFER_SIZE);
				int bytes_read;
				if(file_pointer!=0L)
				{
					boolean to_break=false;
					while((bytes_read=fc.read(buf))!=-1)
					{
						buf.flip();
						for(int i=0;i<bytes_read;++i)
						{
							char m=(char)buf.get(i);
							char n=0;
							if(i+1<bytes_read)
							{
								n=(char)buf.get(i+1);
							}

							file_pointer++;
							if(m==10)
							{
								to_break=true;
								viewModel.eol=viewModel.altered_eol=EOL_N;
								break;
							}
							else if(m==13)
							{
								if(n==10)
								{
									file_pointer++;
									viewModel.eol=viewModel.altered_eol=EOL_RN;

								}
								else
								{
									viewModel.eol=viewModel.altered_eol=EOL_R;
								}
								to_break=true;
								break;
							}
						}

						if(to_break)
						{
							break;
						}
					}
					viewModel.page_pointer_hashmap.put(viewModel.current_page,file_pointer);
				}

				buf.clear();
				fc.position(file_pointer);
				bufferedReader=new BufferedReader(Channels.newReader(fc,"UTF-8"));
				String line;
				int count=0;
				long br=0,total_bytes_read=0;
				int eol_len=(viewModel.eol==EOL_RN) ? 2 : 1;
                int max_lines_to_display = 200;
                while((line=bufferedReader.readLine())!=null)
				{
					br+=line.getBytes().length+eol_len;
					stringBuilder.append(line).append("\n");
					count++;
					if(count>= max_lines_to_display)
					{
						viewModel.file_end=false;
						total_bytes_read=file_pointer+br;

						break;
					}
				}
				if(count< max_lines_to_display)
				{
					viewModel.file_end=true;
					total_bytes_read=file.length();
				}

				viewModel.current_page++;
				viewModel.current_page_end_point=total_bytes_read;
				viewModel.page_pointer_hashmap.put(viewModel.current_page,viewModel.current_page_end_point);
				return true;

			} catch(IOException e)
			{

				return false;
			} finally
			{
				try
				{
					fileInputStream.close();
					if(bufferedReader!=null)
					{
						bufferedReader.close();
					}

				}
				catch(IOException e){}
			}

		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			if(!result)
			{
				viewModel.file_start=viewModel.file_end=true;
				Global.print(context,getString(R.string.file_could_not_be_opened));
			}
			viewModel.file_format_supported=result;
			viewModel.textViewUndoRedo.stopListening();
			viewModel.textViewUndoRedo.clearHistory();
			undo_button.setEnabled(false);
			undo_button.setAlpha(Global.DISABLE_ALFA);
			redo_button.setEnabled(false);
			redo_button.setAlpha(Global.DISABLE_ALFA);

			if(viewModel.file_start)
			{
				up_button.setEnabled(false);
				up_button.setAlpha(Global.DISABLE_ALFA);

			}
			else
			{
				up_button.setEnabled(true);
				up_button.setAlpha(Global.ENABLE_ALFA);
			}

			if(viewModel.file_end)
			{
				down_button.setEnabled(false);
				down_button.setAlpha(Global.DISABLE_ALFA);
			}
			else
			{
				down_button.setEnabled(true);
				down_button.setAlpha(Global.ENABLE_ALFA);
			}


			{

				filetext_container_edittext.setText(stringBuilder.toString());
				scrollview.smoothScrollTo(0,0);

			}

			viewModel.textViewUndoRedo.startListening();
//			cpbf.dismissAllowingStateLoss();
			progress_bar.setVisibility(View.GONE);
			viewModel.file_loading_started=false;
		}
		
	}

 */
	

	static Class getEmptyService()
	{
		Class emptyService=null;
		if(FileSaveService1.SERVICE_COMPLETED)
		{
			emptyService=FileSaveService1.class;
		}
		else if(FileSaveService2.SERVICE_COMPLETED)
		{
			emptyService=FileSaveService2.class;
		}

		return emptyService;
	}
	
	
	private int getEOL(Uri data)
	{
		int eol=EOL_N;
		BufferedReader bufferedReader=null;
		try
		{

            bufferedReader=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(data), StandardCharsets.UTF_8), BUFFER_SIZE);
			String line;
			line=bufferedReader.readLine();
			bufferedReader.close();
			if(line!=null)
			{
				bufferedReader=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(data), StandardCharsets.UTF_8), BUFFER_SIZE);
				int length=line.length();
				char [] c=new char[2];
				bufferedReader.skip(length);
				bufferedReader.read(c);
				int i=c[0];
				int o=c[1];
				if(i==10)
				{
					eol=EOL_N;
				}
				else if(i==13)
				{
					if(o==10)
					{
						eol=EOL_RN;
					}
					else
					{
						eol=EOL_R;
					}
				}
			}
			bufferedReader.close();
		}
		catch(IOException e)
		{
			
		}
		finally
		{
			try
			{
				if(bufferedReader!=null)bufferedReader.close();
			}
			catch(IOException e){}
			return eol;
		}
		
	}

/*
	private class DeleteFileAsyncTask extends svl.kadatha.filex.AsyncTask<Void,File,Boolean>
	{

		final List<FilePOJO> src_file_list;
		final List<String> deleted_file_name_list=new ArrayList<>();

		int counter_no_files;
		long counter_size_files;
		String current_file_name;
		boolean isFromInternal;
		String size_of_files_format;
		final ProgressBarFragment pbf=ProgressBarFragment.newInstance();
		final FileObjectType fileObjectType;
		DeleteFileAsyncTask(List<FilePOJO> src_file_list, FileObjectType fileObjectType)
		{
			this.src_file_list=src_file_list;
			this.fileObjectType=fileObjectType;
		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			try {
				pbf.show(fm,"progressbar_dialog");
			}
			catch (Exception e)
			{
				cancel(true);
				Global.print(context,getString(R.string.could_not_delete_file));
			}
		}

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			if(deleted_files.size()>0)
			{
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,ACTIVITY_NAME);
				viewModel.clear_cache=false;
				finish();

			}

			pbf.dismissAllowingStateLoss();
			asynctask_running=false;

		}

		@Override
		protected Boolean doInBackground(Void...p)
		{
			// TODO: Implement this method
			boolean success;

			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				isFromInternal=FileUtil.isFromInternal(fileObjectType,src_file_list.get(0).getPath());
			}
			success=deleteFromFolder();
			return success;
		}


		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method

			super.onPostExecute(result);
			if(deleted_files.size()>0)
			{
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,ACTIVITY_NAME);
				viewModel.clear_cache=false;
				finish();

			}

			pbf.dismissAllowingStateLoss();
			asynctask_running=false;

		}


		private boolean deleteFromFolder()
		{
			boolean success=false;
			int iteration=0;
			int size=src_file_list.size();
			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				if(isFromInternal)
				{
					for(int i=0;i<size;++i)
					{
						if(isCancelled())
						{
							return false;
						}
						FilePOJO filePOJO=src_file_list.get(i);
						File f=new File(filePOJO.getPath());
						current_file_name=f.getName();
						success=FileUtil.deleteNativeDirectory(f);
						if(success)
						{
							deleted_files.add(filePOJO);
							deleted_file_name_list.add(current_file_name);
						}
						files_selected_for_delete.remove(filePOJO);
					}

				}
				else
				{
					if(check_SAF_permission(src_file_list.get(0).getPath(),fileObjectType))
					{
						for(int i=0;i<size;++i)
						{
							if(isCancelled())
							{
								return false;
							}
							FilePOJO filePOJO=src_file_list.get(i);
							File file=new File(filePOJO.getPath());
							current_file_name=file.getName();
							success=FileUtil.deleteSAFDirectory(context,file.getAbsolutePath(),tree_uri,tree_uri_path);
							if(success)
							{
								deleted_files.add(filePOJO);
								deleted_file_name_list.add(current_file_name);
							}
							files_selected_for_delete.remove(filePOJO);
						}
					}

				}
			}
			else if(fileObjectType==FileObjectType.USB_TYPE)
			{
				for(int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return false;
					}
					FilePOJO filePOJO=src_file_list.get(i);
					UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,filePOJO.getPath());
					current_file_name=f.getName();
					success=FileUtil.deleteUsbDirectory(f);
					if(success)
					{
						deleted_files.add(filePOJO);
						deleted_file_name_list.add(current_file_name);
					}
					files_selected_for_delete.remove(filePOJO);
				}
			}

			return success;
		}

	}

 */



	private static class FileSaveBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context p1, Intent p2)
		{
			// TODO: Implement this method
		}

	}
}
