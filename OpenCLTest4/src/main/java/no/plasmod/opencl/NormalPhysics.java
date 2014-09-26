package no.plasmod.opencl;

public class NormalPhysics {

	private static final float G = 66.74f;
	
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
			
			//Check for collisions and gravity interactions against other particles
			for (int j = i + 1; j < particles.length; j++) {
				Particle otherParticle = particles[j];
				float[] otherVel = otherParticle.getVelocity();
				float[] otherPos = otherParticle.getPosition();
				
				float range = calculateRange(pos, otherParticle.getPosition());
				if (range >= 2.0) {
					//Calculate gravity between the two particles, and apply it
					float gForce = G * (1 / (range * range));
					float[] unitVector = new float[]{(otherPos[0] - pos[0]) / range, (otherPos[1] - pos[1]) / range};
					vel[0] += (unitVector[0] * gForce / 1000) * deltaTime;
					vel[1] += (unitVector[1] * gForce / 1000) * deltaTime;
					
					unitVector = new float[]{(pos[0] - otherPos[0]) / range, (pos[1] - otherPos[1]) / range};
					otherVel[0] += (unitVector[0] * gForce / 1000) * deltaTime;
					otherVel[1] += (unitVector[1] * gForce / 1000) * deltaTime;
				}

				if (!otherParticle.equals(particle)) {
					if (range < 2.0f) {
						//Collision occurred
						float[] temp = new float[]{vel[0], vel[1]};
						
						vel[0] = otherVel[0];
						vel[1] = otherVel[1];
						otherVel[0] = temp[0];
						otherVel[1] = temp[1];
					}
				}
			}			
		}
		
		//Update all particle positions
		for (Particle particle : particles) {
			float[] pos = particle.getPosition();
			float[] vel = particle.getVelocity();
			//Brake a little
			vel[0] = vel[0] * (1.0f - (0.00001f * deltaTime));
			vel[1] = vel[1] * (1.0f - (0.00001f * deltaTime));

			//Update position
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
