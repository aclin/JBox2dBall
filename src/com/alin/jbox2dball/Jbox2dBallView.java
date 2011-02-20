package com.alin.jbox2dball;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.CircleShape;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Jbox2dBallView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "Jbox2dBallView";
	private static final float PADDLE_WIDTH = 30.0f; // Note this is half-width, total is 60.0f
	private static final float PADDLE_HEIGHT = 5.0f; // Note this is half-height, total is 10.0f
	public float targetFPS = 40.0f;
	public float timeStep = (10.0f / targetFPS);  
	public int iterations = 5;
	
	private boolean init = false;
	
	private World world;
	private AABB worldAABB;
	private BodyDef groundBodyDef = new BodyDef();
	private List<Body> bodies = new ArrayList<Body>();
	private Body groundBody;
	private Body paddleBody, computerBody, pongBallBody;
	private CircleDef ball;
	
	private ballLoop loop;
	private GestureDetector gestureDetector;

	public Jbox2dBallView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		gestureDetector = new GestureDetector(context, new GestureListener());
		loop = new ballLoop();
	}
	
	// Initialize this world
	public void createWorld() {
		worldAABB = new AABB();
		worldAABB.lowerBound.set(new Vec2(0.0f, 0.0f));
		worldAABB.upperBound.set(new Vec2(getWidth(), getHeight()));
		
		//Vec2 gravity = new Vec2(0.0f, 9.8f);
		Vec2 gravity = new Vec2(0.0f, 0.0f); // Zero-gravity environment, all velocities persists
		boolean doSleep = true;
		world = new World(worldAABB, gravity, doSleep);
		world.setContactListener(new customListener());
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
		
		eastWall.friction = 0.05f;
		westWall.friction = 0.05f;
		
		boundaryBody.createShape(northWall);
		boundaryBody.createShape(eastWall);
		boundaryBody.createShape(southWall);
		boundaryBody.createShape(westWall);
	}
	
	private void createPaddle() {
		BodyDef paddleBodyDef = new BodyDef();
		paddleBodyDef.position.set(getWidth() / 2, getHeight() - 10.0f);
        paddleBody = world.createBody(paddleBodyDef);
        
        PolygonDef paddle = new PolygonDef();
        paddle.setAsBox(PADDLE_WIDTH, PADDLE_HEIGHT);
        paddle.density = 1.0f;
        paddle.friction = 0.08f;
        paddle.restitution = 0.0f;
        paddleBody.createShape(paddle);
        Log.i(TAG, "Paddle created at (" + paddleBody.getPosition().x + ", " + paddleBody.getPosition().y + ")");
	}
	
	private void createComputerPaddle() {
		BodyDef paddleBodyDef = new BodyDef();
		paddleBodyDef.position.set(getWidth() / 2, 10.0f);
		computerBody = world.createBody(paddleBodyDef);
		
		PolygonDef computer = new PolygonDef();
		computer.setAsBox(PADDLE_WIDTH, PADDLE_HEIGHT);
		computer.density = 1.0f;
		computer.friction = 0.0f;
		computer.restitution = 0.0f;
		computerBody.createShape(computer);
		Log.i(TAG, "Computer paddle created at (" + computerBody.getPosition().x + ", " + computerBody.getPosition().y + ")");
	}
	
	private void rain() {
		CircleDef ball = new CircleDef();
		ball.radius = 2.0f;
		ball.density = 1.0f;
		ball.restitution = 0.7f;
		
		for (int i = 0; i < 10; i++) {
			BodyDef ballBodyDef = new BodyDef();
			ballBodyDef.position.set(new Vec2(getWidth() / 2, 90.0f - 5.0f * i));
			Body ballBody = world.createBody(ballBodyDef);
			ballBody.createShape(ball);
			ballBody.setMassFromShapes();
			bodies.add(ballBody);
		}
		
	}
	
	private void createBall() {
		
		// Create dynamic body
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(20.0f, 20.0f);
		Body ballBody = world.createBody(ballBodyDef);
		
		// Create shape with properties
		ball = new CircleDef();
		ball.radius = 3.0f;
		ball.density = 1.0f;
		ball.restitution = 0.7f;
		
		//Assign shape to Body
		ballBody.createShape(ball);
		ballBody.setMassFromShapes();
	}
	
	private void createIncline(float angle) {
		groundBodyDef.position.set(30.0f, 90.0f);
		groundBodyDef.angle = angle;
		groundBody = world.createBody(groundBodyDef);
		PolygonDef incline = new PolygonDef();
		incline.setAsBox(30.0f, 5.0f);
		groundBody.createShape(incline);
	}
	
	private void addBall(float x, float y) {
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(x, y);
		Body ballBody = world.createBody(ballBodyDef);
		
		CircleDef ball = new CircleDef();
		ball.radius = 5.0f;
		ball.density = 1.0f;
		ball.restitution = 0.7f;
		
		ballBody.createShape(ball);
		ballBody.setMassFromShapes();
		bodies.add(ballBody);
		Log.i(TAG, "Added 1 ball at: (" + x + " ," + y + ")");
	}
	
	private void addPongBall() {
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(getWidth() / 2, getHeight() / 2);
		pongBallBody = world.createBody(ballBodyDef);
		
		CircleDef ball = new CircleDef();
		ball.radius = 5.0f;
		ball.density = 1.0f;
		ball.restitution = 1.0f;
		ball.friction = 0.05f;
		
		pongBallBody.createShape(ball);
		pongBallBody.setMassFromShapes();
		Log.i(TAG, "Added Pong ball at: (" + pongBallBody.getPosition().x + " ," + pongBallBody.getPosition().y + ")");
	}
	
	private void drawPaddle(Canvas canvas, Paint mpaint) {
		// TODO: Come up with more efficient way to draw polygons
		PolygonShape ps;
		
		/* Draw player paddle */
		ps = (PolygonShape) paddleBody.getShapeList();
		canvas.drawRect(ps.getVertices()[0].x + paddleBody.getPosition().x,
						ps.getVertices()[0].y + paddleBody.getPosition().y,
						ps.getVertices()[2].x + paddleBody.getPosition().x,
						ps.getVertices()[2].y + paddleBody.getPosition().y,
						mpaint);
		/* Draw computer paddle */
		ps = (PolygonShape) computerBody.getShapeList();
		canvas.drawRect(ps.getVertices()[0].x + computerBody.getPosition().x,
						ps.getVertices()[0].y + computerBody.getPosition().y,
						ps.getVertices()[2].x + computerBody.getPosition().x,
						ps.getVertices()[2].y + computerBody.getPosition().y,
						mpaint);
	}
	
	// Set world gravity
	public void setGravity(float gx, float gy) {
		world.setGravity(new Vec2(gx, gy));
	}
	
	public class ballLoop extends Thread {
		private static final float SPEED_STANDARD = 10.0f;
		private static final float SPEED_UP = 5.0f;
		
		private MotionEvent downEvent;
		
		// Flags
		private boolean touchDown = false;
		private boolean addBall = false;
		private boolean scroll = false;
		private boolean contact = false;
		private boolean speedStandard = false;
		private boolean speedUp = false;
		
		private float contactX;
		private float contactY;
		private float slide = 0.0f;
		
		private void slidePaddle(float slide) {
			float x = paddleBody.getXForm().position.x;
			float y = paddleBody.getXForm().position.y;
			
			// Keep the paddle within the screen width
			if (x - slide > getWidth() - PADDLE_WIDTH)
				paddleBody.setXForm(new Vec2(getWidth() - PADDLE_WIDTH, y), 0.0f);
			else if (x - slide < PADDLE_WIDTH)
				paddleBody.setXForm(new Vec2(PADDLE_WIDTH, y), 0.0f);
			else
				paddleBody.setXForm(new Vec2(x - slide, y), 0.0f);
			
			Log.i(TAG, "Paddle slid to (" + paddleBody.getPosition().x + ", " + paddleBody.getPosition().y + ")");
			Log.i(TAG, "Slide distance: " + slide);
			Log.i(TAG, "Width: " + getWidth());
		}
		
		public void run() {
			while(!isInterrupted()) {
				updateState();
				updateInput();
				updateAI();
				updatePhysics();
				updateAnimation();
				updateView();
			}
		}
		
		private void updateState() {
			if (!init) {
				createWorld();
				createBoundary();
				createPaddle();
				createComputerPaddle();
				addPongBall();
				pongBallBody.setLinearVelocity(new Vec2(SPEED_STANDARD, SPEED_STANDARD));
				
				init = true;
			}
			world.step(timeStep, iterations);
		}
		
		private void updateInput() {
			/*
			if (addBall) {
				addBall(downEvent.getX(), downEvent.getY());
				addBall = false;
			}
			*/
			
			/* Check if scrolling occurred */
			if (scroll) {
				slidePaddle(slide);
				scroll = false;
			}
			
			/* Check if contact occurred */
			if (contact) {
				// First check if the contact occurs at the player or computer side
				// Math is mirrored depending on the side
				// Note: Screen orientation is vertical!
				// Combined height of ball and paddle is 20.0f
				if (contactY > getHeight() - 25.0f) {
					// Contact is at bottom half of screen (player half)
					// Check if made contact with paddle
					if(Math.abs(contactX - paddleBody.getPosition().x) <= PADDLE_WIDTH) {
						// Ball hits the paddle
						if(Math.abs(contactX - paddleBody.getPosition().x) <= 10.0f) {
							// Contact is near the middle of the paddle
							// Ball speed is set to STANDARD_SPEED
							speedStandard = true;
						} else {
							// Contact is near the edge of the paddle
							// Ball speed is sped up by SPEED_UP
							speedUp = true;
						}
					}
					
					// Contact is not made with paddle
					// Determine if ball hit the wall, or the ground
					// TODO: What to do when ball hits the ground
				} else if (contactY < 25.0f) {
					// Contact is at top half of screen (computer half)
					// Check if made contact with paddle
					if(Math.abs(contactX - computerBody.getPosition().x) <= PADDLE_WIDTH) {
						// Ball hits the paddle
						if(Math.abs(contactX - computerBody.getPosition().x) <= 10.0f) {
							// Contact is near the middle of the paddle
							// Ball speed is set to STANDARD_SPEED
							speedStandard = true;
						} else {
							// Contact is near the edge of the paddle
							// Ball speed is sped up by SPEED_UP
							speedUp = true;
						}
					}
				}
				contact = false;
			}
		}
		
		/* Computer paddle is set to move horizontally at some linear velocity.
		 * Velocity may be slower than the ball speed, but can reach by using
		 * the width of the paddle.
		 * All the computer will try to do is to close the x distance between
		 * it and the ball.
		 * The difficulty of the computer can be adjusted by how early the computer
		 * begins to give chase, and how fast the computer moves horizontally.
		 * */
		private void updateAI() {
			
		}
		
		private void updatePhysics() {
			float x = pongBallBody.getLinearVelocity().x;
			float y = pongBallBody.getLinearVelocity().y;
			
			/* y divide by the absolute value of y gives the sign of y */
			if (speedStandard) {
				if (x < 0)
					pongBallBody.setLinearVelocity(new Vec2(SPEED_STANDARD * -1.0f, SPEED_STANDARD * y / Math.abs(y)));
				else
					pongBallBody.setLinearVelocity(new Vec2(SPEED_STANDARD, SPEED_STANDARD * y / Math.abs(y)));
				speedStandard = false;
			} else if (speedUp) {
				if (x < 0)
					pongBallBody.setLinearVelocity(new Vec2(x - SPEED_UP, SPEED_STANDARD * y / Math.abs(y)));
				else
					pongBallBody.setLinearVelocity(new Vec2(x + SPEED_UP, SPEED_STANDARD * y / Math.abs(y)));
				speedUp = false;
			}
		}
		
		private void updateAnimation() {
			
		}
		
		private void updateView() {
			Canvas canvas = getHolder().lockCanvas();
			Paint mpaint = new Paint();
			canvas.clipRect(0, 0, getWidth(), getHeight());
			canvas.drawColor(Color.WHITE);
			try {
				synchronized (getHolder()) {
					mpaint.setStyle(Paint.Style.FILL_AND_STROKE);
					mpaint.setColor(Color.RED);
					canvas.drawCircle(pongBallBody.getPosition().x,
									  pongBallBody.getPosition().y,
									  ((CircleShape) pongBallBody.getShapeList()).getRadius(),
									  mpaint);
					mpaint.setStyle(Paint.Style.FILL);
					mpaint.setColor(Color.BLACK);
					drawPaddle(canvas, mpaint);
				}
			} finally {
				getHolder().unlockCanvasAndPost(canvas);
			}
		}
		
		private void setContact(float x, float y) {
			contactX = x;
			contactY = y;
		}
		
		public void pause() {
			// Activity paused, destroys everything
			Body b = world.getBodyList();
			for (int i=0; i<world.getBodyCount(); i++) {
				world.destroyBody(b);
				if (b.getNext() != null)
					b = b.getNext();
			}
			Log.i(TAG, "All bodies in world destroyed");
		}
	}
	
	private class GestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float scrollX, float scrollY) {
			loop.scroll = true;
			loop.slide = scrollX;
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			//loop.downEvent = e;
			//loop.addBall = true;
			Log.i(TAG, "Down press occurred");
			return true;
		}
		
	}
	
	private class customListener implements ContactListener {
		@Override
		public void add(ContactPoint point) {
			// TODO Auto-generated method stub
			float x = point.position.x;
			float y = point.position.y;
			
			loop.setContact(x, y);
			loop.contact = true;
			Log.i(TAG, "Contact made at (" + x + ", " + y + ")");
			//Log.i(TAG, "add");
			
		}

		@Override
		public void persist(ContactPoint point) {
			// TODO Auto-generated method stub
			//Log.i(TAG, "persist");
		}

		@Override
		public void remove(ContactPoint point) {
			// TODO Auto-generated method stub
			//Log.i(TAG, "remove");
		}

		@Override
		public void result(ContactResult point) {
			// TODO Auto-generated method stub
			//Log.i(TAG, "result");
		}
	}
	
	public ballLoop getThread() {
		return loop;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_UP) {
			loop.touchDown = false;
		}
		return gestureDetector.onTouchEvent(e);
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
