package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AudioPlayViewModel extends AndroidViewModel {

    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> isAlbumArtFetched = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public FilePOJO currently_shown_file;
    public FileObjectType fileObjectType;
    public boolean fromThirdPartyApp;
    public boolean fromArchive;
    public String file_path;
    public String album_id;
    public String audio_file_name = "";
    private boolean isCancelled;
    private Future<?> future1, future2;
    public boolean play_screen_expanded_view;

    public AudioPlayViewModel(@NonNull Application application) {
        super(application);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning) {
        if (future1 != null) future1.cancel(mayInterruptRunning);
        if (future2 != null) future2.cancel(mayInterruptRunning);
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }

    public synchronized void albumPolling(String source_folder, FileObjectType fileObjectType, boolean fromThirdPartyApp) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                if (fileObjectType == FileObjectType.FILE_TYPE || fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE || fileObjectType == FileObjectType.ROOT_TYPE) {
                    currently_shown_file = MakeFilePOJOUtil.MAKE_FilePOJO(new File(file_path), false, FileObjectType.FILE_TYPE);
                } else {
                    File cache_file = Global.COPY_TO_CACHE(file_path, fileObjectType);//Global.COPY_TO_FTP_CACHE(file_path);
                    currently_shown_file = MakeFilePOJOUtil.MAKE_FilePOJO(cache_file, false, FileObjectType.FILE_TYPE);
                }

                List<FilePOJO> filePOJOS = new ArrayList<>(), filePOJOS_filtered = new ArrayList<>();
                if (!repositoryClass.hashmap_file_pojo.containsKey(fileObjectType + source_folder)) {
                    FilePOJOUtil.FILL_FILE_POJO(filePOJOS, filePOJOS_filtered, fileObjectType, source_folder, null, false);
                } else {
                    if (MainActivity.SHOW_HIDDEN_FILE) {
                        filePOJOS = repositoryClass.hashmap_file_pojo.get(fileObjectType + source_folder);
                    } else {
                        filePOJOS = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + source_folder);
                    }
                }

                AudioPlayerService.AUDIO_QUEUED_ARRAY = new ArrayList<>();
                AudioPlayerService.CURRENT_PLAY_NUMBER = 0;

                // limiting to the selected only, in case of file selected from usb storage by adding condition below
                if (fromThirdPartyApp || Global.whether_file_cached(fileObjectType)) {
                    AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
                } else {
                    Collections.sort(filePOJOS, FileComparator.FilePOJOComparate(Global.SORT, false));
                    int size = filePOJOS.size();
                    int count = 0;
                    for (int i = 0; i < size; ++i) {
                        FilePOJO filePOJO = filePOJOS.get(i);
                        if (!filePOJO.getIsDirectory()) {
                            String file_ext;
                            int idx = filePOJO.getName().lastIndexOf(".");
                            if (idx != -1) {
                                file_ext = filePOJO.getName().substring(idx + 1);
                                if (file_ext.matches(Global.AUDIO_REGEX)) {
                                    AudioPOJO audio = new AudioPOJO(0, filePOJO.getPath(), filePOJO.getName(), null, null, null, "0", fileObjectType);
                                    AudioPlayerService.AUDIO_QUEUED_ARRAY.add(audio);

                                    if (AudioPlayerActivity.AUDIO_FILE.getTitle().equals(filePOJO.getName()))
                                        AudioPlayerService.CURRENT_PLAY_NUMBER = count;
                                    count++;
                                } else if (filePOJO.getName().equals(AudioPlayerActivity.AUDIO_FILE.getTitle())) {
                                    AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
                                    AudioPlayerService.CURRENT_PLAY_NUMBER = count;
                                    count++;
                                }
                            } else if (filePOJO.getName().equals(AudioPlayerActivity.AUDIO_FILE.getTitle())) {

                                AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
                                AudioPlayerService.CURRENT_PLAY_NUMBER = count;
                                count++;
                            }
                        }
                    }
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public void fetchAlbumArt(int audio_id, String audiofilename, String audiofilepath) {
        if (isAlbumArtFetched.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        this.audio_file_name = audiofilename;
        isAlbumArtFetched.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (audio_id == 0) {
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                    if (repositoryClass.audio_pojo_hashmap.containsKey("audio")) {
                        List<AudioPOJO> temp_audio_pojos = repositoryClass.audio_pojo_hashmap.get("audio");
                        Iterator<AudioPOJO> iterator = temp_audio_pojos.iterator();
                        while (iterator.hasNext()) {
                            AudioPOJO audioPOJO = iterator.next();
                            if (audioPOJO.getData().equals(audiofilepath)) {
                                album_id = audioPOJO.getAlbumId();
                            }
                        }
                    }
                } else {
                    album_id = AudioPlayerActivity.EXISTING_AUDIOS_ID.get(audio_id);
                }
                isAlbumArtFetched.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}
