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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import svl.kadatha.filex.network.NetworkScanUtil;

public class NetworkCloudHostPickerDialogViewModel extends ViewModel {
    public final ArrayList<NetworkCloudTypeSelectDialog.PickerItem> items = new ArrayList<>();
    public String what_type_network_cloud;
    public static final class HostResult {
        public final String host;
        public final int port;
        public final String display;

        public HostResult(String host, int port, String display) {
            this.host = host;
            this.port = port;
            this.display = display;
        }
    }

    public final MutableLiveData<AsyncTaskStatus> scanHostAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> populateNetworkCloudServersAsyncStatus= new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);

    // emits one item at a time (for live append)
    public final MutableLiveData<HostResult> newResult = new MutableLiveData<>();

    private volatile boolean isCancelled;
    private Future<?> future1,future2;

    @Override
    protected void onCleared() {
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning) {
        isCancelled = true;
        if (future1 != null){
            future1.cancel(mayInterruptRunning);
        }
        if (future2 != null) {
            future2.cancel(mayInterruptRunning);
        }
    }

    public void populateNetworkCloudServers(String what_type_network_cloud) {
        if (populateNetworkCloudServersAsyncStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED){
            return;
        }
        this.what_type_network_cloud=what_type_network_cloud;
        populateNetworkCloudServersAsyncStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                items.addAll(buildPickerItems(what_type_network_cloud));
                populateNetworkCloudServersAsyncStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
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
    public void scanStandardPorts(Context context) {
        if (scanHostAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;

        scanHostAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        isCancelled = false;

        // Standard first, then alternates
        final int[] phase1 = new int[]{21, 22, 445, 80, 443};
        final int[] phase2 = new int[]{2121, 2222, 2200, 8080, 8443};

        final int hostCap = 24;
        final long timeLimitMs = 8000;
        final int timeoutMs = 300;
        final int threads = 24;
        final int maxResults = 24;

        Context appCtx = context.getApplicationContext();

        ExecutorService exec = MyExecutorService.getExecutorService();
        future2 = exec.submit(() -> {
            long start = System.currentTimeMillis();

            int myIp = NetworkScanUtil.getMyIpv4IntCompat(appCtx);
            if (myIp == 0 || isCancelled) {
                scanHostAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                return;
            }

            int[] candidates = NetworkScanUtil.pickNeighborHosts(myIp, hostCap);

            Set<String> seen = new HashSet<>();
            AtomicInteger found = new AtomicInteger(0);

            runPhase(candidates, myIp, phase1, start, timeLimitMs, timeoutMs, threads, maxResults, seen, found);

            if (!isCancelled
                    && (System.currentTimeMillis() - start) < timeLimitMs
                    && found.get() < maxResults) {
                runPhase(candidates, myIp, phase2, start, timeLimitMs, timeoutMs, threads, maxResults, seen, found);
            }

            scanHostAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        });
    }

    private void runPhase(int[] candidates,
                          int myIp,
                          int[] ports,
                          long start,
                          long timeLimitMs,
                          int timeoutMs,
                          int threads,
                          int maxResults,
                          Set<String> seen,
                          AtomicInteger found) {

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(Math.max(1, threads));
        ArrayList<java.util.concurrent.Future<?>> jobs = new ArrayList<>(candidates.length);

        for (int ip : candidates) {
            if (isCancelled) break;
            if ((System.currentTimeMillis() - start) >= timeLimitMs) break;
            if (found.get() >= maxResults) break;
            if (ip == myIp) continue;

            jobs.add(pool.submit(() -> {
                if (isCancelled) return;
                if ((System.currentTimeMillis() - start) >= timeLimitMs) return;
                if (found.get() >= maxResults) return;

                String host = NetworkScanUtil.intToIpv4(ip);
                int openPort = NetworkScanUtil.firstOpenPort(host, ports, timeoutMs);
                if (openPort > 0 && !isCancelled) {
                    String key = host + ":" + openPort;
                    synchronized (seen) {
                        if (seen.contains(key)) return;
                        seen.add(key);
                    }

                    int n = found.incrementAndGet();
                    String proto = NetworkScanUtil.protocolByPort(openPort);
                    String display = host + " (" + proto + ":" + openPort + ")";
                    newResult.postValue(new HostResult(host, openPort, display));

                    if (n >= maxResults) return;
                }
            }));
        }

        for (java.util.concurrent.Future<?> j : jobs) {
            if (isCancelled) break;
            if ((System.currentTimeMillis() - start) >= timeLimitMs) break;
            if (found.get() >= maxResults) break;
            try { j.get(); } catch (Exception ignored) {}
        }

        pool.shutdownNow();
    }
}
