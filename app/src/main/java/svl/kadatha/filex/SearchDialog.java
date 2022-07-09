package svl.kadatha.filex;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchDialog extends DialogFragment
{
	private EditText search_file_name_edit_text, lower_bound_edit_text,upper_bound_edit_text;
	private CheckBox wholeword_checkbox,casesensitive_checkbox,regex_checkbox;
    private final SparseBooleanArray dir_selected_booleanarray=new SparseBooleanArray();
	private final Set<FilePOJO> selected_search_dir_list= new HashSet<>();
    private Context context;
	private final List<FilePOJO> storage_list=new ArrayList<>();
	private InputMethodManager imm;
	private long search_lower_limit_size=0;
	private long search_upper_limit_size=0;
	private String search_file_name;
	Set<FilePOJO>search_in_dir;
	String search_file_type;
	boolean search_whole_word,search_case_sensitive,search_regex;
	private long size_multiplying_factor;
	public static final String SEARCH_FILE_NAME="search_file_name";
	public static final String SEARCH_IN_DIR="search_in_dir";
	public static final String SEARCH_FILE_TYPE="search_file_type";
	public static final String SEARCH_WHOLE_WORD="search_whole_word";
	public static final String SEARCH_CASE_SENSITIVE="serach_case_sensitive";
	public static final String SEARCH_REGEX="search_regex";
	public static final String SEARCH_LOWER_LIMIT_SIZE="search_lower_limit_size";
	public static final String SEARCH_UPPER_LIMIT_SIZE="search_upper_limit_size";

	private Group size_group;
	private String file_size_unit;
	private MainActivity mainActivity;


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		mainActivity=(MainActivity) this.context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
		//setRetainInstance(true);
		for(FilePOJO filepojo:Global.STORAGE_DIR)
		{
			if(filepojo.getFileObjectType()==FileObjectType.FILE_TYPE && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(filepojo.getPath()))))
			{
				storage_list.add(filepojo);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_search_parameters,container,false);
		search_file_name_edit_text=v.findViewById(R.id.dialog_fragment_search_file_edittext);
		wholeword_checkbox=v.findViewById(R.id.dialog_fragment_search_wholeword_checkbox);
		casesensitive_checkbox=v.findViewById(R.id.dialog_fragment_search_casesensitive_checkbox);
		regex_checkbox=v.findViewById(R.id.dialog_fragment_search_regex_checkbox);


		size_group=v.findViewById(R.id.dialog_fragment_search_size_label_group);
		RadioGroup rg = v.findViewById(R.id.dialog_fragment_search_rg);
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			public void onCheckedChanged(RadioGroup p1, int p2)
			{
				if (p2 == R.id.dialog_search_rb_filetype) {
					search_file_type = "f";
					size_group.setVisibility(View.VISIBLE);
				} else if (p2 == R.id.dialog_search_rb_foldertype) {
					search_file_type = "d";
					setSizeGroupVisibilityGone();
				} else if (p2 == R.id.dialog_search_rb_filefoldertype) {
					search_file_type = "fd";
					setSizeGroupVisibilityGone();
				}
			}
		});

        RadioButton file_rb = v.findViewById(R.id.dialog_search_rb_filetype);
        RadioButton dir_rb = v.findViewById(R.id.dialog_search_rb_foldertype);
        RadioButton file_dir_rb = v.findViewById(R.id.dialog_search_rb_filefoldertype);

		if(search_file_type!=null)
		{
			switch(search_file_type)
			{
				case "d":
					dir_rb.setChecked(true);
					setSizeGroupVisibilityGone();
					break;
				case "fd":
					file_dir_rb.setChecked(true);
					setSizeGroupVisibilityGone();
					break;
				default:
					file_rb.setChecked(true);
					size_group.setVisibility(View.VISIBLE);
					break;

			}
		}
		else
		{
			file_rb.setChecked(true);
			size_group.setVisibility(View.VISIBLE);
		}

		RadioGroup file_size_radio_group = v.findViewById(R.id.dialog_fragment_search_size_rg);
		file_size_radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				if(i==R.id.dialog_search_rb_size_kb)
				{
					size_multiplying_factor=1024;
					file_size_unit="kb";
				}
				else if(i==R.id.dialog_search_rb_size_mb)
				{
					size_multiplying_factor=1024*1024;
					file_size_unit="mb";
				}
			}
		});

		RadioButton kb_radio_button=v.findViewById(R.id.dialog_search_rb_size_kb);
		RadioButton mb_radio_button=v.findViewById(R.id.dialog_search_rb_size_mb);

		if(file_size_unit!=null)
		{
			switch (file_size_unit)
			{
				case "mb":
					mb_radio_button.setChecked(true);
					size_multiplying_factor=1024*1024;
					break;
				default:
					kb_radio_button.setChecked(true);
					size_multiplying_factor=1024;
					break;
			}
		}
		else
		{
			kb_radio_button.setChecked(true);
			size_multiplying_factor=1024;
		}



		lower_bound_edit_text=v.findViewById(R.id.dialog_fragment_search_lower_bound);
		upper_bound_edit_text=v.findViewById(R.id.dialog_fragment_search_upper_bound);

        RecyclerView search_recyclerview = v.findViewById(R.id.dialog_fragment_search_storage_dir_recyclerview);
		search_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
        SearchRecyclerViewAdapter searchRecyclerViewAdapter = new SearchRecyclerViewAdapter(storage_list);
		search_recyclerview.setAdapter(searchRecyclerViewAdapter);
		search_recyclerview.setLayoutManager(new LinearLayoutManager(context));
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_search_parameters_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
		ok_button.setText(R.string.ok);
		ok_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(lower_bound_edit_text.getText().toString().trim().equals("") && upper_bound_edit_text.getText().toString().trim().equals("") && search_file_name_edit_text.getText().toString().trim().equals(""))
				{
					Global.print(context,getString(R.string.enter_name));
					return;
				}
				else
				{
					search_file_name=search_file_name_edit_text.getText().toString().trim();
					try {
						search_lower_limit_size=Long.parseLong(lower_bound_edit_text.getText().toString())*size_multiplying_factor;
					}
					catch (NumberFormatException e)
					{
						search_lower_limit_size=0L;
					}

					try {
						search_upper_limit_size=Long.parseLong(upper_bound_edit_text.getText().toString())*size_multiplying_factor;
					}
					catch (NumberFormatException e)
					{
						search_upper_limit_size=0L;
					}

				}
				if(selected_search_dir_list.size()==0)
				{
					Global.print(context,getString(R.string.select_directories_to_search_in));
					return;
				}
				else
				{
					search_in_dir=new HashSet<>();
					search_in_dir.addAll(selected_search_dir_list);
				}
				search_whole_word=wholeword_checkbox.isChecked();
				search_case_sensitive=casesensitive_checkbox.isChecked();
				search_regex=regex_checkbox.isChecked();



				mainActivity.search_file_name=search_file_name;
				mainActivity.search_in_dir=search_in_dir;
				mainActivity.search_file_type=search_file_type;
				mainActivity.search_whole_word=search_whole_word;
				mainActivity.search_case_sensitive=search_case_sensitive;
				mainActivity.search_regex=search_regex;
				mainActivity.search_lower_limit_size=search_lower_limit_size;
				mainActivity.search_upper_limit_size=search_upper_limit_size;


				FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(DetailFragment.SEARCH_RESULT),FileObjectType.SEARCH_LIBRARY_TYPE);
				((MainActivity)context).createFragmentTransaction(DetailFragment.SEARCH_RESULT,FileObjectType.SEARCH_LIBRARY_TYPE);
				imm.hideSoftInputFromWindow(search_file_name_edit_text.getWindowToken(),0);
				dismissAllowingStateLoss();
			}
		});

        Button cancel_button = buttons_layout.findViewById(R.id.second_button);
		cancel_button.setText(R.string.cancel);
		cancel_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				imm.hideSoftInputFromWindow(search_file_name_edit_text.getWindowToken(),0);
				dismissAllowingStateLoss();
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
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}

	/*
	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}

		super.onDestroyView();
	}

 */

	private void setSizeGroupVisibilityGone()
	{
		size_group.setVisibility(View.GONE);
		lower_bound_edit_text.setText("");
		upper_bound_edit_text.setText("");
	}

	/*
	interface SearchDialogListener
	{
		void onCloseSearchDialog(String search_file_name, Set<FilePOJO> search_in_dir, String search_file_type, boolean search_whole_word, boolean search_case_sensitive, boolean search_regex, long lower_size_limit, long upper_size_limit);
	}

	 */


	public class SearchRecyclerViewAdapter extends RecyclerView.Adapter<SearchRecyclerViewAdapter.VH>
	{

		final List<FilePOJO> storage_list;
		
		SearchRecyclerViewAdapter(List<FilePOJO> storage_list)
		{
			this.storage_list=storage_list;
		}
		

		class VH extends RecyclerView.ViewHolder
		{
			final View v;
			final ImageView iv;
			final TextView tv;
			final CheckBox cb;
			int pos;
			VH(View vi)
			{
				super(vi);
				this.v=vi;
				iv=v.findViewById(R.id.image_search_storage_dir);
				tv=v.findViewById(R.id.text_search_storage_dir_name);
				cb=v.findViewById(R.id.checkbox_search_storage);

				cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
				{
					public void onCheckedChanged(CompoundButton p1, boolean p2)
					{
						pos=getBindingAdapterPosition();
						if(p2)
						{
							dir_selected_booleanarray.put(pos,p2);
							selected_search_dir_list.add(storage_list.get(pos));
						}
						else
						{
							dir_selected_booleanarray.delete(pos);
							selected_search_dir_list.remove(storage_list.get(pos));
						}
					}

				});

			}

		}
		@Override
		public SearchDialog.SearchRecyclerViewAdapter.VH onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			View itemview=LayoutInflater.from(p1.getContext()).inflate(R.layout.search_storage_dir_recyclerview_layout,p1,false);
			return new VH(itemview);
		
		}

		@Override
		public void onBindViewHolder(SearchDialog.SearchRecyclerViewAdapter.VH p1, int p2)
		{
			// TODO: Implement this method
			if(Global.GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR().getPath().equals(storage_list.get(p2).getPath()))
			{
				p1.iv.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.device_icon));
				p1.tv.setText(storage_list.get(p2).getName());
				if(dir_selected_booleanarray.size()==0)
				{
					p1.cb.setChecked(true);
				}
			}
			else
			{
				p1.iv.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
				p1.tv.setText(storage_list.get(p2).getName());

			}
		
			p1.cb.setChecked(dir_selected_booleanarray.get(p2,false));
			
		}
		
		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return storage_list.size();
		}

	}
	
}
