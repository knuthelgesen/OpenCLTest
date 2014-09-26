package no.plasmod.opencl;

public class NormalPhysics {

	private static final float gAcceleration = -9.81f;	//In m/s², down
	
	public void doPhysics(Particle[] particles, int boxWidth, int boxHeight, long deltaTime) {
		//Update all particle directions
		for (int i = 0; i < particles.length - 1; i++) {
			Particle particle = particles[i];
			float[] pos = particle.getPosition();
			float[] vel = particle.getVelocity();
			//Check for collisions against the walls
			if (pos[0] <= 0) {
				//Hit left side
				vel[0] = -vel[0];
				pos[0] = 0;
			}
			if (pos[0] >= boxWidth) {
				//Hit right side
				vel[0] = -vel[0];
				pos[0] = boxWidth;
			}
			if (pos[1] <= 0) {
				//Hit the floor
				vel[1] = -vel[1];
				pos[1] = 0;
			}
			if (pos[1] >= boxHeight) {
				//Hit the ceiling
				vel[1] = -vel[1];
				pos[1] = boxHeight;
			}
			
			//Check for collisions against other particles
			for (int j = i + 1; j < particles.length; j++) {
				Particle otherParticle = particles[j];
				if (!otherParticle.equals(particle)) {
					if (calculateRange(pos, otherParticle.getPosition()) < 2.0f) {
						//Collision occurred
//						float[] otherPos = otherParticle.getPosition();
						float[] otherVel = otherParticle.getVelocity();

						float[] temp = new float[]{vel[0], vel[1]};
						
						vel[0] = otherVel[0];
						vel[1] = otherVel[1];
						otherVel[0] = temp[0];
						otherVel[1] = temp[1];
						
//						pos[0] = pos[0] + vel[0];
//						pos[1] = pos[1] + vel[1];
//						otherPos[0] = otherPos[0] + otherVel[0];
//						otherPos[1] = otherPos[1] + otherVel[1];
					}
				}
			}			
			
			//Apply gravity
//			vel[1] = vel[1] + (gAcceleration / 1000) * deltaTime;
			
			//Brake a little
//			vel[0] = vel[0] * (1.0f - (0.0001f * deltaTime));
//			vel[1] = vel[1] * (1.0f - (0.0001f * deltaTime));
		}
		
		//Update all particle positions
		for (Particle particle : particles) {
			float[] pos = particle.getPosition();
			float[] vel = particle.getVelocity();
			pos[0] = pos[0] + ((vel[0] / 1000) * deltaTime);
			pos[1] = pos[1] + ((vel[1] / 1000) * deltaTime);
		}
	}
	
	private float calculateRange(float[] pos, float[] otherPos) {
		float deltaX = otherPos[0] - pos[0];
		float deltaY = otherPos[1] - pos[1];
		return (float)(Math.sqrt((deltaX * deltaX) + (deltaY * deltaY))); 
	}
	
}
