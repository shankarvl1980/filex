package svl.kadatha.filex.ftpserver.server;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

public class CmdSIZE extends FtpCmd {
    private static final String TAG = CmdSIZE.class.getSimpleName();

    protected final String input;

    public CmdSIZE(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Timber.tag(TAG).d("SIZE executing");
        String errString = null;
        String param = getParameter(input);
        long size = 0;
        mainblock:
        {
            File currentDir = sessionThread.getWorkingDir();
            if (param.contains(File.separator)) {
                errString = "550 No directory traversal allowed in SIZE param\r\n";
                break mainblock;
            }
            File target = new File(currentDir, param);
            // We should have caught any invalid location access before now, but
            // here we check again, just to be explicitly sure.
            if (violatesChroot(target)) {
                errString = "550 SIZE target violates chroot\r\n";
                break mainblock;
            }
            if (!target.exists()) {
                errString = "550 Cannot get the SIZE of nonexistent object\r\n";
                try {
                    Timber.tag(TAG).i("Failed getting size of: " + target.getCanonicalPath());
                } catch (IOException e) {
                }
                break mainblock;
            }
            if (!target.isFile()) {
                errString = "550 Cannot get the size of a non-file\r\n";
                break mainblock;
            }
            size = target.length();
        }
        if (errString != null) {
            sessionThread.writeString(errString);
        } else {
            sessionThread.writeString("213 " + size + "\r\n");
        }
        Timber.tag(TAG).d("SIZE complete");
    }

}
