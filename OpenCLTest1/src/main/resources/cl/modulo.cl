kernel void sum(global const float *a, global const float *b, global float *answer) {
	unsigned int xid = get_global_id(0);
	answer[xid] = fmod(a[xid], b[xid]);
}