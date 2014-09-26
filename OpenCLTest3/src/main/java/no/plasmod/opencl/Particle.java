package no.plasmod.opencl;

public class Particle {

	private float[] position;
	private float[] velocity;
	
	public Particle() {
		position = new float[2];
		velocity = new float[2];
	}
	
	public float[] getPosition() {
		return position;
	}
	
	public float[] getVelocity() {
		return velocity;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Particle{pos:[");
		sb.append(position[0]);
		sb.append(", ");
		sb.append(position[1]);
		sb.append("], vel:[");
		sb.append(velocity[0]);
		sb.append(", ");
		sb.append(velocity[1]);
		sb.append("]}");
		return sb.toString();
	}

}
