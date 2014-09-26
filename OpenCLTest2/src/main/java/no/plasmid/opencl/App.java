package no.plasmid.opencl;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.OpenCLException;
import org.lwjgl.opencl.Util;

public class App extends AbstractLWJGLApp {

	public static final float NOISE_PERSISTENCE	= 0.42f;
	public static final float NOISE_FREQUENCY	= 0.030f;
	public static final float NOISE_AMPLITUDE	= 70.0f;
	public static final int NOISE_OCTAVES			= 10;
	public static final int NOISE_RANDOM_SEED		= 3;
	
	public static void main(String[] args) throws LWJGLException, FileNotFoundException {
		App app = new App();
  	app.loadNatives(SupportedPlatform.getPlatformForOS());

  	//Init OpenCL
  	app.initializeOpenCL();
  	//Prepare the data and program
  	app.prepareDataAndProgram();
  	//Do calculations
  	long startTime = System.currentTimeMillis();
  	app.calculateOpenCL();
//  	app.calculateNormal();
  	long endTime = System.currentTimeMillis();
  	//Clean up data and program
  	app.cleanupDataAndProgram();
  	//Clean up OpenCL
  	app.cleanupOpenCL();
  	//Print results
  	app.printResults(startTime, endTime);
	}
	
	private PerlinNoise noise;
	
	private int dataSize = 256;
	
	private IntBuffer xPosBuffer;
	private IntBuffer yPosBuffer;
	private FloatBuffer answerBuffer;
	
	private CLMem xPosMem = null;
	private CLMem yPosMem = null;
	private CLMem answerMem = null;
	
	private CLProgram program = null;
	private CLKernel kernel = null;
	
	private void prepareDataAndProgram() throws LWJGLException, FileNotFoundException {
		//Prepare the noise implemented in Java
		noise = new PerlinNoise(NOISE_PERSISTENCE, NOISE_FREQUENCY, NOISE_AMPLITUDE, NOISE_OCTAVES, NOISE_RANDOM_SEED);
		
		xPosBuffer = BufferUtils.createIntBuffer(dataSize * dataSize);
		yPosBuffer = BufferUtils.createIntBuffer(dataSize * dataSize);
		for (int i = 0; i < dataSize * dataSize; i++) {
			xPosBuffer.put(i, i % dataSize);
			yPosBuffer.put(i, i / dataSize);
		}
		xPosBuffer.rewind();
		yPosBuffer.rewind();
		answerBuffer = BufferUtils.createFloatBuffer(dataSize * dataSize);
		System.out.println("Data ready");
		
		//Allocate memory and copy data
		xPosMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, xPosBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		CL10.clEnqueueWriteBuffer(commandQueue, xPosMem, CL10.CL_TRUE, 0, xPosBuffer, null, null);
		yPosMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, yPosBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		CL10.clEnqueueWriteBuffer(commandQueue, yPosMem, CL10.CL_TRUE, 0, yPosBuffer, null, null);
		answerMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, answerBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		CL10.clEnqueueWriteBuffer(commandQueue, answerMem, CL10.CL_TRUE, 0, answerBuffer, null, null);
		CL10.clFinish(commandQueue);
		System.out.println("Data copied to OpenCL");
		
		//Load program source
		String source = loadTextFile("/cl/noise.cl");
		
		//Create the program
		program = CL10.clCreateProgramWithSource(context, source, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		try {
			Util.checkCLError(CL10.clBuildProgram(program, deviceList.get(0), "", null));
		} catch (OpenCLException e) {
			ByteBuffer buffer = BufferUtils.createByteBuffer(1000);
			CL10.clGetProgramBuildInfo(program, deviceList.get(0), CL10.CL_PROGRAM_BUILD_LOG, buffer, null);
			printChars(buffer);
			
			System.out.println(program.getBuildInfoString(deviceList.get(0), CL10.CL_PROGRAM_BUILD_LOG));
			throw e;
		}
		//Sum has to match a kernel method name in the OpenCL source
		kernel = CL10.clCreateKernel(program, "getheight", errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		System.out.println("OpenCL program ready");
	}

	private void cleanupDataAndProgram() {
		//Clean up OpenCL resources
		if (null != kernel) {
			CL10.clReleaseKernel(kernel);
		}
		if (null != program) {
			CL10.clReleaseProgram(program);
		}
		if (null != xPosMem) {
			CL10.clReleaseMemObject(xPosMem);
		}
		if (null != yPosMem) {
			CL10.clReleaseMemObject(yPosMem);
		}
		if (null != answerMem) {
			CL10.clReleaseMemObject(answerMem);
		}
	}
	
	private void calculateNormal() {
		for (int x = 0; x < dataSize; x++) {
			for (int y = 0; y < dataSize; y++) {
				answerBuffer.put((float)noise.getHeight(x, y));
			}
		}
	}
	
	private void calculateOpenCL() throws LWJGLException {
		//Execute the kernel
		PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
		kernel1DGlobalWorkSize.put(0, Math.max(dataSize * dataSize, 1));
		kernel.setArg(0, NOISE_PERSISTENCE);
		kernel.setArg(1, NOISE_FREQUENCY);
		kernel.setArg(2, NOISE_AMPLITUDE);
		kernel.setArg(3, NOISE_OCTAVES);
		kernel.setArg(4, NOISE_RANDOM_SEED);
		kernel.setArg(5, xPosMem);
		kernel.setArg(6, yPosMem);
		kernel.setArg(7, answerMem);
//		kernel.setArg(0, xPosMem);
//		kernel.setArg(1, yPosMem);
//		kernel.setArg(2, answerMem);
		CL10.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
		
		//Read back results
		CL10.clEnqueueReadBuffer(commandQueue, answerMem, CL10.CL_TRUE, 0, answerBuffer, null, null);
		CL10.clFinish(commandQueue);
	}
	
	private void printResults(long startTime, long endTime) {
		System.out.println("Start time: " + startTime);
		System.out.println("End time: " + startTime);
		System.out.println("Time used: " + (endTime - startTime));
		System.out.println(answerBuffer.get(1000));
	}
	
	private void print(FloatBuffer buffer) {
		for (int i = 0; i < buffer.capacity(); i++) {
			System.out.print(buffer.get(i) + " ");
		}
		System.out.println("");
	}

	@Override
	protected String getCodeSourcePathString() {
  	return App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}
	
}
