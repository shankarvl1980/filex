package svl.kadatha.filex.audio;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import svl.kadatha.filex.BaseActivity;
import svl.kadatha.filex.FileIntentDispatch;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.KeyBoardUtil;
import svl.kadatha.filex.R;
import svl.kadatha.filex.RealPathUtil;
import svl.kadatha.filex.TinyDB;


public class AudioPlayerActivity extends BaseActivity implements AudioSelectListener, AudioFragmentListener {

    public static final String CURRENT_PLAY_LIST = "Current play list";
    public static final String ACTIVITY_NAME = "AUDIO_PLAYER_ACTIVITY";
    static final int WRITE_SETTINGS_PERMISSION_REQUEST_CODE = 59;
    private static final boolean[] alreadyNotificationWarned = new boolean[1];
    public static HashMap<Long, String> EXISTING_AUDIOS_ID;
    static AudioPOJO AUDIO_FILE;
    static ArrayList<String> AUDIO_SAVED_LIST = new ArrayList<>();
    static String AUDIO_NOTIFICATION_INTENT_ACTION;
    private final List<AudioChangeListener> audioChangeListeners = new ArrayList<>();
    private final List<SearchFilterListener> searchFilterListeners = new ArrayList<>();
    public EditText search_edittext;
    public TinyDB tinyDB;
    public boolean search_toolbar_visible;
    public KeyBoardUtil keyBoardUtil;
    public boolean fromThirdPartyApp;
    public boolean clear_cache;
    AudioDatabaseHelper audioDatabaseHelper;
    SQLiteDatabase db;
    Uri data;
    FileObjectType fileObjectType;
    String file_path;
    private Group search_toolbar;
    private ViewPager view_pager;
    private Context context;
    private AudioPlayFragment apf;
    private AllAudioListFragment aalf;
    private AlbumListFragment albumlf;
    private AudioSavedListFragment aslf;
    private PlayScreenFragment psf;
    private AudioPlayViewModel audioPlayViewModel;

    public static AudioPOJO getAudioPojo(Context context, String filePath, FileObjectType fileObjectType) {
        try {
            Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = new String[]{filePath};
            String[] projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION
            };

            try (Cursor cursor = context.getContentResolver().query(
                    contentUri,
                    projection,
                    selection,
                    selectionArgs,
                    null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    return new AudioPOJO(
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                            new File(filePath).getName(),//instead of title from media store, setting the title as file name because to get correct position of current audio in the AUDIO_QUEUED_ARRAY
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                            fileObjectType
                    );
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        context = this;
        tinyDB = new TinyDB(context);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        AUDIO_NOTIFICATION_INTENT_ACTION = getPackageName() + ".AUDIO_NOTIFICATION";
        Global.WARN_NOTIFICATIONS_DISABLED(context, AudioPlayerService.CHANNEL_ID, alreadyNotificationWarned);
        IntentFilter intent_filter = new IntentFilter();
        intent_filter.addAction(AUDIO_NOTIFICATION_INTENT_ACTION);

        audioDatabaseHelper = new AudioDatabaseHelper(context);
        db = audioDatabaseHelper.getDatabase();

        View containerLayout = findViewById(R.id.activity_audio_container_layout);
        keyBoardUtil = new KeyBoardUtil(containerLayout);
        search_toolbar = findViewById(R.id.all_audio_search_toolbar);
        search_edittext = findViewById(R.id.search_view);
        search_edittext.setMaxWidth(Integer.MAX_VALUE);
        search_edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!search_toolbar_visible) {
                    return;
                }
                for (SearchFilterListener listener : searchFilterListeners) {
                    if (listener != null) {
                        listener.onSearchFilter(s.toString());
                    }
                }
            }
        });

        ImageButton search_cancel_btn = findViewById(R.id.search_view_cancel_button);
        search_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarVisibility(false);
            }
        });

        TabLayout tab_layout = findViewById(R.id.activity_audio_player_tab_layout);
        view_pager = findViewById(R.id.activity_audio_player_viewpager);
        FloatingActionButton floating_back_button = findViewById(R.id.floating_action_audio_player);
        floating_back_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        ViewPagerFragmentAdapter adapter = new ViewPagerFragmentAdapter(getSupportFragmentManager());
        view_pager.setAdapter(adapter);
        view_pager.setOffscreenPageLimit(3);

        view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageSelected(int p1) {
                if (p1 == 0) {
                    aalf.whether_audios_set_to_current_list = false;
                }
            }

            public void onPageScrolled(int p1, float p2, int p3) {

            }

            public void onPageScrollStateChanged(int p1) {

            }
        });

        tab_layout.setupWithViewPager(view_pager);

        audioPlayViewModel = new ViewModelProvider(AudioPlayerActivity.this).get(AudioPlayViewModel.class);
        adapter.startUpdate(view_pager);
        aalf = (AllAudioListFragment) adapter.instantiateItem(view_pager, 0);
        albumlf = (AlbumListFragment) adapter.instantiateItem(view_pager, 1);
        aslf = (AudioSavedListFragment) adapter.instantiateItem(view_pager, 2);
        adapter.finishUpdate(view_pager);
        psf = (PlayScreenFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_fragment_container);
        Intent intent = getIntent();
        on_intent(intent, savedInstanceState);
        AUDIO_SAVED_LIST = audioDatabaseHelper.getTables();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (keyBoardUtil.getKeyBoardVisibility()) {
                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
                } else if (search_toolbar_visible) {
                    setSearchBarVisibility(false);
                } else if (audioPlayViewModel.play_screen_expanded_view) {
                    audioPlayViewModel.play_screen_expanded_view = false;
                    instantiatePlayScreenFragment(false);
                } else {
                    int current_item = view_pager.getCurrentItem();
                    switch (current_item) {
                        case 0:
                            if (!aalf.audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                                aalf.clear_selection();
                                albumlf.clear_selection();
                                aslf.clear_selection();
                            } else {
                                aalf.clear_selection();
                                albumlf.clear_selection();
                                aslf.clear_selection();

                                clear_cache = false;
                                finish();
                            }
                            break;
                        case 1:
                            if (!albumlf.audioListViewModel.album_pojo_selected_items.isEmpty()) {
                                aalf.clear_selection();
                                albumlf.clear_selection();
                                aslf.clear_selection();

                            } else {
                                aalf.clear_selection();
                                albumlf.clear_selection();
                                aslf.clear_selection();

                                clear_cache = false;
                                finish();
                            }
                            break;
                        case 2:
                            if (!aslf.audioListViewModel.audio_saved_list_selected_items.isEmpty()) {
                                aalf.clear_selection();
                                albumlf.clear_selection();
                                aslf.clear_selection();

                            } else {
                                aalf.clear_selection();
                                albumlf.clear_selection();
                                aslf.clear_selection();

                                clear_cache = false;
                                finish();
                            }

                            break;
                        default:
                            aalf.clear_selection();
                            albumlf.clear_selection();
                            aslf.clear_selection();

                            clear_cache = false;
                            finish();
                            break;
                    }
                }
            }
        });
    }

    private void on_intent(Intent intent, Bundle savedInstanceState) {
        if (intent != null) {
            data = intent.getData();
            fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
            file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
            boolean fromArchive = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
            if (file_path == null) {
                file_path = RealPathUtil.getLastSegmentPath(data);
            }
            if (fileObjectType == null || fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                fileObjectType = FileObjectType.FILE_TYPE;
                fromThirdPartyApp = true;
            }

            if (savedInstanceState == null) {
                if (data != null) {
                    String name = new File(file_path).getName();
                    AUDIO_FILE = AudioPlayerActivity.getAudioPojo(context, file_path, (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) ? FileObjectType.FILE_TYPE : fileObjectType);
                    if (AUDIO_FILE == null) {
                        AUDIO_FILE = new AudioPOJO(0, file_path, name, null, null, null, "0", (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) ? FileObjectType.FILE_TYPE : fileObjectType);
                    }

                    audioPlayViewModel.fileObjectType = fileObjectType;
                    audioPlayViewModel.fromThirdPartyApp = fromThirdPartyApp;
                    audioPlayViewModel.fromArchive = fromArchive;
                    audioPlayViewModel.file_path = file_path;
                    audioPlayViewModel.album_id = AudioPlayerActivity.AUDIO_FILE.getAlbumId();
                    String source_folder = new File(audioPlayViewModel.file_path).getParent();
                    audioPlayViewModel.albumPolling(source_folder, audioPlayViewModel.fileObjectType, audioPlayViewModel.fromThirdPartyApp);


                    if (psf != null) {
                        psf.initiate_audio();
                        // psf.set_audio(AUDIO_FILE);
                    }

                    //apf.initiate_audio();
                }

            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        on_intent(intent, null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache", clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache = savedInstanceState.getBoolean("clear_cache");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations() && clear_cache) {
            clearCache();
        }
    }

    public void clearCache() {
        Global.CLEAR_CACHE();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == WRITE_SETTINGS_PERMISSION_REQUEST_CODE && Settings.System.canWrite(this)) {
                Global.print(context, getString(R.string.now_ringtone_can_be_set));
            } else {
                Global.print(context, getString(R.string.permission_not_granted));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_SETTINGS_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Global.print(context, getString(R.string.now_ringtone_can_be_set));
        } else {
            Global.print(context, getString(R.string.permission_not_granted));
        }
    }

    @Override
    public boolean getSearchBarVisibility() {
        return search_toolbar_visible;
    }

    public void setSearchBarVisibility(boolean visible) {
        if (!AllAudioListFragment.FULLY_POPULATED) {
            Global.print(context, getString(R.string.please_wait));
            return;
        }
        search_toolbar_visible = visible;
        if (search_toolbar_visible) {
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_edittext.requestFocus();
            aalf.clear_selection();
            albumlf.clear_selection();
        } else {
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
            search_toolbar.setVisibility(View.GONE);
            search_edittext.setText("");
            search_edittext.clearFocus();
            aalf.clear_selection();
            albumlf.clear_selection();
            for (SearchFilterListener listener : searchFilterListeners) {
                if (listener != null) {
                    listener.onSearchFilter(null);
                }
            }
        }
    }

    @Override
    public void hideKeyBoard() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
    }

    @Override
    public boolean getKeyBoardVisibility() {
        return keyBoardUtil.getKeyBoardVisibility();
    }

    @Override
    public void onAudioSelect(Uri data, AudioPOJO audio) {
        android.content.Intent service_intent = new android.content.Intent(context, AudioPlayerService.class);
        service_intent.setData(data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service_intent);
        } else {
            startService(service_intent);
        }

        AUDIO_FILE = audio;
        if (psf != null) {
            psf.set_audio(audio);
        }
        on_audio_change();
    }

    @Override
    public void onAudioSave() {
        aslf.onSaveAudioList();
    }

    @Override
    public void refreshAudioPlayNavigationButtons() {
        psf.enable_disable_previous_next_btn();
    }

    @Override
    public void onDeleteAudio(ArrayList<AudioPOJO> list) {
        aalf.clear_selection();
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            String path = list.get(i).getData();
            for (AudioPOJO audio : AudioPlayerService.AUDIO_QUEUED_ARRAY) {
                if (audio.getData().equals(path)) {
                    AudioPlayerService.AUDIO_QUEUED_ARRAY.remove(audio);
                    break;
                }
            }
        }

        if (AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty()) {
            AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
        } else if (AudioPlayerService.CURRENT_PLAY_NUMBER > AudioPlayerService.AUDIO_QUEUED_ARRAY.size() - 1) {
            AudioPlayerService.CURRENT_PLAY_NUMBER = AudioPlayerService.AUDIO_QUEUED_ARRAY.size() - 1;
        }
    }

    public void instantiatePlayScreenFragment(boolean expansion) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (expansion) {
            transaction.setCustomAnimations(R.anim.slide_up_from_below_with_reverse_fade, R.anim.slide_up_from_normal_with_fade, 0, 0);
        } else {
            transaction.setCustomAnimations(R.anim.slide_down_from_above_with_reverse_fade, R.anim.slide_down_from_normal_with_fade, 0, 0);
        }

        psf = new PlayScreenFragment();
        transaction.replace(R.id.bottom_fragment_container, psf);
        transaction.commit();
    }

    public void addAudioChangeListener(AudioChangeListener listener) {
        audioChangeListeners.add(listener);
    }

    public void removeAudioChangeListener(AudioChangeListener listener) {
        audioChangeListeners.remove(listener);
    }

    public void addSearchFilterListener(SearchFilterListener listener) {
        searchFilterListeners.add(listener);
    }

    public void removeSearchFilterListener(SearchFilterListener listener) {
        searchFilterListeners.remove(listener);
    }

    public void on_audio_change() {
        if (!audioChangeListeners.isEmpty()) {
            for (AudioChangeListener listener : audioChangeListeners) {
                listener.onAudioChange();
            }
        }
    }

    interface AudioChangeListener {
        void onAudioChange();
    }

    interface SearchFilterListener {
        void onSearchFilter(String constraint);
    }

    private class ViewPagerFragmentAdapter extends FragmentPagerAdapter {
        ViewPagerFragmentAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int p1) {
            switch (p1) {
                case 1:
                    return new AlbumListFragment();

                case 2:
                    return new AudioSavedListFragment();

                default:
                    return new AllAudioListFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return getString(R.string.album);

                case 2:
                    return getString(R.string.audio_list);

                default:
                    return getString(R.string.all_songs);
            }
        }
    }
}
