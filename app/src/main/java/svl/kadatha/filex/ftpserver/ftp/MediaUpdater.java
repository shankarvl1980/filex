package svl.kadatha.filex.ftpserver.ftp;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.util.Timer;

import svl.kadatha.filex.App;
import timber.log.Timber;

/**
 * This media scanner runs in the background. The rescan might
 * not happen immediately.
 */
public enum MediaUpdater {
    INSTANCE;

    private final static String TAG = MediaUpdater.class.getSimpleName();

    // the system broadcast to remount the media is only done after a little while (5s)
    private static final Timer sTimer = new Timer();

    public static void notifyFileCreated(String path) {
        Context context = App.getAppContext();
        MediaScannerConnection.scanFile(context, new String[]{path}, null,
                new ScanCompletedListener());
    }

    public static void notifyFileDeleted(String path) {
        Timber.tag(TAG).d("Notifying others about deleted file: " + path);
        // on newer devices, we hope that this works correctly:
        Context context = App.getAppContext();
        MediaScannerConnection.scanFile(context, new String[]{path}, null,
                new ScanCompletedListener());
    }

    private static class ScanCompletedListener implements
            MediaScannerConnection.OnScanCompletedListener {
        @Override
        public void onScanCompleted(String path, Uri uri) {
            Timber.tag(TAG).i("Scan completed: " + path + " : " + uri);
        }
    }
}
