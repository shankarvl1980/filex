package svl.kadatha.filex.ftpserver;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.R;
import svl.kadatha.filex.RepositoryClass;
import svl.kadatha.filex.ftpserver.ftp.FsService;
import svl.kadatha.filex.ftpserver.server.FtpUser;

public class FtpServerViewModel extends AndroidViewModel {
    public static int PORT;
    public static FtpUser FTP_USER;
    public static boolean ALLOW_ANONYMOUS;
    public final List<String> chroot_list;
    public String user_name = "pc", password = "pc", chroot = Global.INTERNAL_PRIMARY_STORAGE_PATH;

    public FtpServerViewModel(@NonNull Application application) {
        super(application);
        PORT = Integer.parseInt(application.getString(R.string.portnumber_default));
        FTP_USER = new FtpUser(application.getString(R.string.username_default), application.getString(R.string.password_default), Global.INTERNAL_PRIMARY_STORAGE_PATH);
        chroot_list = new ArrayList<>();
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        for (FilePOJO filePOJO : repositoryClass.storage_dir) {
            if (!filePOJO.getPath().equals(File.separator) && filePOJO.getFileObjectType() == FileObjectType.FILE_TYPE) {
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
