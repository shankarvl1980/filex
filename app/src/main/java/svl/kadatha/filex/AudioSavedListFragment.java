package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AudioSavedListFragment extends Fragment {

    private static final String AUDIO_SELECT_REQUEST_CODE = "audio_saved_list_audio_select_request_code";
    public AudioListViewModel audioListViewModel;
    public FrameLayout progress_bar;
    private Context context;
    private AudioSavedListRecyclerAdapter audio_saved_list_adapter;
    private List<String> saved_audio_list;
    private Button play_btn, remove_btn;
    private Button all_select_btn;
    private TextView file_number_view;
    private Toolbar bottom_toolbar;
    private AudioSelectListener audioSelectListener;
    private AudioFragmentListener audioFragmentListener;
    private boolean toolbar_visible = true;
    private int scroll_distance;
    private int num_all_audio_list;
    private AudioPlayerActivity.AudioChangeListener audioChangeListener;
    private AppCompatActivity activity;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        audioChangeListener = new AudioPlayerActivity.AudioChangeListener() {
            @Override
            public void onAudioChange() {
                AudioSavedListDetailsDialog audioSavedListDetailsDialog = (AudioSavedListDetailsDialog) getParentFragmentManager().findFragmentByTag("audioSavedListDetailsDialog");
                if (audioSavedListDetailsDialog != null) {
                    audioSavedListDetailsDialog.onAudioChange();
                }
            }
        };

        activity = (AppCompatActivity) context;
        if (activity instanceof AudioPlayerActivity) {
            ((AudioPlayerActivity) activity).addAudioChangeListener(audioChangeListener);
        }

        if (activity instanceof AudioSelectListener) {
            audioSelectListener = (AudioSelectListener) activity;
        }

        if (activity instanceof AudioFragmentListener) {
            audioFragmentListener = (AudioFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activity instanceof AudioPlayerActivity) {
            ((AudioPlayerActivity) activity).removeAudioChangeListener(audioChangeListener);
        }
        audioSelectListener = null;
        audioFragmentListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_audio_saved_list, container, false);
        bottom_toolbar = v.findViewById(R.id.audio_saved_list_bottom_toolbar);
        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(context, 3, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.play_icon, R.drawable.remove_list_icon, R.drawable.select_icon};
        String[] titles = {getString(R.string.play), getString(R.string.remove), getString(R.string.select)};
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);

        bottom_toolbar.addView(tb_layout);
        play_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        remove_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        all_select_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);

        ToolbarClickListener toolbarClickListener = new ToolbarClickListener();
        play_btn.setOnClickListener(toolbarClickListener);
        remove_btn.setOnClickListener(toolbarClickListener);
        all_select_btn.setOnClickListener(toolbarClickListener);

        file_number_view = v.findViewById(R.id.audio_saved_list_file_number);
        progress_bar = v.findViewById(R.id.audio_saved_list_progressbar);
        RecyclerView audio_recycler_view = v.findViewById(R.id.fragment_audio_saved_list_recyclerview);
        audio_recycler_view.addItemDecoration(Global.DIVIDERITEMDECORATION);
        audio_recycler_view.setLayoutManager(new LinearLayoutManager(context));
        audio_recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener() {

            final int threshold = 5;

            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (scroll_distance > threshold && toolbar_visible) {

                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    toolbar_visible = false;
                    scroll_distance = 0;
                } else if (scroll_distance < -threshold && !toolbar_visible) {

                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                }

                if ((toolbar_visible && dy > 0) || (!toolbar_visible && dy < 0)) {
                    scroll_distance += dy;
                }

            }

        });

        saved_audio_list = new ArrayList<>();
        saved_audio_list.add(AudioPlayerActivity.CURRENT_PLAY_LIST);
        saved_audio_list.addAll(AudioPlayerActivity.AUDIO_SAVED_LIST);
        audio_saved_list_adapter = new AudioSavedListRecyclerAdapter();
        audio_recycler_view.setAdapter(audio_saved_list_adapter);
        progress_bar.setVisibility(View.GONE);
        num_all_audio_list = saved_audio_list.size();

        audioListViewModel = new ViewModelProvider(this).get(AudioListViewModel.class);
        audioListViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    AudioPlayerService.AUDIO_QUEUED_ARRAY = audioListViewModel.audio_list;
                    if (audioSelectListener != null && !AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty()) {
                        AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
                        AudioPOJO audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
                        Uri data = null;
                        File f = new File(audio.getData());
                        if (f.exists()) {
                            data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", f);
                        }

                        audioSelectListener.onAudioSelect(data, audio);

                    }
                    if (audioFragmentListener != null) {
                        audioFragmentListener.refreshAudioPlayNavigationButtons();
                    }

                    clear_selection();
                    audioListViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(AUDIO_SELECT_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(AUDIO_SELECT_REQUEST_CODE)) {
                    AudioPOJO audio = result.getParcelable("audio");
                    long id = audio.getId();
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


        int size = audioListViewModel.audio_saved_list_selected_items.size();
        enable_disable_buttons(size != 0);

        file_number_view.setText(size + "/" + num_all_audio_list);
        return v;
    }


    private void enable_disable_buttons(boolean enable) {
        if (enable) {
            play_btn.setAlpha(Global.ENABLE_ALFA);
            remove_btn.setAlpha(Global.ENABLE_ALFA);
        } else {
            play_btn.setAlpha(Global.DISABLE_ALFA);
            remove_btn.setAlpha(Global.DISABLE_ALFA);
        }
        play_btn.setEnabled(enable);
        remove_btn.setEnabled(enable);

    }


    public void onSaveAudioList() {
        saved_audio_list = new ArrayList<>();
        saved_audio_list.add(AudioPlayerActivity.CURRENT_PLAY_LIST);
        saved_audio_list.addAll(AudioPlayerActivity.AUDIO_SAVED_LIST);
        clear_selection();

    }

    public void clear_selection() {
        audioListViewModel.audio_saved_list_selected_items = new IndexedLinkedHashMap<>();
        //audioListViewModel.audio_list_selected_array=new ArrayList<>();
        if (audio_saved_list_adapter != null) {
            audio_saved_list_adapter.notifyDataSetChanged();
        }
        enable_disable_buttons(false);
        file_number_view.setText(audioListViewModel.audio_saved_list_selected_items.size() + "/" + num_all_audio_list);
        all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.select_icon, 0, 0);
    }

    private class ToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View p1) {
            if (progress_bar.getVisibility() == View.VISIBLE) {
                Global.print(context, getString(R.string.please_wait));
                return;
            }
            int id = p1.getId();
            if (id == R.id.toolbar_btn_1) {
                if (!audioListViewModel.audio_saved_list_selected_items.isEmpty()) {
                    {
                        progress_bar.setVisibility(View.VISIBLE);
                        audioListViewModel.fetch_saved_audio_list(new ArrayList<>(audioListViewModel.audio_saved_list_selected_items.values()));

                    }

                }

            } else if (id == R.id.toolbar_btn_2) {
                if (!audioListViewModel.audio_saved_list_selected_items.isEmpty()) {
                    if (audioListViewModel.audio_saved_list_selected_items.containsValue(AudioPlayerActivity.CURRENT_PLAY_LIST)) {
                        AudioPlayerService.AUDIO_QUEUED_ARRAY = new ArrayList<>();
                        int size = audioListViewModel.audio_saved_list_selected_items.size();
                        for (int i = 0; i < size; ++i) {
                            String list_name = audioListViewModel.audio_saved_list_selected_items.getValueAtIndex(i);
                            Integer key = audioListViewModel.audio_saved_list_selected_items.getKeyAtIndex(i);
                            if (list_name.equals(AudioPlayerActivity.CURRENT_PLAY_LIST)) {
                                audioListViewModel.audio_saved_list_selected_items.remove(key);
                                break;
                            }
                        }

                    }
                    for (String list_name : audioListViewModel.audio_saved_list_selected_items.values()) {
                        AudioPlayerActivity.AUDIO_SAVED_LIST.remove(list_name);
                        saved_audio_list.remove(list_name);
                        if (activity instanceof AudioPlayerActivity) {
                            ((AudioPlayerActivity) activity).audioDatabaseHelper.deleteTable(list_name);
                        }

                    }

                    num_all_audio_list = saved_audio_list.size();
                    clear_selection();
                }

            } else if (id == R.id.toolbar_btn_3) {
                if (audioListViewModel.audio_saved_list_selected_items.size() < num_all_audio_list) {
                    audioListViewModel.audio_saved_list_selected_items = new IndexedLinkedHashMap<>();
                    //audioListViewModel.audio_saved_list_selected_items =new IndexedLinkedHashMap<>();

                    for (int i = 0; i < num_all_audio_list; ++i) {
                        audioListViewModel.audio_saved_list_selected_items.put(i, saved_audio_list.get(i));
                        //audioListViewModel.audio_list_selected_array.add();
                    }
                    all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.deselect_icon, 0, 0);
                    audio_saved_list_adapter.notifyDataSetChanged();
                } else {
                    clear_selection();

                }

                int s = audioListViewModel.audio_saved_list_selected_items.size();
                if (s >= 1) {
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                    enable_disable_buttons(true);
                }
                file_number_view.setText(s + "/" + num_all_audio_list);
            } else {
                clear_selection();
            }

            if (audioFragmentListener != null) {
                audioFragmentListener.refreshAudioPlayNavigationButtons();
            }

        }

    }

    private class AudioSavedListRecyclerAdapter extends RecyclerView.Adapter<AudioSavedListRecyclerAdapter.ViewHolder> {
        int first_line_font_size, second_line_font_size;

        @Override
        public AudioSavedListRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            View itemview = LayoutInflater.from(context).inflate(R.layout.working_dir_recyclerview_layout, p1, false);
            return new ViewHolder(itemview);
        }

        @Override
        public void onBindViewHolder(AudioSavedListRecyclerAdapter.ViewHolder p1, int p2) {
            if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 0) {
                first_line_font_size = Global.FONT_SIZE_SMALL_FIRST_LINE;
                second_line_font_size = Global.FONT_SIZE_SMALL_DETAILS_LINE;

            } else if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 2) {
                first_line_font_size = Global.FONT_SIZE_LARGE_FIRST_LINE;
                second_line_font_size = Global.FONT_SIZE_LARGE_DETAILS_LINE;
            } else {
                first_line_font_size = Global.FONT_SIZE_MEDIUM_FIRST_LINE;
                second_line_font_size = Global.FONT_SIZE_SMALL_DETAILS_LINE;
            }


            p1.textView.setTextSize(first_line_font_size);
            p1.textView.setText(saved_audio_list.get(p2));
            boolean item_selected = audioListViewModel.audio_saved_list_selected_items.containsKey(p2);
            p1.view.setSelected(item_selected);
            p1.select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);

        }

        @Override
        public int getItemCount() {
            num_all_audio_list = saved_audio_list.size();
            return num_all_audio_list;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View view;
            final TextView textView;
            final ImageView select_indicator;
            int pos;

            ViewHolder(View view) {
                super(view);
                this.view = view;
                textView = view.findViewById(R.id.working_dir_name);
                select_indicator = view.findViewById(R.id.working_dir_select_indicator);
                select_indicator.setVisibility(View.INVISIBLE);
                view.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View p) {
                        pos = getBindingAdapterPosition();
                        int size = audioListViewModel.audio_saved_list_selected_items.size();
                        if (size > 0) {

                            onLongClickProcedure(p, size);
                        } else {
                            AudioSavedListDetailsDialog audioSavedListDetailsDialog = AudioSavedListDetailsDialog.getInstance(AUDIO_SELECT_REQUEST_CODE, pos, saved_audio_list.get(pos));
                            audioSavedListDetailsDialog.show(getParentFragmentManager(), "audioSavedListDetailsDialog");
                        }
                    }

                });


                view.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View p) {
                        onLongClickProcedure(p, audioListViewModel.audio_saved_list_selected_items.size());
                        return true;

                    }
                });
            }


            private void onLongClickProcedure(View v, int size) {
                pos = getBindingAdapterPosition();
                if (audioListViewModel.audio_saved_list_selected_items.containsKey(pos)) {
                    v.setSelected(false);
                    select_indicator.setVisibility(View.INVISIBLE);
                    //audioListViewModel.audio_list_selected_array.remove(saved_audio_list.get(pos));
                    audioListViewModel.audio_saved_list_selected_items.remove(pos);
                    --size;
                    if (size >= 1) {
                        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                        toolbar_visible = true;
                        scroll_distance = 0;
                        enable_disable_buttons(true);
                    }

                    if (size == 0) {
                        enable_disable_buttons(false);
                        all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.select_icon, 0, 0);
                    }
                } else {
                    v.setSelected(true);
                    select_indicator.setVisibility(View.VISIBLE);
                    //audioListViewModel.audio_list_selected_array.add(saved_audio_list.get(pos));
                    audioListViewModel.audio_saved_list_selected_items.put(pos, saved_audio_list.get(pos));

                    bottom_toolbar.setVisibility(View.VISIBLE);
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    toolbar_visible = true;
                    scroll_distance = 0;
                    enable_disable_buttons(true);
                    ++size;
                    if (size == num_all_audio_list) {
                        all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.deselect_icon, 0, 0);
                    }

                }
                file_number_view.setText(size + "/" + num_all_audio_list);
            }
        }
    }
}
