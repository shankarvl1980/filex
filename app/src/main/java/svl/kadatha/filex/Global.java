package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;

import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class Global
{
	static ArrayList<FilePOJO> STORAGE_DIR=new ArrayList<>();
	static final List<String> INTERNAL_STORAGE_PATH=new ArrayList<>();
	static String EXTERNAL_STORAGE_PATH="";
	static String USB_STORAGE_PATH;

	static File ARCHIVE_EXTRACT_DIR;
	static File APK_ICON_DIR;
	static final List<String>APK_ICON_PACKAGE_NAME_LIST=new ArrayList<>();
	static int ARCHIVE_CACHE_DIR_LENGTH;

	static final HashMap<String, List<FilePOJO>> HASHMAP_FILE_POJO_FILTERED=new HashMap<>();
	static final HashMap<String,List<FilePOJO>> HASHMAP_FILE_POJO=new HashMap<>();


	static final List<UriPOJO> URI_PERMISSION_LIST=new ArrayList<>();
	static int ORIENTATION;
	static int SCREEN_WIDTH,SCREEN_HEIGHT,DIALOG_WIDTH,DIALOG_HEIGHT,WIDTH;
	static float SCREEN_RATIO;
	static String SORT;
	static String STORAGE_ANALYSER_SORT;
	static String THEME;
	static int RECYCLER_VIEW_FONT_SIZE_FACTOR;

	
	static final String TEXT_REGEX="(?i)txt|java|xml|cpp|c|h";
	static final String RTF_REGEX="(?i)rtf";
	static final String IMAGE_REGEX="(?i)png|jpg|jpeg|svg|gif|tif|webp";
	static final String AUDIO_REGEX="(?i)mp3|ogg|wav|aac|wma|opus";
	static final String VIDEO_REGEX="(?i)3gp|mp4|avi|mov|flv|wmv|webm";
	static final String ZIP_REGEX="(?i)zip|rar";
	static final String UNIX_ARCHIVE_REGEX="(?i)tar|gzip|gz";
	static final String APK_REGEX="(?i)apk";
	static final String PDF_REGEX="(?i)pdf";
	static final String DOC_REGEX="(?i)doc|docx";
	static final String XLS_REGEX="(?i)xls|xlsx";
	static final String PPT_REGEX="(?i)ppt|pptx";
	static final String DB_REGEX="(?i)db";
	

	static int IMAGEVIEW_DIMENSION_SMALL_LIST;
	static int IMAGEVIEW_DIMENSION_MEDIUM_LIST;
	static int IMAGEVIEW_DIMENSION_LARGE_LIST;

	static int IMAGEVIEW_DIMENSION_SMALL_GRID;
	static int IMAGEVIEW_DIMENSION_MEDIUM_GRID;
	static int IMAGEVIEW_DIMENSION_LARGE_GRID;

	static int GRID_COUNT_SMALL,GRID_COUNT_MEDIUM,GRID_COUNT_LARGE,GRID_COUNT;
	static int BUTTON_HEIGHT;
	static int ONE_DP;
	static int TWO_DP;
	static int THIRTY_SIX_DP;
	static int SELECTOR_ICON_DIMENSION;
	static int ONE_SP;
	static int FOUR_DP;
	static int SIX_DP;
	static int EIGHT_DP;
	static int TEN_DP;
	static int RECYCLERVIEW_ITEM_SPACING;
	//static int DRAWER_WIDTH;

	static final float DISABLE_ALFA= (float) 0.4;
	static final float ENABLE_ALFA= (float) 1.0;
	
	static final int FONT_SIZE_SMALL_FIRST_LINE=14;
	static final int FONT_SIZE_SMALL_DETAILS_LINE=11;
	
	static final int FONT_SIZE_MEDIUM_FIRST_LINE=16;
	static final int FONT_SIZE_MEDIUM_DETAILS_LINE=12;
	
	static final int FONT_SIZE_LARGE_FIRST_LINE=18;
	static final int FONT_SIZE_LARGE_DETAILS_LINE=14;
	
	static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL=8;
	static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM=9;
	static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE=10;
	
	static boolean BYTE_COUNT_BLOCK_1000;
	static boolean FILE_GRID_LAYOUT;
	static boolean SHOW_FILE_PATH;
	static boolean IS_TABLET;

	static final Set<String> SORT_CODE_SET=new HashSet<>(Arrays.asList("d_name_asc","d_name_desc","d_date_asc","d_date_desc","d_size_asc","d_size_desc",
			"f_name_asc","f_name_desc","f_date_asc","f_date_desc","f_size_asc","f_size_desc"));

	static final Set<String> THEME_CODE_SET=new HashSet<>(Arrays.asList("system","light","dark"));

	static final List<MimePOJO> SUPPORTED_MIME_POJOS=new ArrayList<>(Arrays.asList(
			new MimePOJO("Text","text/*",TEXT_REGEX),
			new MimePOJO("Image","image/*",IMAGE_REGEX),
			new MimePOJO("Audio","audio/*",AUDIO_REGEX),
			new MimePOJO("Video","video/*",VIDEO_REGEX),
			new MimePOJO("PDF","application/pdf",PDF_REGEX)));

	static final List<MimePOJO> MIME_POJOS=new ArrayList<>(Arrays.asList(
			new MimePOJO("MS Word","application/msword",DOC_REGEX),
			new MimePOJO("MS Excel","application/vnd.ms-excel",XLS_REGEX),
			new MimePOJO("MS PowerPoint","application/vnd.ms-powerpoint",PPT_REGEX),
			new MimePOJO("DB","application/vnd.sqlite3",DB_REGEX),
			new MimePOJO("Zip","application/zip",ZIP_REGEX),
			new MimePOJO("RTF","application/rtf",RTF_REGEX),
			new MimePOJO("APK","application/vnd.android.package-archive",APK_REGEX),

			//same as supported mimepojos above
			new MimePOJO("Text","text/*",TEXT_REGEX),
			new MimePOJO("Image","image/*",IMAGE_REGEX),
			new MimePOJO("Audio","audio/*",AUDIO_REGEX),
			new MimePOJO("Video","video/*",VIDEO_REGEX),
			new MimePOJO("PDF","application/pdf",PDF_REGEX)));


	static public final int LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY=4000;
	static public int ACTION_BAR_HEIGHT;

	static public final boolean AFTER_ARCHIVE_GOTO_DEST_FOLDER=true;

	static public boolean DETAILED_SEARCH_LIBRARY;

	static public final String FILEX_PACKAGE="svl.kadatha.filex";

	static public boolean SHARE_USB;

	static public final String LOCAL_BROADCAST_DELETE_FILE_ACTION=FILEX_PACKAGE+".FILE_DELETE";
	static public final String LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION=FILEX_PACKAGE+".MODIFICATION_OBSERVED";
	static public final String LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION=FILEX_PACKAGE+".FILE_POJO_CACHE_CLEAR";

	static public int NAVIGATION_BAR_HEIGHT;

	static final LinkedHashMap<String,SpacePOJO> SPACE_ARRAY=new LinkedHashMap<>();

	static DividerItemDecoration DIVIDERITEMDECORATION;

	static void GET_URI_PERMISSIONS_LIST(Context context)
	{
		List<UriPermission> permission_list=context.getContentResolver().getPersistedUriPermissions();
		if(permission_list.size()>0)
		{
			for(UriPermission permission : permission_list)
			{
				if(permission.isWritePermission())
				{
					Uri uri=permission.getUri();
					String uri_authority=uri.getAuthority();
					String uri_path=FileUtil.getFullPathFromTreeUri(uri,context);
					if(uri_path!=null && !uri_path.equals(File.separator)) URI_PERMISSION_LIST.add(new UriPOJO(uri,uri_authority,uri_path)); //check path is not equl to file separator as it becomes to / when SD card is removed

				}
			}
		}
	}

	static void ON_REQUEST_URI_PERMISSION(Context context,Uri treeUri)
	{
		String uri_authority=treeUri.getAuthority();
		String uri_path;
		if(uri_authority.equals(UsbDocumentProvider.DOCUMENTS_AUTHORITY))
		{

			final String docId = DocumentsContract.getTreeDocumentId(treeUri);
			final String[] split = docId.split(":");
			if(split.length==1)
			{
				uri_path="/";
			}
			else
			{
				uri_path=split[1];
			}

		}
		else
		{
			uri_path=FileUtil.getFullPathFromTreeUri(treeUri,context);
		}


		Iterator<UriPOJO> iterator=URI_PERMISSION_LIST.iterator();

		while(iterator.hasNext())
		{
			UriPOJO uriPOJO=iterator.next();
			if(uriPOJO.get_authority().equals(uri_authority) && Global.IS_CHILD_FILE(uriPOJO.get_path(),uri_path) && uriPOJO.get_path().length()>uri_path.length())
			{
				final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				context.getContentResolver().releasePersistableUriPermission(uriPOJO.get_uri(),takeFlags);
				iterator.remove();

			}
		}

		if(URI_PERMISSION_LIST.size()==0)
		{
			URI_PERMISSION_LIST.add(new UriPOJO(treeUri,uri_authority,uri_path));
			final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
		}
		else
		{
			boolean parent_uri_exists = false;
			for(UriPOJO uriPOJO:URI_PERMISSION_LIST)
			{
				if(uriPOJO.get_authority().equals(uri_authority))
				{
					if(Global.IS_CHILD_FILE(uri_path,uriPOJO.get_path()))
					{
						parent_uri_exists=true;
						break;
					}

				}
			}
			if(!parent_uri_exists)
			{
				URI_PERMISSION_LIST.add(new UriPOJO(treeUri,uri_authority,uri_path));
				final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
			}
		}

	}


	static UriPOJO CHECK_AVAILABILITY_URI_PERMISSION(String file_path, FileObjectType fileObjectType)
	{
		for(UriPOJO uriPOJO:URI_PERMISSION_LIST)
		{
			if(fileObjectType==FileObjectType.USB_TYPE && uriPOJO.get_authority().equals(UsbDocumentProvider.DOCUMENTS_AUTHORITY))
			{
				if(Global.IS_CHILD_FILE(file_path,uriPOJO.get_path()))
				{
					return uriPOJO;
				}

			}
			else if(fileObjectType==FileObjectType.FILE_TYPE &&  uriPOJO.get_authority().equals("com.android.externalstorage.documents"))
			{
				if(Global.IS_CHILD_FILE(file_path,uriPOJO.get_path()))
				{
					return uriPOJO;
				}
			}

		}
		return null;
	}
	
	static void GET_SCREEN_DIMENSIONS(Context context)
	{
		ORIENTATION=context.getResources().getConfiguration().orientation;
		if(ORIENTATION==Configuration.ORIENTATION_LANDSCAPE)
		{
			SCREEN_WIDTH=context.getResources().getDisplayMetrics().heightPixels;
			SCREEN_HEIGHT=context.getResources().getDisplayMetrics().widthPixels;
			WIDTH=SCREEN_HEIGHT;


		}
		else
		{
			SCREEN_WIDTH=context.getResources().getDisplayMetrics().widthPixels;
			SCREEN_HEIGHT=context.getResources().getDisplayMetrics().heightPixels;
			WIDTH=SCREEN_WIDTH;

		}

		DIALOG_WIDTH=SCREEN_WIDTH*90/100;
		DIALOG_HEIGHT=SCREEN_HEIGHT*90/100;

		SCREEN_RATIO=(float) SCREEN_WIDTH/(float) SCREEN_HEIGHT;
		IS_TABLET=context.getResources().getBoolean(R.bool.isTablet);

	}
	
	

	static void GET_IMAGE_VIEW_DIMENSIONS(Context context)
	{
		
		if(IMAGEVIEW_DIMENSION_SMALL_LIST==0)
		{
			DisplayMetrics displayMetrics=context.getResources().getDisplayMetrics();
			ONE_SP=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,1,displayMetrics);

			FOUR_DP=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,displayMetrics);
			SIX_DP=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,displayMetrics);
			EIGHT_DP=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8,displayMetrics);
			TEN_DP=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10,displayMetrics);

			int list_s= FOUR_DP;
			int list_g=IS_TABLET ? TEN_DP : EIGHT_DP;


			ONE_DP=FOUR_DP/4;
			TWO_DP=FOUR_DP/2;

			THIRTY_SIX_DP=FOUR_DP*9;
			SELECTOR_ICON_DIMENSION=TEN_DP+TEN_DP+SIX_DP;
			RECYCLERVIEW_ITEM_SPACING=list_g;

			IMAGEVIEW_DIMENSION_SMALL_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL;
			IMAGEVIEW_DIMENSION_MEDIUM_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM;
			IMAGEVIEW_DIMENSION_LARGE_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE;

			IMAGEVIEW_DIMENSION_SMALL_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL;
			IMAGEVIEW_DIMENSION_MEDIUM_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM;
			IMAGEVIEW_DIMENSION_LARGE_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE;

			BUTTON_HEIGHT=FOUR_DP*12;

			DIVIDERITEMDECORATION=new DividerItemDecoration(context,DividerItemDecoration.VERTICAL);
		}
		GRID_COUNT_SMALL=WIDTH/(IMAGEVIEW_DIMENSION_SMALL_GRID+FOUR_DP);
		GRID_COUNT_MEDIUM=WIDTH/(IMAGEVIEW_DIMENSION_MEDIUM_GRID+FOUR_DP);
		GRID_COUNT_LARGE=WIDTH/(IMAGEVIEW_DIMENSION_LARGE_GRID+FOUR_DP);


		//apk cache directory setting
		ARCHIVE_EXTRACT_DIR=new File(context.getFilesDir(),"Archive");
		APK_ICON_DIR=context.getExternalFilesDir(".apk_icons");
		APK_ICON_PACKAGE_NAME_LIST.addAll(Arrays.asList(APK_ICON_DIR.list()));
		ARCHIVE_CACHE_DIR_LENGTH=Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath().length();


	}

	static void GET_PREFERENCES(TinyDB tinyDB)
	{
//
		if(SORT==null)
		{
			SORT=tinyDB.getString("sort");
			if(SORT.trim().isEmpty() || !SORT_CODE_SET.contains(SORT))
			{
				SORT="d_name_asc";
			}
		}

		//

		if(STORAGE_ANALYSER_SORT==null)
		{
			STORAGE_ANALYSER_SORT=tinyDB.getString("storage_analyser_sort");
			if(STORAGE_ANALYSER_SORT.trim().isEmpty() || !SORT_CODE_SET.contains(STORAGE_ANALYSER_SORT))
			{
				STORAGE_ANALYSER_SORT="d_size_desc";
			}
		}


		//
		if(THEME==null)
		{
			THEME=tinyDB.getString("theme");
			if(THEME.trim().isEmpty() || !THEME_CODE_SET.contains(THEME))
			{
				THEME="system";
			}
		}

		//
		BYTE_COUNT_BLOCK_1000=tinyDB.getBoolean("byte_count_block_1000");

		//
		FILE_GRID_LAYOUT=tinyDB.getBoolean("file_grid_layout");

		//

		DETAILED_SEARCH_LIBRARY=tinyDB.getBoolean("detailed_search_library");
		//

		SHOW_FILE_PATH=tinyDB.getBoolean("show_file_path");

		//
		RECYCLER_VIEW_FONT_SIZE_FACTOR=tinyDB.getInt("recycler_view_font_size_factor");
		if(!tinyDB.getBoolean("not_first_run"))
		{
			Global.RECYCLER_VIEW_FONT_SIZE_FACTOR=1;
			tinyDB.putBoolean("not_first_run",true);
			tinyDB.putInt("recycler_view_font_size_factor",Global.RECYCLER_VIEW_FONT_SIZE_FACTOR);
		}

		if(RECYCLER_VIEW_FONT_SIZE_FACTOR!=0 && RECYCLER_VIEW_FONT_SIZE_FACTOR!=1 && RECYCLER_VIEW_FONT_SIZE_FACTOR!=2)
		{
			RECYCLER_VIEW_FONT_SIZE_FACTOR=1;
		}

		switch(RECYCLER_VIEW_FONT_SIZE_FACTOR)
		{
			case 0:
				GRID_COUNT=Global.GRID_COUNT_SMALL;
				break;
			case 2:
				GRID_COUNT=Global.GRID_COUNT_LARGE;
				break;
			default:
				GRID_COUNT=Global.GRID_COUNT_MEDIUM;
				break;
		}



	}





	static void GET_ACTION_BAR_HEIGHT(Context context)
	{
		if(ACTION_BAR_HEIGHT==0)
		{
			TypedValue tv = new TypedValue();
			if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			{
				ACTION_BAR_HEIGHT = TypedValue.complexToDimensionPixelSize(tv.data,context.getResources().getDisplayMetrics());
			}
		}

	}

	static int GET_HEIGHT_LIST_VIEW(ListView listView) {

		ListAdapter mAdapter= listView.getAdapter();
		int listviewElementsheight = 0;
		for (int i = 0; i < mAdapter.getCount(); i++) {
			View mView = mAdapter.getView(i, null, listView);
			mView.measure(
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
			listviewElementsheight += mView.getMeasuredHeight()+Global.ONE_DP;
		}
		return listviewElementsheight;
	}

	static void LOCAL_BROADCAST(String action, LocalBroadcastManager localBroadcastManager, String activity_name)
	{
		Intent intent=new Intent();
		intent.setAction(action);
		intent.putExtra("activity_name",activity_name);
		localBroadcastManager.sendBroadcast(intent);
	}

	static void LOCAL_BROADCAST(String action, LocalBroadcastManager localBroadcastManager, String activity_name, String file_path, FileObjectType fileObjectType)
	{
		Intent intent=new Intent();
		intent.setAction(action);
		intent.putExtra("activity_name",activity_name);
		intent.putExtra("file_path",file_path);
		intent.putExtra("fileObjectType",fileObjectType);
		localBroadcastManager.sendBroadcast(intent);
	}

	static final FilenameFilter File_NAME_FILTER=new FilenameFilter()
	{
		@Override
		public boolean accept(File file, String s) {
			return !s.startsWith(".");
		}
	};


	static FilePOJO GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR()
	{
		if(STORAGE_DIR.get(0).getPath().equals("/"))
		{
			return STORAGE_DIR.get(1);
		}
		else
		{
			return STORAGE_DIR.get(0);
		}
	}


	static String GET_INTERNAL_STORAGE_PATH_STORAGE_DIR()
	{
		return GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR().getPath();
	}

	static void GET_STORAGE_DIR(Context context)
	{
		if(STORAGE_DIR.size()==0)
		{
			STORAGE_DIR=new ArrayList<>(StorageUtil.getSdCardPaths(context,true));
			WORKOUT_AVAILABLE_SPACE();
		}
	}
	public static boolean HAS_NAVBAR(WindowManager windowManager){
		Display d = windowManager.getDefaultDisplay();

		DisplayMetrics realDisplayMetrics = new DisplayMetrics();
		d.getRealMetrics(realDisplayMetrics);

		int realHeight = realDisplayMetrics.heightPixels;
		int realWidth = realDisplayMetrics.widthPixels;

		DisplayMetrics displayMetrics = new DisplayMetrics();
		d.getMetrics(displayMetrics);

		int displayHeight = displayMetrics.heightPixels;
		int displayWidth = displayMetrics.widthPixels;

		return (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
	}


	public static void GET_NAVIGATION_BAR_HEIGHT(Context context){

		Resources resources = context.getResources();


		int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
		if(id > 0 && resources.getBoolean(id) && IS_TABLET)

		//if(HAS_NAVBAR((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)))
		{
			int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
			if (resourceId > 0) {
				NAVIGATION_BAR_HEIGHT=resources.getDimensionPixelSize(resourceId);
			}
		}
		else
		{
			NAVIGATION_BAR_HEIGHT=0;
		}

	}



	public static String GET_FILE_PERMISSION(String file_path)
	{
		String permission_string = null;
		/*
		String[] command_line_long = {"ls", "-ld", file_path};
		try {
			java.lang.Process process_long = Runtime.getRuntime().exec(command_line_long);
			BufferedReader bf_long = new BufferedReader(new InputStreamReader(process_long.getInputStream()));
			String line_long=bf_long.readLine(); //consume first line as not required
			if(line_long != null) {
				String [] split_line=line_long.split("\\s+");
				permission_string=split_line[0];
			}
			process_long.waitFor();
		}
		catch (Exception e){}

		 */
		return permission_string;
	}

	public static String GET_OTHER_FILE_PERMISSION(String file_path)
	{
		String permission_string=GET_FILE_PERMISSION(file_path);
		if(permission_string==null) return null;
		String other_permission_string=permission_string.replaceAll(".*?(.?.?.?)?$", "$1");
		return other_permission_string.replaceAll("-","");
	}

	public static boolean SET_OTHER_FILE_PERMISSION(String permission, String file_path)
	{
		boolean success = false;
	/*
		RootUtils.EXECUTE(Arrays.asList("mount -o rw,remount /"));
		success=RootUtils.EXECUTE(Arrays.asList("chmod o="+permission,file_path));

	 */
		return success;
	}


	static void WORKOUT_AVAILABLE_SPACE()
	{
		for(FilePOJO filePOJO:Global.STORAGE_DIR)
		{
			long totalspace=0,availabelspace=0;
			FileObjectType fileObjectType=filePOJO.getFileObjectType();
			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				totalspace=new File(filePOJO.getPath()).getTotalSpace();
				availabelspace=new File(filePOJO.getPath()).getUsableSpace();

				SPACE_ARRAY.put(fileObjectType+filePOJO.getPath(),new SpacePOJO(filePOJO.getPath(),totalspace,availabelspace));
			}
			else if(fileObjectType== FileObjectType.USB_TYPE)
			{
				String name=MainActivity.usbFileRoot.getName();
				totalspace=MainActivity.usbCurrentFs.getCapacity();
				availabelspace=MainActivity.usbCurrentFs.getOccupiedSpace();
				SPACE_ARRAY.put(fileObjectType+name,new SpacePOJO(name,totalspace,availabelspace));
			}
			else if(fileObjectType==FileObjectType.ROOT_TYPE)
			{
				SPACE_ARRAY.put(fileObjectType+filePOJO.getPath(),new SpacePOJO(filePOJO.getPath(),totalspace,availabelspace));
			}

		}

	}

	public static boolean CHECK_APPS_FOR_RECOGNISED_FILE_EXT(Context context,String file_extn){

		String mime_type = "";
		Uri uri;
		File f=new File("/dummy");
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
		{
			uri = FileProvider.getUriForFile(context,context.getPackageName()+".provider",f);
		}
		else
		{
			uri=Uri.fromFile(f);
		}
		final Intent intent=new Intent(Intent.ACTION_VIEW);
		mime_type=FileIntentDispatch.SET_INTENT_FOR_VIEW(intent,mime_type,"",file_extn,null,false,false,uri);
		List<ResolveInfo> resolveInfoList=context.getPackageManager().queryIntentActivities(intent,0);
		return resolveInfoList.size() != 0;
	}

	public static void REMOVE_RECURSIVE_PATHS(List<String> files_selected_array, String dest_folder, FileObjectType destFileObjectType, FileObjectType sourceFileObjectType)
	{
		if(sourceFileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE))
		{
			if(FileObjectType.FILE_TYPE.equals(destFileObjectType))
			{
				Iterator<String> iterator=files_selected_array.iterator();
				while(iterator.hasNext())
				{
					if(Global.IS_CHILD_FILE(dest_folder,iterator.next()))
					{
						iterator.remove();
					}
				}
			}
		}
		else
		{
			if(sourceFileObjectType.equals(destFileObjectType))
			{
				Iterator<String> iterator=files_selected_array.iterator();
				while(iterator.hasNext())
				{
					//if((dest_folder+File.separator).startsWith(iterator.next()+File.separator))
					if(Global.IS_CHILD_FILE(dest_folder,iterator.next()))
					{
						iterator.remove();
					}
				}
			}
		}

	}


	public static Bitmap GET_BITMAP(String path){
		if(path==null) return null;
		File f=new File(path);
		Bitmap b = null;

		//Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			BitmapFactory.decodeStream(fis, null, o);
		} catch (FileNotFoundException e) {
			return null;
		}


		int scale = 1;
		if (o.outHeight > IMAGEVIEW_DIMENSION_LARGE_LIST || o.outWidth > IMAGEVIEW_DIMENSION_LARGE_LIST) {
			scale = (int)Math.pow(2, (int) Math.ceil(Math.log(IMAGEVIEW_DIMENSION_LARGE_LIST /
					(double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
		}

		//Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		try {
			fis = new FileInputStream(f);
			b = BitmapFactory.decodeStream(fis, null, o2);
		} catch (FileNotFoundException e) {
			return null;
		}
		finally {
			try {
				fis.close();
			} catch (IOException e) {
				return null;
			}
		}
		return b;
	}


	public static Bitmap GET_BITMAP_FILE_DESCRIPTOR_METHOD(String path)   {
	if(path==null) return null;
	Bitmap bitmap = null;
	BitmapFactory.Options bfOptions=new BitmapFactory.Options();
	bfOptions.inDither=false;                     //Disable Dithering mode
	bfOptions.inTempStorage=new byte[32 * 1024];

	File file=new File(path);
	FileInputStream fs=null;
	try {
		fs = new FileInputStream(file);
	} catch (FileNotFoundException e) {
		return null;
	}

	try {
		if(fs!=null) bitmap=BitmapFactory.decodeFileDescriptor(fs.getFD(), null, bfOptions);
	} catch (IOException e) {
		return null;
	} finally{
		if(fs!=null) {
			try {
				fs.close();
			} catch (IOException e) {
				return null;
			}
		}
	}

	return bitmap;
}

	public static boolean CHECK_FTP_SERVER_CONNECTED()
	{
		int reply_code=MainActivity.FTP_CLIENT.getReplyCode();
		if(FTPReply.isPositiveCompletion(reply_code))
		{
			return true;
		}
		else
		{

			//try {
				//MainActivity.FTP_CLIENT.disconnect();
				Iterator<FilePOJO> iterator=STORAGE_DIR.iterator();
				while(iterator.hasNext())
				{
					FilePOJO filePOJO= iterator.next();
					if(filePOJO.getFileObjectType()==FileObjectType.FTP_TYPE)
					{
						iterator.remove();
						break;
					}

				}

				return false;
			//} catch (IOException e) {
			//	return false;
			//}
		}
	}

	public static String GET_TRUNCATED_FILE_PATH_USB(String file_path)
	{
		if(file_path.equals(File.separator)) return file_path;
		if(file_path.startsWith(File.separator))
		{
			return file_path.substring(1);
		}
		else
		{
			return file_path;
		}
	}

	public static boolean IS_CHILD_FILE(String child_path,String parent_path)
	{
		if(parent_path.equals(File.separator))
		{
			return child_path.startsWith(parent_path);
		}
		else
		{
			return (child_path+File.separator).startsWith(parent_path+File.separator);
		}
	}

	public static FileObjectType GET_FILE_OBJECT_TYPE(String file_object_type)
	{
		if(file_object_type==null) return null;
		switch (file_object_type)
		{
			case "ROOT_TYPE":
				return FileObjectType.ROOT_TYPE;
			case "FILE_TYPE":
				return FileObjectType.FILE_TYPE;
			case "SEARCH_LIBRARY_TYPE":
				return FileObjectType.SEARCH_LIBRARY_TYPE;
			case "USB_TYPE":
				return FileObjectType.USB_TYPE;
			case "FTP_TYPE":
				return FileObjectType.FTP_TYPE;
			default:
				return null;
		}
	}

	public static double AVAILABLE_MEMORY_MB(){
		double max = Runtime.getRuntime().maxMemory()/1024;
		Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
		Debug.getMemoryInfo(memoryInfo);
		return (max - memoryInfo.getTotalPss())/1024;
	}

	public static long GET_URI_FILE_SIZE(Uri fileUri,Context context) {
		Cursor returnCursor = context.getContentResolver().
				query(fileUri, null, null, null, null);
		int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
		returnCursor.moveToFirst();

		long size = returnCursor.getLong(sizeIndex);
		returnCursor.close();

		return size;
	}

	public static float GET_BITMAP_ASPECT_RATIO(InputStream inputStream)
	{
		BitmapFactory.Options options=new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		BitmapFactory.decodeStream(inputStream,null,options);
		int width=options.outWidth;
		int height=options.outHeight;
		if(width==0 || height==0)
		{
			return 0;
		}
		else
		{
			return (float) (width/height);
		}

	}
}

