package svl.kadatha.filex;

import java.util.ArrayList;

public interface AudioFragmentListener {
    void onAudioSave();
    void refreshAudioPlayNavigationButtons();
    void onDeleteAudio(ArrayList<AudioPOJO> list);
    void setSearchBarVisibility(boolean visible);
    boolean getSearchBarVisibility();
    void hideKeyBoard();
    boolean getKeyBoardVisibility();
}
