package svl.kadatha.filex.asynctasks;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import svl.kadatha.filex.AlternativeAsyncTask;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.FilePOJOUtil;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.Iterate;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import svl.kadatha.filex.filemodel.FtpFileModel;

// ArchiveAsyncTask.java
public class ArchiveAsyncTask extends AlternativeAsyncTask<Void, Void, Boolean> {
    public static final String TASK_TYPE = "archive-zip";
    private final ArrayList<String> files_selected_array;
    private final Uri source_uri;
    private final String source_uri_path;
    private final TaskProgressListener listener;
    private final String dest_folder;
    private final String zip_file_path;
    private final String zip_file_name;
    private final FileObjectType destFileObjectType;
    private final FileObjectType sourceFileObjectType;
    private int counter_no_files;
    private long counter_size_files;
    private String copied_file_name;
    private FilePOJO filePOJO;


    public ArchiveAsyncTask(ArrayList<String> files_selected_array, String zip_file_name, String dest_folder, String zip_file_path, FileObjectType destFileObjectType, FileObjectType sourceFileObjectType, Uri source_uri, String source_uri_path, TaskProgressListener listener) {
        this.files_selected_array = files_selected_array;
        this.zip_file_name = zip_file_name;
        this.source_uri = source_uri;
        this.source_uri_path = source_uri_path;
        this.listener = listener;
        this.dest_folder = dest_folder;
        this.zip_file_path = zip_file_path;
        this.destFileObjectType = destFileObjectType;
        this.sourceFileObjectType = sourceFileObjectType;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        OutputStream outStream;
        FileModel destFileModel = FileModelFactory.getFileModel(dest_folder, destFileObjectType, source_uri, source_uri_path);
        outStream = destFileModel.getChildOutputStream(zip_file_name, 0);

        if (outStream != null) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
            ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);

            if (sourceFileObjectType == FileObjectType.FILE_TYPE || sourceFileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                int size = files_selected_array.size();
                for (int i = 0; i < size; ++i) {
                    if (isCancelled()) {
                        return false;
                    }
                    String path = files_selected_array.get(i);
                    List<File> file_array = new ArrayList<>();
                    Iterate.populate(new File(path), file_array, false);
                    try {
                        put_zip_entry_file_type(path, file_array, zipOutputStream);
                    } catch (IOException e) {
                        return false;
                    }
                }

                try {
                    filePOJO = FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, Collections.singletonList(zip_file_name), destFileObjectType, Collections.singletonList(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, zip_file_name)));
                    return true;
                } catch (Exception ignored) {
                } finally {
                    try {
                        zipOutputStream.closeEntry();
                        zipOutputStream.close();
                        if (outStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
                            ((FtpFileModel.FTPOutputStreamWrapper) outStream).completePendingCommand();
                        }
                    } catch (Exception e) {
                        // ignore exception
                    }
                }
            } else {
                List<FileModel> fileModels = new ArrayList<>();
                FileModel[] sourceFileModels = FileModelFactory.getFileModelArray(files_selected_array, sourceFileObjectType, source_uri, source_uri_path);
                Iterate.populate(sourceFileModels, fileModels, false);

                int lengthParentPath = 0;
                try {
                    if (!zip_file_path.isEmpty()) {
                        lengthParentPath = new File(zip_file_path).getParent().length();

                    }
                    int size1 = fileModels.size();
                    for (int i = 0; i < size1; ++i) {
                        if (isCancelled()) {
                            return false;
                        }
                        FileModel fileModel = fileModels.get(i);
                        counter_no_files++;
                        copied_file_name = fileModel.getName();
                        publishProgress(null);
                        String zip_entry_path;
                        if (lengthParentPath == 1) {
                            zip_entry_path = fileModel.getPath().substring(lengthParentPath);
                        } else {
                            zip_entry_path = (lengthParentPath != 0) ? fileModel.getPath().substring(lengthParentPath + 1) : fileModel.getPath().substring(fileModel.getParentPath().length() + 1);
                        }

                        ZipEntry zipEntry;

                        if (fileModel.isDirectory()) {
                            zipEntry = new ZipEntry(zip_entry_path + File.separator);
                            zipOutputStream.putNextEntry(zipEntry);
                        } else {
                            zipEntry = new ZipEntry(zip_entry_path);
                            zipOutputStream.putNextEntry(zipEntry);
                            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileModel.getInputStream())) {
                                byte[] b = new byte[8192];
                                int bytesread;
                                while ((bytesread = bufferedInputStream.read(b)) != -1) {
                                    zipOutputStream.write(b, 0, bytesread);
                                    counter_size_files += bytesread;
                                    publishProgress(null);
                                }
                            }
                        }
                    }

                    filePOJO = FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, Collections.singletonList(zip_file_name), destFileObjectType, Collections.singletonList(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, zip_file_name)));
                    return true;
                } catch (Exception e) {
                    return false;
                } finally {
                    try {
                        zipOutputStream.closeEntry();
                        zipOutputStream.close();
                        if (outStream instanceof FtpFileModel.FTPOutputStreamWrapper) {
                            ((FtpFileModel.FTPOutputStreamWrapper) outStream).completePendingCommand();
                        }
                    } catch (Exception e) {
                        // ignore exception
                    }
                }
            }
        }
        return false;
    }


    @Override
    protected void onProgressUpdate(Void value) {
        super.onProgressUpdate(value);
        if (listener != null) {
            listener.onProgressUpdate(TASK_TYPE, counter_no_files, counter_size_files, zip_file_name, copied_file_name);
        }
    }


    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (listener != null) {
            listener.onTaskCompleted(TASK_TYPE, result, filePOJO);
        }
    }


    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        if (listener != null) {
            listener.onTaskCancelled(TASK_TYPE, filePOJO);
        }
    }


    private void put_zip_entry_file_type(String file_path, List<File> file_array, ZipOutputStream zipOutputStream) throws IOException {
        int lengthParentPath = 0;
        if (!file_path.isEmpty()) {
            lengthParentPath = new File(file_path).getParent().length(); //should be calculated for each file separately in library_search

        }
        int size1 = file_array.size();
        for (int i = 0; i < size1; ++i) {
            if (isCancelled()) {
                return;
            }
            File file = file_array.get(i);
            counter_no_files++;
            copied_file_name = file.getName();
            publishProgress(null);
            String zip_entry_path;
            if (lengthParentPath == 1) {
                zip_entry_path = file.getCanonicalPath().substring(lengthParentPath);
            } else {
                zip_entry_path = (lengthParentPath != 0) ? file.getCanonicalPath().substring(lengthParentPath + 1) : file.getCanonicalPath().substring(file.getParentFile().getCanonicalPath().length() + 1);
            }

            ZipEntry zipEntry;
            if (file.isDirectory()) {
                zipEntry = new ZipEntry(zip_entry_path + "/");
                zipOutputStream.putNextEntry(zipEntry);
            } else {
                zipEntry = new ZipEntry(zip_entry_path);
                zipOutputStream.putNextEntry(zipEntry);
                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] b = new byte[8192];
                    int bytesread;
                    while ((bytesread = bufferedInputStream.read(b)) != -1) {
                        zipOutputStream.write(b, 0, bytesread);
                        counter_size_files += bytesread;
                        publishProgress(null);
                    }
                }
            }
        }
    }
}

