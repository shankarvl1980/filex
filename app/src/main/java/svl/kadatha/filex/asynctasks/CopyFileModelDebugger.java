package svl.kadatha.filex.asynctasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.Global;
import svl.kadatha.filex.filemodel.FileModel;
import timber.log.Timber;

public class CopyFileModelDebugger {

    public static void debugCopyOperation(FileModel sourceFileModel, FileModel destFileModel) {
        Timber.tag("CopyModelDebug").d("Starting debug process");

        // 1. Verify source and destination paths
        String sourcePath = sourceFileModel.getPath();
        String destPath = destFileModel.getPath();
        Timber.tag("CopyModelDebug").d("Source path: %s", sourcePath);
        Timber.tag("CopyModelDebug").d("Destination path: %s", destPath);

        // 2. Check source root path construction
        String sourceRootPath = sourcePath;
        if (!sourceRootPath.endsWith(File.separator)) {
            sourceRootPath += File.separator;
        }
        Timber.tag("CopyModelDebug").d("Corrected source root path: %s", sourceRootPath);

        // 3. Test relative path calculation
        List<FileModel> filesToCopy = new ArrayList<>();
        collectFilesToCopy(sourceFileModel, filesToCopy);

        for (FileModel file : filesToCopy) {
            String filePath = file.getPath();
            String relativePath = getRelativePath(sourceRootPath, filePath);
            String computedDestPath = computeDestinationPath(destPath, relativePath);

            Timber.tag("CopyModelDebug").d("File: %s", filePath);
            Timber.tag("CopyModelDebug").d("Relative path: %s", relativePath);
            Timber.tag("CopyModelDebug").d("Computed destination: %s", computedDestPath);
            Timber.tag("CopyModelDebug").d("Is directory: %s", file.isDirectory());
            Timber.tag("CopyModelDebug").d("------------------------");
        }
    }

    private static void collectFilesToCopy(FileModel source, List<FileModel> filesToCopy) {
        if (source.isDirectory()) {
            filesToCopy.add(source);
            FileModel[] children = source.list();
            if (children != null) {
                for (FileModel child : children) {
                    collectFilesToCopy(child, filesToCopy);
                }
            }
        } else {
            filesToCopy.add(source);
        }
    }

    private static String getRelativePath(String sourceRootPath, String filePath) {
        if (filePath.startsWith(sourceRootPath)) {
            return filePath.substring(sourceRootPath.length());
        }
        return filePath;
    }

    private static String computeDestinationPath(String destRootPath, String relativePath) {
        return Global.CONCATENATE_PARENT_CHILD_PATH(destRootPath, relativePath);
    }
}