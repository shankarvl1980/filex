package svl.kadatha.filex.filemodel;

import java.io.InputStream;

public interface StreamUploadFileModel {
    /**
     * Uploads a child file into this directory using a streaming upload path.
     *
     * @param childName             file name under this folder
     * @param in                    source stream (caller may pass BufferedInputStream)
     * @param contentLengthOrMinus1 length in bytes, or -1 if unknown
     * @param bytesRead             optional long[1] progress sink
     */
    boolean putChildFromStream(String childName,
                               InputStream in,
                               long contentLengthOrMinus1,
                               long[] bytesRead);
}
