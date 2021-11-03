package svl.kadatha.filex;

import android.os.*;
import android.content.*;
import android.widget.*;
import android.view.*;
import android.widget.AbsListView.*;

import java.io.*;

public class ArchiveDeletePasteProgressActivity2 extends BaseActivity
{

	private final Handler h=new Handler();

    private TextView from_textview;
    private TextView to_textview;
    private TextView copied_textview;
    private TextView no_files,size_files;
	private EditText current_file;
    private ServiceConnection serviceConnection;
	private ArchiveDeletePasteFileService2 archiveDeletePasteFileService;
	static boolean PROGRESS_ACTIVITY_SHOWN;
	private String intent_action;
	private FileObjectType sourceFileObjectType;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method

		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_cut_copy_delete_archive_progress);
		setFinishOnTouchOutside(false);
        TextView dialog_title = findViewById(R.id.dialog_fragment_cut_copy_title);
        TableRow to_table_row = findViewById(R.id.fragment_cut_copy_delete_archive_totablerow);
        TextView from_label = findViewById(R.id.dialog_fragment_cut_copy_delete_from_label);
        TextView to_label = findViewById(R.id.dialog_fragment_cut_copy_delete_to_label);
		from_textview=findViewById(R.id.dialog_fragment_cut_copy_from);
		to_textview=findViewById(R.id.dialog_fragment_cut_copy_to);
		current_file=findViewById(R.id.dialog_fragment_cut_copy_archive_current_file);
		current_file.setKeyListener(null);
		copied_textview=findViewById(R.id.dialog_fragment_copied_file);
		copied_textview.setKeyListener(null);
		no_files=findViewById(R.id.fragment_cut_copy_delete_archive_no_files);
		size_files=findViewById(R.id.fragment_cut_copy_delete_archive_size_files);
        ViewGroup buttons_layout = findViewById(R.id.fragment_cut_copy_delete_progress_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(this,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
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
					ProgressBarFragment progressBarFragment=ProgressBarFragment.getInstance();
					progressBarFragment.show(getSupportFragmentManager(),"");
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							if(archiveDeletePasteFileService!=null)
							{
								archiveDeletePasteFileService.cancelService();
							}
							print(getString(R.string.process_cancelled));
							PROGRESS_ACTIVITY_SHOWN=false;
							progressBarFragment.dismissAllowingStateLoss();
							finish();
						}
					},750);

				}
			});

		serviceConnection=new ServiceConnection()
		{
			public void onServiceConnected(ComponentName componentName,IBinder binder)
			{
				archiveDeletePasteFileService=((ArchiveDeletePasteFileService2.ArchiveDeletePasteBinder)binder).getService();
				archiveDeletePasteFileService.setServiceCompletionListener(new ArchiveDeletePasteFileService2.ServiceCompletionListener()
					{
						public void onServiceCompletion(String service_action,boolean result ,String target_file, String dest_folder)
						{
							switch(service_action)
							{
								case "archive-zip":
									print(result ? getString(R.string.created)+" '"+target_file+"' "+getString(R.string.at)+" "+dest_folder : getString(R.string.could_not_create)+" '"+target_file+"'");
									break;

								case "archive-unzip":
									print(result ? getString(R.string.unzipped)+" '"+target_file+"' "+getString(R.string.at)+" "+dest_folder : getString(R.string.could_not_extract)+" '"+target_file+"'");
									break;

								case "delete":
									print(result ? getString(R.string.deleted_selected_files)+" "+dest_folder : getString(R.string.could_not_delete_selected_files)+" "+dest_folder);
									break;

								case "paste-cut":
									print(result ? getString(R.string.moved_selected_files)+" "+dest_folder : getString(R.string.could_not_move_selected_files)+" "+dest_folder);
									break;

								case "paste-copy":
									print(result ? getString(R.string.copied_selected_files)+" "+dest_folder : getString(R.string.could_not_copy_selected_files)+" "+dest_folder);
									break;
							}

							finish();
						}

					});
			}

			public void onServiceDisconnected(ComponentName componentName)
			{
				archiveDeletePasteFileService=null;
			}

		};

		if(savedInstanceState==null)
		{
			Intent intent=getIntent();
			if(intent!=null)
			{
				intent_action=intent.getAction();
				Bundle bundle=intent.getBundleExtra("bundle");
				if(bundle!=null)
				{
					sourceFileObjectType= (FileObjectType) bundle.getSerializable("sourceFileObjectType");
					Intent adp_intent=new Intent(this,ArchiveDeletePasteFileService2.class);
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
		}
		else {
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
				dialog_title.setText(R.string.copying);
				break;
		}

	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		// TODO: Implement this method
		super.onNewIntent(intent);
		String action=intent.getStringExtra("REPLACE_ACTION");
		if(action!=null  && action.equals(ArchiveDeletePasteFileService2.REPLACE_ACTION))
		{

			String duplicate_file_name=intent.getStringExtra("duplicate_file_name");
			FileReplaceConfirmationDialog replaceFileConfirmationDialog=new FileReplaceConfirmationDialog();
			replaceFileConfirmationDialog.setReplaceListener(new FileReplaceConfirmationDialog.FileReplaceListener()
				{
			 		public void onReplaceClick(boolean r, boolean a_all)
			 		{
						if(archiveDeletePasteFileService!=null)
						{
							archiveDeletePasteFileService.onReplaceSelection(r,a_all);
						}

					}

				});
			Bundle b=new Bundle();
			b.putString("duplicate_file_name",duplicate_file_name);

			replaceFileConfirmationDialog.setArguments(b);

			replaceFileConfirmationDialog.show(getSupportFragmentManager(),null);

		}
	}


	@Override
	protected void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		getWindow().setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);

		PROGRESS_ACTIVITY_SHOWN=true;
		Intent service_intent=new Intent(this,ArchiveDeletePasteFileService2.class);
		bindService(service_intent,serviceConnection,Context.BIND_AUTO_CREATE);


		h.post(new Runnable()
			{
				public void run()
				{

					switch(intent_action)
					{
						case "archive-zip":
							if(archiveDeletePasteFileService!=null && archiveDeletePasteFileService.archive_action!=null)
							{
								current_file.setText(archiveDeletePasteFileService.zip_folder_name);
								copied_textview.setText(archiveDeletePasteFileService.copied_file_name);

								from_textview.setText(archiveDeletePasteFileService.zip_file_path);
								to_textview.setText(archiveDeletePasteFileService.dest_folder + File.separator + archiveDeletePasteFileService.zip_folder_name + ".zip");

								no_files.setText(archiveDeletePasteFileService.counter_no_files + "/" + archiveDeletePasteFileService.total_no_of_files);
								size_files.setText(archiveDeletePasteFileService.size_of_files_archived + "/" + archiveDeletePasteFileService.size_of_files_to_be_archived_copied);
							}
							break;

						case "archive-unzip":

							if(archiveDeletePasteFileService!=null && archiveDeletePasteFileService.archive_action!=null)
							{
								from_textview.setText(archiveDeletePasteFileService.zip_file_path);
								to_textview.setText(archiveDeletePasteFileService.zip_folder_name!=null ? archiveDeletePasteFileService.dest_folder+File.separator+archiveDeletePasteFileService.zip_folder_name : archiveDeletePasteFileService.dest_folder);
								current_file.setText(archiveDeletePasteFileService.current_file_name);
								copied_textview.setText(archiveDeletePasteFileService.copied_file_name);
								no_files.setText(getString(R.string.extracted) + ": " + archiveDeletePasteFileService.counter_no_files + (archiveDeletePasteFileService.counter_no_files < 2 ? " file" : " files"));
								size_files.setText(getString(R.string.size) + ": " + archiveDeletePasteFileService.size_of_files_archived);
							}
							break;

						case "delete":
							if(archiveDeletePasteFileService!=null && archiveDeletePasteFileService.current_file_name!=null)
							{
								from_textview.setText(archiveDeletePasteFileService.source_folder);
								current_file.setText(archiveDeletePasteFileService.current_file_name);
								copied_textview.setText(archiveDeletePasteFileService.deleted_file_name);
								if(archiveDeletePasteFileService.isFromInternal)
								{
									no_files.setText(getString(R.string.deleted) + ": " + archiveDeletePasteFileService.counter_no_files + (archiveDeletePasteFileService.counter_no_files < 2 ? " file" : " files"));
									size_files.setText(getString(R.string.size) + ": " + archiveDeletePasteFileService.size_of_files_format);
								}

							}

							break;

						case "paste-cut":
						case "paste-copy":

							if(archiveDeletePasteFileService!=null && archiveDeletePasteFileService.dest_folder!=null)
							{
								from_textview.setText(archiveDeletePasteFileService.source_folder);
								to_textview.setText(archiveDeletePasteFileService.dest_folder);

								current_file.setText(archiveDeletePasteFileService.current_file_name);
								copied_textview.setText(archiveDeletePasteFileService.copied_file);
								no_files.setText(archiveDeletePasteFileService.counter_no_files + "/" + archiveDeletePasteFileService.total_no_of_files);
								size_files.setText(archiveDeletePasteFileService.size_of_files_copied + "/" + archiveDeletePasteFileService.size_of_files_to_be_archived_copied);

							}
							break;

					}


					if(ArchiveDeletePasteFileService2.SERVICE_COMPLETED)
					{
						h.removeCallbacks(this);
						finish();
					}
					h.postDelayed(this,500);
				}
			});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);
		outState.putString("intent_action",intent_action);
		outState.putSerializable("sourceFileObjectType",sourceFileObjectType);
	}

	@Override
	protected void onPause()
	{
		// TODO: Implement this method
		super.onPause();
		unbindService(serviceConnection);
		PROGRESS_ACTIVITY_SHOWN=false;
	}

	private void print(String msg)
	{
		Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
	}

}
