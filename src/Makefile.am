jarfile = zmq.jar
jardir = $(datadir)/java

JZMQ_JAVA_FILES = \
	main/java/org/zeromq/ZMQ.java \
	main/java/org/zeromq/ZMQException.java \
	main/java/org/zeromq/ZMQQueue.java \
	main/java/org/zeromq/ZMQForwarder.java \
	main/java/org/zeromq/ZMQStreamer.java \
	main/java/org/zeromq/ZContext.java \
	main/java/org/zeromq/ZFrame.java \
	main/java/org/zeromq/ZMsg.java \
	main/java/org/zeromq/ZThread.java \
	main/java/org/zeromq/ZLoop.java \
	main/java/org/zeromq/ZDispatcher.java \
	main/java/org/zeromq/EmbeddedLibraryTools.java
	main/java/org/zeromq/ZAuth.java

JZMQ_CPP_FILES = \
	ZMQ.cpp \
	Context.cpp \
	Socket.cpp \
	Poller.cpp \
	util.cpp

JZMQ_H_FILES = \
	org_zeromq_ZMQ.h \
	org_zeromq_ZMQ_Error.h \
	org_zeromq_ZMQ_Context.h \
	org_zeromq_ZMQ_Socket.h \
	org_zeromq_ZMQ_Poller.h

JZMQ_HPP_FILES = \
	util.hpp

JZMQ_CLASS_FILES = \
	main/java/org/zeromq/ZMQ.class \
	main/java/org/zeromq/ZMQ$$Context.class \
	main/java/org/zeromq/ZMQ$$Socket.class \
	main/java/org/zeromq/ZMQ$$Poller.class \
	main/java/org/zeromq/ZMQ$$Error.class \
	main/java/org/zeromq/ZMQException.class \
	main/java/org/zeromq/ZMQQueue.class \
	main/java/org/zeromq/ZMQForwarder.class \
	main/java/org/zeromq/ZMQStreamer.class \
	main/java/org/zeromq/ZContext.class \
	main/java/org/zeromq/ZFrame.class \
	main/java/org/zeromq/ZMsg.class \
	main/java/org/zeromq/ZThread.class \
	main/java/org/zeromq/ZLoop.class \
	main/java/org/zeromq/ZAuth.class \
	main/java/org/zeromq/ZDispatcher.class 

JAVAH_FLAGS = -d . -classpath .

$(jarfile):
	$(JAVAC) -d ./main/java/ $(JZMQ_JAVA_FILES)
	$(JAR) cf $(JARFLAGS) $@ main/java/org/zeromq/*.class

jar_DATA = $(jarfile)

dist_noinst_JAVA = $(JZMQ_JAVA_FILES)

lib_LTLIBRARIES = libjzmq.la
libjzmq_la_SOURCES = $(JZMQ_CPP_FILES)

nodist_libjzmq_la_SOURCES = \
	$(JZMQ_H_FILES) \
	$(JZMQ_HPP_FILES)

libjzmq_la_CXXFLAGS = -Wall

if DISABLE_VERSION
libjzmq_la_LDFLAGS = -avoid-version
else
libjzmq_la_LDFLAGS = -version-info @JLTVER@
endif

BUILT_SOURCES = \
	$(JZMQ_H_FILES) \
	$(JZMQ_CLASS_FILES)

CLEANFILES = \
	$(JZMQ_H_FILES) \
	$(JZMQ_CLASS_FILES) \
	$(jarfile)


$(JZMQ_H_FILES): $(jarfile)
	$(CLASSPATH_ENV) $(JAVAH) -jni -classpath ./main/java org.zeromq.ZMQ

$(srcdir)/ZMQ.cpp: \
	$(JZMQ_H_FILES) \
	$(JZMQ_HPP_FILES)

$(srcdir)/Context.cpp: \
	org_zeromq_ZMQ_Context.h \
	$(JZMQ_HPP_FILES)

$(srcdir)/Socket.cpp: \
	org_zeromq_ZMQ_Socket.h \
	$(JZMQ_HPP_FILES)

$(srcdir)/Poller.cpp: \
	org_zeromq_ZMQ_Poller.h \
	$(JZMQ_HPP_FILES)

$(srcdir)/util.cpp: \
	$(JZMQ_HPP_FILES)

dist-hook:
	-rm $(distdir)/*.h
