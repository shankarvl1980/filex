package svl.kadatha.filex.asynctasks;

import svl.kadatha.filex.FilePOJO;

// TaskProgressListener.java
public interface TaskProgressListener {
    void onProgressUpdate(String taskType, int filesProcessed, long totalBytesProcessed, String currentFileName, String delete_copied_file_name);

    void onTaskCompleted(String taskType, boolean success, FilePOJO filePOJO);

    void onTaskCancelled(String taskType, FilePOJO filePOJO);

}