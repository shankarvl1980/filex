package svl.kadatha.filex;
import android.content.Context;
import android.os.Handler;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class DetailRecyclerViewAdapter extends  RecyclerView.Adapter <DetailRecyclerViewAdapter.ViewHolder> implements Filterable
{
	
	private final Context context;
	private final DetailFragment df;
	private final MainActivity mainActivity;

	private CardViewClickListener cardViewClickListener;
    private final Handler handler_remove;
	private boolean show_file_path;


	DetailRecyclerViewAdapter(Context context,boolean archiveview)
	{
		this.context=context;
		mainActivity=(MainActivity)context;
		df=(DetailFragment)mainActivity.fm.findFragmentById(R.id.detail_fragment);
        //Handler handler_refresh = new Handler();
		handler_remove=new Handler();
		mainActivity.current_dir_textview.setText(df.file_click_selected_name);
		mainActivity.file_number_view.setText(df.mselecteditems.size()+"/"+df.file_list_size);
		if(df.fileObjectType==FileObjectType.FILE_TYPE || df.fileObjectType==FileObjectType.ROOT_TYPE)
		{
			File f=new File(df.fileclickselected);
			File parent_file=f.getParentFile();
			if(parent_file!=null)
			{
				if(!(archiveview && f.equals(MainActivity.ARCHIVE_EXTRACT_DIR)))
				{
					mainActivity.parent_dir_imagebutton.setEnabled(true);
					mainActivity.parent_dir_imagebutton.setAlpha(Global.ENABLE_ALFA);
				}
			}
			else
			{
				mainActivity.current_dir_textview.setText(R.string.root_directory);
				mainActivity.parent_dir_imagebutton.setEnabled(false);
				mainActivity.parent_dir_imagebutton.setAlpha(Global.DISABLE_ALFA);
			}
		}
		else if(df.fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			mainActivity.parent_dir_imagebutton.setEnabled(false);
			mainActivity.parent_dir_imagebutton.setAlpha(Global.DISABLE_ALFA);

			if(df.fileclickselected.equals(DetailFragment.SEARCH_RESULT))
			{
				show_file_path=true;
			}
			else
			{
				show_file_path=Global.SHOW_FILE_PATH;
			}
		}
		else if(df.fileObjectType== FileObjectType.USB_TYPE)
		{
			UsbFile f=df.currentUsbFile;
			UsbFile parent_file=null;
			if (f != null) {
				parent_file=f.getParent();
			}

			if(parent_file!=null)
			{
				mainActivity.parent_dir_imagebutton.setEnabled(true);
				mainActivity.parent_dir_imagebutton.setAlpha(Global.ENABLE_ALFA);
			}
			else
			{
				mainActivity.current_dir_textview.setText(DetailFragment.USB_FILE_PREFIX+File.separator);
				mainActivity.parent_dir_imagebutton.setEnabled(false);
				mainActivity.parent_dir_imagebutton.setAlpha(Global.DISABLE_ALFA);
			}
		}

	}

	class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnLongClickListener
	{
		final RecyclerViewLayout view;
		int pos;
		
		ViewHolder (RecyclerViewLayout view)
		{
			super(view);
			this.view=view;
			this.view.setOnClickListener(this);
			this.view.setOnLongClickListener(this);

		}

		@Override
		public void onClick(View p1)
		{
			pos=getBindingAdapterPosition();
			int size=df.mselecteditems.size();
			if(size>0)
			{
				longClickMethod(p1,size);
			}
			else 
			{
				if(cardViewClickListener!=null)
				{
					FilePOJO filePOJO=df.filePOJO_list.get(pos);
					cardViewClickListener.onClick(filePOJO);
				}
			}
		}
		
		private void longClickMethod (View v, int size)
		{
			pos=getBindingAdapterPosition();
			if(df.mselecteditems.get(pos,false))
			{
				df.mselecteditems.delete(pos);
				df.mselecteditemsFilePath.delete(pos);
				v.setSelected(false);
				((RecyclerViewLayout)v).set_selected(false);
				--size;

				if(size==1)
				{
					mainActivity.rename.setEnabled(true);
					mainActivity.rename.setAlpha(Global.ENABLE_ALFA);

					if(cardViewClickListener!=null)
					{
						FilePOJO filePOJO=df.filePOJO_list.get(pos);
						cardViewClickListener.onLongClick(filePOJO);
					}
				}
				else if(size>1)
				{
					mainActivity.rename.setEnabled(false);
					mainActivity.rename.setAlpha(Global.DISABLE_ALFA);

					if(cardViewClickListener!=null)
					{
						FilePOJO filePOJO=df.filePOJO_list.get(pos);
						cardViewClickListener.onLongClick(filePOJO);
					}
				}

				if(size==0)
				{
					mainActivity.DeselectAllAndAdjustToolbars(df,df.fileclickselected);
				}
			}
			else
			{
				df.mselecteditems.put(pos,true);
				df.mselecteditemsFilePath.put(pos,df.filePOJO_list.get(pos).getPath());
				v.setSelected(true);
				((RecyclerViewLayout)v).set_selected(true);
				++size;

				if(size==1)
				{
					mainActivity.rename.setEnabled(true);
					mainActivity.rename.setAlpha(Global.ENABLE_ALFA);
				}
				else if(size>1)
				{
					mainActivity.rename.setEnabled(false);
					mainActivity.rename.setAlpha(Global.DISABLE_ALFA);
				}

				if(size==df.file_list_size)
				{
					mainActivity.all_select.setImageResource(R.drawable.deselect_icon);
				}

				if(cardViewClickListener!=null)
				{
					FilePOJO filePOJO=df.filePOJO_list.get(pos);
					cardViewClickListener.onLongClick(filePOJO);
				}
			}
			mainActivity.file_number_view.setText(size+"/"+df.file_list_size);
		}
		
		@Override
		public boolean onLongClick(View p1)
		{
			longClickMethod(p1,df.mselecteditems.size());
			return true;
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
	{
		return new ViewHolder(new RecyclerViewLayout(context,show_file_path));
	}

	@Override
	public void onBindViewHolder(DetailRecyclerViewAdapter.ViewHolder p1, int p2)
	{
		FilePOJO file=df.filePOJO_list.get(p2);
		boolean selected=df.mselecteditems.get(p2,false);
		p1.view.setData(file,selected);
		p1.view.setSelected(selected);
			
	}

	@Override
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {

				df.filePOJO_list = new ArrayList<>();
				if (constraint == null || constraint.length() == 0) {
					df.filePOJO_list = df.totalFilePOJO_list;
				} else {
					String pattern = constraint.toString().toLowerCase().trim();
					for (int i = 0; i < df.totalFilePOJO_list_Size; ++i) {
						FilePOJO filePOJO = df.totalFilePOJO_list.get(i);
						if (filePOJO.getLowerName().contains(pattern)) {
							df.filePOJO_list.add(filePOJO);
						}
					}
				}
				return new FilterResults();
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {

				int t=df.filePOJO_list.size();
				if(df.mselecteditems.size()>0)
				{
					deselectAll();
				}
				else
				{
					notifyDataSetChanged();
				}
				if(t>0)
				{
						df.recyclerView.setVisibility(View.VISIBLE);
						df.folder_empty.setVisibility(View.GONE);
				}
				mainActivity.file_number_view.setText(df.mselecteditems.size()+ "/" +t);

			}
		};
	}


	@Override
	public int getItemCount()
	{
		return df.filePOJO_list.size();
	}
	



	public void clear_cache_and_refresh(String file_path, FileObjectType fileObjectType)
	{
		if(df!=null)
		{
			df.mselecteditems=new SparseBooleanArray();
			df.mselecteditemsFilePath=new SparseArray<>();
			df.mainActivity.clearCache(file_path,fileObjectType);
			df.modification_observed=true;
		}
		Global.WORKOUT_AVAILABLE_SPACE();
	}
	

	public void remove_item(final String source_folder, final List<String> deleted_files_list)
	{

		final int size=deleted_files_list.size();
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		pbf.show(mainActivity.fm, "");
		handler_remove.post(new Runnable() {
			@Override
			public void run() {
				if(df.filled_filePOJOs)
				{
					String name;
					for(int i=0;i<size;++i)
					{
						name=deleted_files_list.get(i);
						boolean removed=false;
						int idx=0;
						Iterator<FilePOJO> iterator=df.filePOJO_list.iterator();
						while(iterator.hasNext())
						{
							if(iterator.next().getName().equals(name))
							{

								iterator.remove();
								removed=true;
								break;
							}
							++idx;
						}
						if(removed)
						{
							notifyItemRemoved(idx);
						}
						remove_from_FilePOJO(name,df.totalFilePOJO_list);
					}

					df.file_list_size=df.totalFilePOJO_list.size();
					mainActivity.file_number_view.setText(df.mselecteditems.size()+"/"+df.file_list_size);
					if(df.filePOJO_list!=null && df.filePOJO_list.size()==0)
					{
						df.recyclerView.setVisibility(View.GONE);
						df.folder_empty.setVisibility(View.VISIBLE);

					}
					Global.WORKOUT_AVAILABLE_SPACE();
					pbf.dismissAllowingStateLoss();
					handler_remove.removeCallbacks(this);
				}
				else
				{
					handler_remove.postDelayed(this,50);
				}
			}
		});
	}

	private void remove_from_FilePOJO(String name, List<FilePOJO> list)
	{
		Iterator<FilePOJO> iterator=list.iterator();
		while(iterator.hasNext())
		{
			if(iterator.next().getName().equals(name))
			{
				iterator.remove();
				break;
			}
		}
	}

/*
	public void remove_item_index_based(final LinkedHashMap<Integer, String> index_array)
	{
		final List<Integer> reveresedKeySet=new ArrayList<>(index_array.keySet());
		Collections.sort(reveresedKeySet,Collections.<Integer>reverseOrder());
		final int s=reveresedKeySet.size();
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		pbf.show(MainActivity.FM,"");
		handler_remove.post(new Runnable() {
			@Override
			public void run() {
				if(df.filled_filePOJOs)
				{
					pbf.dismissAllowingStateLoss();
					for(int i=0;i<s;++i)
					{
						int idx=reveresedKeySet.get(i);
						String file_path=index_array.get(idx);
						FilePOJO filePOJO=filePOJO_list.get(idx);
						if(filePOJO.getPath().equals(file_path))
						{
							filePOJO_list.remove(filePOJO);
							notifyItemRemoved(idx);
						}
						totalFilePOJO.remove(filePOJO);

					}

					df.file_list_size=totalFilePOJO.size();
					mainActivity.file_number_view.setText(df.mselecteditems.size()+"/"+df.file_list_size);
					MainActivity.Global.WORKOUT_AVAILABLE_SPACE();
					handler_remove.removeCallbacks(this);
				}
				else
				{
					handler_remove.postDelayed(this,50);
				}
			}
		});

	}

 */

	public void selectAll()
	{
		df.mselecteditems=new SparseBooleanArray();
		df.mselecteditemsFilePath=new SparseArray<>();
		int size=df.filePOJO_list.size();

		for(int i=0;i<size;++i)
		{
			df.mselecteditems.put(i,true);
			df.mselecteditemsFilePath.put(i,df.filePOJO_list.get(i).getPath());
		}

		int s=df.mselecteditems.size();
		if(s==1)
		{
			mainActivity.rename.setEnabled(true);
			mainActivity.rename.setAlpha(Global.ENABLE_ALFA);
		}
		else if(s>1)
		{
			mainActivity.rename.setEnabled(false);
			mainActivity.rename.setAlpha(Global.DISABLE_ALFA);
		}
		mainActivity.file_number_view.setText(s+"/"+size);
		notifyDataSetChanged();
		if(!mainActivity.toolbar_shown.equals("paste") && !mainActivity.toolbar_shown.equals("extract"))
		{
			mainActivity.actionmode_toolbar.setVisibility(View.VISIBLE);
			mainActivity.paste_toolbar.setVisibility(View.GONE);
			mainActivity.bottom_toolbar.setVisibility(View.GONE);
			mainActivity.toolbar_shown ="actionmode";
		}
	}
	
	public void deselectAll()
	{
		mainActivity.DeselectAllAndAdjustToolbars(df,df.fileclickselected);
	}


	interface CardViewClickListener
	{
		void onClick(FilePOJO filePOJO);
		void onLongClick(FilePOJO filePOJO);
	}
	
	
	public void setCardViewClickListener(CardViewClickListener listner)
	{
		this.cardViewClickListener=listner;
	}
	
	
	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
}
