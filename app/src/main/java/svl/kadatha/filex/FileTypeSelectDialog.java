package svl.kadatha.filex;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.content.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class FileTypeSelectDialog extends DialogFragment
{
	private Context context;
    private FileTypeSelectListener fileTypeSelectListener;
	//private final LinkedHashMap<String,String> file_type_set=new LinkedHashMap<>();
	//private List<String> file_type_list;
	//private List<String> file_mime_list;


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_select_file_type,container,false);
        RecyclerView file_type_recyclerview = v.findViewById(R.id.fragment_file_type_RecyclerView);
		file_type_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
		file_type_recyclerview.setLayoutManager(new LinearLayoutManager(context));
		file_type_recyclerview.setAdapter(new FileTypeRecyclerViewAdapter());

        ViewGroup button_layout = v.findViewById(R.id.fragment_file_type_button_layout);
		button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button cancel = button_layout.findViewById(R.id.first_button);
		cancel.setText(R.string.cancel);
		cancel.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View p1)
			{
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

		WindowManager.LayoutParams params=window.getAttributes();
		int height=params.height;
		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);

		}
		else
		{
			window.setLayout(Global.DIALOG_WIDTH,Math.min(height,Global.DIALOG_HEIGHT));
		
		}

		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();

	}
	
	private class FileTypeRecyclerViewAdapter extends RecyclerView.Adapter<FileTypeRecyclerViewAdapter.VH>
	{

		@Override
		public FileTypeSelectDialog.FileTypeRecyclerViewAdapter.VH onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			View v=LayoutInflater.from(context).inflate(R.layout.working_dir_recyclerview_layout,p1,false);
			return new VH(v);
		}

		@Override
		public void onBindViewHolder(FileTypeSelectDialog.FileTypeRecyclerViewAdapter.VH p1, int p2)
		{
			// TODO: Implement this method
			p1.file_type_tv.setText(Global.SUPPORTED_MIME_POJOS.get(p2).getFile_type());
		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return Global.SUPPORTED_MIME_POJOS.size();
		}

		
		private class VH extends RecyclerView.ViewHolder
		{
			final View v;
			final TextView file_type_tv;
			int pos;
			VH(View vi)
			{
				super(vi);
				v=vi;
				file_type_tv=v.findViewById(R.id.working_dir_name);
				
				vi.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View p1)
					{
						pos=getBindingAdapterPosition();
						if(fileTypeSelectListener!=null)
						{
							MimePOJO mimePOJO=Global.SUPPORTED_MIME_POJOS.get(pos);
							fileTypeSelectListener.onSelectType(mimePOJO.getMime_type());
						}
						dismissAllowingStateLoss();
					}
				});
			}
		}
		
	}
	
	
	interface FileTypeSelectListener
	{
		void onSelectType(String mime_type);
	}
	
	public void setFileTypeSelectListener(FileTypeSelectListener listener)
	{
		fileTypeSelectListener=listener;
	}

}
