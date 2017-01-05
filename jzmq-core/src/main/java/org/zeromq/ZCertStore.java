package org.zeromq;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
/**
 * Simple certstore that manages certificate file in a directory. Those files need to be in ZMP-Format which is created by ZConf.java
 * 
 * @author thomas (dot) trocha (at) gmail (dot) com
 *
 */
public class ZCertStore {

	private static interface IFileVisitor {
		void handleFile(File f);
		void handleDir(File f);
	}
	
	
	private String location; //  Directory location
	//  This isn't sufficient; we should check the hash of all files
	//  or else use a trigger like inotify on Linux.
	private long modified;	//  Modified time of directory
	private int certCount;		//  Number of certificates
	private int fileSize;	//  Total size of certificates
	private int tempCounter;
	
	private boolean fileCheckRunning = false;
	private boolean requestStopThread = false;
	
	// save publickey as key and the modified-date in milliseconds as value
	private Set<String> publicKeys = new HashSet<String>();
	
	/**
	 * Create a CertificationStore at that filesystem location
	 * 
	 * @param location
	 */
	public ZCertStore(String location) {
		loadFiles(location);
//		startDummyCheckThread(1000);
	}
	
	private void traverseDirectory(String path,IFileVisitor visitor) {
		File root = new File(path);
		traverseDirectory(root, visitor);
	}	
	private void traverseDirectory(File root,IFileVisitor visitor) {
		if (!root.exists()) {
			throw new RuntimeException("There is no path:"+root.getPath());
		}
		if (!root.isDirectory()) {
			throw new RuntimeException("Path:"+root.getPath()+" is not a directory!");
		}
		
		for (File f : root.listFiles()) {
			if (f.isFile()) {
				visitor.handleFile(f);
			}
			else if (f.isDirectory()) {
				visitor.handleDir(f);
				traverseDirectory(f, visitor);
			}
			else {
				System.out.println("WARNING:"+f+" is neither file nor directory? This shouldn't happen....SKIPPING");
			}
		}
	}

	/**
	 * Check if a publickey is in the certstore
	 * @param publicKey byte[] : needs to be a 32byte-string representing the publickey
	 * @return
	 */
	public boolean containsPublicKey(byte[] publicKey) {
		if (publicKey.length!=32) {
			throw new RuntimeException("publickey needs to have a size of 32bytes. got only "+publicKey.length);
		}
		String z85Key=ZMQ.Curve.z85Encode(publicKey);
		return containsPublicKey(z85Key);
	}
	
	/**
	 * check if a z85-based publickey is in the certstore. 
	 * 
	 * if you have no checkthread running this method will scan the cert-folder for changes on every call
	 * 
	 * @param publicKey
	 * @return
	 */
	public boolean containsPublicKey(String publicKey) {
		if (publicKey.length()!=40) {
			throw new RuntimeException("z85 publickeys should have a length of 40bytes but got "+publicKey.length());
		}
		
		// if the checkthread is not running we scan the folders on each publickey-check
		if (!isCheckThreadRunning()){
			checkAndReload();
		} 
		
		return publicKeys.contains(publicKey);
	}
	
	private void loadFiles(String directory) {
		publicKeys.clear();
		certCount=0;
		location = directory;
		File f = new File(directory);
		if (!f.exists()) {
			// create folder if not existant
			f.mkdirs();
		}
		if ( f.isDirectory()) {
			modified = f.lastModified();
		}
		
		fileSize=f.list().length;
		
		traverseDirectory(directory, new IFileVisitor() {
			@Override
			public void handleFile(File f) {
				try {
					ZConfig zconf = ZConfig.load(f.getAbsolutePath());
					String publicKey = zconf.getValue("curve/public-key");
					if (publicKey==null) {
						System.out.println("Warning!! File has no curve/public-key-element: "+f.getAbsolutePath()+" SKIPPING!");
						return;
					}
					if (publicKey.length()==32) { // we want to store the public-key as Z85-String
						publicKey=ZMQ.Curve.z85Encode(publicKey.getBytes());
					}
					publicKeys.add(publicKey);
					certCount++;
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void handleDir(File f) {
				fileSize += f.list().length;
			}
		});
	}
	
	/**
	 * How many certificates are registered at the moment
	 * @return int
	 */
	public int getAmountCertificates() {
		if (!isCheckThreadRunning()) {
			checkAndReload();
		}
		return publicKeys.size();
	}
	
	private boolean directoryModified(String path) {
		File f = new File(path);
		if (f.exists() && f.isDirectory()) {
			if (f.lastModified() != modified) {
				return true;
			};
		}
		return false;
	}
	
	/**
	 * Check if files in the cert-folder have been added or removed. Changes are also recognized in subfolder
	 * This might differ from OS to OS. At least on Linux this works. Timestamps are second-based!! 
	 * 
	 * 
	 * @return boolean
	 */
	public boolean checkCertFolderForChanges() {
		if (directoryModified(location)) {
			return true;
		}

		File f = new File(this.location);
		if (!f.exists()) {
			return true; // run load-files if the main-folder is not present,yet
		}
		// initalize with fileCount of current-directory and add subdirs via traversal
		tempCounter = f.list().length;
		traverseDirectory(f, new IFileVisitor() {
			
			@Override
			public void handleFile(File f) {
				
			}
			
			@Override
			public void handleDir(File f) {
				tempCounter += f.list().length;
			}
		});
		
		if (tempCounter != fileSize) {
			return true;
		}
		
		return false;
	}

	/**
	 * Check if certificates in the cert-folder changed and reload them
	 */
	public void checkAndReload() {
		checkAndReload(false);
	}
	
	/**
	 * Check if certificates in the cert-folder changed and reload them
	 */
	public void checkAndReload(boolean force) {
		if (force || checkCertFolderForChanges()) {
			loadFiles(location);
		}
	}
	
	/**
	 * Start simple thread that checks if the the directory timestamp changed or the overall count of files did change. If yes it will reload all certificates again
	 * 
	 * @param checkTime - the time in milliseconds the check should run
	 */
	public void startCheckThread(final int checkTime) {
		if (fileCheckRunning){
			return;
		}
		fileCheckRunning = true;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(fileCheckRunning && !requestStopThread) {
					checkAndReload();
					try {
						Thread.sleep(checkTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				fileCheckRunning = false;
				requestStopThread = false;
			}
		}).start();
	}
	
	/**
	 * stop the dummy check after the next sleep-period is over
	 */
	public void stopCheckThread() {
		requestStopThread = true;
	}
	
	/**
	 * Check if the Thread is running that periodically scans the cert-folder for new data
	 * @return
	 */
	public boolean isCheckThreadRunning() {
		return fileCheckRunning;
	}

}
