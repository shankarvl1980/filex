package svl.kadatha.filex;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import com.github.mjdev.libaums.fs.UsbFile;

import java.util.*;
import java.io.*;

public class Iterate
{
	
	private static int TOTAL_NO_OF_FILES;
	private static long TOTAL_SIZE_OF_FILES;
	private static final List<Integer> FOLDERWISE_NO_OF_FILES=new ArrayList<>();
	private static final List<Long> FOLDERWISE_SIZE_OF_FILES=new ArrayList<>();

	public static List<File> populate(File[] source_list_files,List<File> target_list_files, boolean include_folder)
	{
		int size=source_list_files.length;
		for(int i=0;i<size;++i)
		{

			int no_of_files=0;
			long size_of_files=0L;
			File f=source_list_files[i];
			if(f.isDirectory())
			{
				File[] files_array=f.listFiles();
				if (files_array.length != 0) {
					populate(files_array, target_list_files, include_folder);
				}
				target_list_files.add(f);
				if(include_folder)
				{
					no_of_files++;
				}
			}
			else
			{
				target_list_files.add(f);
				no_of_files++;
				size_of_files+=f.length();
			}
			FOLDERWISE_NO_OF_FILES.add(no_of_files);
			FOLDERWISE_SIZE_OF_FILES.add(size_of_files);
			
			TOTAL_NO_OF_FILES+=no_of_files;
			TOTAL_SIZE_OF_FILES+=size_of_files;
		}

		return target_list_files;
	}

	public static List<File> populate(File f,List<File> target_list_files, boolean include_folder)
	{
		int no_of_files=0;
		long size_of_files=0L;
		if(f.isDirectory())
		{
			File[] files_array=f.listFiles();

			if (files_array!=null && files_array.length != 0) {
				populate(files_array, target_list_files, include_folder);
			}
			target_list_files.add(f);
			if(include_folder)
			{
				no_of_files++;
			}
		}
		else
		{
			target_list_files.add(f);
			no_of_files++;
			size_of_files+=f.length();
		}
		FOLDERWISE_NO_OF_FILES.add(no_of_files);
		FOLDERWISE_SIZE_OF_FILES.add(size_of_files);

		TOTAL_NO_OF_FILES+=no_of_files;
		TOTAL_SIZE_OF_FILES+=size_of_files;
		return target_list_files;
	}


	public static List<UsbFile> populate(UsbFile[] source_list_files, List<UsbFile> target_list_files, boolean include_folder)
	{
		int size=source_list_files.length;
		for(int i=0;i<size;++i)
		{

			int no_of_files=0;
			long size_of_files=0L;
			UsbFile f=source_list_files[i];
			if(f.isDirectory())
			{

				try
				{
					UsbFile[] files_array=f.listFiles();
					if (files_array.length != 0) {
						populate(files_array, target_list_files, include_folder);
					}
					target_list_files.add(f);
					if(include_folder)
					{
						no_of_files++;
					}
				}
				catch (IOException e)
				{

				}
			}
			else
			{
				target_list_files.add(f);
				no_of_files++;
				size_of_files+=f.getLength();
			}
			FOLDERWISE_NO_OF_FILES.add(no_of_files);
			FOLDERWISE_SIZE_OF_FILES.add(size_of_files);

			TOTAL_NO_OF_FILES+=no_of_files;
			TOTAL_SIZE_OF_FILES+=size_of_files;

		}

		return target_list_files;
	}

	public static List<String> populate(Context context, List<String> source_list_files, List<String> target_list_files, boolean include_folder, Uri source_uri, String source_uri_path)
	{
		int size=source_list_files.size();
		for(int i=0;i<size;++i)
		{
			int no_of_files=0;
			long size_of_files=0L;
			String parent_file_path=source_list_files.get(i);
			Uri uri=FileUtil.getDocumentUri(parent_file_path,source_uri,source_uri_path);
			if(FileUtil.isDirectory(context,uri))
			{
				Uri children_uri= DocumentsContract.buildChildDocumentsUriUsingTree(source_uri,FileUtil.getDocumentID(parent_file_path,source_uri,source_uri_path));
				Cursor cursor=context.getContentResolver().query(children_uri,new String[] {DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
				if(cursor!=null && cursor.getCount()>0)
				{
					List<String> inner_source_list_files=new ArrayList<>();
					while(cursor.moveToNext())
					{
						String docID=cursor.getString(0);
						String displayName=cursor.getString(1);
						inner_source_list_files.add(parent_file_path+File.separator+displayName);
					}
					cursor.close();
					populate(context,inner_source_list_files,target_list_files,include_folder,source_uri,source_uri_path);
					target_list_files.add(parent_file_path);
				}
				else
				{
					target_list_files.add(parent_file_path);
				}

				if(include_folder)
				{
					no_of_files++;
				}
			}
			else
			{
				target_list_files.add(parent_file_path);
				no_of_files++;
				size_of_files+=FileUtil.getSize(context,uri);
			}
			FOLDERWISE_NO_OF_FILES.add(no_of_files);
			FOLDERWISE_SIZE_OF_FILES.add(size_of_files);

			TOTAL_NO_OF_FILES+=no_of_files;
			TOTAL_SIZE_OF_FILES+=size_of_files;
		}
		return target_list_files;
	}
	
	public static List<File>[] populate_folderwise(File [] source_list_files,List<File>[] target_file_list_array,boolean include_folder)
	{
		int size=source_list_files.length;
		for(int i=0;i<size;++i)
		{
			target_file_list_array[i]=new ArrayList<>();
			populate(source_list_files,target_file_list_array[i],include_folder);
			//TOTAL_NO_OF_FILES+=TOTAL_NO_OF_FILES;
			//TOTAL_SIZE_OF_FILES+=TOTAL_SIZE_OF_FILES;
		}
		return target_file_list_array;
	}


}
