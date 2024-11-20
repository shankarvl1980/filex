/*
Copyright 2011-2013 Pieter Pareit
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
*/

package svl.kadatha.filex.ftpserver.ftp;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import svl.kadatha.filex.App;
import svl.kadatha.filex.ftpserver.FtpServerViewModel;
import svl.kadatha.filex.R;
import svl.kadatha.filex.ftpserver.server.FtpUser;
import timber.log.Timber;


public class FsSettings {

    private final static String TAG = FsSettings.class.getSimpleName();

    public static List<FtpUser> getUsers() {
        final Context context = App.getAppContext();
//        final SharedPreferences sp = getSharedPreferences();
//        if (sp.contains("users")) {
//            Gson gson = new Gson();
//            Type listType = new TypeToken<List<FtpUser>>() {
//            }.getType();
//            return gson.fromJson(sp.getString("users", null), listType);
//        } else if (sp.contains("username")) {
//            // on ftp server version < 2.19 we had username/password preference
//            String username = sp.getString("username", context.getString(R.string.username_default));
//            String password = sp.getString("password", context.getString(R.string.password_default));
//            String chroot = sp.getString("chrootDir", "");
//            if (username == null || password == null || chroot == null) {
//                username = context.getString(R.string.username_default);
//                password = context.getString(R.string.password_default);
//                chroot = "";
//            }
//            return new ArrayList<>(Collections.singletonList(new FtpUser(username, password, chroot)));
//        } else {
        FtpUser defaultUser = new FtpUser(context.getString(R.string.username_default), context.getString(R.string.password_default), "\\");
        return new ArrayList<>(Collections.singletonList(defaultUser));
//        }
    }

    public static FtpUser getUser(String username) {

        return FtpServerViewModel.FTP_USER;
    }

    public static boolean allowAnonymous() {
        return FtpServerViewModel.ALLOW_ANONYMOUS;
    }

    public static File getDefaultChrootDir() {
        File chrootDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            chrootDir = Environment.getExternalStorageDirectory();
        } else {
            chrootDir = new File("/");
        }
        if (!chrootDir.isDirectory()) {
            Timber.tag(TAG).e("getChrootDir: not a directory");
            // if this happens, we are screwed
            // we give it the application directory
            // but this will probably not be what the user wants
            return App.getAppContext().getFilesDir();
        }
        return chrootDir;
    }

    public static int getPortNumber() {
        return FtpServerViewModel.PORT;

    }

/**
 * @return the SharedPreferences for this application
 */

}
