package svl.kadatha.filex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;

public class PropertiesDialog extends DialogFragment {
    public static final String PROPERTIES_DIALOG_REQUEST_CODE = "properties_dialog_request_code";
    private Context context;
    private TextView no_files_textview;
    private TextView size_files_textview;
    private String filename_str, file_path_str, file_type_str, file_no_str, file_size_str, file_date_str, file_permissions_str, symbolic_link_str, readable_str, writable_str, hidden_str;
    private ArrayList<String> files_selected_array = new ArrayList<>();
    private FileObjectType fileObjectType;

    public static PropertiesDialog getInstance(ArrayList<String> files_selected_array, FileObjectType fileObjectType) {
        PropertiesDialog propertiesDialog = new PropertiesDialog();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("files_selected_array", files_selected_array);
        bundle.putSerializable("fileObjectType", fileObjectType);
        propertiesDialog.setArguments(bundle);
        return propertiesDialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle bundle = getArguments();
        files_selected_array = bundle.getStringArrayList("files_selected_array");
        int size = files_selected_array.size();
        String source_folder = new File(files_selected_array.get(0)).getParent();
        fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);

        if (files_selected_array.size() == 1) {
            if (fileObjectType == FileObjectType.FILE_TYPE || fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                File file = new File(files_selected_array.get(0));
                filename_str = file.getName();
                file_path_str = file.getAbsolutePath();
                file_date_str = Global.SDF.format(file.lastModified());
                file_type_str = file.isDirectory() ? getString(R.string.directory) : getString(R.string.file);
                getPermissions(file);

                readable_str = file.canRead() ? getString(R.string.yes) : getString(R.string.no);
                writable_str = file.canWrite() ? getString(R.string.yes) : getString(R.string.no);
                hidden_str = file.isHidden() ? getString(R.string.yes) : getString(R.string.no);
            } else {
                FilePOJO filePOJO=FilePOJOUtil.GET_FILE_POJO(files_selected_array.get(0),fileObjectType);
                if(filePOJO!=null){
                    filename_str = filePOJO.getName();
                    file_path_str = files_selected_array.get(0);
                    file_date_str=filePOJO.getDate();
                    file_type_str = filePOJO.getIsDirectory() ? getString(R.string.directory) : getString(R.string.file);

                    //getPermissions(file);
                    readable_str = getString(R.string.yes);
                    writable_str = getString(R.string.yes);
                    hidden_str = getString(R.string.yes);
                } else{
                    filename_str = new File(files_selected_array.get(0)).getName();
                    file_path_str = files_selected_array.get(0);
                    //file_date_str=sdf.format(ftpFile.)
                    file_type_str = "";//ftpFile.isDirectory() ? getString(R.string.directory) : getString(R.string.file);

                    //getPermissions(file);
                    readable_str = getString(R.string.yes);
                    writable_str = getString(R.string.yes);
                    hidden_str = getString(R.string.yes);
                }

            }
        } else if (files_selected_array.size() > 1) {
            filename_str = files_selected_array.size() + " " + getString(R.string.files);
            file_path_str = new File(files_selected_array.get(0)).getParent();
            if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                file_path_str = "NA";
            }
            file_date_str = "NA";
            file_type_str = "NA";
            symbolic_link_str = "NA";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_properties, container, false);
        //private TextView permissions;
        TableLayout properties_details_table_layout = v.findViewById(R.id.fragment_properties_details_table_layout);

        int heightPx = (int) (0.5f * context.getResources().getDisplayMetrics().density);
        //int marginPx = context.getResources().getDimensionPixelSize(R.dimen.divider_margin);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.recycler_second_text_color, typedValue, true);
        int separatorColor = ContextCompat.getColor(context, typedValue.resourceId);

        for (int i = 0; i < 6; ++i) {
            View row_item = inflater.inflate(R.layout.properties_item_view_layout, null);
            TextView label = row_item.findViewById(R.id.properties_label);
            final TextView property = row_item.findViewById(R.id.properties_description);

            switch (i) {
                case 0:
                    label.setText(R.string.name);
                    property.setText(filename_str);
                    break;
                case 1:
                    label.setText(R.string.path);
                    property.setText(file_path_str);
                    property.setPaintFlags(property.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    property.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clipData = ClipData.newPlainText("Path copied", property.getText());
                            clipboardManager.setPrimaryClip(clipData);
                            Global.print(context, getString(R.string.path_copied));
                        }
                    });
                    break;

                case 2:
                    label.setText(R.string.type);
                    property.setText(file_type_str);
                    break;
                case 3:
                    no_files_textview = row_item.findViewById(R.id.properties_description);
                    label.setText(R.string.total_files);
                    no_files_textview.setText(file_no_str);
                    break;
                case 4:
                    size_files_textview = row_item.findViewById(R.id.properties_description);
                    label.setText(R.string.size);
                    size_files_textview.setText(file_size_str);
                    break;
                case 5:
                    label.setText(R.string.modified);
                    property.setText(file_date_str);
                    break;
            }
            properties_details_table_layout.addView(row_item);

            /// Add separator except after last item
            if (i < 5) {
                View separator = new View(context);
                TableLayout.LayoutParams params = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        heightPx
                );
                separator.setLayoutParams(params);
                separator.setBackgroundColor(separatorColor);
                properties_details_table_layout.addView(separator);
            }
        }

        TableLayout properties_rwh_table_layout = v.findViewById(R.id.fragment_properties_rwh_tablelayout);
        properties_rwh_table_layout.setVisibility((files_selected_array.size() == 1 || fileObjectType == FileObjectType.USB_TYPE) ? View.VISIBLE : View.GONE);

        for (int i = 0; i < 5; ++i) {
            View row_item = inflater.inflate(R.layout.properties_item_view_layout, null);
            TextView label = row_item.findViewById(R.id.properties_label);
            TextView property = row_item.findViewById(R.id.properties_description);
            switch (i) {
                case 0:
                    label.setText(R.string.readable);
                    property.setText(readable_str);
                    break;
                case 1:
                    label.setText(R.string.writable);
                    property.setText(writable_str);
                    break;
                case 2:
                    label.setText(R.string.hidden);
                    property.setText(hidden_str);
                    break;
                case 3:
                    label.setText(R.string.permissions);
                    property.setText(file_permissions_str);
                    property.setPaintFlags(property.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    property.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (file_permissions_str == null || file_permissions_str.isEmpty()) {
                                return;
                            }

                            PermissionsDialog permissionsDialog = PermissionsDialog.getInstance(file_path_str, file_permissions_str, symbolic_link_str);
                            permissionsDialog.show(getParentFragmentManager().beginTransaction(), "permissions_dialog");
                        }
                    });
                    break;
                case 4:
                    label.setText(R.string.symbolic_link);
                    property.setText(symbolic_link_str);
                    break;
            }

            properties_rwh_table_layout.addView(row_item);

            /// Add separator except after last item
            if (i < 4) {
                View separator = new View(context);
                TableLayout.LayoutParams params = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        heightPx
                );
                separator.setLayoutParams(params);
                separator.setBackgroundColor(separatorColor);
                properties_rwh_table_layout.addView(separator);
            }
        }

        ViewModelFileCount.ViewModelFileCountFactory factory = new ViewModelFileCount.ViewModelFileCountFactory(context, files_selected_array, fileObjectType);
        ViewModelFileCount viewModel = new ViewModelProvider(this, factory).get(ViewModelFileCount.class);

        viewModel.total_no_of_files.observe(this, new androidx.lifecycle.Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                no_files_textview.setText(" " + integer);
            }
        });

        viewModel.size_of_files_formatted.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                size_files_textview.setText(" " + s);
            }
        });

        ViewGroup buttons_layout = v.findViewById(R.id.fragment_properties_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context, 1, Global.DIALOG_WIDTH, Global.DIALOG_WIDTH));
        Button OKBtn = buttons_layout.findViewById(R.id.first_button);
        OKBtn.setText(R.string.close);
        OKBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PropertiesDialog.this.getViewModelStore().clear();
                dismissAllowingStateLoss();
            }
        });

        getParentFragmentManager().setFragmentResultListener(PROPERTIES_DIALOG_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(PROPERTIES_DIALOG_REQUEST_CODE)) {
                    String file_path = result.getString("file_path");
                    getPermissions(new File(file_path));
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(Global.DIALOG_WIDTH, LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void getPermissions(File file) {
        BufferedReader buffered_reader;
        String exec;
        if (file.isDirectory()) {
            exec = "ls -d -l ";
        } else {
            exec = "ls -l ";
        }
        try {
            java.lang.Process proc = Runtime.getRuntime().exec(exec + file.getAbsolutePath());
            buffered_reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;

            while ((line = buffered_reader.readLine()) != null) {
                String[] line_split = line.split("\\s+");
                if (line_split.length > 5) {
                    file_permissions_str = line_split[0];
                }
                line_split = line.split("->");
                if (line_split.length > 1) {
                    symbolic_link_str = "->" + line_split[1];
                } else {
                    symbolic_link_str = "-";
                }
            }
            proc.waitFor();
            buffered_reader.close();
        } catch (Exception e) {

        }
    }
}
