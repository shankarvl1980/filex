package svl.kadatha.filex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

import java.io.File;

public class ArchiveDeletePasteProgressActivity1 extends BaseActivity
{

	private Context context;
	private TextView from_textview;
	private TextView to_textview;
	private TextView copied_textview;
	private TextView no_files;
	private TextView size_files;
	private TextView total_no_of_files;
	private TextView total_size_files;
	private EditText current_file;
	private ServiceConnection serviceConnection;
	private ArchiveDeletePasteFileService1 archiveDeletePasteFileService;
	static boolean PROGRESS_ACTIVITY_SHOWN;
	private String intent_action;
	private FileObjectType sourceFileObjectType;
	private ProgressBar progressBar;
	private boolean clear_cache;
	public static final String ACTIVITY_NAME="ADPP_ACTIVITY_1";
	private TextView dialog_title,from_label,to_label;
	private TableRow to_table_row;
	private Handler handler;
	private TextView size_progress_text_view;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		context=this;
		setContentView(R.layout.fragment_cut_copy_delete_archive_progress);
		setFinishOnTouchOutside(false);
		handler=new Handler();
		dialog_title = findViewById(R.id.dialog_fragment_cut_copy_title);
		to_table_row = findViewById(R.id.fragment_cut_copy_delete_archive_totablerow);
		from_label = findViewById(R.id.dialog_fragment_cut_copy_delete_from_label);
		to_label = findViewById(R.id.dialog_fragment_cut_copy_delete_to_label);
		from_textview=findViewById(R.id.dialog_fragment_cut_copy_from);
		to_textview=findViewById(R.id.dialog_fragment_cut_copy_to);
		current_file=findViewById(R.id.dialog_fragment_cut_copy_archive_current_file);
		current_file.setKeyListener(null);
		copied_textview=findViewById(R.id.dialog_fragment_copied_file);
		copied_textview.setKeyListener(null);
		no_files=findViewById(R.id.fragment_cut_copy_delete_archive_no_files);
		size_files=findViewById(R.id.fragment_cut_copy_delete_archive_size_files);
		total_no_of_files=findViewById(R.id.fragment_cut_copy_delete_archive_total_no_files);
		total_size_files=findViewById(R.id.fragment_cut_copy_delete_archive_total_size_files);
		size_progress_text_view = findViewById(R.id.dialog_fragment_size_progress);
		progressBar=findViewById(R.id.fragment_cut_copy_delete_archive_progressBar);
		ViewGroup buttons_layout = findViewById(R.id.fragment_cut_copy_delete_progress_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(this,2,Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
		Button hide_button = buttons_layout.findViewById(R.id.first_button);
		hide_button.setText(R.string.hide);
		Button cancel_button = findViewById(R.id.second_button);
		cancel_button.setText(R.string.cancel);
		
		hide_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				PROGRESS_ACTIVITY_SHOWN=false;
				finish();
			}
			
		});
		
		cancel_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(archiveDeletePasteFileService!=null)
				{
					archiveDeletePasteFileService.cancelService();
				}
				Global.print(context,getString(R.string.process_cancelled));
				PROGRESS_ACTIVITY_SHOWN=false;
				finish();

			}
		});
		
		serviceConnection=new ServiceConnection()
		{
			public void onServiceConnected(ComponentName componentName,IBinder binder)
			{
				archiveDeletePasteFileService=((ArchiveDeletePasteFileService1.ArchiveDeletePasteBinder)binder).getService();
				archiveDeletePasteFileService.setServiceCompletionListener(new ArchiveDeletePasteFileService1.ServiceCompletionListener()
					{
						public void onServiceCompletion(String service_action,boolean result ,String target_file, String dest_folder)
						{
							switch(service_action)
							{
								case "archive-zip":
									Global.print(context,result ? getString(R.string.created)+" '"+target_file+"' "+getString(R.string.at)+" "+dest_folder : getString(R.string.could_not_create)+" '"+target_file+"'");
									break;
								
								case "archive-unzip":
									Global.print(context,result ? getString(R.string.unzipped)+" '"+target_file+"' "+getString(R.string.at)+" "+dest_folder : getString(R.string.could_not_extract)+" '"+target_file+"'");
									break;
									
								case "delete":
									Global.print(context,result ? getString(R.string.deleted_selected_files)+" "+dest_folder : getString(R.string.could_not_delete_selected_files)+" "+dest_folder);
									break;
									
								case "paste-cut":
									Global.print(context,result ? getString(R.string.moved_selected_files)+" "+dest_folder : getString(R.string.could_not_move_selected_files)+" "+dest_folder);
									break;
									
								case "paste-copy":
								case "copy_to":
									Global.print(context,result ? getString(R.string.copied_selected_files)+" "+dest_folder : getString(R.string.could_not_copy_selected_files)+" "+dest_folder);
									break;
							}
							finish();
						}

					});

				if(archiveDeletePasteFileService!=null)
				{
					bind_data();
				}
				if(ArchiveDeletePasteFileService1.SERVICE_COMPLETED)finish();
			}

			public void onServiceDisconnected(ComponentName componentName)
			{
				archiveDeletePasteFileService.setServiceCompletionListener(null);
				archiveDeletePasteFileService=null;
			}

		};

		Intent intent=getIntent();
		on_intent(intent,savedInstanceState);
	}

	private void on_intent(Intent intent, Bundle savedInstanceState)
	{
		if(intent!=null)
		{
			if(savedInstanceState==null)
			{
				intent_action=intent.getAction();
				Bundle bundle=intent.getBundleExtra("bundle");
				if(bundle!=null)
				{
					sourceFileObjectType= (FileObjectType) bundle.getSerializable("sourceFileObjectType");
					Intent adp_intent=new Intent(this,ArchiveDeletePasteFileService1.class);

					adp_intent.setAction(intent_action);
					adp_intent.putExtra("bundle",bundle);

					if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
					{
						startForegroundService(adp_intent);
					}
					else
					{
						startService(adp_intent);
					}
				}
			}
			else
			{
				intent_action=savedInstanceState.getString("intent_action");
				sourceFileObjectType= (FileObjectType) savedInstanceState.getSerializable("sourceFileObjectType");
			}

			switch(intent_action)
			{
				case "archive-zip":
					dialog_title.setText(R.string.archiving);
					to_label.setText(R.string.archive_file);
					break;
				case "archive-unzip":
					dialog_title.setText(R.string.extracting);
					from_label.setText(R.string.archive_file);
					to_label.setText(R.string.output_folder);
					break;
				case "delete":
					dialog_title.setText(R.string.deleting);
					to_table_row.setVisibility(View.GONE);
					break;
				case "paste-cut":
					dialog_title.setText(R.string.moving);
					break;
				case "paste-copy":
				case "copy_to":
					dialog_title.setText(R.string.copying);
					break;
			}

		}

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		on_intent(intent,null);
	}


	@Override
	protected void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		getWindow().setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		PROGRESS_ACTIVITY_SHOWN=true;
		Intent service_intent=new Intent(this,ArchiveDeletePasteFileService1.class);
		boolean bound=bindService(service_intent,serviceConnection,Context.BIND_AUTO_CREATE);
		handler.post(new Runnable() {
			@Override
			public void run() {
				if(bound)
				{
					if(archiveDeletePasteFileService!=null)
					{
						String size_progress=FileUtil.humanReadableByteCount(archiveDeletePasteFileService.total_bytes_read[0]);
						if(archiveDeletePasteFileService.fileCountSize!=null)
						{
							int percentage=(int)(archiveDeletePasteFileService.total_bytes_read[0]*100.0/archiveDeletePasteFileService.fileCountSize.total_size_of_files+0.5);
							progressBar.setProgress(percentage);
							size_progress_text_view.setText(percentage+"%");
						}
						else
						{
							progressBar.setIndeterminate(true);
						}

						switch(intent_action)
						{
							case "archive-zip":

							{
								current_file.setText(archiveDeletePasteFileService.zip_folder_name);
								copied_textview.setText(archiveDeletePasteFileService.copied_file_name);

								from_textview.setText(archiveDeletePasteFileService.zip_file_path);
								to_textview.setText(archiveDeletePasteFileService.dest_folder + File.separator + archiveDeletePasteFileService.zip_folder_name + ".zip");

								no_files.setText(""+archiveDeletePasteFileService.counter_no_files);
								size_files.setText(size_progress);
							}
							break;

							case "archive-unzip":

							{
								from_textview.setText(archiveDeletePasteFileService.zip_file_path);
								to_textview.setText(archiveDeletePasteFileService.zip_folder_name!=null ? archiveDeletePasteFileService.dest_folder+File.separator+archiveDeletePasteFileService.zip_folder_name : archiveDeletePasteFileService.dest_folder);
								current_file.setText(archiveDeletePasteFileService.current_file_name);
								copied_textview.setText(archiveDeletePasteFileService.copied_file_name);
								no_files.setText(getString(R.string.extracted) + ": " + archiveDeletePasteFileService.counter_no_files + (archiveDeletePasteFileService.counter_no_files < 2 ? " file" : " files"));
								size_files.setText(getString(R.string.size) + ": " + size_progress);
							}
							break;

							case "delete":
							{
								from_textview.setText(archiveDeletePasteFileService.source_folder);
								current_file.setText(archiveDeletePasteFileService.current_file_name);
								copied_textview.setText(archiveDeletePasteFileService.deleted_file_name);
								if(archiveDeletePasteFileService.isFromInternal)
								{
									no_files.setText(getString(R.string.deleted) + ": " + archiveDeletePasteFileService.counter_no_files + (archiveDeletePasteFileService.counter_no_files < 2 ? " file" : " files"));
									size_files.setText(getString(R.string.size) + ": " + size_progress);
								}

							}

							break;

							case "paste-cut":
							case "paste-copy":
							{
								from_textview.setText(archiveDeletePasteFileService.source_folder);
								to_textview.setText(archiveDeletePasteFileService.dest_folder);

								current_file.setText(archiveDeletePasteFileService.current_file_name);
								copied_textview.setText(archiveDeletePasteFileService.copied_file);
								no_files.setText(""+archiveDeletePasteFileService.counter_no_files);
								size_files.setText(size_progress);

							}
							break;


							case "copy_to":
							{
								to_textview.setText(archiveDeletePasteFileService.dest_folder);

								current_file.setText(archiveDeletePasteFileService.current_file_name);
								copied_textview.setText(archiveDeletePasteFileService.copied_file);
								no_files.setText(""+archiveDeletePasteFileService.counter_no_files);
								size_files.setText(size_progress);

							}
							break;
						}

					}
					handler.postDelayed(this, 250);

				}
			}
		});
	}

	private void bind_data()
	{
		if ("archive-zip".equals(intent_action) || "paste-cut".equals(intent_action) || "paste-copy".equals(intent_action) || "copy_to".equals(intent_action))
		{
			archiveDeletePasteFileService.fileCountSize.mutable_size_of_files_to_be_archived_copied.observe(this, new Observer<String>() {
				@Override
				public void onChanged(String s) {
					total_no_of_files.setText("/" + archiveDeletePasteFileService.fileCountSize.total_no_of_files);
					total_size_files.setText("/" + s);
				}
			});
		}


	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);
		outState.putString("intent_action",intent_action);
		outState.putSerializable("sourceFileObjectType",sourceFileObjectType);
		outState.putBoolean("clear_cache",clear_cache);
	}

	@Override
	protected void onPause()
	{
		// TODO: Implement this method
		super.onPause();
		if(serviceConnection!=null)unbindService(serviceConnection);
		PROGRESS_ACTIVITY_SHOWN=false;
		handler.removeCallbacksAndMessages(null);
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
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		clear_cache=savedInstanceState.getBoolean("clear_cache");
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(!isFinishing() && !isChangingConfigurations() && clear_cache)
		{
			clearCache();
		}
	}

	public void clearCache()
	{
		Global.CLEAR_CACHE();
	}

}
