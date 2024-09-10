package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

public class FileSaveHelper {

    private static final String TAG = "FileSaveHelper";

    public static class SaveResult {
        public boolean success;
        public LinkedHashMap<Integer, FileEditorViewModel.PagePointer> pagePointerHashmap;
        public String errorMessage;

        public SaveResult(boolean success, LinkedHashMap<Integer, FileEditorViewModel.PagePointer> pagePointerHashmap, String errorMessage) {
            this.success = success;
            this.pagePointerHashmap = pagePointerHashmap;
            this.errorMessage = errorMessage;
        }
    }

    public static SaveResult saveFile(Context context, Bundle bundle) {
        if (bundle == null) {
            return new SaveResult(false, new LinkedHashMap<>(), "Bundle is null");
        }

        String filePath = bundle.getString("file_path");
        String tempFilePath = bundle.getString("temp_file_path");
        int eol = bundle.getInt("eol");
        int alteredEol = bundle.getInt("altered_eol");
        int currentPage = bundle.getInt("current_page");
        boolean isWritable = bundle.getBoolean("isWritable");
        Uri treeUri = bundle.getParcelable("tree_uri");
        String treeUriPath = bundle.getString("tree_uri_path");

        LinkedHashMap<Integer, FileEditorViewModel.PagePointer> pagePointerHashmap = getPagePointerHashmap(bundle);

        FileEditorViewModel.PagePointer currentPagePointer = pagePointerHashmap.get(currentPage);
        FileEditorViewModel.PagePointer prevPagePointer = pagePointerHashmap.get(currentPage - 1);

        if (currentPagePointer == null) {
            return new SaveResult(false, pagePointerHashmap, "Current page pointer is null");
        }

        long prevPageEndPoint = (prevPagePointer != null) ? prevPagePointer.getEndPoint() : 0L;
        long currentPageEndPoint = currentPagePointer.getEndPoint();

        File file = new File(filePath);
        File modifiedChunkFile = new File(tempFilePath);
        File intermediaryTempFile = new File(context.getCacheDir(), "temp_" + file.getName());

        if (!file.exists()) {
            Timber.tag(TAG).e("File does not exist: %s", filePath);
            return new SaveResult(false, pagePointerHashmap, "File does not exist");
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // Open input stream for the original file
            if (isWritable) {
                inputStream = new FileInputStream(file);
            } else {
                Uri documentUri = FileUtil.getDocumentUri(filePath, treeUri, treeUriPath);
                inputStream = context.getContentResolver().openInputStream(documentUri);
            }

            if (inputStream == null) {
                return new SaveResult(false, pagePointerHashmap, "Failed to open input stream");
            }

            // Use the intermediary temp file for writing
            outputStream = new FileOutputStream(intermediaryTempFile);

            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                 BufferedReader contentReader = new BufferedReader(new FileReader(modifiedChunkFile))) {

                // Copy content before the edited chunk
                copyBytes(bufferedInputStream, bufferedOutputStream, prevPageEndPoint);

                // Write new content
                String line;
                while ((line = contentReader.readLine()) != null) {
                    byte[] lineBytes = convertLineToBytes(line, eol, alteredEol);
                    bufferedOutputStream.write(lineBytes);
                }

                // Skip the old content in the original file
                long bytesToSkip = currentPageEndPoint - prevPageEndPoint;
                long actualSkipped = 0;
                while (actualSkipped < bytesToSkip) {
                    long skipped = bufferedInputStream.skip(bytesToSkip - actualSkipped);
                    if (skipped == 0) {
                        int byteRead = bufferedInputStream.read();
                        if (byteRead == -1) {
                            // End of stream reached
                            break;
                        }
                        actualSkipped++;
                    } else {
                        actualSkipped += skipped;
                    }
                }

                if (actualSkipped < bytesToSkip) {
                    Timber.tag(TAG).w("Skipped fewer bytes than expected: %d/%d", actualSkipped, bytesToSkip);
                }

                // Copy remaining content after the edited chunk
                copyBytes(bufferedInputStream, bufferedOutputStream, Long.MAX_VALUE);
            }

            // Close streams before copying the intermediary file to the final destination
            inputStream.close();
            outputStream.close();
            inputStream = null;
            outputStream = null;

            // Now copy the intermediary file to the final destination
            if (isWritable) {
                copyFile(intermediaryTempFile, file);
            } else {
                Uri documentUri = FileUtil.getDocumentUri(filePath, treeUri, treeUriPath);
                outputStream = context.getContentResolver().openOutputStream(documentUri, "wt");
                if (outputStream == null) {
                    throw new IOException("Failed to open output stream for SAF file");
                }
                copyFile(intermediaryTempFile, outputStream);
            }

            // Update page pointers
            updatePagePointers(pagePointerHashmap, currentPage);

            Timber.tag(TAG).d("File saved successfully");
            return new SaveResult(true, pagePointerHashmap, null);

        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Error saving file: %s", e.getMessage());
            return new SaveResult(false, pagePointerHashmap, "Error saving file: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Error closing streams");
            }
            deleteTempFile(modifiedChunkFile);
            deleteTempFile(intermediaryTempFile);
        }
    }
    private static void copyFile(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private static void copyFile(File source, OutputStream os) throws IOException {
        try (InputStream is = new FileInputStream(source)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private static void copyBytes(InputStream in, OutputStream out, long bytesToCopy) throws IOException {
        byte[] buffer = new byte[8192];
        long totalBytesCopied = 0;
        int bytesRead;

        while (totalBytesCopied < bytesToCopy) {
            int bytesToRead = (int) Math.min(buffer.length, bytesToCopy - totalBytesCopied);
            bytesRead = in.read(buffer, 0, bytesToRead);

            if (bytesRead == -1) {
                break;
            }

            out.write(buffer, 0, bytesRead);
            totalBytesCopied += bytesRead;
        }

        if (totalBytesCopied < bytesToCopy && bytesToCopy != Long.MAX_VALUE) {
            Timber.tag(TAG).w("Copied fewer bytes than requested: %d/%d", totalBytesCopied, bytesToCopy);
        }
    }

    private static void deleteTempFile(File file) {
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                Timber.tag(TAG).w("Failed to delete temp file: %s", file.getPath());
            } else {
                Timber.tag(TAG).d("Temp file deleted successfully: %s", file.getPath());
            }
        }
    }


    private static LinkedHashMap<Integer, FileEditorViewModel.PagePointer> getPagePointerHashmap(Bundle bundle) {
        LinkedHashMap<Integer, FileEditorViewModel.PagePointer> pagePointerHashmap = new LinkedHashMap<>();
        try {
            Object obj = bundle.getSerializable("page_pointer_hashmap");
            if (obj instanceof HashMap) {
                @SuppressWarnings("unchecked")
                HashMap<Integer, FileEditorViewModel.PagePointer> hashMap = (HashMap<Integer, FileEditorViewModel.PagePointer>) obj;
                pagePointerHashmap = new LinkedHashMap<>(hashMap);
            }
        } catch (Exception e) {
            // Handle or ignore exception as per your error handling strategy
        }
        return pagePointerHashmap;
    }

    private static byte[] convertLineToBytes(String line, int eol, int alteredEol) {
        String eolString = getEolString(alteredEol);
        return (line + eolString).getBytes(StandardCharsets.UTF_8);
    }

    private static String getEolString(int alteredEol) {
        switch (alteredEol) {
            case FileEditorActivity.EOL_N:
                return "\n";
            case FileEditorActivity.EOL_R:
                return "\r";
            case FileEditorActivity.EOL_RN:
                return "\r\n";
            default:
                return "\n";
        }
    }

    private static void updatePagePointers(LinkedHashMap<Integer, FileEditorViewModel.PagePointer> pagePointerHashmap,
                                           int currentPage) {
        Iterator<Map.Entry<Integer, FileEditorViewModel.PagePointer>> iterator = pagePointerHashmap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, FileEditorViewModel.PagePointer> entry = iterator.next();
            if (entry.getKey() > currentPage) {
                iterator.remove();
            }
        }

        // Log the updated hashmap
        Timber.tag(Global.TAG).d("Updated page pointers. Current page: %d, Total pages: %d", currentPage, pagePointerHashmap.size());
    }

}