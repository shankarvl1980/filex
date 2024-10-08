/*
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

package svl.kadatha.filex.ftpserver.server;

import java.io.File;

import svl.kadatha.filex.App;
import svl.kadatha.filex.ftpserver.ftp.MediaUpdater;
import svl.kadatha.filex.ftpserver.utils.FtpServerFileUtil;
import timber.log.Timber;


public class CmdRMD extends FtpCmd implements Runnable {
    private static final String TAG = CmdRMD.class.getSimpleName();

    protected final String input;

    public CmdRMD(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Timber.tag(TAG).d("RMD executing");
        String param = getParameter(input);
        File toRemove;
        String errString = null;
        mainblock:
        {
            if (param.isEmpty()) {
                errString = "550 Invalid argument\r\n";
                break mainblock;
            }
            toRemove = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);
            if (violatesChroot(toRemove)) {
                errString = "550 Invalid name or chroot violation\r\n";
                break mainblock;
            }
            if (!toRemove.isDirectory()) {
                errString = "550 Can't RMD a non-directory\r\n";
                break mainblock;
            }
            if (toRemove.equals(new File("/"))) {
                errString = "550 Won't RMD the root directory\r\n";
                break mainblock;
            }
            if (!recursiveDelete(toRemove)) {
                errString = "550 Deletion error, possibly incomplete\r\n";
                break mainblock;
            }
        }
        if (errString != null) {
            sessionThread.writeString(errString);
            Timber.tag(TAG).i("RMD failed: " + errString.trim());
        } else {
            sessionThread.writeString("250 Removed directory\r\n");
        }
        Timber.tag(TAG).d("RMD finished");
    }

    /**
     * Accepts a file or directory name, and recursively deletes the contents of that
     * directory and all subdirectories.
     *
     * @param toDelete
     * @return Whether the operation completed successfully
     */
    protected boolean recursiveDelete(File toDelete) {
        if (!toDelete.exists()) {
            return false;
        }
        if (toDelete.isDirectory()) {
            // If any of the recursive operations fail, then we return false
            boolean success = true;
            for (File entry : toDelete.listFiles()) {
                success &= recursiveDelete(entry);
            }
            Timber.tag(TAG).d("Recursively deleted: " + toDelete);
            return success && FtpServerFileUtil.deleteFile(toDelete, App.getAppContext());
        } else {
            Timber.tag(TAG).d("RMD deleting file: " + toDelete);
            boolean success = FtpServerFileUtil.deleteFile(toDelete, App.getAppContext());
            MediaUpdater.notifyFileDeleted(toDelete.getPath());
            return success;
        }
    }
}
