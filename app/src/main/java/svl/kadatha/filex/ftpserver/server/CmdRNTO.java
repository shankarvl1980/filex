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
import svl.kadatha.filex.ftpserver.utils.FtpServerFileUtil;
import timber.log.Timber;


/**
 * CmdRNTO implements RENAME TO (RNTO)
 * This command specifies the new pathname of the file
 * specified in the immediately preceding "rename from"
 * command. Together the two commands cause a file to be
 * renamed.
 */
public class CmdRNTO extends FtpCmd implements Runnable {

    protected final String input;

    public CmdRNTO(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Timber.tag(TAG).d("RNTO executing");
        String param = getParameter(input);
        String errString = null;
        File toFile = null;
        mainblock:
        {
            toFile = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);
            Timber.tag(TAG).d("RNTO to file: " + toFile.getPath());
            if (violatesChroot(toFile)) {
                errString = "550 Invalid name or chroot violation\r\n";
                break mainblock;
            }
            File fromFile = sessionThread.getRenameFrom();
            if (fromFile == null) {
                errString = "550 Rename error, maybe RNFR not sent\r\n";
                break mainblock;
            }
            Timber.tag(TAG).d("RNTO from file: " + fromFile.getPath());

            if (fromFile.isDirectory()) {
                FtpServerFileUtil.renameFolder(fromFile, toFile, App.getAppContext());
            } else {
                FtpServerFileUtil.moveFile(fromFile, toFile, App.getAppContext());
            }

        }
        if (errString != null) {
            sessionThread.writeString(errString);
            Timber.tag(TAG).d("RNFR failed: " + errString.trim());
        } else {
            sessionThread.writeString("250 rename successful\r\n");
        }
        sessionThread.setRenameFrom(null);
        Timber.tag(TAG).d("RNTO finished");
    }
}
