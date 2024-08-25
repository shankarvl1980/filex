package svl.kadatha.filex;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.asynctasks.ArchiveAsyncTask;
import svl.kadatha.filex.asynctasks.CopyToAsyncTask;
import svl.kadatha.filex.asynctasks.CutCopyAsyncTask;
import svl.kadatha.filex.asynctasks.DeleteAsyncTask;
import svl.kadatha.filex.asynctasks.TaskProgressListener;
import svl.kadatha.filex.asynctasks.UnarchiveAsyncTask;

public class ArchiveDeletePasteFileService2 extends Service implements TaskProgressListener
{

	String dest_folder,zip_file_path, zip_folder_name,archive_action;
	public static FileObjectType SOURCE_FILE_OBJECT=null,DEST_FILE_OBJECT=null;
	private Context context;
	private final ArrayList<String> files_selected_array=new ArrayList<>();
	private final ArrayList<String> zipentry_selected_array=new ArrayList<>();

	private NotifManager nm;
	private int notification_id;
	public int counter_no_files;
	public long counter_size_files;
	String copied_file_name, file_name;
	public FileCountSize fileCountSize;
	private ArchiveAsyncTask archiveAsyncTask;
	private UnarchiveAsyncTask unarchiveAsyncTask;
	private String tree_uri_path="",source_uri_path="";
	private Uri tree_uri,source_uri;
	private ArchiveDeletePasteBinder binder=new ArchiveDeletePasteBinder();
	private ServiceCompletionListener serviceCompletionListener;
	static boolean SERVICE_COMPLETED=true;
	private String intent_action;

	private DeleteAsyncTask delete_async_task;
	String current_file_name;

	String source_folder;
	boolean cut;
	private CutCopyAsyncTask cutCopyAsyncTask;
	private CopyToAsyncTask copyToAsyncTask;

	String size_of_files_copied;
	String copied_file;
	FileObjectType sourceFileObjectType, destFileObjectType;

	private String source_other_file_permission,dest_other_file_permission;
	private boolean storage_analyser_delete;
	final List<String> overwritten_file_path_list=new ArrayList<>();

	private String zip_file_name;

	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		SERVICE_COMPLETED=false;
		super.onCreate();
		context=this;
		String class_name=getClass().getName();
		switch (class_name) {
			case "svl.kadatha.filex.ArchiveDeletePasteFileService1":
				notification_id = 870;
				break;
			case "svl.kadatha.filex.ArchiveDeletePasteFileService2":
				notification_id = 871;
				break;
			case "svl.kadatha.filex.ArchiveDeletePasteFileService3":
				notification_id = 872;
				break;
			default:
				notification_id = (int) System.currentTimeMillis();
				break;
		}
		nm=new NotifManager(context);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// TODO: Implement this method
		String notification_content = null;
		Bundle bundle=intent.getBundleExtra("bundle");
		intent_action=intent.getAction();

		switch(intent_action)
		{
			case ArchiveAsyncTask.TASK_TYPE:
				if(bundle!=null)
				{
					dest_folder=bundle.getString("dest_folder");
					destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
					zip_file_path=bundle.getString("zip_file_path");
					source_folder=bundle.getString("source_folder");
					sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
					zip_folder_name=bundle.getString("zip_folder_name");
					archive_action=bundle.getString("archive_action");
					tree_uri_path=bundle.getString("tree_uri_path");
					tree_uri=bundle.getParcelable("tree_uri");
					files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
					zip_file_name=zip_folder_name+".zip";
					fileCountSize=new FileCountSize(context,files_selected_array,source_uri,source_uri_path,sourceFileObjectType);
					fileCountSize.fileCount();
					archiveAsyncTask = new ArchiveAsyncTask(context,files_selected_array,zip_file_name,dest_folder,zip_file_path,destFileObjectType,sourceFileObjectType,tree_uri,tree_uri_path,this);
					archiveAsyncTask.execute(null);
					notification_content=getString(R.string.zipping)+" '"+zip_folder_name+".zip "+getString(R.string.at)+" "+dest_folder;

				}
				break;

			case UnarchiveAsyncTask.TASK_TYPE:
				if(bundle!=null)
				{
					dest_folder=bundle.getString("dest_folder");
					destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
					zip_file_path=bundle.getString("zip_file_path");
					source_folder=bundle.getString("source_folder");
					sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
					zip_folder_name=bundle.getString("zip_folder_name");
					archive_action=bundle.getString("archive_action");
					tree_uri_path=bundle.getString("tree_uri_path");
					tree_uri=bundle.getParcelable("tree_uri");
					files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
					if(bundle.getStringArrayList("zipentry_selected_array")!=null)
					{
						zipentry_selected_array.addAll(bundle.getStringArrayList("zipentry_selected_array"));
					}

					unarchiveAsyncTask=new UnarchiveAsyncTask(dest_folder,zipentry_selected_array,destFileObjectType,zip_folder_name,zip_file_path,tree_uri,tree_uri_path,this);
					unarchiveAsyncTask.execute(null);
					notification_content=getString(R.string.unzipping)+" '"+zip_file_path+" "+getString(R.string.at)+" "+dest_folder;

				}
				break;

			case DeleteAsyncTask.TASK_TYPE:
				if(bundle!=null)
				{
					source_uri_path=bundle.getString("source_uri_path");
					source_uri=bundle.getParcelable("source_uri");
					files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
					sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
					source_folder=bundle.getString("source_folder");
					storage_analyser_delete = bundle.getBoolean("storage_analyser_delete");
					fileCountSize=new FileCountSize(context,files_selected_array,source_uri,source_uri_path,sourceFileObjectType);
					fileCountSize.fileCount();
					delete_async_task=new DeleteAsyncTask(files_selected_array,source_folder,source_uri,source_uri_path,sourceFileObjectType,this);
					delete_async_task.execute(null);
					notification_content=getString(R.string.deleting_files)+" "+getString(R.string.at)+" "+source_folder;

				}
				break;

			case CutCopyAsyncTask.TASK_TYPE_CUT:
			case CutCopyAsyncTask.TASK_TYPE_COPY:
				if(bundle!=null)
				{
					files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
					overwritten_file_path_list.addAll(bundle.getStringArrayList("overwritten_file_path_list"));
					sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
					dest_folder=bundle.getString("dest_folder");
					destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
					tree_uri_path=bundle.getString("tree_uri_path");
					tree_uri=bundle.getParcelable("tree_uri");
					source_uri_path=bundle.getString("source_uri_path");
					source_uri=bundle.getParcelable("source_uri");
					cut=bundle.getBoolean("cut");
					boolean isWritable = bundle.getBoolean("isWritable");
					source_folder=bundle.getString("source_folder");
					fileCountSize=new FileCountSize(context,files_selected_array,source_uri,source_uri_path,sourceFileObjectType);
					fileCountSize.fileCount();
					cutCopyAsyncTask=new CutCopyAsyncTask(files_selected_array,source_folder,sourceFileObjectType,source_uri,source_uri_path,dest_folder,destFileObjectType,tree_uri,tree_uri_path,cut,overwritten_file_path_list,this);
					cutCopyAsyncTask.execute(null);
					notification_content=(cut? getString(R.string.moving_files) +" "+getString(R.string.to_symbol)+" "+dest_folder : getString(R.string.copying_files)+" "+getString(R.string.to_symbol)+" "+dest_folder);

				}
				break;

			case CopyToAsyncTask.TASK_TYPE:
				if(bundle!=null)
				{
					List<Uri> data_list = bundle.getParcelableArrayList("data_list");
					overwritten_file_path_list.addAll(bundle.getStringArrayList("overwritten_file_path_list"));
					file_name=bundle.getString("file_name");
					dest_folder=bundle.getString("dest_folder");
					destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
					tree_uri_path=bundle.getString("tree_uri_path");
					tree_uri=bundle.getParcelable("tree_uri");
					long uri_size = 0;
					for(Uri data: data_list){
						uri_size += CopyToAsyncTask.getLengthUri(context,data);
					}

					size_of_files_copied = FileUtil.humanReadableByteCount(uri_size);

					fileCountSize=new FileCountSize(data_list.size(),counter_size_files);
					fileCountSize.fileCount();
					copyToAsyncTask=new CopyToAsyncTask(context, data_list,file_name,dest_folder,destFileObjectType,tree_uri,tree_uri_path,overwritten_file_path_list,this);
					copyToAsyncTask.execute(null);
					notification_content=(getString(R.string.copying_files)+" "+getString(R.string.to_symbol)+" "+dest_folder);

				}
				break;

			default:
				stopSelf();
				SERVICE_COMPLETED=true;
				SOURCE_FILE_OBJECT=null;
				DEST_FILE_OBJECT=null;
				break;
		}

		SOURCE_FILE_OBJECT=sourceFileObjectType;
		DEST_FILE_OBJECT=destFileObjectType;

		dest_other_file_permission=Global.GET_OTHER_FILE_PERMISSION(dest_folder);
		source_other_file_permission=Global.GET_OTHER_FILE_PERMISSION(source_folder);

		startForeground(notification_id,nm.buildADPPActivity2(intent_action,notification_content,notification_id));
		return START_NOT_STICKY;
	}


	@Override
	public IBinder onBind(Intent p1)
	{
		// TODO: Implement this method
		if(binder==null)
		{
			binder=new ArchiveDeletePasteBinder();
		}
		return binder;
	}

	class ArchiveDeletePasteBinder extends Binder
	{
		public ArchiveDeletePasteFileService2 getService()
		{
			return ArchiveDeletePasteFileService2.this;
		}
	}

	public void cancelService()
	{
		if(intent_action==null)
		{
			stopForeground(true);
			stopSelf();
		}
		else
		{
			switch(intent_action)
			{
				case ArchiveAsyncTask.TASK_TYPE:
					if(fileCountSize!=null && archiveAsyncTask!=null)
					{
						fileCountSize.cancel(true);
						archiveAsyncTask.cancel(true);
					}
					break;
				case UnarchiveAsyncTask.TASK_TYPE:
					if(unarchiveAsyncTask!=null)
					{
						unarchiveAsyncTask.cancel(true);
					}
					break;
				case DeleteAsyncTask.TASK_TYPE:
					if(delete_async_task!=null)
					{
						delete_async_task.cancel(true);
					}
					break;
				case CutCopyAsyncTask.TASK_TYPE_CUT:
				case CutCopyAsyncTask.TASK_TYPE_COPY:
					if(cutCopyAsyncTask!=null)
					{
						cutCopyAsyncTask.cancel(true);
					}
					break;

				case CopyToAsyncTask.TASK_TYPE:
					if(copyToAsyncTask!=null)
					{
						copyToAsyncTask.cancel(true);
					}
					break;

				default:
					stopForeground(true);
					stopSelf();
					break;
			}
		}
		SERVICE_COMPLETED=true;
		SOURCE_FILE_OBJECT=null;
		DEST_FILE_OBJECT=null;
		Global.SET_OTHER_FILE_PERMISSION(dest_other_file_permission,dest_folder);
		Global.SET_OTHER_FILE_PERMISSION(source_other_file_permission,source_folder);
	}

	@Override
	public void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		SERVICE_COMPLETED=true;
		SOURCE_FILE_OBJECT=null;
		DEST_FILE_OBJECT=null;
		Global.SET_OTHER_FILE_PERMISSION(dest_other_file_permission,dest_folder);
		Global.SET_OTHER_FILE_PERMISSION(source_other_file_permission,source_folder);
	}


	@Override
	public void onProgressUpdate(String taskType, int filesProcessed, long totalBytesProcessed, String currentFileName,String copied_file_name) {
		this.counter_no_files = filesProcessed;
		this.counter_size_files = totalBytesProcessed;
		this.current_file_name = currentFileName;
		this.copied_file_name= copied_file_name;
		size_of_files_copied = FileUtil.humanReadableByteCount(counter_size_files);

	}

	@Override
	public void onTaskCompleted(String taskType, boolean result, FilePOJO filePOJO) {
		if(taskType.equals(ArchiveAsyncTask.TASK_TYPE)){
			String notification_content=ArchiveDeletePasteServiceUtil.ON_ARCHIVE_ASYNCTASK_COMPLETE(context,result,filePOJO,dest_folder,zip_file_name,destFileObjectType);
			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,result,zip_file_name,dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}
		}
		else if(taskType.equals(UnarchiveAsyncTask.TASK_TYPE)){
			String notification_content=ArchiveDeletePasteServiceUtil.ON_UNARCHIVE_ASYNCTASK_COMPLETE(context,counter_no_files,filePOJO,dest_folder, destFileObjectType, zip_file_path,!result);
			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,counter_no_files>0,new File(zip_file_path).getName(),dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}
		}
		else if(taskType.equals(DeleteAsyncTask.TASK_TYPE)){
			int s=delete_async_task.deleted_file_names.size();
			String notification_content=ArchiveDeletePasteServiceUtil.ON_DELETE_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,sourceFileObjectType,delete_async_task.deleted_file_names,delete_async_task.deleted_files_path_list,!result,storage_analyser_delete);
			if(serviceCompletionListener!=null)
			{
				if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
				{
					serviceCompletionListener.onServiceCompletion(intent_action,s>0,null,"");
				}
				else
				{
					serviceCompletionListener.onServiceCompletion(intent_action,s>0,null,source_folder);
				}
			}
			else {
				nm.notify(notification_content,notification_id);
			}
		}
		else if(taskType.equals(CutCopyAsyncTask.TASK_TYPE_CUT) || taskType.equals(CutCopyAsyncTask.TASK_TYPE_COPY)){
			String notification_content=ArchiveDeletePasteServiceUtil.ON_CUT_COPY_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,dest_folder,sourceFileObjectType,destFileObjectType,filePOJO, cut,!result);
			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,counter_no_files>0,null,dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}
		}
		else if(taskType.equals(CopyToAsyncTask.TASK_TYPE)){
			String notification_content=ArchiveDeletePasteServiceUtil.ON_COPY_TO_ASYNCTASK_COMPLETE(context,result,dest_folder,file_name,destFileObjectType,filePOJO);
			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,counter_no_files>0,null,dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}
		}

		stopForeground(true);
		stopSelf();
		SERVICE_COMPLETED=true;
		SOURCE_FILE_OBJECT=null;
		DEST_FILE_OBJECT=null;
	}

	@Override
	public void onTaskCancelled(String taskType, FilePOJO filePOJO) {
		if(taskType.equals(ArchiveAsyncTask.TASK_TYPE)){
			ArchiveDeletePasteServiceUtil.ON_ARCHIVE_ASYNCTASK_CANCEL(context,dest_folder,zip_file_name,destFileObjectType,tree_uri,tree_uri_path);
			nm.notify(getString(R.string.could_not_create)+" '"+zip_file_name+"'",notification_id);
		}
		else if(taskType.equals(UnarchiveAsyncTask.TASK_TYPE)){
			String notification_content=ArchiveDeletePasteServiceUtil.ON_UNARCHIVE_ASYNCTASK_COMPLETE(context,counter_no_files,filePOJO,dest_folder, destFileObjectType, zip_file_path,true);
			nm.notify(getString(R.string.could_not_extract)+" '"+new File(zip_file_path).getName()+"'",notification_id);
		}
		else if(taskType.equals(DeleteAsyncTask.TASK_TYPE)){
			ArchiveDeletePasteServiceUtil.ON_DELETE_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,sourceFileObjectType,delete_async_task.deleted_file_names,delete_async_task.deleted_files_path_list,true,storage_analyser_delete);
			nm.notify(getString(R.string.could_not_delete_selected_files)+" "+source_folder,notification_id);
		}
		else if(taskType.equals(CutCopyAsyncTask.TASK_TYPE_CUT) || taskType.equals(CutCopyAsyncTask.TASK_TYPE_COPY)){
			String notification_content=ArchiveDeletePasteServiceUtil.ON_CUT_COPY_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,dest_folder,sourceFileObjectType,destFileObjectType,filePOJO, cut,true);
			if(cut)
			{
				nm.notify(R.string.could_not_move_selected_files+" "+dest_folder,notification_id);
			}
			else
			{
				nm.notify(getString(R.string.could_not_copy_selected_files)+" "+dest_folder,notification_id);
			}

		}
		else if(taskType.equals(CopyToAsyncTask.TASK_TYPE)){
			String notification_content=ArchiveDeletePasteServiceUtil.ON_COPY_TO_ASYNCTASK_COMPLETE(context,false,dest_folder,file_name,destFileObjectType,filePOJO);
			if(cut)
			{
				nm.notify(R.string.could_not_move_selected_files+" "+dest_folder,notification_id);
			}
			else
			{
				nm.notify(getString(R.string.could_not_copy_selected_files)+" "+dest_folder,notification_id);
			}
		}

		stopForeground(true);
		stopSelf();

		SERVICE_COMPLETED=true;
		SOURCE_FILE_OBJECT=null;
		DEST_FILE_OBJECT=null;
	}


	interface ServiceCompletionListener
	{
		void onServiceCompletion(String intent_action, boolean service_result, String target, String dest_folder);
	}

	public void setServiceCompletionListener(ServiceCompletionListener listener)
	{
		serviceCompletionListener=listener;
	}
}
