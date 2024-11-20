package svl.kadatha.filex;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

public class KeyBoardUtil {
    private boolean keyboardShown;

    public KeyBoardUtil(final View view) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);

                int screenHeight = view.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                keyboardShown = keypadHeight > screenHeight * 0.15;

            }
        });
    }

    public boolean getKeyBoardVisibility() {
        return keyboardShown;
    }

}
