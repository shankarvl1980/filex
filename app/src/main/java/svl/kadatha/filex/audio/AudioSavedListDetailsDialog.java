package svl.kadatha.filex.audio;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.EquallyDistributedButtonsWithTextLayout;
import svl.kadatha.filex.FastScrollerView;
import svl.kadatha.filex.FileIntentDispatch;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.ItemSeparatorDecoration;
import svl.kadatha.filex.ListPopupWindowPOJO;
import svl.kadatha.filex.PropertiesDialog;
import svl.kadatha.filex.R;

public class AudioSavedListDetailsDialog extends DialogFragment {
    public List<AudioPOJO> clicked_audio_list, total_audio_list;
    private CurrentListRecyclerViewAdapter currentAudioListRecyclerViewAdapter;
    private Context context;
    private RecyclerView currentAudioListRecyclerview;
    private TextView file_number_view;
    private ImageButton all_select_btn;
    private TextView empty_audio_list_tv;
    private Toolbar bottom_toolbar;
    private Button remove_btn;
    private Button play_btn;
    private Button overflow_btn;
    private String audio_list_clicked_name;
    private boolean whether_saved_play_list;
    private int number_button = 3;
    private boolean toolbar_visible;
    private int scroll_distance;
    private int audio_list_size, total_audio_list_size;
    private ConstraintLayout search_toolbar;
    private EditText search_edittext;
    private boolean search_toolbar_visible;
    private PopupWindow listPopWindow;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    private int playing_audio_text_color, rest_audio_main_text_color, rest_audio_second_text_color;
    private AudioListViewModel audioListViewModel;
    private FrameLayout progress_bar;
    private String request_code;
    private Bundle bundle;
    private AppCompatActivity activity;
    private AudioFragmentListener audioFragmentListener;

    public static AudioSavedListDetailsDialog getInstance(String request_code, int pos, String list_name) {
        AudioSavedListDetailsDialog audioSavedListDetailsDialog = new AudioSavedListDetailsDialog();
        Bundle bundle = new Bundle();
        bundle.putString("request_code", request_code);
        bundle.putInt("pos", pos);
        bundle.putString("list_name", list_name);
        audioSavedListDetailsDialog.setArguments(bundle);
        return audioSavedListDetailsDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        activity = (AppCompatActivity) context;
        if (activity instanceof AudioFragmentListener) {
            audioFragmentListener = (AudioFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        audioFragmentListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);

        bundle = getArguments();
        if (bundle != null) {
            request_code = bundle.getString("request_code");
            int saved_audio_clicked_pos = bundle.getInt("pos");
            if (saved_audio_clicked_pos != 0) {
                whether_saved_play_list = true;
                number_button = 4;
            }
            audio_list_clicked_name = bundle.getString("list_name");
        }

        list_popupwindowpojos = new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 2));
        playing_audio_text_color = getResources().getColor(R.color.light_item_select_text_color);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.recycler_main_text_color, typedValue, true);
        rest_audio_main_text_color = typedValue.data;
        theme.resolveAttribute(R.attr.recycler_second_text_color, typedValue, true);
        rest_audio_second_text_color = typedValue.data;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_album_details, container, false);
        TextView dialog_title = v.findViewById(R.id.album_details_panel_title_TextView);
        dialog_title.setText(audio_list_clicked_name);
        ImageButton search_btn = v.findViewById(R.id.album_details_search_img_btn);
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                if (!search_toolbar_visible) {
                    set_visibility_searchbar(true);
                }
            }
        });

        all_select_btn = v.findViewById(R.id.album_details_all_select);
        all_select_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                if (progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                int size = clicked_audio_list.size();

                if (audioListViewModel.audio_pojo_selected_items.size() < size) {
                    audioListViewModel.audio_pojo_selected_items = new IndexedLinkedHashMap<>();
                    if (whether_saved_play_list) {
                        audioListViewModel.selected_audio_rowid_list = new ArrayList<>();
                    }
                    for (int i = 0; i < size; ++i) {
                        audioListViewModel.audio_pojo_selected_items.put(i, clicked_audio_list.get(i));
                        if (whether_saved_play_list) {
                            audioListViewModel.selected_audio_rowid_list.add(audioListViewModel.audio_rowid_list.get(i));
                        }
                    }

                    currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                    all_select_btn.setImageResource(R.drawable.deselect_icon);
                } else {
                    clear_selection();
                }
                file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + audio_list_size);
            }
        });

        search_toolbar = v.findViewById(R.id.album_details_search_toolbar);
        search_edittext = v.findViewById(R.id.album_details_search_view);
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
                currentAudioListRecyclerViewAdapter.getFilter().filter(s.toString());
            }
        });

        ImageButton search_cancel_btn = v.findViewById(R.id.album_details_search_view_cancel_button);
        search_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_visibility_searchbar(false);
            }
        });

        file_number_view = v.findViewById(R.id.album_details_file_number);

        currentAudioListRecyclerview = v.findViewById(R.id.album_details_recyclerview);
        ItemSeparatorDecoration itemSeparatorDecoration = new ItemSeparatorDecoration(context, 1, currentAudioListRecyclerview);
        currentAudioListRecyclerview.addItemDecoration(itemSeparatorDecoration);
        FastScrollerView fastScrollerView = v.findViewById(R.id.fastScroller_album_detail);
        fastScrollerView.setRecyclerView(currentAudioListRecyclerview);
        currentAudioListRecyclerview.setLayoutManager(new LinearLayoutManager(context));
        currentAudioListRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            final int threshold = 5;

            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (scroll_distance > threshold && toolbar_visible) {
                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    toolbar_visible = false;
                    scroll_distance = 0;
                } else if (scroll_distance < -threshold && !toolbar_visible) {
                    if (!audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                        toolbar_visible = true;
                        scroll_distance = 0;
                    }
                }

                if ((toolbar_visible && dy > 0) || (!toolbar_visible && dy < 0)) {
                    scroll_distance += dy;
                }
            }
        });
        empty_audio_list_tv = v.findViewById(R.id.album_details_empty_list_tv);
        FloatingActionButton floating_back_button = v.findViewById(R.id.album_details_floating_action_button);
        floating_back_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                if (!audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                    clear_selection();
                } else if (audioFragmentListener != null && audioFragmentListener.getKeyBoardVisibility()) {
                    ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
                } else if (search_toolbar_visible) {
                    set_visibility_searchbar(false);
                } else {
                    dismissAllowingStateLoss();
                }
            }
        });

        progress_bar = v.findViewById(R.id.album_details_progressbar);

        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(context, number_button, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH);

        int[] drawables;
        String[] titles;
        if (number_button == 4) {
            drawables = new int[]{R.drawable.remove_list_icon, R.drawable.play_icon, R.drawable.add_list_icon, R.drawable.overflow_icon};
            titles = new String[]{getString(R.string.remove), getString(R.string.play), getString(R.string.list), getString(R.string.more)};
        } else {
            drawables = new int[]{R.drawable.remove_list_icon, R.drawable.play_icon, R.drawable.overflow_icon};
            titles = new String[]{getString(R.string.remove), getString(R.string.play), getString(R.string.more)};
        }
        tb_layout.setResourceImageDrawables(drawables, titles);

        ToolbarButtonClickListener toolbarButtonClickListener = new ToolbarButtonClickListener();
        bottom_toolbar = v.findViewById(R.id.album_details_bottom_toolbar);

        bottom_toolbar.addView(tb_layout);

        remove_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        play_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_2);

        if (number_button == 4) {
            Button add_to_list_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
            overflow_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_4);
            add_to_list_btn.setOnClickListener(toolbarButtonClickListener);
        } else {
            overflow_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        }

        remove_btn.setOnClickListener(toolbarButtonClickListener);
        play_btn.setOnClickListener(toolbarButtonClickListener);
        overflow_btn.setOnClickListener(toolbarButtonClickListener);


        listPopWindow = new PopupWindow(context);
        ListView listView = new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context, list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popup_window_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.list_popup_background));
        listView.setOnItemClickListener(new ListPopupWindowClickListener());

        audioListViewModel = new ViewModelProvider(this).get(AudioListViewModel.class);
        audioListViewModel.fetch_saved_audio_list(audio_list_clicked_name, whether_saved_play_list);
        audioListViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    clicked_audio_list = audioListViewModel.audio_list;
                    total_audio_list = audioListViewModel.audio_list;
                    currentAudioListRecyclerViewAdapter = new CurrentListRecyclerViewAdapter();
                    currentAudioListRecyclerview.setAdapter(currentAudioListRecyclerViewAdapter);
                    total_audio_list_size = total_audio_list.size();
                    audio_list_size = clicked_audio_list.size();
                    if (audio_list_size == 0) {
                        currentAudioListRecyclerview.setVisibility(View.GONE);
                        empty_audio_list_tv.setVisibility(View.VISIBLE);
                    }
                    file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + audio_list_size);
                    if (!whether_saved_play_list) {
                        currentAudioListRecyclerview.scrollToPosition(AudioPlayerService.CURRENT_PLAY_NUMBER);
                    }
                }
            }
        });

        int size = audioListViewModel.audio_pojo_selected_items.size();
        if (size == 0) {
            bottom_toolbar.setVisibility(View.GONE);
            bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
            toolbar_visible = false;
        } else {
            toolbar_visible = true;
        }
        file_number_view.setText(size + "/" + audio_list_size);
        return v;
    }


    private void remove_and_save(String list_name, List<AudioPOJO> audio_list_to_be_removed, List<Integer> index, List<Long> rowid_list) {
        progress_bar.setVisibility(View.VISIBLE);
        if (!whether_saved_play_list) {
            AudioPlayerService.AUDIO_QUEUED_ARRAY.removeAll(audio_list_to_be_removed);
            int size = audio_list_to_be_removed.size();
            for (int i = 0; i < size; ++i) {
                if (AudioPlayerService.CURRENT_PLAY_NUMBER >= index.get(i)) {
                    AudioPlayerService.CURRENT_PLAY_NUMBER--;
                }
            }
        }

        clicked_audio_list.removeAll(audio_list_to_be_removed);
        total_audio_list.removeAll(audio_list_to_be_removed);
        total_audio_list_size = total_audio_list.size();
        audio_list_size = clicked_audio_list.size();
        if (audio_list_size == 0) {
            currentAudioListRecyclerview.setVisibility(View.GONE);
            empty_audio_list_tv.setVisibility(View.VISIBLE);
        }
        file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + audio_list_size);

        if (AudioPlayerService.CURRENT_PLAY_NUMBER < 0) {
            AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
        }

        if (whether_saved_play_list) {
            if (activity instanceof AudioPlayerActivity) {
                ((AudioPlayerActivity) activity).audioDatabaseHelper.deleteByRowId(list_name, rowid_list);
            }
        }
        progress_bar.setVisibility(View.GONE);
    }

    public void onAudioChange() {
        currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_WIDTH);
        } else {
            window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_HEIGHT);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        int size = audioListViewModel.audio_pojo_selected_items.size();
        if (size == audio_list_size && size != 0) {
            all_select_btn.setImageResource(R.drawable.deselect_icon);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (search_toolbar_visible) {
            set_visibility_searchbar(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listPopWindow.dismiss();
    }

    public void clear_selection() {
        audioListViewModel.audio_pojo_selected_items = new IndexedLinkedHashMap<>();
        audioListViewModel.selected_audio_rowid_list = new ArrayList<>();
        if (currentAudioListRecyclerViewAdapter != null) {
            currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
        }
        bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
        toolbar_visible = false;
        scroll_distance = 0;
        file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + audio_list_size);
        all_select_btn.setImageResource(R.drawable.select_icon);
    }

    private void enable_disable_buttons(boolean enable) {
        if (enable) {
            remove_btn.setAlpha(Global.ENABLE_ALFA);
            play_btn.setAlpha(Global.ENABLE_ALFA);
            overflow_btn.setAlpha(Global.ENABLE_ALFA);
        } else {
            remove_btn.setAlpha(Global.DISABLE_ALFA);
            play_btn.setAlpha(Global.DISABLE_ALFA);
            overflow_btn.setAlpha(Global.DISABLE_ALFA);
        }
        remove_btn.setEnabled(enable);
        play_btn.setEnabled(enable);
        overflow_btn.setEnabled(enable);
    }

    private void set_visibility_searchbar(boolean visible) {
        search_toolbar_visible = visible;
        if (search_toolbar_visible) {
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_edittext.requestFocus();
            clear_selection();
        } else {
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
            search_edittext.setText("");
            search_edittext.clearFocus();
            search_toolbar.setVisibility(View.GONE);
            clear_selection();
            currentAudioListRecyclerViewAdapter.getFilter().filter(null);
        }
    }

    private class ToolbarButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View p1) {
            if (progress_bar.getVisibility() == View.VISIBLE) {
                Global.print(context, getString(R.string.please_wait));
                return;
            }
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
            if (audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                return;
            }

            int id = p1.getId();
            if (id == R.id.toolbar_btn_1) {
                remove_and_save(audio_list_clicked_name, new ArrayList<>(audioListViewModel.audio_pojo_selected_items.values()), new ArrayList<>(audioListViewModel.audio_pojo_selected_items.keySet()), audioListViewModel.selected_audio_rowid_list);
                Global.print(context, getString(R.string.removed_the_selected_audios));
                clear_selection();
            } else if (id == R.id.toolbar_btn_2) {
                AudioPlayerService.AUDIO_QUEUED_ARRAY = new ArrayList<>(audioListViewModel.audio_pojo_selected_items.values());
                if (!whether_saved_play_list) {
                    clicked_audio_list.clear();
                    clicked_audio_list.addAll(audioListViewModel.audio_pojo_selected_items.values());
                    currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
                }

                if (!AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty()) {
                    AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
                    AudioPOJO audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
                    File f = new File(audio.getData());
                    if (f.exists()) {
                        bundle.putParcelable("audio", audio);
                        getParentFragmentManager().setFragmentResult(request_code, bundle);
                    }
                }
                clear_selection();
            } else if (id == R.id.toolbar_btn_3) {
                if (number_button == 4) {

                    AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(audioListViewModel.audio_pojo_selected_items.values());
                    Global.print(context, getString(R.string.added_audios_current_play_list));
                    clear_selection();
                } else {
                    Global.SHOW_LIST_POPUP_WINDOW_BOTTOM(bottom_toolbar, listPopWindow, Global.FOUR_DP);
                }
            } else if (id == R.id.toolbar_btn_4) {
                Global.SHOW_LIST_POPUP_WINDOW_BOTTOM(bottom_toolbar, listPopWindow, Global.FOUR_DP);
            }

            if (audioFragmentListener != null) {
                audioFragmentListener.refreshAudioPlayNavigationButtons();
            }
        }
    }

    private class ListPopupWindowClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
            final ArrayList<String> files_selected_array = new ArrayList<>();
            switch (p3) {
                case 0:
                    if (audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                        break;
                    }
                    ArrayList<File> file_list = new ArrayList<>();
                    for (AudioPOJO audio : audioListViewModel.audio_pojo_selected_items.values()) {
                        file_list.add(new File(audio.getData()));
                    }
                    FileIntentDispatch.sendFile(context, file_list);
                    clear_selection();
                    break;

                case 1:
                    if (audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                        break;
                    }
                    for (AudioPOJO audio : audioListViewModel.audio_pojo_selected_items.values()) {
                        files_selected_array.add(audio.getData());
                    }

                    PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, FileObjectType.FILE_TYPE);
                    propertiesDialog.show(getParentFragmentManager(), "properties_dialog");

                    break;
                default:
                    break;

            }
            listPopWindow.dismiss();
        }
    }

    private class CurrentListRecyclerViewAdapter extends RecyclerView.Adapter<CurrentListRecyclerViewAdapter.ViewHolder> implements Filterable {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new ViewHolder(new AudioListRecyclerViewItem(context, true));
        }

        @Override
        public void onBindViewHolder(CurrentListRecyclerViewAdapter.ViewHolder p1, int p2) {
            AudioPOJO audio = clicked_audio_list.get(p2);
            String album_id = audio.getAlbumId();
            String title = audio.getTitle();
            String album = getString(R.string.album_colon) + " " + audio.getAlbum();
            long duration = 0L;
            String duration_string = audio.getDuration();
            if (duration_string != null) {
                duration = Long.parseLong(duration_string);
            }
            String duration_str = getString(R.string.duration_colon) + " " + (String.format("%d:%02d", duration / 1000 / 60, duration / 1000 % 60));
            String artist = getString(R.string.artists_colon) + " " + audio.getArtist();
            boolean item_selected = audioListViewModel.audio_pojo_selected_items.containsKey(p2);

            if (!whether_saved_play_list && AudioPlayerActivity.AUDIO_FILE != null && audio.getId() == AudioPlayerActivity.AUDIO_FILE.getId()) {
                p1.view.titletextview.setTextColor(playing_audio_text_color);
                p1.view.albumtextview.setTextColor(playing_audio_text_color);
                p1.view.durationtextview.setTextColor(playing_audio_text_color);
                p1.view.artisttextview.setTextColor(playing_audio_text_color);
            } else {
                p1.view.titletextview.setTextColor(rest_audio_main_text_color);
                p1.view.albumtextview.setTextColor(rest_audio_second_text_color);
                p1.view.durationtextview.setTextColor(rest_audio_second_text_color);
                p1.view.artisttextview.setTextColor(rest_audio_second_text_color);
            }

            p1.view.setData(album_id, title, album, duration_str, artist, item_selected);
            p1.view.setSelected(item_selected);
        }

        @Override
        public int getItemCount() {
            return clicked_audio_list.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    return new FilterResults();
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clicked_audio_list = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        clicked_audio_list = total_audio_list;
                    } else {
                        String pattern = constraint.toString().toLowerCase().trim();
                        for (int i = 0; i < total_audio_list_size; ++i) {
                            AudioPOJO audioPOJO = total_audio_list.get(i);
                            if (audioPOJO.getLowerTitle().contains(pattern)) {
                                clicked_audio_list.add(audioPOJO);
                            }
                        }
                    }

                    audio_list_size = clicked_audio_list.size();
                    if (!audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                        clear_selection();
                    } else {
                        notifyDataSetChanged();
                    }
                    file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + audio_list_size);
                }
            };
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnLongClickListener {
            final AudioListRecyclerViewItem view;
            int pos;

            ViewHolder(AudioListRecyclerViewItem view) {
                super(view);
                this.view = view;
                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View p1) {
                pos = getBindingAdapterPosition();
                int size = audioListViewModel.audio_pojo_selected_items.size();
                if (size > 0) {
                    onLongClickProcedure(p1, size);
                } else {
                    AudioPOJO audio = clicked_audio_list.get(pos);
                    File f = new File(audio.getData());
                    if (f.exists()) {
                        if (whether_saved_play_list && (!audioListViewModel.whether_audios_set_to_current_list || AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty())) {
                            AudioPlayerService.AUDIO_QUEUED_ARRAY = clicked_audio_list;
                            audioListViewModel.whether_audios_set_to_current_list = true;
                        }
                        AudioPlayerService.CURRENT_PLAY_NUMBER = pos;
                        bundle.putParcelable("audio", audio);
                        getParentFragmentManager().setFragmentResult(request_code, bundle);
                        if (audioFragmentListener != null) {
                            audioFragmentListener.refreshAudioPlayNavigationButtons();
                        }
                    }
                }
            }

            @Override
            public boolean onLongClick(View p1) {
                onLongClickProcedure(p1, audioListViewModel.audio_pojo_selected_items.size());
                return true;
            }

            private void onLongClickProcedure(View v, int size) {
                pos = getBindingAdapterPosition();

                if (audioListViewModel.audio_pojo_selected_items.containsKey(pos)) {
                    audioListViewModel.audio_pojo_selected_items.remove(pos);
                    if (whether_saved_play_list) {
                        audioListViewModel.selected_audio_rowid_list.remove(audioListViewModel.audio_rowid_list.get(pos));
                    }

                    v.setSelected(false);
                    ((AudioListRecyclerViewItem) v).set_selected(false);
                    --size;
                    if (size >= 1) {
                        bottom_toolbar.setVisibility(View.VISIBLE);
                        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                        toolbar_visible = true;
                        scroll_distance = 0;
                    }

                    if (size == audio_list_size) {
                        all_select_btn.setImageResource(R.drawable.deselect_icon);
                    } else {
                        if (size == 0) {
                            bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                            toolbar_visible = false;
                            scroll_distance = 0;
                        }
                        all_select_btn.setImageResource(R.drawable.select_icon);
                    }
                } else {
                    audioListViewModel.audio_pojo_selected_items.put(pos, clicked_audio_list.get(pos));
                    if (whether_saved_play_list) {
                        audioListViewModel.selected_audio_rowid_list.add(audioListViewModel.audio_rowid_list.get(pos));
                    }
                    v.setSelected(true);
                    ((AudioListRecyclerViewItem) v).set_selected(true);
                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                    ++size;
                    if (size == audio_list_size) {
                        all_select_btn.setImageResource(R.drawable.deselect_icon);
                    }
                }
                file_number_view.setText(size + "/" + audio_list_size);
            }
        }
    }
}
