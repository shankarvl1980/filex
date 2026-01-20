package svl.kadatha.filex;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import svl.kadatha.filex.network.NetworkScanUtil;

public class NetworkCloudHostPickerDialogViewModel extends ViewModel {

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

    public final MutableLiveData<AsyncTaskStatus> scanHostAsyncTaskStatus =
            new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);

    // emits one item at a time (for live append)
    public final MutableLiveData<HostResult> newResult = new MutableLiveData<>();

    private volatile boolean isCancelled;
    private Future<?> future;

    @Override
    protected void onCleared() {
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning) {
        isCancelled = true;
        if (future != null) future.cancel(mayInterruptRunning);
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
        future = exec.submit(() -> {
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
