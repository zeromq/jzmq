What is JZMQ?
-------------

This is the Java language binding for libzmq (aka ZeroMQ, 0MQ).

[![Build Status](https://travis-ci.org/zeromq/jzmq.png?branch=master)](https://travis-ci.org/zeromq/jzmq)

The latest [javadocs](http://zeromq.github.com/jzmq/javadocs/).

Building and Installing JZMQ
----------------------------

To build you need to have the libzmq library already installed, then you run:

```bash
cd jzmq-jni/
./autogen.sh
./configure
make
make install
cd ..
mvn package
```

If you hope to install to your local maven, then you should run:

```
mvn install -Dgpg.skip=true
```

Building Windows 64bit with CMake & NMake
-----------------------------------------

It is recommended to follow these steps with the *Visual C++ 2015 Build Tools*.

1. create a new and empty directory:
```
d:\temp\>mkdir JZMQ
d:\temp\>cd JZMQ
d:\temp\JZMQ\>
```
2. Clone the repository to your new directory
```
d:\temp\JZMQ\>git clone https://github.com/zeromq/jzmq.git
```
3. dive into the checkedout repository and create a new build64 folder
```
d:\temp\JZMQ\>cd jzmq\jzmq-jni
d:\temp\JZMQ\jzmq\jzmq-jni\>mkdir build64
d:\temp\JZMQ\jzmq\jzmq-jni\>cd build64
```
4. Now call CMake to generate the project
```
D:\temp\JZMQ\jzmq\jzmq-jni\build64>cmake .. -G "NMake Makefiles" -DZMQ_C_INCLUDE_PATH=<path to zmq include> -DZMQ_C_LIB_PATH=<path to zmq library>
-- The C compiler identification is MSVC 19.0.24210.0
-- The CXX compiler identification is MSVC 19.0.24210.0
-- Check for working C compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe
-- Check for working C compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe -- works
-- Detecting C compiler ABI info
-- Detecting C compiler ABI info - done
-- Check for working CXX compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe
-- Check for working CXX compiler: C:/Program Files (x86)/Microsoft Visual Studio 14.0/VC/bin/amd64/cl.exe -- works
-- Detecting CXX compiler ABI info
-- Detecting CXX compiler ABI info - done
-- Detecting CXX compile features
-- Detecting CXX compile features - done
-- Found Java: C:/Program Files/Java/jdk1.8.0_91/bin/java.exe (found version "1.8.0.91")
-- Found JNI: C:/Program Files/Java/jdk1.8.0_91/lib/jawt.lib
-- Configuring done
-- Generating done
-- Build files have been written to: D:/temp/JZMQ/jzmq/jzmq-jni/build64
```
5. Now finally call NMake to build the zmq.jar and jzmq.lib, jzmq.dll
```
D:\temp\JZMQ\jzmq\jzmq-jni\build64>nmake
Microsoft (R) Program Maintenance Utility Version 14.00.24210.0
Copyright (C) Microsoft Corporation.  All rights reserved.

[ 10%] Generating config.hpp
[ 20%] Generating org/zeromq/ZMQ.class, org/zeromq/Utils.class, org/zeromq/ZMQ$$Context.class, org/zeromq/ZMQ$$Socket.class, org/zeromq/ZMQ$$PollItem.class, org/zeromq/ZMQ$$Poller.class, org/zeromq/ZM
Q$$Error.class, org/zeromq/ZMQException.class, org/zeromq/ZMQQueue.class, org/zeromq/ZMQForwarder.class, org/zeromq/ZMQStreamer.class, org/zeromq/EmbeddedLibraryTools.class, org/zeromq/App.class, org/
zeromq/ZContext.class, org/zeromq/ZDispatcher.class, org/zeromq/ZDispatcher$$1.class, org/zeromq/ZDispatcher$$SocketDispatcher$$1.class, org/zeromq/ZDispatcher$$SocketDispatcher$$2.class, org/zeromq/Z
Dispatcher$$SocketDispatcher$$ZMessageBuffer.class, org/zeromq/ZDispatcher$$SocketDispatcher.class, org/zeromq/ZDispatcher$$ZMessageHandler.class, org/zeromq/ZDispatcher$$ZSender.class, org/zeromq/ZFr
ame.class, org/zeromq/ZMsg.class, org/zeromq/ZLoop.class, org/zeromq/ZLoop$$IZLoopHandler.class, org/zeromq/ZLoop$$SPoller.class, org/zeromq/ZLoop$$STimer.class, org/zeromq/ZThread.class, org/zeromq/Z
Thread$$IAttachedRunnable.class, org/zeromq/ZThread$$IDetachedRunnable.class, org/zeromq/ZThread$$ShimThread.class
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
[ 30%] Generating org_zeromq_ZMQ.h, org_zeromq_ZMQ_Error.h, org_zeromq_ZMQ_Context.h, org_zeromq_ZMQ_Socket.h, org_zeromq_ZMQ_PollItem.h, org_zeromq_ZMQ_Poller.h
[ 40%] Generating lib/zmq.jar
Scanning dependencies of target jzmq
[ 40%] Generating org/zeromq/ZMQ.class, org/zeromq/Utils.class, org/zeromq/ZMQ$$Context.class, org/zeromq/ZMQ$$Socket.class, org/zeromq/ZMQ$$PollItem.class, org/zeromq/ZMQ$$Poller.class, org/zeromq/ZM
Q$$Error.class, org/zeromq/ZMQException.class, org/zeromq/ZMQQueue.class, org/zeromq/ZMQForwarder.class, org/zeromq/ZMQStreamer.class, org/zeromq/EmbeddedLibraryTools.class, org/zeromq/App.class, org/
zeromq/ZContext.class, org/zeromq/ZDispatcher.class, org/zeromq/ZDispatcher$$1.class, org/zeromq/ZDispatcher$$SocketDispatcher$$1.class, org/zeromq/ZDispatcher$$SocketDispatcher$$2.class, org/zeromq/Z
Dispatcher$$SocketDispatcher$$ZMessageBuffer.class, org/zeromq/ZDispatcher$$SocketDispatcher.class, org/zeromq/ZDispatcher$$ZMessageHandler.class, org/zeromq/ZDispatcher$$ZSender.class, org/zeromq/ZFr
ame.class, org/zeromq/ZMsg.class, org/zeromq/ZLoop.class, org/zeromq/ZLoop$$IZLoopHandler.class, org/zeromq/ZLoop$$SPoller.class, org/zeromq/ZLoop$$STimer.class, org/zeromq/ZThread.class, org/zeromq/Z
Thread$$IAttachedRunnable.class, org/zeromq/ZThread$$IDetachedRunnable.class, org/zeromq/ZThread$$ShimThread.class
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
[ 40%] Generating org_zeromq_ZMQ.h, org_zeromq_ZMQ_Error.h, org_zeromq_ZMQ_Context.h, org_zeromq_ZMQ_Socket.h, org_zeromq_ZMQ_PollItem.h, org_zeromq_ZMQ_Poller.h
[ 50%] Building CXX object CMakeFiles/jzmq.dir/src/main/c++/Context.cpp.obj
Context.cpp
[ 60%] Building CXX object CMakeFiles/jzmq.dir/src/main/c++/Poller.cpp.obj
Poller.cpp
D:\temp\JZMQ\jzmq\jzmq-jni\src\main\c++\Poller.cpp(76): warning C4244: '=': conversion from 'jint' to 'short', possible loss of data
[ 70%] Building CXX object CMakeFiles/jzmq.dir/src/main/c++/Socket.cpp.obj
Socket.cpp
D:\temp\JZMQ\jzmq\jzmq-jni\src\main\c++\Socket.cpp(266): warning C4267: 'argument': conversion from 'size_t' to 'jsize', possible loss of data
D:\temp\JZMQ\jzmq\jzmq-jni\src\main\c++\Socket.cpp(272): warning C4267: 'argument': conversion from 'size_t' to 'jsize', possible loss of data
D:\temp\JZMQ\jzmq\jzmq-jni\src\main\c++\Socket.cpp(847): warning C4267: 'initializing': conversion from 'size_t' to 'int', possible loss of data
D:\temp\JZMQ\jzmq\jzmq-jni\src\main\c++\Socket.cpp(875): warning C4267: 'initializing': conversion from 'size_t' to 'int', possible loss of data
[ 80%] Building CXX object CMakeFiles/jzmq.dir/src/main/c++/util.cpp.obj
util.cpp
[ 90%] Building CXX object CMakeFiles/jzmq.dir/src/main/c++/ZMQ.cpp.obj
ZMQ.cpp
[100%] Linking CXX shared library lib\jzmq.dll
   Creating library lib\jzmq.lib and object lib\jzmq.exp
   Creating library lib\jzmq.lib and object lib\jzmq.exp
[100%] Built target jzmq
```

Avoiding JNI
------------

JZMQ uses JNI to wrap libzmq for the best performance. If performance isn't your primary goal, look at the [JeroMQ](https://github.com/zeromq/jeromq) project, which is a pure Java implementation that provides an identical API to JZMQ, and uses the same protocol.

Building Packages
-----------------

To build a Debian package, run:

```bash
$ dpkg-buildpackage -rfakeroot
```

To build an RPM package, run:

```bash
$ rpmbuild -tb jzmq-X.Y.Z.tar.gz
```

Where X.Y.Z is replaced with the version that you've downloaded.

If configure can't find your libzmq installation, you can tell it where to look, using e.g. `--with-zeromq=/usr/local`.

You may want to take a look at http://www.zeromq.org/docs:tuning-zeromq for additional hints.

For more information, refer to the ØMQ website at http://www.zeromq.org/.

On Mac OS X you may need to compile and make install pkg-config if configure fails with "syntax error near unexpected token newline".   
See http://stackoverflow.com/questions/3522248/how-do-i-compile-jzmq-for-zeromq-on-osx for details.   

You may also need to symlink the header files of your standard Java installation (e.g. `/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Headers/*.h`) into a suitable directory (e.g. `/usr/local/include`) and point the `JAVA_HOME` environment variable to the parent directory (e.g.`/usr/local`).

## Acknowledgements

YourKit is kindly supporting ZeroMQ project with its full-featured [Java Profiler](http://www.yourkit.com/java/profiler/index.jsp).

Copying
-------

Free use of this software is granted under the terms of the GNU Lesser General
Public License (LGPL). For details see the files `COPYING` and `COPYING.LESSER`
included with the Java binding for ØMQ.
