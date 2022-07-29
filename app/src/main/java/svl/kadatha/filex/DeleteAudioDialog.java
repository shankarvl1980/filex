package svl.kadatha.filex;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteAudioDialog extends DialogFragment
{

	private Context context;
	private String parent_dir;
    private TextView copied_textview;
	private TextView no_files_textview,size_files_textview;
	private EditText current_file;
    private DeleteFileAsyncTask delete_file_async_task;
	private final ArrayList<String> files_selected_for_delete =new ArrayList<>();
	public final List<Integer> deleted_files_idx=new ArrayList<>();
	private List<String> files_selected_array =new ArrayList<>();
	//private final int request_code=8079;
	//private boolean permission_requested;
	private String tree_uri_path="";
	private Uri tree_uri;
    private DeleteAudioCompleteListener deleteAudioCompleteListener;
    private boolean whetherFromAlbum;
    private LocalBroadcastManager localBroadcastManager;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		localBroadcastManager=LocalBroadcastManager.getInstance(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		setCancelable(false);
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			files_selected_array=bundle.getStringArrayList("files_selected_array");
			whetherFromAlbum=bundle.getBoolean("whetherFromAlbum");
			tree_uri= bundle.getParcelable("tree_uri");
			tree_uri_path=bundle.getString("tree_uri_path");

		}

		files_selected_for_delete.addAll(files_selected_array);

		parent_dir=new File(files_selected_for_delete.get(0)).getParent();

		delete_file_async_task=new DeleteFileAsyncTask();
		delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		View v=inflater.inflate(R.layout.fragment_cut_copy_delete_archive_progress,container,false);
        TextView dialog_title = v.findViewById(R.id.dialog_fragment_cut_copy_title);
        TextView from_textview = v.findViewById(R.id.dialog_fragment_cut_copy_from);
        TableRow to_table_row = v.findViewById(R.id.fragment_cut_copy_delete_archive_totablerow);
		current_file=v.findViewById(R.id.dialog_fragment_cut_copy_archive_current_file);
		copied_textview=v.findViewById(R.id.dialog_fragment_copied_file);
		no_files_textview=v.findViewById(R.id.fragment_cut_copy_delete_archive_no_files);
		size_files_textview=v.findViewById(R.id.fragment_cut_copy_delete_archive_size_files);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_cut_copy_delete_progress_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));

        Button cancel_button = buttons_layout.findViewById(R.id.first_button);
		cancel_button.setText(R.string.cancel);
		dialog_title.setText(R.string.deleting);
		to_table_row.setVisibility(View.GONE);
		if(whetherFromAlbum)
		{
			from_textview.setText(parent_dir);
		}
		else
		{
			from_textview.setText(getString(R.string.all_songs));
		}

		cancel_button.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					if(delete_file_async_task!=null)
					{
						delete_file_async_task.cancel(true);

					}
					dismissAllowingStateLoss();

				}
			});

		return v;
	}

	public static DeleteAudioDialog getInstance(ArrayList<String> files_selected_array, boolean whetherFromAlbum, Uri tree_uri, String tree_uri_path)
	{
		DeleteAudioDialog deleteAudioDialog=new DeleteAudioDialog();
		Bundle bundle=new Bundle();
		bundle.putStringArrayList("files_selected_array",files_selected_array);
		bundle.putBoolean("whetherFromAlbum",whetherFromAlbum);
		bundle.putParcelable("tree_uri",tree_uri);
		bundle.putString("tree_uri_path",tree_uri_path);
		deleteAudioDialog.setArguments(bundle);
		return deleteAudioDialog;
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


	public void setDeleteAudioCompleteListener(DeleteAudioCompleteListener listener)
	{
		deleteAudioCompleteListener=listener;
	}

	interface DeleteAudioCompleteListener
	{
		void onDeleteComplete();
	}

	private class DeleteFileAsyncTask extends svl.kadatha.filex.AsyncTask<Void,File,Boolean>
	{
		final List<String> src_file_path_list;
		final List<String> deleted_file_path_list=new ArrayList<>();
		final List<String> deleted_file_name_list=new ArrayList<>();
		int counter_no_files;
		long counter_size_files;
		String current_file_name;
		boolean isFromInternal;
		String size_of_files_format;

		DeleteFileAsyncTask()
		{
			src_file_path_list=files_selected_array;

		}


		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);

			if(deleted_files_idx.size()>0)
			{
				if(whetherFromAlbum)
				{
					FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_dir,deleted_file_name_list,FileObjectType.FILE_TYPE);
				}
				else
				{
					for(String file_path:deleted_file_path_list)
					{
						String parent_dir=new File(file_path).getParent();
						String file_name=new File(file_path).getName();
						FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_dir, Collections.singletonList(file_name),FileObjectType.FILE_TYPE);
					}

				}

				if(deleteAudioCompleteListener!=null)
				{
					deleteAudioCompleteListener.onDeleteComplete();
				}
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,AudioPlayerActivity.ACTIVITY_NAME);
			}
			//if(!permission_requested)
			{
				dismissAllowingStateLoss();
			}

		}

		@Override
		protected Boolean doInBackground(Void...p)
		{
			// TODO: Implement this method
			boolean success;
			if(whetherFromAlbum)
			{
				isFromInternal=FileUtil.isFromInternal(FileObjectType.FILE_TYPE,src_file_path_list.get(0));
				success=deleteFromFolder();
			}
			else
			{
				success=deleteFromLibrarySearch();
			}

			return success;
		}


		private boolean deleteFromFolder()
		{
			boolean success=false;
			int size=src_file_path_list.size();
			if(isFromInternal)
			{

				for(int i=0;i<size;++i)
				{
					String file_path=src_file_path_list.get(i);
					File f=new File(file_path);
					current_file_name=f.getName();
					success=deleteNativeDirectory(f);
					if(success)
					{
						deleted_files_idx.add(src_file_path_list.indexOf(file_path));
						deleted_file_path_list.add(file_path);
						deleted_file_name_list.add(current_file_name);
					}
					files_selected_for_delete.remove(file_path);
				}

			}
			else
			{
				// no need to check SAF permission as this dialog started only after obtaining SAF permission for all files
				for(int i=0;i<size;++i)
				{
					String file_path=src_file_path_list.get(i);
					File file=new File(file_path);
					current_file_name=file.getName();
					success=deleteSAFDirectory(file);
					if(success)
					{
						deleted_files_idx.add(src_file_path_list.indexOf(file_path));
						deleted_file_path_list.add(file_path);
						deleted_file_name_list.add(current_file_name);
					}
					files_selected_for_delete.remove(file_path);
				}

			}
			return success;
		}

		private boolean deleteFromLibrarySearch()
		{
			boolean success=false;
			int size=src_file_path_list.size();
			for(int i=0;i<size;++i)
			{

				String file_path=src_file_path_list.get(i);
				File f=new File(file_path);
				if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,file_path))
				{
					current_file_name=f.getName();
					success=deleteNativeDirectory(f);
				}
				else
				{
					current_file_name=f.getName();
					success=deleteSAFDirectory(f);
				}
				if(success)
				{
					deleted_files_idx.add(src_file_path_list.indexOf(file_path));
					deleted_file_path_list.add(file_path);
					deleted_file_name_list.add(current_file_name);
				}
				files_selected_for_delete.remove(file_path);
			}

			return success;
		}


		public boolean deleteNativeDirectory(final File folder)
		{     
			boolean success=false;

			if (folder.isDirectory())            //Check if folder file is a real folder
			{
				if(isCancelled())
				{
					return false;
				}

				File[] list = folder.listFiles(); //Storing all file name within array
				int size=list.length;
				for (int i = 0; i < size; ++i)
				{
					if(isCancelled())
					{
						return false;
					}

					File tmpF = list[i];
					success=deleteNativeDirectory(tmpF);

				}
			}
			counter_no_files++;
			counter_size_files+=folder.length();
			size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files,Global.BYTE_COUNT_BLOCK_1000);
			publishProgress(folder);
			success=folder.delete();
			return success;
		}

		public boolean deleteSAFDirectory(final File folder)
		{
			boolean success=true;
			if (folder.isDirectory())            //Check if folder file is a real folder
			{
				if(isCancelled())
				{
					return false;
				}
				File[] list = folder.listFiles(); //Storing all file name within array
				int size=list.length;
				for (int i = 0; i < size; ++i)
				{
					if(isCancelled())
					{
						return false;
					}
					File tmpF = list[i];
					success=deleteSAFDirectory(tmpF);

				}

			}

			counter_no_files++;
			counter_size_files+=folder.length();
			size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files,Global.BYTE_COUNT_BLOCK_1000);
			publishProgress(folder);
			success=FileUtil.deleteSAFDirectory(context,folder.getAbsolutePath(),tree_uri,tree_uri_path);

			return success;
		}

		@Override
		protected void onProgressUpdate(File... file)
		{
			// TODO: Implement this method
			super.onProgressUpdate(file);
			if(copied_textview!=null)
			{
				current_file.setText(current_file_name);
				copied_textview.setText(file[0].getName());
				if(isFromInternal)
				{
					no_files_textview.setText(getString(R.string.deleted_colon)+" "+counter_no_files + (counter_no_files<2 ? getString(R.string.file1) : getString(R.string.files)));
					size_files_textview.setText(getString(R.string.size_colon)+" "+size_of_files_format);
				}

			}

		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method

			super.onPostExecute(result);

			if(deleted_files_idx.size()>0)
			{
				if(whetherFromAlbum)
				{
					FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_dir,deleted_file_name_list,FileObjectType.FILE_TYPE);
				}
				else
				{
					for(String file_path:deleted_file_path_list)
					{
						String parent_dir=new File(file_path).getParent();
						String file_name=new File(file_path).getName();
						FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_dir, Collections.singletonList(file_name),FileObjectType.FILE_TYPE);
					}
				}

				if(deleteAudioCompleteListener!=null)
				{
					deleteAudioCompleteListener.onDeleteComplete();
				}
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,AudioPlayerActivity.ACTIVITY_NAME);
				Global.print(context,getString(R.string.selected_audios_deleted));

			}
			else
			{
				Global.print(context,getString(R.string.selected_audios_could_not_be_deleted));

			}

			dismissAllowingStateLoss();
		}

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
}
