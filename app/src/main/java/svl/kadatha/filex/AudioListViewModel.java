package svl.kadatha.filex;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Handler;

public class AudioListViewModel extends AndroidViewModel {

    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3,future4,future5,future6;
    public MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
    public MutableLiveData<Boolean> isAudioFetchingFromAlbumFinished=new MutableLiveData<>();
    public MutableLiveData<Boolean> isSavingAudioFinished=new MutableLiveData<>();
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();

    public List<AudioPOJO> audio_selected_array=new ArrayList<>();
    public List<AudioPOJO> audio_list;

    public List<AlbumPOJO> album_selected_array=new ArrayList<>();
    public List<AlbumPOJO> album_list;

    public List<String> audio_list_selected_array=new ArrayList<>();

    public boolean audio_list_created;
    public List<AudioPOJO> audios_selected_for_delete;


    public AudioListViewModel(@NonNull Application application) {
        super(application);
        this. application=application;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        if(future4!=null) future4.cancel(mayInterruptRunning);
        if(future5!=null) future5.cancel(mayInterruptRunning);
        if(future6!=null) future6.cancel(mayInterruptRunning);
        isCancelled=true;
    }


    public void listAudio()
    {
        if(Boolean.TRUE.equals(isFinished.getValue())) return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                audio_list=new ArrayList<>();
                AudioPlayerActivity.EXISTING_AUDIOS_ID=new ArrayList<>();
                Cursor audio_cursor;
                Cursor cursor=application.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,null,null,null,null);
                if(cursor!=null && cursor.getCount()>0)
                {
                    while(cursor.moveToNext())
                    {
                        if(isCancelled)break;
                        String album_id=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                        String album_path=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                        Bitmap albumart=null;//Global.GET_RESIZED_BITMAP(album_path,Global.IMAGEVIEW_DIMENSION_LARGE_LIST);

                        String where=MediaStore.Audio.Media.ALBUM_ID+"="+album_id;
                        audio_cursor=application.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,where,null,null);
                        if(audio_cursor!=null && audio_cursor.getCount()>0)
                        {
                            while(audio_cursor.moveToNext())
                            {
                                if(isCancelled)break;
                                int id=audio_cursor.getInt(audio_cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                                String data=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                                String title=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                                String album=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                                String artist=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                                String duration=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                                if(new File(data).exists())
                                {
                                    audio_list.add(new AudioPOJO(id,data,title,album,artist,duration,FileObjectType.FILE_TYPE));
                                    AudioPlayerActivity.EXISTING_AUDIOS_ID.add(id);
                                }
                            }

                            audio_cursor.close();
                        }

                    }

                    cursor.close();
                }

                isFinished.postValue(true);
            }
        });
    }


    public synchronized void listAudio(List<AlbumPOJO> album_list, String action, String list_name)
    {
        if(Boolean.TRUE.equals(isAudioFetchingFromAlbumFinished.getValue())) return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {
                audio_list=new ArrayList<>();
                audio_list_created=false;
                AudioDatabaseHelper audioDatabaseHelper = null;
                if(list_name!=null)
                {
                    audioDatabaseHelper=new AudioDatabaseHelper(application);
                }

                for(AlbumPOJO albumPOJO:album_list)
                {
                    String album_id=albumPOJO.getId();
                    Uri uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    String where=MediaStore.Audio.Media.ALBUM_ID+"="+album_id;

                    Cursor cursor=application.getContentResolver().query(uri,null,where,null,null);

                    if(cursor!=null && cursor.getCount()>0)
                    {
                        while(cursor.moveToNext())
                        {
                            int id=cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            String data=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                            String title=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            String album=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                            String artist=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                            String duration=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                            if(new File(data).exists())
                            {
                                audio_list.add(new AudioPOJO(id,data,title,album,artist,duration,FileObjectType.FILE_TYPE));
                            }
                        }
                        cursor.close();

                    }
                }

                if(action!=null)
                {

                    switch(action)
                    {
                        case "q":
                            AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(audio_list);
                            Global.print_background_thread(application,application.getString(R.string.added_audios_current_play_list));
                            break;
                        case "s":
                            if(audioDatabaseHelper!=null)
                            {
                                if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name))
                                {
                                    audioDatabaseHelper.insert(list_name,audio_list);
                                    Global.print_background_thread(application,application.getString(R.string.added_audios_to) + list_name + "'");
                                }
                                else
                                {
                                    audioDatabaseHelper.createTable(list_name);
                                    audioDatabaseHelper.insert(list_name,audio_list);
                                    AudioPlayerActivity.AUDIO_SAVED_LIST.add(list_name);
                                    audio_list_created=true;
                                    Global.print_background_thread(application,"'" + list_name + "' " + application.getString(R.string.audio_list_created));
                                }

                            }

                            break;
                        default:
                            AudioPlayerService.AUDIO_QUEUED_ARRAY=audio_list;
                            Global.print_background_thread(application,application.getString(R.string.added_audios_current_play_list));
                            break;
                    }
                }

                isAudioFetchingFromAlbumFinished.postValue(true);
            }
        });
    }

    public synchronized void save_audio(String action, String list_name)
    {
        if(Boolean.TRUE.equals(isSavingAudioFinished.getValue())) return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {
                AudioDatabaseHelper audioDatabaseHelper = null;
                if(list_name!=null)
                {
                    audioDatabaseHelper=new AudioDatabaseHelper(application);
                }

                if(action!=null)
                {
                    switch(action)
                    {
                        case "q":
                            AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(audio_selected_array);
                            Global.print_background_thread(application,application.getString(R.string.added_audios_current_play_list));
                            break;
                        case "s":
                            if(audioDatabaseHelper!=null)
                            {
                                if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name))
                                {
                                    audioDatabaseHelper.insert(list_name,audio_selected_array);
                                    Global.print_background_thread(application,application.getString(R.string.added_audios_to) + list_name + "'");
                                }
                                else
                                {
                                    audioDatabaseHelper.createTable(list_name);
                                    audioDatabaseHelper.insert(list_name,audio_selected_array);
                                    AudioPlayerActivity.AUDIO_SAVED_LIST.add(list_name);
                                    audio_list_created=true;
                                    Global.print_background_thread(application,"'" + list_name + "' " + application.getString(R.string.audio_list_created));
                                }
                            }

                            break;
                        default:
                            AudioPlayerService.AUDIO_QUEUED_ARRAY=audio_selected_array;
                            Global.print_background_thread(application,application.getString(R.string.added_audios_current_play_list));
                            break;
                    }
                }
                isSavingAudioFinished.postValue(true);
            }
        });

    }

    public synchronized void listAlbum()
    {
        if(Boolean.TRUE.equals(isFinished.getValue())) return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                album_list=new ArrayList<>();
                Cursor cursor=application.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,null,null,null,null);
                if(cursor!=null && cursor.getCount()>0)
                {
                    while(cursor.moveToNext())
                    {
                        if(isCancelled)break;
                        String id=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                        String album_name=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                        String artist=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                        String no_of_songs=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
                        String album_path=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                        album_list.add(new AlbumPOJO(id,album_name,artist,no_of_songs,album_path));
                    }

                    cursor.close();
                }

                isFinished.postValue(true);
            }
        });
    }


    public synchronized void fetch_saved_audio_list(String list_name, boolean whether_saved_play_list)
    {
        if(Boolean.TRUE.equals(isFinished.getValue())) return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future5=executorService.submit(new Runnable() {
            @Override
            public void run() {
               audio_list=new ArrayList<>();
                if(!whether_saved_play_list)
                {
                    for(AudioPOJO audio:AudioPlayerService.AUDIO_QUEUED_ARRAY)
                    {
                        if(new File(audio.getData()).exists())
                        {
                            audio_list.add(audio);
                        }
                    }
                }
                else if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name))
                {
                    AudioDatabaseHelper audioDatabaseHelper=new AudioDatabaseHelper(application);
                    audio_list=audioDatabaseHelper.getAudioList(list_name);
                    Iterator<AudioPOJO> it=audio_list.iterator();
                    while(it.hasNext())
                    {
                        AudioPOJO audio=it.next();
                        if(!new File(audio.getData()).exists())
                        {
                            audioDatabaseHelper.delete(list_name,audio.getId());
                            it.remove();
                        }
                    }
                }

                isFinished.postValue(true);
            }
        });
    }


    public synchronized void fetch_saved_audio_list(List<String> audio_selected_list)
    {
        if(Boolean.TRUE.equals(isFinished.getValue()))return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future6=executorService.submit(new Runnable() {
            @Override
            public void run() {
                audio_list=new ArrayList<>();
                for(String list_name:audio_selected_list)
                {
                    audio_list.addAll(fetch_audio_list(list_name));
                }
                isFinished.postValue(true);
            }
        });
    }


    private List<AudioPOJO> fetch_audio_list(String list_name)
    {
        List<AudioPOJO> clicked_audio_list=new ArrayList<>();
        if(list_name.equals(AudioPlayerActivity.CURRENT_PLAY_LIST))
        {
            clicked_audio_list=AudioPlayerService.AUDIO_QUEUED_ARRAY;
        }

        else if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name))
        {
            AudioDatabaseHelper audioDatabaseHelper=new AudioDatabaseHelper(application);
            clicked_audio_list=audioDatabaseHelper.getAudioList(list_name);
            Iterator<AudioPOJO> it=clicked_audio_list.iterator();
            while(it.hasNext())
            {
                AudioPOJO audio=it.next();
                if(!new File(audio.getData()).exists())
                {
                    audioDatabaseHelper.delete(list_name,audio.getId());
                    it.remove();
                }
            }

        }
        return clicked_audio_list;

    }
}