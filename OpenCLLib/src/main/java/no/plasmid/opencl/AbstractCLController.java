package no.plasmid.opencl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Scanner;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;

public abstract class AbstractCLController {

	protected IntBuffer errorCodeBuffer;
	
	protected CLPlatform platform = null;
	protected List<CLDevice> deviceList = null;
	protected CLContext context = null;
	protected CLCommandQueue commandQueue = null;
	
	protected void initializeOpenCL() throws LWJGLException {
		errorCodeBuffer = BufferUtils.createIntBuffer(1);

		CL.create();
		//Prepare platform
		platform = CLPlatform.getPlatforms().get(0);
		//Get list of devices
		deviceList = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
		//Get context
		context = CLContext.create(platform, deviceList, null, null, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		//Create command queue
		commandQueue = CL10.clCreateCommandQueue(context, deviceList.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
			
		ByteBuffer buffer = BufferUtils.createByteBuffer(100);
		CL10.clGetPlatformInfo(platform, CL10.CL_PLATFORM_NAME, buffer, null);
		printChars(buffer);
		buffer.rewind();
		CL10.clGetPlatformInfo(platform, CL10.CL_PLATFORM_VERSION, buffer, null);
		printChars(buffer);
	}
	
	protected void cleanupOpenCL() {
		//Clean up OpenCL resources
		if (null != commandQueue) {
			CL10.clReleaseCommandQueue(commandQueue);
		}
		if (null != context) {
			CL10.clReleaseContext(context);
		}
		CL.destroy();
	}
	
	protected void checkErrorCodeBuffer(IntBuffer errorCodeBuffer) {
		if (errorCodeBuffer.get(0) != 0) {
			System.out.println("Got error code " + errorCodeBuffer.get(0) + " from OpenCL");
		}
	}
	
	protected void printChars(ByteBuffer buffer) {
		for (int i = 0; i < buffer.capacity(); i++) {
			System.out.print(((char)buffer.get(i)));
		}
		System.out.println("");
	}
	
	protected String loadTextFile(String fileName) throws FileNotFoundException {
		URL fileURL = AbstractCLController.class.getResource(fileName);
		if (fileURL == null) {
			throw new FileNotFoundException("Could not find shader source file " + fileName);
		}
		
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		FileInputStream fis;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(fileURL.getFile());
			scanner = new Scanner(fis);
			
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + NL);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		
		return text.toString();
	}
	
}
