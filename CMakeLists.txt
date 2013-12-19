# CMake build script for ØMQ Java bindings on Windows

cmake_minimum_required (VERSION 2.8)

project (JZMQ)
find_package (Java REQUIRED)
find_package (JNI REQUIRED)

find_program (JNI_JAVAH
  NAMES javah
	HINTS ${_JAVA_HINTS}
	PATHS ${_JAVA_PATHS}
)

#-----------------------------------------------------------------------------
# force off-tree build


if(${CMAKE_SOURCE_DIR} STREQUAL ${CMAKE_BINARY_DIR})
message(FATAL_ERROR "CMake generation is not allowed within the source directory!
Remove the CMakeCache.txt file and try again from another folder, e.g.:

   del CMakeCache.txt
   mkdir build
   cd build
   cmake ..
")
endif(${CMAKE_SOURCE_DIR} STREQUAL ${CMAKE_BINARY_DIR})

#-----------------------------------------------------------------------------
# default to Release build

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING
      "Choose the type of build, options are: None Debug Release RelWithDebInfo MinSizeRel."
      FORCE)
endif(NOT CMAKE_BUILD_TYPE)

set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin)
set(LIBRARY_OUTPUT_PATH  ${CMAKE_BINARY_DIR}/lib)

#-----------------------------------------------------------------------------
# platform specifics

add_definitions(
        -DWIN32
	-DDLL_EXPORT
	-DFD_SETSIZE=1024
)

#-----------------------------------------------------------------------------
# source files

set(java-sources
	org/zeromq/ZMQ.java
	org/zeromq/ZMQException.java
	org/zeromq/ZMQForwarder.java
	org/zeromq/ZMQQueue.java
	org/zeromq/ZMQStreamer.java
	org/zeromq/EmbeddedLibraryTools.java
	org/zeromq/App.java
	org/zeromq/ZContext.java
	org/zeromq/ZDispatcher.java
	org/zeromq/ZFrame.java
	org/zeromq/ZMsg.java
	org/zeromq/ZLoop.java
	org/zeromq/ZThread.java
)
set(java-classes
	org/zeromq/ZMQ.class
	org/zeromq/ZMQ$$Context.class
	org/zeromq/ZMQ$$Socket.class
	org/zeromq/ZMQ$$PollItem.class
	org/zeromq/ZMQ$$Poller.class
	org/zeromq/ZMQ$$Error.class				
	org/zeromq/ZMQException.class
	org/zeromq/ZMQQueue.class
	org/zeromq/ZMQForwarder.class
	org/zeromq/ZMQStreamer.class
	org/zeromq/EmbeddedLibraryTools.class
	org/zeromq/App.class
	org/zeromq/ZContext.class
	org/zeromq/ZDispatcher.class
	org/zeromq/ZDispatcher$$1.class
	org/zeromq/ZDispatcher$$SocketDispatcher$$1.class
	org/zeromq/ZDispatcher$$SocketDispatcher$$2.class
	org/zeromq/ZDispatcher$$SocketDispatcher$$ZMessageBuffer.class
	org/zeromq/ZDispatcher$$SocketDispatcher.class
	org/zeromq/ZDispatcher$$ZMessageHandler.class
	org/zeromq/ZDispatcher$$ZSender.class
	org/zeromq/ZFrame.class
	org/zeromq/ZMsg.class
	org/zeromq/ZLoop.class
	org/zeromq/ZLoop$$IZLoopHandler.class
	org/zeromq/ZLoop$$SPoller.class
	org/zeromq/ZLoop$$STimer.class
	org/zeromq/ZThread.class
	org/zeromq/ZThread$$IAttachedRunnable.class
	org/zeromq/ZThread$$IDetachedRunnable.class
	org/zeromq/ZThread$$ShimThread.class
)
set(javah-headers
	org_zeromq_ZMQ.h
	org_zeromq_ZMQ_Error.h
	org_zeromq_ZMQ_Context.h
	org_zeromq_ZMQ_Socket.h
	org_zeromq_ZMQ_PollItem.h
	org_zeromq_ZMQ_Poller.h	
)
set(cxx-sources
	Context.cpp
	Poller.cpp
	Socket.cpp
	util.cpp
	ZMQ.cpp
)

include_directories(
        src
        src/main/java
	${CMAKE_CURRENT_BINARY_DIR}
)

#-----------------------------------------------------------------------------
# optional modules

add_definitions(
	-DZMQ_HAVE_OPENPGM
)
include_directories(
#	/zeromq/zeromq-2.1.10/include
#	c:/zeromq/zeromq-3.3.0/include
	c:/zeromq/zeromq-3.2.2/include
	${JNI_INCLUDE_DIRS}
)
link_directories(
#	/zeromq/zeromq-2.1.10/build/lib
#	/zeromq/zeromq-2.1.10/build64/lib
#	c:/zeromq/zeromq-3.3.0/lib
	c:/zeromq/zeromq-3.2.2/lib
)

#-----------------------------------------------------------------------------
# source generators

foreach (source ${cxx-sources})
	list(APPEND sources ${CMAKE_CURRENT_SOURCE_DIR}/src/main/c++/${source})	
endforeach()

add_custom_command(
	OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/config.hpp
	COMMAND ${CMAKE_COMMAND}
	ARGS	-E
		copy
		${CMAKE_CURRENT_SOURCE_DIR}/builds/msvc/config.hpp
		${CMAKE_CURRENT_BINARY_DIR}/config.hpp
	DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/builds/msvc/config.hpp
)
list(APPEND sources ${CMAKE_CURRENT_BINARY_DIR}/config.hpp)

add_custom_command(
	OUTPUT ${javah-headers}
	COMMAND ${JNI_JAVAH}
	ARGS	-jni
		-classpath ${CMAKE_CURRENT_BINARY_DIR}
		org.zeromq.ZMQ
	WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
	DEPENDS ${java-classes}
)
list(APPEND sources ${javah-headers})

set (source-tmp "")
foreach (source ${java-sources})
	list (APPEND source-tmp ${CMAKE_CURRENT_SOURCE_DIR}/src/main/java/${source})
endforeach()
set (java-sources ${source-tmp})

add_custom_command(
	OUTPUT ${java-classes}
	COMMAND ${JAVA_COMPILE}
	ARGS	-classpath ${CMAKE_CURRENT_BINARY_DIR}
		-sourcepath ${CMAKE_CURRENT_SOURCE_DIR}/src/main/java
		-d ${CMAKE_CURRENT_BINARY_DIR}
		${java-sources}
	DEPENDS ${java-sources}
)

add_custom_command(
	OUTPUT lib/zmq.jar
	COMMAND ${JAVA_ARCHIVE}
	ARGS	cf
		lib/zmq.jar
		${java-classes}
	WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
	DEPENDS ${java-classes}
)
list(APPEND sources lib/zmq.jar)

#-----------------------------------------------------------------------------
# output

add_library(jzmq SHARED ${sources})
target_link_libraries(jzmq libzmq.lib)

set(docs
        AUTHORS
	COPYING
	COPYING.LESSER
	ChangeLog
	INSTALL
	NEWS
	README
	README-PERF
)

install (TARGETS jzmq DESTINATION lib)
install (FILES ${CMAKE_CURRENT_BINARY_DIR}/lib/zmq.jar DESTINATION lib)
install (FILES ${docs} DESTINATION doc)

# By default, do not warn when built on machines using only VS Express:
IF(NOT DEFINED CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS_NO_WARNINGS)
	SET(CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS_NO_WARNINGS ON)
ENDIF()

include (InstallRequiredSystemLibraries)
set (CPACK_PACKAGE_VENDOR "Miru Limited")
set (CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/COPYING")
#set (CPACK_PACKAGE_VERSION_MAJOR "2")
#set (CPACK_PACKAGE_VERSION_MINOR "1")
#set (CPACK_PACKAGE_VERSION_PATCH "10")
set (CPACK_PACKAGE_VERSION_MAJOR "3")
set (CPACK_PACKAGE_VERSION_MINOR "2")
set (CPACK_PACKAGE_VERSION_PATCH "2")
include (CPack)

# end of file
