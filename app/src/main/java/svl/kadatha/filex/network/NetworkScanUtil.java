package svl.kadatha.filex.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import svl.kadatha.filex.NetworkCloudHostPickerDialogViewModel;

public final class NetworkScanUtil {

    private NetworkScanUtil() {}

    // ---- Contracts ----
    public interface CancelledSupplier { boolean isCancelled(); }
    public interface HitConsumer { void onHit(ScanHit hit); }

    // ---- Outputs ----
    public static final class ScanHit {
        public final String host;
        public final int port;
        public final String serviceName;

        public ScanHit(String host, int port, String serviceName) {
            this.host = host;
            this.port = port;
            this.serviceName = serviceName;
        }
    }

    public static final class NetContext {
        public final int myIpInt;
        public final int prefixLength;

        public NetContext(int myIpInt, int prefixLength) {
            this.myIpInt = myIpInt;
            this.prefixLength = prefixLength;
        }
    }

    // ---- Service -> ports mapping ----
    public static int[] portsForFilter(NetworkCloudHostPickerDialogViewModel.ServiceFilter filter) {
        switch (filter) {
            case FTP:    return new int[]{21};
            case SFTP:   return new int[]{22};
            case WEBDAV: return new int[]{80, 443};   // common WebDAV HTTP/HTTPS
            case SMB:    return new int[]{445};
            case ANY:
            default:
                return new int[]{21, 22, 445, 80, 443};
        }
    }

    public static String serviceNameForPort(int port) {
        switch (port) {
            case 21: return "FTP";
            case 22: return "SFTP";
            case 80: return "HTTP/WebDAV";
            case 443: return "HTTPS/WebDAV";
            case 445: return "SMB";
            default: return "PORT-" + port;
        }
    }

    // ---- LAN context resolution (private IPv4 only) ----
    @Nullable
    public static NetContext resolveLanContext(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;

        Network[] networks = cm.getAllNetworks();
        if (networks == null) return null;

        // Prefer Wi-Fi/Ethernet and private IPv4
        for (Network n : networks) {
            if (n == null) continue;

            NetworkCapabilities caps = cm.getNetworkCapabilities(n);
            if (caps == null) continue;

            boolean isLan = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);

            if (!isLan) continue;

            LinkProperties lp = cm.getLinkProperties(n);
            if (lp == null) continue;

            for (LinkAddress la : lp.getLinkAddresses()) {
                InetAddress addr = la.getAddress();
                if (!(addr instanceof Inet4Address)) continue;

                String ipStr = addr.getHostAddress();
                if (!isPrivateLanIpv4(ipStr)) continue;

                int ipInt = ipv4ToInt(ipStr);
                if (ipInt == 0) continue;

                int prefix = la.getPrefixLength();
                return new NetContext(ipInt, prefix);
            }
        }

        // Fallback: scan interfaces
        int any = getAnyPrivateIpv4Int();
        if (any != 0) return new NetContext(any, 24);

        return null;
    }

    private static boolean isPrivateLanIpv4(String ip) {
        if (ip == null) return false;

        if (ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;

        if (ip.startsWith("172.")) {
            String[] p = ip.split("\\.");
            if (p.length >= 2) {
                try {
                    int second = Integer.parseInt(p[1]);
                    return second >= 16 && second <= 31;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    // ---- Scan engine ----
    public static void runScanWithPool(
            ExecutorService pool,
            int[] candidates,
            int[] ports,
            long startMs,
            long timeLimitMs,
            int timeoutMs,
            int maxResults,
            CancelledSupplier cancelled,
            HitConsumer consumer
    ) {
        if (pool == null || candidates == null || ports == null) return;

        final java.util.concurrent.atomic.AtomicInteger found =
                new java.util.concurrent.atomic.AtomicInteger(0);

        for (int ipInt : candidates) {
            if (cancelled.isCancelled()) break;
            if ((System.currentTimeMillis() - startMs) > timeLimitMs) break;
            if (found.get() >= maxResults) break;

            final String ipStr = intToIpv4(ipInt);

            pool.submit(() -> {
                if (cancelled.isCancelled()) return;
                if ((System.currentTimeMillis() - startMs) > timeLimitMs) return;
                if (found.get() >= maxResults) return;

                // First open port among filter ports
                int open = firstOpenPort(ipStr, ports, timeoutMs);
                if (open > 0) {
                    int now = found.incrementAndGet();
                    consumer.onHit(new ScanHit(ipStr, open, serviceNameForPort(open)));
                    if (now >= maxResults) return;
                }
            });
        }

        // Wait bounded by remaining time
        try {
            long remaining = Math.max(1, timeLimitMs - (System.currentTimeMillis() - startMs));
            pool.shutdown();
            pool.awaitTermination(remaining, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // returns first open port from list, or -1
    public static int firstOpenPort(@NonNull String host, @NonNull int[] ports, int timeoutMs) {
        for (int p : ports) {
            if (isTcpOpen(host, p, timeoutMs)) return p;
        }
        return -1;
    }

    public static boolean isTcpOpen(String host, int port, int timeoutMs) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (s != null) try { s.close(); } catch (Exception ignored2) {}
        }
    }

    // ---- Candidates: ARP + subnet ----
    public static List<Integer> readArpIpv4Ints() {
        Set<Integer> out = new LinkedHashSet<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 1) {
                    int ip = ipv4ToInt(parts[0]);
                    if (ip != 0) out.add(ip);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (br != null) try { br.close(); } catch (Exception ignored2) {}
        }
        return new ArrayList<>(out);
    }

    /**
     * Safe subnet sweep (unsigned math). cap keeps it sane.
     */
    public static int[] buildSubnetCandidates(int myIpInt, int prefixLen, int cap) {
        if (myIpInt == 0) return new int[0];
        if (prefixLen <= 0 || prefixLen > 32) prefixLen = 24;

        long ip = myIpInt & 0xFFFFFFFFL;
        long mask = prefixLen == 0 ? 0L : (0xFFFFFFFFL << (32 - prefixLen)) & 0xFFFFFFFFL;

        long network = ip & mask;
        long broadcast = network | (~mask & 0xFFFFFFFFL);

        List<Integer> ips = new ArrayList<>();

        for (long host = network + 1; host < broadcast; host++) {
            int hostInt = (int) host;
            if (hostInt == myIpInt) continue;
            ips.add(hostInt);
            if (cap > 0 && ips.size() >= cap) break;
        }

        int[] arr = new int[ips.size()];
        for (int i = 0; i < ips.size(); i++) arr[i] = ips.get(i);
        return arr;
    }

    // ---- Fallback: interface scan ----
    public static int getAnyPrivateIpv4Int() {
        try {
            for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni == null) continue;
                if (!ni.isUp() || ni.isLoopback()) continue;

                for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address)) continue;

                    String ip = addr.getHostAddress();
                    if (!isPrivateLanIpv4(ip)) continue;

                    int ipInt = ipv4ToInt(ip);
                    if (ipInt != 0) return ipInt;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // ---- IPv4 helpers ----
    public static int ipv4ToInt(String ip) {
        try {
            String[] p = ip.split("\\.");
            if (p.length != 4) return 0;
            int a = Integer.parseInt(p[0]) & 0xFF;
            int b = Integer.parseInt(p[1]) & 0xFF;
            int c = Integer.parseInt(p[2]) & 0xFF;
            int d = Integer.parseInt(p[3]) & 0xFF;
            return (a << 24) | (b << 16) | (c << 8) | d;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String intToIpv4(int ip) {
        return ((ip >> 24) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "."
                + (ip & 0xFF);
    }
}
