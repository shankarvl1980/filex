package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;

import java.util.ArrayList;

import svl.kadatha.filex.asynctasks.CutCopyAsyncTask;

public class PasteSetUpDialog extends DialogFragment {
    private final static String SAF_PERMISSION_REQUEST_CODE_SOURCE = "paste_set_up_saf_permission_request_code_source";
    private final static String SAF_PERMISSION_REQUEST_CODE_DEST = "paste_set_up_saf_permission_request_code_dest";
    private static final String KEY_CHECKED_SAF_PERMISSION = "checked_saf_permission";
    private Context context;
    private Bundle bundle;
    private boolean isWritable, isSourceFromInternal, cut;
    private String tree_uri_path = "", source_uri_path = "", source_folder, dest_folder;
    private Uri tree_uri, source_uri;
    private ParcelableStringStringLinkedMap sourceFileDestNameMap = new ParcelableStringStringLinkedMap();
    private FileObjectType sourceFileObjectType, destFileObjectType;
    private int size;
    private boolean checkedSAFPermissionPasteSetUp = false;

    public static PasteSetUpDialog getInstance(String source_folder, FileObjectType sourceFileObjectType, String dest_folder, FileObjectType destFileObjectType,
                                               ParcelableStringStringLinkedMap sourceFileDestNameMap, ArrayList<String> overwritten_file_path_list, boolean cut_selected) {
        PasteSetUpDialog pasteSetUpDialog = new PasteSetUpDialog();
        Bundle bundle = new Bundle();
        bundle.putString("source_folder", source_folder);
        bundle.putParcelable("sourceFileDestNameMap", sourceFileDestNameMap);
        bundle.putStringArrayList("overwritten_file_path_list", overwritten_file_path_list);
        bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
        bundle.putSerializable("destFileObjectType", destFileObjectType);
        bundle.putString("dest_folder", dest_folder);
        bundle.putBoolean("cut", cut_selected);
        pasteSetUpDialog.setArguments(bundle);
        return pasteSetUpDialog;
    }

    public static PasteSetUpDialog getInstance(Bundle bundle) {
        PasteSetUpDialog pasteSetUpDialog = new PasteSetUpDialog();
        pasteSetUpDialog.setArguments(bundle);
        return pasteSetUpDialog;
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

        bundle = getArguments();
        if (bundle != null) {
            sourceFileDestNameMap = bundle.getParcelable("sourceFileDestNameMap");
            cut = bundle.getBoolean("cut");
            source_folder = bundle.getString("source_folder");
            dest_folder = bundle.getString("dest_folder");
            sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
            destFileObjectType = (FileObjectType) bundle.getSerializable("destFileObjectType");
            size = sourceFileDestNameMap.size();
        } else {
            dismissAllowingStateLoss();
        }

        if (sourceFileDestNameMap.isEmpty()) {
            dismissAllowingStateLoss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            checkedSAFPermissionPasteSetUp = savedInstanceState.getBoolean(KEY_CHECKED_SAF_PERMISSION, false);
        }

        if (!checkedSAFPermissionPasteSetUp) {
            if (check_permission_for_source(source_folder, sourceFileObjectType)) {
                if (check_permission_for_destination(dest_folder, destFileObjectType)) {
                    initiate_startActivity();
                }
            }
            checkedSAFPermissionPasteSetUp = true;
        }

        getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE_DEST, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE_DEST)) {
                    tree_uri = result.getParcelable("tree_uri");
                    tree_uri_path = result.getString("tree_uri_path");
                    if (check_permission_for_destination(dest_folder, destFileObjectType) && check_permission_for_source(source_folder, sourceFileObjectType)) {
                        initiate_startActivity();
                    }
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE_SOURCE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE_SOURCE)) {
                    source_uri = result.getParcelable("tree_uri");
                    source_uri_path = result.getString("tree_uri_path");
                    if (check_permission_for_destination(dest_folder, destFileObjectType) && check_permission_for_source(source_folder, sourceFileObjectType)) {
                        initiate_startActivity();
                    }
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(SAFPermissionHelperDialog.SAF_PERMISSION_CANCEL_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAFPermissionHelperDialog.SAF_PERMISSION_CANCEL_REQUEST_CODE)) {
                    Global.print(context, getString(R.string.permission_not_granted));
                    dismissAllowingStateLoss();
                }
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CHECKED_SAF_PERMISSION, checkedSAFPermissionPasteSetUp);
    }

    private boolean check_permission_for_source(String file_path, FileObjectType fileObjectType) {
        if (!cut) {
            return true;
        }

        if (fileObjectType == FileObjectType.FILE_TYPE) {
            isSourceFromInternal = FileUtil.isFromInternal(fileObjectType, file_path);
            if (isSourceFromInternal) {
                return true;
            } else {
                return check_SAF_permission_source(file_path, fileObjectType);
            }
        } else if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
            for (int n = 0; n < size; ++n) {
                file_path = sourceFileDestNameMap.getKeyAtIndex(n);
                if (!FileUtil.isFromInternal(FileObjectType.FILE_TYPE, file_path)) {
                    if (!check_SAF_permission_source(file_path, FileObjectType.FILE_TYPE)) {
                        return false;
                    }
                }
            }
            return true;
        } else if (fileObjectType == FileObjectType.ROOT_TYPE) {
            if (!RootUtils.canRunRootCommands()) {
                Global.print(context, getString(R.string.root_access_not_avaialable));
                dismissAllowingStateLoss();
                return false;
            } else {
                return true;
            }

        }
        return true; //this needs to be true, after success checking of permission of in searchlibrarytype to return true
    }

    private boolean check_permission_for_destination(String file_path, FileObjectType fileObjectType) {
        if (fileObjectType == FileObjectType.FILE_TYPE) {
            isWritable = FileUtil.isWritable(fileObjectType, file_path);
            if (isWritable) {
                return true;
            } else {
                return check_SAF_permission_destination(file_path, fileObjectType);
            }
        } else if (fileObjectType == FileObjectType.USB_TYPE) {
            return true;
        } else if (fileObjectType == FileObjectType.FTP_TYPE) {
            return true;
        } else if (fileObjectType == FileObjectType.ROOT_TYPE) {
            if (!RootUtils.canRunRootCommands()) {
                Global.print(context, getString(R.string.root_access_not_avaialable));
                dismissAllowingStateLoss();
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    private boolean check_SAF_permission_destination(String parent_file_path, FileObjectType fileObjectType) {
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path, fileObjectType);
        if (uriPOJO != null) {
            tree_uri_path = uriPOJO.get_path();
            tree_uri = uriPOJO.get_uri();
        }


        if (uriPOJO == null || tree_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE_DEST, parent_file_path, fileObjectType);
            safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
            return false;
        } else {
            return true;
        }
    }

    private boolean check_SAF_permission_source(String parent_file_path, FileObjectType fileObjectType) {
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path, fileObjectType);
        if (uriPOJO != null) {
            source_uri_path = uriPOJO.get_path();
            source_uri = uriPOJO.get_uri();
        }


        if (source_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE_SOURCE, parent_file_path, fileObjectType);
            safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
            return false;
        } else {
            return true;
        }
    }


    private void initiate_startActivity() {
        if (sourceFileDestNameMap.isEmpty()) {
            Global.print(context, getString(R.string.could_not_perform_action));
            dismissAllowingStateLoss();
            return;
        }

        if (!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(sourceFileObjectType, destFileObjectType)) {
            Global.print(context, getString(R.string.wait_till_completion_on_going_operation_on_usb));
            dismissAllowingStateLoss();
            return;
        }

        Class emptyService = ArchiveDeletePasteServiceUtil.getEmptyService(context);
        if (emptyService == null) {
            Global.print(context, getString(R.string.maximum_3_services_processed));
            dismissAllowingStateLoss();
            return;
        }

        Intent intent = new Intent(context, emptyService);
        intent.setAction(cut ? CutCopyAsyncTask.TASK_TYPE_CUT : CutCopyAsyncTask.TASK_TYPE_COPY);
        bundle.putString("tree_uri_path", tree_uri_path);
        bundle.putString("source_uri_path", source_uri_path);
        bundle.putParcelable("tree_uri", tree_uri);
        bundle.putParcelable("source_uri", source_uri);
        bundle.putBoolean("isWritable", isWritable);
        bundle.putBoolean("isSourceFromInternal", isSourceFromInternal);
        intent.putExtra("bundle", bundle);
        context.startActivity(intent);
        dismissAllowingStateLoss();
    }
}
