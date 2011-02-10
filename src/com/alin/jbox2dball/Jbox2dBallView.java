package com.alin.jbox2dball;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Jbox2dBallView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "Jbox2dBallView";
	public float targetFPS = 40.0f;
	public float timeStep = (10.0f / targetFPS);  
	public int iterations = 5;  
	
	private World world;
	private AABB worldAABB;
	private Body ballBody;
//	private BodyDef bodyDef;
	private CircleDef ball;
	
	private ballLoop loop;

	public Jbox2dBallView(Context context, AttributeSet attrs) {
		super(context, attrs);
		loop = new ballLoop();
		getHolder().addCallback(this);
	}
	
	// Initialize this world
	public void createWorld() {
		worldAABB = new AABB();
		worldAABB.lowerBound.set(new Vec2(0.0f, 0.0f));
		worldAABB.upperBound.set(new Vec2(150.0f, 150.0f));
		
		Vec2 gravity = new Vec2(0.0f, -9.8f);
		boolean doSleep = true;
		world = new World(worldAABB, gravity, doSleep);
		createGround();
		createBall();
	}
	
	private class ballLoop extends Thread {
		
		public void run() {
			while(!isInterrupted()) {
				synchronized(this) {
					updateState();
					updatePhysics();
					updateAnimation();
					updateView();
				}
			}
		}
		
		private void updateState() {
			world.step(timeStep, iterations);
		}
		
		private void updatePhysics() {
			
		}
		
		private void updateAnimation() {
			
		}
		
		private void updateView() {
			Canvas canvas = null;
			Paint mpaint = new Paint();
			try {
				canvas = getHolder().lockCanvas();
				canvas.clipRect(0, 0, getWidth(), getHeight());
				canvas.drawColor(Color.WHITE);
				synchronized (this) {
					mpaint.setStyle(Paint.Style.FILL_AND_STROKE);
					mpaint.setColor(Color.RED);
					canvas.drawCircle(ballBody.getPosition().x, ballBody.getPosition().y, ball.radius, mpaint);
				}
			} finally {
				getHolder().unlockCanvasAndPost(canvas);
			}
		}
	}
	
	private void createGround() {
		BodyDef groundBodyDef = new BodyDef();
		groundBodyDef.position.set(25.0f, 5.0f);
        Body groundBody = world.createBody(groundBodyDef);
        PolygonDef groundShapeDef = new PolygonDef();
        groundShapeDef.setAsBox(50.0f, 10.0f);
        groundBody.createShape(groundShapeDef);
	}
	
	private void createBall() {
		
		// Create dynamic body
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(20.0f, 100.0f);
		ballBody = world.createBody(ballBodyDef);
		
		// Create shape with properties
		ball = new CircleDef();
		ball.radius = 3.0f;
		ball.density = 1.0f;
		ball.restitution = 0.7f;
		
		//Assign shape to Body
		ballBody.createShape(ball);
		ballBody.setMassFromShapes();
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i(TAG, "Surface changed");
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "Surface created");
		loop.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "Surface destroyed");
		loop.interrupt();
	}

}
