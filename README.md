# jzmq: Java language binding for ZeroMQ
*****

## Building

### Requirements
 - the ZeroMQ library
 - c compiler
 - Java 1.6 JDK
 - Maven 2.0 +
 - JAVA_HOME environment variable set to the location of your JDK

Build as follows:

				./autogen.sh
				./configure
				make
				mvn clean install

This will produce a JAR file in the jzmq/target directory named jzmq-VERSION.jar. It is self-contained, holding both the Java classes as well as the native JNI library for the target OS and platform. Additionally, the JAR is executable allowing a simple self-test to be performed that will print out version information to the console. Try it as follows:

				java -jar jzmq-VERSION.jar

## Embedded native library
The jzmq JNI library will be extracted from the JAR to a temporary location and loaded upon running, making it unnecessary to manually refer to its location in the java.library.path system property. After building, the JAR file will contain the native JNI library for the OS and platform on which it was built. It is however possible to build JAR files that contain native libraries for multiple platforms. Native libraries are stored in the JAR file using the following naming convention:

				/NATIVE/{os.arch}/{os.name}/libjzmq.[so|dylib|dll]

## Debian
To build Debian package run:

				$ dpkg-buildpackage -rfakeroot

## Notes
If your zmq installation is not found by configure you can add this information manually e.g. --with-zeromq=/usr/local/lib.
 
On Mac OS X you may need to compile and make install pkg-config if configure fails with "syntax error near unexpected token `newline'". See http://stackoverflow.com/questions/3522248/how-do-i-compile-jzmq-for-zeromq-on-osx for details. You may also need to symlink the header files of your standard Java installation (e.g. /Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Headers/*.h) into a suitable directory (e.g. /usr/local/include) and point the JAVA_HOME environment variable to the parent directory (e.g. /usr/local).

For more information, refer to the 0MQ website at http://www.zeromq.org/.
