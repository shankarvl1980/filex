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

import java.net.ServerSocket;
import java.net.Socket;

import svl.kadatha.filex.ftpserver.ftp.FsService;
import timber.log.Timber;


public class TcpListener extends Thread {
    private static final String TAG = TcpListener.class.getSimpleName();

    final ServerSocket listenSocket;
    final FsService ftpServerService;

    public TcpListener(ServerSocket listenSocket, FsService ftpServerService) {
        this.listenSocket = listenSocket;
        this.ftpServerService = ftpServerService;
    }

    public void quit() {
        try {
            listenSocket.close(); // if the TcpListener thread is blocked on accept,
            // closing the socket will raise an exception
        } catch (Exception e) {
            Timber.tag(TAG).d("Exception closing TcpListener listenSocket");
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket clientSocket = listenSocket.accept();
                Timber.tag(TAG).i("New connection, spawned thread");
                SessionThread newSession = new SessionThread(clientSocket, new LocalDataSocket());
                newSession.start();
                ftpServerService.registerSessionThread(newSession);
            }
        } catch (Exception e) {
            Timber.tag(TAG).d("Exception in TcpListener");
        }
    }
}
