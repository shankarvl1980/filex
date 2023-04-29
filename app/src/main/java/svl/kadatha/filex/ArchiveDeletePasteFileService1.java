package svl.kadatha.filex;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileInputStream;
import me.jahnen.libaums.core.fs.UsbFileOutputStream;

public class ArchiveDeletePasteFileService1 extends Service
{

	String dest_folder,zip_file_path, zip_folder_name,archive_action;
	public static FileObjectType SOURCE_FILE_OBJECT=null,DEST_FILE_OBJECT=null;
	private Context context;
	private final ArrayList<String> files_selected_array=new ArrayList<>();
	private final ArrayList<String> zipentry_selected_array=new ArrayList<>();

	private NotifManager nm;
	private final int notification_id=870;
	public int counter_no_files;
	public long counter_size_files;
	public final long[] total_bytes_read =new long[1];
	String copied_file_name, file_name;
	final List<Integer> total_folderwise_no_of_files=new ArrayList<>();
	final List<Long> total_folderwise_size_of_files=new ArrayList<>();
	public ArchiveDeletePasteServiceUtil.FileCountSize fileCountSize;
	private ArchiveAsyncTask archiveAsyncTask;
	private UnarchiveAsyncTask unarchiveAsyncTask;
	private String tree_uri_path="",source_uri_path="";
	private Uri tree_uri,source_uri;
	private ArchiveDeletePasteBinder binder=new ArchiveDeletePasteBinder();
	private ServiceCompletionListener serviceCompletionListener;
	static boolean SERVICE_COMPLETED=true;
	private String intent_action;

	private DeleteFileAsyncTask delete_file_async_task;

	boolean isFromInternal;
	String current_file_name;

	String size_of_files_format,deleted_file_name;

	String source_folder;
	boolean copy_result, cut;
	private CutCopyAsyncTask cutCopyAsyncTask;
	private CopyToAsyncTask copyToAsyncTask;
	boolean permanent_cancel;

	String size_of_files_copied;
	int it;
	String copied_file;
	private boolean isWritable;
	FileObjectType sourceFileObjectType, destFileObjectType;

	final List<String> copied_files_name=new ArrayList<>();  //declared here instead of at Asynctask class to keep track of copied files in case replacement
	final List<String> copied_source_file_path_list=new ArrayList<>(); //declared here instead of at Asynctask to keep track of copied files in case replacement

	private String source_other_file_permission,dest_other_file_permission;
	private boolean storage_analyser_delete;
	final List<String> overwritten_file_path_list=new ArrayList<>();
	private Uri data;

	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		SERVICE_COMPLETED=false;
		super.onCreate();
		context=this;
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
			case "archive-zip":
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
					fileCountSize=new ArchiveDeletePasteServiceUtil.FileCountSize(context,files_selected_array,source_uri,source_uri_path,sourceFileObjectType);
					fileCountSize.fileCount();
					archiveAsyncTask=new ArchiveAsyncTask();
					archiveAsyncTask.execute(null);//executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					notification_content=getString(R.string.zipping)+" '"+zip_folder_name+".zip "+getString(R.string.at)+" "+dest_folder;

				}
				break;

			case "archive-unzip":
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

					unarchiveAsyncTask=new UnarchiveAsyncTask();
					unarchiveAsyncTask.execute(null);//executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					notification_content=getString(R.string.unzipping)+" '"+zip_file_path+" "+getString(R.string.at)+" "+dest_folder;

				}
				break;

			case "delete":
				if(bundle!=null)
				{

					source_uri_path=bundle.getString("source_uri_path");
					source_uri=bundle.getParcelable("source_uri");
					files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
					sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
					source_folder=bundle.getString("source_folder");
					storage_analyser_delete = bundle.getBoolean("storage_analyser_delete");
					delete_file_async_task=new DeleteFileAsyncTask();
					delete_file_async_task.execute(null);///executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					notification_content=getString(R.string.deleting_files)+" "+getString(R.string.at)+" "+source_folder;

				}
				break;

			case "paste-cut":
			case "paste-copy":
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
					isWritable=bundle.getBoolean("isWritable");
					source_folder=bundle.getString("source_folder");
					fileCountSize=new ArchiveDeletePasteServiceUtil.FileCountSize(context,files_selected_array,source_uri,source_uri_path,sourceFileObjectType);
					fileCountSize.fileCount();
					cutCopyAsyncTask=new CutCopyAsyncTask();
					cutCopyAsyncTask.execute(null);//executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					notification_content=(cut? getString(R.string.moving_files) +" "+getString(R.string.to_symbol)+" "+dest_folder : getString(R.string.copying_files)+" "+getString(R.string.to_symbol)+" "+dest_folder);

				}
				break;

			case "copy_to":
				if(bundle!=null)
				{
					data=bundle.getParcelable("data");
					file_name=bundle.getString("file_name");
					dest_folder=bundle.getString("dest_folder");
					destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
					tree_uri_path=bundle.getString("tree_uri_path");
					tree_uri=bundle.getParcelable("tree_uri");
					counter_no_files ++;
					counter_size_files += getLengthUri(data);
					size_of_files_copied = FileUtil.humanReadableByteCount(counter_size_files);

					fileCountSize=new ArchiveDeletePasteServiceUtil.FileCountSize(1,counter_size_files);
					fileCountSize.fileCount();
					copyToAsyncTask=new CopyToAsyncTask();
					copyToAsyncTask.execute(null);//executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

		startForeground(notification_id,nm.buildADPPActivity1(intent_action,notification_content,notification_id));
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
		public ArchiveDeletePasteFileService1 getService()
		{
			return ArchiveDeletePasteFileService1.this;
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
				case "archive-zip":
					if(fileCountSize!=null && archiveAsyncTask!=null)
					{
						fileCountSize.cancel(true);
						archiveAsyncTask.cancel(true);
					}
					break;
				case "archive-unzip":
					if(unarchiveAsyncTask!=null)
					{
						unarchiveAsyncTask.cancel(true);
					}
					break;
				case "delete":
					if(delete_file_async_task!=null)
					{
						delete_file_async_task.cancel(true);
					}
					break;
				case "paste-cut":
				case "paste-copy":
					if(cutCopyAsyncTask!=null)
					{
						permanent_cancel=true;
						cutCopyAsyncTask.cancel(true);
					}
					break;

				case "copy_to":
					if(copyToAsyncTask!=null)
					{
						permanent_cancel=true;
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

	public class ArchiveAsyncTask extends AlternativeAsyncTask<Void,Void,Boolean>
	{
		final String zip_file_name=zip_folder_name+".zip";
		UsbFile parentUsbFile,zipUsbFile;
		FilePOJO filePOJO;

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			ArchiveDeletePasteServiceUtil.ON_ARCHIVE_ASYNCTASK_CANCEL(context,dest_folder,zip_file_name,destFileObjectType,tree_uri,tree_uri_path,zipUsbFile);
			stopForeground(true);
			stopSelf();
			nm.notify(getString(R.string.could_not_create)+" '"+zip_file_name+"'",notification_id);
			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}

		@Override
		protected Boolean doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			OutputStream outStream=null;
			if(destFileObjectType==FileObjectType.FILE_TYPE)
			{
				if(FileUtil.isWritable(destFileObjectType,dest_folder))
				{
					File f=new File(dest_folder,zip_file_name);
					try {
						outStream=new FileOutputStream(f);
					} catch (FileNotFoundException e) {

					}
				}
				else
				{
					Uri uri= FileUtil.createDocumentUri(context,dest_folder,zip_file_name,false,tree_uri,tree_uri_path);
					if (uri != null)
					{
						try {
							outStream = context.getContentResolver().openOutputStream(uri);
						} catch (FileNotFoundException e) {

						}
					}
				}
			}
			else if (destFileObjectType==FileObjectType.USB_TYPE)
			{
				parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,dest_folder);
				if(parentUsbFile!=null)
				{
					UsbFile targetUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,zip_file_name));
					if(targetUsbFile!=null && targetUsbFile.getLength()==0) FileUtil.deleteUsbFile(targetUsbFile);
					try {
						zipUsbFile=parentUsbFile.createFile(zip_file_name);
						outStream=new UsbFileOutputStream(zipUsbFile);
					} catch (IOException e) {

					}

				}
			}

			if(outStream!=null) {
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
				ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);

				if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
				{
					int size=files_selected_array.size();
					for(int i=0;i<size;++i)
					{
						if(isCancelled())
						{
							return false;
						}
						String path=files_selected_array.get(i);
						List<File> file_array=new ArrayList<>();
						Iterate.populate(new File(path),file_array,false);
						try {
							put_zip_entry_file_type(path,file_array, zipOutputStream);
						} catch (IOException e) {

						}
					}

					try
					{
						filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, Collections.singletonList(zip_file_name),destFileObjectType, Collections.singletonList(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,zip_file_name)));
						return true;
					}


					finally
					{
						try
						{
							zipOutputStream.closeEntry();
							zipOutputStream.close();
						}
						catch (Exception e)
						{
							// ignore exception
						}
					}
				}
				else if(sourceFileObjectType==FileObjectType.USB_TYPE)
				{
					List<UsbFile> file_array=new ArrayList<>();
					int size=files_selected_array.size();
					UsbFile[] f_array=new UsbFile[size];
					for(int i=0;i<size;++i)
					{
						f_array[i]=FileUtil.getUsbFile(MainActivity.usbFileRoot,files_selected_array.get(i));
					}
					Iterate.populate(f_array,file_array,false);

					int lengthParentPath=0;
					try
					{
						if(!zip_file_path.equals(""))
						{
							lengthParentPath=new File(zip_file_path).getParent().length();

						}
						int size1=file_array.size();
						for(int i=0;i<size1;++i)
						{
							if(isCancelled())
							{
								return false;
							}
							UsbFile file=file_array.get(i);
							counter_no_files++;
							counter_size_files+=(!file.isDirectory()) ? file.getLength() : 0;
							//size_of_files_archived=FileUtil.humanReadableByteCount(counter_size_files);
							copied_file_name=file.getName();
							String zip_entry_path;
							if(lengthParentPath==1)
							{
								zip_entry_path=file.getAbsolutePath().substring(lengthParentPath);
							}
							else {
								zip_entry_path=(lengthParentPath!=0) ? file.getAbsolutePath().substring(lengthParentPath+1):file.getAbsolutePath().substring(file.getParent().getAbsolutePath().length()+1);
							}


							ZipEntry zipEntry;

							if(file.isDirectory())
							{
								zipEntry=new ZipEntry(zip_entry_path+File.separator);
								zipOutputStream.putNextEntry(zipEntry);
							}
							else
							{
								zipEntry=new ZipEntry(zip_entry_path);
								zipOutputStream.putNextEntry(zipEntry);
								BufferedInputStream bufferedInputStream=new BufferedInputStream(new UsbFileInputStream(file));
								byte [] b=new byte[8192];
								int bytesread;
								while((bytesread=bufferedInputStream.read(b))!=-1)
								{
									zipOutputStream.write(b,0,bytesread);
									total_bytes_read[0]+=bytesread;

								}
								bufferedInputStream.close();
							}
							//mutable_count_no_files.postValue(counter_no_files);

						}
						filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, Collections.singletonList(zip_file_name),destFileObjectType, Collections.singletonList(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,zip_file_name)));
						return true;
					}

					catch(IOException e)
					{
						//print("Exception thrown");
					}
					finally
					{
						try
						{
							zipOutputStream.closeEntry();
							zipOutputStream.close();
						}
						catch (Exception e)
						{
							// ignore exception
						}
					}
				}

			}

			return false;
		}

		private void put_zip_entry_file_type(String file_path,List<File> file_array, ZipOutputStream zipOutputStream) throws IOException {
			int lengthParentPath = 0;
			if(!file_path.equals(""))
			{
				lengthParentPath=new File(file_path).getParent().length(); //should be calculated for each file separately in library_search

			}
			int size1=file_array.size();
			for(int i=0;i<size1;++i)
			{
				if(isCancelled())
				{
					return;
				}
				File file=file_array.get(i);
				counter_no_files++;
				counter_size_files+=file.length();
				//size_of_files_archived=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file_name=file.getName();
				String zip_entry_path;
				if(lengthParentPath==1)
				{
					zip_entry_path=file.getCanonicalPath().substring(lengthParentPath);
				}
				else {
					zip_entry_path=(lengthParentPath!=0) ? file.getCanonicalPath().substring(lengthParentPath+1):file.getCanonicalPath().substring(file.getParentFile().getCanonicalPath().length()+1);
				}


				ZipEntry zipEntry;

				if(file.isDirectory())
				{
					zipEntry=new ZipEntry(zip_entry_path+File.separator);
					zipOutputStream.putNextEntry(zipEntry);
				}
				else
				{
					zipEntry=new ZipEntry(zip_entry_path);
					zipOutputStream.putNextEntry(zipEntry);
					BufferedInputStream bufferedInputStream=new BufferedInputStream(new FileInputStream(file));
					byte [] b=new byte[8192];
					int bytesread;
					while((bytesread=bufferedInputStream.read(b))!=-1)
					{
						zipOutputStream.write(b,0,bytesread);
						total_bytes_read[0]+=bytesread;

					}
					bufferedInputStream.close();
				}
				//mutable_count_no_files.postValue(counter_no_files);
			}
		}


		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			String notification_content=ArchiveDeletePasteServiceUtil.ON_ARCHIVE_ASYNCTASK_COMPLETE(context,result,filePOJO,dest_folder,zip_file_name,destFileObjectType);
			stopForeground(true);
			stopSelf();


			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,result,zip_file_name,dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}
			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}
	}

	public class UnarchiveAsyncTask extends AlternativeAsyncTask<Void,Void,Boolean>
	{

		String zip_dest_path;
		ZipFile zipfile=null;
		FilePOJO filePOJO;
		final List<String> written_file_name_list=new ArrayList<>();
		final List<String> written_file_path_list=new ArrayList<>();
		final Set<String> first_part_entry_name_set=new HashSet<>();
		final Set<String> first_part_entry_path_set=new HashSet<>();

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			if(zip_folder_name==null)
			{
				zip_dest_path=dest_folder;
			}
			else {
				zip_dest_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,zip_folder_name);
			}

			current_file_name=new File(zip_file_path).getName();

		}

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			String notification_content=ArchiveDeletePasteServiceUtil.ON_UNARCHIVE_ASYNCTASK_COMPLETE(context,counter_no_files,filePOJO,dest_folder, destFileObjectType, zip_file_path,true);
			stopForeground(true);
			stopSelf();

			nm.notify(getString(R.string.could_not_extract)+" '"+new File(zip_file_path).getName()+"'",notification_id);
			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}

		@Override
		protected Boolean doInBackground(Void[] p1)
		{
			boolean success=false, isWritable=false;
			if(destFileObjectType==FileObjectType.FILE_TYPE){
				isWritable=FileUtil.isWritable(destFileObjectType,zip_dest_path);
				if(isWritable)
				{
					if(zip_folder_name!=null)
					{
						success=FileUtil.mkdirNative(new File(zip_dest_path));
						if(!success) return false;
					}

				}
				else {
					if(zip_folder_name!=null)
					{
						success=FileUtil.mkdirSAF(context,dest_folder,zip_folder_name,tree_uri,tree_uri_path);
						if(!success) return false;
					}
				}

				try
				{
					zipfile=new ZipFile(zip_file_path);
				}
				catch (IOException e)
				{
					return unzip(zip_file_path,tree_uri,tree_uri_path,zip_dest_path,isWritable);
				}
			}
			else if(destFileObjectType==FileObjectType.USB_TYPE)
			{
				if(zip_folder_name!=null)
				{
					UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,dest_folder);
					if(parentUsbFile!=null)
					{
						UsbFile targetUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,zip_folder_name));
						if(targetUsbFile==null)
						{
							success=FileUtil.mkdirUsb(parentUsbFile,zip_folder_name);
						}

					}
					else
					{
						return false;
					}

					if(!success) return success;
				}
				return unzip(zip_file_path,tree_uri,tree_uri_path,zip_dest_path,isWritable);
			}


			if(zipentry_selected_array.size()!=0)
			{
				for(String s:zipentry_selected_array)
				{
					if(isCancelled())
					{
						return false;
					}
					ZipEntry zipEntry=zipfile.getEntry(s.substring(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath().length()+1));
					success=read_zipentry(zipEntry,zip_dest_path,isWritable,tree_uri,tree_uri_path);
				}
			}
			else
			{
				Enumeration<? extends ZipEntry> zip_entries=zipfile.entries();
				while(zip_entries.hasMoreElements())
				{
					if(isCancelled())
					{
						return false;
					}
					ZipEntry zipEntry=zip_entries.nextElement();
					success=read_zipentry(zipEntry,zip_dest_path,isWritable,tree_uri,tree_uri_path);
				}

			}
			if(zip_folder_name==null)
			{
				written_file_name_list.addAll(first_part_entry_name_set);
				written_file_path_list.addAll(first_part_entry_path_set);
			}
			else
			{
				written_file_name_list.add(zip_folder_name);
				written_file_path_list.add(zip_dest_path);
			}
			if(counter_no_files>0) filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,written_file_name_list,destFileObjectType,written_file_path_list);
			return success;
		}

		private boolean read_zipentry(ZipEntry zipEntry, String zip_dest_path,boolean isWritable,Uri uri,String uri_path)
		{
			InputStream inStream;
			BufferedInputStream bufferedInputStream=null;
			try
			{
				inStream=zipfile.getInputStream(zipEntry);
				bufferedInputStream=new BufferedInputStream(inStream);
				String zip_entry_name=ArchiveDeletePasteServiceUtil.UNARCHIVE(context,zip_dest_path,zipEntry,isWritable,destFileObjectType,uri,uri_path,bufferedInputStream);
				counter_no_files++;
				counter_size_files+=zipEntry.getSize();
				total_bytes_read[0]=counter_size_files;
				//size_of_files_archived=FileUtil.humanReadableByteCount(counter_size_files);
				//mutable_count_no_files.postValue(counter_no_files);
				copied_file_name=zip_entry_name;

				int idx=zip_entry_name.indexOf(File.separator);
				if(idx!=-1)
				{
					String first_part=zip_entry_name.substring(0,idx);
					first_part_entry_name_set.add(first_part);
					first_part_entry_path_set.add(Global.CONCATENATE_PARENT_CHILD_PATH(zip_dest_path,first_part));
				}
				else
				{
					first_part_entry_name_set.add(zip_entry_name);
					first_part_entry_path_set.add(Global.CONCATENATE_PARENT_CHILD_PATH(zip_folder_name,zip_entry_name));
				}

				return true;
			}
			catch(IOException e)
			{
				return false;
			}

			finally
			{
				try
				{
					if(bufferedInputStream!=null)
					{
						bufferedInputStream.close();
					}
				}
				catch(Exception e)
				{
					return false;
				}
			}

		}

		private boolean unzip(String zip_file_path,Uri uri,String uri_path,String zip_dest_path,boolean isWritable)
		{
			// TODO: Implement this method
			File f;
			BufferedInputStream bufferedInputStream = null;
			ZipInputStream zipInputStream;
			try {
				if((sourceFileObjectType== FileObjectType.FILE_TYPE)||(sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE))
				{
					f=new File(zip_file_path);
					bufferedInputStream=new BufferedInputStream(new FileInputStream(f));
				}
				else if(sourceFileObjectType==FileObjectType.USB_TYPE)
				{
					UsbFile usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,zip_file_path);
					if(usbFile!=null)
					{
						bufferedInputStream=new BufferedInputStream(new UsbFileInputStream(usbFile));
					}
					else
					{
						return false;
					}

				}

			}
			catch(FileNotFoundException e){return false;}

			zipInputStream=new ZipInputStream(bufferedInputStream);

			try
			{
				ZipEntry zipEntry;
				while((zipEntry=zipInputStream.getNextEntry())!=null && !isCancelled())
				{
					String zip_entry_name=ArchiveDeletePasteServiceUtil.UNARCHIVE(context,zip_dest_path,zipEntry,isWritable,destFileObjectType,uri,uri_path,zipInputStream);
					counter_no_files++;
					counter_size_files+=zipEntry.getSize();
					total_bytes_read[0]=counter_size_files;
					//size_of_files_archived=FileUtil.humanReadableByteCount(counter_size_files);
					//mutable_count_no_files.postValue(counter_no_files);
					copied_file_name=zip_entry_name;
					String entry_name=zipEntry.getName();
					int idx=entry_name.indexOf(File.separator);
					if(idx!=-1)
					{
						String first_part=zip_entry_name.substring(0,idx);
						first_part_entry_name_set.add(first_part);
						first_part_entry_path_set.add((Global.CONCATENATE_PARENT_CHILD_PATH(zip_dest_path,first_part)));
					}
					else
					{
						first_part_entry_name_set.add(zip_entry_name);
						first_part_entry_path_set.add(Global.CONCATENATE_PARENT_CHILD_PATH(zip_folder_name,zip_entry_name));
					}

				}
				if(zip_folder_name==null)
				{
					written_file_name_list.addAll(first_part_entry_name_set);
					written_file_path_list.addAll(first_part_entry_path_set);
				}
				else
				{
					written_file_name_list.add(zip_folder_name);
					written_file_path_list.add(zip_dest_path);
				}
				zipInputStream.close();
				bufferedInputStream.close();
				if(counter_no_files>0) filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,written_file_name_list,destFileObjectType,written_file_path_list);
				return true;
			}

			catch(IOException e)
			{
				return false;
			}
			finally
			{
				try
				{
					zipInputStream.close();
				}
				catch(Exception e)
				{

				}
			}
		}


		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			String notification_content=ArchiveDeletePasteServiceUtil.ON_UNARCHIVE_ASYNCTASK_COMPLETE(context,counter_no_files,filePOJO,dest_folder, destFileObjectType, zip_file_path,!result);
			stopForeground(true);
			stopSelf();
			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,counter_no_files>0,new File(zip_file_path).getName(),dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}
			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}
	}

	private class DeleteFileAsyncTask extends AlternativeAsyncTask<Void,Void,Boolean>
	{
		final List<String> deleted_file_names=new ArrayList<>();
		final List<String> deleted_files_path_list=new ArrayList<>();

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			ArchiveDeletePasteServiceUtil.ON_DELETE_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,sourceFileObjectType,deleted_file_names,deleted_files_path_list,true,storage_analyser_delete);
			stopForeground(true);
			stopSelf();

			nm.notify(getString(R.string.could_not_delete_selected_files)+" "+source_folder,notification_id);
			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}

		@Override
		protected Boolean doInBackground(Void...p)
		{
			// TODO: Implement this method
			boolean success;
			if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				success=deleteFromLibrarySearch();
			}
			else
			{
				if(sourceFileObjectType==FileObjectType.FILE_TYPE)
				{
					isFromInternal=FileUtil.isFromInternal(sourceFileObjectType,files_selected_array.get(0));
				}
				success=deleteFromFolder(isFromInternal);
			}
			if(deleted_file_names.size()>0)
			{
				if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
				{
					FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(source_folder,deleted_files_path_list,FileObjectType.FILE_TYPE);
				}
				else
				{
					FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_names,sourceFileObjectType);
				}
			}
			return success;
		}

		private boolean deleteFromLibrarySearch()
		{
			boolean success=false;
			int size=files_selected_array.size();
			for(int i=0;i<size;++i)
			{
				if(isCancelled())
				{
					return false;
				}
				String file_path=files_selected_array.get(i);
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
					deleted_file_names.add(current_file_name);
					deleted_files_path_list.add(file_path);
				}

			}

			return success;
		}


		private boolean deleteFromFolder(boolean isFromInternal)
		{
			boolean success=false;
			int size=files_selected_array.size();
			if(sourceFileObjectType==FileObjectType.FILE_TYPE)
			{
				if(isFromInternal)
				{
					for(int i=0;i<size;++i)
					{
						if(isCancelled())
						{
							return false;
						}

						String file_path=files_selected_array.get(i);
						File f=new File(file_path);

						current_file_name=f.getName();
						success=deleteNativeDirectory(f);
						if(success)
						{
							deleted_file_names.add(current_file_name);
							deleted_files_path_list.add(file_path);
						}

					}
				}
				else
				{
					for(int i=0;i<size;++i)
					{
						if(isCancelled())
						{
							return false;
						}
						String file_path=files_selected_array.get(i);
						File file=new File(file_path);
						current_file_name=file.getName();
						success=deleteSAFDirectory(file);
						if(success)
						{
							deleted_file_names.add(current_file_name);
							deleted_files_path_list.add(file_path);
						}
					}
				}
			}
			else if(sourceFileObjectType==FileObjectType.USB_TYPE)
			{
				if(MainActivity.usbFileRoot==null)return false;
				for(int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return false;
					}

					UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,files_selected_array.get(i));
					if(f==null)return false;
					String file_path=f.getAbsolutePath();
					current_file_name=f.getName();
					success=deleteUsbDirectory(f);
					if(success)
					{
						deleted_file_names.add(current_file_name);
						deleted_files_path_list.add(file_path);

					}

				}
			}
			else if(sourceFileObjectType==FileObjectType.ROOT_TYPE)
			{
				//can be removed all the files simultaneously
				for(int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return false;
					}
					String file_path=files_selected_array.get(i);
					File file=new File(file_path);
					current_file_name=file.getName();
					success=RootUtils.EXECUTE(Arrays.asList("rm",file_path));
					if(success)
					{
						deleted_file_names.add(current_file_name);
						deleted_files_path_list.add(file_path);
					}
				}
			}
			else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
			{
				for(int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return false;
					}
					String file_path=files_selected_array.get(i);
					File file=new File(file_path);
					current_file_name=file.getName();
					success=deleteFtpDirectory(file_path);
					if(success)
					{
						deleted_file_names.add(current_file_name);
						deleted_files_path_list.add(file_path);
					}
				}
			}
			return success;
		}


		public boolean deleteNativeDirectory(final File folder)
		{
			boolean success=true;
			if (folder.isDirectory())            //Check if folder file is a real folder
			{
				if(isCancelled())
				{
					return false;
				}
				File[] list = folder.listFiles(); //Storing all file name within array
				if(list!=null)
				{
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
			}

			counter_no_files++;
			counter_size_files+=folder.length();
			total_bytes_read[0]=counter_size_files;
			size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files);
			//mutable_count_no_files.postValue(counter_no_files);
			deleted_file_name=folder.getName();
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
				if(list!=null)
				{
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

			}

			counter_no_files++;
			counter_size_files+=folder.length();
			total_bytes_read[0]=counter_size_files;
			size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files);
			//mutable_count_no_files.postValue(counter_no_files);
			deleted_file_name=folder.getName();
			success=FileUtil.deleteSAFDirectory(context,folder.getAbsolutePath(),source_uri,source_uri_path);

			return success;
		}

		public boolean deleteUsbDirectory(final UsbFile folder)
		{
			boolean success=true;
			if (folder.isDirectory())            //Check if folder file is a real folder
			{
				if(isCancelled())
				{
					return false;
				}
				UsbFile[] list; //Storing all file name within array
				try {
					list = folder.listFiles();
				} catch (IOException e) {
					return false;
				}
				int size=list.length;
				for (int i = 0; i < size; ++i)
				{
					if(isCancelled())
					{
						return false;
					}
					UsbFile tmpF = list[i];
					success=deleteUsbDirectory(tmpF);

				}

			}

			counter_no_files++;
			counter_size_files+=(!folder.isDirectory()) ? folder.getLength() : 0;
			total_bytes_read[0]=counter_size_files;
			size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files);
			//mutable_count_no_files.postValue(counter_no_files);
			deleted_file_name=folder.getName();
			success=FileUtil.deleteUsbFile(folder);
			return success;
		}

		private boolean deleteFtpDirectory(final String file_path)
		{
			boolean success=true;
			FTPFile folder;
			try {
				if(FileUtil.isFtpPathDirectory(file_path))
				{
					if(isCancelled())
					{
						return false;
					}
					String[] list = MainActivity.FTP_CLIENT.listNames(file_path); //Storing all file name within array
					if(list!=null)
					{
						int size=list.length;
						for (int i = 0; i < size; ++i)
						{
							if(isCancelled())
							{
								return false;
							}
							success=deleteFtpDirectory(list[i]);

						}
					}


				}
				counter_no_files++;
				counter_size_files+=0;//folder.getSizeUri();
				total_bytes_read[0]=counter_size_files;
				size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files);
				//mutable_count_no_files.postValue(counter_no_files);
				deleted_file_name=new File(file_path).getName();
				//if(folder.isDirectoryUri())
				if(FileUtil.isFtpPathDirectory(file_path))
				{
					success=MainActivity.FTP_CLIENT.removeDirectory(file_path);
				}
				else {
					success=MainActivity.FTP_CLIENT.deleteFile(file_path);
				}

			} catch (IOException e) {
				return false;
			}

			return success;
		}


		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			int s=deleted_file_names.size();
			String notification_content=ArchiveDeletePasteServiceUtil.ON_DELETE_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,sourceFileObjectType,deleted_file_names,deleted_files_path_list,!result,storage_analyser_delete);
			stopForeground(true);
			stopSelf();
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
			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}
	}

	public class CutCopyAsyncTask extends AlternativeAsyncTask<Void,Void,Boolean>
	{
		FilePOJO filePOJO;

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			if(permanent_cancel)
			{
				String notification_content=ArchiveDeletePasteServiceUtil.ON_CUT_COPY_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,dest_folder,sourceFileObjectType,destFileObjectType,filePOJO, cut,true);
				stopForeground(true);
				stopSelf();
				if(cut)
				{
					nm.notify(R.string.could_not_move_selected_files+" "+dest_folder,notification_id);
				}
				else
				{
					nm.notify(getString(R.string.could_not_copy_selected_files)+" "+dest_folder,notification_id);
				}
				SERVICE_COMPLETED=true;
				SOURCE_FILE_OBJECT=null;
				DEST_FILE_OBJECT=null;
			}
		}

		@Override
		protected Boolean doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			if(sourceFileObjectType==FileObjectType.ROOT_TYPE || destFileObjectType==FileObjectType.ROOT_TYPE)
			{
				if(destFileObjectType==FileObjectType.USB_TYPE || sourceFileObjectType==FileObjectType.USB_TYPE)
				{
					return false;
				}

			}
			else if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				List<File> src_file_list=new ArrayList<>();

				for(String s: files_selected_array)
				{
					File file=new File(s);
					src_file_list.add(file);
				}

				for (File file : src_file_list) {
					if (isCancelled() || file == null) {
						return false;
					}
					current_file_name = file.getName();
					String dest_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, current_file_name);
					boolean isSourceFromInternal = FileUtil.isFromInternal(sourceFileObjectType, file.getAbsolutePath());
					if (isWritable) {
						if (isSourceFromInternal) {
							if (it < total_folderwise_size_of_files.size()) {
								counter_no_files += total_folderwise_no_of_files.get(it);
								counter_size_files += total_folderwise_size_of_files.get(it);
								size_of_files_copied = FileUtil.humanReadableByteCount(counter_size_files);

							}
							copied_file = file.getName();
							copy_result = Copy_File_File(file, dest_file_path, cut);

						} else // that is cut and paste  from external directory
						{
							copy_result = Copy_File_File(file, dest_file_path, false);
							if (copy_result && cut) {
								FileUtil.deleteSAFDirectory(context, file.getAbsolutePath(), source_uri, source_uri_path);
							}
						}
						//mutable_count_no_files.postValue(counter_no_files);
					} else {
						if (isSourceFromInternal) {
							if (destFileObjectType == FileObjectType.FILE_TYPE) {
								copy_result = Copy_File_SAFFile(context, file, dest_folder, file.getName(), tree_uri, tree_uri_path, cut);
							} else if (destFileObjectType == FileObjectType.USB_TYPE) {
								copy_result = Copy_File_UsbFile(file, dest_folder, file.getName(), cut);
							} else if (destFileObjectType == FileObjectType.FTP_TYPE) {
								copy_result = Copy_File_FtpFile(file, dest_folder, file.getName(), cut);
							}

						} else {
							if (destFileObjectType == FileObjectType.FILE_TYPE) {
								copy_result = Copy_File_SAFFile(context, file, dest_folder, file.getName(), tree_uri, tree_uri_path, false);  //in case of cut in saf not cutting here, cutting separately down
							} else if (destFileObjectType == FileObjectType.USB_TYPE) {
								copy_result = Copy_File_UsbFile(file, dest_folder, file.getName(), false);
							} else if (destFileObjectType == FileObjectType.FTP_TYPE) {
								copy_result = Copy_File_FtpFile(file, dest_folder, file.getName(), false);
							}

							if (copy_result && cut) {
								FileUtil.deleteSAFDirectory(context, file.getAbsolutePath(), source_uri, source_uri_path);
							}
						}

					}
					String f_p = file.getAbsolutePath();
					if (copy_result) {
						copied_files_name.add(file.getName());
						copied_source_file_path_list.add(f_p);

					}

					files_selected_array.remove(f_p);
					it++;
				}
			}
			else if(sourceFileObjectType ==FileObjectType.USB_TYPE)
			{
				List<String> src_file_path_list = new ArrayList<>(files_selected_array);

				for (String src_file_path : src_file_path_list) {
					if (isCancelled() || src_file_path == null) {
						return false;
					}

					String src_file_name = new File(src_file_path).getName();
					current_file_name = src_file_name;
					UsbFile src_usbfile = FileUtil.getUsbFile(MainActivity.usbFileRoot, src_file_path);
					if (src_usbfile == null) {
						return false;
					}

					if (destFileObjectType == FileObjectType.FILE_TYPE) {
						if (isWritable) {
							copy_result = Copy_UsbFile_File(src_usbfile, dest_folder, src_file_name, cut);

						} else {
							copy_result = Copy_UsbFile_SAFFile(context, src_usbfile, dest_folder, src_file_name, tree_uri, tree_uri_path, cut);
						}
					} else if (destFileObjectType == FileObjectType.USB_TYPE) {
						copy_result = Copy_UsbFile_UsbFile(src_usbfile, dest_folder, src_file_name, cut);
					}

					if (copy_result) {
						copied_files_name.add(new File(src_file_path).getName());
						copied_source_file_path_list.add(src_file_path);
					}

					files_selected_array.remove(src_file_path);
					it++;
				}
			}
			else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
			{
				List<String> src_file_path_list = new ArrayList<>(files_selected_array);

				for (String src_file_path : src_file_path_list) {
					if (isCancelled() || src_file_path == null) {
						return false;
					}

					String src_file_name = new File(src_file_path).getName();
					current_file_name = src_file_name;

					if (destFileObjectType == FileObjectType.FILE_TYPE) {
						if (isWritable) {
							copy_result = Copy_FtpFile_File(src_file_path, dest_folder, src_file_name, cut);

						} else {
							copy_result = Copy_FtpFile_SAFFile(context, src_file_path, dest_folder, src_file_name, tree_uri, tree_uri_path, cut);
						}
					}
					else if(destFileObjectType==FileObjectType.FTP_TYPE)
					{
						copy_result=Copy_FtpFile_FtpFile(src_file_path,dest_folder,src_file_name,cut);
					}
							/*
							else if(destFileObjectType==FileObjectType.USB_TYPE)
							{
								copy_result=Copy_UsbFile_UsbFile(src_usbfile,dest_folder,src_file_name,cut);
							}

							 */



					if (copy_result) {
						copied_files_name.add(new File(src_file_path).getName());
						copied_source_file_path_list.add(src_file_path);
					}

					files_selected_array.remove(src_file_path);
					it++;

				}
			}
			if(counter_no_files>0)
			{
				filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,copied_files_name,destFileObjectType,overwritten_file_path_list);
				if(cut)
				{
					if(sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
					{
						FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(source_folder,copied_source_file_path_list,FileObjectType.FILE_TYPE);
					}
					else
					{
						FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,copied_files_name,sourceFileObjectType);
					}

				}
				copied_files_name.clear();
				copied_source_file_path_list.clear();
			}
			return copy_result;
		}

		@SuppressWarnings("null")
		public boolean Copy_File_File(File source, String dest_file_path,boolean cut)
		{
			boolean success=false;
			File destination=new File(dest_file_path);
			if (source.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}
				if(!destination.exists())// || !destination.isDirectoryUri())
				{
					if(!(success=FileUtil.mkdirsNative(destination)))
					{
						return false;
					}
				}
				else {
					if(destination.isDirectory()) success=true;   //make success true as destination dir existsUri to execute cut directory
				}

				String[] files_name_array = source.list();
				if(files_name_array==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}

				int size=files_name_array.length;
				for (int i=0;i<size;++i)
				{
					String inner_file_name=files_name_array[i];
					if(isCancelled())
					{
						return false;
					}
					File srcFile = new File(source, inner_file_name);
					String inner_dest_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,inner_file_name);
					success=Copy_File_File(srcFile,inner_dest_file_path,cut);
				}
				++counter_no_files;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteNativeDirectory(source);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=source.length();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=source.getName();
				success=FileUtil.copy_File_File(source,destination,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}


		@SuppressWarnings("null")
		public boolean Copy_File_SAFFile(Context context, File source, String dest_file_path,String name,Uri uri,String uri_path,boolean cut)
		{
			boolean success=false;

			if (source.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}

				if(destFileObjectType==FileObjectType.FILE_TYPE)
				{
					File destination=new File(dest_file_path,name);
					if(!destination.exists())// || !destination.isDirectoryUri())
					{
						if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
						{
							return false;
						}
					}
					else {
						if(destination.isDirectory()) success=true;
					}

				}
				/*
				//for other SAF
				else
				{
					Uri dest_uri=FileUtil.getDocumentUri(context,Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name),uri,uri_path);
					if(!FileUtil.existsUri(context,dest_uri) || !FileUtil.isDirectoryUri(context,dest_uri))
					{
						if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
						{
							return false;
						}
					}

				}

				 */


				String[] files_name_list = source.list();
				if(files_name_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}
				int size=files_name_list.length;
				for (int i=0;i<size;++i)
				{
					String inner_file_name=files_name_list[i];
					if(isCancelled())
					{
						return false;
					}
					File srcFile = new File(source, inner_file_name);
					String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
					success=Copy_File_SAFFile(context,srcFile,inner_dest_file,inner_file_name,uri,uri_path,cut);
				}
				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteNativeDirectory(source);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=source.length();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=source.getName();
				success=FileUtil.copy_File_SAFFile(context,source,dest_file_path,name,uri,uri_path,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}

		public boolean Copy_File_UsbFile(File source, String dest_file_path,String name,boolean cut)
		{
			boolean success=false;

			if (source.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}

				String file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
				UsbFile dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, file_path);
				if(dest_usbFile==null) // || !dest_usbFile.isDirectoryUri())
				{
					dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, dest_file_path);
					if(!(success=FileUtil.mkdirUsb(dest_usbFile,name)))
					{
						return false;
					}
				}
				else {
					if(dest_usbFile.isDirectory()) success=true;
				}


				String[] files_name_list = source.list();
				if(files_name_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}
				int size=files_name_list.length;
				for (int i=0;i<size;++i)
				{
					String inner_file_name=files_name_list[i];
					if(isCancelled())
					{
						return false;
					}
					File srcFile = new File(source, inner_file_name);
					success=Copy_File_UsbFile(srcFile, file_path,inner_file_name,cut);
				}
				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteNativeDirectory(source);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=source.length();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=source.getName();
				success=FileUtil.copy_File_UsbFile(source,dest_file_path,name,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}

		public boolean Copy_File_FtpFile(File source, String dest_file_path,String name,boolean cut)
		{
			boolean success=false;

			if (source.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}

				String file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
				if(!(success=FileUtil.mkdirFtp(file_path)))
				{
					return false;
				}

				String[] files_name_list = source.list();
				if(files_name_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}
				int size=files_name_list.length;
				for (int i=0;i<size;++i)
				{
					String inner_file_name=files_name_list[i];
					if(isCancelled())
					{
						return false;
					}
					File srcFile = new File(source, inner_file_name);
					success=Copy_File_FtpFile(srcFile, file_path,inner_file_name,cut);
				}
				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteNativeDirectory(source);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=source.length();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=source.getName();
				success=FileUtil.copy_File_FtpFile(source,dest_file_path,name,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}


		@SuppressWarnings("null")

		public boolean Copy_UsbFile_UsbFile(UsbFile src_usbfile, String dest_file_path, String name,boolean cut)
		{
			boolean success=false;
			if (src_usbfile.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}

				String file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
				UsbFile dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, file_path);
				if(dest_usbFile==null)// || !dest_usbFile.isDirectoryUri())
				{
					dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, dest_file_path);
					if(!(success=FileUtil.mkdirUsb(dest_usbFile,name)))
					{
						return false;
					}
				}
				else {
					if(dest_usbFile.isDirectory()) success=true;
				}


				UsbFile[] inner_source_list;
				try {
					inner_source_list = src_usbfile.listFiles();
				} catch (IOException e) {
					return false;
				}

				if(inner_source_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}

				int size=inner_source_list.length;
				for (int i=0;i<size;++i)
				{
					UsbFile inner_usbfile=inner_source_list[i];
					if(isCancelled())
					{
						return false;
					}
					success=Copy_UsbFile_UsbFile(inner_usbfile, file_path, inner_usbfile.getName(),cut);
				}
				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteUsbDirectory(src_usbfile);
				}
			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=src_usbfile.getLength();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=new File(src_usbfile.getAbsolutePath()).getName();
				success=FileUtil.copy_UsbFile_UsbFile(src_usbfile,dest_file_path,name,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}
			return success;
		}


		@SuppressWarnings("null")
		public boolean Copy_UsbFile_File(UsbFile src_usbfile, String parent_file_path, String name,boolean cut)
		{
			boolean success=false;
			File destination=new File(parent_file_path,name);
			if (src_usbfile.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}
				if(!destination.exists()) // || !destination.isDirectoryUri())
				{
					if(!(success=FileUtil.mkdirsNative(destination)))
					{
						return false;
					}
				}
				else {
					if(destination.isDirectory()) success=true;
				}


				UsbFile[] inner_source_list;
				try {
					inner_source_list = src_usbfile.listFiles();
				} catch (IOException e) {
					return false;
				}
				if(inner_source_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}

				int size=inner_source_list.length;
				for (int i=0;i<size;++i)
				{
					UsbFile inner_usbfile=inner_source_list[i];
					if(isCancelled())
					{
						return false;
					}

					String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,name);
					success=Copy_UsbFile_File(inner_usbfile,inner_dest_file, inner_usbfile.getName(),cut);
				}
				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteUsbDirectory(src_usbfile);
				}
			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=src_usbfile.getLength();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=new File(src_usbfile.getAbsolutePath()).getName();
				success=FileUtil.copy_UsbFile_File(src_usbfile,destination,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}
			return success;
		}


		@SuppressWarnings("null")
		public boolean Copy_UsbFile_SAFFile(Context context, UsbFile source, String dest_file_path,String name,Uri uri,String uri_path,boolean cut)
		{
			boolean success=false;

			if (source.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}

				if(destFileObjectType==FileObjectType.FILE_TYPE)
				{
					File destination=new File(dest_file_path,name);
					if(!destination.exists()) // || !destination.isDirectoryUri())
					{
						if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
						{
							return false;
						}
					}
					else {
						if(destination.isDirectory()) success=true;
					}

				}
				/*
				//for other SAF
				else
				{
					Uri dest_uri=FileUtil.getDocumentUri(context,Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name),uri,uri_path);
					if(!FileUtil.existsUri(context,dest_uri) || !FileUtil.isDirectoryUri(context,dest_uri))
					{
						if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
						{
							return false;
						}
					}

				}

				 */


				UsbFile[] files_name_list;
				try {
					files_name_list = source.listFiles();
				} catch (IOException e) {
					return  false;
				}

				if(files_name_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}
				int size=files_name_list.length;
				for (int i=0;i<size;++i)
				{
					UsbFile inner_usbfile=files_name_list[i];
					if(isCancelled())
					{
						return false;
					}

					String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
					success=Copy_UsbFile_SAFFile(context,inner_usbfile,inner_dest_file,inner_usbfile.getName(),uri,uri_path,cut);
				}
				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteUsbDirectory(source);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=source.getLength();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=new File(source.getAbsolutePath()).getName();
				success=FileUtil.copy_UsbFile_SAFFile(context,source,dest_file_path,name,uri,uri_path,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}


		@SuppressWarnings("null")
		public boolean Copy_FtpFile_File(String src_file_path, String parent_file_path, String name,boolean cut)
		{
			boolean success=false;
			File destination=new File(parent_file_path,name);
			if(FileUtil.isFtpPathDirectory(src_file_path))
			{
				if(isCancelled())
				{
					return false;
				}
				if(!destination.exists()) // || !destination.isDirectoryUri())
				{
					if(!(success=FileUtil.mkdirsNative(destination)))
					{
						return false;
					}
				}
				else {
					if(destination.isDirectory()) success=true;
				}


				String[] inner_source_list;
				try {
					inner_source_list = MainActivity.FTP_CLIENT.listNames(src_file_path);
				} catch (IOException e) {
					return false;
				}

				if(inner_source_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}

				int size=inner_source_list.length;
				for (int i=0;i<size;++i)
				{
					String inner_source_file=inner_source_list[i];
					String inner_ftpfile_name=new File(inner_source_file).getName();
					if(isCancelled())
					{
						return false;
					}

					String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,name);
					//String inner_source_file=Global.CONCATENATE_PARENT_CHILD_PATH(src_file_path,inner_ftpfile_name);
					success=Copy_FtpFile_File(inner_source_file,inner_dest_file, inner_ftpfile_name,cut);
				}
				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteFtpDirectory(src_file_path);
				}
			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=0;//src_ftpfile.getSizeUri();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=new File(src_file_path).getName();
				success=FileUtil.copy_FtpFile_File(src_file_path,destination,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}


		@SuppressWarnings("null")
		public boolean Copy_FtpFile_SAFFile(Context context, String src_file_path, String dest_file_path,String name,Uri uri,String uri_path,boolean cut)
		{
			boolean success=false;
			if(FileUtil.isFtpPathDirectory(src_file_path))
			{
				if(isCancelled())
				{
					return false;
				}

				if(destFileObjectType==FileObjectType.FILE_TYPE)
				{
					File destination=new File(dest_file_path,name);
					if(!destination.exists()) // || !destination.isDirectoryUri())
					{
						if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
						{
							return false;
						}
					}
					else {
						if(destination.isDirectory()) success=true;
					}

				}

				String[] inner_source_list;
				try {
					inner_source_list = MainActivity.FTP_CLIENT.listNames(src_file_path);
				} catch (IOException e) {
					return false;
				}

				if(inner_source_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}

				int size=inner_source_list.length;
				for (int i=0;i<size;++i)
				{
					String inner_source_file=inner_source_list[i];
					String inner_ftpfile_name=new File(inner_source_file).getName();
					if(isCancelled())
					{
						return false;
					}

					String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
					//String inner_source_file=Global.CONCATENATE_PARENT_CHILD_PATH(src_file_path,inner_ftpfile_name);
					success=Copy_FtpFile_SAFFile(context,inner_source_file,inner_dest_file,inner_ftpfile_name,uri,uri_path,cut);
				}

				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteFtpDirectory(src_file_path);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=0;//src_ftpfile.getSizeUri();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=new File(src_file_path).getName();
				success=FileUtil.copy_FtpFile_SAFFile(context,src_file_path,dest_file_path,name,uri,uri_path,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}

		@SuppressWarnings("null")
		public boolean Copy_FtpFile_FtpFile(String src_file_path, String dest_file_path,String name,boolean cut)
		{
			boolean success=false;
			if(FileUtil.isFtpPathDirectory(src_file_path))
			{
				if(isCancelled())
				{
					return false;
				}

				String file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
				if(!(success=FileUtil.mkdirFtp(file_path)))
				{
					return false;
				}

				String[] inner_source_list;
				try {
					inner_source_list = MainActivity.FTP_CLIENT.listNames(src_file_path);
				} catch (IOException e) {
					return false;
				}

				if(inner_source_list==null)
				{
					++counter_no_files;
					//mutable_count_no_files.postValue(counter_no_files);
					return true;
				}


				int size=inner_source_list.length;
				for (int i=0;i<size;++i)
				{
					String inner_source_file=inner_source_list[i];
					String inner_ftpfile_name=new File(inner_source_file).getName();
					if(isCancelled())
					{
						return false;
					}

					//String inner_source_file=Global.CONCATENATE_PARENT_CHILD_PATH(src_file_path,inner_ftpfile_name);
					success=Copy_FtpFile_FtpFile(inner_source_file,file_path,inner_ftpfile_name,cut);
				}

				counter_no_files++;
				//mutable_count_no_files.postValue(counter_no_files);
				if(success&&cut)
				{
					FileUtil.deleteFtpDirectory(src_file_path);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=0;//src_ftpfile.getSizeUri();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=new File(src_file_path).getName();
				success=FileUtil.copy_FtpFile_FtpFile(src_file_path,dest_file_path,cut,total_bytes_read);
				//mutable_count_no_files.postValue(counter_no_files);
			}

			return success;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			String notification_content=ArchiveDeletePasteServiceUtil.ON_CUT_COPY_ASYNCTASK_COMPLETE(context,counter_no_files,source_folder,dest_folder,sourceFileObjectType,destFileObjectType,filePOJO, cut,!result);
			stopForeground(true);
			stopSelf();

			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,counter_no_files>0,null,dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}

			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}
	}


	public class CopyToAsyncTask extends AlternativeAsyncTask<Void,Void,Boolean>
	{
		FilePOJO filePOJO;

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			if(permanent_cancel)
			{
				String notification_content=ArchiveDeletePasteServiceUtil.ON_COPY_TO_ASYNCTASK_COMPLETE(context,result,dest_folder,file_name,destFileObjectType,filePOJO);
				stopForeground(true);
				stopSelf();
				if(cut)
				{
					nm.notify(R.string.could_not_move_selected_files+" "+dest_folder,notification_id);
				}
				else
				{
					nm.notify(getString(R.string.could_not_copy_selected_files)+" "+dest_folder,notification_id);
				}
				SERVICE_COMPLETED=true;
				SOURCE_FILE_OBJECT=null;
				DEST_FILE_OBJECT=null;
			}
		}

		@Override
		protected Boolean doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			if(destFileObjectType==FileObjectType.ROOT_TYPE)
			{
				return false;
			}
			if (isCancelled() || data == null) {
				return false;
			}

			String dest_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, file_name);
			boolean isDestFromInternal = FileUtil.isFromInternal(destFileObjectType, dest_file_path);

			if (destFileObjectType == FileObjectType.FILE_TYPE) {
				if(isDestFromInternal)
				{
					copy_result = Copy_to_File(data, dest_folder, file_name);
				}
				else {
					copy_result = Copy_to_SAFFile(context, data, dest_folder, file_name, tree_uri, tree_uri_path);
				}

			} else if (destFileObjectType == FileObjectType.USB_TYPE) {
				copy_result = Copy_to_UsbFile(data, dest_folder,file_name);
			} else if (destFileObjectType == FileObjectType.FTP_TYPE) {
				copy_result = Copy_to_FtpFile(data, dest_folder, file_name);
			}


			if (copy_result) {
				copied_files_name.add(file_name);
				copied_source_file_path_list.add(dest_file_path);

				filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,copied_files_name,destFileObjectType, Collections.singletonList(dest_file_path));

				copied_files_name.clear();
				copied_source_file_path_list.clear();

			}


			return copy_result;
		}


		@SuppressWarnings("null")
		public boolean Copy_to_File(Uri data, String dest_file_path, String file_name)
		{
			boolean success=false;
			File destination=new File(dest_file_path,file_name);
			if(isCancelled())
			{
				return false;
			}

			copied_file=file_name;
			success=FileUtil.copy_to_File(context,data,destination, total_bytes_read);
			//mutable_count_no_files.postValue(counter_no_files);
			return success;
		}


		@SuppressWarnings("null")
		public boolean Copy_to_SAFFile(Context context, Uri data, String dest_file_path,String name,Uri uri,String uri_path)
		{
			boolean success=false;

			if(isCancelled())
			{
				return false;
			}
			copied_file=file_name;
			success=FileUtil.copy_to_SAFFile(context,data,dest_file_path,name,uri,uri_path, total_bytes_read);
			return success;
		}

		public boolean Copy_to_UsbFile(Uri data, String dest_file_path,String name)
		{
			boolean success=false;
			if(isCancelled())
			{
				return false;
			}
			copied_file=file_name;
			success=FileUtil.copy_to_UsbFile(context,data,dest_file_path,name, total_bytes_read);
			//mutable_count_no_files.postValue(counter_no_files);
			return success;
		}

		public boolean Copy_to_FtpFile(Uri data, String dest_file_path,String name)
		{
			boolean success=false;

			if(isCancelled())
			{
				return false;
			}
			copied_file=file_name;
			success=FileUtil.copy_to_FtpFile(context, data,dest_file_path,name,total_bytes_read);
			//mutable_count_no_files.postValue(counter_no_files);
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			String notification_content=ArchiveDeletePasteServiceUtil.ON_COPY_TO_ASYNCTASK_COMPLETE(context,result,dest_folder,file_name,destFileObjectType,filePOJO);
			stopForeground(true);
			stopSelf();

			if(serviceCompletionListener!=null)
			{
				serviceCompletionListener.onServiceCompletion(intent_action,counter_no_files>0,null,dest_folder);
			}
			else {
				nm.notify(notification_content,notification_id);
			}

			SERVICE_COMPLETED=true;
			SOURCE_FILE_OBJECT=null;
			DEST_FILE_OBJECT=null;
		}
	}

	private long getLengthUri(Uri uri)
	{
		long fileSize = 0;
		try (AssetFileDescriptor fileDescriptor = getApplicationContext().getContentResolver().openAssetFileDescriptor(uri, "r")) {
			fileSize = fileDescriptor.getLength();
		} catch (IOException e) {

		}
		finally {
			return fileSize;
		}
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
