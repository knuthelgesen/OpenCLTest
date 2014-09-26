package no.plasmod.opencl;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.NVMultisampleFilterHint;
import org.lwjgl.util.glu.GLU;

public class Renderer {
	
	public static void initOpenGL(int boxWidth, int boxHeight) {
		/*
		 * GL context
		 */
		GL11.glViewport(0, 0, boxWidth, boxHeight);
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GL11.glPointSize(5);
		
		//Enable multisample anti aliasing
		GL11.glEnable(GL13.GL_MULTISAMPLE);
		GL11.glHint(NVMultisampleFilterHint.GL_MULTISAMPLE_FILTER_HINT_NV, GL11.GL_NICEST);

		GLU.gluOrtho2D(0, boxWidth, 0, boxHeight);
		
//		checkGL();
	}
	
	public static void render(Particle[] particles) {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		
		GL11.glBegin(GL11.GL_POINTS);
		{
			for (Particle particle : particles) {
				float[] position = particle.getPosition();
				GL11.glVertex2f(position[0], position[1]);
			}
		}
		GL11.glEnd();
	}

	public static void render(FloatBuffer posBuffer, int particleCount) {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		
		GL11.glBegin(GL11.GL_POINTS);
		{
			for (int i = 0; i < particleCount; i++) {
				GL11.glVertex2f(posBuffer.get(), posBuffer.get());
			}
		}
		GL11.glEnd();
	}

}
