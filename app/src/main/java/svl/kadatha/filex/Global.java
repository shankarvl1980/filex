package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Global
{
	static ArrayList<FilePOJO> STORAGE_DIR=new ArrayList<>();
	static final List<String> INTERNAL_STORAGE_PATH=new ArrayList<>();
	static String EXTERNAL_STORAGE_PATH="";
	static String USB_STORAGE_PATH;

	static HashMap<String, List<FilePOJO>> HASHMAP_FILE_POJO_FILTERED=new HashMap<>();
	static HashMap<String,List<FilePOJO>> HASHMAP_FILE_POJO=new HashMap<>();


	static final List<UriPOJO> URI_PERMISSION_LIST=new ArrayList<>();
	static int ORIENTATION;
	static int SCREEN_WIDTH,SCREEN_HEIGHT,DIALOG_WIDTH,DIALOG_HEIGHT;
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
	static int THIRTY_FOUR_DP;
	static int SIXTY_DP;
	static int ONE_SP;
	static int FOUR_DP;
	static int SIX_DP;
	static int EIGHT_DP;
	static int TEN_DP;
	static int RECYCLERVIEW_ITEM_SPACING;
	static int DRAWER_WIDTH;

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

	static public int NAVIGATION_BAR_HEIGHT;

	static final IndexedLinkedHashMap<String,SpacePOJO> SPACE_ARRAY=new IndexedLinkedHashMap<>();

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
			if(uriPOJO.get_authority().equals(uri_authority) && uriPOJO.get_path().startsWith(uri_path) && uriPOJO.get_path().length()>uri_path.length())
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
			//boolean uri_authority_exists_in_list;
			boolean parent_uri_exists = false;
			for(UriPOJO uriPOJO:URI_PERMISSION_LIST)
			{
				if(uriPOJO.get_authority().equals(uri_authority))
				{
					//uri_authority_exists_in_list=true;
					if(uri_path.startsWith(uriPOJO.get_path()))
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

				if(file_path.startsWith(uriPOJO.get_path()))
				{
					return uriPOJO;
				}

			}
			else if(fileObjectType==FileObjectType.FILE_TYPE &&  uriPOJO.get_authority().equals("com.android.externalstorage.documents"))
			{
				if(file_path.startsWith(uriPOJO.get_path()))
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
		if(SCREEN_WIDTH==0 || SCREEN_HEIGHT==0)
		{
			if(ORIENTATION==Configuration.ORIENTATION_LANDSCAPE)
			{
				SCREEN_WIDTH=context.getResources().getDisplayMetrics().heightPixels;
				SCREEN_HEIGHT=context.getResources().getDisplayMetrics().widthPixels;
			}
			else
			{
				SCREEN_WIDTH=context.getResources().getDisplayMetrics().widthPixels;
				SCREEN_HEIGHT=context.getResources().getDisplayMetrics().heightPixels;
			}

			DIALOG_WIDTH=SCREEN_WIDTH*90/100;
			DIALOG_HEIGHT=SCREEN_HEIGHT*90/100;

			SCREEN_RATIO=(float) SCREEN_WIDTH/(float) SCREEN_HEIGHT;

		}

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


	static void GET_IMAGEVIEW_DIMENSIONS(Context context)
	{
		
		if(IMAGEVIEW_DIMENSION_SMALL_LIST==0)
		{
			DisplayMetrics displayMetrics=context.getResources().getDisplayMetrics();
			int list_s=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,displayMetrics);
			int list_g=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,7,displayMetrics);
			ONE_SP=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,1,displayMetrics);
			ONE_DP=list_s/4;
			TWO_DP=ONE_DP*2;
			THIRTY_FOUR_DP=ONE_DP*34;
			FOUR_DP=list_s;
			EIGHT_DP=list_s*2;
			RECYCLERVIEW_ITEM_SPACING=list_s*2;
			TEN_DP=ONE_DP*10;
			SIX_DP=ONE_DP*6;
			DRAWER_WIDTH=252*ONE_DP;

			SIXTY_DP=list_s*15;
			IMAGEVIEW_DIMENSION_SMALL_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL;
			IMAGEVIEW_DIMENSION_MEDIUM_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM;
			IMAGEVIEW_DIMENSION_LARGE_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE;

			IMAGEVIEW_DIMENSION_SMALL_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL;
			IMAGEVIEW_DIMENSION_MEDIUM_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM;
			IMAGEVIEW_DIMENSION_LARGE_GRID=list_s*2*IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE;


			GRID_COUNT_SMALL=SCREEN_WIDTH/(IMAGEVIEW_DIMENSION_SMALL_GRID+list_s);
			GRID_COUNT_MEDIUM=SCREEN_WIDTH/(IMAGEVIEW_DIMENSION_MEDIUM_GRID+list_s);
			GRID_COUNT_LARGE=SCREEN_WIDTH/(IMAGEVIEW_DIMENSION_LARGE_GRID+list_s);

			BUTTON_HEIGHT=FOUR_DP*12;

			DIVIDERITEMDECORATION=new DividerItemDecoration(context,DividerItemDecoration.VERTICAL);
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

	static void LOCAL_BROADCAST(String action, LocalBroadcastManager localBroadcastManager)
	{
		Intent intent=new Intent();
		intent.setAction(action);
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


	public static void GET_NAVIGATION_BAR_HEIGHT(Context context){
		Resources resources = context.getResources();
		int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			NAVIGATION_BAR_HEIGHT=resources.getDimensionPixelSize(resourceId);
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

				SPACE_ARRAY.put(filePOJO.getPath(),new SpacePOJO(filePOJO.getPath(),totalspace,availabelspace));
			}
			else if(fileObjectType== FileObjectType.USB_TYPE)
			{
				String name=MainActivity.usbFileRoot.getName();
				totalspace=MainActivity.usbCurrentFs.getCapacity();
				availabelspace=MainActivity.usbCurrentFs.getOccupiedSpace();
				SPACE_ARRAY.put(MainActivity.usbFileRoot.getName(),new SpacePOJO(MainActivity.usbFileRoot.getName(),totalspace,availabelspace));
			}
			else if(fileObjectType==FileObjectType.ROOT_TYPE)
			{
				SPACE_ARRAY.put(filePOJO.getPath(),new SpacePOJO(filePOJO.getPath(),totalspace,availabelspace));
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
		List<ResolveInfo> resolveInfoList=MainActivity.PM.queryIntentActivities(intent,0);
		return resolveInfoList.size() != 0;
	}


}




