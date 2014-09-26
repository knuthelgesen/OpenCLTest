float calculateRange(float* pos, float* otherPos) {
	float deltaX = otherPos[0] - pos[0];
	float deltaY = otherPos[1] - pos[1];
	
	return sqrt((deltaX * deltaX) + (deltaY * deltaY));
}

kernel void physics(global const float* inPos, global const float* inVel,
		global float* outPos, global float* outVel, const int particleCount, const long deltaTime) {
	unsigned int xid = get_global_id(0);
	unsigned index = xid * 2;

	outVel[index] = inVel[index];
	outVel[index + 1] = inVel[index + 1];

	/*
	* Check for collisions against other particles
	*/
	float pos[2] = {inPos[index], inPos[index + 1]};
	for (unsigned int otherIndex = 0; otherIndex < particleCount * 2; otherIndex = otherIndex + 2) {
		if (otherIndex != index) {
			float otherPos[2] = {inPos[otherIndex], inPos[otherIndex + 1]};

			float range = calculateRange(pos, otherPos);
			if (range >= 2.0) {
				//Calculate the gravity between this particle and the other one
				float gForce = 66.74 * (1 / (range * range));
				float unitVector[] = {(otherPos[0] - pos[0]) / range, (otherPos[1] - pos[1]) / range};
				outVel[index] = outVel[index] + ((unitVector[0] * gForce / 1000) * deltaTime);
				outVel[index + 1] = outVel[index + 1] + ((unitVector[1] * gForce / 1000) * deltaTime);
			}
			if (range < 2.0) {
				//Collision occurred
				outVel[index] = inVel[otherIndex];
				outVel[index + 1] = inVel[otherIndex + 1];
			}
		}
	}
	
	/*
	* Check for collisions on the wall
	*/
	
	if (inPos[index] <= 0) {
		//Hit left side
		outVel[index] = -inVel[index];
		outPos[index] = 0;
	}
	if (inPos[index] >= 1800) {
		//Hit right side
		outVel[index] = -inVel[index];
		outPos[index] = 1800;
	}
	if (inPos[index + 1] <= 0) {
		//Hit the floor
		outVel[index + 1] = -inVel[index + 1];
		outPos[index + 1] = 0;
	}
	if (inPos[index + 1] >= 1000) {
		//Hit the ceiling
		outVel[index + 1] = -inVel[index + 1];
		outPos[index + 1] = 1000;
	}


	//Brake a little
	outVel[index] = outVel[index] * (1.0f - (0.00001 * deltaTime));
	outVel[index + 1] = outVel[index + 1] * (1.0f - (0.00001 * deltaTime));
	
	outPos[index] = inPos[index] + ((outVel[index] / 1000) * deltaTime);
	outPos[index + 1] = inPos[index + 1] + ((outVel[index + 1] / 1000) * deltaTime);
}