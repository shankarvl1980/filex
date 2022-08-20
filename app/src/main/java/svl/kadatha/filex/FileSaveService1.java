package svl.kadatha.filex;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FileSaveService1 extends Service
{
	private FileSaveServiceBinder binder=new FileSaveServiceBinder();
	private FileSaveServiceCompletionListener fileSaveServiceCompletionListener;
	private boolean isWritable;
	private File file,temporary_file_for_save;
	private String content,tree_uri_path;
	private Uri tree_uri;
	private int eol,altered_eol;
	private Context context;
	private int current_page;
	long prev_page_end_point,current_page_end_point;
	final LinkedHashMap<Integer, Long> page_pointer_hashmap=new LinkedHashMap<>();
	private NotifManager nm;
    static boolean SERVICE_COMPLETED=true;
	private Handler handler;
	
	
	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		super.onCreate();
		SERVICE_COMPLETED=false;
		context=this;
		nm=new NotifManager(context);
		handler=new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// TODO: Implement this method
		Bundle bundle=intent.getBundleExtra("bundle");
		if(bundle!=null)
		{
			isWritable=bundle.getBoolean("isWritable");
			file=new File(bundle.getString("file_path"));
			content=bundle.getString("content");
			tree_uri_path=bundle.getString("tree_uri_path");
			tree_uri=bundle.getParcelable("tree_uri");
			eol=bundle.getInt("eol");
			altered_eol=bundle.getInt("altered_eol");
			prev_page_end_point=bundle.getLong("prev_page_end_point");
			current_page_end_point=bundle.getLong("current_page_end_point");
			HashMap<Integer,Long>temp  =(HashMap<Integer,Long>)bundle.getSerializable("page_pointer_hashmap");
			page_pointer_hashmap.putAll(temp);
			temporary_file_for_save=new File(bundle.getString("temporary_file_path"));
			current_page=bundle.getInt("current_page");

			filesave();
            int notification_id = 980;
            startForeground(notification_id,nm.build(getString(R.string.being_updated)+"-"+"'"+file.getName()+"'", notification_id));
		
		}
		else
		{
			SERVICE_COMPLETED=true;
			stopSelf();
		}
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent p1)
	{
		// TODO: Implement this method
		if(binder==null)
		{
			binder=new FileSaveServiceBinder();
		}
		return binder;
	}
	
	class FileSaveServiceBinder extends Binder
	{
		public FileSaveService1 getService()
		{
			return FileSaveService1.this;
		}
	}

	@Override
	public void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		SERVICE_COMPLETED=true;
	}
	

	private void filesave()
	{
		String eol_string = null;
		if(file==null || !file.exists())
		{
			return;
		}

		switch(altered_eol)
		{

			case FileEditorActivity.EOL_N:
				eol_string="\n";
				break;
			case FileEditorActivity.EOL_R:
				eol_string="\r";
				break;
			case FileEditorActivity.EOL_RN:
				eol_string="\r\n";
				break;
		}

		String finalEol_string = eol_string;
		ExecutorService executorService=MyExecutorService.getExecutorService();
		Future future = executorService.submit(new Runnable() {
			@Override
			public void run() {
				if(!finalEol_string.equals("\n"))
				{
					content=content.replaceAll("\n",finalEol_string);
				}

				boolean result;
				FileOutputStream fileOutputStream;
				if (isWritable) {
					if (eol == altered_eol) {
						result = save_file(null, prev_page_end_point, current_page_end_point, content.getBytes());
					} else {
						result = save_file_with_altered_eol(null, prev_page_end_point, current_page_end_point, content, finalEol_string);
					}

				} else {

					fileOutputStream = FileUtil.get_fileoutputstream(context, file.getAbsolutePath(), tree_uri, tree_uri_path);
					if (fileOutputStream != null) {
						if (eol == altered_eol) {
							result = save_file(fileOutputStream, prev_page_end_point, current_page_end_point, content.getBytes());

						} else {
							result = save_file_with_altered_eol(fileOutputStream, prev_page_end_point, current_page_end_point, content, finalEol_string);
						}


					} else {
						result = false;
					}


				}

				handler.post(new Runnable() {
					@Override
					public void run() {
						if (fileSaveServiceCompletionListener != null) {
							fileSaveServiceCompletionListener.onServiceCompletion(result);
						}
						stopForeground(true);
						stopSelf();
						SERVICE_COMPLETED = true;

					}
				});
			}
		});
	}


	private boolean save_file(FileOutputStream fileOutputStream,long prev_page_end_point, long current_page_end_point, byte[] content)
	{
		FileChannel source_fc=null,temp_fc=null,r_fc=null;
		try
		{
			long length=file.length();

			RandomAccessFile r_raf=new RandomAccessFile(file,"r");
			File temp_file=new File(temporary_file_for_save,file.getName());
			RandomAccessFile temp_raf=new RandomAccessFile(temp_file,"rw");

			r_fc=r_raf.getChannel();

			temp_fc=temp_raf.getChannel();

			if(length==current_page_end_point)
			{
				r_fc.transferTo(prev_page_end_point, 0,temp_fc);
			}
			else
			{
				r_fc.transferTo(current_page_end_point,length-current_page_end_point,temp_fc);
			}
			r_raf.close();
			
			if(isWritable)
			{
				FileOutputStream outputStream=new FileOutputStream(file,true);
				source_fc=outputStream.getChannel();
			}
			else
			{
				source_fc=fileOutputStream.getChannel();
			}

			source_fc.truncate(prev_page_end_point);
			source_fc.position(prev_page_end_point);
			ByteBuffer buf=ByteBuffer.wrap(content);
			long writtenbytes=source_fc.write(buf);


			buf.compact();
			buf.flip();
			if(buf.hasRemaining())
			{
				writtenbytes+=source_fc.write(buf);
			}
			long new_offset=source_fc.position();


			temp_fc.position(0L);
			source_fc.transferFrom(temp_fc,new_offset,temp_fc.size());
			current_page_end_point=new_offset;
			page_pointer_hashmap.put(current_page,current_page_end_point);

			
			temp_fc.close();

			if(temp_file.exists())
			{
				temp_file.delete();
			}


			return true;

		}
		catch(IOException | NullPointerException | IllegalArgumentException e)
		{
			return false;
		}
		finally
		{
			try
			{

				temp_fc.close();
				r_fc.close();
				if(fileOutputStream!=null)
				{
					fileOutputStream.close();
				}
				source_fc.close();
			}
			catch(IOException | NullPointerException e)
			{

			}
			
			
		}

	}
	
	private boolean save_file_with_altered_eol(FileOutputStream fileOutputStream,long prev_page_end_point, long current_page_end_point, String content, String eol_string)
	{
		BufferedReader bufferedReader=null;
		BufferedWriter bufferedWriter=null;
		FileChannel fc=null;

		try
		{
			FileInputStream fileInputStream=new FileInputStream(file);
			bufferedReader=new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));


			bufferedReader.skip(current_page_end_point);
			File temp_file_2=new File(temporary_file_for_save,file.getName()+"_2");
			bufferedWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp_file_2,true), StandardCharsets.UTF_8));
			String line;
			while((line=bufferedReader.readLine())!=null)
			{

				bufferedWriter.write(line+eol_string);
				bufferedWriter.flush();
			}

			bufferedWriter.close();
			bufferedReader.close();


			if(isWritable)
			{
				fc=new FileOutputStream(file,true).getChannel();
			}
			else
			{
				fc=fileOutputStream.getChannel();
			}

			fc.truncate(prev_page_end_point);

			fileInputStream=new FileInputStream(file);
			bufferedReader=new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));

			File temp_file_1=new File(temporary_file_for_save,file.getName()+"_1");
			bufferedWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp_file_1,true), StandardCharsets.UTF_8));
			while((line=bufferedReader.readLine())!=null)
			{

				bufferedWriter.write(line+eol_string);
				bufferedWriter.flush();
			}

			bufferedWriter.write(content);
			bufferedWriter.flush();
			bufferedWriter.close();

			bufferedReader.close();

			fc.truncate(0L);

			current_page_end_point=temp_file_1.length();
			FileChannel first_part_fc=new FileInputStream(temp_file_1).getChannel();
			fc.transferFrom(first_part_fc,0,current_page_end_point);


			FileChannel second_part_fc=new FileInputStream(temp_file_2).getChannel();
			fc.transferFrom(second_part_fc,current_page_end_point,temp_file_2.length());

			first_part_fc.close();
			second_part_fc.close();

			page_pointer_hashmap.put(current_page,current_page_end_point);
			temp_file_1.delete();
			temp_file_2.delete();

		} catch(IOException e)
		{

			return false;
		} finally
		{
			try
			{

				bufferedWriter.close();
				bufferedReader.close();
				fc.close();
				
				if(fileOutputStream!=null)
				{
					fileOutputStream.close();
				}

			}
			catch(IOException e)
			{

			}

		}
		return true;
	}
	
	interface FileSaveServiceCompletionListener
	{
		void onServiceCompletion(boolean result);
	}
	
	public void setServiceCompletionListener(FileSaveServiceCompletionListener listener)
	{
		fileSaveServiceCompletionListener=listener;
	}
}
