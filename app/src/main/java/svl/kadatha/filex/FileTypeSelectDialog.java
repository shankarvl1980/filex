package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class FileTypeSelectDialog extends DialogFragment
{
	private Context context;
	private String mime_type;
    private String file_path;
    private long file_size;
    private FileObjectType fileObjectType;
	private boolean select_app;
	private final static String SAF_PERMISSION_REQUEST_CODE="file_type_selector_dialog_saf_permission_request_code";
	private List<MimePOJO> mimePOJOList;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setCancelable(false);
		Bundle bundle = getArguments();
		file_path= bundle.getString("file_path");
		fileObjectType= (FileObjectType) bundle.getSerializable("fileObjectType");
        Uri tree_uri = bundle.getParcelable("tree_uri");
        String tree_uri_path = bundle.getString("tree_uri_path");
		select_app=bundle.getBoolean("select_app");
		file_size=bundle.getLong("file_size");

		mimePOJOList=new ArrayList<>(Global.SUPPORTED_MIME_POJOS);
		mimePOJOList.add(new MimePOJO("Other","*/*",""));
	}

	public static FileTypeSelectDialog getInstance(String file_path, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path,boolean select_app,long file_size)
	{
		FileTypeSelectDialog fileTypeSelectDialog=new FileTypeSelectDialog();
		Bundle bundle=new Bundle();
		bundle.putString("file_path",file_path);
		bundle.putSerializable("fileObjectType",fileObjectType);
		bundle.putParcelable("tree_uri",tree_uri);
		bundle.putString("tree_uri_path",tree_uri_path);
		bundle.putBoolean("select_app",select_app);
		bundle.putLong("file_size",file_size);
		fileTypeSelectDialog.setArguments(bundle);
		return fileTypeSelectDialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_select_file_type,container,false);
		FrameLayout progress_bar = v.findViewById(R.id.fragment_file_type_select_progressbar);
		progress_bar.setVisibility(View.GONE);
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
		window.setLayout(Global.DIALOG_WIDTH, GridLayout.LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
			p1.file_type_tv.setText(mimePOJOList.get(p2).getFile_type());
		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return mimePOJOList.size();
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
						MimePOJO mimePOJO=mimePOJOList.get(pos);
						mime_type=mimePOJO.getMime_type();

						FileIntentDispatch.openFile(context,file_path,mime_type,false,fileObjectType,select_app,file_size);
						dismissAllowingStateLoss();
					}
				});
			}
		}
		
	}

}
