package org.zeromq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Simple App to display version information about jzmq.
 * 
 */
public class App {

	public static void main(final String[] args) throws Exception {

		final Package p = App.class.getPackage();
		final String appname = p.getSpecificationTitle();
		final String versionMaven = p.getSpecificationVersion();
		String[] version = new String[] {"", ""};
		if (p.getImplementationVersion() != null) {
			version = p.getImplementationVersion().split(" ", 2);
		}

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
		
		final String fmt = "%-7.7s %-15.15s %s%n";
		
		System.out.printf(fmt, "ZeroMQ", "version:", zmqVersion);
		System.out.printf(fmt, appname, "version:", versionMaven);
		System.out.printf(fmt, appname, "build time:", version[1]);
		System.out.printf(fmt, appname, "build commit:", version[0]);
		System.out.printf(fmt, "JNI lib", "location:", libzmqLocation);
		System.out.printf(fmt, "current", "platform:", 
				System.getProperty("os.arch") + "/" + System.getProperty("os.name"));

		listEmbeddedBinaries();
		
	}
	
	private static void listEmbeddedBinaries() {
		
		System.out.println("--- list of embedded JNI libraries ---");
		
		final Collection<String> files = catalogClasspath();
		
		for (final String file : files) {
			if (file.startsWith("NATIVE")) {
				System.out.println(file);
			}
		}
		
	}
	
	private static Collection<String> catalogClasspath() {
		
		final List<String> files = new ArrayList<String>();		
		final String[] classpath = System.getProperty("java.class.path", "").split(File.pathSeparator);
		
		for (final String path : classpath) {
			final File tmp = new File(path);
			if (tmp.isFile() && path.toLowerCase().endsWith(".jar")) {
				catalogArchive(tmp, files);
			} else if (tmp.isDirectory()) {
				final int len = tmp.getPath().length() +1;
				catalogFiles(len, tmp, files);
			}
		}
		
		return files;
		
	}
	
	private static void catalogArchive(final File jarfile, final Collection<String> files) {
		
		try {
		
			final JarFile j = new JarFile(jarfile);
			final Enumeration<JarEntry> e = j.entries();
			while (e.hasMoreElements()) {
				final JarEntry entry = e.nextElement();
				if (!entry.isDirectory()) {
					files.add(entry.getName());
				}
			}
	
		} catch (IOException x) {
			System.err.println(x.toString());
		}
		
	}
	
	private static void catalogFiles(final int prefixlen, final File root, final Collection<String> files) {
		final File[] ff = root.listFiles();
		for (final File f : ff) {
			if (f.isDirectory()) {
				catalogFiles(prefixlen, f, files);
			} else {
				files.add(f.getPath().substring(prefixlen));
			}
		}
	}

}
