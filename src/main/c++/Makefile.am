jarfile = zmq.jar
jardir = $(datadir)/java

JZMQ_JAVA_FILES = \
	../java/org/zeromq/ZMQ.java \
	../java/org/zeromq/ZMQException.java \
	../java/org/zeromq/ZMQQueue.java \
	../java/org/zeromq/ZMQForwarder.java \
	../java/org/zeromq/ZMQStreamer.java \
	../java/org/zeromq/ZContext.java \
	../java/org/zeromq/ZFrame.java \
	../java/org/zeromq/ZMsg.java \
	../java/org/zeromq/ZThread.java \
	../java/org/zeromq/ZLoop.java \
	../java/org/zeromq/ZDispatcher.java \
	../java/org/zeromq/EmbeddedLibraryTools.java

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
	../java/org/zeromq/ZMQ.class \
	../java/org/zeromq/ZMQ$$Context.class \
	../java/org/zeromq/ZMQ$$Socket.class \
	../java/org/zeromq/ZMQ$$Poller.class \
	../java/org/zeromq/ZMQ$$Error.class \
	../java/org/zeromq/ZMQException.class \
	../java/org/zeromq/ZMQQueue.class \
	../java/org/zeromq/ZMQForwarder.class \
	../java/org/zeromq/ZMQStreamer.class \
	../java/org/zeromq/ZContext.class \
	../java/org/zeromq/ZFrame.class \
	../java/org/zeromq/ZMsg.class \
	../java/org/zeromq/ZThread.class \
	../java/org/zeromq/ZLoop.class \
	../java/org/zeromq/ZDispatcher.class

$(jarfile):
	$(JAVAC) -d ../java/ $(JZMQ_JAVA_FILES)
	$(JAR) cf $(JARFLAGS) $@ ../java/org/zeromq/*.class

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
	$(CLASSPATH_ENV) $(JAVAH) -jni -classpath ../java org.zeromq.ZMQ

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
