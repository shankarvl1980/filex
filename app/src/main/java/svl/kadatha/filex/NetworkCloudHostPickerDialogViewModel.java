package svl.kadatha.filex;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import svl.kadatha.filex.network.NetworkScanUtil;
import timber.log.Timber;

public class NetworkCloudHostPickerDialogViewModel extends ViewModel {

    public final ArrayList<NetworkCloudTypeSelectDialog.PickerItem> items = new ArrayList<>();
    public String what_type_network_cloud;

    public enum ServiceFilter {
        ANY,
        FTP,
        SFTP,
        WEBDAV,
        SMB
    }

    public final MutableLiveData<AsyncTaskStatus> scanHostAsyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);

    public final MutableLiveData<AsyncTaskStatus> populateNetworkCloudServersAsyncStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);

    private volatile boolean isCancelled;
    private Future<?> future1, future2;

    @Override
    protected void onCleared() {
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning) {
        isCancelled = true;
        if (future1 != null) future1.cancel(mayInterruptRunning);
        if (future2 != null) future2.cancel(mayInterruptRunning);
    }

    public void populateNetworkCloudServers(String what_type_network_cloud) {
        if (populateNetworkCloudServersAsyncStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;

        this.what_type_network_cloud = what_type_network_cloud;
        populateNetworkCloudServersAsyncStatus.setValue(AsyncTaskStatus.STARTED);

        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(() -> {
            items.addAll(buildPickerItems(what_type_network_cloud));
            populateNetworkCloudServersAsyncStatus.postValue(AsyncTaskStatus.COMPLETED);
        });
    }

    private List<NetworkCloudTypeSelectDialog.PickerItem> buildPickerItems(String what_type_network_cloud) {
        if (NetworkCloudTypeSelectDialog.NETWORK.equals(what_type_network_cloud)) {
            List<String> networkTypes = Arrays.asList(App.getAppContext().getResources().getStringArray(R.array.network_types));
            List<NetworkCloudTypeSelectDialog.PickerItem> out = new ArrayList<>(networkTypes.size());
            for (String s : networkTypes) out.add(new NetworkCloudTypeSelectDialog.PickerItem(R.drawable.network_icon, s, null));
            return out;
        } else if (NetworkCloudTypeSelectDialog.CLOUD.equals(what_type_network_cloud)) {
            List<String> cloudTypes = Arrays.asList(App.getAppContext().getResources().getStringArray(R.array.cloud_types));
            List<NetworkCloudTypeSelectDialog.PickerItem> out = new ArrayList<>(cloudTypes.size());
            for (String s : cloudTypes) {
                FileObjectType fot = null;
                switch (s) {
                    case "Google Drive":
                        fot = FileObjectType.GOOGLE_DRIVE_TYPE;
                        break;
                    case "Drop Box":
                        fot = FileObjectType.DROP_BOX_TYPE;
                        break;
                    case "MediaFire":
                        fot = FileObjectType.MEDIA_FIRE_TYPE;
                        break;
                    case "Yandex":
                        fot = FileObjectType.YANDEX_TYPE;
                        break;
                }
                out.add(new NetworkCloudTypeSelectDialog.PickerItem(R.drawable.cloud_icon, s, fot));
            }
            return out;
        }
        return new ArrayList<>();
    }

    /**
     * Call this with a filter:
     *  - ServiceFilter.FTP    -> only port 21 hits
     *  - ServiceFilter.SFTP   -> only port 22 hits
     *  - ServiceFilter.WEBDAV -> only port 80/443 hits
     *  - ServiceFilter.SMB    -> only port 445 hits
     *  - ServiceFilter.ANY    -> common ports
     */
    public void scanHosts(Context context, ServiceFilter filter) {
        if (scanHostAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;

        scanHostAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        isCancelled = false;

        // Tuning
        final long timeLimitMs = 8000;
        final int timeoutMs = 300;
        final int threads = 24;
        final int maxResults = 24;

        final Context appCtx = context.getApplicationContext();
        final ExecutorService exec = MyExecutorService.getExecutorService();

        future2 = exec.submit(() -> {
            long start = System.currentTimeMillis();

            // clear old hits (keep your UI clean)
            synchronized (items) {
                items.clear();
            }

            // Resolve LAN IPv4 (private 10/172/192.168); avoid 100.x / public
            NetworkScanUtil.NetContext nc = NetworkScanUtil.resolveLanContext(appCtx);
            if (nc == null || isCancelled) {
                Timber.tag(Global.TAG).d("SCAN nc=null or cancelled");
                scanHostAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                return;
            }

            int myIp = nc.myIpInt;
            int prefix = nc.prefixLength;

            // Hotspot often reports weird prefix (/32). Force /24 in that case.
            if (prefix <= 0 || prefix > 30) prefix = 24;

            Timber.tag(Global.TAG).d("SCAN lanMyIp=%s prefix=%d",
                    NetworkScanUtil.intToIpv4(myIp), prefix);

            // Ports for the requested service
            final int[] ports = NetworkScanUtil.portsForFilter(filter);

            // Candidates: ARP + subnet sweep
            List<Integer> arp = NetworkScanUtil.readArpIpv4Ints();
            int[] sweep = NetworkScanUtil.buildSubnetCandidates(myIp, prefix, 256);

            Set<Integer> cand = new HashSet<>();
            for (int ip : arp) cand.add(ip);
            for (int ip : sweep) cand.add(ip);
            cand.remove(myIp);

            // Deterministic order (optional but helps repeatability)
            List<Integer> ordered = new ArrayList<>(cand.size());
            ordered.addAll(cand);

            int[] candidates = new int[ordered.size()];
            for (int i = 0; i < ordered.size(); i++) candidates[i] = ordered.get(i);

            Timber.tag(Global.TAG).d("SCAN candidates=%d filter=%s ports=%s",
                    candidates.length, filter.name(), java.util.Arrays.toString(ports));

            ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));

            // Dedup by "ip:port" so the same host can show multiple services cleanly
            final Set<String> seen = new HashSet<>();

            try {
                NetworkScanUtil.runScanWithPool(
                        pool,
                        candidates,
                        ports,
                        start,
                        timeLimitMs,
                        timeoutMs,
                        maxResults,
                        () -> isCancelled,
                        hit -> {
                            if (isCancelled) return;

                            String key = hit.host + ":" + hit.port;
                            synchronized (seen) {
                                if (seen.contains(key)) return;
                                seen.add(key);
                            }

                            String display = hit.host + " (" + hit.serviceName + ":" + hit.port + ")";
                            Timber.tag(Global.TAG).d("HIT %s", display);

                            synchronized (items) {
                                items.add(NetworkCloudTypeSelectDialog.PickerItem.forHost(display, hit.host, hit.port));
                            }
                        }
                );
            } finally {
                try { pool.shutdownNow(); } catch (Exception ignored) {}
                scanHostAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
