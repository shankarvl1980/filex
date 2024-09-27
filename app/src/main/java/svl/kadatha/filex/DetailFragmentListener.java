package svl.kadatha.filex;

import androidx.fragment.app.Fragment;

public interface DetailFragmentListener {
    void onScrollRecyclerView(boolean showToolBar);

    void actionModeFinish(Fragment fragment, String fileclickeselected);

    void onLongClickItem(int selected_size);

    void setFileNumberView(String file_number_string);

    void createFragmentTransaction(String file_path, FileObjectType fileObjectType);

    void setSearchBarVisibility(boolean visible);

    MainActivity.SearchParameters getSearchParameters();

    void clearCache(String file_path, FileObjectType fileObjectType);

    void setCurrentDirText(String current_dir_name);

    void enableParentDirImageButton(boolean enable);

    void rescanLargeDuplicateFilesLibrary(String type);

    void onCreateView(String fileclickselected, FileObjectType fileObjectType);
}
