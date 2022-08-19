package svl.kadatha.filex;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExtractZipFileViewModel extends AndroidViewModel
{

	private boolean isCancelled;
	private Future<?> future1,future2,future3, future4;
	public final MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
	public boolean isZipExtracted;
	public FilePOJO filePOJO;

	public ExtractZipFileViewModel(@NonNull Application application) {
		super(application);
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		cancel(true);
	}

	public void cancel(boolean mayInterruptRunning){
		if(future1!=null) future1.cancel(mayInterruptRunning);
		if(future2!=null) future2.cancel(mayInterruptRunning);
		if(future3!=null) future3.cancel(mayInterruptRunning);
		if(future4!=null) future4.cancel(mayInterruptRunning);
		isCancelled=true;
	}

	private boolean isCancelled()
	{
		return isCancelled;
	}

	public synchronized void extractZip(FilePOJO filePOJO,ZipFile finalZipfile, ZipEntry zip_entry)
	{
		if(Boolean.TRUE.equals(isFinished.getValue()))return;
		isZipExtracted=false;
		this.filePOJO=filePOJO;
		ExecutorService executorService=MyExecutorService.getExecutorService();
		future1=executorService.submit(new Runnable() {
			@Override
			public void run() {
				isZipExtracted=EXTRACT_ZIP(finalZipfile,zip_entry,Global.ARCHIVE_EXTRACT_DIR);
				isFinished.postValue(true);
			}
		});
	}


	public static boolean EXTRACT_ZIP(ZipFile zipfile, ZipEntry zipEntry, File ZipDestFolder)
	{
		InputStream inStream=null;

		try
		{

			inStream=zipfile.getInputStream(zipEntry);
			BufferedInputStream bufferedinStream=new BufferedInputStream(inStream);
			File dir=new File(Global.CONCATENATE_PARENT_CHILD_PATH(ZipDestFolder.getAbsolutePath(),zipEntry.getName()));
			if(zipEntry.isDirectory() && !dir.exists())
			{
				return FileUtil.mkdirsNative(dir);

			}
			else if(zipEntry.isDirectory() && dir.exists())
			{
				return true;
			}
			else
			{
				File parent_dir=dir.getParentFile();
				if(!parent_dir.exists())
				{
					FileUtil.mkdirsNative(parent_dir);
				}
				OutputStream outStream;

				outStream=new FileOutputStream(dir);



				if(outStream!=null)
				{

					BufferedOutputStream bufferedoutStream=new BufferedOutputStream(outStream);
					byte[] b=new byte[8192];
					int bytesread;
					while((bytesread=bufferedinStream.read(b))!=-1)
					{
						bufferedoutStream.write(b,0,bytesread);
					}

					bufferedoutStream.close();
					bufferedinStream.close();
					return true;
				}

			}


		}

		catch(IOException e)
		{
			return false;
		}

		finally
		{
			try
			{

				if(inStream!=null)
				{
					inStream.close();

				}

			}
			catch(Exception e)
			{
				return false;
			}

		}

		return false;
	}

}

