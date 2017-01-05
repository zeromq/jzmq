package org.zeromq;

import java.io.File;

public class TestUtils {
	/**
	 * Remove a directory and delete all files and subdir recursively 
	 * CAUTION: watchout with symbolic links. not tested how the behaviour is but suspect this function will follow those as well.
	 * @param path as String
	 */
	public static void cleanupDir(String path) {
		cleanupSimpleDir(new File(path));
	}	
	
	/**
	 * Remove a directory and delete all files and subdir recursively
	 * CAUTION: watchout with symbolic links. not tested how the behaviour is but suspect this function will follow those as well.
	 * @param path File-path
	 */
	public static void cleanupSimpleDir(File path) {
		if (!path.exists()){
			return;
		}
			
		for (File fileToDelete : path.listFiles()) {
			if (fileToDelete.isDirectory()) {
				cleanupSimpleDir(fileToDelete);
			}
			fileToDelete.delete();
//			System.out.println("Deleted "+fileToDelete.getAbsolutePath());
		}
		path.delete();
	}
	
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
}
