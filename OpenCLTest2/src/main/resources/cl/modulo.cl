kernel void sum(global const int* xPos, global const int* yPos, global float *answer) {
	unsigned int xid = get_global_id(0);
	answer[xid] = xPos[xid] % yPos[xid];
}