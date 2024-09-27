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

/**
 * Since STOR and APPE are essentially identical except for append vs truncate,
 * the common code is in this class, and inherited by CmdSTOR and CmdAPPE.
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import svl.kadatha.filex.App;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.ftpserver.ftp.MediaUpdater;
import svl.kadatha.filex.ftpserver.utils.FtpServerFileUtil;
import timber.log.Timber;


abstract public class CmdAbstractStore extends FtpCmd {

    public CmdAbstractStore(SessionThread sessionThread, String input) {
        super(sessionThread);
    }

    public void doStorOrAppe(String param, boolean append) {
        Timber.tag(Global.TAG).d("STOR/APPE executing with append = " + append);

        File storeFile = inputPathToChrootedFile(sessionThread.getChrootDir(), sessionThread.getWorkingDir(), param);

        String errString = null;
        FileOutputStream out = null;

        storing:
        {
            // Get a normalized absolute path for the desired file
            if (violatesChroot(storeFile)) {
                errString = "550 Invalid name or chroot violation\r\n";
                break storing;
            }
            if (storeFile.isDirectory()) {
                errString = "451 Can't overwrite a directory\r\n";
                break storing;
            }
            if ((sessionThread.offset >= 0) && (append)) {
                errString = "555 Append can not be used after a REST command\r\n";
                break storing;
            }

            try {
                if (storeFile.exists()) {
                    if (!append && sessionThread.offset < 0) {
                        if (!FtpServerFileUtil.deleteFile(storeFile, App.getAppContext())) {
                            errString = "451 Couldn't truncate file\r\n";
                            break storing;
                        }
                        // Notify other apps that we just deleted a file
                        MediaUpdater.notifyFileDeleted(storeFile.getPath());
                    }
                }
                if (!storeFile.exists()) {
                    FtpServerFileUtil.mkfile(storeFile, App.getAppContext());
                }
                out = FtpServerFileUtil.getOutputStream(storeFile, App.getAppContext());

                //file
                if (out == null) {
                    errString = "451 Couldn't open file \"" + param + "\" aka \""
                            + storeFile.getCanonicalPath() + "\" for writing\r\n";
                    break storing;
                }
                try {
                    if (sessionThread.offset < 0) {
                        out.getChannel().position(storeFile.length());
                    } else {
                        out.getChannel().position(sessionThread.offset);
                        sessionThread.offset = -1;
                    }
                } catch (NullPointerException e) {
                    errString = "451 Couldn't open file \"" + param + "\" aka \""
                            + storeFile.getCanonicalPath() + "\" for writing. Failed to create output stream.\r\n";
                    break storing;
                }

            } catch (FileNotFoundException e) {
                Timber.tag(TAG).d("error : ", e);
                try {
                    errString = "451 Couldn't open file \"" + param + "\" aka \""
                            + storeFile.getCanonicalPath() + "\" for writing\r\n";
                } catch (IOException io_e) {
                    errString = "451 Couldn't open file, nested exception\r\n";
                }
                break storing;
            } catch (IOException e) {
                errString = "451 Unable to seek in file to append\r\n";
                break storing;
            }
            if (!sessionThread.openDataSocket()) {
                errString = "425 Couldn't open data socket\r\n";
                break storing;
            }
            Timber.tag(TAG).d("Data socket ready");
            sessionThread.writeString("150 Data socket ready\r\n");
            byte[] buffer = new byte[SessionThread.DATA_CHUNK_SIZE];

            int numRead;

            Timber.tag(TAG).d("Mode is " + (sessionThread.isBinaryMode() ? "binary" : "ascii"));

            while (true) {
                switch (numRead = sessionThread.receiveFromDataSocket(buffer)) {
                    case -1:
                        Timber.tag(TAG).d("Returned from final read");
                        // We're finished reading
                        break storing;
                    case 0:
                        errString = "426 Couldn't receive data\r\n";
                        break storing;
                    case -2:
                        errString = "425 Could not connect data socket\r\n";
                        break storing;
                    default:
                        try {
                            if (sessionThread.isBinaryMode()) {
                                out.write(buffer, 0, numRead);
                            } else {
                                // ASCII mode, substitute \r\n to \n
                                int startPos = 0, endPos;
                                for (endPos = 0; endPos < numRead; endPos++) {
                                    if (buffer[endPos] == '\r') {
                                        out.write(buffer, startPos, endPos - startPos);
                                        // Our hacky method is to drop all \r
                                        startPos = endPos + 1;
                                    }
                                }
                                // Write last part of buffer as long as there was something
                                // left after handling the last \r
                                if (startPos < numRead) {
                                    out.write(buffer, startPos, endPos - startPos);
                                }
                            }
                        } catch (IOException e) {
                            errString = "451 File IO problem. Device might be full.\r\n";
                            Timber.tag(TAG).d("Exception while storing: " + e);
                            break storing;
                        }
                        break;
                }
            }
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ignored) {
        }

        if (errString != null) {
            Timber.tag(TAG).d("STOR error: " + errString.trim());
            sessionThread.writeString(errString);
        } else {
            sessionThread.writeString("226 Transmission complete\r\n");
            // Notify the music player (and possibly others) that a few file has
            // been uploaded.
            MediaUpdater.notifyFileCreated(storeFile.getPath());
        }
        sessionThread.closeDataSocket();
        Timber.tag(TAG).d("STOR finished");
    }
}
