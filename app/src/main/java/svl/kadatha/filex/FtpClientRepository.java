package svl.kadatha.filex;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;

public class FtpClientRepository {

    private static FtpClientRepository ftpClientRepository;

    public FTPClient ftpClientMain;
    public FTPClient ftpClientForCount;
    public FTPClient ftpClientForProgress;
    public FTPClient ftpClientForCopyView;
    public FTPClient ftpClientForListing;
    public FTPClient ftpClientForCreatingFilePojo;
    public FTPClient ftpClientForCheckDirectory;
    public FTPClient ftpClientForAddPojo;
    public FTPClient ftpClientForNoop;


    private FtpClientRepository(){}

    public static FtpClientRepository getInstance()
    {
        if(ftpClientRepository==null)
        {
            ftpClientRepository=new FtpClientRepository();
        }
        return ftpClientRepository;
    }

    public synchronized void instantiate()
    {
        ftpClientMain =new FTPClient();
        ftpClientForCount =new FTPClient();
        ftpClientForProgress =new FTPClient();
        ftpClientForCopyView =new FTPClient();
        ftpClientForListing =new FTPClient();
        ftpClientForCreatingFilePojo =new FTPClient();
        ftpClientForCheckDirectory =new FTPClient();
        ftpClientForAddPojo =new FTPClient();
        ftpClientForNoop =new FTPClient();
    }

    public synchronized void disconnect_ftp_clients() {
        try {

            RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
            Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                    iterator.remove();
                }
            }

            Iterator<FilePOJO> iterator1 = MainActivity.RECENTS.iterator();
            while (iterator1.hasNext()) {
                if (iterator1.next().getFileObjectType() == FileObjectType.FTP_TYPE) {
                    iterator1.remove();
                }
            }

            FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), FileObjectType.FTP_TYPE);
            Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.FTP_CACHE_DIR);

            ftpClientMain.disconnect();
            ftpClientForCount.disconnect();
            ftpClientForProgress.disconnect();
            ftpClientForCopyView.disconnect();
            ftpClientForListing.disconnect();
            ftpClientForCreatingFilePojo.disconnect();
            ftpClientForCheckDirectory.disconnect();
            ftpClientForAddPojo.disconnect();
            ftpClientForNoop.disconnect();

        } catch (Exception e) {
                //e.getMessage();
        }
    }

    public synchronized boolean connect_ftp_client(FTPClient ftpClient, FtpDetailsDialog.FtpPOJO ftpPOJO) throws IOException {
        if(ftpPOJO==null)return false;
        ftpClient.connect(ftpPOJO.server,ftpPOJO.port);
        if(FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {

            /**
             * Sets the time to wait between sending control connection keepalive messages when processing file upload or download.
             * <p>
             * See the class Javadoc section "Control channel keep-alive feature"
             * </p>
             *
             * @deprecated Use {@link #setControlKeepAliveTimeout(Duration)}.
             * @param controlIdleSeconds the wait (in seconds) between keepalive messages. Zero (or less) disables.
             * @since 3.0
             * @see #setControlKeepAliveReplyTimeout(int)
             */
            ftpClient.setControlKeepAliveTimeout(10);//(1200);


            /**
             * Sets how long to wait for control keep-alive message replies.
             *
             * @deprecated Use {@link #setControlKeepAliveReplyTimeout(Duration)}.
             * @param timeoutMillis number of milliseconds to wait (defaults to 1000)
             * @since 3.0
             * @see #setControlKeepAliveTimeout(long)
             */
            ftpClient.setControlKeepAliveReplyTimeout(500);//(20000);

            boolean loggedInStatus = ftpClient.login(ftpPOJO.user_name,ftpPOJO.password);
            if (loggedInStatus) {

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();
                return true;
            }
        }

        return false;
    }

    public boolean connect_all_ftp_clients(FtpDetailsDialog.FtpPOJO ftpPOJO) throws IOException
    {
        boolean loggedInStatus=connect_ftp_client(ftpClientMain,ftpPOJO);
        connect_ftp_client(ftpClientForProgress,ftpPOJO);
        connect_ftp_client(ftpClientForCopyView,ftpPOJO);
        connect_ftp_client(ftpClientForListing,ftpPOJO);
        connect_ftp_client(ftpClientForCreatingFilePojo,ftpPOJO);
        connect_ftp_client(ftpClientForCheckDirectory,ftpPOJO);
        connect_ftp_client(ftpClientForAddPojo,ftpPOJO);
        connect_ftp_client(ftpClientForNoop,ftpPOJO);

        return loggedInStatus;
    }

}
