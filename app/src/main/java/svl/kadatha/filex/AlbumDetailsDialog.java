package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlbumDetailsDialog extends DialogFragment {
    private static final String SAVE_AUDIO_LIST_REQUEST_CODE = "album_details_save_audio_request_code";
    private static final String DELETE_FILE_REQUEST_CODE = "album_details_file_delete_request_code";
    private Context context;
    private AudioListRecyclerViewAdapter audioListRecyclerViewAdapter;
    private RecyclerView selected_album_recyclerview;
    private List<AudioPOJO> audio_list, total_audio_list;
    private ImageButton all_select_btn;
    private TextView file_number_view;
    private TextView empty_audio_list_tv;
    private Toolbar bottom_toolbar;
    private Button delete_btn, play_btn, add_list_btn, overflow_btn;
    private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
    private boolean toolbar_visible;
    private int scroll_distance;
    private PopupWindow listPopWindow;
    private String albumID, album_name;
    private int num_all_audio;
    private ConstraintLayout search_toolbar;
    private EditText search_edittext;
    private boolean search_toolbar_visible;
    private AudioListViewModel audioListViewModel;
    private FrameLayout progress_bar;
    private String request_code;
    private Bundle bundle;
    private AudioFragmentListener audioFragmentListener;

    public static AlbumDetailsDialog getInstance(String request_code, String albumID, String album_name) {
        AlbumDetailsDialog albumDetailsDialog = new AlbumDetailsDialog();
        Bundle bundle = new Bundle();
        bundle.putString("request_code", request_code);
        bundle.putString("albumID", albumID);
        bundle.putString("album_name", album_name);
        albumDetailsDialog.setArguments(bundle);
        return albumDetailsDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        AppCompatActivity activity = (AppCompatActivity) context;
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
            albumID = bundle.getString("albumID");
            album_name = bundle.getString("album_name");
            request_code = bundle.getString("request_code");
        }
        list_popupwindowpojos = new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 2));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v;
        v = inflater.inflate(R.layout.fragment_album_details, container, false);
        TextView dialog_title = v.findViewById(R.id.album_details_panel_title_TextView);
        dialog_title.setText(album_name);

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
                int size = audio_list.size();
                if (audioListViewModel.audio_pojo_selected_items.size() < size) {
                    audioListViewModel.audio_pojo_selected_items = new IndexedLinkedHashMap<>();
                    //audioListViewModel.audio_selected_array=new ArrayList<>();
                    for (int i = 0; i < size; ++i) {
                        audioListViewModel.audio_pojo_selected_items.put(i, audio_list.get(i));
                        //audioListViewModel.audio_selected_array.add(audio_list.get(i));
                    }

                    audioListRecyclerViewAdapter.notifyDataSetChanged();
                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                    all_select_btn.setImageResource(R.drawable.deselect_icon);
                } else {
                    clear_selection();
                }

                file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + num_all_audio);
            }
        });

        file_number_view = v.findViewById(R.id.album_details_file_number);
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
                audioListRecyclerViewAdapter.getFilter().filter(s.toString());
            }
        });

        ImageButton search_cancel_btn = v.findViewById(R.id.album_details_search_view_cancel_button);
        search_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_visibility_searchbar(false);
            }
        });

        empty_audio_list_tv = v.findViewById(R.id.album_details_empty_list_tv);

        selected_album_recyclerview = v.findViewById(R.id.album_details_recyclerview);
        ItemSeparatorDecoration itemSeparatorDecoration = new ItemSeparatorDecoration(context, 1, selected_album_recyclerview);
        selected_album_recyclerview.addItemDecoration(itemSeparatorDecoration);
        FastScrollerView fastScrollerView = v.findViewById(R.id.fastScroller_album_detail);
        fastScrollerView.setRecyclerView(selected_album_recyclerview);
        selected_album_recyclerview.setLayoutManager(new LinearLayoutManager(context));
        selected_album_recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(context, 4, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH);
        int[] drawables = {R.drawable.delete_icon, R.drawable.play_icon, R.drawable.add_list_icon, R.drawable.overflow_icon};
        String[] titles = {getString(R.string.delete), getString(R.string.play), getString(R.string.list), getString(R.string.more)};
        tb_layout.setResourceImageDrawables(drawables, titles);
        bottom_toolbar = v.findViewById(R.id.album_details_bottom_toolbar);

        bottom_toolbar.addView(tb_layout);
        delete_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        play_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        add_list_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        overflow_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_4);

        ToolbarButtonClickListener toolbarButtonClickListener = new ToolbarButtonClickListener();
        delete_btn.setOnClickListener(toolbarButtonClickListener);
        play_btn.setOnClickListener(toolbarButtonClickListener);
        add_list_btn.setOnClickListener(toolbarButtonClickListener);
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
        AlbumPOJO albumPOJO = new AlbumPOJO(albumID, "", null, null, null);
        audioListViewModel.listAudio(Collections.singletonList(albumPOJO), null, null);
        audioListViewModel.isAudioFetchingFromAlbumFinished.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    audio_list = audioListViewModel.audio_list;
                    total_audio_list = audioListViewModel.audio_list;
                    audioListRecyclerViewAdapter = new AudioListRecyclerViewAdapter();
                    selected_album_recyclerview.setAdapter(audioListRecyclerViewAdapter);

                    num_all_audio = (total_audio_list == null) ? 0 : total_audio_list.size();
                    if (num_all_audio == 0) {
                        selected_album_recyclerview.setVisibility(View.GONE);
                        empty_audio_list_tv.setVisibility(View.VISIBLE);
                    }
                    file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + num_all_audio);
                }
            }
        });

        audioListViewModel.isSavingAudioFinished.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (audioFragmentListener != null) {
                        audioFragmentListener.onAudioSave();
                        audioFragmentListener.refreshAudioPlayNavigationButtons();
                    }

                    clear_selection();
                    audioListViewModel.isSavingAudioFinished.setValue(AsyncTaskStatus.NOT_YET_STARTED);
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
        file_number_view.setText(size + "/" + num_all_audio);

        //hide keyboard when coming from search list of albumlist dialog
        if (audioFragmentListener != null) {
            audioFragmentListener.hideKeyBoard();
        }


        DeleteAudioViewModel deleteAudioViewModel = new ViewModelProvider(AlbumDetailsDialog.this).get(DeleteAudioViewModel.class);
        deleteAudioViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!deleteAudioViewModel.deleted_audio_files.isEmpty()) {
                        audio_list.removeAll(deleteAudioViewModel.deleted_audio_files);
                        total_audio_list.removeAll(deleteAudioViewModel.deleted_audio_files);
                        num_all_audio = total_audio_list.size();
                        if (num_all_audio == 0) {
                            selected_album_recyclerview.setVisibility(View.GONE);
                            empty_audio_list_tv.setVisibility(View.VISIBLE);
                        }

                        if (audioFragmentListener != null) {
                            audioFragmentListener.onDeleteAudio(deleteAudioViewModel.deleted_audio_files);
                            audioFragmentListener.refreshAudioPlayNavigationButtons();
                        }

                    }
                    clear_selection();
                    deleteAudioViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });


        getParentFragmentManager().setFragmentResultListener(SAVE_AUDIO_LIST_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAVE_AUDIO_LIST_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    String list_name = result.getString("list_name");
                    audioListViewModel.save_audio(list_name.isEmpty() ? "q" : "s", list_name);
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
                    deleteAudioViewModel.deleteAudioPOJO(true, audioListViewModel.audios_selected_for_delete, tree_uri, tree_uri_path);
                }

            }
        });

        return v;
    }

    public void clear_selection() {
        //audioListViewModel.audio_selected_array=new ArrayList<>();
        audioListViewModel.audio_pojo_selected_items = new IndexedLinkedHashMap<>();
        if (audioListRecyclerViewAdapter != null)
            audioListRecyclerViewAdapter.notifyDataSetChanged();
        bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
        toolbar_visible = false;
        scroll_distance = 0;
        file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + num_all_audio);
        all_select_btn.setImageResource(R.drawable.select_icon);
    }

    private void enable_disable_buttons(boolean enable) {
        if (enable) {
            delete_btn.setAlpha(Global.ENABLE_ALFA);
            play_btn.setAlpha(Global.ENABLE_ALFA);
            add_list_btn.setAlpha(Global.ENABLE_ALFA);
            overflow_btn.setAlpha(Global.ENABLE_ALFA);
        } else {
            delete_btn.setAlpha(Global.DISABLE_ALFA);
            play_btn.setAlpha(Global.DISABLE_ALFA);
            add_list_btn.setAlpha(Global.DISABLE_ALFA);
            overflow_btn.setAlpha(Global.DISABLE_ALFA);
        }
        delete_btn.setEnabled(enable);
        play_btn.setEnabled(enable);
        add_list_btn.setEnabled(enable);
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
            audioListRecyclerViewAdapter.getFilter().filter(null);
        }

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
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
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

    private class AudioListRecyclerViewAdapter extends RecyclerView.Adapter<AudioListRecyclerViewAdapter.ViewHolder> implements Filterable {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new ViewHolder(new AudioListRecyclerViewItem(context, true));
        }

        @Override
        public void onBindViewHolder(AudioListRecyclerViewAdapter.ViewHolder p1, int p2) {
            AudioPOJO audio = audio_list.get(p2);
            String title = audio.getTitle();
            String album = getString(R.string.album_colon) + " " + audio.getAlbum();
            long duration = 0L;
            String duration_string = audio.getDuration();
            if (duration_string != null) duration = Long.parseLong(duration_string);
            String duration_str = getString(R.string.duration) + " " + (String.format("%d:%02d", duration / 1000 / 60, duration / 1000 % 60));
            String artist = getString(R.string.artists_colon) + " " + audio.getArtist();
            boolean item_selected = audioListViewModel.audio_pojo_selected_items.containsKey(p2);
            p1.view.setData(albumID, title, album, duration_str, artist, item_selected);
            p1.view.setSelected(item_selected);
        }

        @Override
        public int getItemCount() {
            return audio_list.size();
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
                    audio_list = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        audio_list = total_audio_list;
                    } else {
                        String pattern = constraint.toString().toLowerCase().trim();
                        for (int i = 0; i < num_all_audio; ++i) {
                            AudioPOJO audioPOJO = total_audio_list.get(i);
                            if (audioPOJO.getLowerTitle().contains(pattern)) {
                                audio_list.add(audioPOJO);
                            }
                        }
                    }

                    int t = audio_list.size();
                    if (!audioListViewModel.audio_pojo_selected_items.isEmpty()) {
                        clear_selection();
                    } else {
                        notifyDataSetChanged();
                    }

                    file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size() + "/" + t);

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
                    AudioPOJO audio = audio_list.get(pos);
                    File f = new File(audio.getData());
                    if (f.exists()) {
                        if (!audioListViewModel.whether_audios_set_to_current_list || AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty()) {
                            AudioPlayerService.AUDIO_QUEUED_ARRAY = audio_list;
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
                    v.setSelected(false);
                    ((AudioListRecyclerViewItem) v).set_selected(false);
                    //audioListViewModel.audio_selected_array.remove(audio_list.get(pos));
                    --size;
                    if (size >= 1) {
                        bottom_toolbar.setVisibility(View.VISIBLE);
                        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                        toolbar_visible = true;
                        scroll_distance = 0;
                    }

                    if (size == 0) {
                        bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                        toolbar_visible = false;
                        scroll_distance = 0;
                        all_select_btn.setImageResource(R.drawable.select_icon);
                    }
                } else {
                    audioListViewModel.audio_pojo_selected_items.put(pos, audio_list.get(pos));
                    v.setSelected(true);
                    ((AudioListRecyclerViewItem) v).set_selected(true);
                    //audioListViewModel.audio_selected_array.add(audio_list.get(pos));
                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                    ++size;
                    if (size == num_all_audio) {
                        all_select_btn.setImageResource(R.drawable.deselect_icon);
                    }

                }
                file_number_view.setText(size + "/" + num_all_audio);
            }

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
            final Bundle bundle = new Bundle();
            final ArrayList<String> files_selected_array = new ArrayList<>();

            int id = p1.getId();
            if (id == R.id.toolbar_btn_1) {

                if (!AllAudioListFragment.FULLY_POPULATED) {
                    Global.print(context, getString(R.string.wait_till_all_audios_populated));
                    return;
                }

                audioListViewModel.audios_selected_for_delete = new ArrayList<>();
                int size = audioListViewModel.audio_pojo_selected_items.size();
                for (int i = 0; i < size; ++i) {
                    AudioPOJO audio = audioListViewModel.audio_pojo_selected_items.getValueAtIndex(i);
                    files_selected_array.add(audio.getData());
                    audioListViewModel.audios_selected_for_delete.add(audio);

                }
                final DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE, files_selected_array, FileObjectType.SEARCH_LIBRARY_TYPE);
                deleteFileAlertDialogOtherActivity.show(getParentFragmentManager(), "deletefilealertdialog");

            } else if (id == R.id.toolbar_btn_2) {
                AudioPlayerService.AUDIO_QUEUED_ARRAY = new ArrayList<>(audioListViewModel.audio_pojo_selected_items.values());
                if (!AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty()) {
                    AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
                    AudioPOJO audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
                    File f = new File(audio.getData());
                    if (f.exists()) {
                        bundle.putParcelable("audio", audio);
                        getParentFragmentManager().setFragmentResult(request_code, bundle);
                    }

                }
                if (audioFragmentListener != null) {
                    audioFragmentListener.refreshAudioPlayNavigationButtons();
                }

                clear_selection();
            } else if (id == R.id.toolbar_btn_3) {

                AudioSaveListDialog audioSaveListDialog = AudioSaveListDialog.getInstance(SAVE_AUDIO_LIST_REQUEST_CODE);
                audioSaveListDialog.show(getParentFragmentManager(), "");
            } else if (id == R.id.toolbar_btn_4) {
                Global.SHOW_LIST_POPUP_WINDOW_BOTTOM(bottom_toolbar, listPopWindow, Global.FOUR_DP);
            }
        }

    }

}
