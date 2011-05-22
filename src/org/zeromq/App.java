package org.zeromq;

import org.zeromq.ZMQ;

/**
 * Simple App to display version information about jzmq.
 * 
 */
public class App {

	public static void main(final String[] args) throws Exception {

		final Package p = App.class.getPackage();
		final String appname = p.getSpecificationTitle();
		final String versionMaven = p.getSpecificationVersion();
		final String[] version = p.getImplementationVersion().split(" ", 2);

		String zmqVersion = null;
		String libzmqLocation = null;
		
		try {

			final int major = ZMQ.version_major();
			final int minor = ZMQ.version_minor();
			final int patch = ZMQ.version_patch();
			zmqVersion = major + "." + minor + "." + patch;
			libzmqLocation = ZMQ.LIB_LOCATION;
	
		} catch (Throwable x) {
			zmqVersion = "ERROR! " + x.getMessage();
			libzmqLocation = "ERROR! Not found.";
		}
		
		System.out.printf("%s version:    %s%n", "ZeroMQ", zmqVersion);
		System.out.printf("%s version:      %s%n", appname, versionMaven);
		System.out.printf("%s build time:   %s%n", appname, version[1]);
		System.out.printf("%s build commit: %s%n", appname, version[0]);
		System.out.printf("%s location:  %s%n", "JNI lib", libzmqLocation);

	}

}
