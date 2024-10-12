package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import java.io.IOException;

public class NetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean noConnectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false
            );

            if (noConnectivity) {
                if (NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO != null) {
                    FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
                    ftpClientRepository.shutdown();
                }
                if (NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO != null) {
                    SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
                    sftpChannelRepository.shutdown();
                }
                if (NetworkAccountDetailsViewModel.WEBDAV_WORKING_DIR_PATH != null) {
                    WebDavClientRepository webDavClientRepository = null;
                    try {
                        webDavClientRepository = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
                        webDavClientRepository.shutdown();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (NetworkAccountDetailsViewModel.SMB_WORKING_DIR_PATH != null) {
                    SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                    smbClientRepository.shutdown();
                }
            }
        }
    }
}
