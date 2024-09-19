package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class NetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean noConnectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false
            );

            if (noConnectivity) {
                if(FtpDetailsViewModel.FTP_POJO!=null){
                    FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
                    ftpClientRepository.shutdown();
                }
                if(FtpDetailsViewModel.SFTP_POJO!=null){
                    SftpChannelRepository sftpChannelRepository=SftpChannelRepository.getInstance(FtpDetailsViewModel.SFTP_POJO);
                    sftpChannelRepository.shutdown();
                }
            }
        }
    }
}
