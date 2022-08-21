package svl.kadatha.filex;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WorkingDirRecyclerAdapter extends RecyclerView.Adapter<WorkingDirRecyclerAdapter.ViewHolder>
{
	private final Context context;
	private final List<String> working_dir_arraylist;
	public ArrayList<String> custom_dir_selected_array=new ArrayList<>();
	public HashMap<Integer,Boolean> custom_dir_selected_hash_map=new HashMap<>();
	private ItemClickListener itemClickListener;
	WorkingDirRecyclerAdapter(Context context,List<String> working_dir_arraylist)
	{
		this.context=context;
		this.working_dir_arraylist=working_dir_arraylist;
		
	}
	
	class ViewHolder extends RecyclerView.ViewHolder
	{
		final View view;
		final TextView textView_working_dir;
		final ImageView select_indicator;
		int pos;
		
		ViewHolder(View v)
		{
			super(v);
			this.view=v;
			textView_working_dir=view.findViewById(R.id.working_dir_name);
			select_indicator=view.findViewById(R.id.working_dir_select_indicator);
			view.setOnLongClickListener(new View.OnLongClickListener()
				{
					public boolean onLongClick(View p)
					{
						pos=getBindingAdapterPosition();
						onLongClickProcedure(p);
						return true;

					}
				});

			view.setOnClickListener(new View.OnClickListener()
				{

					public void onClick(View p)
					{
						pos=getBindingAdapterPosition();
						if(custom_dir_selected_array.size()>0)
						{
							onLongClickProcedure(p);
						}
						else
						{
							if(itemClickListener!=null)
							{
								itemClickListener.onItemClick(pos,working_dir_arraylist.get(pos));
							}

						}
					}

				});

		}
		private void onLongClickProcedure(View v)
		{
			if(custom_dir_selected_hash_map.get(pos) != null)
			{
				v.setSelected(false);
				select_indicator.setVisibility(View.INVISIBLE);
				custom_dir_selected_hash_map.remove(pos);
				custom_dir_selected_array.remove(working_dir_arraylist.get(pos));
			}
			else
			{
				v.setSelected(true);
				select_indicator.setVisibility(View.VISIBLE);
				custom_dir_selected_hash_map.put(pos,true);
				custom_dir_selected_array.add(working_dir_arraylist.get(pos));
			}
		}
	}

	@Override
	public WorkingDirRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
	{
		// TODO: Implement this method
		View itemview=LayoutInflater.from(context).inflate(R.layout.working_dir_recyclerview_layout,p1,false);
		return new ViewHolder(itemview);
	}

	@Override
	public void onBindViewHolder(WorkingDirRecyclerAdapter.ViewHolder p1, int p2)
	{
		// TODO: Implement this method
		p1.textView_working_dir.setText(working_dir_arraylist.get(p2));
		if(custom_dir_selected_hash_map.get(p2) != null)
		{
			p1.view.setSelected(true);
			p1.select_indicator.setVisibility(View.VISIBLE);
		}
		else
		{
			p1.view.setSelected(false);
			p1.select_indicator.setVisibility(View.INVISIBLE);
		}

	}

	@Override
	public int getItemCount()
	{
		// TODO: Implement this method
		return working_dir_arraylist.size();
	}

	
	public int insert(String f)
	{
		working_dir_arraylist.add(f);
		custom_dir_selected_array=new ArrayList<>();
		custom_dir_selected_hash_map=new HashMap<>();
		notifyDataSetChanged();
		return working_dir_arraylist.indexOf(f);
	}

	public void remove(List<String> selectedDirs)
	{
		working_dir_arraylist.removeAll(selectedDirs);
		custom_dir_selected_array=new ArrayList<>();
		custom_dir_selected_hash_map=new HashMap<>();
		notifyDataSetChanged();
	}
	
	public void setOnItemClickListenerForWorkingDirAdapter(ItemClickListener listener)
	{
		itemClickListener=listener;
	}

	interface ItemClickListener
	{
		void onItemClick(int position, String name);
	}
	

}
