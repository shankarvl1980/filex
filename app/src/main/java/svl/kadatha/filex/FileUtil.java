package svl.kadatha.filex;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;

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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
	 * Utility class for helping parsing file systems.
	 */
	public final class FileUtil 
	{

		/**
		 * The name of the primary volume (LOLLIPOP).
		 */
		private static final String PRIMARY_VOLUME_NAME = "primary";
		public final static int BUFFER_SIZE=8192;
		public static int USB_CHUNK_SIZE;

		/**
		 * Hide default constructor.
		 */
		private FileUtil() 
		{
			throw new UnsupportedOperationException();
		}

	
	
	/**
	 * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If the file is not
	 * existing, it is created.
	 *
	 * @param file              The file.
	 * @param isDirectory       flag indicating if the file should be a directory.
	 * @param createDirectories flag indicating if intermediate path directories should be created if not existing.
	 * @return The DocumentFile
	 */
	 



		@RequiresApi(api = VERSION_CODES.LOLLIPOP)
		public static Uri getDocumentUri(@NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
		{
			String target_uri_id=getDocumentID(target_file_path,tree_uri,tree_uri_path);
			return DocumentsContract.buildDocumentUriUsingTree(tree_uri,target_uri_id);
		}

		public static Uri createDocumentUri(Context context, @NonNull final String parent_file_path, @Nullable String name, @NonNull final boolean isDirectory,
											@NonNull Uri tree_uri, String tree_uri_path)
		{
			Uri uri=getDocumentUri(parent_file_path+File.separator+name,tree_uri,tree_uri_path);
			if(exists(context,uri))
			{
				return uri;
			}
			Uri parent_uri=getDocumentUri(parent_file_path,tree_uri, tree_uri_path);
			if (isDirectory)
			{
				try {
					uri=DocumentsContract.createDocument(context.getContentResolver(),parent_uri, DocumentsContract.Document.MIME_TYPE_DIR,name);
				} catch (FileNotFoundException ignored) {

				}
			}
			else
			{
				try {
					uri=DocumentsContract.createDocument(context.getContentResolver(),parent_uri,"text",name);
				} catch (FileNotFoundException ignored) {
				}
			}
			return uri;
		}

		public static String getDocumentID(String file_path,@NonNull Uri tree_uri, @NonNull String tree_uri_path)
		{

			String relativePath="";
			if(!file_path.equals(tree_uri_path))
			{
				if(tree_uri_path.equals(File.separator))
				{
					relativePath = file_path.substring(tree_uri_path.length());
				}
				else
				{
					relativePath = file_path.substring(tree_uri_path.length() + 1);
				}
			}
			String target_uri_id=DocumentsContract.getTreeDocumentId(tree_uri);
			if(!target_uri_id.endsWith(File.separator))
			{
				target_uri_id=target_uri_id+File.separator;
			}
			target_uri_id=target_uri_id+relativePath;
			return target_uri_id;
		}

	public static String getMimeType(Context context,Uri uri)
	{
		return context.getContentResolver().getType(uri);
	}
 	public static boolean isDirectory(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
	{
		Uri uri=getDocumentUri(target_file_path,tree_uri, tree_uri_path);
		if(uri!=null)
		{
			String mime_type=getMimeType(context,uri);
			return mime_type.equals(DocumentsContract.Document.MIME_TYPE_DIR);
		}
		else
		{
			return false;
		}
	}

	public static boolean isDirectory(Context context, @NonNull Uri uri)
	{

		String mime_type;
		Cursor cursor=context.getContentResolver().query(uri,new String[] {DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null);
		if(cursor!=null)
		{
			cursor.moveToFirst();
			mime_type=cursor.getString(0);
			cursor.close();
			return mime_type.equals(DocumentsContract.Document.MIME_TYPE_DIR);
		}
		return false;
	}

	public static long getSize(Context context, @NonNull Uri uri)
	{
		String size="0";
		Cursor cursor=context.getContentResolver().query(uri,new String[] {DocumentsContract.Document.COLUMN_SIZE},null,null,null);
		if(cursor!=null)
		{
			cursor.moveToFirst();
			size=cursor.getString(0);
			cursor.close();
		}
		return Long.parseLong(size);
	}

	public static boolean exists(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
	{
		Uri uri=getDocumentUri(target_file_path,tree_uri,tree_uri_path);
		if(uri!=null)
		{
			return exists(context,uri);
		}
		else
		{
			return false;
		}
	}

	public static boolean exists(Context context, Uri uri)
	{
		return context.getContentResolver().getType(uri) !=null;
	}



	public static List<String> list(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
	{
		List<String> list=new ArrayList<>();
		Uri uri=getDocumentUri(target_file_path,tree_uri, tree_uri_path);
		if(uri!=null)
		{
			if(isDirectory(context,uri))
			{
				Uri children_uri=DocumentsContract.buildChildDocumentsUriUsingTree(tree_uri,FileUtil.getDocumentID(target_file_path,tree_uri,tree_uri_path));
				Cursor cursor=context.getContentResolver().query(children_uri,new String[] {DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
				if(cursor!=null && cursor.getCount()>0)
				{
					while(cursor.moveToNext())
					{
						String displayName=cursor.getString(0);
						list.add(displayName);
					}
					cursor.close();
				}
			}
		}
		return  list;
	}


		@SuppressWarnings("null")
		public static boolean copy_File_File(@NonNull final File source, @NonNull final File target, boolean cut)
		{
			try (FileInputStream fileInStream = new FileInputStream(source); FileOutputStream fileOutStream = new FileOutputStream(target)) {
				FileChannel inputChannel = fileInStream.getChannel();
				FileChannel outputChannel = fileOutStream.getChannel();
				channelCopy(inputChannel, outputChannel);
				if (cut) {
					deleteNativeFile(source);
				}

			} catch (Exception e) {
				//Log.e(Application.TAG,
				//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
				return false;
			}
			// ignore exception
			// ignore exception
			return true;
		}


		@SuppressWarnings("null")
		public static boolean copy_SAFFile_File(Context context, @NonNull final String source_file_path, Uri source_uri, String source_uri_path, File target_file)
		{
			FileOutputStream fileOutputStream=null;
			InputStream inStream=null;
			try
			{
				fileOutputStream=new FileOutputStream(target_file);
				Uri uri = getDocumentUri(source_file_path, source_uri,source_uri_path);
				if (uri != null)
				{
					inStream = context.getContentResolver().openInputStream(uri);
				}
				else
				{
					return false;
				}

				if (inStream != null)
				{

					fastChannelCopy(Channels.newChannel(inStream),Channels.newChannel(fileOutputStream),false);
				}

			}
			catch (Exception e) {

				return false;
			}
			finally
			{
				try
				{
					inStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

				try {
					fileOutputStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}
			}
			return true;
		}

		@SuppressWarnings("null")
		public static boolean copy_UsbFile_File(UsbFile src_usbfile, File target_file,boolean cut)
		{
			try (InputStream inStream = UsbFileStreamFactory.createBufferedInputStream(src_usbfile,MainActivity.usbCurrentFs); OutputStream outputStream = new FileOutputStream(target_file)) {

				bufferedCopy(inStream, outputStream,true);
				if (cut) {
					deleteUsbFile(src_usbfile);
				}

			} catch (Exception e) {

				return false;
			}
			// ignore exception

			// ignore exception
			return true;
		}

		@SuppressWarnings("null")
		public static boolean copy_FtpFile_File(String src_file_path, File target_file,boolean cut)
		{
			boolean success;
			try {

				OutputStream outputStream=new BufferedOutputStream(new FileOutputStream(target_file));
				success=MainActivity.FTP_CLIENT.retrieveFile(src_file_path,outputStream);
				if (cut && success) {
					deleteFTPFile(src_file_path);
				}

			} catch (Exception e) {

				return false;
			}
			// ignore exception

			// ignore exception
			return true;
		}


		@SuppressWarnings("null")
		public static boolean copy_UsbFile_UsbFile(@NonNull final UsbFile source, @NonNull String target_file_path,String name, boolean cut)
		{
			InputStream inStream = null;
			OutputStream outStream=null;

			try
			{
				inStream = UsbFileStreamFactory.createBufferedInputStream(source,MainActivity.usbCurrentFs);
				UsbFile parentUsbFile=getUsbFile(MainActivity.usbFileRoot,target_file_path);
				UsbFile targeUsbFile;
				if (parentUsbFile != null)
				{
					targeUsbFile = parentUsbFile.createFile(name);
					long length=source.getLength();
					if(length>0) targeUsbFile.setLength(length); // causes problem
					outStream=UsbFileStreamFactory.createBufferedOutputStream(targeUsbFile,MainActivity.usbCurrentFs);
				}
				else
				{
					return false;
				}

				fastChannelCopy(Channels.newChannel(inStream),Channels.newChannel(outStream),true);

				if(cut)
				{
					deleteUsbFile(source);
				}
			}
			catch (Exception e)
			{
				//Log.e(Application.TAG,
				//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
				return false;
			}
			finally
			{
				try
				{
					inStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

				try
				{
					outStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

			}
			return true;
		}

		@SuppressWarnings("null")
	public static boolean copy_SAFFile_SAFFile(Context context, @NonNull final String source_file_path, Uri source_uri, String source_uri_path, String target_file_path, String name,Uri tree_uri,String tree_uri_path)
	{
		InputStream inStream = null;
		OutputStream outStream=null;
		try 
		{
			if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,source_file_path))
			{
				inStream = new FileInputStream(source_file_path);

			}
			else
			{
				Uri uri=getDocumentUri(source_file_path,source_uri,source_uri_path);
				inStream=context.getContentResolver().openInputStream(uri);
			}

			Uri uri = createDocumentUri(context, target_file_path,name, false,tree_uri,tree_uri_path);
			if (uri != null)
			{
				outStream = context.getContentResolver().openOutputStream(uri);
			}
			else
			{
				return false;
			}


			if (outStream != null && inStream!=null)
			{

				fastChannelCopy(Channels.newChannel(inStream),Channels.newChannel(outStream),false);

			}

		}
		catch (Exception e) {
			//Log.e(Application.TAG,
			//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
			return false;
		}
		finally 
		{
			try 
			{
				inStream.close();
			}
			catch (Exception e) 
			{
				// ignore exception
			}
		
			try {
				outStream.close();
			}
			catch (Exception e) 
			{
				// ignore exception
			}

		}
		return true;
	}




	@SuppressWarnings("null")
	public static boolean copy_File_SAFFile(Context context,@NonNull final File source, @NonNull String target_file_path,String name,Uri tree_uri, String tree_uri_path, boolean cut)
	{
		FileInputStream fileInStream = null;
		OutputStream outStream=null;

		try 
		{
			fileInStream = new FileInputStream(source);
			Uri uri = createDocumentUri(context,target_file_path,name,false,tree_uri,tree_uri_path);
			if (uri != null)
			{
				outStream = context.getContentResolver().openOutputStream(uri);
			}
			else
			{
				return false;
			}


			if (outStream != null) 
			{

				channelCopy(fileInStream.getChannel(),Channels.newChannel(outStream));

			}

			if(cut)
			{
				deleteNativeFile(source);
			}
		}
		catch (Exception e) 
		{
			//Log.e(Application.TAG,
			//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
			return false;
		}
		finally 
		{
			try 
			{
				fileInStream.close();
			}
			catch (Exception e) 
			{
				// ignore exception
			}
			
			try 
			{
				outStream.close();
			}
			catch (Exception e) 
			{
				// ignore exception
			}

		}
		return true;
	}

		@SuppressWarnings("null")
		public static boolean copy_UsbFile_SAFFile(Context context,@NonNull final UsbFile source, @NonNull String target_file_path,String name,Uri tree_uri, String tree_uri_path, boolean cut)
		{
			InputStream inStream = null;
			OutputStream outStream=null;

			try
			{
				inStream = UsbFileStreamFactory.createBufferedInputStream(source,MainActivity.usbCurrentFs);
				Uri uri = createDocumentUri(context,target_file_path,name,false,tree_uri,tree_uri_path);

				if (uri != null)
				{
					outStream = context.getContentResolver().openOutputStream(uri);
				}
				else
				{
					return false;
				}


				if (outStream != null)
				{
					fastChannelCopy(Channels.newChannel(inStream),Channels.newChannel(outStream),true);
				}

				if(cut)
				{
					deleteUsbFile(source);
				}

			}
			catch(IOException e)
			{
				//Log.e(Application.TAG,
				//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
				return false;
			}

			finally
			{
				try
				{
					inStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

				try
				{
					outStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

			}
			return true;
		}

		@SuppressWarnings("null")
		public static boolean copy_FtpFile_SAFFile(Context context,@NonNull final String source_file_path, @NonNull String target_file_path,String name,Uri tree_uri, String tree_uri_path, boolean cut)
		{
			boolean success;
			OutputStream outStream=null;

			try
			{
				Uri uri = createDocumentUri(context,target_file_path,name,false,tree_uri,tree_uri_path);

				if (uri != null)
				{
					outStream = context.getContentResolver().openOutputStream(uri);
				}
				else
				{
					return false;
				}


				if (outStream != null)
				{
					success=MainActivity.FTP_CLIENT.retrieveFile(source_file_path,outStream);
					if(success && cut)
					{
						deleteFTPFile(source_file_path);
					}
				}



			}
			catch(IOException e)
			{
				//Log.e(Application.TAG,
				//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
				return false;
			}

			finally
			{

				try
				{
					outStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

			}
			return true;
		}


		@SuppressWarnings("null")
		public static boolean copy_File_UsbFile(@NonNull final File source, @NonNull String target_file_path,String name, boolean cut)
		{
			FileInputStream fileInStream = null;
			OutputStream outStream=null;

			try
			{
				fileInStream = new FileInputStream(source);

				UsbFile parentUsbFile=getUsbFile(MainActivity.usbFileRoot,target_file_path);
				UsbFile targeUsbFile;
				if (parentUsbFile != null)
				{
					targeUsbFile = parentUsbFile.createFile(name);
					long length=source.length();
					if(length>0) targeUsbFile.setLength(length); // dont set length causes problems
					outStream=UsbFileStreamFactory.createBufferedOutputStream(targeUsbFile,MainActivity.usbCurrentFs);
				}
				else
				{
					return false;
				}

				channelCopy(fileInStream.getChannel(),Channels.newChannel(outStream));

				if(cut)
				{
					deleteNativeFile(source);
				}
			}
			catch (Exception e)
			{
				//Log.e(Application.TAG,
				//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
				return false;
			}
			finally
			{
				try
				{
					fileInStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

				try
				{
					outStream.close();
				}
				catch (Exception e)
				{
					// ignore exception
				}

			}
			return true;
		}

		@SuppressWarnings("null")
		public static boolean copy_File_FtpFile(@NonNull final File source, @NonNull String target_file_path,String name, boolean cut)
		{
			boolean success;
			//OutputStream outStream=null;

			try (FileInputStream fileInStream = new FileInputStream(source)) {
				String file_path = target_file_path.equals(File.separator) ? target_file_path + name : target_file_path + File.separator + name;
				success = MainActivity.FTP_CLIENT.storeFile(file_path, fileInStream);

				if (success && cut) {
					deleteNativeFile(source);
				}
			} catch (Exception e) {
				//Log.e(Application.TAG,
				//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
				return false;
			}
			// ignore exception

			return success;
		}


	public static UsbFile getUsbFile(UsbFile rootUsbFile,String file_path)
	{
		UsbFile usbFile=null;
		try
		{
			usbFile=rootUsbFile.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path));
		}

		catch (IOException e)
		{

		}
		return usbFile;
	}

	public static FTPFile getFTPFile(String file_path)
	{
		FTPFile ftpFile = null;
		File file=new File(file_path);
		String parent_path=file.getParent();
		String name=file.getName();
		try {
			FTPFile[] ftpFiles_array=MainActivity.FTP_CLIENT.listFiles(parent_path);
			int size= ftpFiles_array.length;
			for(int i=0;i<size;++i)
			{
				ftpFile=ftpFiles_array[i];
				if(ftpFile.getName().equals(name))
				{
					break;
				}
			}
		} catch (IOException e) {
			return null;
		}

		return ftpFile;
	}

	public static boolean renameUsbFile(UsbFile usbFile,String new_name)
	{
		try {
			usbFile.setName(new_name);
			return true;

		} catch (IOException e) {
			return false;
		}
	}



		public static boolean mkdirUsb(UsbFile parentUsbFile, String name)
		{
			try {
				parentUsbFile.createDirectory(name);
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		public static boolean mkdirFtp(String file_path)
		{
			try {
				return MainActivity.FTP_CLIENT.makeDirectory(file_path);
			} catch (IOException e) {
				return false;
			}
		}


		public static boolean createUsbFile(UsbFile parentUsbFile,String name)
		{
			try {
				parentUsbFile.createFile(name);
				return true;
			} catch (IOException e) {
				return false;
			}
		}



	@SuppressWarnings("null")
	public static FileOutputStream get_fileoutputstream(Context context,@NonNull final String target_file_path, Uri tree_uri,String tree_uri_path)
	{
		FileOutputStream fileOutStream=null;
		try 
		{
			// Storage Access Framework
			Uri uri = getDocumentUri(target_file_path,tree_uri, tree_uri_path);
			if (uri != null)
			{
				ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri,"rw");
				fileOutStream=new FileOutputStream(pfd.getFileDescriptor());

			}

			return fileOutStream;

		}
		catch (Exception e) {
			//Log.e(Application.TAG,
			//  "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
			return null;
		}
	}

	/**
	 * Delete a file. May be even on external SD card.
	 *
	 * @param file the file to be deleted.
	 * @return True if successfully deleted.
	 */
	private static boolean deleteNativeFile(@NonNull final File file)
	{

		// First try the normal deletion.
		if(file.delete())
		{
			return true;
		}
		return !file.exists();
	}
	
	
	private static boolean deleteSAFFile(Context context,String target_file_path,Uri tree_uri,String tree_uri_path)
	{
		// Try with Storage Access Framework.
		Uri uri = getDocumentUri(target_file_path,tree_uri,tree_uri_path);
		try {
			return uri != null && DocumentsContract.deleteDocument(context.getContentResolver(),uri);
		} catch (FileNotFoundException | IllegalArgumentException e) {
			return false;
		}


	}

		private static boolean deleteUsbFile(UsbFile usbFile)
		{
			try {
				usbFile.delete();
				return true;
			} catch (IOException e) {
				return false;
			}
		}


		private static boolean deleteFTPFile(String file_path)
		{
			try {
				return MainActivity.FTP_CLIENT.deleteFile(file_path);
			} catch (IOException e) {
				return false;
			}
		}


		public static boolean deleteNativeDirectory(final File folder)
	{     
		boolean success=true;
		
		if (folder.isDirectory())            //Check if folder file is a real folder
		{
			File[] list = folder.listFiles(); //Storing all file name within array
			if (list != null)                //Checking list value is null or not to check folder containts atlest one file
			{
				int size=list.length;
				for (int i = 0; i < size; ++i)
				{
					File tmpF = list[i];
					success=deleteNativeDirectory(tmpF);

				}
			}

		}
		success=deleteNativeFile(folder);

		return success;
	}

		public static boolean deleteSAFDirectory(Context context,final String file_path, Uri tree_uri, String tree_uri_path)
		{
			boolean success=true;
			File folder=new File(file_path);
			if (folder.isDirectory())            //Check if folder file is a real folder
			{
				File[] list = folder.listFiles(); //Storing all file name within array
				if(list!=null)
				{
					int size=list.length;
					for (int i = 0; i < size; ++i)
					{

						File tmpF = list[i];
						success=deleteSAFDirectory(context,tmpF.getAbsolutePath(),tree_uri,tree_uri_path);

					}

				}

			}

			success=deleteSAFFile(context,folder.getAbsolutePath(),tree_uri,tree_uri_path);
			return success;
		}

		public static boolean deleteUsbDirectory(final UsbFile folder)
		{
			boolean success=true;

			if (folder.isDirectory())            //Check if folder file is a real folder
			{
				UsbFile[] list = new UsbFile[0]; //Storing all file name within array
				try {
					list = folder.listFiles();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (list != null)                //Checking list value is null or not to check folder containts atlest one file
				{
					int size=list.length;
					for (int i = 0; i < size; ++i)
					{
						UsbFile tmpF = list[i];
						success=deleteUsbDirectory(tmpF);

					}
				}

			}
			success=deleteUsbFile(folder);

			return success;
		}

		public static boolean deleteFtpDirectory(final String file_path)
		{
			boolean success=true;
			FTPFile folder;
			try {
				folder = FileUtil.getFTPFile(file_path); //MainActivity.FTP_CLIENT.mlistFile(file_path);
				if (folder.isDirectory())            //Check if folder file is a real folder
				{

					FTPFile[] list = MainActivity.FTP_CLIENT.listFiles(file_path); //Storing all file name within array
					if(list!=null)
					{
						int size=list.length;
						for (int i = 0; i < size; ++i)
						{

							FTPFile tmpF = list[i];
							String name=tmpF.getName();
							String path=(file_path.endsWith(File.separator)) ? file_path+name : file_path+File.separator+name;
							success=deleteFtpDirectory(path);

						}
					}


				}

				if(folder.isDirectory())
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




/*

	 //This method is not useful because we can directly delete entire directory through deleteSAFFile(@NonNull final File file, Context context,String baseFolder) method without resorting to any recursive method.
	public static boolean deleteSAFDirectory(final File folder, Context context,String baseFolder) {     
		boolean success=false;

		if (folder.isDirectory())            //Check if folder file is a real folder
		{
			File[] list = folder.listFiles(); //Storing all file name within array
			if (list != null)                //Checking list value is null or not to check folder containts atlest one file
			{
				for (int i = 0; i < list.length; ++i)
				{
					File tmpF = list[i];
					if (tmpF.isDirectory())   //if folder  found within folder remove that folder using recursive method
					{
						success=deleteSAFDirectory(tmpF,context,baseFolder);
					}
					else
					{
						success=deleteSAFFile(tmpF,context,baseFolder); //else delete filr
					}

				}
			}

			if(folder.exists())  //delete empty folder
			{
				success=deleteSAFFile(folder,context,baseFolder);
			}

		}
		else
		{
			success=deleteSAFFile(folder,context,baseFolder);
		}
		
		return success;
	}

	*/
		
		/**
		 * Rename a folder. In case of extSdCard in Kitkat, the old folder stays in place, but files are moved.
		 *
		 * @param source The source folder.
		 * @param target The target folder.
		 * @return true if the renaming was successful.
		 */
		public static boolean renameNativeFile(@NonNull final File source, @NonNull final File target) 
		{
			// First try the normal rename.
			if (source.renameTo(target)) return true;

			if (target.exists()) return false;

			return false;
		}

	/**
	 * Rename a folder. In case of extSdCard in Kitkat, the old folder stays in place, but files are moved.
	 *
	 * @param source The source folder.
	 * @param target The target folder.
	 * @return true if the renaming was successful.
	 (@NonNull final File source, @NonNull final File target,Context context,String uri_string,String baseFolder)
	 */
	public static boolean renameSAFFile(Context context, String target_file_path, String new_name, Uri tree_uri, String tree_uri_path)
	{
	
		// Try the Storage Access Framework if it is just a rename within the same parent folder.
		Uri uri = getDocumentUri(target_file_path,tree_uri,tree_uri_path);
		try {
			uri=DocumentsContract.renameDocument(context.getContentResolver(),uri,new_name);
		} catch (FileNotFoundException e) {

		}
		return uri!=null;
	}
	
		

	public static boolean createNativeNewFile(@NonNull final File file) 
	{
		
		if (file.exists()) 
		{
			return false;
		}

		try
		{
			
			if (file.createNewFile()) 
			{
				return true;
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return false;
	}


	public static boolean createSAFNewFile(Context context, String target_file_path, String name, Uri tree_uri, String tree_uri_path)
	{
		Uri uri = createDocumentUri(context, target_file_path,name,false,tree_uri,tree_uri_path);
		return uri != null;

	}

		/**
		 * Create a folder. The folder may even be on external SD card for Kitkat.
		 *
		 * @param file The folder to be created.
		 * @return True if creation was successful.
		 */
		public static boolean mkdirNative(@NonNull final File file)
		{
			if (file.exists())
			{
				return true;
			}

			return file.mkdir();
		}
		public static boolean mkdirsNative(@NonNull final File file)
		{
			if (file.exists())
			{
				return file.isDirectory();
			}

			return file.mkdirs();
		}

	public static boolean mkdirSAF(Context context, String target_file_path, String name, Uri tree_uri, String tree_uri_path)
	{

		// Try with Storage Access Framework.
		Uri uri=createDocumentUri(context,target_file_path,name, true,tree_uri,tree_uri_path);
		return uri!=null;

	}
		public static boolean mkdirsSAFFile(Context context, String parent_file_path, @NonNull String path, Uri tree_uri, String tree_uri_path)
		{
			boolean success=true;
			String [] file_path_substring=path.split("/");
			int size=file_path_substring.length;
			for (int i=0; i<size;++i)
			{
				String path_string=file_path_substring[i];
				if(!path_string.equals(""))
				{
					if(!new File(parent_file_path,path_string).exists())
					{
						success=mkdirSAF(context,parent_file_path,path_string,tree_uri,tree_uri_path);

					}
					parent_file_path+=File.separator+path_string;
					if(!success)
					{
						return false;
					}
				}

			}
			return success;
		}

		public static boolean mkdirsUsb(String parent_file_path, @NonNull String path)
		{
			boolean success=true;
			UsbFile parentUsbFile=getUsbFile(MainActivity.usbFileRoot,parent_file_path);
			if(parentUsbFile==null)
			{
				return false;

			}
			String [] path_substring=path.split("/");
			int size=path_substring.length;
			for (int i=0; i<size;++i)
			{
				String path_string=path_substring[i];
				if(!path_string.equals(""))
				{
					UsbFile usbFile;
					if((usbFile=getUsbFile(parentUsbFile,path_string))==null)
					{
						success=mkdirUsb(parentUsbFile,path_string);
						parentUsbFile=getUsbFile(parentUsbFile,path_string);
					}
					else
					{
						parentUsbFile=usbFile;
					}

					if(!success)
					{
						return false;
					}
				}

			}
			return success;
		}
		public static boolean mkdirsSAFD(Context context, String target_file_path, @NonNull String name, Uri tree_uri, String tree_uri_path)
		{
			boolean success=false;

			target_file_path=target_file_path+File.separator+name;

			String target_uri_path_copy="";
			int offset;
			if(tree_uri_path.equals(File.separator))
			{
				offset=1;
			}
			else
			{
				target_uri_path_copy=tree_uri_path;
				offset=tree_uri_path.length()+1;
			}

			String [] file_path_substring=target_file_path.substring(offset).split("/");
			for (int i=0; i< file_path_substring.length;++i)
			{
				if(!FileUtil.exists(context,target_uri_path_copy+File.separator+file_path_substring[i],tree_uri,tree_uri_path))
				{

					if(target_uri_path_copy.equals(""))
					{
						success=mkdirSAF(context,tree_uri_path,file_path_substring[i],tree_uri,tree_uri_path);
					}
					else
					{
						success=mkdirSAF(context,target_uri_path_copy,file_path_substring[i],tree_uri,tree_uri_path);
					}

					if(!success)
					{
						return false;
					}

				}
				target_uri_path_copy=target_uri_path_copy+File.separator+file_path_substring[i];

			}
			return success;
		}



		/**
		 * Delete a folder.
		 *
		 * @param file The folder name.
		 * @return true if successful.
		 */
		public static boolean rmdirNative(@NonNull final File file) 
		{
			if (!file.exists()) 
			{
				return true;
			}
			if (!file.isDirectory()) 
			{
				return false;
			}
			String[] fileList = file.list();
			if (fileList != null && fileList.length > 0) 
			{
				// Delete only empty folder.
				return false;
			}

			// Try the normal way
			if (file.delete()) 
			{
				return true;
			}
			return !file.exists();
		}


	public static boolean rmdirSAF(Context context,String target_file_path, Uri tree_uri, String tree_uri_path)
	{

		// Try with Storage Access Framework.
		Uri uri  = getDocumentUri(target_file_path,tree_uri,tree_uri_path);
		try {
			return uri != null && DocumentsContract.deleteDocument(context.getContentResolver(),uri);
		} catch (FileNotFoundException e) {

		}
		return false;
	}
		
		
		/**
		 * Delete all files in a folder.
		 *
		 * @param folder the folder
		 * @return true if successful.
		 */
		public static boolean deleteNativeFilesInFolder(@NonNull final File folder) 
		{
			boolean totalSuccess = true;

			String[] children = folder.list();
			if (children != null) 
			{
				int size=children.length;
				for(int i=0;i<size;++i)
				//for (String child : children)
				{
					String child=children[i];
					File file = new File(folder, child);
					if (!file.isDirectory()) 
					{
						boolean success =FileUtil.deleteNativeFile(file);
						if (!success) 
						{
							//Log.w(Application.TAG, "Failed to delete file" + child);
							totalSuccess = false;
						}
					}
				}
			}
			return totalSuccess;
		}


	public static boolean isFromInternal(FileObjectType fileObjectType, @NonNull final String file_path)
	{
		boolean is_from_internal = false;
		if(!fileObjectType.equals(FileObjectType.FILE_TYPE) && !fileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE)) return  false;
		for(String internal_storage_path:Global.INTERNAL_STORAGE_PATH)
		{
			//if ((file_path+File.separator).startsWith(internal_storage_path+File.separator)) {
			if (Global.IS_CHILD_FILE(file_path,internal_storage_path)) {
				is_from_internal = true;
				break;
			}
		}
		return is_from_internal;
	}


	public static boolean isWritable(FileObjectType fileObjectType,@NonNull final String file_path)
	{
		return isFromInternal(fileObjectType,file_path);

	}



	public static String humanReadableByteCount(long bytes, boolean si) 
	{
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "KMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		//return (long)(bytes/Math.pow(unit,exp)*100+0.5)/100.0 + " "+pre+"B";
		return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
		/**
		 * Get the SD card directory.
		 *
		 * @return The SD card directory.
		 */
		@NonNull
		public static String getSdCardPath() 
		{
			String sdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

			try 
			{
				sdCardDirectory = new File(sdCardDirectory).getCanonicalPath();
			}
			catch (IOException ioe) 
			{
				//Log.e(Application.TAG, "Could not get SD directory", ioe);
			}
			return sdCardDirectory;
		}

		/**
		 * Get a list of external SD card paths. (Kitkat or higher.)
		 *
		 * @return A list of external SD card paths.
		 */
		@RequiresApi(Build.VERSION_CODES.KITKAT)
		public static String[] getExtSdCardPaths(Context context) 
		{
			List<String> paths = new ArrayList<>();
			for (File file : context.getExternalFilesDirs("external")) 
			{
				if (file != null && !file.equals(context.getExternalFilesDir("external"))) 
				{
					int index = file.getAbsolutePath().lastIndexOf("/Android/data");
					if (index >= 0) {
						String path = file.getAbsolutePath().substring(0, index);
						try
						{
							path = new File(path).getCanonicalPath();
						}
						catch (IOException e)
						{
							// Keep non-canonical path.
						}
						paths.add(path);
					} //else {
						//.Log.w(Application.TAG, "Unexpected external file dir: " + file.getAbsolutePath());
					//}
				}
			}
			return paths.toArray(new String[0]);
		}

		/**
		 * Determine the main folder of the external SD card containing the given file.
		 *
		 * @param file the file.
		 * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
		 * null is returned.
		 */
		@RequiresApi(Build.VERSION_CODES.KITKAT)
		public static String getExtSdCardFolder(@NonNull final File file,Context context) 
		{
			String[] extSdPaths = getExtSdCardPaths(context);
			try 
			{
				for (String extSdPath : extSdPaths) 
				{
					//if ((file.getCanonicalPath()+File.separator).startsWith(extSdPath+File.separator))
					if (Global.IS_CHILD_FILE(file.getCanonicalPath(),extSdPath))
					{
						return extSdPath;
					}
				}
			}
			catch (IOException e) 
			{
				return null;
			}
			return null;
		}

		/**
		 * Determine if a file is on external sd card. (Kitkat or higher.)
		 *
		 * @param file The file.
		 * @return true if on external sd card.
		 */
		@RequiresApi(Build.VERSION_CODES.KITKAT)
		public static boolean isOnExtSdCard(@NonNull final File file,Context context) 
		{
			return getExtSdCardFolder(file,context) != null;
		}


		/**
		 * Get the full path of a document from its tree URI.
		 *
		 * @param tree_uri The tree RI.
		 * @return The path (without trailing file separator).
		 */
		@RequiresApi(api = VERSION_CODES.LOLLIPOP)
		@Nullable
		public static String getFullPathFromTreeUri(@Nullable final Uri tree_uri,Context context) 
		{
			if (tree_uri == null) 
			{
				return null;
			}
			String volumePath = FileUtil.getVolumePath(FileUtil.getVolumeIdFromTreeUri(tree_uri),context);
			if (volumePath == null) 
			{
				return File.separator;
			}
			if (volumePath.endsWith(File.separator)) 
			{
				volumePath = volumePath.substring(0, volumePath.length() - 1);
			}

			String documentPath = FileUtil.getDocumentPathFromTreeUri(tree_uri);
			if (documentPath.endsWith(File.separator)) 
			{
				documentPath = documentPath.substring(0, documentPath.length() - 1);
			}

			if (documentPath.length() > 0) 
			{
				if (documentPath.startsWith(File.separator)) 
				{
					return volumePath + documentPath;
				}
				else 
				{
					return volumePath + File.separator + documentPath;
				}
			}
			else 
			{
				return volumePath;
			}
		}

		/**
		 * Get the path of a certain volume.
		 *
		 * @param volumeId The volume id.
		 * @return The path.
		 */
		private static String getVolumePath(final String volumeId,Context context) 
		{

			try 
			{
				StorageManager mStorageManager =
					(StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

				Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

				Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
				Method getUuid = storageVolumeClazz.getMethod("getUuid");
				Method getPath = storageVolumeClazz.getMethod("getPath");
				Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
				Object result = getVolumeList.invoke(mStorageManager);

				final int length = Array.getLength(result);
				for (int i = 0; i < length; ++i)
				{
					Object storageVolumeElement = Array.get(result, i);
					String uuid = (String) getUuid.invoke(storageVolumeElement);
					Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

					// primary volume?
					if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) 
					{
						return (String) getPath.invoke(storageVolumeElement);
					}

					// other volumes?
					if (uuid != null) 
					{
						if (uuid.equals(volumeId))
						{
							return (String) getPath.invoke(storageVolumeElement);
						}
					}
				}

				// not found.
				return null;
			}
			catch (Exception ex) 
			{
				return null;
			}
		}

		/**
		 * Get the volume ID from the tree URI.
		 *
		 * @param tree_uri The tree URI.
		 * @return The volume ID.
		 */
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		private static String getVolumeIdFromTreeUri(final Uri tree_uri) 
		{
			final String docId = DocumentsContract.getTreeDocumentId(tree_uri);
			final String[] split = docId.split(":");

			if (split.length > 0) 
			{
				return split[0];
			}
			else 
			{
				return null;
			}
		}

		/**
		 * Get the document path (relative to volume name) for a tree URI (LOLLIPOP).
		 *
		 * @param tree_uri The tree URI.
		 * @return the document path.
		 */
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		private static String getDocumentPathFromTreeUri(final Uri tree_uri) 
		{
			final String docId = DocumentsContract.getTreeDocumentId(tree_uri);
			final String[] split = docId.split(":");
			if ((split.length >= 2) && (split[1] != null)) 
			{
				return split[1];
			}
			else 
			{
				return File.separator;
			}
		}

	
	public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest, boolean fromUsbFile) throws IOException
	{
		//Log.d("shankar", "fastChannelcopy : chunk-size - "+USB_CHUNK_SIZE);
		final ByteBuffer buffer = (fromUsbFile) ? ByteBuffer.allocate(USB_CHUNK_SIZE) : ByteBuffer.allocateDirect(16384);
		while (src.read(buffer) != -1)
		{
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining())
		{
			dest.write(buffer);
		}

	}

	public static void channelCopy(final FileChannel src, final WritableByteChannel dest) throws IOException
	{
		long size=src.size();
		src.transferTo(0,size,dest);
		src.close();
	}

	public static void bufferedCopy(InputStream inputStream, OutputStream outputStream, boolean fromUsbFile)
	{
		//Log.d("shankar", "bufferedcopy : chunk-size - "+USB_CHUNK_SIZE);
		byte[] buffer=(fromUsbFile) ? new byte[USB_CHUNK_SIZE] : new byte[BUFFER_SIZE];
		int count;
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream); BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
			while ((count = bufferedInputStream.read(buffer)) != -1) {
				bufferedOutputStream.write(buffer, 0, count);
			}
		} catch (IOException e) {

		}

	}

	public static void channelCopy(FileChannel srcChannel, FileChannel destChannel)
	{
		try
		{
			long size=srcChannel.size();
			srcChannel.transferTo(0,size,destChannel);

		}
		catch(IOException e){}
	
		finally
		{
			
			try
			{
				if(srcChannel!=null)
				{
					srcChannel.close();
				}
				if(destChannel!=null)
				{
					destChannel.close();
				}
			}
			catch(IOException e){}
			
		}
	
	}


}
	
