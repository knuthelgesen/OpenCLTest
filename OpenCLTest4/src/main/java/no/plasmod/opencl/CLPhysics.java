package no.plasmod.opencl;

import java.io.FileNotFoundException;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.OpenCLException;
import org.lwjgl.opencl.Util;

import no.plasmid.opencl.AbstractCLController;

public class CLPhysics extends AbstractCLController {

	private CLProgram physicsProgram;
	private CLKernel physicsKernel;
	
	//Buffers to hold data for host program
	private FloatBuffer currentPosBuffer;
	private FloatBuffer currentVelBuffer;
	private FloatBuffer nextPosBuffer;
	private FloatBuffer nextVelBuffer;
	
	//Memory locations to hold data on the graphics card
	private CLMem currentPosMem = null;
	private CLMem currentVelMem = null;
	private CLMem nextPosMem = null;
	private CLMem nextVelMem = null;
	
	public void initializeCLPhysics(Particle[] particles) throws LWJGLException, FileNotFoundException {
		//Initialize OpenCL
		initializeOpenCL();
		
		//Initialize the program
		String source = loadTextFile("/cl/physics.cl");
		physicsProgram = CL10.clCreateProgramWithSource(context, source, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		try {
			Util.checkCLError(CL10.clBuildProgram(physicsProgram, deviceList.get(0), "", null));
		} catch (OpenCLException e) {
			//Check compilation error
			System.err.println(physicsProgram.getBuildInfoString(
					deviceList.get(0), CL10.CL_PROGRAM_BUILD_LOG));
		}
		//Sum has to match a kernel method name in the OpenCL source
		physicsKernel = CL10.clCreateKernel(physicsProgram, "physics", errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		System.out.println("OpenCL program ready");		
		
		//Initialize the data
		//We need four buffers. Two for positions (prev-next) and two for velocities (prev-next). Each value is two floats (two dimensions)
		currentPosBuffer = BufferUtils.createFloatBuffer(particles.length * 2);
		currentVelBuffer = BufferUtils.createFloatBuffer(particles.length * 2);
		nextPosBuffer = BufferUtils.createFloatBuffer(particles.length * 2);
		nextVelBuffer = BufferUtils.createFloatBuffer(particles.length * 2);
		
		//Fill in data to the buffers
		for (Particle particle : particles) {
			currentPosBuffer.put(particle.getPosition());
			currentVelBuffer.put(particle.getVelocity());
		}
		currentPosBuffer.rewind();
		currentVelBuffer.rewind();
		
		//Allocate memory and copy data
		currentPosMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, currentPosBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
//		CL10.clEnqueueWriteBuffer(commandQueue, currentPosMem, CL10.CL_TRUE, 0, currentPosBuffer, null, null);
		currentVelMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, currentVelBuffer, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
//		CL10.clEnqueueWriteBuffer(commandQueue, currentVelMem, CL10.CL_TRUE, 0, currentVelBuffer, null, null);
		nextPosMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR,  nextPosBuffer, errorCodeBuffer);
//		CL10.clEnqueueWriteBuffer(commandQueue, nextPosMem, CL10.CL_TRUE, 0, nextPosBuffer, null, null);
		checkErrorCodeBuffer(errorCodeBuffer);
		nextVelMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, nextVelBuffer, errorCodeBuffer);
//		CL10.clEnqueueWriteBuffer(commandQueue, nextVelMem, CL10.CL_TRUE, 0, nextVelBuffer, null, null);
		CL10.clFinish(commandQueue);
		checkErrorCodeBuffer(errorCodeBuffer);
		
		System.out.println("Data ready and copied to OpenCL");		
	}
	
	public void doPhysics(int particleCount, long deltaTime) {
		currentPosBuffer.rewind();
		currentVelBuffer.rewind();
		CL10.clEnqueueWriteBuffer(commandQueue, currentPosMem, CL10.CL_TRUE, 0, currentPosBuffer, null, null);
		CL10.clEnqueueWriteBuffer(commandQueue, currentVelMem, CL10.CL_TRUE, 0, currentVelBuffer, null, null);
		CL10.clFinish(commandQueue);
		
		currentPosBuffer.rewind();
		currentVelBuffer.rewind();
		//Set which buffers to use for this calculation
		physicsKernel.setArg(0, currentPosMem);
		physicsKernel.setArg(1, currentVelMem);
		physicsKernel.setArg(2, nextPosMem);
		physicsKernel.setArg(3, nextVelMem);
		physicsKernel.setArg(4, particleCount);
		physicsKernel.setArg(5, deltaTime);
		checkErrorCodeBuffer(errorCodeBuffer);
		
		currentPosBuffer.rewind();
		currentVelBuffer.rewind();
		//Perform calculation
		PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
		kernel1DGlobalWorkSize.put(0, Math.max(particleCount, 1));
		CL10.clEnqueueNDRangeKernel(commandQueue, physicsKernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
		checkErrorCodeBuffer(errorCodeBuffer);
		
		currentPosBuffer.rewind();
		currentVelBuffer.rewind();
		//Download results
		CL10.clEnqueueReadBuffer(commandQueue, nextPosMem, CL10.CL_TRUE, 0, currentPosBuffer, null, null);
		CL10.clEnqueueReadBuffer(commandQueue, nextVelMem, CL10.CL_TRUE, 0, currentVelBuffer, null, null);
		CL10.clFinish(commandQueue);
		checkErrorCodeBuffer(errorCodeBuffer);
		currentPosBuffer.rewind();
		currentVelBuffer.rewind();
	}
	
	public void cleanupCLPhysics() {
		//Clean up the data
		if (null != nextVelMem) {
			CL10.clReleaseMemObject(nextVelMem);
		}
		if (null != nextPosMem) {
			CL10.clReleaseMemObject(nextPosMem);
		}
		if (null != currentVelMem) {
			CL10.clReleaseMemObject(currentVelMem);
		}
		if (null != currentPosMem) {
			CL10.clReleaseMemObject(currentPosMem);
		}
		
		//Clean up the program
		if (null != physicsKernel) {
			CL10.clReleaseKernel(physicsKernel);
		}
		if (null != physicsProgram) {
			CL10.clReleaseProgram(physicsProgram);
		}
		
		//Clean up OpenCL
		cleanupOpenCL();
	}

	public FloatBuffer getCurrentPosBuffer() {
		return currentPosBuffer;
	}
	
}
