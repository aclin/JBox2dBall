package com.alin.jbox2dball;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
        	Log.i(TAG, "SIS is null");
        } else {
        	mBallLoop.restoreState(savedInstanceState);
        	Log.i(TAG, "SIS is non-null");
        }
        
        //mBallLoop.setState(ballLoop.STATE_READY);
    	//Log.i(TAG, "SIS is null");
    	
        Log.i(TAG, "Activity created");
    }
    
    private Jbox2dBallView getJbox2dBallView() {
    	return (Jbox2dBallView) findViewById(R.id.jbox2dBall_view);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	Log.i(TAG, "Activity is starting");
    }
    
    @Override
    public void onRestart() {
    	super.onRestart();
    	mBallLoop.unsuspendLoop();
    	Log.i(TAG, "Activity is restarting");
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	mBallLoop.pause();
    	Log.i(TAG, "Activity on pause");
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	//mBallLoop.restoreState(savedInstanceState);
    	Log.i(TAG, "Activity restoring");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	/*Thread.State s = mBallLoop.getState();
    	if (s == Thread.State.NEW)
			Log.i(TAG, "TLoop thread is new");
		else if (s == Thread.State.BLOCKED)
			Log.i(TAG, "Loop thread is blocked");
		else if (s == Thread.State.RUNNABLE)
			Log.i(TAG, "Loop thread is running");
		else if (s == Thread.State.WAITING)
			Log.i(TAG, "Loop thread is waiting");
		else if (s == Thread.State.TERMINATED)
			Log.i(TAG, "Loop thread is terminated");*/
    	Log.i(TAG, "Activity on resume");
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	mBallLoop.suspendLoop();
    	Log.i(TAG, "Activity on stop");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	/*boolean retry = true;
    	while (retry) {
    		try {
    			mBallLoop.join();
    			retry = false;
    		} catch (InterruptedException e) {
    			
    		}
    	}*/
    	//mBallLoop.unsuspendLoop(); // unsuspend the game thread so that it can be destroyed
    	Log.i(TAG, "Activity is destroyed");
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
        //mBallLoop.saveState(outState);
        Log.i(TAG, "Activity instance saved");
    }
}