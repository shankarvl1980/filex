package svl.kadatha.filex;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileStreamFactory;

public class AudioPlayFragment extends Fragment {
    private static final String DELETE_FILE_REQUEST_CODE = "audio_play_file_delete_request_code";
    private static final String AUDIO_SELECT_REQUEST_CODE = "audio_play_audio_select_request_code";
    public AudioPlayerService audio_player_service;
    private ImageView album_art_imageview;
    private ImageButton previous_btn;
    private ImageButton play_pause_btn;
    private ImageButton next_btn;
    private TextView audio_name_tv, audio_album_tv, audio_artists_tv, next_audio_tv, total_time_tv, current_progress_tv;
    private SeekBar seekbar;
    private int total_duration;
    private Handler handler, onserviceconnection_handler, handler_for_art;
    private ServiceConnection service_connection;
    private boolean service_bound;
    private Context context;
    private final ActivityResultLauncher<Intent> activityResultLauncher_write_settings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                set_ring_tone();
            } else {
                Global.print(context, getString(R.string.permission_not_granted));
            }
        }
    });
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    private List<AudioPOJO> files_selected_for_delete;
    private boolean isDurationMoreThanHour;
    private Uri data;
    private AudioManager audioManager;
    private FrameLayout progress_bar;
    private AudioPlayViewModel audioPlayViewModel;
    private AudioSelectListener audioSelectListener;
    private AppCompatActivity activity;
    private AudioFragmentListener audioFragmentListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        activity = ((AppCompatActivity) context);
        if (activity instanceof AudioSelectListener) {
            audioSelectListener = (AudioSelectListener) activity;
        }

        if (activity instanceof AudioFragmentListener) {
            audioFragmentListener = (AudioFragmentListener) activity;
        }

        audioPlayViewModel = new ViewModelProvider(requireActivity()).get(AudioPlayViewModel.class);
        audioPlayViewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    if (progress_bar != null)
                        progress_bar.setVisibility(View.VISIBLE);  //because on_intent is called before inflation of view
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    if (progress_bar != null)
                        progress_bar.setVisibility(View.GONE);  //because on_intent is called before inflation of view
                    if (Global.whether_file_cached(audioPlayViewModel.fileObjectType)) {
                        if (activity instanceof AudioPlayerActivity) {
                            ((AudioPlayerActivity) activity).data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(audioPlayViewModel.currently_shown_file.getPath()));
                            data = ((AudioPlayerActivity) activity).data;
                        }
                    }
                    Intent service_intent = new Intent(context, AudioPlayerService.class);
                    service_intent.setData(data);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(service_intent);
                    } else {
                        context.startService(service_intent);
                    }
                    audioPlayViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        audioSelectListener = null;
        audioFragmentListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list_popupwindowpojos = new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon, getString(R.string.delete), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 2));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.copy_icon, getString(R.string.copy_to), 3));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 4));
    }

    public void set_audio(AudioPOJO audioPOJO) {
        audioPlayViewModel.fileObjectType = audioPOJO.getFileObjectType();
        audioPlayViewModel.fromThirdPartyApp = false;
        audioPlayViewModel.file_path = audioPOJO.getData();
        audioPlayViewModel.album_id = audioPOJO.getAlbumId();

        setTitleArt(audioPOJO.getId(), audioPOJO.getTitle(), audioPOJO.getData());
        audio_player_service.current_audio = audioPOJO;
    }

    public void initiate_audio() {
        if (activity instanceof AudioPlayerActivity) {
            data = ((AudioPlayerActivity) activity).data;
        }

        if (data != null) {
            if (progress_bar != null)
                progress_bar.setVisibility(View.VISIBLE); //because on_intent is called before inflation of view
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_play, container, false);
        handler = new Handler();
        onserviceconnection_handler = new Handler();
        handler_for_art = new Handler();
        service_connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName component_name, IBinder binder) {
                audio_player_service = ((AudioPlayerService.AudioBinder) binder).getService();
                audio_player_service.setMediaPlayerPrepareListener(new AudioPlayerService.MediaPlayerServicePrepareListener() {
                    public void onMediaPrepare() {
                        total_duration = audio_player_service.get_duration();
                        isDurationMoreThanHour = (total_duration / 1000) > 3599;
                        total_time_tv.setText(convertSecondsToHMmSs(total_duration));
                        seekbar.setMax(total_duration);
                        play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pause_icon));
                    }
                });

                audio_player_service.setAudioPlayerServiceBroadcastListener(new AudioPlayerService.AudioPlayerServiceBroadCastListener() {
                    @Override
                    public void onBroadcast(int number) {
                        switch (number) {
                            case AudioPlayerService.GOTO_PREVIOUS:
                            case AudioPlayerService.GOTO_NEXT:
                                if (audio_player_service.current_audio != null) {
                                    setTitleArt(audio_player_service.current_audio.getId(), audio_player_service.current_audio.getTitle(), audio_player_service.current_audio.getData());
                                }
                                ((AudioPlayerActivity) context).on_completion_audio();
                                break;
                            case AudioPlayerService.START:
                            case AudioPlayerService.PAUSE:
                                if (audio_player_service.prepared && !audio_player_service.playmode) {
                                    play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.play_icon));
                                } else if (audio_player_service.prepared && audio_player_service.playmode) {
                                    play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pause_icon));
                                }
                                break;
                            case AudioPlayerService.STOP:
                                setTitleArt(0, "", null);
                                total_time_tv.setText("00.00");
                                break;
                            default:
                                break;
                        }
                        enable_disable_previous_next_btn();
                    }
                });
                service_bound = true;
            }

            public void onServiceDisconnected(ComponentName component_nane) {
                audio_player_service.setMediaPlayerPrepareListener(null);
                audio_player_service.setAudioPlayerServiceBroadcastListener(null);
                audio_player_service = null;
                service_bound = false;
            }
        };

        SeekBar volumeControlSeekbar = v.findViewById(R.id.current_play_volume_seekbar);
        volumeControlSeekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeControlSeekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volumeControlSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        audio_name_tv = v.findViewById(R.id.current_play_audio_name);
        audio_album_tv = v.findViewById(R.id.current_play_album);
        audio_artists_tv = v.findViewById(R.id.current_play_artists);
        next_audio_tv = v.findViewById(R.id.current_play_next_audio_title);

        ImageButton overflow_btn = v.findViewById(R.id.current_play_overflow);
        overflow_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listPopWindow.showAsDropDown(v, 0, Global.LIST_POPUP_WINDOW_DROP_DOWN_OFFSET);
            }
        });

        ImageButton exit_btn = v.findViewById(R.id.audio_player_exit_btn);
        exit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((AudioPlayerActivity) context).keyBoardUtil.getKeyBoardVisibility()) {
                    ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(), 0);
                }
                ((AudioPlayerActivity) context).getOnBackPressedDispatcher().onBackPressed();
                audio_player_service.handler.obtainMessage(AudioPlayerService.STOP).sendToTarget();
            }
        });

        ImageButton audio_play_list_btn = v.findViewById(R.id.current_play_list_image_btn);
        audio_play_list_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioSavedListDetailsDialog audioSavedListDetailsDialog = AudioSavedListDetailsDialog.getInstance(AUDIO_SELECT_REQUEST_CODE, 0, AudioPlayerActivity.CURRENT_PLAY_LIST);
                audioSavedListDetailsDialog.show(getParentFragmentManager(), "audioSavedListDetailsDialog");
            }
        });

        listPopWindow = new PopupWindow(context);
        ListView listView = new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context, list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popup_window_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.list_popup_background));
        listView.setOnItemClickListener(new ListPopupWindowClickListener());

        EquallyDistributedImageButtonsLayout tb_layout = new EquallyDistributedImageButtonsLayout(context, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] drawables = {R.drawable.previous_icon, R.drawable.backward_10_icon, R.drawable.play_icon, R.drawable.forward_10_icon, R.drawable.next_icon};
        tb_layout.setResourceImageDrawables(drawables);

        Toolbar bottom_toolbar = v.findViewById(R.id.current_play_bottom_toolbar);
        bottom_toolbar.addView(tb_layout);
        previous_btn = bottom_toolbar.findViewById(R.id.toolbar_img_btn_1);
        ImageButton backward_btn = bottom_toolbar.findViewById(R.id.toolbar_img_btn_2);
        play_pause_btn = bottom_toolbar.findViewById(R.id.toolbar_img_btn_3);
        ImageButton forward_btn = bottom_toolbar.findViewById(R.id.toolbar_img_btn_4);
        next_btn = bottom_toolbar.findViewById(R.id.toolbar_img_btn_5);
        album_art_imageview = v.findViewById(R.id.fragment_current_play_albumart);
        total_time_tv = v.findViewById(R.id.audio_player_total_time);
        current_progress_tv = v.findViewById(R.id.audio_player_current_progress);
        seekbar = v.findViewById(R.id.audio_player_seekbar);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    audio_player_service.seek_to(progress);
                }
            }

            public void onStartTrackingTouch(SeekBar sb) {

            }

            public void onStopTrackingTouch(SeekBar sb) {

            }

        });

        progress_bar = v.findViewById(R.id.audio_play_progressbar);
        progress_bar.setVisibility(View.GONE);

        previous_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) return;
                audio_player_service.handler.obtainMessage(AudioPlayerService.GOTO_PREVIOUS).sendToTarget();
            }
        });

        backward_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) return;
                audio_player_service.handler.obtainMessage(AudioPlayerService.MOVE_BACKWARD).sendToTarget();
            }
        });

        play_pause_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) return;
                if (audio_player_service.prepared && !audio_player_service.playmode) {
                    audio_player_service.handler.obtainMessage(AudioPlayerService.START).sendToTarget();
                    play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pause_icon));
                    update_position();
                } else if (audio_player_service.prepared && audio_player_service.playmode) {
                    audio_player_service.handler.obtainMessage(AudioPlayerService.PAUSE).sendToTarget();
                    play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.play_icon));
                }
            }
        });

        forward_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) return;
                audio_player_service.handler.obtainMessage(AudioPlayerService.MOVE_FORWARD).sendToTarget();
            }
        });

        next_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) return;
                audio_player_service.handler.obtainMessage(AudioPlayerService.GOTO_NEXT).sendToTarget();
            }
        });

        audioPlayViewModel.isAlbumArtFetched.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    audio_name_tv.setText(audioPlayViewModel.audio_file_name);
                    GlideApp.with(context).load(Global.GET_ALBUM_ART_URI(audioPlayViewModel.album_id)).placeholder(R.drawable.woofer_icon).error(R.drawable.woofer_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(album_art_imageview);
                    audioPlayViewModel.isAlbumArtFetched.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        DeleteFileOtherActivityViewModel deleteFileOtherActivityViewModel = new ViewModelProvider(AudioPlayFragment.this).get(DeleteFileOtherActivityViewModel.class);
        deleteFileOtherActivityViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!deleteFileOtherActivityViewModel.deleted_audio_files.isEmpty()) {
                        if (audio_player_service != null) {
                            audio_player_service.handler.obtainMessage(AudioPlayerService.STOP).sendToTarget();
                        }
                        if (audioFragmentListener != null) {
                            audioFragmentListener.onDeleteAudio(deleteFileOtherActivityViewModel.deleted_audio_files);
                        }

                        AudioPlayerActivity.AUDIO_FILE = null;
                    }

                    deleteFileOtherActivityViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });


        getParentFragmentManager().setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(DELETE_FILE_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    Uri tree_uri = result.getParcelable("tree_uri");
                    String tree_uri_path = result.getString("tree_uri_path");
                    String source_folder = result.getString("source_folder");
                    files_selected_for_delete = new ArrayList<>();
                    files_selected_for_delete.add(AudioPlayerActivity.AUDIO_FILE);
                    deleteFileOtherActivityViewModel.deleteAudioPOJO(source_folder, files_selected_for_delete, audioPlayViewModel.fileObjectType, tree_uri, tree_uri_path);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(AUDIO_SELECT_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(AUDIO_SELECT_REQUEST_CODE)) {
                    AudioPOJO audio = result.getParcelable("audio");
                    int id = audio.getId();
                    Uri data;
                    if (id == 0) {
                        File file = new File(audio.getData());
                        data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", file);
                    } else {
                        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        data = Uri.withAppendedPath(uri, String.valueOf(id));
                    }

                    if (audioSelectListener != null && data != null) {
                        audioSelectListener.onAudioSelect(data, audio);
                    }
                }
            }
        });
        return v;
    }

    private void update_position() {
        handler.post(new Runnable() {
            public void run() {
                int current_pos = audio_player_service.get_current_position(); //audio_player;_service.get_current_position();
                seekbar.setProgress(current_pos);
                current_progress_tv.setText(convertSecondsToHMmSs(current_pos));

                if (audio_player_service.completed) {
                    play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.play_icon));
                    current_progress_tv.setText(isDurationMoreThanHour ? String.format("%d:%d:%d", 0, 0, 0) : String.format("%d:%d", 0, 0));
                    seekbar.setProgress(0);
                    handler.removeCallbacks(this);
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private String convertSecondsToHMmSs(int milliseconds) {
        int seconds = milliseconds / 1000;
        int s = seconds % 60;
        int m = (seconds / 60) % 60;

        if (isDurationMoreThanHour) {
            int h = (seconds / (60 * 60)) % 24;
            return String.format("%d:%02d:%02d", h, m, s);
        } else {
            return String.format("%02d:%02d", m, s);
        }
    }

    public void enable_disable_previous_next_btn() {
        if (AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty()) {
            previous_btn.setEnabled(false);
            previous_btn.setAlpha(Global.DISABLE_ALFA);
            next_btn.setEnabled(false);
            next_btn.setAlpha(Global.DISABLE_ALFA);
            audio_album_tv.setText(getString(R.string.album_colon) + " null");
            audio_artists_tv.setText(getString(R.string.artists_colon) + " null");
            next_audio_tv.setText(getString(R.string.next_audio_colon) + " null");
            return;
        }

        if (AudioPlayerService.CURRENT_PLAY_NUMBER <= 0) {
            previous_btn.setEnabled(false);
            previous_btn.setAlpha(Global.DISABLE_ALFA);
        } else {
            previous_btn.setEnabled(true);
            previous_btn.setAlpha(Global.ENABLE_ALFA);
        }

        if (AudioPlayerService.CURRENT_PLAY_NUMBER >= AudioPlayerService.AUDIO_QUEUED_ARRAY.size() - 1) {
            next_btn.setEnabled(false);
            next_btn.setAlpha(Global.DISABLE_ALFA);
        } else {
            next_btn.setEnabled(true);
            next_btn.setAlpha(Global.ENABLE_ALFA);
        }

        // Below is placed here instead of at setTittleArt method because, AudioPlayerService.AUDIO_QUEUED_ARRAY and CURRENT_PLAY not yet updated on selection of audio
        if (audio_player_service != null && audio_player_service.current_audio != null) {
            audio_album_tv.setText(getString(R.string.album_colon) + " " + audio_player_service.current_audio.getAlbum());
            audio_artists_tv.setText(getString(R.string.artists_colon) + " " + audio_player_service.current_audio.getArtist());
        } else {
            audio_album_tv.setText(getString(R.string.album_colon) + " null");
            audio_artists_tv.setText(getString(R.string.artists_colon) + " null");

        }
        if (next_btn.isEnabled() && AudioPlayerService.AUDIO_QUEUED_ARRAY.size() > AudioPlayerService.CURRENT_PLAY_NUMBER + 1) {
            next_audio_tv.setText(getString(R.string.next_audio_colon) + " " + AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER + 1).getTitle());
        } else {
            next_audio_tv.setText(getString(R.string.next_audio_colon) + " null");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent service_intent = new Intent(context, AudioPlayerService.class);
        service_bound = context.bindService(service_intent, service_connection, Context.BIND_AUTO_CREATE);
        Runnable runnable = new Runnable() {
            public void run() {
                if (audio_player_service == null) {
                    onserviceconnection_handler.postDelayed(this, 1000);
                } else {
                    if (AudioPlayerActivity.AUDIO_FILE != null) {
                        String path = AudioPlayerActivity.AUDIO_FILE.getData();
                        setTitleArt(AudioPlayerActivity.AUDIO_FILE.getId(), AudioPlayerActivity.AUDIO_FILE.getTitle(), path); // dont try audio_player_service.current_audio, it may not have been instantiated.
                    }
                    total_duration = audio_player_service.get_duration();
                    isDurationMoreThanHour = (total_duration / 1000) > 3599;
                    current_progress_tv.setText(isDurationMoreThanHour ? String.format("%d:%d:%d", 0, 0, 0) : String.format("%d:%d", 0, 0));
                    total_time_tv.setText(convertSecondsToHMmSs(total_duration));

                    seekbar.setMax(total_duration);
                    enable_disable_previous_next_btn();
                    if (audio_player_service.playmode) {
                        play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pause_icon));
                    }

                    update_position();
                    onserviceconnection_handler.removeCallbacks(this);
                }
            }
        };
        onserviceconnection_handler.post(runnable);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (service_bound) {
            context.unbindService(service_connection);
            service_bound = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        handler_for_art.removeCallbacksAndMessages(null);
        onserviceconnection_handler.removeCallbacksAndMessages(null);
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        audio_player_service.removeAudioPlayerServiceBroadcastListener();
    }

    public void setTitleArt(int audio_id, String audiofilename, final String audiofilepath) {
        audioPlayViewModel.fetchAlbumArt(audio_id, audiofilename, audiofilepath);
    }

    private void set_ring_tone() {
        if (AudioPlayerActivity.AUDIO_FILE.getFileObjectType() == null) {
            Global.print(context, getString(R.string.not_able_to_process));
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DATA, AudioPlayerActivity.AUDIO_FILE.getData());
        contentValues.put(MediaStore.MediaColumns.TITLE, AudioPlayerActivity.AUDIO_FILE.getTitle());
        contentValues.put(MediaStore.MediaColumns.SIZE, AudioPlayerActivity.AUDIO_FILE.getDuration());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
        contentValues.put(MediaStore.Audio.Media.ARTIST, "artist");
        contentValues.put(MediaStore.Audio.Media.DURATION, 500);
        contentValues.put(MediaStore.Audio.Media.IS_ALARM, false);
        contentValues.put(MediaStore.Audio.Media.IS_MUSIC, false);
        contentValues.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        contentValues.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        ContentResolver cr = context.getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri addedUri = cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                OutputStream outputStream = cr.openOutputStream(addedUri);
                byte[] byte_array = new byte[500];
                BufferedInputStream bufferedInputStream = null;
                if (AudioPlayerActivity.AUDIO_FILE.getFileObjectType() == FileObjectType.FILE_TYPE) {
                    bufferedInputStream = new BufferedInputStream(new FileInputStream(AudioPlayerActivity.AUDIO_FILE.getData()));
                } else if (AudioPlayerActivity.AUDIO_FILE.getFileObjectType() == FileObjectType.USB_TYPE) {
                    UsbFile usbFile = null;
                    if (MainActivity.usbFileRoot != null) {
                        usbFile = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(AudioPlayerActivity.AUDIO_FILE.getData()));
                    }

                    bufferedInputStream = UsbFileStreamFactory.createBufferedInputStream(usbFile, MainActivity.usbCurrentFs);
                }

                int size = bufferedInputStream.read(byte_array, 0, byte_array.length);
                outputStream.write(byte_array, 0, size);
                bufferedInputStream.close();
                outputStream.flush();
                outputStream.close();

            } catch (IOException e) {

            }
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, addedUri);
        } else {
            Uri url = MediaStore.Audio.Media.getContentUriForPath(AudioPlayerActivity.AUDIO_FILE.getData());
            Uri addedUri = cr.insert(url, contentValues);
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, addedUri);
        }

        Global.print(context, getString(R.string.ringtone_set));
    }

    private class ListPopupWindowClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
            final ArrayList<String> files_selected_array = new ArrayList<>();
            if (AudioPlayerActivity.AUDIO_FILE == null) return;
            if (activity instanceof AudioPlayerActivity) {
                data = ((AudioPlayerActivity) activity).data;
            }

            switch (p3) {
                case 0:
                    if (!new File(AudioPlayerActivity.AUDIO_FILE.getData()).exists() || Global.whether_file_cached(audioPlayViewModel.fileObjectType) || AudioPlayerActivity.AUDIO_FILE.getFileObjectType() == null || Global.IS_CHILD_FILE(AudioPlayerActivity.AUDIO_FILE.getData(), Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath())) {
                        Global.print(context, getString(R.string.not_able_to_process));
                        break;
                    }

                    if (!AllAudioListFragment.FULLY_POPULATED) {
                        Global.print(context, getString(R.string.wait_till_all_audios_populated_in_all_songs_tab));
                        break;
                    }
                    files_selected_array.add(AudioPlayerActivity.AUDIO_FILE.getData());
                    DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE, files_selected_array, AudioPlayerActivity.AUDIO_FILE.getFileObjectType());
                    deleteFileAlertDialogOtherActivity.show(getParentFragmentManager(), "deletefilealertotheractivity");
                    break;
                case 1:
                    Uri src_uri = null;
                    if (AudioPlayerActivity.AUDIO_FILE.getFileObjectType() == null) {
                        src_uri = data;

                    } else if (audioPlayViewModel.fileObjectType==FileObjectType.FILE_TYPE || Global.whether_file_cached(audioPlayViewModel.fileObjectType)) {
                        src_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(AudioPlayerActivity.AUDIO_FILE.getData()));
                    } else {
                        src_uri = data;
                    }

                    if (src_uri == null) {
                        Global.print(context, getString(R.string.not_able_to_process));
                        break;
                    }
                    ArrayList<Uri> uri_list = new ArrayList<>();
                    uri_list.add(src_uri);
                    FileIntentDispatch.sendUri(context, uri_list);

                    break;
                case 2:
                    Uri copy_uri;
                    if (AudioPlayerActivity.AUDIO_FILE.getFileObjectType() == null) {
                        copy_uri = data;
                    } else if (audioPlayViewModel.fileObjectType==FileObjectType.FILE_TYPE || Global.whether_file_cached(audioPlayViewModel.fileObjectType)) {
                        copy_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(AudioPlayerActivity.AUDIO_FILE.getData()));
                    } else {
                        copy_uri = data;
                    }

                    if (copy_uri == null) {
                        Global.print(context, getString(R.string.not_able_to_process));
                        break;
                    }
                    if (activity instanceof AudioPlayerActivity) {
                        ((AudioPlayerActivity) activity).clear_cache = false;
                    }

                    Intent copy_intent = new Intent(context, CopyToActivity.class);
                    copy_intent.setAction(Intent.ACTION_SEND);
                    copy_intent.putExtra(Intent.EXTRA_STREAM, copy_uri);
                    copy_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    copy_intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    copy_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(copy_intent);
                    } catch (Exception e) {
                        Global.print(context, getString(R.string.could_not_perform_action));
                    }
                    break;

                case 3:
                    if (AudioPlayerActivity.AUDIO_FILE.getFileObjectType() == null || Global.whether_file_cached(audioPlayViewModel.fileObjectType)) {
                        Global.print(context, getString(R.string.not_able_to_process));
                        break;
                    }
                    files_selected_array.add(AudioPlayerActivity.AUDIO_FILE.getData());
                    PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, AudioPlayerActivity.AUDIO_FILE.getFileObjectType());
                    propertiesDialog.show(getParentFragmentManager(), "properties_dialog");
                    break;
                case 4:
                    boolean permission;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        permission = Settings.System.canWrite(context);
                    } else {
                        permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
                    }
                    if (permission) {
                        set_ring_tone();
                        break;
                    } else {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            intent.setData(Uri.parse("package:" + context.getPackageName()));
                            activityResultLauncher_write_settings.launch(intent);
                        } else {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_SETTINGS}, AudioPlayerActivity.WRITE_SETTINGS_PERMISSION_REQUEST_CODE);
                        }
                    }
                    break;
                default:
                    break;
            }
            listPopWindow.dismiss();
        }
    }
}
