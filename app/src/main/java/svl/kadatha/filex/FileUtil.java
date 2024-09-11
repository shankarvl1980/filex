package svl.kadatha.filex;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
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
import java.util.Stack;

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

	public static boolean copy_File_FileModel(@NonNull final File sourceFile, @NonNull final FileModel destFileModel, String child_name, boolean cut, long[] bytes_read) {
		FileInputStream fileInputStream = null;
		OutputStream outputStream = null;
		boolean success = false;

		try {
			fileInputStream = new FileInputStream(sourceFile);
			outputStream = destFileModel.getChildOutputStream(child_name, 0);

			bufferedCopy(fileInputStream, outputStream, false, bytes_read);

			// Ensure FTP transfer is completed if it's an FTP output stream
			if (outputStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
				((FtpFileModel.FTPOutputStreamWrapper) outputStream).completePendingCommand();
			}

			if (cut) {
				sourceFile.delete();
			}

			success = true;
		} catch (Exception e) {
			Timber.tag(TAG).e("Error during file copy: %s", e.getMessage());
		} finally {
			// Close streams
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					Timber.tag(TAG).e("Error closing input stream: %s", e.getMessage());
					success = false;  // Mark as failed if we couldn't close the input stream
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					Timber.tag(TAG).e("Error closing output stream: %s", e.getMessage());
					success = false;  // Mark as failed if we couldn't close the output stream
				}
			}
		}

		return success;
	}


	public static boolean copy_FileModel_FileModel(@NonNull final FileModel sourceFileModel, @NonNull final FileModel destFileModel, String child_name, boolean cut, long[] bytes_read) {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		boolean success = false;

		try {
			inputStream = sourceFileModel.getInputStream();
			outputStream = destFileModel.getChildOutputStream(child_name, 0);

			boolean fromUsbFile = sourceFileModel instanceof UsbFileModel;
			bufferedCopy(inputStream, outputStream, fromUsbFile, bytes_read);

			// Ensure FTP transfer is completed if it's an FTP output stream
			if (outputStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
				((FtpFileModel.FTPOutputStreamWrapper) outputStream).completePendingCommand();
			}

			if (cut) {
				sourceFileModel.delete();
			}

			success = true;
		} catch (Exception e) {
			Timber.tag(TAG).e("Error during file copy: %s", e.getMessage());
		} finally {
			// Close streams
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					Timber.tag(TAG).e("Error closing input stream: %s", e.getMessage());
					success = false;  // Mark as failed if we couldn't close the input stream
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					Timber.tag(TAG).e("Error closing output stream: %s", e.getMessage());
					success = false;  // Mark as failed if we couldn't close the output stream
				}
			}
		}

		return success;
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

	public static boolean CopyUriFileModel(@NonNull Uri data, FileModel destFileModel, String file_name, long[] bytes_read) {
		InputStream inStream = null;
		OutputStream fileOutStream = null;

        try {
			inStream = App.getAppContext().getContentResolver().openInputStream(data);
			fileOutStream = destFileModel.getChildOutputStream(file_name, 0);

			bufferedCopy(inStream, fileOutStream, false, bytes_read);

			// Ensure FTP transfer is completed if it's an FTP output stream
			if (fileOutStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
				((FtpFileModel.FTPOutputStreamWrapper) fileOutStream).completePendingCommand();
			}

			return true;
		} catch (Exception e) {
			Timber.tag(TAG).e("Error during URI to FileModel copy: %s", e.getMessage());
			return false;

		} finally {
			// Close streams
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					Timber.tag(TAG).e("Error closing input stream: %s", e.getMessage());
                    // Mark as failed if we couldn't close the input stream
                }
			}
			if (fileOutStream != null) {
				try {
					fileOutStream.close();
				} catch (IOException e) {
					Timber.tag(TAG).e("Error closing output stream: %s", e.getMessage());
                    // Mark as failed if we couldn't close the output stream
                }
			}
		}

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

	public static FTPFile getFtpFile(FTPClient ftpClient, String file_path) {
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

	private static boolean deleteNativeFile(@NonNull final File file)
	{
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

	public static boolean deleteFileModel(final FileModel fileModel) {
		if (fileModel == null) {
			return false;
		}

		Stack<FileModel> stack = new Stack<>();
		stack.push(fileModel);
		boolean success = true;

		while (!stack.isEmpty() && success) {
			FileModel currentFile = stack.pop();

			if (currentFile.isDirectory()) {
				FileModel[] list = currentFile.list();
				if (list != null && list.length > 0) {
					// Push the current directory back onto the stack
					stack.push(currentFile);
					// Push all children onto the stack
					for (FileModel child : list) {
						stack.push(child);
					}
				} else {
					// Empty directory, try to delete it
					success = currentFile.delete();
					if (!success) {
						System.err.println("Failed to delete directory: " + currentFile);
					}
				}
			} else {
				// It's a file, try to delete it
				success = currentFile.delete();
				if (!success) {
					System.err.println("Failed to delete file: " + currentFile);
				}
			}
		}

		// If the original fileModel was a directory and we've successfully deleted all its contents,
		// we need to delete the directory itself
		if (success && fileModel.isDirectory() && fileModel.exists()) {
			success = fileModel.delete();
			if (!success) {
				System.err.println("Failed to delete root directory: " + fileModel);
			}
		}

		return success;
	}



	public static boolean deleteNativeDirectory(final File folder) {
		if (folder == null || !folder.exists()) {
			return false;
		}

		Stack<File> stack = new Stack<>();
		stack.push(folder);
		boolean success = true;

		while (!stack.isEmpty() && success) {
			File current = stack.pop();

			if (current.isDirectory()) {
				File[] list = current.listFiles();
				if (list != null && list.length > 0) {
					// Push the current directory back onto the stack
					stack.push(current);
					// Push all children onto the stack
					for (File child : list) {
						stack.push(child);
					}
				} else {
					// Empty directory, try to delete it
					success = deleteNativeFile(current);
					if (!success) {
						System.err.println("Failed to delete directory: " + current);
					}
				}
			} else {
				// It's a file, try to delete it
				success = deleteNativeFile(current);
				if (!success) {
					System.err.println("Failed to delete file: " + current);
				}
			}
		}

		// If the original folder still exists (it was not empty initially),
		// we need to delete it now
		if (success && folder.exists()) {
			success = deleteNativeFile(folder);
			if (!success) {
				System.err.println("Failed to delete root folder: " + folder);
			}
		}

		return success;
	}


	public static boolean deleteSAFDirectory(Context context, final String file_path, Uri tree_uri, String tree_uri_path) {
		File folder = new File(file_path);
		if (!folder.exists()) {
			return false;
		}

		Stack<File> stack = new Stack<>();
		stack.push(folder);
		boolean success = true;

		while (!stack.isEmpty() && success) {
			File current = stack.pop();

			if (current.isDirectory()) {
				File[] list = current.listFiles();
				if (list != null && list.length > 0) {
					// Push the current directory back onto the stack
					stack.push(current);
					// Push all children onto the stack
					for (File child : list) {
						stack.push(child);
					}
				} else {
					// Empty directory, try to delete it
					success = deleteSAFFile(context, current.getAbsolutePath(), tree_uri, tree_uri_path);
					if (!success) {
						System.err.println("Failed to delete directory: " + current.getAbsolutePath());
					}
				}
			} else {
				// It's a file, try to delete it
				success = deleteSAFFile(context, current.getAbsolutePath(), tree_uri, tree_uri_path);
				if (!success) {
					System.err.println("Failed to delete file: " + current.getAbsolutePath());
				}
			}
		}

		// If the original folder still exists (it was not empty initially),
		// we need to delete it now
		if (success && folder.exists()) {
			success = deleteSAFFile(context, folder.getAbsolutePath(), tree_uri, tree_uri_path);
			if (!success) {
				System.err.println("Failed to delete root folder: " + folder.getAbsolutePath());
			}
		}

		return success;
	}

	public static boolean deleteUsbDirectory(final UsbFile folder) {
		if (folder == null) {
			return false;
		}

		Stack<UsbFile> stack = new Stack<>();
		stack.push(folder);
		boolean success = true;

		while (!stack.isEmpty() && success) {
			UsbFile current = stack.pop();

			if (current.isDirectory()) {
				UsbFile[] list = new UsbFile[0];
				try {
					list = current.listFiles();
				} catch (IOException e) {
					System.err.println("Error listing files: " + e.getMessage());
					success = false;
					continue;
				}

				if (list != null && list.length > 0) {
					// Push the current directory back onto the stack
					stack.push(current);
					// Push all children onto the stack
					for (UsbFile child : list) {
						stack.push(child);
					}
				} else {
					// Empty directory, try to delete it
					success = deleteUsbFile(current);
					if (!success) {
						System.err.println("Failed to delete directory: " + current.getName());
					}
				}
			} else {
				// It's a file, try to delete it
				success = deleteUsbFile(current);
				if (!success) {
					System.err.println("Failed to delete file: " + current.getName());
				}
			}
		}

		// If the original folder still exists (it was not empty initially),
		// we need to delete it now
		if (success && folder.isDirectory()) {
			success = deleteUsbFile(folder);
			if (!success) {
				System.err.println("Failed to delete root folder: " + folder.getName());
			}
		}

		return success;
	}


	public static boolean deleteFtpDirectory(final String file_path) {
		Timber.tag(TAG).d("Attempting to delete FTP directory: %s", file_path);
		FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
		FTPClient ftpClient = null;
		boolean success = true;

		try {
			ftpClient = ftpClientRepository.getFtpClient();
			if (ftpClient == null) {
				throw new IllegalStateException("Failed to obtain FTP client");
			}

			Stack<String> stack = new Stack<>();
			stack.push(file_path);

			while (!stack.isEmpty() && success) {
				String currentPath = stack.pop();

				if (FileUtil.isFtpPathDirectory(currentPath)) {
					String[] list = ftpClient.listNames(currentPath);
					if (list != null && list.length > 0) {
						for (String item : list) {
							stack.push(item);
						}
					} else {
						success = ftpClient.removeDirectory(currentPath);
					}
				} else {
					success = ftpClient.deleteFile(currentPath);
				}

				if (!success) {
					Timber.tag(TAG).e("Failed to delete: %s", currentPath);
				}
			}

			// If the original path was a directory and all contents were successfully deleted, delete the directory itself
			if (success && FileUtil.isFtpPathDirectory(file_path)) {
				success = ftpClient.removeDirectory(file_path);
			}

			Timber.tag(TAG).d("FTP directory deletion result: %b for path: %s", success, file_path);
		} catch (IOException e) {
			Timber.tag(TAG).e("Error deleting FTP directory: %s", e.getMessage());
			success = false;
		} catch (IllegalStateException e) {
			Timber.tag(TAG).e("Failed to obtain FTP client: %s", e.getMessage());
			success = false;
		} finally {
			if (ftpClientRepository != null && ftpClient != null) {
				ftpClientRepository.releaseFtpClient(ftpClient);
				Timber.tag(TAG).d("FTP client released");
			}
		}
		return success;
	}

	public static boolean renameNativeFile(@NonNull final File source, @NonNull final File target)
	{
		if (source.renameTo(target)) return true;

		if (target.exists()) return false;

		return false;
	}


	public static boolean renameSAFFile(Context context, String target_file_path, String new_name, Uri tree_uri, String tree_uri_path)
	{
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

	public static boolean isOnExtSdCard(@NonNull final File file,Context context)
	{
		return getExtSdCardFolder(file,context) != null;
	}


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


	public static void bufferedCopy(InputStream inputStream, OutputStream outputStream, boolean fromUsbFile, long[] bytes_read) throws IOException {
		byte[] buffer = (fromUsbFile) ? new byte[USB_CHUNK_SIZE] : new byte[BUFFER_SIZE];
		int count;
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
			while ((count = bufferedInputStream.read(buffer)) != -1) {
				bufferedOutputStream.write(buffer, 0, count);
				bytes_read[0] += count;
			}
			bufferedOutputStream.flush(); // Explicit flush at the end of the transfer
		} catch (IOException e) {
			Timber.tag(TAG).e("Error during buffered copy: %s", e.getMessage());
			throw e; // Re-throw the exception to be handled by the caller
		}
	}

}
	
