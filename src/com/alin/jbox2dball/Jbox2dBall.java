package com.alin.jbox2dball;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.widget.TextView;

import com.alin.jbox2dball.Jbox2dBallView.ballLoop;

public class Jbox2dBall extends Activity {
	private static final String TAG = "JBox2DBall";
	
	private Jbox2dBallView mJbox2dBallView;
	private ballLoop mBallLoop;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mJbox2dBallView = (Jbox2dBallView) findViewById(R.id.jbox2dBall_view);
        mBallLoop = mJbox2dBallView.getThread();
        
        mJbox2dBallView.setTextView((TextView) findViewById(R.id.text));
        //getJbox2dBallView().createWorld();
        
        if (savedInstanceState == null) {
        	mBallLoop.setState(ballLoop.STATE_READY);
        } else {
        	mBallLoop.restoreState(savedInstanceState);
        }
    }
    
    private Jbox2dBallView getJbox2dBallView() {
    	return (Jbox2dBallView) findViewById(R.id.jbox2dBall_view);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	mBallLoop.pause();
    	Log.i(TAG, "Activity on pause");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(TAG, "Activity on resume");
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	Log.i(TAG, "Activity on stop");
    }
    
    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     * 
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mBallLoop.saveState(outState);
    }
}