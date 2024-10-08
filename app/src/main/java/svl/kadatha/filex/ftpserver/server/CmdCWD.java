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
import java.io.IOException;

import timber.log.Timber;

public class CmdCWD extends FtpCmd implements Runnable {
    private static final String TAG = CmdCWD.class.getSimpleName();

    protected final String input;

    public CmdCWD(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    @Override
    public void run() {
        Timber.tag(TAG).d("CWD executing");
        String param = getParameter(input);
        File newDir;
        String errString = null;
        mainblock:
        {
            newDir = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);

            // Ensure the new path does not violate the chroot restriction
            if (violatesChroot(newDir)) {
                errString = "550 Invalid name or chroot violation\r\n";
                sessionThread.writeString(errString);
                Timber.tag(TAG).i(errString);
                break mainblock;
            }

            try {
                newDir = newDir.getCanonicalFile();
                Timber.tag(TAG).i("New directory: " + newDir);
                if (!newDir.isDirectory()) {
                    sessionThread.writeString("550 Can't CWD to invalid directory\r\n");
                } else if (newDir.canRead()) {
                    sessionThread.setWorkingDir(newDir);
                    sessionThread.writeString("250 CWD successful\r\n");
                } else {
                    sessionThread.writeString("550 That path is inaccessible\r\n");
                }
            } catch (IOException e) {
                sessionThread.writeString("550 Invalid path\r\n");
                break mainblock;
            }
        }
        Timber.tag(TAG).d("CWD complete");
    }
}
