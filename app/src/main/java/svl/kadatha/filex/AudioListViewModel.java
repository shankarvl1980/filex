package svl.kadatha.filex;

import android.app.Application;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AudioListViewModel extends AndroidViewModel {

    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> isAudioFetchingFromAlbumFinished = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> isSavingAudioFinished = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private final Application application;
    private final Object audio_pojo_lock = new Object();
    private final Object album_pojo_lock = new Object();
    public IndexedLinkedHashMap<Integer, AudioPOJO> audio_pojo_selected_items = new IndexedLinkedHashMap<>();
    //public List<AudioPOJO> audio_selected_array=new ArrayList<>();
    public List<AudioPOJO> audio_list;
    public List<Long> audio_rowid_list = new ArrayList<>();
    public List<Long> selected_audio_rowid_list = new ArrayList<>();


    public IndexedLinkedHashMap<Integer, AlbumPOJO> album_pojo_selected_items = new IndexedLinkedHashMap<>();
    //public List<AlbumPOJO> album_selected_array=new ArrayList<>();
    public List<AlbumPOJO> album_list;

    public IndexedLinkedHashMap<Integer, String> audio_saved_list_selected_items = new IndexedLinkedHashMap<>();
    //public List<String> audio_list_selected_array=new ArrayList<>();

    public boolean audio_list_created;
    public List<AudioPOJO> audios_selected_for_delete;
    public boolean whether_audios_set_to_current_list;

    public String action = "p";
    private boolean isCancelled;
    private Future<?> future1, future2, future3, future4, future5, future6;


    public AudioListViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning) {
        if (future1 != null) future1.cancel(mayInterruptRunning);
        if (future2 != null) future2.cancel(mayInterruptRunning);
        if (future3 != null) future3.cancel(mayInterruptRunning);
        if (future4 != null) future4.cancel(mayInterruptRunning);
        if (future5 != null) future5.cancel(mayInterruptRunning);
        if (future6 != null) future6.cancel(mayInterruptRunning);
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }


    public void listAudio() {
        synchronized (audio_pojo_lock) {
            if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
            asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
            ExecutorService executorService = MyExecutorService.getExecutorService();
            future1 = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                    audio_list = new ArrayList<>();
                    repositoryClass.getAudioPOJOList(false);
                    List<AudioPOJO> temp_audio_pojos = repositoryClass.audio_pojo_hashmap.get("audio");
                    if (temp_audio_pojos != null) audio_list = temp_audio_pojos;
                    asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                }
            });
        }
    }


    public synchronized void listAudio(List<AlbumPOJO> album_list, String action, String list_name) {
        if (isAudioFetchingFromAlbumFinished.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        isAudioFetchingFromAlbumFinished.setValue(AsyncTaskStatus.STARTED);
        this.action = action;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                audio_list = new ArrayList<>();
                audio_list_created = false;
                AudioDatabaseHelper audioDatabaseHelper = null;
                if (list_name != null) {
                    audioDatabaseHelper = new AudioDatabaseHelper(application);
                }

                for (AlbumPOJO albumPOJO : album_list) {
                    String album_id = albumPOJO.getId();
                    Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    String where = MediaStore.Audio.Media.ALBUM_ID + "=" + album_id;

                    Cursor cursor = application.getContentResolver().query(uri, null, where, null, null);

                    if (cursor != null && cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                            String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                            if (new File(data).exists()) {
                                audio_list.add(new AudioPOJO(id, data, title, album_id, album, artist, duration, FileObjectType.FILE_TYPE));
                            }
                        }
                        cursor.close();

                    }
                }

                if (action != null) {
                    switch (action) {
                        case "q":
                            AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(audio_list);
                            Global.print_background_thread(application, application.getString(R.string.added_audios_current_play_list));
                            break;
                        case "s":
                            if (audioDatabaseHelper != null) {
                                if (AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name)) {
                                    audioDatabaseHelper.insert(list_name, audio_list);
                                    Global.print_background_thread(application, application.getString(R.string.added_audios_to) + " '" + list_name + "'");
                                } else {
                                    audioDatabaseHelper.createTable(list_name);
                                    audioDatabaseHelper.insert(list_name, audio_list);
                                    AudioPlayerActivity.AUDIO_SAVED_LIST.add(list_name);
                                    audio_list_created = true;
                                    Global.print_background_thread(application, "'" + list_name + "' " + application.getString(R.string.audio_list_created));
                                }

                            }

                            break;
                        default:
                            AudioPlayerService.AUDIO_QUEUED_ARRAY = audio_list;
                            Global.print_background_thread(application, application.getString(R.string.added_audios_current_play_list));
                            break;
                    }
                }

                isAudioFetchingFromAlbumFinished.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void save_audio(String action, String list_name) {
        if (isSavingAudioFinished.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        isSavingAudioFinished.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future3 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                AudioDatabaseHelper audioDatabaseHelper = null;
                if (list_name != null) {
                    audioDatabaseHelper = new AudioDatabaseHelper(application);
                }

                if (action != null) {
                    switch (action) {
                        case "q":
                            AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(audio_pojo_selected_items.values());
                            Global.print_background_thread(application, application.getString(R.string.added_audios_current_play_list));
                            break;
                        case "s":
                            if (audioDatabaseHelper != null) {
                                ArrayList<AudioPOJO> to_be_saved_list = new ArrayList<>(audio_pojo_selected_items.values());
                                if (AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name)) {
                                    audioDatabaseHelper.insert(list_name, to_be_saved_list);
                                    Global.print_background_thread(application, application.getString(R.string.added_audios_to) + " '" + list_name + "'");
                                } else {
                                    audioDatabaseHelper.createTable(list_name);
                                    audioDatabaseHelper.insert(list_name, to_be_saved_list);
                                    AudioPlayerActivity.AUDIO_SAVED_LIST.add(list_name);
                                    audio_list_created = true;
                                    Global.print_background_thread(application, "'" + list_name + "' " + application.getString(R.string.audio_list_created));
                                }
                            }

                            break;
                        default:
                            AudioPlayerService.AUDIO_QUEUED_ARRAY = new ArrayList<>(audio_pojo_selected_items.values());
                            Global.print_background_thread(application, application.getString(R.string.added_audios_current_play_list));
                            break;
                    }
                }
                isSavingAudioFinished.postValue(AsyncTaskStatus.COMPLETED);
            }
        });

    }

    public void listAlbum() {
        synchronized (album_pojo_lock) {
            if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
            asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
            ExecutorService executorService = MyExecutorService.getExecutorService();
            future4 = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                    album_list = new ArrayList<>();
                    repositoryClass.getAlbumList(false);
                    List<AlbumPOJO> temp_album_pojos = repositoryClass.album_pojo_hashmap.get("album");
                    if (temp_album_pojos != null) album_list = temp_album_pojos;
                    asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                }
            });
        }

    }


    public synchronized void fetch_saved_audio_list(String list_name, boolean whether_saved_play_list) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future5 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                audio_list = new ArrayList<>();
                if (!whether_saved_play_list) {
                    for (AudioPOJO audio : AudioPlayerService.AUDIO_QUEUED_ARRAY) {
                        if (new File(audio.getData()).exists()) {
                            audio_list.add(audio);
                        }
                    }
                } else if (AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name)) {
                    AudioDatabaseHelper audioDatabaseHelper = new AudioDatabaseHelper(application);
                    IndexedLinkedHashMap<Long, AudioPOJO> audio_indexed_hashmap = audioDatabaseHelper.getAudioList(list_name);
                    audio_list = new ArrayList<>(audio_indexed_hashmap.values());
                    audio_rowid_list = new ArrayList<>(audio_indexed_hashmap.keySet());
                    Iterator<AudioPOJO> it = audio_list.iterator();
                    while (it.hasNext()) {
                        AudioPOJO audio = it.next();
                        if (!new File(audio.getData()).exists()) {
                            int removed_row = audioDatabaseHelper.delete_by_audio_id(list_name, audio.getId());
                            audio_rowid_list.remove(removed_row);
                            it.remove();
                        }
                    }
                }

                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    public synchronized void fetch_saved_audio_list(List<String> audio_selected_list) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future6 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                audio_list = new ArrayList<>();
                for (String list_name : audio_selected_list) {
                    audio_list.addAll(fetch_audio_list(list_name));
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    private List<AudioPOJO> fetch_audio_list(String list_name) {
        List<AudioPOJO> clicked_audio_list = new ArrayList<>();
        if (list_name.equals(AudioPlayerActivity.CURRENT_PLAY_LIST)) {
            clicked_audio_list = AudioPlayerService.AUDIO_QUEUED_ARRAY;
        } else if (AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name)) {
            AudioDatabaseHelper audioDatabaseHelper = new AudioDatabaseHelper(application);
            IndexedLinkedHashMap<Long, AudioPOJO> audio_indexed_hashmap = audioDatabaseHelper.getAudioList(list_name);
            clicked_audio_list = new ArrayList<>(audio_indexed_hashmap.values());
            audio_rowid_list = new ArrayList<>(audio_indexed_hashmap.keySet());

            Iterator<AudioPOJO> it = clicked_audio_list.iterator();
            while (it.hasNext()) {
                AudioPOJO audio = it.next();
                if (!new File(audio.getData()).exists()) {
                    int removed_row = audioDatabaseHelper.delete_by_audio_id(list_name, audio.getId());
                    audio_rowid_list.remove(removed_row);
                    it.remove();
                }
            }

        }
        return clicked_audio_list;

    }
}
