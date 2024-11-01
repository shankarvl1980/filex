package svl.kadatha.filex.ftpserver.ftp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import svl.kadatha.filex.App;
import svl.kadatha.filex.FsNotification;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.R;
import svl.kadatha.filex.ftpserver.server.SessionThread;
import svl.kadatha.filex.ftpserver.server.TcpListener;
import timber.log.Timber;

public class FsService extends Service implements Runnable {
    // Service will check following actions when started through intent
    static public final String ACTION_REQUEST_START = "svl.kadatha.filex.REQUEST_START";
    static public final String ACTION_REQUEST_STOP = "svl.kadatha.filex.REQUEST_STOP";
    // Service will (global) broadcast when server start/stop
    static public final String ACTION_STARTED = "svl.kadatha.filex.FTPSERVER_STARTED";
    static public final String ACTION_STOPPED = "svl.kadatha.filex.FTPSERVER_STOPPED";
    static public final String ACTION_FAILEDTOSTART = "svl.kadatha.filex.FTPSERVER_FAILEDTOSTART";
    // The server thread will check this often to look for incoming
    // connections. We are forced to use non-blocking accept() and polling
    // because we cannot wait forever in accept() if we want to be able
    // to receive an exit signal and cleanly exit.
    public static final int WAKE_INTERVAL_MS = 1000; // milliseconds
    private static final String TAG = FsService.class.getSimpleName();
    protected static Thread serverThread = null;
    private final List<SessionThread> sessionThreads = new ArrayList<>();
    private final FsServiceBinder fsServiceBinder = new FsServiceBinder();
    protected boolean shouldExit = false;
    protected ServerSocket listenSocket;
    private TcpListener wifiListener = null;
    private PowerManager.WakeLock wakeLock;
    private WifiLock wifiLock = null;

    /**
     * Check to see if the FTP Server is up and running
     *
     * @return true if the FTP Server is up and running
     */
    public static boolean isRunning() {
        // return true if and only if a server Thread is running
        if (serverThread == null) {
            Timber.tag(TAG).d("Server is not running (null serverThread)");
            return false;
        }
        if (!serverThread.isAlive()) {
            Timber.tag(TAG).d("serverThread non-null but !isAlive()");
        } else {
            Timber.tag(TAG).d("Server is alive");
        }
        return true;
    }

    /**
     * Start this service, which will start the FTP Server
     */
    public static void start() {
        Context context = App.getAppContext();
        Intent serviceIntent = new Intent(context, FsService.class);
        if (!FsService.isRunning()) {
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }

    /**
     * Stop the service and thus stop the FTP Server
     */
    public static void stop() {
        Context context = App.getAppContext();
        Intent serverService = new Intent(context, FsService.class);
        context.stopService(serverService);
    }

    /**
     * Will check if the device contains external storage (sdcard) and display a warning
     * for the user if there is no external storage. Nothing more.
     */
    private static void warnIfNoExternalStorage() {
        String storageState = Environment.getExternalStorageState();
        if (!storageState.equals(Environment.MEDIA_MOUNTED)) {
            Timber.tag(TAG).v("Warning due to storage state " + storageState);
            Toast toast = Toast.makeText(App.getAppContext(),
                    R.string.storage_warning, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /**
     * Gets the local ip address
     *
     * @return local ip address or null if not found
     */
    public static InetAddress getLocalInetAddress() {
        InetAddress returnAddress = null;
        if (!isConnectedToLocalNetwork()) {
            Timber.tag(TAG).e("getLocalInetAddress called and no connection");
            return null;
        }
        try {
            ArrayList<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                // only check network interfaces that give local connection
                if (!networkInterface.getName().matches("^(eth|wlan).*")) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress()
                            && !address.isLinkLocalAddress()
                            && address instanceof Inet4Address) {
                        if (returnAddress != null) {
                            Timber.tag(TAG).d("Found more than one valid address local inet address, why???");
                        }
                        returnAddress = address;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnAddress;
    }

    /**
     * Checks to see if we are connected to a local network, for instance wifi or ethernet
     *
     * @return true if connected to a local network
     */
    public static boolean isConnectedToLocalNetwork() {
        boolean connected = false;
        Context context = App.getAppContext();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        connected = ni != null
                && ni.isConnected()
                && (ni.getType() & (ConnectivityManager.TYPE_WIFI | ConnectivityManager.TYPE_ETHERNET)) != 0;
        if (!connected) {
            Timber.tag(TAG).d("isConnectedToLocalNetwork: see if it is an WIFI AP");
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            try {
                Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
                connected = (Boolean) method.invoke(wm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!connected) {
            Timber.tag(TAG).d("isConnectedToLocalNetwork: see if it is an USB AP");
            try {
                ArrayList<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface netInterface : networkInterfaces) {
                    if (netInterface.getDisplayName().startsWith("rndis")) {
                        connected = true;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return connected;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FsNotification.NOTIFICATION_ID, FsNotification.setupNotification(this));

        //https://developer.android.com/reference/android/app/Service.html
        //if there are not any pending start commands to be delivered to the service, it will be called with a null intent object,
        if (intent != null && intent.getAction() != null) {
            Timber.tag(TAG).d("onStartCommand called with action: " + intent.getAction());

            switch (intent.getAction()) {
                case ACTION_REQUEST_START:
                    if (isRunning()) {
                        return START_STICKY;
                    }
                    break;
                case ACTION_REQUEST_STOP:
                    stopSelf();
                    return START_NOT_STICKY;
            }
        }

        warnIfNoExternalStorage();

        shouldExit = false;
        int attempts = 10;
        // The previous server thread may still be cleaning up, wait for it to finish.
        while (serverThread != null) {
            Timber.tag(TAG).w("Won't start, server thread exists");
            if (attempts > 0) {
                attempts--;
                Util.sleepIgnoreInterrupt(1000);
            } else {
                Timber.tag(TAG).w("Server thread already exists");
                return START_STICKY;
            }
        }
        Timber.tag(TAG).d("Creating server thread");
        serverThread = new Thread(this);
        serverThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.tag(TAG).i("onDestroy() Stopping server");
        shouldExit = true;
        if (serverThread == null) {
            Timber.tag(TAG).w("Stopping with null serverThread");
            return;
        }
        serverThread.interrupt();
        try {
            serverThread.join(10000); // wait 10 sec for server thread to finish
        } catch (InterruptedException ignored) {
        }
        if (serverThread.isAlive()) {
            Timber.tag(TAG).w("Server thread failed to exit");
            // it may still exit eventually if we just leave the shouldExit flag set
        } else {
            Timber.tag(TAG).d("serverThread join()ed ok");
            serverThread = null;
        }
        try {
            if (listenSocket != null) {
                Timber.tag(TAG).i("Closing listenSocket");
                listenSocket.close();
            }
        } catch (IOException ignored) {
        }

        if (wifiLock != null) {
            Timber.tag(TAG).d("onDestroy: Releasing wifi lock");
            wifiLock.release();
            wifiLock = null;
        }
        if (wakeLock != null) {
            Timber.tag(TAG).d("onDestroy: Releasing wake lock");
            wakeLock.release();
            wakeLock = null;
        }
        Timber.tag(TAG).d("FTPServerService.onDestroy() finished");
    }

    // This opens a listening socket on all interfaces.
    void setupListener() throws IOException {
        listenSocket = new ServerSocket();
        listenSocket.setReuseAddress(true);
        listenSocket.bind(new InetSocketAddress(FsSettings.getPortNumber()));
    }

    @Override
    public void run() {
        Timber.tag(TAG).d("Server thread running");

        if (!isConnectedToLocalNetwork()) {
            Timber.tag(TAG).w("run: There is no local network, bailing out");
            stopSelf();
            Intent intent = new Intent(ACTION_FAILEDTOSTART);
            intent.setPackage(Global.FILEX_PACKAGE);
            sendBroadcast(intent);
            return;
        }

        // Initialization of wifi, set up the socket
        try {
            setupListener();
        } catch (IOException e) {
            Timber.tag(TAG).w("run: Unable to open port, bailing out.");
            stopSelf();
            Intent intent = new Intent(ACTION_FAILEDTOSTART);
            intent.setPackage(Global.FILEX_PACKAGE);
            sendBroadcast(intent);
            return;
        }

        // @TODO: when using ethernet, is it needed to take wifi lock?
        takeWifiLock();
        takeWakeLock();

        // A socket is open now, so the FTP server is started, notify rest of world
        Timber.tag(TAG).i("Ftp Server up and running, broadcasting ACTION_STARTED");
        Intent intent = new Intent(ACTION_STARTED);
        intent.setPackage(Global.FILEX_PACKAGE);
        sendBroadcast(intent);

        while (!shouldExit) {
            if (wifiListener != null) {
                if (!wifiListener.isAlive()) {
                    Timber.tag(TAG).d("Joining crashed wifiListener thread");
                    try {
                        wifiListener.join();
                    } catch (InterruptedException ignored) {
                    }
                    wifiListener = null;
                }
            }
            if (wifiListener == null) {
                // Either our wifi listener hasn't been created yet, or has crashed,
                // so spawn it
                wifiListener = new TcpListener(listenSocket, this);
                wifiListener.start();
            }
            try {
                // TODO: think about using ServerSocket, and just closing
                // the main socket to send an exit signal
                Thread.sleep(WAKE_INTERVAL_MS);
            } catch (InterruptedException e) {
                Timber.tag(TAG).d("Thread interrupted");
            }
        }

        terminateAllSessions();

        if (wifiListener != null) {
            wifiListener.quit();
            wifiListener = null;
        }
        shouldExit = false; // we handled the exit flag, so reset it to acknowledge
        Timber.tag(TAG).d("Exiting cleanly, returning from run()");

        stopSelf();
        Intent intent_stop = new Intent(ACTION_STOPPED);
        intent_stop.setPackage(Global.FILEX_PACKAGE);
        sendBroadcast(intent_stop);
    }

    private void terminateAllSessions() {
        Timber.tag(TAG).i("Terminating " + sessionThreads.size() + " session thread(s)");
        synchronized (this) {
            for (SessionThread sessionThread : sessionThreads) {
                if (sessionThread != null) {
                    sessionThread.closeDataSocket();
                    sessionThread.closeSocket();
                }
            }
        }
    }

    /**
     * Takes the wake lock
     * <p>
     * Many devices seem to not properly honor a PARTIAL_WAKE_LOCK, which should prevent
     * CPU throttling. For these devices, we have a option to force the phone into a full
     * wake lock.
     */
    private void takeWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            Timber.tag(TAG).d("takeWakeLock: Taking full wake lock");
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.setReferenceCounted(false);
        }
        wakeLock.acquire();
    }

    private void takeWifiLock() {
        Timber.tag(TAG).d("takeWifiLock: Taking wifi lock");
        if (wifiLock == null) {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiLock = manager.createWifiLock(TAG);
            wifiLock.setReferenceCounted(false);
        }
        wifiLock.acquire();
    }

    /**
     * The FTPServerService must know about all running session threads so they can be
     * terminated on exit. Called when a new session is created.
     */
    public void registerSessionThread(SessionThread newSession) {
        // Before adding the new session thread, clean up any finished session
        // threads that are present in the list.

        // Since we're not allowed to modify the list while iterating over
        // it, we construct a list in toBeRemoved of threads to remove
        // later from the sessionThreads list.
        synchronized (this) {
            List<SessionThread> toBeRemoved = new ArrayList<>();
            for (SessionThread sessionThread : sessionThreads) {
                if (!sessionThread.isAlive()) {
                    Timber.tag(TAG).d("Cleaning up finished session...");
                    try {
                        sessionThread.join();
                        Timber.tag(TAG).d("Thread joined");
                        toBeRemoved.add(sessionThread);
                        sessionThread.closeSocket(); // make sure socket closed
                    } catch (InterruptedException e) {
                        Timber.tag(TAG).d("Interrupted while joining");
                        // We will try again in the next loop iteration
                    }
                }
            }
            for (SessionThread removeThread : toBeRemoved) {
                sessionThreads.remove(removeThread);
            }

            // Cleanup is complete. Now actually add the new thread to the list.
            sessionThreads.add(newSession);
        }
        Timber.tag(TAG).d("Registered session thread");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return fsServiceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stop();
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Timber.tag(TAG).d("user has removed my activity, we got killed! restarting...");
        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 2000, restartServicePI);
    }


    public class FsServiceBinder extends Binder {

        public FsService getService() {
            return FsService.this;
        }
    }
}
