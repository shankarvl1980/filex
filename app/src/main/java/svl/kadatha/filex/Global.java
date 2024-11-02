package svl.kadatha.filex;

import android.Manifest;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
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
import java.util.Stack;
import java.util.concurrent.ExecutorService;

import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import timber.log.Timber;

public class Global {
    static public final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy");
    static public final int LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY = 4000;
    static public final boolean AFTER_ARCHIVE_GOTO_DEST_FOLDER = true;
    static public final String FILEX_PACKAGE = "svl.kadatha.filex";
    static public final String LOCAL_BROADCAST_DELETE_FILE_ACTION = FILEX_PACKAGE + ".FILE_DELETE";
    static public final String LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION = FILEX_PACKAGE + ".MODIFICATION_OBSERVED";
    static public final String LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION = FILEX_PACKAGE + ".STORAGE_DIR_REFRESH";
    static public final String LOCAL_BROADCAST_POP_UP_NETWORK_FILE_TYPE_FRAGMENT = FILEX_PACKAGE + ".POP_UP_NETWORK_FILE_TYPE_FRAGMENT";
    static public final String TAG = "shankar";
    public static final List<FileObjectType> NETWORK_FILE_OBJECT_TYPES = Arrays.asList(FileObjectType.FTP_TYPE, FileObjectType.SFTP_TYPE, FileObjectType.WEBDAV_TYPE, FileObjectType.SMB_TYPE);
    static final List<String> APK_ICON_PACKAGE_NAME_LIST = new ArrayList<>();
    static final HashMap<String, List<LibraryAlbumSelectDialog.LibraryDirPOJO>> LIBRARY_FILTER_HASHMAP = new HashMap<>();
    static final String TEXT_REGEX = "(?i)txt|json|java|xml|cpp|c|h|log|html|htm";
    static final String RTF_REGEX = "(?i)rtf";
    static final String IMAGE_REGEX = "(?i)png|jpg|jpeg|svg|gif|tif|webp|avif";
    static final String AUDIO_REGEX = "(?i)mp3|ogg|wav|aac|wma|opus|m4r|m4a|awb";
    static final String VIDEO_REGEX = "(?i)3gp|mp4|avi|mov|flv|wmv|webm";
    static final String ZIP_REGEX = "(?i)zip|rar|jar|7z";
    static final String UNIX_ARCHIVE_REGEX = "(?i)tar|gzip|gz";
    static final String GZIP_REGEX = "(?i)gzip|gz";
    static final String APK_REGEX = "(?i)apk";
    static final String PDF_REGEX = "(?i)pdf";
    static final String DOC_REGEX = "(?i)doc|docx";
    static final String XLS_REGEX = "(?i)xls|xlsx";
    static final String PPT_REGEX = "(?i)ppt|pptx";
    static final String DB_REGEX = "(?i)db";
    static final float DISABLE_ALFA = (float) 0.4;
    static final float ENABLE_ALFA = (float) 1.0;
    static final int FONT_SIZE_SMALL_FIRST_LINE = 15;
    static final int FONT_SIZE_SMALL_DETAILS_LINE = 12;
    static final int FONT_SIZE_MEDIUM_FIRST_LINE = 17;
    static final int FONT_SIZE_MEDIUM_DETAILS_LINE = 13;
    static final int FONT_SIZE_LARGE_FIRST_LINE = 19;
    static final int FONT_SIZE_LARGE_DETAILS_LINE = 15;
    static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL = 8;
    static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM = 9;
    static final int IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE = 10;
    static final Set<String> SORT_CODE_SET = new HashSet<>(Arrays.asList("d_name_asc", "d_name_desc", "d_date_asc", "d_date_desc", "d_size_asc", "d_size_desc",
            "f_name_asc", "f_name_desc", "f_date_asc", "f_date_desc", "f_size_asc", "f_size_desc"));
    static final Set<String> THEME_CODE_SET = new HashSet<>(Arrays.asList("system", "light", "dark"));
    static final List<MimePOJO> SUPPORTED_MIME_POJOS = new ArrayList<>(Arrays.asList(
            new MimePOJO("Text", "text/*", TEXT_REGEX),
            new MimePOJO("Image", "image/*", IMAGE_REGEX),
            new MimePOJO("Audio", "audio/*", AUDIO_REGEX),
            new MimePOJO("Video", "video/*", VIDEO_REGEX),
            new MimePOJO("PDF", "application/pdf", PDF_REGEX)));
    static final List<MimePOJO> MIME_POJOS = new ArrayList<>(Arrays.asList(
            new MimePOJO("MS Word", "application/msword", DOC_REGEX),
            new MimePOJO("MS Excel", "application/vnd.ms-excel", XLS_REGEX),
            new MimePOJO("MS PowerPoint", "application/vnd.ms-powerpoint", PPT_REGEX),
            new MimePOJO("DB", "application/vnd.sqlite3", DB_REGEX),
            new MimePOJO("Zip", "application/zip", ZIP_REGEX),
            new MimePOJO("GZip", "application/gzip", GZIP_REGEX),
            new MimePOJO("RTF", "application/rtf", RTF_REGEX),
            new MimePOJO("APK", "application/vnd.android.package-archive", APK_REGEX),

            //same as supported mimepojos above
            new MimePOJO("Text", "text/*", TEXT_REGEX),
            new MimePOJO("Image", "image/*", IMAGE_REGEX),
            new MimePOJO("Audio", "audio/*", AUDIO_REGEX),
            new MimePOJO("Video", "video/*", VIDEO_REGEX),
            new MimePOJO("PDF", "application/pdf", PDF_REGEX)));
    static final LinkedHashMap<String, SpacePOJO> SPACE_ARRAY = new LinkedHashMap<>();
    static final long CACHE_FILE_MAX_LIMIT = 1024 * 1024 * 20;
    static final FilenameFilter File_NAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File file, String s) {
            return !s.startsWith(".");
        }
    };
    public static File ARCHIVE_EXTRACT_DIR;
    static public int ACTION_BAR_HEIGHT;
    static public int ACTION_BAR_HEIGHT_IN_DP;
    static public boolean DETAILED_SEARCH_LIBRARY;
    public static FileObjectType USB_CACHED_FILE_OBJECT;
    static String INTERNAL_PRIMARY_STORAGE_PATH = "";
    static String USB_STORAGE_PATH;
    static File USB_CACHE_DIR;
    static File TEMP_ROTATE_CACHE_DIR;
    static File FTP_CACHE_DIR;
    static File SFTP_CACHE_DIR;
    static File WEBDAV_CACHE_DIR;
    static File SMB_CACHE_DIR;
    static File ROOT_CACHE_DIR;
    static File APK_ICON_DIR;
    //static int DRAWER_WIDTH;
    public static int ARCHIVE_CACHE_DIR_LENGTH;
    static List<UriPOJO> URI_PERMISSION_LIST = new ArrayList<>();
    static int ORIENTATION;
    static int SCREEN_WIDTH, SCREEN_HEIGHT, DIALOG_WIDTH, DIALOG_HEIGHT, WIDTH;
    static float SCREEN_RATIO;
    static String SORT;
    static String STORAGE_ANALYSER_SORT;
    static String APP_MANAGER_SORT;
    static String THEME;
    static int RECYCLER_VIEW_FONT_SIZE_FACTOR;
    static int IMAGEVIEW_DIMENSION_SMALL_LIST;
    static int IMAGEVIEW_DIMENSION_MEDIUM_LIST;
    static int IMAGEVIEW_DIMENSION_LARGE_LIST;
    static int IMAGEVIEW_DIMENSION_SMALL_GRID;
    static int IMAGEVIEW_DIMENSION_MEDIUM_GRID;
    static int IMAGEVIEW_DIMENSION_LARGE_GRID;
    static int GRID_COUNT_SMALL, GRID_COUNT_MEDIUM, GRID_COUNT_LARGE, GRID_COUNT;
    static int BUTTON_HEIGHT;
    static float ONE_DP;
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
    static boolean BYTE_COUNT_BLOCK_1000;
    static boolean FILE_GRID_LAYOUT;
    static boolean IMAGE_VIDEO_GRID_LAYOUT;
    static boolean SHOW_FILE_PATH;
    static boolean IS_TABLET;
    static boolean RECOGNISE_USB;
    static DividerItemDecoration DIVIDERITEMDECORATION;
    static boolean WHETHER_TO_CLEAR_CACHE_TODAY;
    static int SIZE_APK_ICON_LIST, CURRENT_MONTH;

    static void GET_URI_PERMISSIONS_LIST(Context context) {
        URI_PERMISSION_LIST = new ArrayList<>();
        List<UriPermission> permission_list = context.getContentResolver().getPersistedUriPermissions();
        if (!permission_list.isEmpty()) {
            for (UriPermission permission : permission_list) {
                if (permission.isWritePermission()) {
                    Uri uri = permission.getUri();
                    String uri_authority = uri.getAuthority();
                    String uri_path = FileUtil.getFullPathFromTreeUri(uri, context);
                    if (uri_path != null) {
                        if (uri_path.equals(File.separator) && uri_authority.equals("com.android.externalstorage.documents")) {
                            final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            try {
                                App.getAppContext().getContentResolver().releasePersistableUriPermission(uri, takeFlags);
                            } catch (SecurityException e) {
                            }
                        } else {
                            URI_PERMISSION_LIST.add(new UriPOJO(uri, uri_authority, uri_path)); //check path is not equl to file separator as it becomes to / when SD card is removed
                        }
                    }
                }
            }
        }
    }

    static void REMOVE_ALL_URI_PERMISSIONS() {
        GET_URI_PERMISSIONS_LIST(App.getAppContext());
        Iterator<UriPOJO> iterator = URI_PERMISSION_LIST.iterator();

        while (iterator.hasNext()) {
            UriPOJO uriPOJO = iterator.next();
            final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                App.getAppContext().getContentResolver().releasePersistableUriPermission(uriPOJO.get_uri(), takeFlags);
            } catch (SecurityException e) {
            }
            iterator.remove();
        }
    }

    static void REMOVE_USB_URI_PERMISSIONS() {

    }

    static void ON_REQUEST_URI_PERMISSION(Context context, Uri treeUri) {
        String uri_authority = treeUri.getAuthority();
        String uri_path;
        if (uri_authority.equals(UsbDocumentProvider.DOCUMENTS_AUTHORITY)) {
            final String docId = DocumentsContract.getTreeDocumentId(treeUri);
            final String[] split = docId.split(":");
            if (split.length == 1) {
                uri_path = "/";
            } else {
                uri_path = split[1];
            }
        } else {
            uri_path = FileUtil.getFullPathFromTreeUri(treeUri, context);
        }

        GET_URI_PERMISSIONS_LIST(App.getAppContext());
        Iterator<UriPOJO> iterator = URI_PERMISSION_LIST.iterator();

        while (iterator.hasNext()) {
            UriPOJO uriPOJO = iterator.next();
            if (uriPOJO.get_authority().equals(uri_authority) && IS_CHILD_FILE(uriPOJO.get_path(), uri_path) && uriPOJO.get_path().length() > uri_path.length()) {
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    context.getContentResolver().releasePersistableUriPermission(uriPOJO.get_uri(), takeFlags);
                } catch (SecurityException e) {
                }
                iterator.remove();
            }
        }

        if (URI_PERMISSION_LIST.isEmpty()) {
            final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
            } catch (SecurityException e) {
                REMOVE_ALL_URI_PERMISSIONS();
            }
        } else {
            boolean parent_uri_exists = false;
            for (UriPOJO uriPOJO : URI_PERMISSION_LIST) {
                if (uriPOJO.get_authority().equals(uri_authority)) {
                    if (IS_CHILD_FILE(uri_path, uriPOJO.get_path())) {
                        parent_uri_exists = true;
                        break;
                    }
                }
            }
            if (!parent_uri_exists) {
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                } catch (SecurityException e) {
                    REMOVE_ALL_URI_PERMISSIONS();
                }
            }
        }
        GET_URI_PERMISSIONS_LIST(App.getAppContext());
    }

    public static UriPOJO CHECK_AVAILABILITY_URI_PERMISSION(String file_path, FileObjectType fileObjectType) {
        for (UriPOJO uriPOJO : URI_PERMISSION_LIST) {
            if (fileObjectType == FileObjectType.USB_TYPE && uriPOJO.get_authority().equals(UsbDocumentProvider.DOCUMENTS_AUTHORITY)) {
                if (IS_CHILD_FILE(file_path, uriPOJO.get_path())) {
                    return uriPOJO;
                }
            } else if (fileObjectType == FileObjectType.FILE_TYPE && uriPOJO.get_authority().equals("com.android.externalstorage.documents")) {
                if (uriPOJO.get_path().equals(File.separator)) {
                    return null;
                }
                if (IS_CHILD_FILE(file_path, uriPOJO.get_path())) {
                    return uriPOJO;
                }
            }
        }
        return null;
    }

    static void GET_SCREEN_DIMENSIONS(Context context) {
        ORIENTATION = context.getResources().getConfiguration().orientation;
        if (ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            SCREEN_WIDTH = context.getResources().getDisplayMetrics().heightPixels;
            SCREEN_HEIGHT = context.getResources().getDisplayMetrics().widthPixels;
            WIDTH = SCREEN_HEIGHT;
        } else {
            SCREEN_WIDTH = context.getResources().getDisplayMetrics().widthPixels;
            SCREEN_HEIGHT = context.getResources().getDisplayMetrics().heightPixels;
            WIDTH = SCREEN_WIDTH;
        }

        DIALOG_WIDTH = SCREEN_WIDTH * 90 / 100;
        DIALOG_HEIGHT = SCREEN_HEIGHT * 90 / 100;

        SCREEN_RATIO = (float) SCREEN_WIDTH / (float) SCREEN_HEIGHT;
        IS_TABLET = context.getResources().getBoolean(R.bool.isTablet);
    }

    static void GET_IMAGE_VIEW_DIMENSIONS(Context context) {
        if (IMAGEVIEW_DIMENSION_SMALL_LIST == 0) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            //ONE_SP=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,1,displayMetrics);
            ONE_DP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics);

            TWO_DP = (int) (ONE_DP * 2);
            FOUR_DP = (int) (ONE_DP * 4);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,displayMetrics);
            FIVE_DP = (int) (ONE_DP * 5);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,5,displayMetrics);
            SIX_DP = (int) (ONE_DP * 6);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,displayMetrics);
            EIGHT_DP = (int) (ONE_DP * 8);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8,displayMetrics);
            TEN_DP = (int) (ONE_DP * 10);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10,displayMetrics);
            TWELVE_DP = (int) (ONE_DP * 12);//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,12,displayMetrics);
            FOURTEEN_DP = (int) (ONE_DP * 14);//TEN_DP+FOUR_DP;
            THIRTY_SIX_DP = (int) (ONE_DP * 36);


            int list_s = FIVE_DP;
            int list_g = IS_TABLET ? TWELVE_DP : TEN_DP;


            SELECTOR_ICON_DIMENSION = TEN_DP + TEN_DP + SIX_DP;
            RECYCLERVIEW_ITEM_SPACING = IS_TABLET ? TEN_DP : FIVE_DP;
            LIST_POPUP_WINDOW_DROP_DOWN_OFFSET = IS_TABLET ? TEN_DP : SIX_DP;


            IMAGEVIEW_DIMENSION_SMALL_LIST = list_s * IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL + FOUR_DP;
            IMAGEVIEW_DIMENSION_MEDIUM_LIST = list_s * IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM + FOUR_DP;
            IMAGEVIEW_DIMENSION_LARGE_LIST = list_s * IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE + FOUR_DP;

            IMAGEVIEW_DIMENSION_SMALL_GRID = list_g * IMAGE_VIEW_DIMENSION_MULTIPLIER_SMALL;
            IMAGEVIEW_DIMENSION_MEDIUM_GRID = list_g * IMAGE_VIEW_DIMENSION_MULTIPLIER_MEDIUM;
            IMAGEVIEW_DIMENSION_LARGE_GRID = list_g * IMAGE_VIEW_DIMENSION_MULTIPLIER_LARGE;

            BUTTON_HEIGHT = (int) (ONE_DP * 48);

            DIVIDERITEMDECORATION = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
        }
        GRID_COUNT_SMALL = WIDTH / (IMAGEVIEW_DIMENSION_SMALL_GRID + RECYCLERVIEW_ITEM_SPACING);
        GRID_COUNT_MEDIUM = WIDTH / (IMAGEVIEW_DIMENSION_MEDIUM_GRID + RECYCLERVIEW_ITEM_SPACING);
        GRID_COUNT_LARGE = WIDTH / (IMAGEVIEW_DIMENSION_LARGE_GRID + RECYCLERVIEW_ITEM_SPACING);


        //cache directory setting
        ARCHIVE_EXTRACT_DIR = new File(context.getFilesDir(), "Archive");
        USB_CACHE_DIR = context.getExternalFilesDir(".usb_cache");
        TEMP_ROTATE_CACHE_DIR = context.getExternalFilesDir(".temp_rotate_cache");
        FTP_CACHE_DIR = context.getExternalFilesDir(".ftp_cache");
        SFTP_CACHE_DIR = context.getExternalFilesDir(".sftp_cache");
        WEBDAV_CACHE_DIR = context.getExternalFilesDir(".webdav_cache");
        SMB_CACHE_DIR = context.getExternalFilesDir(".smb_cache");
        ROOT_CACHE_DIR = context.getExternalFilesDir(".root_cache");
        APK_ICON_DIR = context.getExternalFilesDir(".apk_icons");
        APK_ICON_PACKAGE_NAME_LIST.addAll(Arrays.asList(APK_ICON_DIR.list()));
        SIZE_APK_ICON_LIST = APK_ICON_PACKAGE_NAME_LIST.size();
        ARCHIVE_CACHE_DIR_LENGTH = ARCHIVE_EXTRACT_DIR.getAbsolutePath().length();
    }

    static void GET_PREFERENCES(TinyDB tinyDB) {
//
        if (SORT == null) {
            SORT = tinyDB.getString("sort");
            if (SORT.trim().isEmpty() || !SORT_CODE_SET.contains(SORT)) {
                SORT = "d_name_asc";
            }
        }
//

        if (FileSelectorActivity.SORT == null) {
            FileSelectorActivity.SORT = tinyDB.getString("file_selector_sort");
            if (FileSelectorActivity.SORT.trim().isEmpty() || !SORT_CODE_SET.contains(FileSelectorActivity.SORT)) {
                FileSelectorActivity.SORT = "d_name_asc";
            }
        }
        //

        if (STORAGE_ANALYSER_SORT == null) {
            STORAGE_ANALYSER_SORT = tinyDB.getString("storage_analyser_sort");
            if (STORAGE_ANALYSER_SORT.trim().isEmpty() || !SORT_CODE_SET.contains(STORAGE_ANALYSER_SORT)) {
                STORAGE_ANALYSER_SORT = "d_size_desc";
            }
        }

//

        if (APP_MANAGER_SORT == null) {
            APP_MANAGER_SORT = tinyDB.getString("app_manager_sort");
            if (APP_MANAGER_SORT.trim().isEmpty() || !SORT_CODE_SET.contains(APP_MANAGER_SORT)) {
                APP_MANAGER_SORT = "d_name_asc";
            }
        }

        //
        if (THEME == null) {
            THEME = tinyDB.getString("theme");
            if (THEME.trim().isEmpty() || !THEME_CODE_SET.contains(THEME)) {
                THEME = "system";
            }
        }

        //
        BYTE_COUNT_BLOCK_1000 = tinyDB.getBoolean("byte_count_block_1000");

        //
        FILE_GRID_LAYOUT = tinyDB.getBoolean("file_grid_layout");

        //
        IMAGE_VIDEO_GRID_LAYOUT = tinyDB.getBoolean("image_video_grid_layout");

        //
        FileSelectorActivity.FILE_GRID_LAYOUT = tinyDB.getBoolean("file_selector_file_grid_layout");
        //

        AppManagerActivity.FILE_GRID_LAYOUT = tinyDB.getBoolean("app_manager_file_grid_layout");
        //
        DETAILED_SEARCH_LIBRARY = tinyDB.getBoolean("detailed_search_library");
        //

        SHOW_FILE_PATH = tinyDB.getBoolean("show_file_path");

        //
        RECOGNISE_USB = tinyDB.getBoolean("recognise_usb");
        //
        RECYCLER_VIEW_FONT_SIZE_FACTOR = tinyDB.getInt("recycler_view_font_size_factor");
        FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR = tinyDB.getInt("file_selector_recycler_view_font_size_factor");
        if (!tinyDB.getBoolean("not_first_run")) {
            RECYCLER_VIEW_FONT_SIZE_FACTOR = 1;
            FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR = 1;
            IMAGE_VIDEO_GRID_LAYOUT = true;
            tinyDB.putBoolean("not_first_run", true);
            tinyDB.putInt("recycler_view_font_size_factor", RECYCLER_VIEW_FONT_SIZE_FACTOR);
            tinyDB.putInt("file_selector_recycler_view_font_size_factor", FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR);
            tinyDB.putBoolean("image_video_grid_layout", IMAGE_VIDEO_GRID_LAYOUT);
        }
        //

        if (RECYCLER_VIEW_FONT_SIZE_FACTOR != 0 && RECYCLER_VIEW_FONT_SIZE_FACTOR != 1 && RECYCLER_VIEW_FONT_SIZE_FACTOR != 2) {
            RECYCLER_VIEW_FONT_SIZE_FACTOR = 1;
        }

        switch (RECYCLER_VIEW_FONT_SIZE_FACTOR) {
            case 0:
                GRID_COUNT = GRID_COUNT_SMALL;
                break;
            case 2:
                GRID_COUNT = GRID_COUNT_LARGE;
                break;
            default:
                GRID_COUNT = GRID_COUNT_MEDIUM;
                break;
        }
//

        if (FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR != 0 && FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR != 1 && FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR != 2) {
            RECYCLER_VIEW_FONT_SIZE_FACTOR = 1;
        }

        switch (FileSelectorActivity.RECYCLER_VIEW_FONT_SIZE_FACTOR) {
            case 0:
                FileSelectorActivity.GRID_COUNT = GRID_COUNT_SMALL;
                break;
            case 2:
                FileSelectorActivity.GRID_COUNT = GRID_COUNT_LARGE;
                break;
            default:
                FileSelectorActivity.GRID_COUNT = GRID_COUNT_MEDIUM;
                break;
        }

        DETERMINE_TO_CLEAR_CACHE_TODAY(tinyDB);
    }

    static void GET_ACTION_BAR_HEIGHT(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            ACTION_BAR_HEIGHT = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            float density = context.getResources().getDisplayMetrics().density;
            ACTION_BAR_HEIGHT_IN_DP = (int) (ACTION_BAR_HEIGHT / density);
        }
    }

    public static void LOCAL_BROADCAST(String action, LocalBroadcastManager localBroadcastManager, Bundle bundle) {
        Intent intent = new Intent();
        intent.setAction(action);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        localBroadcastManager.sendBroadcast(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static DirectoryStream.Filter<Path> GET_NIO_FILE_NAME_FILTER() {
        return new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path path) throws IOException {
                return !Files.isHidden(path);
            }
        };
    }

    static FilePOJO GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR() {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (repositoryClass.storage_dir.get(0).getPath().equals("/")) {
            return repositoryClass.storage_dir.get(1);
        } else {
            return repositoryClass.storage_dir.get(0);
        }
    }

    static void GET_STORAGE_DIR(Context context) {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (repositoryClass.storage_dir.isEmpty()) {
            repositoryClass.storage_dir = new ArrayList<>(StorageUtil.getSdCardPaths(context, true));
            INTERNAL_PRIMARY_STORAGE_PATH = GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR().getPath();
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


    public static String GET_FILE_PERMISSION(String file_path) {
        String permission_string = null;
        return permission_string;
    }

    public static String GET_OTHER_FILE_PERMISSION(String file_path) {
        String permission_string = GET_FILE_PERMISSION(file_path);
        if (permission_string == null) {
            return null;
        }
        String other_permission_string = permission_string.replaceAll(".*?(.?.?.?)?$", "$1");
        return other_permission_string.replaceAll("-", "");
    }

    public static boolean SET_OTHER_FILE_PERMISSION(String permission, String file_path) {
        boolean success = false;
        return success;
    }


    static void WORKOUT_AVAILABLE_SPACE() {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        for (FilePOJO filePOJO : repositoryClass.storage_dir) {
            if (filePOJO == null) {
                continue;
            }
            long totalspace = 0, availabelspace = 0;
            FileObjectType fileObjectType = filePOJO.getFileObjectType();
            if (fileObjectType == FileObjectType.FILE_TYPE) {
                totalspace = new File(filePOJO.getPath()).getTotalSpace();
                availabelspace = new File(filePOJO.getPath()).getUsableSpace();

                SPACE_ARRAY.put(fileObjectType + filePOJO.getPath(), new SpacePOJO(filePOJO.getPath(), totalspace, availabelspace));
            } else if (fileObjectType == FileObjectType.USB_TYPE) {
                if (MainActivity.usbFileRoot == null) {
                    return;
                }
                String name = MainActivity.usbFileRoot.getName();
                totalspace = MainActivity.usbCurrentFs.getCapacity();
                availabelspace = MainActivity.usbCurrentFs.getOccupiedSpace();
                SPACE_ARRAY.put(fileObjectType + name, new SpacePOJO(name, totalspace, availabelspace));
            } else if (fileObjectType == FileObjectType.ROOT_TYPE) {
                SPACE_ARRAY.put(fileObjectType + filePOJO.getPath(), new SpacePOJO(filePOJO.getPath(), totalspace, availabelspace));
            }
        }
    }

    public static boolean CHECK_APPS_FOR_RECOGNISED_FILE_EXT(Context context, String file_extn) {

        String mime_type = "";
        Uri uri;
        File f = new File("/dummy");
        uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", f);
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        mime_type = FileIntentDispatch.SET_INTENT_FOR_VIEW(intent, mime_type, "", file_extn, null, false, uri);
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(intent, 0);
        return !resolveInfoList.isEmpty();
    }

    public static void REMOVE_RECURSIVE_PATHS(List<String> files_selected_array, FileObjectType sourceFileObjectType, String dest_folder, FileObjectType destFileObjectType) {
        if (NETWORK_FILE_OBJECT_TYPES.contains(sourceFileObjectType) && NETWORK_FILE_OBJECT_TYPES.contains(destFileObjectType)) {
            if (!sourceFileObjectType.equals(destFileObjectType)) {
                return;
            }
        }

        if (sourceFileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE)) {
            if (FileObjectType.FILE_TYPE.equals(destFileObjectType)) {
                Iterator<String> iterator = files_selected_array.iterator();
                while (iterator.hasNext()) {
                    if (IS_CHILD_FILE(dest_folder, iterator.next())) {
                        iterator.remove();
                    }
                }
            }
        } else {
            if (sourceFileObjectType.equals(destFileObjectType)) {
                Iterator<String> iterator = files_selected_array.iterator();
                while (iterator.hasNext()) {
                    if (IS_CHILD_FILE(dest_folder, iterator.next())) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private static String normalizePath(String path, FileObjectType fileObjectType) {
        // Remove protocol prefix if present
        String normalizedPath = path.replaceFirst("^[a-zA-Z]+://", "");

        // Remove hostname and share name for SMB
        if (fileObjectType == FileObjectType.SMB_TYPE) {
            // SMB path format: smb://hostname/share/path
            // After removing protocol, format is hostname/share/path
            int firstSlashIndex = normalizedPath.indexOf('/');
            if (firstSlashIndex != -1) {
                // Remove hostname
                normalizedPath = normalizedPath.substring(firstSlashIndex + 1);
                int secondSlashIndex = normalizedPath.indexOf('/');
                if (secondSlashIndex != -1) {
                    // Remove share name
                    normalizedPath = normalizedPath.substring(secondSlashIndex);
                } else {
                    normalizedPath = "/";
                }
            } else {
                normalizedPath = "/";
            }
        } else {
            // For FTP, SFTP, WebDAV: Remove hostname if present
            int firstSlashIndex = normalizedPath.indexOf('/');
            if (firstSlashIndex != -1) {
                normalizedPath = normalizedPath.substring(firstSlashIndex);
            } else {
                normalizedPath = "/";
            }
        }

        // Replace backslashes with forward slashes
        normalizedPath = normalizedPath.replace("\\", "/");

        // Remove multiple consecutive slashes
        normalizedPath = normalizedPath.replaceAll("/+", "/");

        // Remove trailing slash unless it's the root "/"
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        // Resolve "." and ".." in the path
        normalizedPath = normalizeDotsInPath(normalizedPath);
        return normalizedPath;
    }

    private static String normalizeDotsInPath(String path) {
        String[] parts = path.split("/");
        Stack<String> pathStack = new Stack<>();
        for (String part : parts) {
            if (part.equals("..")) {
                if (!pathStack.isEmpty()) {
                    pathStack.pop();
                }
            } else if (!part.equals(".") && !part.isEmpty()) {
                pathStack.push(part);
            }
        }
        StringBuilder normalizedPath = new StringBuilder();
        for (String part : pathStack) {
            normalizedPath.append("/").append(part);
        }
        if (normalizedPath.length() == 0) {
            normalizedPath.append("/");
        }
        return normalizedPath.toString();
    }

    private static boolean isSameFileSystem(NetworkAccountsDetailsDialog.NetworkAccountPOJO source,
                                            FileObjectType sourceFileObjectType,
                                            NetworkAccountsDetailsDialog.NetworkAccountPOJO dest,
                                            FileObjectType destFileObjectType) {
        if (source == null || dest == null) {
            // Cannot proceed without both accounts; but to avoid unnecessary copying, assume same filesystem
            return true;
        }

        // Extract hostnames
        String host1 = extractHost(source.host);
        String host2 = extractHost(dest.host);

        if (host1 != null && host2 != null) {
            // Resolve hostnames to IP addresses
            try {
                InetAddress[] addresses1 = InetAddress.getAllByName(host1);
                InetAddress[] addresses2 = InetAddress.getAllByName(host2);
                boolean sameHost = false;
                for (InetAddress addr1 : addresses1) {
                    for (InetAddress addr2 : addresses2) {
                        if (addr1.equals(addr2)) {
                            sameHost = true;
                            break;
                        }
                    }
                    if (sameHost) {
                        break;
                    }
                }
                if (!sameHost) {
                    // Hosts are different; file systems are different
                    return false; // Only return false when certain
                }
            } catch (UnknownHostException e) {
                // Cannot resolve hostnames; assume they might be the same to avoid unnecessary copying
                // Do not return false here
            }
        } else {
            // Host information is incomplete; assume hosts might be the same
            // Do not return false here
        }

        // At this point, hosts are the same or unresolved

        // Optionally compare usernames
        if (source.user_name != null && dest.user_name != null && !source.user_name.equals(dest.user_name)) {
            // Different usernames may point to different areas; but to avoid unnecessary copying, proceed
        }

        // Proceed with cautious protocol-specific comparisons

        // SMB-specific comparisons
        if (sourceFileObjectType == FileObjectType.SMB_TYPE && destFileObjectType == FileObjectType.SMB_TYPE) {
            if (source.shareName != null && dest.shareName != null && !source.shareName.equals(dest.shareName)) {
                // Different share names might still access the same file system
                // Proceed cautiously and do not return false
            }
            // Additional SMB-specific checks can be added here if necessary
        }

        // WebDAV-specific comparisons
        if (sourceFileObjectType == FileObjectType.WEBDAV_TYPE && destFileObjectType == FileObjectType.WEBDAV_TYPE) {
            if (source.basePath != null && dest.basePath != null) {
                String normalizedSourceBasePath = normalizePath(source.basePath);
                String normalizedDestBasePath = normalizePath(dest.basePath);
                // Instead of returning false, check if one base path is a subpath of the other
                // or proceed cautiously without returning false
            }
            if (source.useHTTPS != dest.useHTTPS) {
                // Different protocols may still access the same file system
                // Do not return false solely based on protocol differences
            }
            // Additional WebDAV-specific checks can be added here if necessary
        }

        // Other protocol-specific comparisons can be added here if applicable

        // Since we have not found definitive evidence that the file systems are different,
        // we return true to avoid unnecessary copying
        return true;
    }


    private static String extractHost(String host) {
        if (host == null) {
            return null;
        }
        host = host.trim();

        // Check if the host string contains a scheme
        if (host.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            try {
                URI uri = new URI(host);
                String extractedHost = uri.getHost();
                if (extractedHost != null) {
                    return extractedHost;
                }
                // If getHost() is null, attempt to extract the authority
                String authority = uri.getAuthority();
                if (authority != null) {
                    // Remove any user info (e.g., user:pass@)
                    int atIndex = authority.lastIndexOf('@');
                    if (atIndex != -1) {
                        authority = authority.substring(atIndex + 1);
                    }
                    // Remove port if present
                    int colonIndex = authority.indexOf(':');
                    if (colonIndex != -1) {
                        authority = authority.substring(0, colonIndex);
                    }
                    return authority;
                }
            } catch (URISyntaxException e) {
                // If URI parsing fails, proceed to manual extraction
            }
        }

        // If there's no scheme or parsing failed, manually extract the host

        // Remove any leading protocol prefixes manually
        String hostWithoutProtocol = host.replaceFirst("^(ftp|sftp|http|https|smb)://", "");

        // Remove user info if present (e.g., user:pass@)
        int atIndex = hostWithoutProtocol.lastIndexOf('@');
        if (atIndex != -1) {
            hostWithoutProtocol = hostWithoutProtocol.substring(atIndex + 1);
        }

        // Remove any path starting with '/'
        int slashIndex = hostWithoutProtocol.indexOf('/');
        if (slashIndex != -1) {
            hostWithoutProtocol = hostWithoutProtocol.substring(0, slashIndex);
        }

        // Remove port if present (after ':')
        int colonIndex = hostWithoutProtocol.indexOf(':');
        if (colonIndex != -1) {
            hostWithoutProtocol = hostWithoutProtocol.substring(0, colonIndex);
        }

        // The remaining string should be the host
        return hostWithoutProtocol;
    }


    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        // Replace backslashes with forward slashes
        path = path.replace('\\', '/');
        // Remove redundant slashes
        path = path.replaceAll("/+", "/");
        // Remove trailing slash
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static boolean IS_CHILD_FILE(String child_path, String parent_path) {
        if (parent_path.equals(File.separator)) {
            return child_path.startsWith(parent_path);
        } else {
            return (child_path + File.separator).startsWith(parent_path + File.separator);
        }
    }

    public static boolean CHECK_WHETHER_STORAGE_DIR_CONTAINS_FILE_OBJECT(FileObjectType fileObjectType) {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getFileObjectType() == fileObjectType) {
                return true;
            }
        }
        return false;
    }

    public static boolean WHETHER_FILE_OBJECT_TYPE_NETWORK_TYPE_AND_CONTAINED_IN_STORAGE_DIR(FileObjectType fileObjectType) {
        if (NETWORK_FILE_OBJECT_TYPES.contains(fileObjectType)) {
            RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
            Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getFileObjectType() == fileObjectType) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean WHETHER_FILE_ALREADY_EXISTS(FileObjectType fileObjectType, String file_path, List<FilePOJO> destFilePOJOs) {
        if (fileObjectType == FileObjectType.FILE_TYPE || fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
            File new_file = new File(file_path);
            return new_file.exists();
        } else {
            File f = new File(file_path);
            String file_name = f.getName();
            if (destFilePOJOs != null) {
                for (FilePOJO filePOJO : destFilePOJOs) {
                    if (filePOJO.getName().equals(file_name)) {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        return false;
    }

    public static String GET_TRUNCATED_FILE_PATH_USB(String file_path) {
        if (file_path.equals(File.separator)) {
            return file_path;
        }
        if (file_path.startsWith(File.separator)) {
            return file_path.substring(1);
        } else {
            return file_path;
        }
    }

    public static FileObjectType GET_FILE_OBJECT_TYPE(String file_object_type) {
        if (file_object_type == null) {
            return null;
        }
        switch (file_object_type) {
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
            case "SFTP_TYPE":
                return FileObjectType.SFTP_TYPE;
            case "WEBDAV_TYPE":
                return FileObjectType.WEBDAV_TYPE;
            case "SMB_TYPE":
                return FileObjectType.SMB_TYPE;
            default:
                return null;
        }
    }

    public static String GET_FileObjectType(FileObjectType fileObjectType) {
        switch (fileObjectType) {
            case ROOT_TYPE:
                return "(Root)";
            case FILE_TYPE:
                return "(Device)";
            case USB_TYPE:
                return "(USB)";
            case FTP_TYPE:
                return "(FTP)";
            case SFTP_TYPE:
                return "(SFTP)";
            case WEBDAV_TYPE:
                return "(WebDAV)";
            case SMB_TYPE:
                return "(SMB)";
            default:
                return "";
        }
    }


    public static long AVAILABLE_MEMORY_MB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        long maxHeapSizeInMB = runtime.maxMemory() / 1048576L;
        return maxHeapSizeInMB - usedMemInMB;
    }

    public static long GET_URI_FILE_SIZE(Uri fileUri, Context context) {
        Cursor returnCursor = context.getContentResolver().
                query(fileUri, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        long size = returnCursor.getLong(sizeIndex);
        returnCursor.close();

        return size;
    }

    public static void print(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void print_background_thread(Context context, String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);
            }
        });
    }


    public static String CONCATENATE_PARENT_CHILD_PATH(String parent_file_path, String child_file_name) {
        if (parent_file_path == null) {
            parent_file_path = "";
        }
        return parent_file_path.endsWith(File.separator) ? parent_file_path + child_file_name : parent_file_path + File.separator + child_file_name;
    }

    public static void CLEAR_CACHE() {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<Map.Entry<String, List<FilePOJO>>> iterator = repositoryClass.hashmap_file_pojo.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<FilePOJO>> entry = iterator.next();
            if (!entry.getKey().startsWith(FileObjectType.SEARCH_LIBRARY_TYPE.toString())) {
                iterator.remove();
            }
        }
    }

    public static void WARN_NOTIFICATIONS_DISABLED(Context context, String channelId, boolean[] alreadyWarned) {
        if (alreadyWarned[0]) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    print(context, context.getString(R.string.notification_not_enabled_grant_permission_in_device_settings));
                    alreadyWarned[0] = true;
                    return;
                }
            } else {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (!manager.areNotificationsEnabled()) {
                    print(context, context.getString(R.string.notification_not_enabled_grant_permission_in_device_settings));
                    alreadyWarned[0] = true;
                    return;
                }
            }

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            List<NotificationChannel> channels = null;
            channels = manager.getNotificationChannels();
            for (NotificationChannel channel : channels) {
                if (channel.getId().equals(channelId) && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    String message = context.getString(R.string.enable_specified_notification_channel_for_better_control);
                    switch (channelId) {
                        case NotifManager.CHANNEL_ID:
                            print(context, NotifManager.CHANNEL_NAME + " - " + message);
                            break;
                        case AudioPlayerService.CHANNEL_ID:
                            print(context, AudioPlayerService.CHANNEL_NAME + " - " + message);
                            break;
                        case FsNotification.CHANNEL_ID:
                            print(context, FsNotification.CHANNEL_NAME + " - " + message);
                            break;
                    }
                    alreadyWarned[0] = true;
                    break;
                }
            }
        } else {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                print(context, context.getString(R.string.notification_not_enabled_grant_permission_in_device_settings));
                alreadyWarned[0] = true;
            }
        }
    }

    public static Uri GET_ALBUM_ART_URI(String album_id) {
        long id;
        try {
            id = Long.parseLong(album_id);
        } catch (NumberFormatException e) {
            id = 0;
        }
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id);
    }

    public static void DELETE_DIRECTORY_ASYNCHRONOUSLY(File dir) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FileUtil.deleteNativeDirectory(dir);
            }
        });
    }

    public static void DETERMINE_TO_CLEAR_CACHE_TODAY(TinyDB tinyDB) {
        int cache_cleared_month = tinyDB.getInt("cache_cleared_month");
        CURRENT_MONTH = Calendar.getInstance().get(Calendar.MONTH);
        if (cache_cleared_month < CURRENT_MONTH || (cache_cleared_month == 11 && CURRENT_MONTH != 11)) {
            WHETHER_TO_CLEAR_CACHE_TODAY = true;
        }
    }

    public static boolean createNativeNewFile(@NonNull final File file) {
        if (file.exists()) {
            return false;
        }
        try {
            if (file.createNewFile()) {
                return true;
            }
        } catch (IOException e) {

        }
        return false;
    }

    public static File COPY_TO_CACHE(String file_path, FileObjectType fileObjectType) {
        File cache_file = null;
        switch (fileObjectType) {
            case USB_TYPE:
                cache_file = new File(USB_CACHE_DIR, file_path);
                break;
            case FTP_TYPE:
                cache_file = new File(FTP_CACHE_DIR, file_path);
                break;
            case SFTP_TYPE:
                cache_file = new File(SFTP_CACHE_DIR, file_path);
                break;
            case WEBDAV_TYPE:
                cache_file = new File(WEBDAV_CACHE_DIR, file_path);
                break;
            case SMB_TYPE:
                cache_file = new File(SMB_CACHE_DIR, file_path);
                break;
            case ROOT_TYPE:
                cache_file = new File(ROOT_CACHE_DIR, file_path);
                break;
        }

        long[] bytes_read = new long[1];
        File parent_file = cache_file.getParentFile();
        if (parent_file != null) {
            FileUtil.mkdirsNative(parent_file);
            FileModel source_file_model = FileModelFactory.getFileModel(file_path, fileObjectType, null, null);
            FileModel dest_file_model = FileModelFactory.getFileModel(parent_file.getPath(), FileObjectType.FILE_TYPE, null, null);
            FileUtil.copy_FileModel_FileModel(source_file_model, dest_file_model, cache_file.getName(), false, bytes_read);
        }
        return cache_file;
    }

    public static boolean whether_file_cached(FileObjectType fileObjectType) {
        return fileObjectType == FileObjectType.ROOT_TYPE || fileObjectType == FileObjectType.USB_TYPE || fileObjectType == FileObjectType.FTP_TYPE
                || fileObjectType == FileObjectType.SFTP_TYPE || fileObjectType == FileObjectType.WEBDAV_TYPE || fileObjectType == FileObjectType.SMB_TYPE;
    }

    public static Bitmap scaleToFitHeight(Bitmap bitmap, int height) {
        float factor = height / (float) bitmap.getHeight();
        return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * factor), height, true);
    }

    public static String getParentPath(String path) {
        String[] segments = path.split("/");
        StringBuilder parentPathBuilder = new StringBuilder();

        // Iterate through all segments except the last one
        for (int i = 0; i < segments.length - 1; i++) {
            if (!segments[i].isEmpty()) {  // Skip empty segments
                parentPathBuilder.append("/").append(segments[i]);
            }
        }

        // If the path starts with a slash, make sure we keep it
        String parentPath = parentPathBuilder.toString();
        return path.startsWith("/") && !parentPath.startsWith("/") ? "/" + parentPath : parentPath;
    }

    public static void TOGGLE_VIEW(Context context, View view, boolean show) {
        // Load animations from XML
        Animation slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_up_from_below_with_reverse_fade);
        Animation slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_down_from_normal_with_fade);

        Animation animation = show ? slideDown : slideUp;

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (show) {
                    view.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!show) {
                    view.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        view.startAnimation(animation);
    }
}
