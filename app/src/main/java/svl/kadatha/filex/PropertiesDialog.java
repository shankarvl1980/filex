package svl.kadatha.filex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import me.jahnen.libaums.core.fs.UsbFile;

public class PropertiesDialog extends DialogFragment
{
	private Context context;
	//private final SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-yyyy hh:mm");
	private TextView no_files_textview;
	private TextView size_files_textview;
	private String filename_str,file_path_str,file_type_str,file_no_str,file_size_str,file_date_str,file_permissions_str,symbolic_link_str,readable_str,writable_str,hidden_str;
    private int total_no_of_files;
	private String size_of_files_format;
	private ArrayList<String> files_selected_array=new ArrayList<>();
	//private final ArrayList<File> files_selected_for_properties=new ArrayList<>();
	//private FileCountSize AsyncTaskFileCountSize;
	private FileObjectType fileObjectType;
	private String source_folder;
	private int size;
	public static final String PROPERTIES_DIALOG_REQUEST_CODE="properties_dialog_request_code";


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
//		setRetainInstance(true);
		Bundle bundle=getArguments();
		files_selected_array=bundle.getStringArrayList("files_selected_array");
		size=files_selected_array.size();
		source_folder=new File(files_selected_array.get(0)).getParent();
		fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
		if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE) fileObjectType=FileObjectType.FILE_TYPE;
	//	AsyncTaskFileCountSize=new FileCountSize(files_selected_array,fileObjectType);
	//	AsyncTaskFileCountSize.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		if(files_selected_array.size()==1)
		{
			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				File file=new File(files_selected_array.get(0));
				filename_str=file.getName();
				file_path_str=file.getAbsolutePath();
				file_date_str=Global.SDF.format(file.lastModified());
				file_type_str=file.isDirectory() ? getString(R.string.directory) : getString(R.string.file);
				getPermissions(file);

				readable_str=file.canRead() ? getString(R.string.yes) : getString(R.string.no);
				writable_str=file.canWrite() ? getString(R.string.yes) : getString(R.string.no);
				hidden_str=file.isHidden() ? getString(R.string.yes) : getString(R.string.no);
			}
			else if(fileObjectType==FileObjectType.USB_TYPE)
			{
				UsbFile file=FileUtil.getUsbFile(MainActivity.usbFileRoot,files_selected_array.get(0));
				filename_str=file.getName();
				file_path_str=file.getAbsolutePath();
				file_date_str=Global.SDF.format(file.lastModified());
				file_type_str=file.isDirectory() ? getString(R.string.directory) : getString(R.string.file);
				//getPermissions(file);
				readable_str=getString(R.string.yes);
				writable_str=getString(R.string.yes);
				hidden_str=getString(R.string.yes);
			}
			else if(fileObjectType==FileObjectType.FTP_TYPE)
			{
				//FTPFile ftpFile=FileUtil.getFTPFile(files_selected_array.get(0));
				filename_str=new File(files_selected_array.get(0)).getName();
				file_path_str=files_selected_array.get(0);
				//file_date_str=sdf.format(ftpFile.)
				file_type_str="";//ftpFile.isDirectory() ? getString(R.string.directory) : getString(R.string.file);

				//getPermissions(file);
				readable_str=getString(R.string.yes);
				writable_str=getString(R.string.yes);
				hidden_str=getString(R.string.yes);
			}
		}
		else if(files_selected_array.size()>1)
		{
			filename_str=files_selected_array.size()+" "+ getString(R.string.files);
			file_path_str=new File(files_selected_array.get(0)).getParent();
			file_date_str="NA";
			file_type_str="NA";
			symbolic_link_str="NA";
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_properties,container,false);
		//private TextView permissions;
		TableLayout properties_details_table_layout = v.findViewById(R.id.fragment_properties_details_table_layout);

		for(int i=0;i<6;++i)
		{
			View row_item=inflater.inflate(R.layout.properties_item_view_layout,null);
			TextView label=row_item.findViewById(R.id.properties_label);
			final TextView property=row_item.findViewById(R.id.properties_description);

			switch (i)
			{
				case 0:
					label.setText(R.string.name);
					property.setText(filename_str);
					break;
				case 1:
					label.setText(R.string.path);
					property.setText(file_path_str);
					if(files_selected_array.size()==1)
					{
						property.setPaintFlags(property.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
						property.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								ClipboardManager clipboardManager= (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
								ClipData clipData=ClipData.newPlainText("Path copied",property.getText());
								clipboardManager.setPrimaryClip(clipData);
								Global.print(context,getString(R.string.path_copied));
							}
						});
					}
					break;

				case 2:
					label.setText(R.string.type);
					property.setText(file_type_str);
					break;
				case 3:
					no_files_textview=row_item.findViewById(R.id.properties_description);
					label.setText(R.string.total_files);
					no_files_textview.setText(file_no_str);
					break;
				case 4:

					size_files_textview=row_item.findViewById(R.id.properties_description);
					label.setText(R.string.size);
					size_files_textview.setText(file_size_str);
					break;
				case 5:
					label.setText(R.string.modified);
					property.setText(file_date_str);
					break;
			}
			properties_details_table_layout.addView(row_item);
		}

		TableLayout properties_rwh_table_layout = v.findViewById(R.id.fragment_properties_rwh_tablelayout);
		properties_rwh_table_layout.setVisibility((files_selected_array.size()==1 || fileObjectType==FileObjectType.USB_TYPE) ? View.VISIBLE : View.GONE);

		for(int i=0;i<5;++i)
		{
			View row_item=inflater.inflate(R.layout.properties_item_view_layout,null);
			TextView label=row_item.findViewById(R.id.properties_label);
			TextView property=row_item.findViewById(R.id.properties_description);
			switch (i)
			{
				case 0:
					label.setText(R.string.readable);
					property.setText(readable_str);
					break;
				case 1:
					label.setText(R.string.writable);
					property.setText(writable_str);
					break;
				case 2:
					label.setText(R.string.hidden);
					property.setText(hidden_str);
					break;
				case 3:
					label.setText(R.string.permissions);
					property.setText(file_permissions_str);
					property.setOnClickListener(new View.OnClickListener()
					{
						public void onClick(View v)
						{
							if(file_permissions_str== null || file_permissions_str.equals(""))
							{
								return;
							}

							PermissionsDialog permissionsDialog=PermissionsDialog.getInstance(file_path_str,file_permissions_str,symbolic_link_str);
							/*
							permissionsDialog.setPermissionChangeListener(new PermissionsDialog.PermissionChangeListener()
							{
								public void onPermissionChange(File file)
								{
									getPermissions(file);
								}
							});

							 */
							permissionsDialog.show(((AppCompatActivity)context).getSupportFragmentManager().beginTransaction(),"permissions_dialog");
						}
					});

					break;
				case 4:
					label.setText(R.string.symbolic_link);
					property.setText(symbolic_link_str);
					break;

			}
			properties_rwh_table_layout.addView(row_item);
		}

		ViewModelFileCount viewModel=new ViewModelProvider(this).get(ViewModelFileCount.class);
		viewModel.count(source_folder,fileObjectType,files_selected_array,size,true);

		viewModel.total_no_of_files.observe(this, new androidx.lifecycle.Observer<Integer>() {
			@Override
			public void onChanged(Integer integer) {
				no_files_textview.setText(getString(R.string.total_files_colon)+" "+integer);
			}
		});

		viewModel.size_of_files_formatted.observe(this, new Observer<String>() {
			@Override
			public void onChanged(String s) {

				size_files_textview.setText(getString(R.string.size_colon)+" "+s);
			}
		});



		ViewGroup buttons_layout = v.findViewById(R.id.fragment_properties_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button OKBtn = buttons_layout.findViewById(R.id.first_button);
		OKBtn.setText(R.string.close);
		OKBtn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				PropertiesDialog.this.getViewModelStore().clear();
				dismissAllowingStateLoss();
			}
			
		});

		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(PROPERTIES_DIALOG_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(PROPERTIES_DIALOG_REQUEST_CODE))
				{
					String file_path=result.getString("file_path");
					getPermissions(new File(file_path));
				}
			}
		});
		return v;
	}

	public static PropertiesDialog getInstance(ArrayList<String>files_selected_array, FileObjectType fileObjectType)
	{
		PropertiesDialog propertiesDialog=new PropertiesDialog();
		Bundle bundle=new Bundle();
		bundle.putStringArrayList("files_selected_array",files_selected_array);
		bundle.putSerializable("fileObjectType",fileObjectType);
		propertiesDialog.setArguments(bundle);
		return propertiesDialog;
	}

	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
	}

/*
	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

 */

/*
	@Override
	public void onDismiss(DialogInterface dialog)
	{
		// TODO: Implement this method
		super.onDismiss(dialog);
		AsyncTaskFileCountSize.cancel(true);
	}

 */


	public void getPermissions(File file)
	{
		// TODO: Implement this method
		BufferedReader buffered_reader;
		String exec;
		if(file.isDirectory())
		{
			exec="ls -d -l ";
		}
		else
		{
			exec="ls -l ";
		}
		try
		{
			java.lang.Process proc=Runtime.getRuntime().exec(exec+file.getAbsolutePath());
			buffered_reader=new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;

			while((line=buffered_reader.readLine())!=null)
			{
				String [] line_split=line.split("\\s+");
				if(line_split.length>5)
				{
					file_permissions_str=line_split[0];
				}
				line_split=line.split("->");
				if(line_split.length>1)
				{
					symbolic_link_str="->"+line_split[1];
				}
				else
				{
					symbolic_link_str="-";
				}


			}
			proc.waitFor();
			buffered_reader.close();
		}
		catch(Exception e)
		{

		}
	}


/*
	private class FileCountSize extends svl.kadatha.filex.AsyncTask<Void,Void,Void> {
		long total_size_of_files;
		final List<String> source_list_files;
		final boolean include_folder;
		final FileObjectType sourceFileObjectType;

		FileCountSize(ArrayList<String> source_list_files, FileObjectType fileObjectType) {
			this.source_list_files = source_list_files;
			this.include_folder = true;
			this.sourceFileObjectType = fileObjectType;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

		}

		@Override
		protected Void doInBackground(Void[] p1) {
			// TODO: Implement this method
			int size = source_list_files.size();
			if (sourceFileObjectType == FileObjectType.FILE_TYPE) {
				File[] f_array = new File[size];
				for (int i = 0; i < size; ++i) {
					File f = new File(source_list_files.get(i));
					f_array[i] = f;
				}
				populate(f_array, include_folder);


			} else if (sourceFileObjectType == FileObjectType.USB_TYPE) {
				UsbFile[] f_array = new UsbFile[size];
				for (int i = 0; i < size; ++i) {
					UsbFile f = FileUtil.getUsbFile(MainActivity.usbFileRoot, source_list_files.get(i));
					f_array[i] = f;
				}
				populate(f_array, include_folder);
			}
			else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
			{
				FTPFile[] f_array=new FTPFile[size];
				for(int i=0;i<size;++i)
				{

					FTPFile f = FileUtil.getFTPFile(source_list_files.get(i));//MainActivity.FTP_CLIENT.mlistFile(source_list_files.get(i));
					f_array[i]=f;

				}
				populate(f_array,include_folder,source_folder);

			}

			return null;
		}

		private void populate(File[] source_list_files, boolean include_folder) {
			int size = source_list_files.length;
			for (int i = 0; i < size; ++i) {
				File f = source_list_files[i];
				if (isCancelled()) {
					return;
				}
				int no_of_files = 0;
				long size_of_files = 0L;
				if (f.isDirectory()) {
					if (f.list() != null) {
						populate(f.listFiles(), include_folder);
					}
					if (include_folder) {
						no_of_files++;
					}
				} else {
					no_of_files++;
					size_of_files += f.length();
				}
				total_no_of_files += no_of_files;
				total_size_of_files += size_of_files;
				size_of_files_format = FileUtil.humanReadableByteCount(total_size_of_files, Global.BYTE_COUNT_BLOCK_1000);
				publishProgress();
			}
		}

		private void populate(UsbFile[] source_list_files, boolean include_folder) {
			int size = source_list_files.length;
			for (int i = 0; i < size; ++i) {
				UsbFile f = source_list_files[i];
				if (isCancelled()) {
					return;
				}
				int no_of_files = 0;
				long size_of_files = 0L;
				if (f.isDirectory()) {
					try {
						if (f.list() != null) {
							populate(f.listFiles(), include_folder);
						}
					} catch (IOException e) {

					}
					if (include_folder) {
						no_of_files++;
					}
				} else {
					no_of_files++;
					size_of_files += f.getLength();
				}
				total_no_of_files += no_of_files;
				total_size_of_files += size_of_files;
				size_of_files_format = FileUtil.humanReadableByteCount(total_size_of_files, Global.BYTE_COUNT_BLOCK_1000);
				publishProgress();
			}
		}

		private void populate(FTPFile[] source_list_files, boolean include_folder, String path)
		{
			int size=source_list_files.length;
			for(int i=0;i<size;++i)
			{
				FTPFile f=source_list_files[i];
				if(isCancelled())
				{
					return;
				}
				int no_of_files=0;
				long size_of_files=0L;
				if(f.isDirectory())
				{
					try {
						String name=f.getName();
						path=(path.endsWith(File.separator)) ? path+name : path+File.separator+name;
						populate(MainActivity.FTP_CLIENT.listFiles(path),include_folder,path);
					} catch (IOException e) {

					}
					if(include_folder)
					{
						no_of_files++;
					}
				}
				else
				{
					no_of_files++;
					size_of_files+=f.getSize();
				}
				total_no_of_files+=no_of_files;
				total_size_of_files+=size_of_files;
				size_of_files_format=FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000);
				publishProgress();
			}
		}


		@Override
		protected void onProgressUpdate(Void[] values) {
			// TODO: Implement this method
			super.onProgressUpdate(values);
			if (no_files_textview != null) {

				no_files_textview.setText(file_no_str=total_no_of_files +" " +(total_no_of_files==1 ? getString(R.string.file1) : getString(R.string.files)));
				size_files_textview.setText(file_size_str=size_of_files_format);

			}
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO: Implement this method
			super.onPostExecute(result);
		}

		@Override
		protected void onCancelled(Void result) {
			// TODO: Implement this method
			super.onCancelled(result);
		}
	}

 */

}
