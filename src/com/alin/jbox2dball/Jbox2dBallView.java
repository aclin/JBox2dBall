package com.alin.jbox2dball;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.CircleShape;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.collision.Shape;
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
	
	private boolean init = false;
	private World world;
	private AABB worldAABB;
	private BodyDef groundBodyDef = new BodyDef();
	private List<Body> bodies = new ArrayList();
	private Body groundBody, ballBody, ballBody2;
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
		worldAABB.upperBound.set(new Vec2(getWidth(), getHeight()));
		
		Vec2 gravity = new Vec2(0.0f, 9.8f);
		boolean doSleep = true;
		world = new World(worldAABB, gravity, doSleep);
	}
	
	private class ballLoop extends Thread {
		
		public void run() {
			while(!isInterrupted()) {
				updateState();
				updatePhysics();
				updateAnimation();
				updateView();
			}
		}
		
		private void updateState() {
			if (!init) {
				createWorld();
				createBoundary();
				//createGround();
				//createIncline((float) Math.PI * 0.1f);
				//createBall();
				//createBall2();
				rain();
				init = true;
			}
			world.step(timeStep, iterations);
			
			Vec2 posB = ballBody.getPosition();
	        Log.v("PHYSICS TEST", "Pos: (" + posB.x + ", " + posB.y + ")");
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
				synchronized (getHolder()) {
					mpaint.setStyle(Paint.Style.FILL_AND_STROKE);
					mpaint.setColor(Color.RED);
					for (Body b : bodies) {
						canvas.drawCircle(b.getPosition().x,
										  b.getPosition().y,
										  ((CircleShape) b.getShapeList()).getRadius(),
										  mpaint);
					}
					//mpaint.setColor(Color.BLACK);
					//canvas.drawCircle(ballBody2.getPosition().x, ballBody2.getPosition().y, ((CircleShape) ballBody2.getShapeList()).getRadius(), mpaint);
					//canvas.drawLine(10.0f, 120.0f, 110.0f, 120.0f, mpaint);
					//drawPolygon(mpaint);
				}
			} finally {
				getHolder().unlockCanvasAndPost(canvas);
			}
		}
		
		private void drawPolygon(Canvas canvas, Paint mpaint) {
			PolygonShape ps;
			
			ps = (PolygonShape) groundBody.getShapeList();
			canvas.drawRect(ps.getVertices()[0].x + groundBodyDef.position.x,
							ps.getVertices()[0].y + groundBodyDef.position.y,
							ps.getVertices()[2].x + groundBodyDef.position.x,
							ps.getVertices()[2].y + groundBodyDef.position.y,
							mpaint);
		}
	}
	
	private void createBoundary() {
		// Create a boundary so objects on the screen doesn't fall out of view
		// TODO: Find better way to define boundary
		BodyDef boundaryDef = new BodyDef();
		boundaryDef.position.set(world.getWorldAABB().lowerBound);
		Body boundaryBody = world.createBody(boundaryDef);
		
		PolygonDef northWall = new PolygonDef();
		PolygonDef eastWall = new PolygonDef();
		PolygonDef southWall = new PolygonDef();
		PolygonDef westWall = new PolygonDef();
		
		Vec2 xOffset = new Vec2(getWidth() - 1.0f, 0.0f);
		Vec2 yOffset = new Vec2(0.0f, getHeight() - 1.0f);
		Vec2 hrz[] = new Vec2[4];
		Vec2 vrt[] = new Vec2[4];
		
		hrz[0] = new Vec2(0.0f, 0.0f);
		hrz[1] = new Vec2(getWidth(), 0.0f);
		hrz[2] = new Vec2(getWidth(), 1.0f);
		hrz[3] = new Vec2(0.0f, 1.0f);
		
		vrt[0] = new Vec2(0.0f, 0.0f);
		vrt[1] = new Vec2(1.0f, 0.0f);
		vrt[2] = new Vec2(1.0f, getHeight());
		vrt[3] = new Vec2(0.0f, getHeight());
		
		for (int i=0; i<hrz.length; i++) {
			northWall.addVertex(hrz[i]);
			southWall.addVertex(hrz[i].add(yOffset));
			westWall.addVertex(vrt[i]);
			eastWall.addVertex(vrt[i].add(xOffset));
		}
		
		boundaryBody.createShape(northWall);
		boundaryBody.createShape(eastWall);
		boundaryBody.createShape(southWall);
		boundaryBody.createShape(westWall);
	}
	
	private void createGround() {
		groundBodyDef = new BodyDef();
		groundBodyDef.position.set(60.0f, getHeight());
        groundBody = world.createBody(groundBodyDef);
        PolygonDef groundShapeDef = new PolygonDef();
        groundShapeDef.setAsBox(50.0f, 1.0f);
        groundBody.createShape(groundShapeDef);
	}
	
	private void rain() {
		CircleDef ball = new CircleDef();
		ball.radius = 2.0f;
		ball.density = 1.0f;
		ball.restitution = 0.7f;
		
		for (int i = 0; i < 10; i++) {
			BodyDef ballBodyDef = new BodyDef();
			ballBodyDef.position.set(new Vec2(getWidth() / 2, 90.0f - 5.0f * i));
			ballBody = world.createBody(ballBodyDef);
			ballBody.createShape(ball);
			ballBody.setMassFromShapes();
			bodies.add(ballBody);
		}
		
	}
	
	private void createBall() {
		
		// Create dynamic body
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(20.0f, 20.0f);
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
	
	private void createBall2() {
		
		// Create dynamic body
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(30.0f, 10.0f);
		ballBody2 = world.createBody(ballBodyDef);
		
		// Create shape with properties
		ball = new CircleDef();
		ball.radius = 8.0f;
		ball.density = 1.0f;
		ball.restitution = 0.95f;
		
		//Assign shape to Body
		ballBody2.createShape(ball);
		ballBody2.setMassFromShapes();
	}
	
	private void createIncline(float angle) {
		groundBodyDef.position.set(30.0f, 90.0f);
		groundBodyDef.angle = angle;
		groundBody = world.createBody(groundBodyDef);
		PolygonDef incline = new PolygonDef();
		incline.setAsBox(30.0f, 5.0f);
		groundBody.createShape(incline);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i(TAG, "Surface changed, world initialized");
		
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
