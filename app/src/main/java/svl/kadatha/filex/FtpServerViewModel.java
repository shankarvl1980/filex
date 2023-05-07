package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.ftpserver.ftp.FsService;
import svl.kadatha.filex.ftpserver.server.FtpUser;

public class FtpServerViewModel extends AndroidViewModel {
    public String user_name="pc",password="pc",chroot=Global.INTERNAL_PRIMARY_STORAGE_PATH;
    public static int PORT;
    public static FtpUser FTP_USER;
    public static boolean ALLOW_ANONYMOUS;
    public List<String> chroot_list;

    public FtpServerViewModel(@NonNull Application application) {
        super(application);
        PORT=Integer.parseInt(application.getString(R.string.portnumber_default));
        FTP_USER=new FtpUser(application.getString(R.string.username_default),application.getString(R.string.password_default),Global.INTERNAL_PRIMARY_STORAGE_PATH);
        chroot_list= new ArrayList<>();
        for(FilePOJO filePOJO:Global.STORAGE_DIR)
        {
            if(!filePOJO.getPath().equals(File.separator) && filePOJO.getFileObjectType()==FileObjectType.FILE_TYPE)
            {
                chroot_list.add(filePOJO.getPath());
            }
        }
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        FsService.stop();
    }
}
