package org.zeromq;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EmbeddedLibraryTools {

    public static final boolean LOADED_EMBEDDED_LIBRARY;

    private static final File jniLibDir;

    static {
        try {
            Path jniLibPath = Files.createTempDirectory("jniloader");
            jniLibDir = jniLibPath.toFile();
            jniLibDir.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("failed to set up jniloader directory", e);
        }
        LOADED_EMBEDDED_LIBRARY = loadEmbeddedLibrary();
    }

    public static String getCurrentPlatformIdentifier() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows")) {
            osName = "Windows";
        } else if (osName.toLowerCase().contains("mac os x")) {
            osName = "Darwin";
        } else {
            osName = osName.replaceAll("\\s+", "_");
        }
        return System.getProperty("os.arch") + "/" + osName;
    }

    public static Collection<String> getEmbeddedLibraryList() {
        final Collection<String> result = new ArrayList<String>();
        final Collection<String> files = catalogClasspath();

        for (final String file : files) {
            if (file.startsWith("NATIVE")) {
                result.add(file);
            }
        }

        return result;
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

    private static Collection<String> catalogClasspath() {

        final List<String> files = new ArrayList<String>();
        final String[] classpath = System.getProperty("java.class.path", "").split(File.pathSeparator);

        for (final String path : classpath) {
            final File tmp = new File(path);
            if (tmp.isFile() && path.toLowerCase().endsWith(".jar")) {
                catalogArchive(tmp, files);
            } else if (tmp.isDirectory()) {
                final int len = tmp.getPath().length() + 1;
                catalogFiles(len, tmp, files);
            }
        }

        return files;

    }

    private static void catalogFiles(final int prefixlen, final File root, final Collection<String> files) {
        final File[] ff = root.listFiles();
        if (ff == null) {
            throw new IllegalStateException("invalid path listed: " + root);
        }

        for (final File f : ff) {
            if (f.isDirectory()) {
                catalogFiles(prefixlen, f, files);
            } else {
                files.add(f.getPath().substring(prefixlen));
            }
        }
    }

    private static boolean loadEmbeddedLibrary() {
        URL zmqLibLoc = searchForNativeLibrary("zmq");
        URL jniLibLoc = searchForNativeLibrary("jzmq");

        // attempt to load ZMQ from the JAR as well; static linking does not work on all platforms (e.g. MacOS X).
        // we allow this one to fail because not every case will package it
        tryLoadEmbedded(zmqLibLoc, System.mapLibraryName("zmq"), true);

        // now try to load the JNI bindings, which may depend upon external ZMQ, or the above loaded lib
        return tryLoadEmbedded(jniLibLoc, System.mapLibraryName("jzmq"));
    }

    private static URL searchForNativeLibrary(String baseName) {
        // attempt to locate embedded native library within JAR at following location:
        // /NATIVE/${os.arch}/${os.name}/$baseName.[so|dylib|dll]
        StringBuilder url = new StringBuilder();
        url.append("/NATIVE/");
        url.append(getCurrentPlatformIdentifier());
        url.append("/");
        url.append(System.mapLibraryName(baseName));
        return ZMQ.class.getResource(url.toString());
    }

    private static boolean tryLoadEmbedded(URL nativeLibraryUrl, String targetName) {
        return tryLoadEmbedded(nativeLibraryUrl, targetName, false);
    }

    private static boolean tryLoadEmbedded(URL nativeLibraryUrl, String targetName, boolean preload) {
        if (nativeLibraryUrl == null) {
            return false;
        }

        // native library found within JAR, extract and load
        try {
            final File libfile = new File(jniLibDir, targetName);

            final InputStream in = nativeLibraryUrl.openStream();
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(libfile));

            int len = 0;
            byte[] buffer = new byte[8192];
            while ((len = in.read(buffer)) > -1)
                out.write(buffer, 0, len);
            out.close();
            in.close();

            if (!preload) {
                System.load(libfile.getAbsolutePath());
            }

            return true;
        } catch (IOException x) {
            // failed, force external loading
            return false;
        }
    }

    private EmbeddedLibraryTools() {
    }
}
