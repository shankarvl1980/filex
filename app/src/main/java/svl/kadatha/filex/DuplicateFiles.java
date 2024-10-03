package svl.kadatha.filex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DuplicateFiles {
    public static TreeMap<String, List<String>> fillDuplicateFiles(List<String> file_path_list, MessageDigest digest) {
        TreeMap<String, List<String>> fileMap = new TreeMap<>();
        int i = 0;
        for (String file_path : file_path_list) {
            i++;
            File file = new File(file_path);
            long size = file.length();

            String checksum;
            try {
                checksum = getMD5Sum(file, digest);

            } catch (IOException e) {
                return fileMap;
            }
            List<String> map = fileMap.get(checksum);
            if (map == null)
                fileMap.put(checksum, map = new ArrayList<>());
            map.add(file_path);
        }
        return fileMap;
    }

    public static void fillDuplicateFilesByName(String file_path, Map<String, List<String>> duplicate_file_map) {
        String file_name = new File(file_path).getName();
        List<String> map = duplicate_file_map.get(file_name);
        if (map == null) {
            duplicate_file_map.put(file_name, map = new ArrayList<>());
        }
        map.add(file_path);
    }

    private static String getMD5Sum(File file, MessageDigest digest) throws IOException {
        digest.reset();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytes;
            while ((bytes = in.read(buffer)) != -1) {
                digest.update(buffer, 0, bytes);
            }
        }

        byte[] byte_digest = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : byte_digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        String checksum = sb.toString();
        return file.getName() + checksum;
    }
}