package svl.kadatha.filex;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class NioFileIterator extends SimpleFileVisitor<Path> {
    private final int[] count;
    private final long[] size;
    MutableLiveData<Integer> mutable_total_no_of_files = new MutableLiveData<>();
    MutableLiveData<String> mutable_size_of_files_formatted = new MutableLiveData<>();

    public NioFileIterator(String file_path, int[] total_no_of_files, long[] total_size_of_files) throws IOException {
        count = total_no_of_files;
        size = total_size_of_files;
        Files.walkFileTree(Paths.get(file_path), this);
    }

    public NioFileIterator(List<String> file_path_list, int[] total_no_of_files, long[] total_size_of_files, MutableLiveData<Integer> mutable_integer, MutableLiveData<String> mutable_string) throws IOException {
        mutable_total_no_of_files = mutable_integer;
        mutable_size_of_files_formatted = mutable_string;
        count = total_no_of_files;
        size = total_size_of_files;
        int s = file_path_list.size();
        for (int i = 0; i < s; ++i) {
            String file_path = file_path_list.get(i);
            Files.walkFileTree(Paths.get(file_path), this);
        }

    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
        ++count[0];
        size[0] += attributes.size();
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
        ++count[0];
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        mutable_total_no_of_files.postValue(count[0]);
        mutable_size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(size[0]));
        return FileVisitResult.CONTINUE;
    }

    public int getCount() {
        return count[0];
    }

    public long getSize() {
        return size[0];
    }
}