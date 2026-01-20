package svl.kadatha.filex.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class NetworkScanUtil {

    private NetworkScanUtil() {}

    // API 21+ : get Wi-Fi/Ethernet IPv4 as int (big-endian)
    public static int getMyIpv4IntCompat(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return 0;

            Network[] networks = cm.getAllNetworks(); // API 21+
            if (networks == null) return 0;

            for (Network n : networks) {
                if (n == null) continue;

                NetworkCapabilities caps = cm.getNetworkCapabilities(n); // API 21+
                if (caps == null) continue;

                boolean isLan = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                if (!isLan) continue;

                NetworkInfo ni = cm.getNetworkInfo(n); // deprecated but OK on API21-22
                if (ni == null || !ni.isConnected()) continue;

                LinkProperties lp = cm.getLinkProperties(n); // API 21+
                if (lp == null) continue;

                for (LinkAddress la : lp.getLinkAddresses()) {
                    InetAddress addr = la.getAddress();
                    if (addr instanceof Inet4Address) {
                        byte[] b = addr.getAddress();
                        return ((b[0] & 0xFF) << 24)
                                | ((b[1] & 0xFF) << 16)
                                | ((b[2] & 0xFF) << 8)
                                | (b[3] & 0xFF);
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static String intToIpv4(int ip) {
        return ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }

    public static boolean isPortOpen(String host, int port, int timeoutMs) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }

    public static int firstOpenPort(String host, int[] ports, int timeoutMs) {
        if (ports == null) return 0;
        for (int p : ports) {
            if (isPortOpen(host, p, timeoutMs)) return p;
        }
        return 0;
    }

    // Pick at most 24 IPs around your phone's last octet in the same /24
    public static int[] pickNeighborHosts(int myIp, int hostCap) {
        int base = myIp & 0xFFFFFF00;
        int last = myIp & 0xFF;

        int half = Math.max(1, hostCap / 2);
        int start = Math.max(1, last - half);
        int end = Math.min(254, start + hostCap - 1);
        start = Math.max(1, end - hostCap + 1);

        int size = end - start + 1;
        int[] out = new int[size];
        int idx = 0;
        for (int i = start; i <= end; i++) out[idx++] = base | i;
        return out;
    }

    // Optional: nice label by port
    public static String protocolByPort(int port) {
        switch (port) {
            case 21:  return "FTP";
            case 22:  return "SFTP";
            case 80:  return "WebDAV";
            case 443: return "WebDAV(HTTPS)";
            case 445: return "SMB";
            case 8080:return "WebDAV";
            case 8443:return "WebDAV(HTTPS)";
            case 2121:return "FTP";
            case 2222:return "SFTP";
            case 2200:return "SFTP";
            default:  return "LAN";
        }
    }
}
