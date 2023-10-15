package svl.kadatha.filex;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;

public class ArchiveDeletePasteFileService2 extends Service
{

	String dest_folder,zip_file_path, zip_folder_name,archive_action;
	public static FileObjectType SOURCE_FILE_OBJECT=null,DEST_FILE_OBJECT=null;
	private Context context;
	private final ArrayList<String> files_selected_array=new ArrayList<>();
	private final ArrayList<String> zipentry_selected_array=new ArrayList<>();

	private NotifManager nm;
	private final int notification_id=871;
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
	String current_file_name;

	String size_of_files_format,deleted_file_name;

	String source_folder;
	boolean copy_result, cut;
	private CutCopyAsyncTask cutCopyAsyncTask;
	private CopyToAsyncTask copyToAsyncTask;
	boolean permanent_cancel;

	String size_of_files_copied;
	String copied_file;
	FileObjectType sourceFileObjectType, destFileObjectType;

	final List<String> copied_files_name=new ArrayList<>();  //declared here instead of at Asynctask class to keep track of copied files in case replacement
	final List<String> copied_source_file_path_list=new ArrayList<>(); //declared here instead of at Asynctask to keep track of copied files in case replacement

	private String source_other_file_permission,dest_other_file_permission;
	private boolean storage_analyser_delete;
	final List<String> overwritten_file_path_list=new ArrayList<>();
	private Uri data;

	private FileModel[] sourceFileModels;
	private FileModel destFileModel;

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
					archiveAsyncTask.execute(null);
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
					unarchiveAsyncTask.execute(null);
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
					fileCountSize=new ArchiveDeletePasteServiceUtil.FileCountSize(context,files_selected_array,source_uri,source_uri_path,sourceFileObjectType);
					fileCountSize.fileCount();
					delete_file_async_task=new DeleteFileAsyncTask();
					delete_file_async_task.execute(null);
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
					boolean isWritable = bundle.getBoolean("isWritable");
					source_folder=bundle.getString("source_folder");
					fileCountSize=new ArchiveDeletePasteServiceUtil.FileCountSize(context,files_selected_array,source_uri,source_uri_path,sourceFileObjectType);
					fileCountSize.fileCount();
					cutCopyAsyncTask=new CutCopyAsyncTask();
					cutCopyAsyncTask.execute(null);
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
		UsbFile zipUsbFile;
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
			OutputStream outStream;
			destFileModel=FileModelFactory.getFileModel(dest_folder,destFileObjectType,tree_uri,tree_uri_path);
			outStream=destFileModel.getChildOutputStream(zip_file_name,0);

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
					catch (Exception e){}


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
					List<FileModel> fileModels=new ArrayList<>();
					sourceFileModels=FileModelFactory.getFileModelArray(files_selected_array,sourceFileObjectType,source_uri,source_uri_path);
					Iterate.populate(sourceFileModels,fileModels,false);

					int lengthParentPath=0;
					try
					{
						if(!zip_file_path.equals(""))
						{
							lengthParentPath=new File(zip_file_path).getParent().length();

						}
						int size1=fileModels.size();
						for(int i=0;i<size1;++i)
						{
							if(isCancelled())
							{
								return false;
							}
							FileModel fileModel=fileModels.get(i);
							counter_no_files++;
							counter_size_files+=(!fileModel.isDirectory()) ? fileModel.getLength() : 0;
							copied_file_name=fileModel.getName();
							String zip_entry_path;
							if(lengthParentPath==1)
							{
								zip_entry_path=fileModel.getPath().substring(lengthParentPath);
							}
							else {
								zip_entry_path=(lengthParentPath!=0) ? fileModel.getPath().substring(lengthParentPath+1):fileModel.getPath().substring(fileModel.getParentPath().length()+1);
							}


							ZipEntry zipEntry;

							if(fileModel.isDirectory())
							{
								zipEntry=new ZipEntry(zip_entry_path+File.separator);
								zipOutputStream.putNextEntry(zipEntry);
							}
							else
							{
								zipEntry=new ZipEntry(zip_entry_path);
								zipOutputStream.putNextEntry(zipEntry);
								BufferedInputStream bufferedInputStream=new BufferedInputStream(fileModel.getInputStream());
								byte [] b=new byte[8192];
								int bytesread;
								while((bytesread=bufferedInputStream.read(b))!=-1)
								{
									zipOutputStream.write(b,0,bytesread);
									total_bytes_read[0]+=bytesread;

								}

								bufferedInputStream.close();
							}

						}

						filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, Collections.singletonList(zip_file_name),destFileObjectType, Collections.singletonList(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,zip_file_name)));
						return true;
					}

					catch(Exception e)
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
				else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
				{
					Global.print_background_thread(context,getString(R.string.not_supported));
					try
					{
						zipOutputStream.closeEntry();
						zipOutputStream.close();
					}
					catch (Exception e)
					{
						// ignore exception
					}
					return false;
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
			destFileModel=FileModelFactory.getFileModel(dest_folder,destFileObjectType,tree_uri,tree_uri_path);
			if(zip_folder_name!=null)
			{
				if(!destFileModel.makeDirIfNotExists(zip_folder_name)) return  false;
			}

			if(destFileObjectType==FileObjectType.FILE_TYPE)
			{
				try
				{
					zipfile=new ZipFile(zip_file_path);
				}
				catch (IOException e)
				{
					return unzip(zip_file_path,tree_uri,tree_uri_path,zip_dest_path,isWritable);
				}
			}
			else if(destFileObjectType==FileObjectType.USB_TYPE || destFileObjectType==FileObjectType.FTP_TYPE){
				return unzip(zip_file_path,tree_uri,tree_uri_path,zip_dest_path,isWritable);
			}
			else {
				return false;
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
				String zip_entry_name=ArchiveDeletePasteServiceUtil.UNARCHIVE(zip_dest_path,zipEntry,destFileObjectType,uri,uri_path,bufferedInputStream);
				counter_no_files++;
				counter_size_files+=zipEntry.getSize();
				total_bytes_read[0]=counter_size_files;
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
			BufferedInputStream bufferedInputStream;
			ZipInputStream zipInputStream;
			FileModel destZipFileModel=FileModelFactory.getFileModel(zip_file_path,destFileObjectType,uri, uri_path);
			InputStream inputStream=destZipFileModel.getInputStream();
			if(inputStream==null)return false;
			bufferedInputStream=new BufferedInputStream(inputStream);
			zipInputStream=new ZipInputStream(bufferedInputStream);

			try
			{
				ZipEntry zipEntry;
				while((zipEntry=zipInputStream.getNextEntry())!=null && !isCancelled())
				{
					String zip_entry_name=ArchiveDeletePasteServiceUtil.UNARCHIVE(zip_dest_path,zipEntry,destFileObjectType,uri,uri_path,zipInputStream);
					counter_no_files++;
					counter_size_files+=zipEntry.getSize();
					total_bytes_read[0]=counter_size_files;
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
			sourceFileModels= FileModelFactory.getFileModelArray(files_selected_array,sourceFileObjectType,source_uri,source_uri_path);
			success=deleteFileModelArray();

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

		private boolean deleteFileModelArray()
		{
			boolean success=false;
			int size=sourceFileModels.length;
			for(int i=0;i<size;++i)
			{
				if(isCancelled())
				{
					return false;
				}
				FileModel fileModel=sourceFileModels[i];
				String file_path=fileModel.getPath();
				current_file_name=fileModel.getName();
				success=deleteFileModel(fileModel);


				if(success)
				{
					deleted_file_names.add(current_file_name);
					deleted_files_path_list.add(file_path);
				}
			}

			return success;
		}


		public boolean deleteFileModel(final FileModel fileModel)
		{
			boolean success;
			if (fileModel.isDirectory())            //Check if folder file is a real folder
			{
				if(isCancelled())
				{
					return false;
				}
				FileModel[] list = fileModel.list(); //Storing all file name within array
				if(list!=null)
				{
					int size=list.length;
					for (int i = 0; i < size; ++i)
					{
						if(isCancelled())
						{
							return false;
						}
						FileModel tmpF = list[i];
						success=deleteFileModel(tmpF);

					}
				}
			}

			counter_no_files++;
			counter_size_files+=(!fileModel.isDirectory()) ? fileModel.getLength() : 0;
			total_bytes_read[0]=counter_size_files;
			size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files);
			deleted_file_name=fileModel.getName();
			success=fileModel.delete();
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
			else {

				sourceFileModels = FileModelFactory.getFileModelArray(files_selected_array, sourceFileObjectType, tree_uri, tree_uri_path);
				destFileModel = FileModelFactory.getFileModel(dest_folder, destFileObjectType, tree_uri, tree_uri_path);

				int size = sourceFileModels.length;
				for (int i = 0; i < size; ++i) {
					if (isCancelled()) return false;
					FileModel sourceFileModel = sourceFileModels[i];
					String file_path = sourceFileModel.getPath();
					current_file_name = sourceFileModel.getName();
					boolean isSourceFromInternal = FileUtil.isFromInternal(sourceFileObjectType, file_path);
					if (sourceFileObjectType == FileObjectType.FILE_TYPE) {
						if (isSourceFromInternal) {
							copied_file = copied_file_name;
							copy_result = CopyFileModel(sourceFileModel, destFileModel, current_file_name, cut);

						} else // that is cut and paste  from external directory
						{
							copy_result = CopyFileModel(sourceFileModel, destFileModel, current_file_name, false);
							if (copy_result && cut) {
								sourceFileModel.delete();
							}
						}
					} else {
						copy_result = CopyFileModel(sourceFileModel, destFileModel, current_file_name, false);
						if (copy_result && cut) {
							sourceFileModel.delete();
						}
					}


					if (copy_result) {
						copied_files_name.add(current_file_name);
						copied_source_file_path_list.add(file_path);
					}

					files_selected_array.remove(file_path);
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
		public boolean CopyFileModel(FileModel sourceFileModel, FileModel destFileModel,String current_file_name,boolean cut)
		{
			boolean success;
			if (sourceFileModel.isDirectory())
			{
				if(isCancelled())
				{
					return false;
				}
				String dest_path=Global.CONCATENATE_PARENT_CHILD_PATH(destFileModel.getPath(),current_file_name);
				FileModel childDestFileModel;
				if(destFileModel.makeDirIfNotExists(current_file_name)){
					success=true;
					childDestFileModel=FileModelFactory.getFileModel(dest_path,destFileObjectType,tree_uri,tree_uri_path);
				}
				else {
					return false;
				}


				FileModel[] sourceChildFileModels = sourceFileModel.list();
				if(sourceChildFileModels==null)
				{
					++counter_no_files;
					return true;
				}

				int size=sourceChildFileModels.length;
				for (int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return false;
					}
					String inner_file_name=sourceChildFileModels[i].getName();
					success=CopyFileModel(sourceChildFileModels[i],childDestFileModel,inner_file_name,cut);
				}
				++counter_no_files;
				if(success&&cut)
				{
					FileUtil.deleteFileModel(sourceFileModel);
				}

			}
			else
			{
				if(isCancelled())
				{
					return false;
				}
				counter_no_files++;
				counter_size_files+=sourceFileModel.getLength();
				size_of_files_copied=FileUtil.humanReadableByteCount(counter_size_files);
				copied_file=sourceFileModel.getName();
				success=FileUtil.copy_FileModel_FileModel(sourceFileModel,destFileModel,current_file_name,cut,total_bytes_read);
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

			FileModel destFileModel=FileModelFactory.getFileModel(dest_folder,destFileObjectType,tree_uri,tree_uri_path);
			copy_result=FileUtil.CopyUriFileModel(data,destFileModel,file_name,total_bytes_read);

			if (copy_result) {
				String dest_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, file_name);
				copied_files_name.add(file_name);
				copied_source_file_path_list.add(dest_file_path);

				filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,copied_files_name,destFileObjectType, Collections.singletonList(dest_file_path));

				copied_files_name.clear();
				copied_source_file_path_list.clear();

			}

			return copy_result;
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
