package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

public class FileEditorSettingsDialog extends DialogFragment {
    final static int MIN_TEXT_SIZE = 10, MAX_TEXT_SIZE = 20;
    FileEditorActivity fileEditorActivity;
    private RadioButton unix_rb, mac_rb, wnd_rb;
    private ImageButton text_size_decrease_btn, text_size_increase_btn;
    private boolean not_wrap;
    private int selected_eol;
    private float selected_text_size;
    private EditText sample_edittext;
    private TextView text_size_tv;
    private Context context;
    private EOL_ChangeListener eol_changeListener;
    private Group eol_group;
    private FileEditorViewModel viewModel;
    private ImageButton eol_group_expander;

    public static FileEditorSettingsDialog getInstance(int selected_eol) {
        FileEditorSettingsDialog fileEditorSettingsDialog = new FileEditorSettingsDialog();
        Bundle bundle = new Bundle();
        bundle.putInt("selected_eol", selected_eol);
        fileEditorSettingsDialog.setArguments(bundle);
        return fileEditorSettingsDialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        fileEditorActivity = ((FileEditorActivity) context);
        eol_changeListener = (FileEditorActivity) context;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        eol_changeListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        not_wrap = FileEditorActivity.NOT_WRAP;
        selected_text_size = FileEditorActivity.FILE_EDITOR_TEXT_SIZE;
        Bundle bundle = getArguments();
        selected_eol = bundle.getInt("selected_eol");
        if (savedInstanceState != null) {
            selected_text_size = savedInstanceState.getFloat("selected_text_size");
            not_wrap = savedInstanceState.getBoolean("not_wrap");
            selected_eol = savedInstanceState.getInt("selected_eol");
        }
        viewModel = new ViewModelProvider(requireActivity()).get(FileEditorViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_file_editor_settings, container, false);
        eol_group_expander = v.findViewById(R.id.file_editor_settings_advanced_expander);
        View advanced_label_group = v.findViewById(R.id.file_editor_settings_advanced_group);

        advanced_label_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewModel.is_eol_group_visible) {
                    eol_group.setVisibility(View.GONE);
                    eol_group_expander.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.down_arrow_dialog_icon));
                } else {
                    eol_group.setVisibility(View.VISIBLE);
                    eol_group_expander.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_dialog_icon));
                }
                viewModel.is_eol_group_visible = !viewModel.is_eol_group_visible;
            }
        });
        eol_group = v.findViewById(R.id.file_editor_settings_eol_group);
        if (viewModel.is_eol_group_visible) {
            eol_group.setVisibility(View.VISIBLE);
            eol_group_expander.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
        }
        RadioGroup eol_rg = v.findViewById(R.id.eol_rg);
        eol_rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup rg, int p1) {
                if (!fileEditorActivity.viewModel.fromThirdPartyApp && !fileEditorActivity.viewModel.isFileBig) {
                    if (unix_rb.isChecked()) {
                        selected_eol = FileEditorActivity.EOL_N;
                    } else if (mac_rb.isChecked()) {
                        selected_eol = FileEditorActivity.EOL_R;
                    } else if (wnd_rb.isChecked()) {
                        selected_eol = FileEditorActivity.EOL_RN;
                    }
                } else {
                    switch (fileEditorActivity.viewModel.eol) {
                        case FileEditorActivity.EOL_N:
                            unix_rb.setChecked(true);
                            break;
                        case FileEditorActivity.EOL_R:
                            mac_rb.setChecked(true);
                            break;
                        case FileEditorActivity.EOL_RN:
                            wnd_rb.setChecked(true);
                            break;

                    }
                    Global.print(context, getString(R.string.cant_edit_this_file));
                }
            }
        });

        unix_rb = v.findViewById(R.id.eol_rb_n);
        mac_rb = v.findViewById(R.id.eol_rb_r);
        wnd_rb = v.findViewById(R.id.eol_rb_rn);

        CheckBox wrap_check_box = v.findViewById(R.id.file_editor_settings_wrap_checkbox);
        wrap_check_box.setChecked(!not_wrap);
        wrap_check_box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                not_wrap = !checked;
            }
        });

        text_size_decrease_btn = v.findViewById(R.id.file_editor_text_size_decrease);
        text_size_increase_btn = v.findViewById(R.id.file_editor_text_size_increase);
        text_size_tv = v.findViewById(R.id.file_editor_settings_text_size);
        text_size_tv.setText(getString(R.string.text_size) + " " + (int) selected_text_size);

        text_size_decrease_btn.setOnTouchListener(new RepeatListener(400, 101, new View.OnClickListener() {
            public void onClick(View p1) {
                if (selected_text_size > MIN_TEXT_SIZE) {
                    selected_text_size--;
                    text_size_tv.setText(getString(R.string.text_size) + " " + (int) selected_text_size);
                    sample_edittext.setTextSize(selected_text_size);
                    enable_disable_btns();
                }


            }
        }));

        text_size_increase_btn.setOnTouchListener(new RepeatListener(400, 101, new View.OnClickListener() {
            public void onClick(View p1) {
                if (selected_text_size < MAX_TEXT_SIZE) {
                    selected_text_size++;
                    text_size_tv.setText(getString(R.string.text_size) + " " + (int) selected_text_size);
                    sample_edittext.setTextSize(selected_text_size);
                    enable_disable_btns();
                }
            }
        }));


        sample_edittext = v.findViewById(R.id.file_editor_settings_sample_text);
        sample_edittext.setTextSize(selected_text_size);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_file_editor_settings_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 2, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button ok_button = buttons_layout.findViewById(R.id.first_button);
        ok_button.setText(R.string.ok);
        ok_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (FileEditorActivity.NOT_WRAP != not_wrap) {
                    FileEditorActivity.NOT_WRAP = not_wrap;
                    fileEditorActivity.tinyDB.putBoolean("file_editor_not_wrap", not_wrap);
                    fileEditorActivity.recreate();

                }

                if (FileEditorActivity.FILE_EDITOR_TEXT_SIZE != selected_text_size) {
                    fileEditorActivity.filetext_container_edittext.setTextSize(selected_text_size);
                    FileEditorActivity.FILE_EDITOR_TEXT_SIZE = selected_text_size;
                    fileEditorActivity.tinyDB.putFloat("file_editor_text_size", FileEditorActivity.FILE_EDITOR_TEXT_SIZE);
                }

                ((FileEditorActivity) context).viewModel.altered_eol = selected_eol;
                if (eol_changeListener != null) {
                    eol_changeListener.onEOLchanged(selected_eol);
                }
                dismissAllowingStateLoss();
            }
        });

        Button cancel_button = buttons_layout.findViewById(R.id.second_button);
        cancel_button.setText(R.string.cancel);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dismissAllowingStateLoss();
            }
        });

        switch (selected_eol) {
            case FileEditorActivity.EOL_N:
                unix_rb.setChecked(true);
                break;
            case FileEditorActivity.EOL_R:
                mac_rb.setChecked(true);
                break;
            case FileEditorActivity.EOL_RN:
                wnd_rb.setChecked(true);
                break;
        }

        enable_disable_btns();
        return v;
    }

    private void enable_disable_btns() {
        if (selected_text_size == MIN_TEXT_SIZE) {
            text_size_decrease_btn.setEnabled(false);
            text_size_decrease_btn.setAlpha(Global.DISABLE_ALFA);
        } else if (selected_text_size == MAX_TEXT_SIZE) {
            text_size_increase_btn.setEnabled(false);
            text_size_increase_btn.setAlpha(Global.DISABLE_ALFA);
        } else if (selected_text_size > MIN_TEXT_SIZE && selected_text_size < MAX_TEXT_SIZE) {
            text_size_decrease_btn.setEnabled(true);
            text_size_decrease_btn.setAlpha(Global.ENABLE_ALFA);

            text_size_increase_btn.setEnabled(true);
            text_size_increase_btn.setAlpha(Global.ENABLE_ALFA);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putFloat("selected_text_size", selected_text_size);
        outState.putBoolean("not_wrap", not_wrap);
        outState.putInt("selected_eol", selected_eol);
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    interface EOL_ChangeListener {
        void onEOLchanged(int eol);
    }
}
