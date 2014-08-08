package no.plasmid.opencl;

import java.io.FileNotFoundException;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;

public class App extends AbstractApp {

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
	
	static int dataSize = 67108864;
	
	private FloatBuffer aBuffer;
	private FloatBuffer bBuffer;
	private FloatBuffer answerBuffer;
	
	private CLMem aMem = null;
	private CLMem bMem = null;
	private CLMem answerMem = null;
	
	private CLProgram program = null;
	private CLKernel kernel = null;
	
	private void prepareDataAndProgram() throws LWJGLException, FileNotFoundException {
		aBuffer = BufferUtils.createFloatBuffer(dataSize);
		bBuffer = BufferUtils.createFloatBuffer(dataSize);
		for (int i = 0; i < dataSize; i++) {
			aBuffer.put(i, i);
			bBuffer.put(i, dataSize - i);
		}
		aBuffer.rewind();
		bBuffer.rewind();
		answerBuffer = BufferUtils.createFloatBuffer(dataSize);
		System.out.println("Data ready");
		
		//Allocate memory and copy data
		aMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, aBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		CL10.clEnqueueWriteBuffer(commandQueue, aMem, CL10.CL_TRUE, 0, aBuffer, null, null);
		bMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, bBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		CL10.clEnqueueWriteBuffer(commandQueue, bMem, CL10.CL_TRUE, 0, bBuffer, null, null);
		answerMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, answerBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		CL10.clEnqueueWriteBuffer(commandQueue, answerMem, CL10.CL_TRUE, 0, answerBuffer, null, null);
		CL10.clFinish(commandQueue);
		System.out.println("Data copied to OpenCL");
		
		//Load program source
		String source = loadTextFile("/cl/modulo.cl");
		
		//Create the program
		program = CL10.clCreateProgramWithSource(context, source, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		Util.checkCLError(CL10.clBuildProgram(program, deviceList.get(0), "", null));
		//Sum has to match a kernel method name in the OpenCL source
		kernel = CL10.clCreateKernel(program, "sum", errorCodeBuffer);
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
		if (null != aMem) {
			CL10.clReleaseMemObject(aMem);
		}
		if (null != bMem) {
			CL10.clReleaseMemObject(bMem);
		}
		if (null != answerMem) {
			CL10.clReleaseMemObject(answerMem);
		}
	}
	
	private void calculateNormal() {
		for (int i = 0; i < dataSize; i++) {
			answerBuffer.put(i, aBuffer.get(i) %+ bBuffer.get(i));
		}
	}
	
	private void calculateOpenCL() throws LWJGLException {
		//Execute the kernel
		PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
		kernel1DGlobalWorkSize.put(0, Math.max(dataSize, 1));
		kernel.setArg(0, aMem);
		kernel.setArg(1, bMem);
		kernel.setArg(2, answerMem);
		CL10.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
		
		//Read back results
		CL10.clEnqueueReadBuffer(commandQueue, answerMem, CL10.CL_TRUE, 0, answerBuffer, null, null);
		CL10.clFinish(commandQueue);
	}
	
	private void printResults(long startTime, long endTime) {
		System.out.println("Start time: " + startTime);
		System.out.println("End time: " + startTime);
		System.out.println("Time used: " + (endTime - startTime));
		System.out.println("" + answerBuffer.get(1));
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
