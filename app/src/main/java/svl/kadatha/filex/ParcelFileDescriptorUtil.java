package svl.kadatha.filex;

import android.os.ParcelFileDescriptor;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

/**
 * ParcelFileDescriptor Utility class.
 * Based on CommonsWare's ParcelFileDescriptorUtil.
 */
/*
public class ParcelFileDescriptorUtil {
    public static ParcelFileDescriptor pipeFrom(InputStream inputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);

        new TransferThread(inputStream, output).start();

        return pipe[0];
    }

    public static ParcelFileDescriptor pipeTo(OutputStream outputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);

        new TransferThread(input, outputStream).start();

        return pipe[1];
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }



        @Override
        public void run() {
            try {
                IOUtils.copy(mIn, mOut);
                mOut.flush();
            } catch (IOException e) {
                Timber.e("TransferThread", "writing failed");
                //e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(mIn);
                //IOUtils.closeQuietly(mOut);
            }
        }
    }
}

 */


public class ParcelFileDescriptorUtil {
    public static ParcelFileDescriptor pipeFrom(InputStream inputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);

        new TransferThread(inputStream, output).start();

        return pipe[0];
    }

    @SuppressWarnings("unused")
    public static ParcelFileDescriptor pipeTo(OutputStream outputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);

        new TransferThread(input, outputStream).start();

        return pipe[1];
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                IOUtils.copy(mIn, mOut);
                mOut.flush();
            } catch (IOException e) {
                Timber.e("TransferThread", "writing failed");
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(mIn);
                IOUtils.closeQuietly(mOut);
            }
        }
    }
}