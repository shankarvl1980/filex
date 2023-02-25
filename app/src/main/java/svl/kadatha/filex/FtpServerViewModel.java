package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import svl.kadatha.filex.ftpserver.ftp.FsService;
import svl.kadatha.filex.ftpserver.server.FtpUser;

public class FtpServerViewModel extends AndroidViewModel {
    public String user_name,password,chroot;
    public static int PORT;
    public static FtpUser FTP_USER;

    public FtpServerViewModel(@NonNull Application application) {
        super(application);
        PORT=Integer.parseInt(application.getString(R.string.portnumber_default));
        FTP_USER=new FtpUser(application.getString(R.string.username_default),application.getString(R.string.password_default),Global.INTERNAL_PRIMARY_STORAGE_PATH);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        FsService.stop();
    }
}
