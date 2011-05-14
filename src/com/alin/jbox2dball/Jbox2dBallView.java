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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class Jbox2dBallView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "Jbox2dBallView";
	//private static final boolean DEBUG_MODE = false;
	
	public float targetFPS = 40.0f;
	public float timeStep = (10.0f / targetFPS);
	public int iterations = 10;
	
	private Context mContext;
	private GestureDetector gestureDetector;
	private TextView mStatusText;
	private ballLoop loop;

	public Jbox2dBallView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		getHolder().addCallback(this);
		gestureDetector = new GestureDetector(context, new GestureListener());
		loop = new ballLoop(getHolder(), context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        });
	}
	
	public class ballLoop extends Thread {
		// Keys
		private static final String KEY_PLAYER_X = "playerX";
		private static final String KEY_PLAYER_Y = "playerY";
		private static final String KEY_COMPUTER_X = "computerX";
		private static final String KEY_COMPUTER_Y = "computerY";
		private static final String KEY_BALL_X = "ballX";
		private static final String KEY_BALL_Y = "ballY";
		private static final String KEY_PLAYER_SCORE = "playScore";
		private static final String KEY_COMPUTER_SCORE = "computerScore";
		
		// States
		public static final int STATE_READY = 0;
		public static final int STATE_RUN = 1;
		public static final int STATE_WIN = 2;
		public static final int STATE_LOSE = 3;
		public static final int STATE_RESET = 4;
		public static final int STATE_PAUSE = 5;
		public static final int STATE_SUSPEND = 6;
		
		// Other constants
		private static final float PADDLE_WIDTH = 30.0f; // Note this is half-width, total is 60.0f
		private static final float PADDLE_HEIGHT = 5.0f; // Note this is half-height, total is 10.0f
		private static final float SPEED_UP = 2.0f;
		
		// Flags
		private boolean init = true;
		private boolean mRun = false;
		private boolean scroll = false;
		private boolean contact = false;
		private boolean speedStandard = false;
		private boolean speedUp = false;
		private boolean move = false;
		private boolean playScore = false;
		private boolean playFail = false;
		
		// Numerical variables
		private float contactX;
		private float contactY;
		private float speed_Standard_X;
		private float speed_Standard_Y;
		private float prevSpeedX;
		private float prevSpeedY;
		private float slide = 0.0f;
		private float chaseTarget = 0.0f;
		
		private Vec2 chaseLeft = new Vec2(-5.0f, 0.0f);
		private Vec2 chaseRight = new Vec2(5.0f, 0.0f);
		
		private int playerScore = 0;
		private int computerScore = 0;
		private int state;
		private int prevState;
		
		private Handler mHandler;
		private Random r = new Random(System.currentTimeMillis());
		private MediaPlayer mpBump;
		private MediaPlayer mpScore;
		private MediaPlayer mpFail;
		private Bitmap mBackgroundImage;
		private Bitmap newBg;
		
		// JBox2D bodies
		private World world;
		private AABB worldAABB;
		private Body paddleBody, computerBody, pongBallBody;
		
		public ballLoop(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mHandler = handler;
			
			mpBump = MediaPlayer.create(context, R.raw.beep);
			mpScore = MediaPlayer.create(context, R.raw.score);
			mpFail = MediaPlayer.create(context, R.raw.fail);
			mBackgroundImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.pong_bg);
		}
		
		// Initialize this world
		public void createWorld() {
			worldAABB = new AABB();
			worldAABB.lowerBound.set(new Vec2(0.0f, 0.0f));
			worldAABB.upperBound.set(new Vec2(getWidth(), getHeight()));
			
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
			
			// Set the north wall just above the computer paddle so
			// that the computer paddle won't move when the ball hits
			northWall.addVertex(new Vec2(0.0f, 48.0f - PADDLE_HEIGHT));
			northWall.addVertex(new Vec2(getWidth(), 48.0f - PADDLE_HEIGHT));
			northWall.addVertex(new Vec2(getWidth(), 50.0f - PADDLE_HEIGHT));
			northWall.addVertex(new Vec2(0.0f, 50.0f - PADDLE_HEIGHT));
			
			for (int i=0; i<hrz.length; i++) {
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
			paddleBodyDef.position.set(getWidth() / 2, getHeight() - 50.0f);
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
			paddleBodyDef.position.set(getWidth() / 2, 50.0f);
			computerBody = world.createBody(paddleBodyDef);
			
			PolygonDef computer = new PolygonDef();
			computer.setAsBox(PADDLE_WIDTH, PADDLE_HEIGHT);
			computer.density = 5.0f;
			computer.friction = 0.0f;
			computer.restitution = 0.0f;
			computerBody.createShape(computer);
			computerBody.setMassFromShapes();
			Log.i(TAG, "Computer paddle created at (" + computerBody.getPosition().x + ", " + computerBody.getPosition().y + ")");
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
		
		private void addPongBall() {
			BodyDef ballBodyDef = new BodyDef();
			ballBodyDef.position.set(getWidth() / 2, getHeight() / 2);
			pongBallBody = world.createBody(ballBodyDef);
			
			CircleDef ball = new CircleDef();
			ball.radius = 5.0f;
			ball.density = 0.5f;
			ball.restitution = 1.0f;
			ball.friction = 0.0f;
			
			pongBallBody.createShape(ball);
			
			Log.i(TAG, "Added Pong ball at: (" + pongBallBody.getPosition().x + " ," + pongBallBody.getPosition().y + ")");
		}
		
		private void pushPongBall() {
			pongBallBody.setMassFromShapes();
			
			// Add 5.0f to make sure the speed is at least 5.0f
			speed_Standard_X = r.nextFloat() * 10.0f + 5.0f;
			speed_Standard_Y = r.nextFloat() * 10.0f + 5.0f;
			
			int rand = r.nextInt() % 4;
			switch (rand) {
			case 0:
				pongBallBody.setLinearVelocity(new Vec2(speed_Standard_X, speed_Standard_Y));
				break;
			case 1:
				pongBallBody.setLinearVelocity(new Vec2(-speed_Standard_X, speed_Standard_Y));
				break;
			case 2:
				pongBallBody.setLinearVelocity(new Vec2(-speed_Standard_X, -speed_Standard_Y));
				break;
			case 3:
				pongBallBody.setLinearVelocity(new Vec2(speed_Standard_X, -speed_Standard_Y));
				break;
			default:
				pongBallBody.setLinearVelocity(new Vec2(speed_Standard_X, speed_Standard_Y));
			}
		}
		
		private void pushPongBall(float vx, float vy) {
			pongBallBody.setLinearVelocity(new Vec2(vx, vy));
			/*if (DEBUG_MODE) {
				Log.i(TAG, "Ball speed: (" + vx + ", " + vy + ")");
			}*/
		}
		
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
			/*
			if (DEBUG_MODE) {
				Log.i(TAG, "Paddle slid to (" + paddleBody.getPosition().x + ", " + paddleBody.getPosition().y + ")");
				Log.i(TAG, "Slide distance: " + slide);
			}*/
		}
		
		/**
		 * Tells the computer paddle to chase after the ball after the player
		 * returns it across the net. The computer will figure out if the ball
		 * is traveling to the left or right of it, and give chase at a set speed.
		 */
		private void chaseBall() {
			float cx = computerBody.getPosition().x;
			float tol = 2.0f;
			
			// Check if the computer paddle is within the tolerance
			// distance from the target location, and not move
			// if it is.
			if (Math.abs(cx - chaseTarget) > tol) { 
				if (chaseTarget > cx) {
					// Target location is to the right of the paddle
					stopPaddle();	// Stop the paddle first before changing direction
					if (cx < getWidth() - 50.0f) {	// Keep the paddle within the boundary
						computerBody.setLinearVelocity(chaseRight);
					}
				} else if (chaseTarget < cx) {
					stopPaddle();
					if (cx > 50.0f) {
						computerBody.setLinearVelocity(chaseLeft);
					}
				}
			} else {
				// The paddle is within the tolerance distance from the target
				// location, so it shouldn't move.
				stopPaddle();
			}
		}
		
		private void stopPaddle() {
			computerBody.setLinearVelocity(new Vec2(0, 0));
		}
		
		/**
		 * Using the velocities of the ball, and the ball's location,
		 * we can calculate where the ball is going to be along the
		 * x-axis when it is level with the paddle. This is the target
		 * location that the computer paddle will know where to chase
		 * to as the ball is moving.
		 */
		private void target() {
			float bvx = pongBallBody.getLinearVelocity().x;
			float bvy = pongBallBody.getLinearVelocity().y;
			float bx = pongBallBody.getPosition().x;
			float by = pongBallBody.getPosition().y;
			
			// Find where the target location is.
			// First calculate how far the target is from the ball,
			// then add it to the ball's current location.
			// Directional velocity of the ball takes care of the sign.
			chaseTarget = (bvx * (by - 50.0f) / (bvy * -1.0f)) + bx;
		}
		
		private void scaleBG() {
			newBg = Bitmap.createScaledBitmap(mBackgroundImage, getWidth(), getHeight(), true);
		}
		
		private void setRunning(boolean run) {
			mRun = run;
		}
		
		public void setState(int s) {
			state = s;
		}
		
		public int checkState() {
			return state;
		}
		
		private void reset() {
			// Stop the PONG ball, reset its position to the middle of the screen
			// then push again to move in random direction
			
			pongBallBody.setLinearVelocity(new Vec2(0.0f, 0.0f));
			pongBallBody.setXForm(new Vec2(getWidth() / 2, getHeight() / 2), 0.0f);
			pushPongBall();
			setState(STATE_RUN);
		}
		
		/**
		 * To start another game when the game is won or lost.
		 * Pong ball and computer paddle need to be wake up again, so
		 * they can move.
		 */
		private void resetGame() {
			pongBallBody.wakeUp();
			computerBody.wakeUp();
			pongBallBody.setLinearVelocity(new Vec2(0.0f, 0.0f));
			pongBallBody.setXForm(new Vec2(getWidth() / 2, getHeight() / 2), 0.0f);
			pushPongBall();
			playerScore = 0;
			computerScore = 0;
			setState(STATE_RUN);
		}
		
		private void setContact(float x, float y) {
			contactX = x;
			contactY = y;
		}
		
		/**
		 * Pauses the game
		 * Game thread is still running.
		 */
		public void pause() {
			synchronized (getHolder()) {
				if (state == STATE_RUN)
					setState(STATE_PAUSE);
				
				prevState = state;
				
				prevSpeedX = pongBallBody.getLinearVelocity().x;
				prevSpeedY = pongBallBody.getLinearVelocity().y;
				
				pongBallBody.setLinearVelocity(new Vec2(0.0f, 0.0f));
			}
		}
		
		public void unpause() {
			setState(ballLoop.STATE_RUN);
			// Wake up the bodies that need to move after unpausing
			pongBallBody.wakeUp();
			computerBody.wakeUp();
			pushPongBall(prevSpeedX, prevSpeedY);
		}
		/**
		 * Suspends the game.
		 * The game thread will wait.
		 */
		public synchronized void suspendLoop(){
			setState(STATE_SUSPEND);
		}
		
		/**
		 * Game returns from suspension.
		 * Calls notifyAll() to start the game thread running again.
		 */
		public synchronized void unsuspendLoop() {
			setState(prevState);
			notifyAll();
			Log.i(TAG, "Loop unsuspended");
		}
		
		/**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         * 
         * @return Bundle with this view's state
         **/
        public Bundle saveState(Bundle bundle) {
            synchronized (getHolder()) {
                if (bundle != null) {
                	bundle.putFloat(KEY_PLAYER_X, paddleBody.getPosition().x);
                	bundle.putFloat(KEY_PLAYER_Y, paddleBody.getPosition().y);
                	bundle.putFloat(KEY_COMPUTER_X, computerBody.getPosition().x);
                	bundle.putFloat(KEY_COMPUTER_Y, computerBody.getPosition().y);
                	bundle.putFloat(KEY_BALL_X, pongBallBody.getPosition().x);
                	bundle.putFloat(KEY_BALL_Y, pongBallBody.getPosition().y);
                	bundle.putFloat(KEY_PLAYER_SCORE, playerScore);
                	bundle.putFloat(KEY_COMPUTER_SCORE, computerScore);
                }
            }
            return bundle;
        }
        
        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         * 
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
        	synchronized (getHolder()) {
        		setState(STATE_PAUSE);
        		paddleBody.setXForm(new Vec2(savedState.getFloat(KEY_PLAYER_X),
        									 savedState.getFloat(KEY_PLAYER_Y)),
        							0.0f);
        		computerBody.setXForm(new Vec2(savedState.getFloat(KEY_COMPUTER_X),
        									   savedState.getFloat(KEY_COMPUTER_Y)),
        							  0.0f);
        		pongBallBody.setXForm(new Vec2(savedState.getFloat(KEY_BALL_X),
        									   savedState.getFloat(KEY_BALL_Y)),
        							  0.0f);
        		playerScore = savedState.getInt(KEY_PLAYER_SCORE);
        		computerScore = savedState.getInt(KEY_COMPUTER_SCORE);
        		Log.i(TAG, "Internal restoration done");
        	}
        }
		
        /**
         * Tells the game thread to run. Each loop goes through all
         * the steps and updates all the changes in every aspect
         * of the game.
         */
		public void run() {
			while(mRun) {
				updateState();
				if (state == STATE_RUN || state == STATE_RESET) {
					updateInput();
					updateAI();
					updatePhysics();
					updateAnimation();
					updateSound();
				}
				updateView();
			}
			
			Log.i(TAG, "Loop stopped running");
		}
		
		/**
		 * Update the state of the game at the beginning of each loop
		 * The usual state is STATE_RUN when the game is running without
		 * anything special happening.
		 * Certain states will print a message on the screen.
		 */
		private void updateState() {
			
			if (state == STATE_RUN) {
                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", "");
                b.putInt("viz", View.INVISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
                
            } else {
                Resources res = mContext.getResources();
                CharSequence str = "";
                if (state == STATE_READY) {
                	if (init) {
	                	createWorld();
	    				createBoundary();
	    				scaleBG();
	    				createPaddle();
	    				createComputerPaddle();
	    				addPongBall();
	    				init = false;
                	}
                    str = res.getText(R.string.state_ready);
                } else if (state == STATE_PAUSE) {
                    str = res.getText(R.string.state_pause);
                } else if (state == STATE_RESET)
                	reset();
                else if (state == STATE_LOSE)
                    str = res.getText(R.string.state_lose);
                else if (state == STATE_WIN) {
                	str = res.getText(R.string.state_win);
                }

                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", str.toString());
                b.putInt("viz", View.VISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
			
			// The suspend state calls wait() in the thread
			// which will pause the loop while state == STATE_SUSPEND
			// and until notifyAll() is called in unsuspendLoop()
			// and state is changed to a non-suspend state.
			synchronized (this) {
				while (state == STATE_SUSPEND) {
					try {
						wait();
					} catch (InterruptedException e) {
						
					}
				}
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
		
		/**
		 * The computer paddle will calculate where the pong ball will land along
		 * the x-axis using the ball's linear velocity, since it is constant.
		 * 
		 * The computer paddle will try to travel to that point at a set speed.
		 * Therefore, it might get there quicker or slower. The computer paddle
		 * will stop when it is within a tolerance distance of the contact point.
		 */
		private void updateAI() {
			float bvy = pongBallBody.getLinearVelocity().y;
			float by = pongBallBody.getPosition().y;
			
			computerBody.wakeUp();
			
			// Check if the ball is heading towards the computer first.
			// Do nothing if the ball is going to the player.
			if (bvy < 0) {
				// Check if the ball is passed the halfway point to
				// reduce the amount of time that the computer has to
				// calculate and move.
				if (by < getHeight() / 2) {
					move = true;	// Tell the computer paddle to move.
					target(); // Find the target point that the computer has to be to return the ball
				} else {
					move = false;
				}
			} else {
				move = false;	// Tell the computer paddle not to move.
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
					pongBallBody.setLinearVelocity(new Vec2(-speed_Standard_X, speed_Standard_Y * bvy / Math.abs(bvy)));
				else
					pongBallBody.setLinearVelocity(new Vec2(speed_Standard_X, speed_Standard_Y * bvy / Math.abs(bvy)));
				speedStandard = false;
			} else if (speedUp) {
				if (bvx < 0)
					pongBallBody.setLinearVelocity(new Vec2(bvx - SPEED_UP, bvy + SPEED_UP * bvy / Math.abs(bvy)));
				else
					pongBallBody.setLinearVelocity(new Vec2(bvx + SPEED_UP, bvy + SPEED_UP * bvy / Math.abs(bvy)));
				speedUp = false;
			}
			
			if (move)
				chaseBall();
			else
				stopPaddle();
			
			if (pongBallBody.getPosition().y < 55.0f) {
				playerScore++;
				playScore = true;
				setState(STATE_RESET);
			} else if (pongBallBody.getPosition().y > getHeight() - 55.0f) {
				computerScore++;
				playFail = true;
				setState(STATE_RESET);
			}
			
			if (playerScore == 10) {
				setState(STATE_WIN);
			} else if (computerScore == 10) {
				setState(STATE_LOSE);
			}
		}
		
		private void updateAnimation() {
			
		}
		
		private void updateSound() {
			
			// If any contact is made, play a 'beep' sound 
			if (contact) {
				mpBump.start();
				contact = false;
			}
			
			// If the player scores a point, play a ring sound
			if (playScore) {
				mpScore.start();
				playScore = false;
			}
			
			// If the computer scores a point, play a buzzer sound
			if (playFail) {;
				mpFail.start();
				playFail = false;
			}
		}
		
		private void updateView() {
			Canvas canvas = getHolder().lockCanvas(null);
			Paint mpaint = new Paint();
			try {
				synchronized (getHolder()) {
					//canvas.drawBitmap(mBackgroundImage, 0, 0, null);
					if (canvas != null) {
					canvas.drawBitmap(newBg, 0, 0, null);
					mpaint.setColor(Color.WHITE);
					mpaint.setStyle(Paint.Style.FILL);
					drawPaddle(canvas, mpaint);
					mpaint.setStyle(Paint.Style.FILL_AND_STROKE);
					canvas.drawCircle(pongBallBody.getPosition().x,
									  pongBallBody.getPosition().y,
									  ((CircleShape) pongBallBody.getShapeList()).getRadius(),
									  mpaint);
					if (state == STATE_RUN || state == STATE_WIN || state == STATE_LOSE) {
						mpaint.setTextSize(30.0f);
						canvas.drawText(Integer.toString(playerScore), getWidth() / 2 - 10.0f, getHeight() / 2 + 60.0f, mpaint);
						canvas.drawText(Integer.toString(computerScore), getWidth() / 2 - 10.0f, getHeight() / 2 - 30.0f, mpaint);
					}
					}
				}
			} finally {
				if (canvas != null) {
					getHolder().unlockCanvasAndPost(canvas);
				}
			}
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
			if (loop.checkState() == ballLoop.STATE_READY) {
				loop.setState(ballLoop.STATE_RUN);
				loop.pushPongBall();
			} else if (loop.checkState() == ballLoop.STATE_PAUSE)
				loop.unpause();
			else if (loop.checkState() == ballLoop.STATE_WIN || loop.checkState() == ballLoop.STATE_LOSE)
				loop.resetGame();
			
			Log.i(TAG, "Game start");
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
			//if (DEBUG_MODE)
			//	Log.i(TAG, "Contact made at (" + x + ", " + y + ")");
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
	
	public void setTextView(TextView textView) {
		mStatusText = textView;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			boolean retry = true;
			loop.setRunning(false);
			while (retry) {
				try {
					loop.join();
					retry = false;
				} catch (InterruptedException e) {
					
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return gestureDetector.onTouchEvent(e);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i(TAG, "Surface changed");
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// When this surface is created, start the thread
		// if the thread isn't already started
		Log.i(TAG, "Surface created");
		loop.setRunning(true);
		Thread.State s = loop.getState();
		try {
			loop.start();
		} catch (IllegalThreadStateException e) {
			if (s == Thread.State.NEW)
				Log.i(TAG, "Thread is new");
			else if (s == Thread.State.BLOCKED)
				Log.i(TAG, "Thread is blocked");
			else if (s == Thread.State.RUNNABLE)
				Log.i(TAG, "Thread is running");
			else if (s == Thread.State.WAITING) {
				Log.i(TAG, "Thread is waiting");
			} else if (s == Thread.State.TERMINATED)
				Log.i(TAG, "Thread is terminated");
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "Surface destroyed");
	}
}
