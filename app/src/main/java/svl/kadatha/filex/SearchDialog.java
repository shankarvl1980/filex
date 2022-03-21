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
import android.widget.Toast;

import androidx.annotation.NonNull;
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
	private EditText search_file_name;
	private CheckBox wholeword_checkbox,casesensitive_checkbox,regex_checkbox;
    private final SparseBooleanArray dir_selected_booleanarray=new SparseBooleanArray();
	private final Set<FilePOJO> selected_search_dir_list= new HashSet<>();
    private Context context;
	private final List<FilePOJO> storage_list=new ArrayList<>();
	private InputMethodManager imm;


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
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
		search_file_name=v.findViewById(R.id.dialog_fragment_search_file_edittext);
		wholeword_checkbox=v.findViewById(R.id.dialog_fragment_search_wholeword_checkbox);
		casesensitive_checkbox=v.findViewById(R.id.dialog_fragment_search_casesensitive_checkbox);
		regex_checkbox=v.findViewById(R.id.dialog_fragment_search_regex_checkbox);
        RadioGroup rg = v.findViewById(R.id.dialog_fragment_search_rg);
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			public void onCheckedChanged(RadioGroup p1, int p2)
			{
				if (p2 == R.id.dialog_search_rb_filetype) {
					DetailFragment.SEARCH_FILE_TYPE = "f";
				} else if (p2 == R.id.dialog_search_rb_foldertype) {
					DetailFragment.SEARCH_FILE_TYPE = "d";
				} else if (p2 == R.id.dialog_search_rb_filefoldertype) {
					DetailFragment.SEARCH_FILE_TYPE = "fd";
				}
			}
		});
        RadioButton file_rb = v.findViewById(R.id.dialog_search_rb_filetype);
        RadioButton dir_rb = v.findViewById(R.id.dialog_search_rb_foldertype);
        RadioButton file_dir_rb = v.findViewById(R.id.dialog_search_rb_filefoldertype);
		if(DetailFragment.SEARCH_FILE_TYPE!=null)
		{
			switch(DetailFragment.SEARCH_FILE_TYPE)
			{
				case "d":
					dir_rb.setChecked(true);
					break;
				case "fd":
					file_dir_rb.setChecked(true);
					break;
				default:
					file_rb.setChecked(true);
					break;

			}
		}
		else
		{
			file_rb.setChecked(true);
		}

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

				//String f_n,f_t;
				if(search_file_name.getText().toString().trim().equals(""))
				{
					print(getString(R.string.enter_name));
					return;
				}
				else
				{
					DetailFragment.SEARCH_FILE_NAME=search_file_name.getText().toString().trim();
				}
				if(selected_search_dir_list.size()==0)
				{
					print(getString(R.string.select_directories_to_search_in));
					return;
				}
				else
				{
					DetailFragment.SEARCH_IN_DIR=new HashSet<>();
					DetailFragment.SEARCH_IN_DIR.addAll(selected_search_dir_list);
					
				}
				DetailFragment.SEARCH_WHOLE_WORD=wholeword_checkbox.isChecked();
				DetailFragment.SEARCH_CASE_SENSITIVE=casesensitive_checkbox.isChecked();
				DetailFragment.SEARCH_REGEX=regex_checkbox.isChecked();
				/*
				if(rg.getCheckedRadioButtonId()==R.id.dialog_search_rb_filetype)
				{
					DetailFragment.SEARCH_FILE_TYPE="f";
					
				}
				else if(rg.getCheckedRadioButtonId()==R.id.dialog_search_rb_foldertype)
				{
					DetailFragment.SEARCH_FILE_TYPE="d";
				}
				else
				{
					DetailFragment.SEARCH_FILE_TYPE="fd";
				}
				*/

				FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(DetailFragment.SEARCH_RESULT),FileObjectType.SEARCH_LIBRARY_TYPE);
				((MainActivity)context).createFragmentTransaction(DetailFragment.SEARCH_RESULT,FileObjectType.SEARCH_LIBRARY_TYPE);
				imm.hideSoftInputFromWindow(search_file_name.getWindowToken(),0);
				dismissAllowingStateLoss();
			}
		});

        Button cancel_button = buttons_layout.findViewById(R.id.second_button);
		cancel_button.setText(R.string.cancel);
		cancel_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				imm.hideSoftInputFromWindow(search_file_name.getWindowToken(),0);
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

	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}

		super.onDestroyView();
	}


	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
	
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
