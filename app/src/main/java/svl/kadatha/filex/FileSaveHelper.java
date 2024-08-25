package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class FileSaveHelper {

    public static class SaveResult {
        public boolean success;
        public LinkedHashMap<Integer, Long> pagePointerHashmap;

        public SaveResult(boolean success, LinkedHashMap<Integer, Long> pagePointerHashmap) {
            this.success = success;
            this.pagePointerHashmap = pagePointerHashmap;
        }
    }

    public static SaveResult saveFile(Context context, Bundle bundle) {
        if (bundle == null) {
            return new SaveResult(false, new LinkedHashMap<>());
        }

        boolean isWritable = bundle.getBoolean("isWritable");
        File file = new File(bundle.getString("file_path"));
        String content = bundle.getString("content");
        String treeUriPath = bundle.getString("tree_uri_path");
        Uri treeUri = bundle.getParcelable("tree_uri");
        int eol = bundle.getInt("eol");
        int alteredEol = bundle.getInt("altered_eol");
        long prevPageEndPoint = bundle.getLong("prev_page_end_point");
        long currentPageEndPoint = bundle.getLong("current_page_end_point");
        HashMap<Integer, Long> temp = (HashMap<Integer, Long>) bundle.getSerializable("page_pointer_hashmap");
        LinkedHashMap<Integer, Long> pagePointerHashmap = new LinkedHashMap<>(temp);
        File temporaryFileForSave = new File(bundle.getString("temporary_file_path"));
        int currentPage = bundle.getInt("current_page");

        String eolString = getEolString(alteredEol);

        if (file == null || !file.exists()) {
            return new SaveResult(false, pagePointerHashmap);
        }

        if (!eolString.equals("\n")) {
            content = content.replaceAll("\n", eolString);
        }

        boolean result;
        FileOutputStream fileOutputStream;
        if (isWritable) {
            if (eol == alteredEol) {
                result = saveFile(null, prevPageEndPoint, currentPageEndPoint, content.getBytes(), file, temporaryFileForSave);
            } else {
                result = saveFileWithAlteredEol(null, prevPageEndPoint, currentPageEndPoint, content, eolString, file, temporaryFileForSave);
            }
        } else {
            fileOutputStream = FileUtil.get_file_outputstream(context, file.getAbsolutePath(), treeUri, treeUriPath);
            if (fileOutputStream != null) {
                if (eol == alteredEol) {
                    result = saveFile(fileOutputStream, prevPageEndPoint, currentPageEndPoint, content.getBytes(), file, temporaryFileForSave);
                } else {
                    result = saveFileWithAlteredEol(fileOutputStream, prevPageEndPoint, currentPageEndPoint, content, eolString, file, temporaryFileForSave);
                }
            } else {
                result = false;
            }
        }

        if (result) {
            pagePointerHashmap.put(currentPage, currentPageEndPoint);
        }

        return new SaveResult(result, pagePointerHashmap);
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

    private static boolean saveFile(FileOutputStream fileOutputStream, long prevPageEndPoint, long currentPageEndPoint, byte[] content, File file, File temporaryFileForSave) {
        FileChannel sourceFC = null, tempFC = null, rFC = null;
        try {
            long length = file.length();

            RandomAccessFile rRaf = new RandomAccessFile(file, "r");
            File tempFile = new File(temporaryFileForSave, file.getName());
            RandomAccessFile tempRaf = new RandomAccessFile(tempFile, "rw");

            rFC = rRaf.getChannel();
            tempFC = tempRaf.getChannel();

            if (length == currentPageEndPoint) {
                rFC.transferTo(prevPageEndPoint, 0, tempFC);
            } else {
                rFC.transferTo(currentPageEndPoint, length - currentPageEndPoint, tempFC);
            }
            rRaf.close();

            if (fileOutputStream == null) {
                sourceFC = new FileOutputStream(file, true).getChannel();
            } else {
                sourceFC = fileOutputStream.getChannel();
            }

            sourceFC.truncate(prevPageEndPoint);
            sourceFC.position(prevPageEndPoint);
            ByteBuffer buf = ByteBuffer.wrap(content);
            long writtenBytes = sourceFC.write(buf);

            buf.compact();
            buf.flip();
            if (buf.hasRemaining()) {
                writtenBytes += sourceFC.write(buf);
            }
            long newOffset = sourceFC.position();

            tempFC.position(0L);
            sourceFC.transferFrom(tempFC, newOffset, tempFC.size());
            currentPageEndPoint = newOffset;

            tempFC.close();

            if (tempFile.exists()) {
                tempFile.delete();
            }

            return true;

        } catch (IOException | NullPointerException | IllegalArgumentException e) {
            return false;
        } finally {
            try {
                if (tempFC != null) tempFC.close();
                if (rFC != null) rFC.close();
                if (fileOutputStream != null) fileOutputStream.close();
                if (sourceFC != null) sourceFC.close();
            } catch (IOException | NullPointerException e) {
                // Handle or log the exception
            }
        }
    }

    private static boolean saveFileWithAlteredEol(FileOutputStream fileOutputStream, long prevPageEndPoint, long currentPageEndPoint, String content, String eolString, File file, File temporaryFileForSave) {
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        FileChannel fc = null;

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));

            bufferedReader.skip(currentPageEndPoint);
            File tempFile2 = new File(temporaryFileForSave, file.getName() + "_2");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile2, true), StandardCharsets.UTF_8));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line + eolString);
                bufferedWriter.flush();
            }

            bufferedWriter.close();
            bufferedReader.close();

            if (fileOutputStream == null) {
                fc = new FileOutputStream(file, true).getChannel();
            } else {
                fc = fileOutputStream.getChannel();
            }

            fc.truncate(prevPageEndPoint);

            fileInputStream = new FileInputStream(file);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));

            File tempFile1 = new File(temporaryFileForSave, file.getName() + "_1");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile1, true), StandardCharsets.UTF_8));
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line + eolString);
                bufferedWriter.flush();
            }

            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();

            bufferedReader.close();

            fc.truncate(0L);

            currentPageEndPoint = tempFile1.length();
            FileChannel firstPartFC = new FileInputStream(tempFile1).getChannel();
            fc.transferFrom(firstPartFC, 0, currentPageEndPoint);

            FileChannel secondPartFC = new FileInputStream(tempFile2).getChannel();
            fc.transferFrom(secondPartFC, currentPageEndPoint, tempFile2.length());

            firstPartFC.close();
            secondPartFC.close();

            tempFile1.delete();
            tempFile2.delete();

            return true;

        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (bufferedWriter != null) bufferedWriter.close();
                if (bufferedReader != null) bufferedReader.close();
                if (fc != null) fc.close();
                if (fileOutputStream != null) fileOutputStream.close();
            } catch (IOException e) {
                // Handle or log the exception
            }
        }
    }
}