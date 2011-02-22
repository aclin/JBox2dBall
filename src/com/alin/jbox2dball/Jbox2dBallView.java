package com.alin.jbox2dball;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import android.media.MediaPlayer;
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
	private MediaPlayer mp;

	public Jbox2dBallView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		gestureDetector = new GestureDetector(context, new GestureListener());
		mp = MediaPlayer.create(context, R.raw.beep);
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
		
		Vec2 xOffset = new Vec2(getWidth() - 2.0f, 0.0f);
		Vec2 yOffset = new Vec2(0.0f, getHeight() - 2.0f);
		Vec2 hrz[] = new Vec2[4];
		Vec2 vrt[] = new Vec2[4];
		
		hrz[0] = new Vec2(0.0f, 0.0f);
		hrz[1] = new Vec2(getWidth(), 0.0f);
		hrz[2] = new Vec2(getWidth(), 2.0f);
		hrz[3] = new Vec2(0.0f, 2.0f);
		
		vrt[0] = new Vec2(0.0f, 0.0f);
		vrt[1] = new Vec2(2.0f, 0.0f);
		vrt[2] = new Vec2(2.0f, getHeight());
		vrt[3] = new Vec2(0.0f, getHeight());
		
		for (int i=0; i<hrz.length; i++) {
			northWall.addVertex(hrz[i]);
			southWall.addVertex(hrz[i].add(yOffset));
			westWall.addVertex(vrt[i]);
			eastWall.addVertex(vrt[i].add(xOffset));
		}
		
		northWall.restitution = 0.0f;
		eastWall.restitution = 0.0f;
		southWall.restitution = 0.0f;
		westWall.restitution = 0.0f;
		
		northWall.friction = 0.0f;
		eastWall.friction = 0.0f;
		southWall.friction = 0.0f;
		westWall.friction = 0.0f;
		
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
        paddle.friction = 0.0f;
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
		ball.friction = 0.0f;
		
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
		private static final float SPEED_STANDARD = 8.0f;
		private static final float SPEED_UP = 2.0f;
		private static final float CHASE_DELAY_STANDARD = 9.0f;
		
		private MotionEvent downEvent;
		
		// Flags
		private boolean touchDown = false;
		private boolean addBall = false;
		private boolean scroll = false;
		private boolean contact = false;
		private boolean speedStandard = false;
		private boolean speedUp = false;
		private boolean firstGuess = true;
		private boolean secondGuess = false;
		
		private float contactX;
		private float contactY;
		private float slide = 0.0f;
		private float chase = 0.0f;
		
		private Random r = new Random(System.currentTimeMillis());
		
		private void slidePaddle() {
			float x = paddleBody.getPosition().x;
			float y = paddleBody.getPosition().y;
			
			// Keep the paddle within the screen width
			if (x - slide > getWidth() - PADDLE_WIDTH)
				paddleBody.setXForm(new Vec2(getWidth() - PADDLE_WIDTH, y), 0.0f);
			else if (x - slide < PADDLE_WIDTH)
				paddleBody.setXForm(new Vec2(PADDLE_WIDTH, y), 0.0f);
			else
				paddleBody.setXForm(new Vec2(x - slide, y), 0.0f);
			
			Log.i(TAG, "Paddle slid to (" + paddleBody.getPosition().x + ", " + paddleBody.getPosition().y + ")");
			Log.i(TAG, "Slide distance: " + slide);
		}
		
		private void chaseBall() {
			float cx = computerBody.getPosition().x;
			float cy = computerBody.getPosition().y;
			
			// Keep the paddle within the screen width
			if (cx + chase > getWidth() - PADDLE_WIDTH)
				computerBody.setXForm(new Vec2(getWidth() - PADDLE_WIDTH, cy), 0.0f);
			else if (cx + chase < PADDLE_WIDTH)
				computerBody.setXForm(new Vec2(PADDLE_WIDTH, cy), 0.0f);
			else
				computerBody.setXForm(new Vec2(cx + chase, cy), 0.0f);
		}
		
		private void guess() {
			float bvx = pongBallBody.getLinearVelocity().x;
			float bvy = pongBallBody.getLinearVelocity().y;
			float bx = pongBallBody.getPosition().x;
			float by = pongBallBody.getPosition().y;
			float cx = computerBody.getPosition().x;
			
			if (r.nextFloat() < 0.5f) {
				// Find how far the computer paddle has to travel
				chase = (bvx * by / (bvy * -1.0f)) + (bx - cx) + (PADDLE_WIDTH * 2.0f * r.nextFloat());
			} else {
				chase = (bvx * by / (bvy * -1.0f)) + (bx - cx) - (PADDLE_WIDTH * 2.0f * r.nextFloat());
			}
		}
		
		public void run() {
			while(!isInterrupted()) {
				updateState();
				updateInput();
				updateAI();
				updatePhysics();
				updateAnimation();
				updateSound();
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
			
			/* Check if scrolling occurred */
			if (scroll) {
				slidePaddle();
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
			}
		}
		
		/* The computer paddle will calculate where the pong ball will land along
		 * the x-axis using the ball's linear velocity, since it is constant.
		 * The computer paddle will then move to that point with some error margin,
		 * which can cause the paddle to stop short or over shoot.
		 * */
		private void updateAI() {
			float bvx = pongBallBody.getLinearVelocity().x;
			float bvy = pongBallBody.getLinearVelocity().y;
			float bx = pongBallBody.getPosition().x;
			float by = pongBallBody.getPosition().y;
			float cx = computerBody.getPosition().x;
			
			// Check if the ball is heading towards the computer first
			if (bvy < 0) {
				// Check if the ball is passed the halfway point
				if (by < getHeight() / 2 && firstGuess) {
					guess();
					firstGuess = false;
					secondGuess = true;
				} else if (by < 50.0f && secondGuess) {
					guess();
					secondGuess = false;
				} else {
					chase = 0.0f;
				}
			} else {
				// The ball is heading away from the computer
				// so it doesn't have to move and firstGuess is reset
				chase = 0.0f;
				firstGuess = true;
			}
		}
		
		private void updatePhysics() {
			float bvx = pongBallBody.getLinearVelocity().x;
			float bvy = pongBallBody.getLinearVelocity().y;
			
			// y divide by the absolute value of y gives the sign of y
			// Determine if the ball needs to maintain normal speed
			// or speed up
			if (speedStandard) {
				if (bvx < 0)
					pongBallBody.setLinearVelocity(new Vec2(SPEED_STANDARD * -1.0f, SPEED_STANDARD * bvy / Math.abs(bvy)));
				else
					pongBallBody.setLinearVelocity(new Vec2(SPEED_STANDARD, SPEED_STANDARD * bvy / Math.abs(bvy)));
				speedStandard = false;
			} else if (speedUp) {
				if (bvx < 0)
					pongBallBody.setLinearVelocity(new Vec2(bvx - SPEED_UP, bvy + SPEED_UP * bvy / Math.abs(bvy)));
				else
					pongBallBody.setLinearVelocity(new Vec2(bvx + SPEED_UP, bvy + SPEED_UP * bvy / Math.abs(bvy)));
				speedUp = false;
			}
			
			chaseBall();
			
		}
		
		private void updateAnimation() {
			
		}
		
		private void updateSound() {
			
			// If any contact is made, play a 'beep' sound 
			if (contact) {
				mp.start();
				contact = false;
			}
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
			// Activity paused, destroys all JBox2D bodies
			Body b = world.getBodyList();
			for (int i=0; i<world.getBodyCount(); i++) {
				world.destroyBody(b);
				if (b.getNext() != null)
					b = b.getNext();
			}
			
			// Release MediaPlayer
			mp.release();
			
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
