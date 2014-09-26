package no.plasmid.opencl;

public class PerlinNoise {

	float persistence, frequency, amplitude;
	int octaves, randomseed;
	  
	public PerlinNoise()
	{
		this(0.0f, 0.0f, 0.0f, 0, 0);
	}

	/**
	 * 
	 * @param persistence amount of small bumps
	 * @param frequency amount of big bumps
	 * @param amplitude height difference
	 * @param octaves 
	 * @param randomseed random seed
	 */
	public PerlinNoise(float persistence, float frequency, float amplitude, int octaves, int randomseed)
	{
		this.persistence = persistence;
		this.frequency = frequency;
		this.amplitude  = amplitude;
		this.octaves = octaves;
		this.randomseed = randomseed;
	}

	public float getHeight(float x, float y) {
		return amplitude * total(x, y);
	}
	
	private float total(float x, float y)
	{
		//properties of one octave (changing each loop)
		float t = 0.0f;
		float amplitude = 1;
		float freq = frequency;

		for(int k = 0; k < octaves; k++)  {
			t += genValue(y * freq + randomseed, x * freq + randomseed) * amplitude;
			amplitude *= persistence;
			freq *= 2;
		}

		return t;
	}
	
	private float genValue(float x, float y) {
		int Xint = (int)x;
		int Yint = (int)y;
		float Xfrac = x - Xint;
		float Yfrac = y - Yint;

		//noise values
		float n01 = noise(Xint-1, Yint-1);
		float n02 = noise(Xint+1, Yint-1);
		float n03 = noise(Xint-1, Yint+1);
		float n04 = noise(Xint+1, Yint+1);
		float n05 = noise(Xint-1, Yint);
		float n06 = noise(Xint+1, Yint);
		float n07 = noise(Xint, Yint-1);
		float n08 = noise(Xint, Yint+1);
		float n09 = noise(Xint, Yint);

		float n12 = noise(Xint+2, Yint-1);
		float n14 = noise(Xint+2, Yint+1);
		float n16 = noise(Xint+2, Yint);

		float n23 = noise(Xint-1, Yint+2);
		float n24 = noise(Xint+1, Yint+2);
		float n28 = noise(Xint, Yint+2);

		float n34 = noise(Xint+2, Yint+2);

		//find the noise values of the four corners
		float x0y0 = 0.0625f*(n01+n02+n03+n04) + 0.125f*(n05+n06+n07+n08) + 0.25f*(n09);  
		float x1y0 = 0.0625f*(n07+n12+n08+n14) + 0.125f*(n09+n16+n02+n04) + 0.25f*(n06);  
		float x0y1 = 0.0625f*(n05+n06+n23+n24) + 0.125f*(n03+n04+n09+n28) + 0.25f*(n08);  
		float x1y1 = 0.0625f*(n09+n16+n28+n34) + 0.125f*(n08+n14+n06+n24) + 0.25f*(n04);  

		//interpolate between those values according to the x and y fractions
		float v1 = interpolate(x0y0, x1y0, Xfrac); //interpolate in x direction (y)
		float v2 = interpolate(x0y1, x1y1, Xfrac); //interpolate in x direction (y+1)
		float fin = interpolate(v1, v2, Yfrac);  //interpolate in y direction

		return fin;
	}

	private float interpolate(float x, float y, float a) {
		float negA = 1.0f - a;
		float negASqr = negA * negA;
		float fac1 = 3.0f * (negASqr) - 2.0f * (negASqr * negA);
		float aSqr = a * a;
		float fac2 = 3.0f * aSqr - 2.0f * (aSqr * a);

		return x * fac1 + y * fac2; //add the weighted factors
	}

	private float noise(int x, int y) {
		int n = x + y * 57;
		n = (n << 13) ^ n;
		int t = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
		return (float)(1.0 - t * 0.931322574615478515625e-9);/// 1073741824.0);
	}	
	
}
