package com.alin.jbox2dball;

import android.app.Activity;
import android.os.Bundle;

public class Jbox2dBall extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    private Jbox2dBallView getJbox2dBallView() {
    	return (Jbox2dBallView) findViewById(R.id.jbox2dBall_view);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    }
}