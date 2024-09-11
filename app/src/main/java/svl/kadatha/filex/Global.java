package svl.kadatha.filex;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import me.jahnen.libaums.core.fs.UsbFile;
import timber.log.Timber;

public class Global
{
	static public final SimpleDateFormat SDF=new SimpleDateFormat("dd.MM.yyyy");
	static String INTERNAL_PRIMARY_STORAGE_PATH="";
	static String USB_STORAGE_PATH;
	public static File ARCHIVE_EXTRACT_DIR;
	static File USB_CACHE_DIR;
	static File TEMP_ROTATE_CACHE_DIR;
	static File FTP_CACHE_DIR;
	static File APK_ICON_DIR;
	static final List<String>APK_ICON_PACKAGE_NAME_LIST=new ArrayList<>();
	static int ARCHIVE_CACHE_DIR_LENGTH;


	static final HashMap<String,List<LibraryAlbumSelectDialog.LibraryDirPOJO>> LIBRARY_FILTER_HASHMAP=new HashMap<>();

	static List<UriPOJO> URI_PERMISSION_LIST=new ArrayList<>();
	static int ORIENTATION;
	static int SCREEN_WIDTH,SCREEN_HEIGHT,DIALOG_WIDTH,DIALOG_HEIGHT,WIDTH;
	static float SCREEN_RATIO;
	static String SORT;
	static String STORAGE_ANALYSER_SORT;
	static String APP_MANAGER_SORT;
	static String THEME;
	static int RECYCLER_VIEW_FONT_SIZE_FACTOR;

	
	static final String TEXT_REGEX="(?i)txt|json|java|xml|cpp|c|h|log|html|htm";
	static final String RTF_REGEX="(?i)rtf";
	static final String IMAGE_REGEX="(?i)png|jpg|jpeg|svg|gif|tif|webp|avif";
	static final String AUDIO_REGEX="(?i)mp3|ogg|wav|aac|wma|opus|m4r|m4a|awb";
	static final String VIDEO_REGEX="(?i)3gp|mp4|avi|mov|flv|wmv|webm";
	static final String ZIP_REGEX="(?i)zip|rar|jar|7z";
	static final String UNIX_ARCHIVE_REGEX="(?i)tar|gzip|gz";
	static final String GZIP_REGEX="(?i)gzip|gz";
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
	static float ONE_DP;
	//static float ONE_SP;
	static int TWO_DP;
	static int THIRTY_SIX_DP;
	static int SELECTOR_ICON_DIMENSION;

	static int FOUR_DP;
	static int FIVE_DP;
	static int SIX_DP;
	static int EIGHT_DP;
	static int TEN_DP;
	static int TWELVE_DP;
	static int FOURTEEN_DP;
	static int RECYCLERVIEW_ITEM_SPACING;
	static int LIST_POPUP_WINDOW_DROP_DOWN_OFFSET;
	//static int DRAWER_WIDTH;

	static final float DISABLE_ALFA= (float) 0.4;
	static final float ENABLE_ALFA= (float) 1.0;
	
	static final int FONT_SIZE_SMALL_FIRST_LINE=15;
	static final int FONT_SIZE_SMALL_DETAILS_LINE=12;
	
	static final int FONT_SIZE_MEDIUM_FIRST_LINE=17;
	static final int FONT_SIZE_MEDIUM_DETAILS_LINE=13;
	
	static final int FONT_SIZE_LARGE_FIRST_LINE=19;
	static final int FONT_SIZE_LARGE_DETAILS_LINE=15;
	
	static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL=8;
	static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM=9;
	static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE=10;
	
	static boolean BYTE_COUNT_BLOCK_1000;
	static boolean FILE_GRID_LAYOUT;
	static boolean IMAGE_VIDEO_GRID_LAYOUT;
	static boolean SHOW_FILE_PATH;
	static boolean IS_TABLET;
	static boolean RECOGNISE_USB;

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
			new MimePOJO("GZip","application/gzip",GZIP_REGEX),
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
	static public int ACTION_BAR_HEIGHT_IN_DP;

	static public final boolean AFTER_ARCHIVE_GOTO_DEST_FOLDER=true;

	static public boolean DETAILED_SEARCH_LIBRARY;

	static public final String FILEX_PACKAGE="svl.kadatha.filex";

	static public boolean SHARE_USB;

	static public final String LOCAL_BROADCAST_DELETE_FILE_ACTION=FILEX_PACKAGE+".FILE_DELETE";
	static public final String LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION=FILEX_PACKAGE+".MODIFICATION_OBSERVED";


	static final LinkedHashMap<String,SpacePOJO> SPACE_ARRAY=new LinkedHashMap<>();

	static DividerItemDecoration DIVIDERITEMDECORATION;

	static public final String TAG="shankar";

	static final long CACHE_FILE_MAX_LIMIT=1024*1024*10;
	static boolean WHETHER_TO_CLEAR_CACHE_TODAY;
	static int SIZE_APK_ICON_LIST, CURRENT_MONTH;
	private static final String FTP_TAG = "Ftp-Global";

	static void GET_URI_PERMISSIONS_LIST(Context context)
	{
		URI_PERMISSION_LIST=new ArrayList<>();
		List<UriPermission> permission_list=context.getContentResolver().getPersistedUriPermissions();
		if(!permission_list.isEmpty())
		{
			for(UriPermission permission : permission_list)
			{
				if(permission.isWritePermission())
				{
					Uri uri=permission.getUri();
					String uri_authority=uri.getAuthority();
					String uri_path=FileUtil.getFullPathFromTreeUri(uri,context);
					if(uri_path!=null)
					{
						if(uri_path.equals(File.separator) && uri_authority.equals("com.android.externalstorage.documents") )
						{
							final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
							try
							{
								App.getAppContext().getContentResolver().releasePersistableUriPermission(uri,takeFlags);
							}
							catch (SecurityException e){}
						}
						else {
							URI_PERMISSION_LIST.add(new UriPOJO(uri,uri_authority,uri_path)); //check path is not equl to file separator as it becomes to / when SD card is removed
						}
					}

				}
			}
		}
	}

	static void REMOVE_ALL_URI_PERMISSIONS()
	{
		GET_URI_PERMISSIONS_LIST(App.getAppContext());
		Iterator<UriPOJO> iterator=URI_PERMISSION_LIST.iterator();

		while(iterator.hasNext())
		{
			UriPOJO uriPOJO=iterator.next();
			final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			try
			{
				App.getAppContext().getContentResolver().releasePersistableUriPermission(uriPOJO.get_uri(),takeFlags);
			}
			catch (SecurityException e){}
			iterator.remove();
		}
	}

	static void REMOVE_USB_URI_PERMISSIONS()
	{

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

		GET_URI_PERMISSIONS_LIST(App.getAppContext());
		Iterator<UriPOJO> iterator=URI_PERMISSION_LIST.iterator();

		while(iterator.hasNext())
		{
			UriPOJO uriPOJO=iterator.next();
			if(uriPOJO.get_authority().equals(uri_authority) && IS_CHILD_FILE(uriPOJO.get_path(),uri_path) && uriPOJO.get_path().length()>uri_path.length())
			{
				final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try
                {
                    context.getContentResolver().releasePersistableUriPermission(uriPOJO.get_uri(),takeFlags);
                }
                catch (SecurityException e){}
				iterator.remove();
			}
		}

		if(URI_PERMISSION_LIST.isEmpty())
		{
			final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			try
			{
				context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
			}
			catch (SecurityException e)
			{
				REMOVE_ALL_URI_PERMISSIONS();
			}

		}
		else
		{
			boolean parent_uri_exists = false;
			for(UriPOJO uriPOJO:URI_PERMISSION_LIST)
			{
				if(uriPOJO.get_authority().equals(uri_authority))
				{
					if(IS_CHILD_FILE(uri_path,uriPOJO.get_path()))
					{
						parent_uri_exists=true;
						break;
					}
				}
			}
			if(!parent_uri_exists)
			{
				final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				try
				{
					context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
				}
				catch (SecurityException e)
				{
					REMOVE_ALL_URI_PERMISSIONS();
				}

			}
		}
		GET_URI_PERMISSIONS_LIST(App.getAppContext());
	}


	public static UriPOJO CHECK_AVAILABILITY_URI_PERMISSION(String file_path, FileObjectType fileObjectType)
	{
		for(UriPOJO uriPOJO:URI_PERMISSION_LIST)
		{
			if(fileObjectType==FileObjectType.USB_TYPE && uriPOJO.get_authority().equals(UsbDocumentProvider.DOCUMENTS_AUTHORITY))
			{
				if(IS_CHILD_FILE(file_path,uriPOJO.get_path()))
				{
					return uriPOJO;
				}
			}
			else if(fileObjectType==FileObjectType.FILE_TYPE &&  uriPOJO.get_authority().equals("com.android.externalstorage.documents"))
			{
				if(uriPOJO.get_path().equals(File.separator)) return null;
				if(IS_CHILD_FILE(file_path,uriPOJO.get_path()))
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
			//ONE_SP=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,1,displayMetrics);
			ONE_DP=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,1,displayMetrics);

			TWO_DP= (int) (ONE_DP*2);
			FOUR_DP= (int) (ONE_DP*4);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,displayMetrics);
			FIVE_DP= (int) (ONE_DP*5);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,5,displayMetrics);
			SIX_DP= (int) (ONE_DP*6);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,displayMetrics);
			EIGHT_DP= (int) (ONE_DP*8);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8,displayMetrics);
			TEN_DP= (int) (ONE_DP*10);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10,displayMetrics);
			TWELVE_DP= (int) (ONE_DP*12);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,12,displayMetrics);
			FOURTEEN_DP= (int) (ONE_DP*14);//TEN_DP+FOUR_DP;
			THIRTY_SIX_DP= (int) (ONE_DP*36);


			int list_s=FIVE_DP;
			int list_g=IS_TABLET ? TWELVE_DP : TEN_DP;



			SELECTOR_ICON_DIMENSION=TEN_DP+TEN_DP+SIX_DP;
			RECYCLERVIEW_ITEM_SPACING=IS_TABLET ? TEN_DP : FIVE_DP;
			LIST_POPUP_WINDOW_DROP_DOWN_OFFSET=IS_TABLET ? TEN_DP : SIX_DP;


			IMAGEVIEW_DIMENSION_SMALL_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL+FOUR_DP;
			IMAGEVIEW_DIMENSION_MEDIUM_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM+FOUR_DP;
			IMAGEVIEW_DIMENSION_LARGE_LIST=list_s*IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE+FOUR_DP;

			IMAGEVIEW_DIMENSION_SMALL_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL;
			IMAGEVIEW_DIMENSION_MEDIUM_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM;
			IMAGEVIEW_DIMENSION_LARGE_GRID=list_g*IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE;

			BUTTON_HEIGHT= (int) (ONE_DP*48);

			DIVIDERITEMDECORATION=new DividerItemDecoration(context,DividerItemDecoration.VERTICAL);
		}
		GRID_COUNT_SMALL=  WIDTH/(IMAGEVIEW_DIMENSION_SMALL_GRID+RECYCLERVIEW_ITEM_SPACING);
		GRID_COUNT_MEDIUM=  WIDTH/(IMAGEVIEW_DIMENSION_MEDIUM_GRID+RECYCLERVIEW_ITEM_SPACING);
		GRID_COUNT_LARGE=  WIDTH/(IMAGEVIEW_DIMENSION_LARGE_GRID+RECYCLERVIEW_ITEM_SPACING);


		//cache directory setting
		ARCHIVE_EXTRACT_DIR=new File(context.getFilesDir(),"Archive");
		USB_CACHE_DIR=context.getExternalFilesDir(".usb_cache");
		TEMP_ROTATE_CACHE_DIR=context.getExternalFilesDir(".temp_rotate_cache");
		FTP_CACHE_DIR=context.getExternalFilesDir(".ftp_cache");
		APK_ICON_DIR=context.getExternalFilesDir(".apk_icons");
		APK_ICON_PACKAGE_NAME_LIST.addAll(Arrays.asList(APK_ICON_DIR.list()));
		SIZE_APK_ICON_LIST=APK_ICON_PACKAGE_NAME_LIST.size();
		ARCHIVE_CACHE_DIR_LENGTH=ARCHIVE_EXTRACT_DIR.getAbsolutePath().length();

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

		if(FileSelectorActivity.SORT==null)
		{
			FileSelectorActivity.SORT=tinyDB.getString("file_selector_sort");
			if(FileSelectorActivity.SORT.trim().isEmpty() || !SORT_CODE_SET.contains(FileSelectorActivity.SORT))
			{
				FileSelectorActivity.SORT="d_name_asc";
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

		if(APP_MANAGER_SORT==null)
		{
			APP_MANAGER_SORT=tinyDB.getString("app_manager_sort");
			if(APP_MANAGER_SORT.trim().isEmpty() || !SORT_CODE_SET.contains(APP_MANAGER_SORT))
			{
				APP_MANAGER_SORT="d_name_asc";
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
		IMAGE_VIDEO_GRID_LAYOUT=tinyDB.getBoolean("image_video_grid_layout");

		//
		FileSelectorActivity.FILE_GRID_LAYOUT=tinyDB.getBoolean("file_selector_file_grid_layout");
		//

		AppManagerActivity.FILE_GRID_LAYOUT=tinyDB.getBoolean("app_manager_file_grid_layout");
		//
		DETAILED_SEARCH_LIBRARY=tinyDB.getBoolean("detailed_search_library");
		//

		SHOW_FILE_PATH=tinyDB.getBoolean("show_file_path");

		//
		RECOGNISE_USB=tinyDB.getBoolean("recognise_usb");
		//
		RECYCLER_VIEW_FONT_SIZE_FACTOR=tinyDB.getInt("recycler_view_font_size_factor");
		FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR=tinyDB.getInt("file_selector_recycler_view_font_size_factor");
		if(!tinyDB.getBoolean("not_first_run"))
		{
			RECYCLER_VIEW_FONT_SIZE_FACTOR=1;
			FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR=1;
			IMAGE_VIDEO_GRID_LAYOUT=true;
			tinyDB.putBoolean("not_first_run",true);
			tinyDB.putInt("recycler_view_font_size_factor",RECYCLER_VIEW_FONT_SIZE_FACTOR);
			tinyDB.putInt("file_selector_recycler_view_font_size_factor",FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR);
			tinyDB.putBoolean("image_video_grid_layout",IMAGE_VIDEO_GRID_LAYOUT);

		}
		//


		if(RECYCLER_VIEW_FONT_SIZE_FACTOR!=0 && RECYCLER_VIEW_FONT_SIZE_FACTOR!=1 && RECYCLER_VIEW_FONT_SIZE_FACTOR!=2)
		{
			RECYCLER_VIEW_FONT_SIZE_FACTOR=1;
		}

		switch(RECYCLER_VIEW_FONT_SIZE_FACTOR)
		{
			case 0:
				GRID_COUNT=GRID_COUNT_SMALL;
				break;
			case 2:
				GRID_COUNT=GRID_COUNT_LARGE;
				break;
			default:
				GRID_COUNT=GRID_COUNT_MEDIUM;
				break;
		}
//

		if(FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR!=0 && FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR!=1 && FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR!=2)
		{
			RECYCLER_VIEW_FONT_SIZE_FACTOR=1;
		}

		switch(FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR)
		{
			case 0:
				FileSelectorActivity.GRID_COUNT=GRID_COUNT_SMALL;
				break;
			case 2:
				FileSelectorActivity.GRID_COUNT=GRID_COUNT_LARGE;
				break;
			default:
				FileSelectorActivity.GRID_COUNT=GRID_COUNT_MEDIUM;
				break;
		}

		DETERMINE_TO_CLEAR_CACHE_TODAY(tinyDB);
	}


	static void GET_ACTION_BAR_HEIGHT(Context context)
	{
		TypedValue tv = new TypedValue();
		if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
		{
			ACTION_BAR_HEIGHT = TypedValue.complexToDimensionPixelSize(tv.data,context.getResources().getDisplayMetrics());
			float density = context.getResources().getDisplayMetrics().density;
			ACTION_BAR_HEIGHT_IN_DP = (int) (ACTION_BAR_HEIGHT / density);
		}
	}

	static void LOCAL_BROADCAST(String action, LocalBroadcastManager localBroadcastManager, String activity_name)
	{
		Intent intent=new Intent();
		intent.setAction(action);
		intent.putExtra("activity_name",activity_name);
		localBroadcastManager.sendBroadcast(intent);
	}

	static final FilenameFilter File_NAME_FILTER=new FilenameFilter()
	{
		@Override
		public boolean accept(File file, String s) {
			return !s.startsWith(".");
		}
	};


	@RequiresApi(api = Build.VERSION_CODES.O)
	static DirectoryStream.Filter<Path> GET_NIO_FILE_NAME_FILTER()
	{
		return new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path path) throws IOException {
				return !Files.isHidden(path);
			}
		};
	}



	static FilePOJO GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR()
	{
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		if(repositoryClass.storage_dir.get(0).getPath().equals("/"))
		{
			return repositoryClass.storage_dir.get(1);
		}
		else
		{
			return repositoryClass.storage_dir.get(0);
		}
	}

	static void GET_STORAGE_DIR(Context context)
	{
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		if(repositoryClass.storage_dir.isEmpty())
		{
			repositoryClass.storage_dir =new ArrayList<>(StorageUtil.getSdCardPaths(context,true));
			INTERNAL_PRIMARY_STORAGE_PATH=GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR().getPath();
			WORKOUT_AVAILABLE_SPACE();
		}
	}

	static void SHOW_LIST_POPUP_WINDOW_BOTTOM(View bottom_toolbar, PopupWindow listPopWindow, int desiredDistanceFromToolbar) {
		View rootView = bottom_toolbar.getRootView();
		int[] location = new int[2];
		bottom_toolbar.getLocationInWindow(location);

		int toolbarTop = location[1];
		int rootHeight = rootView.getHeight();

		int offset = rootHeight - toolbarTop + desiredDistanceFromToolbar;

		listPopWindow.showAtLocation(rootView, Gravity.BOTTOM | Gravity.END, 0, offset);
	}


	public static String GET_FILE_PERMISSION(String file_path)
	{
		String permission_string = null;
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
		return success;
	}


	static void WORKOUT_AVAILABLE_SPACE()
	{
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		for(FilePOJO filePOJO:repositoryClass.storage_dir)
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
				if(MainActivity.usbFileRoot==null)return;
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
		uri = FileProvider.getUriForFile(context,context.getPackageName()+".provider",f);
		final Intent intent=new Intent(Intent.ACTION_VIEW);
		mime_type=FileIntentDispatch.SET_INTENT_FOR_VIEW(intent,mime_type,"",file_extn,null,false,uri);
		List<ResolveInfo> resolveInfoList=context.getPackageManager().queryIntentActivities(intent,0);
		return !resolveInfoList.isEmpty();
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
					if(IS_CHILD_FILE(dest_folder,iterator.next()))
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
					if(IS_CHILD_FILE(dest_folder,iterator.next()))
					{
						iterator.remove();
					}
				}
			}
		}

	}


	public static boolean CHECK_FTP_SERVER_CONNECTED()
	{
		return FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO).testServerConnection();
	}


	public static boolean CHECK_WHETHER_STORAGE_DIR_CONTAINS_FTP_FILE_OBJECT(FileObjectType fileObjectType)
	{
		if(fileObjectType!=FileObjectType.FTP_TYPE) return false;
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		Iterator<FilePOJO> iterator=repositoryClass.storage_dir.iterator();
		while(iterator.hasNext())
		{
			if(iterator.next().getFileObjectType()==fileObjectType)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean CHECK_EXISTENCE_FILE_IN_FILE_POJO(String file_path,List<FilePOJO> destFilePOJOs)
	{
		File f=new File(file_path);
		String file_name=f.getName();
		if(destFilePOJOs!=null)
		{
			for(FilePOJO filePOJO:destFilePOJOs)
			{
				if(filePOJO.getName().equals(file_name))
				{
					return true;
				}
			}
		}
		else
		{
			return false;
		}
		return false;
	}

	public static boolean WHETHER_FILE_ALREADY_EXISTS(FileObjectType fileObjectType,String file_path,List<FilePOJO> destFilePOJOs)
	{
		if(file_path==null || file_path.equals("")) return false;
		if(fileObjectType== FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			File new_file=new File(file_path);
			return new_file.exists();

		}
		else if(fileObjectType== FileObjectType.USB_TYPE)
		{
			UsbFile usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,file_path);
			return usbFile != null;

		}
		else if(fileObjectType==FileObjectType.FTP_TYPE)
		{
			return CHECK_EXISTENCE_FILE_IN_FILE_POJO(file_path,destFilePOJOs);
		}
		else if(fileObjectType==FileObjectType.ROOT_TYPE)
		{
			if(RootUtils.CAN_RUN_ROOT_COMMANDS())
			{
				return !RootUtils.WHETHER_FILE_EXISTS(file_path);
			}
			else
			{
				return false;
			}

		}
		return false;
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

	public static String GET_FileObjectType(FileObjectType fileObjectType)
	{
		switch (fileObjectType)
		{
			case FILE_TYPE:
				return "(Device)";
			case USB_TYPE:
				return "(USB)";
			case FTP_TYPE:
				return "(FTP)";
			default:
				return "";
		}
	}


	public static long AVAILABLE_MEMORY_MB(){
		Runtime runtime=Runtime.getRuntime();
		long usedMemInMB=(runtime.totalMemory()-runtime.freeMemory())/1048576L;
		long maxHeapSizeInMB=runtime.maxMemory()/1048576L;
		return maxHeapSizeInMB-usedMemInMB;
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

	public static void print(Context context,String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}

	public static void print_background_thread(Context context,String msg)
	{
		Handler handler=new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
				handler.removeCallbacksAndMessages(null);
			}
		});
	}


	public static String CONCATENATE_PARENT_CHILD_PATH(String parent_file_path, String child_file_name)
	{
		if(parent_file_path==null)parent_file_path="";
		return parent_file_path.endsWith(File.separator) ? parent_file_path + child_file_name : parent_file_path + File.separator + child_file_name;
	}

	public static void CLEAR_CACHE()
	{
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		Iterator<Map.Entry<String, List<FilePOJO>>> iterator=repositoryClass.hashmap_file_pojo.entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<String,List<FilePOJO>> entry=iterator.next();
			if(!entry.getKey().startsWith(FileObjectType.SEARCH_LIBRARY_TYPE.toString()))
			{
				iterator.remove();
			}
		}
	}

	public static void WARN_NOTIFICATIONS_DISABLED(Context context,String channelId, boolean[] alreadyWarned) {
		if(alreadyWarned[0])return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (Build.VERSION.SDK_INT >= 33){
				if(context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
				{
					print(context, context.getString(R.string.notification_not_enabled_grant_permission_in_device_settings));
					alreadyWarned[0] = true;
					return;
				}
			}
			else
			{
				NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				if (!manager.areNotificationsEnabled()) {
					print(context,context.getString(R.string.notification_not_enabled_grant_permission_in_device_settings));
					alreadyWarned[0]=true;
					return;
				}
			}

			NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			List<NotificationChannel> channels = null;
			channels = manager.getNotificationChannels();
			for (NotificationChannel channel : channels)
			{
				if (channel.getId().equals(channelId) && channel.getImportance() == NotificationManager.IMPORTANCE_NONE)
				{
					String message=context.getString(R.string.enable_specified_notification_channel_for_better_control);
					switch (channelId)
					{
						case NotifManager.CHANNEL_ID:
							print(context,NotifManager.CHANNEL_NAME+" - "+message);
							break;
						case AudioPlayerService.CHANNEL_ID:
							print(context,AudioPlayerService.CHANNEL_NAME+" - "+message);
							break;
						case FsNotification.CHANNEL_ID:
							print(context,FsNotification.CHANNEL_NAME+" - "+message);
							break;
					}
					alreadyWarned[0]=true;
					break;
				}
			}
		}
		else
		{
			if(!NotificationManagerCompat.from(context).areNotificationsEnabled())
			{
				print(context,context.getString(R.string.notification_not_enabled_grant_permission_in_device_settings));
				alreadyWarned[0]=true;
			}
		}
	}

	public static Uri GET_ALBUM_ART_URI(String album_id)
	{
		long id;
		try
		{
			id=Long.parseLong(album_id);
		}
		catch (NumberFormatException e)
		{
			id=0;
		}

		return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),id);
	}

	public static void DELETE_DIRECTORY_ASYNCHRONOUSLY(File dir)
	{
		ExecutorService executorService=MyExecutorService.getExecutorService();
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				FileUtil.deleteNativeDirectory(dir);
			}
		});
	}

	public static void DETERMINE_TO_CLEAR_CACHE_TODAY(TinyDB tinyDB)
	{
		int cache_cleared_month=tinyDB.getInt("cache_cleared_month");
		CURRENT_MONTH= Calendar.getInstance().get(Calendar.MONTH);
		if(cache_cleared_month<CURRENT_MONTH || (cache_cleared_month==11 && CURRENT_MONTH!=11))
		{
			WHETHER_TO_CLEAR_CACHE_TODAY=true;
		}
	}


	public static File COPY_TO_USB_CACHE(String file_path)
	{
		File cache_file=new File(USB_CACHE_DIR,file_path);
		long[] bytes_read=new long[1];
		if(!cache_file.exists())
		{
			File parent_file=cache_file.getParentFile();
			if(parent_file!=null)
			{
				FileUtil.mkdirsNative(parent_file);
				FileUtil.createNativeNewFile(cache_file);
				UsbFile targetUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,file_path);
				if(targetUsbFile!=null)
				{
					FileUtil.copy_UsbFile_File(targetUsbFile,cache_file,false,bytes_read);
				}
			}

		}
		return cache_file;
	}

	public static File COPY_TO_FTP_CACHE(String file_path)
	{
		File cache_file=new File(FTP_CACHE_DIR,file_path);
		long []bytes_read=new long[1];
		if(!cache_file.exists())
		{
			File parent_file=cache_file.getParentFile();
			if(parent_file!=null)
			{
				FileUtil.mkdirsNative(parent_file);
				FileUtil.createNativeNewFile(cache_file);
				//if(CHECK_OTHER_FTP_SERVER_CONNECTED(FtpClientRepository_old.getInstance().ftpClientForCopyView))
				{
					FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
					FTPClient ftpClient=null;
                    try {
                        ftpClient=ftpClientRepository.getFtpClient();
						try (InputStream inputStream= ftpClient.retrieveFileStream(file_path); OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(cache_file))) {
							FileUtil.bufferedCopy(inputStream, outputStream, false, bytes_read);
							ftpClient.completePendingCommand();
							return cache_file;

						} catch (Exception e) {

							return cache_file;
						}

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
					finally {
						if (ftpClientRepository != null && ftpClient != null) {
							ftpClientRepository.releaseFtpClient(ftpClient);
							Timber.tag(FTP_TAG).d("FTP client released");
						}
					}
				}
			}

		}
		return cache_file;
	}


	public static Bitmap scaleToFitHeight(Bitmap bitmap,int height){
		float factor=height/(float)bitmap.getHeight();
		return Bitmap.createScaledBitmap(bitmap,(int)(bitmap.getWidth()*factor),height,true);
	}
}
