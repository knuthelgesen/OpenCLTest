package no.plasmod.opencl;

import java.io.FileNotFoundException;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import no.plasmid.opencl.AbstractLWJGLApp;

public class App extends AbstractLWJGLApp {

	private static boolean useOpenCL = true;
	
	private static int particleCount = 4000;
	
	private static int boxWidth = 1800;
	private static int boxHeight = 1000;
	
	private Particle[] particles;
	
	private static CLPhysics clPhysics;
	private static NormalPhysics normalPhysics;
	
	public static void main(String[] args) throws LWJGLException, FileNotFoundException {
		App app = new App();
  	app.loadNatives(SupportedPlatform.getPlatformForOS());

  	clPhysics = new CLPhysics();
  	normalPhysics = new NormalPhysics();
  	
  	//Init OpenGL
  	app.initializeOpenGL();
  	Renderer.initOpenGL(boxWidth, boxHeight);
  	
  	//Prepare data
  	app.particles = app.prepareData();
  	//Init OpenCL
  	if (useOpenCL) {
  		clPhysics.initializeCLPhysics(app.particles);
  	}
  	
  	//Loop and run simulation until window is closed
  	app.mainLoop();
  	
  	//Clean up OpenCL
  	if (useOpenCL) {
    	clPhysics.cleanupCLPhysics();
  	}
  	
  	//Clean up OpenGL
  	app.cleanupOpenGL();
	}
	
	private void mainLoop() {
		long startTime = 0L;
		long endTime = 0L;
		long deltaTime = 0L;
		while (!Display.isCloseRequested()) {
			startTime = System.currentTimeMillis();
			//Run the physics simulation
	  	if (useOpenCL) {
	  		clPhysics.doPhysics(particleCount, deltaTime);
				//Render
				Renderer.render(clPhysics.getCurrentPosBuffer(), particleCount);
	  	} else {
				normalPhysics.doPhysics(particles, boxWidth, boxHeight, deltaTime);
				//Render
				Renderer.render(particles);
	  	}
			
			//Update the display
			Display.update();
			
			endTime = System.currentTimeMillis();
			deltaTime = endTime - startTime;
//			System.out.println(deltaTime);
		}
	}
	
	private Particle[] prepareData() {
		Particle[] rc = new Particle[particleCount];
		
		for (int i = 0; i < particleCount; i++) {
			rc[i] = createRandomizedParticle();
		}
		
		checkNoCollisions(rc);
		
		return rc;
	}
	
	private void checkNoCollisions(Particle[] rc) {
		boolean collisions = false;
		do {
			collisions = false;
			for (int i = 0; i < rc.length - 1; i++) {
				Particle particle = rc[i];
				for (int j = i; j < rc.length - 1; j++) {
					if (calculateRange(particle.getPosition(), rc[j].getPosition()) < 10.0f) {
						collisions = true;
						rc[i] = createRandomizedParticle();
					}
				}
			}
		} while (!collisions);
	}

	private float calculateRange(float[] pos, float[] otherPos) {
		float deltaX = otherPos[0] - pos[0];
		float deltaY = otherPos[1] - pos[1];
		return (float)(Math.sqrt((deltaX * deltaX) + (deltaY * deltaY))); 
	}


	private Particle createRandomizedParticle() {
		Particle rc = new Particle();
		
		float[] pos = rc.getPosition();
		pos[0] = (float)Math.random() * boxWidth;
		pos[1] = (float)Math.random() * boxHeight;
		
		float[] vel = rc.getVelocity();
//		vel[0] = (float)Math.random() * 150 - 75;
//		vel[1] = (float)Math.random() * 150 - 75;
		
		return rc;
	}
	
	private void initializeOpenGL() {
  	//Open the program window
    try {
    	PixelFormat pf = new PixelFormat(24, 8, 24, 0, 16);
    	Display.setDisplayMode(new DisplayMode(boxWidth, boxHeight));
    	Display.setTitle("Particles");
    	Display.setVSyncEnabled(true);
    	Display.create(pf);
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
		}
	}
	
	private void cleanupOpenGL() {
  	//Destroy the program window
  	Display.destroy();
	}
	
	@Override
	protected String getCodeSourcePathString() {
  	return App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

}
