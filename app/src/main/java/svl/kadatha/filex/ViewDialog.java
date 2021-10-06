package svl.kadatha.filex;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.widget.SeekBar.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.TableRow.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;


public class ViewDialog extends DialogFragment
{

    private TinyDB tinyDB;
    private ImageButton name_asc_btn,name_desc_btn,date_asc_btn,date_desc_btn,size_asc_btn,size_desc_btn;
    private RadioButton list_rb, grid_rb;
    private Context context;
    private FragmentManager fragmentManager;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		//return super.onCreateView(inflater, container, savedInstanceState);
        context = getContext();
        fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();
		tinyDB=new TinyDB(context);
		View v= inflater.inflate(R.layout.fragment_view,container,false);

        RadioGroup rg = v.findViewById(R.id.dialog_view_layout_rg);

		list_rb=v.findViewById(R.id.dialog_view_rb_list);
		grid_rb=v.findViewById(R.id.dialog_view_rb_grid);
		if(Global.FILE_GRID_LAYOUT)
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
					Global.FILE_GRID_LAYOUT=false;
				}
				else if(grid_rb.isChecked())
				{
					Global.FILE_GRID_LAYOUT=true;
				}

				DetailFragment df=(DetailFragment)fragmentManager.findFragmentById(R.id.detail_fragment);
				fragmentManager.beginTransaction().detach(df).commit();
				fragmentManager.beginTransaction().attach(df).commit();
				tinyDB.putBoolean("file_grid_layout",Global.FILE_GRID_LAYOUT);
			}
		});

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
					DetailFragment df=(DetailFragment)fragmentManager.findFragmentById(R.id.detail_fragment);
					fragmentManager.beginTransaction().detach(df).commit();
					fragmentManager.beginTransaction().attach(df).commit();
				

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
		
		
		set_selection();
		
		return v;
	}
	
	private void set_selection()
	{
		switch(Global.SORT)
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
	


	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
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
			
			if(!selected_sort.equals(Global.SORT))
			{

				DetailFragment df=(DetailFragment)fragmentManager.findFragmentById(R.id.detail_fragment);
				if(df!=null && df.filled_filePOJOs)
				{
					Global.SORT=selected_sort;
					set_selection();
					fragmentManager.beginTransaction().detach(df).commit();
					fragmentManager.beginTransaction().attach(df).commit();
					tinyDB.putString("sort",Global.SORT);
				}
				else
				{
					print(getString(R.string.wait_ellipse));
				}

			}
		}

		
		
		
	}

	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
}

