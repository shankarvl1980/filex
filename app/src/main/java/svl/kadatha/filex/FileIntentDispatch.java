package svl.kadatha.filex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class FileIntentDispatch {
    static final String EXTRA_FILE_OBJECT_TYPE = "fileObjectType";
    static final String EXTRA_FILE_PATH = "file_path";
    static final String EXTRA_FROM_ARCHIVE = "fromArchive";

    public static void openFile(Context context, String file_path, String mime_type, FileObjectType fileObjectType, boolean select_app, long file_size, boolean fromArchive) {
        String file_extn = "";
        Uri uri;
        int file_extn_idx = file_path.lastIndexOf(".");
        if (file_extn_idx != -1) {
            file_extn = file_path.substring(file_extn_idx + 1);
        }

        File file = new File(file_path);
        uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", file);
        dispatch_intent(context, uri, file_path, file_extn, mime_type, fileObjectType, select_app, file_size, fromArchive);
    }

    public static void openUri(Context context, String file_path, String mime_type, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path, boolean select_app, long file_size, boolean fromArchive) {
        String file_extn = "";
        int file_extn_idx = file_path.lastIndexOf(".");
        if (file_extn_idx != -1) {
            file_extn = file_path.substring(file_extn_idx + 1);
        }
        Uri uri = FileUtil.getDocumentUri(file_path, tree_uri, tree_uri_path);
        dispatch_intent(context, uri, file_path, file_extn, mime_type, fileObjectType, select_app, file_size, fromArchive);
    }

    public static void sendFile(Context context, ArrayList<File> file_list) {
        ArrayList<Uri> uri_list = new ArrayList<>();
        for (File f : file_list) {
            uri_list.add(FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", f));
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uri_list);
        intent.putExtra(Intent.EXTRA_SUBJECT, file_list.get(0).getName());
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chooser = Intent.createChooser(intent, "Select app");
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        }
    }

    public static void sendUri(Context context, ArrayList<Uri> uri_list) {
        String extra = new File(uri_list.get(0).getPath()).getName();
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uri_list);
        intent.putExtra(Intent.EXTRA_SUBJECT, extra);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chooser = Intent.createChooser(intent, "Select app");
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        }
    }

    private static void dispatch_intent(final Context context, Uri uri, String file_path, String file_extn, String mime_type, FileObjectType fileObjectType, boolean select_app, long file_size, boolean fromArchive) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        mime_type = SET_INTENT_FOR_VIEW(intent, mime_type, file_path, file_extn, fileObjectType, fromArchive, uri);
        if (mime_type == null || mime_type.isEmpty()) return;

        DefaultAppDatabaseHelper defaultAppDatabaseHelper = new DefaultAppDatabaseHelper(context);
        final String package_name = defaultAppDatabaseHelper.getPackageName(mime_type);
        final String app_component_name = defaultAppDatabaseHelper.getComponentName(mime_type);
        if (package_name == null || select_app) {
            launch_app_selector_dialog(context, uri, file_path, mime_type, fromArchive, fileObjectType, file_size);
        } else {
            final List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(intent, 0);
            final int size = resolveInfoList.size();
            boolean package_found = false;

            for (int i = 0; i < size; ++i) {
                final ResolveInfo resolveInfo = resolveInfoList.get(i);
                final String resolved_app_package_name = resolveInfo.activityInfo.packageName;
                if (resolved_app_package_name.equals(package_name)) {
                    if (mime_type.equals("application/vnd.android.package-archive")) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("data", uri);
                        bundle.putString("app_package_name", package_name);
                        bundle.putString("app_component_name", app_component_name);
                        bundle.putString("file_path", file_path);
                        bundle.putString("mime_type", mime_type);
                        bundle.putBoolean("fromArchive", fromArchive);
                        bundle.putSerializable("fileObjectType", fileObjectType);
                        AppInstallAlertDialog appInstallAlertDialog = AppInstallAlertDialog.getInstance(bundle);
                        appInstallAlertDialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "");
                    } else {
                        if (Global.FILEX_PACKAGE.equals(package_name)) {
                            AppCompatActivity appCompatActivity = (AppCompatActivity) context;
                            if (appCompatActivity instanceof MainActivity) {
                                ((MainActivity) context).clear_cache = false;
                            } else if (appCompatActivity instanceof StorageAnalyserActivity) {
                                ((StorageAnalyserActivity) context).clear_cache = false;
                            } else if (appCompatActivity instanceof ArchiveViewActivity) {
                                ((ArchiveViewActivity) context).clear_cache = false;
                            } else if (appCompatActivity instanceof AppManagerActivity) {
                                ((AppManagerActivity) appCompatActivity).clear_cache = false;
                            } else if (appCompatActivity instanceof InstaCropperActivity) {
                                ((InstaCropperActivity) context).clear_cache = false;
                            }
                        }

                        intent.setComponent(new ComponentName(package_name, app_component_name));
                        try {
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Global.print(context, context.getString(R.string.exception_thrown));
                        }
                    }
                    package_found = true;
                    break;
                }
            }

            if (!package_found) {
                defaultAppDatabaseHelper.delete_row(mime_type);
                launch_app_selector_dialog(context, uri, file_path, mime_type, fromArchive, fileObjectType, file_size);
            }
        }
        defaultAppDatabaseHelper.close();
    }

    private static void launch_app_selector_dialog(Context context, Uri uri, String file_path, String mime_type, boolean fromArchive, FileObjectType fileObjectType, long file_size) {
        AppSelectorDialog appSelectorDialog = AppSelectorDialog.getInstance(uri, file_path, mime_type, fromArchive, fileObjectType, file_size);
        appSelectorDialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "");
    }

    public static String SET_INTENT_FOR_VIEW(Intent intent, String mime_type, String file_path, String file_extn, FileObjectType fileObjectType,
                                             boolean fromArchive, Uri uri) {
        if (mime_type == null || mime_type.isEmpty()) {
            for (MimePOJO mimePOJO : Global.MIME_POJOS) {
                if (file_extn.matches(mimePOJO.getRegex())) {
                    mime_type = mimePOJO.getMime_type();
                    break;
                }
            }
        }
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime_type);
        intent.putExtra(EXTRA_FILE_OBJECT_TYPE, fileObjectType != null ? fileObjectType.toString() : null);
        intent.putExtra(EXTRA_FILE_PATH, file_path);
        intent.putExtra(EXTRA_FROM_ARCHIVE, fromArchive);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return mime_type;
    }
}

