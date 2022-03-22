package svl.kadatha.filex;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class FileIntentDispatch
{
	static final  String EXTRA_FROM_ARCHIVE="fromArchiveView";
	static final String EXTRA_FILE_OBJECT_TYPE="fileObjectType";
	static final String EXTRA_FILE_PATH="file_path";

	public static void openFile(Context context,String file_path, String mime_type,boolean clear_top,boolean fromArchiveView,FileObjectType fileObjectType)
	{
		open_file(context,file_path,mime_type,clear_top,fromArchiveView,fileObjectType);
		//OpenFileIntentDispatchAsyncTask openFileIntentDispatchAsyncTask=new OpenFileIntentDispatchAsyncTask(context,file_path,mime_type,clear_top,fromArchiveView,fileObjectType);
		//openFileIntentDispatchAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void openUri(Context context,String file_path,String mime_type,boolean clear_top,boolean fromArchiveView,FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path)
	{
		open_uri(context,file_path,mime_type,clear_top,fromArchiveView,fileObjectType,tree_uri,tree_uri_path);
		//OpenUriIntentDispatchAsyncTask openUriIntentDispatchAsyncTask=new OpenUriIntentDispatchAsyncTask(context,file_path,mime_type,clear_top,fromArchiveView,fileObjectType,tree_uri,tree_uri_path);
		//openUriIntentDispatchAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	public static void sendFile(Context context, ArrayList<File> file_list)
	{
		send_file(context,file_list);
		//SendFileIntentDispatchAsyncTask sendFileIntentDispatchAsyncTask=new SendFileIntentDispatchAsyncTask(context,file_list);
		//sendFileIntentDispatchAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void sendUri(Context context, ArrayList<Uri> uri_list)
	{
		send_uri(context,uri_list);
		//SendUriIntentDispatchAsyncTask sendUriIntentDispatchAsyncTask=new SendUriIntentDispatchAsyncTask(context,uri_list);
		//sendUriIntentDispatchAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
    private static void open_file(Context context, String file_path,String mime_type,boolean clear_top,  boolean fromArchiveView,FileObjectType fileObjectType)
	{
		String file_extn=""; Uri uri;
		int file_extn_idx=file_path.lastIndexOf(".");
		if(file_extn_idx!=-1)
		{
			file_extn=file_path.substring(file_extn_idx+1);
		}

		File file=new File(file_path);
		uri = FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",file);
		despatch_intent(context,uri,file_path,file_extn,mime_type,clear_top,fromArchiveView,fileObjectType);

	}

	private static void open_uri(Context context, String file_path,String mime_type,boolean clear_top, boolean fromArchiveView,FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path)
	{
		String file_extn="";
		int file_extn_idx=file_path.lastIndexOf(".");
		if(file_extn_idx!=-1)
		{
			file_extn=file_path.substring(file_extn_idx+1);
		}
		Uri uri=FileUtil.getDocumentUri(file_path,tree_uri,tree_uri_path);
		despatch_intent(context,uri,file_path,file_extn,mime_type,clear_top,fromArchiveView,fileObjectType);
	}

	private static void despatch_intent(final Context context,Uri uri, String file_path, String file_extn, String mime_type, boolean clear_top, boolean fromArchiveView, FileObjectType fileObjectType)
	{
		final Intent intent=new Intent(Intent.ACTION_VIEW);
		mime_type=SET_INTENT_FOR_VIEW(intent,mime_type,file_path,file_extn,fileObjectType,fromArchiveView,clear_top,uri);
		if(mime_type==null || mime_type.equals("")) return;

		DefaultAppDatabaseHelper defaultAppDatabaseHelper=new DefaultAppDatabaseHelper(context);
		final String package_name=defaultAppDatabaseHelper.getPackageName(mime_type);
		if(package_name==null)
		{
			launch_app_selector_dialog(context,uri,file_path,mime_type, clear_top, fromArchiveView,fileObjectType);
		}
		else
		{
			final List<ResolveInfo> resolveInfoList=context.getPackageManager().queryIntentActivities(intent,0);
			final int size=resolveInfoList.size();
			boolean package_found=false;

			for(int i=0; i<size;++i)
			{
				final ResolveInfo resolveInfo=resolveInfoList.get(i);
				final String resolved_app_package_name=resolveInfo.activityInfo.packageName;
				if(resolved_app_package_name.equals(package_name))
				{
					if(mime_type.equals("application/vnd.android.package-archive"))
					{
						AppInstallAlertDialog appInstallAlertDialog = AppInstallAlertDialog.getInstance(file_path);
						appInstallAlertDialog.setAppInstallDialogListener(new AppInstallAlertDialog.AppInstallDialogListener() {
							@Override
							public void on_ok_click() {
								if(Global.FILEX_PACKAGE.equals(package_name))
								{
									AppCompatActivity appCompatActivity=(AppCompatActivity)context;
									if(appCompatActivity instanceof MainActivity)
									{
										((MainActivity)context).clear_cache=false;
									}
									else if(appCompatActivity instanceof StorageAnalyserActivity)
									{
										((StorageAnalyserActivity)context).clear_cache=false;
									}
								}

								intent.setPackage(package_name);
								context.startActivity(intent);
							}

						});

						appInstallAlertDialog.show(((AppCompatActivity)context).getSupportFragmentManager(),"");
					}
					else
					{
						if(Global.FILEX_PACKAGE.equals(package_name))
						{
							AppCompatActivity appCompatActivity=(AppCompatActivity)context;
							if(appCompatActivity instanceof MainActivity)
							{
								((MainActivity)context).clear_cache=false;
							}
							else if(appCompatActivity instanceof StorageAnalyserActivity)
							{
								((StorageAnalyserActivity)context).clear_cache=false;
							}
						}
						intent.setPackage(package_name);
						context.startActivity(intent);
					}

					package_found=true;
					break;
				}

			}

			if(!package_found)
			{
				defaultAppDatabaseHelper.delete_row(mime_type);
				launch_app_selector_dialog(context,uri,file_path,mime_type, clear_top, fromArchiveView,fileObjectType);
			}
		}
		defaultAppDatabaseHelper.close();
	}

	private static void launch_app_selector_dialog(Context context,Uri uri,String file_path,String mime_type,boolean clear_top,boolean fromArchiveView, FileObjectType fileObjectType)
	{
		AppSelectorDialog appSelectorDialog=AppSelectorDialog.getInstance(uri,file_path,mime_type,clear_top,fromArchiveView,fileObjectType);
		appSelectorDialog.show(((AppCompatActivity)context).getSupportFragmentManager(),"");
	}

	private static void send_file(Context context, ArrayList<File> file_list)
	{
		ArrayList<Uri> uri_list=new ArrayList<>();
		for(File f:file_list)
		{
			uri_list.add(FileProvider.getUriForFile(context,context.getPackageName()+".provider",f));
		}

		Intent intent=new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uri_list);
		intent.putExtra(Intent.EXTRA_SUBJECT,file_list.get(0).getName());
		intent.setType("*/*");
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent chooser=Intent.createChooser(intent,"Select app");
		if(intent.resolveActivity(context.getPackageManager())!=null)
		{
			context.startActivity(chooser);
		}
	}
	
	private static void send_uri(Context context, ArrayList<Uri> uri_list)
	{
		String extra=new File(uri_list.get(0).getPath()).getName();
		Intent intent=new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uri_list);
		intent.putExtra(Intent.EXTRA_SUBJECT,extra);
		intent.setType("*/*");
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent chooser=Intent.createChooser(intent,"Select app");
		if(intent.resolveActivity(context.getPackageManager())!=null)
		{
			context.startActivity(chooser);
		}
	}
	
	/*
	private static class OpenFileIntentDispatchAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final Context context;
		final String file_path;
		final String mime_type;
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		final boolean clear_top;
        final boolean fromArchiveView;
        final FileObjectType fileObjectType;
	
		OpenFileIntentDispatchAsyncTask(Context context,String file_path,String mime_type,boolean clear_top, boolean fromArchiveView, FileObjectType fileObjectType)
		{
			this.context=context;
			this.file_path=file_path;
			this.mime_type=mime_type;
			this.clear_top=clear_top;
			this.fromArchiveView=fromArchiveView;
			this.fileObjectType=fileObjectType;

		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			pbf.show(((AppCompatActivity)context).getSupportFragmentManager(),"progressbar_dialog");
		}
		
		
		@Override
		protected Void doInBackground(Void[] p1) 
		{
			// TODO: Implement this method
			open_file(context,file_path,mime_type,clear_top,fromArchiveView,fileObjectType);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			pbf.dismissAllowingStateLoss();
		}
	}

	private static class OpenUriIntentDispatchAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final Context context;
		final String file_path;
		final Uri tree_uri;
		final String tree_uri_path;
        final String mime_type;
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		final boolean clear_top;
        final boolean fromArchiveView;
        final FileObjectType fileObjectType;

		OpenUriIntentDispatchAsyncTask(Context context,String file_path,String mime_type,boolean clear_top, boolean fromArchiveView,FileObjectType fileObjectType,Uri tree_uri, String tree_uri_path)
		{
			this.context=context;
			this.file_path=file_path;
			this.tree_uri=tree_uri;
			this.tree_uri_path=tree_uri_path;
			this.mime_type=mime_type;
			this.clear_top=clear_top;
			this.fromArchiveView=fromArchiveView;
			this.fileObjectType=fileObjectType;

		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			pbf.show(((AppCompatActivity)context).getSupportFragmentManager(),"progressbar_dialog");
		}


		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			open_uri(context,file_path,mime_type,clear_top,fromArchiveView,fileObjectType,tree_uri,tree_uri_path);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			pbf.dismissAllowingStateLoss();
		}
	}


	private static class SendFileIntentDispatchAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final Context context;
		final ArrayList<File> file_list;
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();

		SendFileIntentDispatchAsyncTask(Context context, ArrayList<File> file_list)
		{
			this.context=context;
			this.file_list=file_list;
		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			pbf.show(((AppCompatActivity)context).getSupportFragmentManager(),"progressbar_dialog");
		}


		@Override
		protected Void doInBackground(Void[] p1) 
		{
			// TODO: Implement this method
			send_file(context,file_list);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			pbf.dismissAllowingStateLoss();
		}

	}

	private static class SendUriIntentDispatchAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final Context context;
		File file;
		final ArrayList<Uri> uri_list;
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();

		SendUriIntentDispatchAsyncTask(Context context, ArrayList<Uri> uri_list)
		{
			this.context=context;
			this.uri_list=uri_list;
		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			pbf.show(((AppCompatActivity)context).getSupportFragmentManager(),"progressbar_dialog");
		}


		@Override
		protected Void doInBackground(Void[] p1) 
		{
			// TODO: Implement this method
			send_uri(context,uri_list);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			pbf.dismissAllowingStateLoss();
		}

	}

	 */

	public static String SET_INTENT_FOR_VIEW(Intent intent,String mime_type,String file_path ,String file_extn,FileObjectType fileObjectType, boolean fromArchiveView,
	boolean clear_top, Uri uri)
	{
		if (mime_type == null || mime_type.equals("")) {
			for (MimePOJO mimePOJO : Global.MIME_POJOS) {
				if (file_extn.matches(mimePOJO.getRegex())) {
					mime_type = mimePOJO.getMime_type();
					break;
				}
			}
		}
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(uri,mime_type);

		intent.putExtra(EXTRA_FROM_ARCHIVE,fromArchiveView);
		intent.putExtra(EXTRA_FILE_OBJECT_TYPE,fileObjectType!=null ? fileObjectType.toString():null);
		intent.putExtra(EXTRA_FILE_PATH,file_path);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		if(clear_top)
		{
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		else
		{
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		return mime_type;
	}

	private static void print(String msg, Context context)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}

}

