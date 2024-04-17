package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PlayerScreenMotionLayout extends MotionLayout {

    private Listener listener;
    private final Rect viewRect = new Rect();
    private final List<TransitionListener> transitionListenerList = new ArrayList<>();
    private Context context;
    private boolean hasTouchStarted=false;
    public PlayerScreenMotionLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
        addTransitionListener(new TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {

            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {

            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                hasTouchStarted = false;
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {

            }
        });

        super.setTransitionListener(new TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {

            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
                for(TransitionListener transitionListener:transitionListenerList){
                    if(transitionListener!=null)transitionListener.onTransitionChange(motionLayout,startId,endId,progress);
                }
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                for(TransitionListener transitionListener:transitionListenerList){
                    if(transitionListener!=null)transitionListener.onTransitionCompleted(motionLayout,currentId);
                }
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {

            }
        });



    }
    
    @Override
    public void setTransitionListener(TransitionListener listener) {
        addTransitionListener(listener);
    }

    @Override
    public void addTransitionListener(TransitionListener listener) {
        transitionListenerList.add(listener);
    }

    //This ensures the Mini Player is maximised on single tap
    GestureDetector gestureDetector=new GestureDetector(context ,new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            transitionToEnd();
            return false;
        }
    });

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                hasTouchStarted=false;
                if(listener!=null)listener.onListeningTransition();
                return super.onTouchEvent(event);
        }

        //This Checks if the touch is on the Player or the transaprent background
        if(!hasTouchStarted){
            View viewToDetectTouch = findViewById(R.id.player_background_view);
            viewToDetectTouch.getHitRect(viewRect);
            hasTouchStarted = viewRect.contains((int) event.getX(), (int) event.getY());

        }
        return hasTouchStarted && super.onTouchEvent(event);
    }

    public void setListener(Listener listener){
        this.listener=listener;
    }

    public void removeListener(){
        listener=null;
    }
    interface Listener{
        void onListeningTransition();
    }


}
