package svl.kadatha.filex;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExtractZipFile
{
	static boolean read_zipentry(Context context,ZipFile zipfile,ZipEntry zipEntry,File ZipDestFolder)
	{
		InputStream inStream=null;

		try
		{

			inStream=zipfile.getInputStream(zipEntry);
			BufferedInputStream bufferedinStream=new BufferedInputStream(inStream);
			File dir=new File(ZipDestFolder.getAbsolutePath()+File.separator+zipEntry.getName());
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

