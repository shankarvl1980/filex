package svl.kadatha.filex.audio;

import java.util.ArrayList;

public interface AudioFragmentListener {
    void onAudioSave();

    void refreshAudioPlayNavigationButtons();

    void onDeleteAudio(ArrayList<AudioPOJO> list);

    boolean getSearchBarVisibility();

    void setSearchBarVisibility(boolean visible);

    void hideKeyBoard();

    boolean getKeyBoardVisibility();

}
