package svl.kadatha.filex;
import android.view.*;
import android.os.*;
import android.widget.RadioGroup.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.*;
import android.content.*;

import androidx.fragment.app.DialogFragment;

public class FileEditorSettingsDialog extends DialogFragment
{

	private RadioButton unix_rb,mac_rb,wnd_rb;
	private ImageButton text_size_decrease_btn,text_size_increase_btn;
	private boolean not_wrap;
	private int selected_eol;
	private float selected_text_size=FileEditorActivity.FILE_EDITOR_TEXT_SIZE;
	private EditText sample_edittext;
	private TextView text_size_tv;
	private Context context;
	final static int MIN_TEXT_SIZE=10, MAX_TEXT_SIZE=20;
	private EOL_ChangeListener eol_changeListener;
	private boolean fromThirdPartyApp,fromArchiveView,isFileBig;
	FileEditorActivity fileEditorActivity;


	@Override
	public void onAttach(Context context)
	{
		// TODO: Implement this method
		super.onAttach(context);
		this.context=context;
		fileEditorActivity=((FileEditorActivity)context);
		eol_changeListener=(FileEditorActivity)context;

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		selected_eol=fileEditorActivity.eol;
		not_wrap=FileEditorActivity.NOT_WRAP;
		selected_text_size=FileEditorActivity.FILE_EDITOR_TEXT_SIZE;
		fromArchiveView=fileEditorActivity.fromArchiveView;
		fromThirdPartyApp=fileEditorActivity.fromThirdPartyApp;
		isFileBig=fileEditorActivity.isFileBig;
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_file_editor_settings,container,false);
		RadioGroup eol_rg = v.findViewById(R.id.eol_rg);
		eol_rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			public void onCheckedChanged(RadioGroup rg, int p1)
			{
				if(!fromThirdPartyApp && !fromArchiveView && !isFileBig)
				{
					if(unix_rb.isChecked())
					{
						selected_eol=FileEditorActivity.EOL_N;
					}
					else if(mac_rb.isChecked())
					{
						selected_eol=FileEditorActivity.EOL_R;
					}
					else if(wnd_rb.isChecked())
					{
						selected_eol=FileEditorActivity.EOL_RN;
					}
				}
				else
				{
					switch(fileEditorActivity.eol)
					{
						case FileEditorActivity.EOL_N:
							unix_rb.setChecked(true);
							break;
						case FileEditorActivity.EOL_R:
							mac_rb.setChecked(true);
							break;
						case FileEditorActivity.EOL_RN:
							wnd_rb.setChecked(true);
							break;

					}
					print(getString(R.string.cant_edit_this_file));
				}

				
			}
			
		});
		
		unix_rb=v.findViewById(R.id.eol_rb_n);
		mac_rb=v.findViewById(R.id.eol_rb_r);
		wnd_rb=v.findViewById(R.id.eol_rb_rn);


		CheckBox wrap_check_box = v.findViewById(R.id.file_editor_settings_wrap_checkbox);
		wrap_check_box.setChecked(!not_wrap);
		wrap_check_box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton b,boolean checked)
			{
				not_wrap=!checked;
			
			}
		});
		
		text_size_decrease_btn=v.findViewById(R.id.file_editor_text_size_decrease);
		text_size_increase_btn=v.findViewById(R.id.file_editor_text_size_increase);
		text_size_tv=v.findViewById(R.id.file_editor_settings_text_size);
		text_size_tv.setText(getString(R.string.text_size_colon)+" "+(int)selected_text_size);
		
		text_size_decrease_btn.setOnTouchListener(new RepeatListener(400,101,new View.OnClickListener()
		{
			public void onClick(View p1)
			{
				if(selected_text_size>MIN_TEXT_SIZE)
				{
					selected_text_size--;
					text_size_tv.setText(getString(R.string.text_size_colon)+" "+(int)selected_text_size);
					sample_edittext.setTextSize(selected_text_size);
					enable_disable_btns();
				}
				
				
			}
		}));
		
		text_size_increase_btn.setOnTouchListener(new RepeatListener(400,101,new View.OnClickListener()
		{
			public void onClick(View p1)
			{
				if(selected_text_size<MAX_TEXT_SIZE)
				{
					selected_text_size++;
					text_size_tv.setText(getString(R.string.text_size_colon)+" "+(int)selected_text_size);
					sample_edittext.setTextSize(selected_text_size);
					enable_disable_btns();
				}
			}
		}));
		
		
		sample_edittext=v.findViewById(R.id.file_editor_settings_sample_text);
		sample_edittext.setTextSize(selected_text_size);
		ViewGroup buttons_layout = v.findViewById(R.id.fragment_file_editor_settings_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
		Button ok_button = buttons_layout.findViewById(R.id.first_button);
		ok_button.setText(R.string.ok);
		ok_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				
				if(FileEditorActivity.NOT_WRAP!=not_wrap)
				{
					FileEditorActivity.NOT_WRAP=not_wrap;
					fileEditorActivity.tinyDB.putBoolean("file_editor_not_wrap",not_wrap);
					fileEditorActivity.recreate();
			
				}
				
				if(FileEditorActivity.FILE_EDITOR_TEXT_SIZE!=selected_text_size)
				{
				
					fileEditorActivity.filetext_container_edittext.setTextSize(selected_text_size);
					FileEditorActivity.FILE_EDITOR_TEXT_SIZE=selected_text_size;
					fileEditorActivity.tinyDB.putFloat("file_editor_text_size",FileEditorActivity.FILE_EDITOR_TEXT_SIZE);
		
				}
				
				((FileEditorActivity)context).altered_eol=selected_eol;
				if(eol_changeListener!=null)
				{
					eol_changeListener.onEOLchanged(selected_eol);
				}
				dismissAllowingStateLoss();
			}
		});

		Button cancel_button = buttons_layout.findViewById(R.id.second_button);
		cancel_button.setText(R.string.cancel);
		cancel_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				dismissAllowingStateLoss();
			}
		});
		
		switch(selected_eol)
		{
			case FileEditorActivity.EOL_N:
				unix_rb.setChecked(true);
				break;
			case FileEditorActivity.EOL_R:
				mac_rb.setChecked(true);
				break;
			case FileEditorActivity.EOL_RN:
				wnd_rb.setChecked(true);
				break;
			
		}
		
		enable_disable_btns();
		return v;
	}


	private void enable_disable_btns()
	{
	
		if(selected_text_size==MIN_TEXT_SIZE)
		{
			text_size_decrease_btn.setEnabled(false);
			text_size_decrease_btn.setAlpha(Global.DISABLE_ALFA);
		}
		else if(selected_text_size==MAX_TEXT_SIZE)
		{
			text_size_increase_btn.setEnabled(false);
			text_size_increase_btn.setAlpha(Global.DISABLE_ALFA);
		}
		else if(selected_text_size>MIN_TEXT_SIZE && selected_text_size<MAX_TEXT_SIZE)
		{
			text_size_decrease_btn.setEnabled(true);
			text_size_decrease_btn.setAlpha(Global.ENABLE_ALFA);

			text_size_increase_btn.setEnabled(true);
			text_size_increase_btn.setAlpha(Global.ENABLE_ALFA);
		}
	}

	
	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}

	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	interface EOL_ChangeListener
	{
		void onEOLchanged(int eol);
	}
	
	private void print(String msg)
	{
		android.widget.Toast.makeText(context,msg,android.widget.Toast.LENGTH_LONG).show();
	}
}
