package svl.kadatha.filex;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileStreamFactory;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FtpFileModel;
import svl.kadatha.filex.filemodel.UsbFileModel;
import timber.log.Timber;

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
	private static final String TAG = "Ftp-FileUtil";
	/**
	 * Hide default constructor.
	 */
	private FileUtil()
	{
		throw new UnsupportedOperationException();
	}






	public static Uri getDocumentUri(@NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
	{
		String target_uri_id=getDocumentID(target_file_path,tree_uri,tree_uri_path);
		return DocumentsContract.buildDocumentUriUsingTree(tree_uri,target_uri_id);
	}

	public static Uri createDocumentUri(Context context, @NonNull final String parent_file_path, @Nullable String name, final boolean isDirectory,
										@NonNull Uri tree_uri, String tree_uri_path)
	{
		Uri uri=getDocumentUri(Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,name),tree_uri,tree_uri_path);
		if(existsUri(context,uri))
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

	public static String getMimeTypeUri(Context context, Uri uri)
	{
		return context.getContentResolver().getType(uri);
	}

 	public static boolean isDirectoryUri(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
	{
		Uri uri=getDocumentUri(target_file_path,tree_uri, tree_uri_path);
		if(uri!=null)
		{
			String mime_type= getMimeTypeUri(context,uri);
			return mime_type.equals(DocumentsContract.Document.MIME_TYPE_DIR);
		}
		else
		{
			return false;
		}
	}

	public static boolean isDirectoryUri(Context context, @NonNull Uri uri)
	{

		String mime_type;
		Cursor cursor=context.getContentResolver().query(uri,new String[] {DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null);
		if(cursor!=null)
		{
			cursor.moveToFirst();
			mime_type=cursor.getString(0);
			cursor.close();
			if(mime_type==null){
				return false;
			}
			else{
				return mime_type.equals(DocumentsContract.Document.MIME_TYPE_DIR);
			}

		}
		return false;
	}

	public static long getSizeUri(Context context, @NonNull Uri uri)
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

	public static boolean existsUri(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
	{
		Uri uri=getDocumentUri(target_file_path,tree_uri,tree_uri_path);
		if(uri!=null)
		{
			return existsUri(context,uri);
		}
		else
		{
			return false;
		}
	}

	public static boolean existsUri(Context context, Uri uri)
	{
		return context.getContentResolver().getType(uri) !=null;
	}

	public static List<String> listUri(Context context, @NonNull final String target_file_path, @NonNull Uri tree_uri, String tree_uri_path)
	{
		List<String> list=new ArrayList<>();
		Uri uri=getDocumentUri(target_file_path,tree_uri, tree_uri_path);
		if(uri!=null)
		{
			if(isDirectoryUri(context,uri))
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







	public static boolean copy_File_FileModel(@NonNull final File sourceFile, @NonNull final FileModel destFileModel, String child_name ,boolean cut, long[] bytes_read)
	{
		try (FileInputStream fileInputStream = new FileInputStream(sourceFile); OutputStream outputStream = destFileModel.getChildOutputStream(child_name,0)) {

			bufferedCopy(fileInputStream,outputStream,false,bytes_read);
			if(destFileModel instanceof FtpFileModel)
			{
				//FtpClientRepository_old.getInstance().ftpClientMain.completePendingCommand();
			}
			if (cut) {
				sourceFile.delete();
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	@SuppressWarnings("null")
	public static boolean copy_FileModel_FileModel(@NonNull final FileModel sourceFileModel, @NonNull final FileModel destFileModel, String child_name ,boolean cut, long[] bytes_read)
	{
		try (InputStream inputStream = sourceFileModel.getInputStream(); OutputStream outputStream = destFileModel.getChildOutputStream(child_name,0)) {
			boolean fromUsbFile=sourceFileModel instanceof UsbFileModel;
			bufferedCopy(inputStream,outputStream,fromUsbFile,bytes_read);
			if(sourceFileModel instanceof FtpFileModel || destFileModel instanceof FtpFileModel)
			{
				//FtpClientRepository_old.getInstance().ftpClientMain.completePendingCommand();
			}
			if (cut) {
				sourceFileModel.delete();
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}
	@SuppressWarnings("null")
	public static boolean copy_File_File(@NonNull final File source, @NonNull final File target, boolean cut, long[] bytes_read)
	{
		try (FileInputStream fileInStream = new FileInputStream(source); FileOutputStream fileOutStream = new FileOutputStream(target)) {
			bufferedCopy(fileInStream,fileOutStream,false,bytes_read);
			if (cut) {
				// rename method does not work where move is between sd and internal memory. hence copy and cut
				deleteNativeFile(source);
			}


		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public static boolean CopyUriFileModel(@NonNull Uri data, FileModel destFileModel,String file_name,long []bytes_read)
	{
		try (InputStream inStream=App.getAppContext().getContentResolver().openInputStream(data); OutputStream fileOutStream = destFileModel.getChildOutputStream(file_name,0)) {

			bufferedCopy(inStream,fileOutStream,false,bytes_read);
			if(destFileModel instanceof FtpFileModel)
			{
				//FtpClientRepository_old.getInstance().ftpClientMain.completePendingCommand();
			}

		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("null")
	public static boolean copy_to_File(Context context, @NonNull final Uri data, @NonNull final File target, long[] bytes_read)
	{
		try (InputStream inStream=context.getContentResolver().openInputStream(data); FileOutputStream fileOutStream = new FileOutputStream(target)) {

			bufferedCopy(inStream,fileOutStream,false,bytes_read);

		} catch (Exception e) {
			return false;
		}

		return true;
	}


	@SuppressWarnings("null")
	public static boolean copy_UsbFile_File(UsbFile src_usbfile, File target_file, boolean cut, long[] bytes_read)
	{
		if(src_usbfile==null)return false;
		try (InputStream inStream = UsbFileStreamFactory.createBufferedInputStream(src_usbfile,MainActivity.usbCurrentFs); OutputStream outputStream = new FileOutputStream(target_file)) {
			bufferedCopy(inStream, outputStream,true,bytes_read);
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
	public static boolean copy_File_SAFFile(Context context, @NonNull final File source, @NonNull String target_file_path, String name, Uri tree_uri, String tree_uri_path, boolean cut, long[] bytes_read)
	{
		OutputStream outStream=null;
		try (FileInputStream fileInStream = new FileInputStream(source))
		{
			Uri uri = createDocumentUri(context,target_file_path,name,false,tree_uri,tree_uri_path);
			if (uri != null)
			{
				outStream = context.getContentResolver().openOutputStream(uri);
				if (outStream != null)
				{
					//channelCopy(fileInStream.getChannel(),Channels.newChannel(outStream),bytes_read);
					bufferedCopy(fileInStream,outStream,false,bytes_read);
				}

				if(cut)
				{
					deleteNativeFile(source);
				}
			}
			else
			{
				return false;
			}

		}
		catch (Exception e) 
		{
			return false;
		}
		finally 
		{
			try 
			{
				if(outStream!=null)outStream.close();
			}
			catch (Exception e) 
			{
				// ignore exception
			}

		}
		return true;
	}


	public static boolean make_UsbFile_non_zero_length(@NonNull String target_file_path)
	{
		String string="abcdefghijklmnopqrstuvwxyz";
		OutputStream outStream=null;
		try
		{
			UsbFile targetUsbFile=  getUsbFile(MainActivity.usbFileRoot,target_file_path);
			if (targetUsbFile != null)
			{
				outStream=UsbFileStreamFactory.createBufferedOutputStream(targetUsbFile,MainActivity.usbCurrentFs);
				outStream.write(string.getBytes(StandardCharsets.UTF_8));
			}
			else
			{
				return false;
			}

		}
		catch (Exception e)
		{
			return false;
		}
		finally
		{
			try
			{
				if(outStream!=null)outStream.close();
			}
			catch (Exception e)
			{
				// ignore exception
			}

		}
		return true;
	}
	public static UsbFile getUsbFile(UsbFile rootUsbFile,String file_path)
	{
		if(rootUsbFile==null) return null;
		UsbFile usbFile=null;
		try
		{
			usbFile=rootUsbFile.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path));
		}

		catch (IOException | ConcurrentModificationException e)
		{
			return usbFile;
		}
		return usbFile;
	}

	public static boolean isFtpFileExists(String file_path) {
		Timber.tag(TAG).d("Checking if FTP file exists: %s", file_path);
		FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
		FTPClient ftpClient=null;
		try {
			ftpClient = ftpClientRepository.getFtpClient();
			ftpClient.enterLocalPassiveMode();

			String parentDir = new File(file_path).getParent();
			String fileName = new File(file_path).getName();

			FTPFileFilter filter = ftpFile -> ftpFile.getName().equals(fileName);

			FTPFile[] files = ftpClient.listFiles(parentDir, filter);

			boolean exists = files.length > 0;

			Timber.tag(TAG).d("FTP file exists result: %b for path: %s", exists, file_path);
			return exists;
		} catch (IOException e) {
			Timber.tag(TAG).e("Error checking if FTP file exists: %s", e.getMessage());
			return false;
		}
		finally {
			if (ftpClientRepository != null && ftpClient != null) {
				ftpClientRepository.releaseFtpClient(ftpClient);
				Timber.tag(TAG).d("FTP client released");
			}
		}
	}

	public static boolean isFtpPathDirectory(String filePath) {
		Timber.tag(TAG).d("Checking if FTP path is directory: %s", filePath);
		FtpClientRepository ftpClientRepository = null;
		FTPClient ftpClient = null;
		try {
			ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
			ftpClient = ftpClientRepository.getFtpClient();
			boolean isDirectory = ftpClient.changeWorkingDirectory(filePath);
			Timber.tag(TAG).d("FTP path is directory result: %b for path: %s", isDirectory, filePath);
			return isDirectory;
		} catch (IOException e) {
			Timber.tag(TAG).e("Error checking if FTP path is directory: %s", e.getMessage());
			return false;
		} finally {
			if (ftpClientRepository != null && ftpClient != null) {
				ftpClientRepository.releaseFtpClient(ftpClient);
			}
		}
	}

	public static FTPFile getFTPFileFromOtherFTPClient(FTPClient ftpClient, String file_path) {
		Timber.tag(TAG).d("Getting FTP file from other FTP client: %s", file_path);
		File file = new File(file_path);
		String parent_path = file.getParent();
		String name = file.getName();
		try {
			FTPFile[] ftpFiles_array = ftpClient.listFiles(parent_path);
			int size = ftpFiles_array.length;
			for (int i = 0; i < size; ++i) {
				FTPFile ftpFile = ftpFiles_array[i];
				if (ftpFile.getName().equals(name)) {
					Timber.tag(TAG).d("Found FTP file: %s", ftpFile.getName());
					return ftpFile;
				}
			}
		} catch (Exception e) {
			Timber.tag(TAG).e("Error getting FTP file: %s", e.getMessage());
			return null;
		}
		Timber.tag(TAG).d("FTP file not found: %s", file_path);
		return null;
	}

	public static boolean renameUsbFile(UsbFile usbFile,String new_name)
	{
		if(usbFile==null) return false;
		try {
			usbFile.setName(new_name);
			return true;

		} catch (IOException e) {
			return false;
		}
	}

	public static boolean mkdirUsb(UsbFile parentUsbFile, String name)
	{
		if(parentUsbFile==null) return false;
		try {
			parentUsbFile.createDirectory(name);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean mkdirFtp(String file_path) {
		Timber.tag(TAG).d("Attempting to create FTP directory: %s", file_path);
		FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
		FTPClient ftpClient=null;
		try {

			boolean dirExists = FileUtil.isFtpPathDirectory(file_path);
			if (dirExists) {
				Timber.tag(TAG).d("FTP directory already exists: %s", file_path);
				return true;
			} else {
				ftpClient = ftpClientRepository.getFtpClient();
				boolean success = ftpClient.makeDirectory(file_path);
				Timber.tag(TAG).d("FTP directory creation result: %b for path: %s", success, file_path);
				return success;
			}
		} catch (IOException e) {
			Timber.tag(TAG).e("Error creating FTP directory: %s", e.getMessage());
			return false;
		}
		finally {
			if (ftpClientRepository != null && ftpClient != null) {
				ftpClientRepository.releaseFtpClient(ftpClient);
				Timber.tag(TAG).d("FTP client released");
			}
		}
	}


	public static boolean createUsbFile(UsbFile parentUsbFile,String name)
	{
		if(parentUsbFile==null) return false;
		try {
			parentUsbFile.createFile(name);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@SuppressWarnings("null")
	public static FileOutputStream get_file_outputstream(Context context, @NonNull final String target_file_path, Uri tree_uri, String tree_uri_path)
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

	public static boolean deleteUsbFile(UsbFile usbFile)
	{
		if(usbFile==null) return false;
		try {
			if(!usbFile.isDirectory() && usbFile.getLength()==0)
			{
				boolean madeNonZero=FileUtil.make_UsbFile_non_zero_length(usbFile.getAbsolutePath());
				if(madeNonZero)
				{
					usbFile.delete();
					return true;
				}
			}
			else
			{
				usbFile.delete();
				return true;
			}


		} catch (IOException e) {
			return false;
		}
		return false;
	}

	private static boolean deleteFTPFile(String file_path) {
		Timber.tag(TAG).d("Attempting to delete FTP file: %s", file_path);
		if (Global.CHECK_FTP_SERVER_CONNECTED()) {
			FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
			FTPClient ftpClient=null;
			try {
				ftpClient = ftpClientRepository.getFtpClient();
				boolean success = ftpClient.deleteFile(file_path);
				Timber.tag(TAG).d("FTP file deletion result: %b for path: %s", success, file_path);
				return success;
			} catch (IOException e) {
				Timber.tag(TAG).e("Error deleting FTP file: %s", e.getMessage());
				return false;
			}
			finally {
				if (ftpClientRepository != null && ftpClient != null) {
					ftpClientRepository.releaseFtpClient(ftpClient);
					Timber.tag(TAG).d("FTP client released");
				}
			}
		}
		Timber.tag(TAG).w("FTP server not connected, cannot delete file: %s", file_path);
		return false;
	}

	public static boolean deleteFileModel(final FileModel fileModel)
	{
		boolean success;
		if (fileModel.isDirectory())            //Check if folder file is a real folder
		{
			FileModel[] list = fileModel.list(); //Storing all file name within array
			if(list!=null)
			{
				int size=list.length;
				for (int i = 0; i < size; ++i)
				{
					FileModel tmpF = list[i];
					success=deleteFileModel(tmpF);

				}
			}
		}
		success=fileModel.delete();
		return success;
	}
	public static boolean deleteNativeDirectory(final File folder)
	{
		boolean success=true;

		if (folder.isDirectory())            //Check if folder file is a real folder
		{
			File[] list = folder.listFiles(); //Storing all file name within array
			if (list != null)                //Checking listUri value is null or not to check folder containts atlest one file
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
		boolean success;
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
		if(folder==null)return false;
		boolean success;

		if (folder.isDirectory())            //Check if folder file is a real folder
		{
			UsbFile[] list = new UsbFile[0]; //Storing all file name within array
			try {
				list = folder.listFiles();
			} catch (IOException e) {
			}
			if (list != null)                //Checking listUri value is null or not to check folder contains atlest one file
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

	public static boolean deleteFtpDirectory(final String file_path) {
		Timber.tag(TAG).d("Attempting to delete FTP directory: %s", file_path);
		boolean success = true;
		FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
		FTPClient ftpClient=null;
		try {
			ftpClient = ftpClientRepository.getFtpClient();
			if (FileUtil.isFtpPathDirectory(file_path)) {
				String[] list = ftpClient.listNames(file_path);
				if (list != null) {
					int size = list.length;
					for (int i = 0; i < size; ++i) {
						success = deleteFtpDirectory(list[i]);
						if (!success) break;
					}
				}
			}

			if (success) {
				if (FileUtil.isFtpPathDirectory(file_path)) {
					success = ftpClient.removeDirectory(file_path);
				} else {
					success = ftpClient.deleteFile(file_path);
				}
			}
			Timber.tag(TAG).d("FTP directory deletion result: %b for path: %s", success, file_path);
		} catch (IOException e) {
			Timber.tag(TAG).e("Error deleting FTP directory: %s", e.getMessage());
			return false;
		}
		finally {
			if (ftpClientRepository != null && ftpClient != null) {
				ftpClientRepository.releaseFtpClient(ftpClient);
				Timber.tag(TAG).d("FTP client released");
			}
		}
		return success;
	}

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

	public static boolean mkdirsFTP(String parent_file_path, @NonNull String path) {
		Timber.tag(TAG).d("Attempting to create multiple FTP directories: %s in %s", path, parent_file_path);
		boolean success = true;
		String[] file_path_substring = path.split("/");
		int size = file_path_substring.length;
		for (int i = 0; i < size; ++i) {
			String path_string = file_path_substring[i];
			if (!path_string.equals("")) {
				String new_dir_path = Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path, path_string);
				success = mkdirFtp(new_dir_path);
				parent_file_path += File.separator + path_string;
				if (!success) {
					Timber.tag(TAG).w("Failed to create FTP directory: %s", new_dir_path);
					return false;
				}
			}
		}
		Timber.tag(TAG).d("Successfully created multiple FTP directories");
		return success;
	}


	public static boolean isFromInternal(FileObjectType fileObjectType, @NonNull final String file_path)
	{
		if(!fileObjectType.equals(FileObjectType.FILE_TYPE) && !fileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE)) return  false;
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		for(String internal_storage_path:repositoryClass.internal_storage_path_list)
		{
			if (Global.IS_CHILD_FILE(file_path,internal_storage_path)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isFilePathFromExternalStorage(FileObjectType fileObjectType,String file_path)
	{
		if(!fileObjectType.equals(FileObjectType.FILE_TYPE) && !fileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE)) return  false;
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		for(String external_path : repositoryClass.external_storage_path_list)
		{
			if(Global.IS_CHILD_FILE(file_path,external_path))
				return true;
		}
		return false;
	}



	public static boolean isWritable(FileObjectType fileObjectType,@NonNull final String file_path)
	{
		return isFromInternal(fileObjectType,file_path);
	}

	public static String humanReadableByteCount(long bytes)
	{
		if (bytes < 1024) return bytes + " B";
		int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
		return String.format("%.2f %sB", (double)bytes / (1L << (z*10)), " KMGTPE".charAt(z));
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
			//Timber.e(Application.TAG, "Could not get SD directory", ioe);
		}
		return sdCardDirectory;
	}

	/**
	 * Get a listUri of external SD card paths. (Kitkat or higher.)
	 *
	 * @return A listUri of external SD card paths.
	 */
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
					//.Timber.w(Application.TAG, "Unexpected external file dir: " + file.getAbsolutePath());
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
	public static String getExtSdCardFolder(@NonNull final File file,Context context)
	{
		String[] extSdPaths = getExtSdCardPaths(context);
		try
		{
			for (String extSdPath : extSdPaths)
			{
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

		if (!documentPath.isEmpty())
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


	public static void bufferedCopy(InputStream inputStream, OutputStream outputStream, boolean fromUsbFile, long[] bytes_read)
	{
		byte[] buffer=(fromUsbFile) ? new byte[USB_CHUNK_SIZE] : new byte[BUFFER_SIZE];
		int count;
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream); BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
			while ((count = bufferedInputStream.read(buffer)) != -1) {
				bufferedOutputStream.write(buffer, 0, count);
				bytes_read[0]+=count;
			}
		} catch (IOException e) {

		}

	}

}
	
