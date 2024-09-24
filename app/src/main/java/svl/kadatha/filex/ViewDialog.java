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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TableRow.LayoutParams;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;


public class ViewDialog extends DialogFragment
{
    private TinyDB tinyDB;
    private ImageButton name_asc_btn,name_desc_btn,date_asc_btn,date_desc_btn,size_asc_btn,size_desc_btn;
    private RadioButton list_rb, grid_rb;
    private Context context;
	private AppCompatActivity appCompatActivity;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		appCompatActivity=(AppCompatActivity)context;
		tinyDB=new TinyDB(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setCancelable(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v= inflater.inflate(R.layout.fragment_view,container,false);
        RadioGroup rg = v.findViewById(R.id.dialog_view_layout_rg);
		list_rb=v.findViewById(R.id.dialog_view_rb_list);
		grid_rb=v.findViewById(R.id.dialog_view_rb_grid);
		if(appCompatActivity instanceof MainActivity)
		{
			DetailFragment df=(DetailFragment)getParentFragmentManager().findFragmentById(R.id.detail_fragment);
			if(df.grid_layout)
			{
				grid_rb.setChecked(true);
			}
			else
			{
				list_rb.setChecked(true);
			}
			rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					if(list_rb.isChecked())
					{
						if(df.file_click_selected_name.equals("Image") || df.file_click_selected_name.equals("Video"))
						{
							Global.IMAGE_VIDEO_GRID_LAYOUT=false;
						}
						else
						{
							Global.FILE_GRID_LAYOUT=false;
						}
					}
					else if(grid_rb.isChecked())
					{
						if(df.file_click_selected_name.equals("Image") || df.file_click_selected_name.equals("Video"))
						{
							Global.IMAGE_VIDEO_GRID_LAYOUT=true;
						}
						else
						{
							Global.FILE_GRID_LAYOUT=true;
						}
					}

					DetailFragment df=(DetailFragment)getParentFragmentManager().findFragmentById(R.id.detail_fragment);
					getParentFragmentManager().beginTransaction().detach(df).commit();
					getParentFragmentManager().beginTransaction().attach(df).commit();
					if(df.file_click_selected_name.equals("Image") || df.file_click_selected_name.equals("Video"))
					{
						tinyDB.putBoolean("image_video_grid_layout",Global.IMAGE_VIDEO_GRID_LAYOUT);
					}
					else
					{
						tinyDB.putBoolean("file_grid_layout",Global.FILE_GRID_LAYOUT);
					}
				}
			});

		}
		else if(appCompatActivity instanceof FileSelectorActivity)
		{
			if(FileSelectorActivity.FILE_GRID_LAYOUT)
			{
				grid_rb.setChecked(true);
			}
			else
			{
				list_rb.setChecked(true);
			}
			rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {

					if(list_rb.isChecked())
					{
						FileSelectorActivity.FILE_GRID_LAYOUT=false;
					}
					else if(grid_rb.isChecked())
					{
						FileSelectorActivity.FILE_GRID_LAYOUT=true;
					}
					FileSelectorFragment fileSelectorFragment=(FileSelectorFragment) getParentFragmentManager().findFragmentById(R.id.file_selector_container);
					getParentFragmentManager().beginTransaction().detach(fileSelectorFragment).commit();
					getParentFragmentManager().beginTransaction().attach(fileSelectorFragment).commit();
					tinyDB.putBoolean("file_selector_file_grid_layout",FileSelectorActivity.FILE_GRID_LAYOUT);
				}
			});

		}


        SeekBar seekbar_fontsize = v.findViewById(R.id.seekbar_fontsize);
		name_asc_btn=v.findViewById(R.id.name_asc);
		name_desc_btn=v.findViewById(R.id.name_desc);
		date_asc_btn=v.findViewById(R.id.date_asc);
		date_desc_btn=v.findViewById(R.id.date_desc);
		size_asc_btn=v.findViewById(R.id.size_asc);
		size_desc_btn=v.findViewById(R.id.size_desc);

        SortButtonClickListener sortButtonClickListener = new SortButtonClickListener();
		name_asc_btn.setOnClickListener(sortButtonClickListener);
		name_desc_btn.setOnClickListener(sortButtonClickListener);
		date_asc_btn.setOnClickListener(sortButtonClickListener);
		date_desc_btn.setOnClickListener(sortButtonClickListener);
		size_asc_btn.setOnClickListener(sortButtonClickListener);
		size_desc_btn.setOnClickListener(sortButtonClickListener);


		SwitchCompat show_hidden_switch = v.findViewById(R.id.view_switch_show_hidden);
		if(appCompatActivity instanceof MainActivity)
		{
			show_hidden_switch.setChecked(MainActivity.SHOW_HIDDEN_FILE);
			show_hidden_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					new Handler().postDelayed(new Runnable()
					{
						public void run()
						{
							MainActivity.SHOW_HIDDEN_FILE=isChecked;
							tinyDB.putBoolean("show_hidden_file",MainActivity.SHOW_HIDDEN_FILE);
							DetailFragment df=(DetailFragment)getParentFragmentManager().findFragmentById(R.id.detail_fragment);
							((MainActivity)appCompatActivity).actionmode_finish(df,df.fileclickselected);
							if(df.fileObjectType==FileObjectType.FILE_TYPE || df.fileObjectType==FileObjectType.ROOT_TYPE)
							{
								getParentFragmentManager().beginTransaction().detach(df).commit();
								getParentFragmentManager().beginTransaction().attach(df).commit();
							}

						}
					},250);

				}
			});
		}
		else if(appCompatActivity instanceof FileSelectorActivity)
		{
			show_hidden_switch.setChecked(FileSelectorActivity.SHOW_HIDDEN_FILE);
			show_hidden_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					new Handler().postDelayed(new Runnable()
					{
						public void run()
						{
							FileSelectorActivity.SHOW_HIDDEN_FILE=isChecked;
							tinyDB.putBoolean("file_selector_show_hidden_file",FileSelectorActivity.SHOW_HIDDEN_FILE);
							FileSelectorFragment fileSelectorFragment=(FileSelectorFragment) getParentFragmentManager().findFragmentById(R.id.file_selector_container);
							fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
							if(fileSelectorFragment.fileObjectType==FileObjectType.FILE_TYPE || fileSelectorFragment.fileObjectType==FileObjectType.ROOT_TYPE)
							{
								getParentFragmentManager().beginTransaction().detach(fileSelectorFragment).commit();
								getParentFragmentManager().beginTransaction().attach(fileSelectorFragment).commit();
							}

						}
					},250);

				}
			});
		}

        ViewGroup buttons_layout = v.findViewById(R.id.fragment_view_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button close_button = buttons_layout.findViewById(R.id.first_button);
		close_button.setText(R.string.close);
		close_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				dismissAllowingStateLoss();
			}
		});
		
		if(appCompatActivity instanceof MainActivity)
		{
			seekbar_fontsize.setProgress(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR);
			seekbar_fontsize.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
			{

				int progress=0;
				public void onStartTrackingTouch(SeekBar p1)
				{

				}
				public void onStopTrackingTouch(SeekBar p1)
				{
					tinyDB.putInt("recycler_view_font_size_factor",Global.RECYCLER_VIEW_FONT_SIZE_FACTOR);
					DetailFragment df=(DetailFragment)getParentFragmentManager().findFragmentById(R.id.detail_fragment);
					getParentFragmentManager().beginTransaction().detach(df).commit();
					getParentFragmentManager().beginTransaction().attach(df).commit();

				}
				public void onProgressChanged(SeekBar p1, int progress_value,boolean fromUser)
				{
					progress=progress_value;
					Global.RECYCLER_VIEW_FONT_SIZE_FACTOR=progress_value;
					switch(progress_value)
					{
						case 0:
							Global.GRID_COUNT=Global.GRID_COUNT_SMALL;
							break;
						case 2:
							Global.GRID_COUNT=Global.GRID_COUNT_LARGE;
							break;
						default:
							Global.GRID_COUNT=Global.GRID_COUNT_MEDIUM;
							break;
					}
				}

			});
		}
		else if(appCompatActivity instanceof FileSelectorActivity)
		{
			seekbar_fontsize.setProgress(FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR);
			seekbar_fontsize.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
			{

				int progress=0;
				public void onStartTrackingTouch(SeekBar p1)
				{

				}
				public void onStopTrackingTouch(SeekBar p1)
				{
					tinyDB.putInt("file_selector_recycler_view_font_size_factor",FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR);
					FileSelectorFragment fileSelectorFragment=(FileSelectorFragment) getParentFragmentManager().findFragmentById(R.id.file_selector_container);
					getParentFragmentManager().beginTransaction().detach(fileSelectorFragment).commit();
					getParentFragmentManager().beginTransaction().attach(fileSelectorFragment).commit();

				}
				public void onProgressChanged(SeekBar p1, int progress_value,boolean fromUser)
				{
					progress=progress_value;
					FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR=progress_value;
					switch(progress_value)
					{
						case 0:
							FileSelectorActivity.GRID_COUNT=Global.GRID_COUNT_SMALL;
							break;
						case 2:
							FileSelectorActivity.GRID_COUNT=Global.GRID_COUNT_LARGE;
							break;
						default:
							FileSelectorActivity.GRID_COUNT=Global.GRID_COUNT_MEDIUM;
							break;
					}
				}
			});
		}

		set_selection();
		
		return v;
	}
	
	private void set_selection()
	{
		String sort = null;
		if(appCompatActivity instanceof MainActivity)
		{
			sort=Global.SORT;
		}
		else if(appCompatActivity instanceof FileSelectorActivity)
		{
			sort=FileSelectorActivity.SORT;
		}
		switch(sort)
		{

			case "d_name_desc":
			case "f_name_desc":
				name_desc_btn.setSelected(true);
				
				name_asc_btn.setSelected(false);
				date_asc_btn.setSelected(false);
				date_desc_btn.setSelected(false);
				size_asc_btn.setSelected(false);
				size_desc_btn.setSelected(false);
				break;

			case "d_date_asc":
			case "f_date_asc":
				date_asc_btn.setSelected(true);
				
				name_asc_btn.setSelected(false);
				name_desc_btn.setSelected(false);
				date_desc_btn.setSelected(false);
				size_asc_btn.setSelected(false);
				size_desc_btn.setSelected(false);
				break;

			case "d_date_desc":
			case "f_date_desc":
				date_desc_btn.setSelected(true);
				
				name_asc_btn.setSelected(false);
				name_desc_btn.setSelected(false);
				date_asc_btn.setSelected(false);
				size_asc_btn.setSelected(false);
				size_desc_btn.setSelected(false);
				break;

			case "d_size_asc":
			case "f_size_asc":
				size_asc_btn.setSelected(true);

				name_asc_btn.setSelected(false);
				name_desc_btn.setSelected(false);
				date_asc_btn.setSelected(false);
				date_desc_btn.setSelected(false);
				size_desc_btn.setSelected(false);
				break;


			case "d_size_desc":
			case "f_size_desc":
				size_desc_btn.setSelected(true);

				name_asc_btn.setSelected(false);
				name_desc_btn.setSelected(false);
				date_asc_btn.setSelected(false);
				date_desc_btn.setSelected(false);
				size_asc_btn.setSelected(false);
				break;

			default:
				name_asc_btn.setSelected(true);
				
				name_desc_btn.setSelected(false);
				date_asc_btn.setSelected(false);
				date_desc_btn.setSelected(false);
				size_asc_btn.setSelected(false);
				size_desc_btn.setSelected(false);
				break;
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
	

	private class SortButtonClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View button)
		{
			// TODO: Implement this method
			String selected_sort;
			int id = button.getId();
			if (id == R.id.name_desc) {
				selected_sort = "d_name_desc";
			} else if (id == R.id.date_asc) {
				selected_sort = "d_date_asc";
			} else if (id == R.id.date_desc) {
				selected_sort = "d_date_desc";
			} else if(id==R.id.size_desc) {
				selected_sort="d_size_desc";
			} else if(id==R.id.size_asc) {
				selected_sort="d_size_asc";
			} else {
				selected_sort = "d_name_asc";
			}
			
			if(appCompatActivity instanceof MainActivity)
			{
				if(!selected_sort.equals(Global.SORT))
				{

					DetailFragment df=(DetailFragment)getParentFragmentManager().findFragmentById(R.id.detail_fragment);
					if(df!=null && df.progress_bar.getVisibility()==View.GONE)
					{
						if(df.fileclickselected.equals("Duplicate Files") && (id!=R.id.name_desc && id!=R.id.name_asc))
						{
							Global.print(context,getString(R.string.cannot_sort_here));
							return;
						}
						Global.SORT=selected_sort;
						set_selection();
						getParentFragmentManager().beginTransaction().detach(df).commit();
						getParentFragmentManager().beginTransaction().attach(df).commit();
						tinyDB.putString("sort",Global.SORT);

						df.viewModel.library_size_desc=false;
						df.viewModel.library_time_desc=false;
						df.size_image_view.setSelected(false);
						df.time_image_view.setSelected(false);
					}
					else
					{
						Global.print(context,getString(R.string.wait_ellipse));
					}

				}
			}
			else if(appCompatActivity instanceof FileSelectorActivity)
			{
				if(!selected_sort.equals(FileSelectorActivity.SORT))
				{

					FileSelectorFragment fileSelectorFragment=(FileSelectorFragment) getParentFragmentManager().findFragmentById(R.id.file_selector_container);
					if(fileSelectorFragment!=null && fileSelectorFragment.progress_bar.getVisibility()==View.GONE)
					{
						FileSelectorActivity.SORT=selected_sort;
						set_selection();
						getParentFragmentManager().beginTransaction().detach(fileSelectorFragment).commit();
						getParentFragmentManager().beginTransaction().attach(fileSelectorFragment).commit();
						tinyDB.putString("file_selector_sort",FileSelectorActivity.SORT);
					}
					else
					{
						Global.print(context,getString(R.string.wait_ellipse));
					}

				}
			}

		}

	}

}

