package no.plasmid.opencl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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

public abstract class AbstractLWJGLApp {

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
		URL fileURL = AbstractLWJGLApp.class.getResource(fileName);
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

  /**
   * Load native libraries required by LWJGL based on platform the application is running on.
   * @param platform the platform the application is running on.
   */
  protected void loadNatives(SupportedPlatform platform) {
  	if (null == platform) {
  		throw new IllegalArgumentException("Platform can not be null");
  	}
  	
  	//Application running path
  	String path = getCodeSourcePathString();
  	
  	//Copy required libraries based on platform
  	switch (platform) {
		case LINUX:
	  	copyFileFromJar("/native/linux/libjinput-linux.so", path + "/native/libjinput-linux.so");
			copyFileFromJar("/native/linux/liblwjgl.so", path + "/native/liblwjgl.so");
			copyFileFromJar("/native/linux/libopenal.so", path + "/native/libopenal.so");  	
			break;
		case LINUX_64:
	  	copyFileFromJar("/native/linux/libjinput-linux64.so", path + "/native/libjinput-linux64.so");
			copyFileFromJar("/native/linux/liblwjgl64.so", path + "/native/liblwjgl64.so");
			copyFileFromJar("/native/linux/libopenal64.so", path + "/native/libopenal64.so");  	
			break;
		case MAC:
	  	copyFileFromJar("/native/macosx/libjinput-osx.jnilib", path + "/native/libjinput-osx.jnilib");
			copyFileFromJar("/native/macosx/liblwjgl.lnilib", path + "/native/liblwjgl.lnilib");
			copyFileFromJar("/native/macosx/openal.dylib", path + "/native/openal.dylib");  	
			break;
		case MAC_64:
	  	copyFileFromJar("/native/macosx/libjinput-osx.jnilib", path + "/native/libjinput-osx.jnilib");
			copyFileFromJar("/native/macosx/liblwjgl.lnilib", path + "/native/liblwjgl.lnilib");
			copyFileFromJar("/native/macosx/openal.dylib", path + "/native/openal.dylib");  	
			break;
		case WINDOWS:
	  	copyFileFromJar("/native/windows/jinput-raw.dll", path + "/native/jinput-raw.dll");
			copyFileFromJar("/native/windows/jinput-dx8.dll", path + "/native/jinput-dx8.dll");
			copyFileFromJar("/native/windows/lwjgl.dll", path + "/native/lwjgl.dll");
			copyFileFromJar("/native/windows/OpenAL32.dll", path + "/native/OpenAL32.dll");  	
			break;
		case WINDOWS_64:
	  	copyFileFromJar("/native/windows/jinput-raw_64.dll", path + "/native/jinput-raw_64.dll");
			copyFileFromJar("/native/windows/jinput-dx8_64.dll", path + "/native/jinput-dx8_64.dll");
			copyFileFromJar("/native/windows/lwjgl64.dll", path + "/native/lwjgl64.dll");
			copyFileFromJar("/native/windows/OpenAL64.dll", path + "/native/OpenAL64.dll");  	
			break;
		default:
  		throw new IllegalStateException("Error when opening native library");
  	}

  	//Set library path to the VM
		System.setProperty("java.library.path", path + "native/");
		try {
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

  /**
   * Copy a file from a .jar (use classloader) to a file on the file system.
   * @param source the name of the source file.
   * @param destination the name of the destination file.
   */
  protected void copyFileFromJar(String source, String destination) {
		InputStream is = null;
		FileOutputStream os = null;
		try {
	  	is = AbstractLWJGLApp.class.getResourceAsStream(source);
	  	if (null == is) {
	  		throw new IllegalStateException("Error when opening native library");
	  	}
	  	os = new FileOutputStream(destination);
	  	
	  	byte[] byteBuffer = new byte[1024];
	  	int readBytes = -1;
	  	do {
	  		readBytes = is.read(byteBuffer);
	  		if (readBytes != -1) {
	  			os.write(byteBuffer, 0, readBytes);
	  		}
	  	} while (readBytes != -1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != is) {
					is.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (null != os) {
					os.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected enum SupportedPlatform {
  	LINUX, LINUX_64, MAC, MAC_64, WINDOWS, WINDOWS_64;
  	
  	public static SupportedPlatform getPlatformForOS() {
    	String OS_Arch = System.getProperty("os.arch").toLowerCase();
    	String OS_Name = System.getProperty("os.name").toLowerCase();
    	
    	SupportedPlatform rc = null;

    	if (OS_Name.indexOf("linux") >= 0) {
    		if (OS_Arch.indexOf("64") >= 0) {
    			rc = LINUX_64;
    		} else {
    			rc = LINUX;
    		}
    	}
    	if (OS_Name.indexOf("mac") >= 0) {
    		if (OS_Arch.indexOf("64") >= 0) {
    			rc = MAC_64;
    		} else {
    			rc = MAC;
    		}
    	}
    	if (OS_Name.indexOf("win") >= 0) {
    		if (OS_Arch.indexOf("64") >= 0) {
    			rc = WINDOWS_64;
    		} else {
    			rc = WINDOWS;
    		}
    	}
    	
    	if (null == rc) {
    		throw new IllegalStateException("Platform not supported: " + OS_Name + OS_Arch);
    	}
    	
    	return rc;
  	}
  	
  }
	
	protected abstract String getCodeSourcePathString();
	
}
