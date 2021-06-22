package svl.kadatha.filex;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

/**
 * Created by Aki on 1/7/2017.
 */

public class PathUtil {
    /*
     * Gets the file path of the given Uri.
     */
    @SuppressLint("NewApi")
    public static String getPath(Context context, Uri uri) {
        final boolean needToCheckUri = true;
        final boolean isOreoOrMore = Build.VERSION.SDK_INT >= 26;
        String selection = null;
        String[] selectionArgs = null;
        uri.normalizeScheme();
        String path = uri.getPath();
        path = path.replaceFirst("^(/\\.)", "");

        return path;

        //if(isOreoOrMore)

/*
        File file = new File(path);//create path from uri
        final String[] split = file.getPath().split(":");//split the path.
        if (split.length > 1) {
            return split[1];
        } else {
            return split[0];
        }
        */

    }


}
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        /*
        if (needToCheckUri && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri))
		{
            if (isExternalStorageDocument(uri))
			{
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
			else if (isDownloadsDocument(uri))
			{
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
					Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            }
			else if (isMediaDocument(uri))
			{
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type))
				{
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
				else if ("video".equals(type))
				{
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
				else if ("audio".equals(type))
				{
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{ split[1] };
            }
        }


        if ("content".equalsIgnoreCase(uri.getScheme()))
		{
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = null;
            try
			{
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst())
				{
                    return cursor.getString(column_index);
                }
            }
			catch (Exception e)
			{
            }
        }
		else if ("file".equalsIgnoreCase(uri.getScheme()))
		{
            return uri.getPath();
        }
        return null;
    }

*/
    /*
    public static boolean isExternalStorageDocument(Uri uri)
	{
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
*/
    /*
    public static boolean isDownloadsDocument(Uri uri)
	{
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
*/
    /*
    public static boolean isMediaDocument(Uri uri)
	{
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

     */

