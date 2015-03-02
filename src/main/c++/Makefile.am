jarfile = zmq.jar
jardir = $(datadir)/java

javac_stamp = javac_stamp
javah_stamp = javah_stamp

JZMQ_JAVA_FILES = \
	../java/org/zeromq/EmbeddedLibraryTools.java \
	../java/org/zeromq/ZAuth.java \
	../java/org/zeromq/ZContext.java \
	../java/org/zeromq/ZDispatcher.java \
	../java/org/zeromq/ZFrame.java \
	../java/org/zeromq/ZLoop.java \
	../java/org/zeromq/ZMQ.java \
	../java/org/zeromq/ZMQException.java \
	../java/org/zeromq/ZMQForwarder.java \
	../java/org/zeromq/ZMQQueue.java \
	../java/org/zeromq/ZMQStreamer.java \
	../java/org/zeromq/ZMsg.java \
	../java/org/zeromq/ZThread.java \
	../java/org/zeromq/ZLoop.java \
	../java/org/zeromq/ZCurveKeyPair.java \
	../java/org/zeromq/ZDispatcher.java \
	../java/org/zeromq/Utils.java

JZMQ_CPP_FILES = \
	ZMQ.cpp \
	Context.cpp \
	Socket.cpp \
	Poller.cpp \
	Event.cpp \
	util.cpp

JZMQ_H_FILES = \
	org_zeromq_ZMQ.h \
	org_zeromq_ZMQ_Context.h \
	org_zeromq_ZMQ_Error.h \
	org_zeromq_ZMQ_Event.h \
	org_zeromq_ZMQ_PollItem.h \
	org_zeromq_ZMQ_Poller.h \
	org_zeromq_ZMQ_ZCurveKeyPair.h \
	org_zeromq_ZMQ_Socket.h

JZMQ_HPP_FILES = \
	util.hpp

JZMQ_CLASS_FILES = \
	org/zeromq/EmbeddedLibraryTools.class \
	org/zeromq/ZAuth$$1.class \
	org/zeromq/ZAuth$$ZAPRequest.class \
	org/zeromq/ZAuth$$ZAuthAgent.class \
	org/zeromq/Zauth.class \
	org/zeromq/ZContext.class \
	org/zeromq/ZCurveKeyPair.class \
	org/zeromq/ZDispatcher.class \
	org/zeromq/ZDispatcher$$1.class \
	org/zeromq/ZDispatcher$$SocketDispatcher$$1.class \
	org/zeromq/ZDispatcher$$SocketDispatcher$$2.class \
	org/zeromq/ZDispatcher$$SocketDispatcher$$ZMessageBuffer.class \
	org/zeromq/ZDispatcher$$SocketDispatcher.class \
	org/zeromq/ZDispatcher$$ZMessageHandler.class \
	org/zeromq/ZDispatcher$$ZSender.class \
	org/zeromq/ZDispatcher.class \
	org/zeromq/ZFrame.class \
	org/zeromq/ZLoop$$IZLoopHandler.class \
	org/zeromq/ZLoop$$SPoller.class \
	org/zeromq/ZLoop$$STimer.class \
	org/zeromq/ZLoop.class \
	org/zeromq/ZMQ$$Context.class \
	org/zeromq/ZMQ$$Error.class \
	org/zeromq/ZMQ$$Event.class \
	org/zeromq/ZMQ$$PollItem.class \
	org/zeromq/ZMQ$$Poller.class \
	org/zeromq/ZMQ$$Socket.class \
	org/zeromq/.class \
	org/zeromq/.class \
	org/zeromq/.class \
	org/zeromq/.class \
	org/zeromq/.class \
	org/zeromq/.class \
	org/zeromq/ZMQException.class \
	org/zeromq/ZMQForwarder.class \
	org/zeromq/ZMQQueue.class \
	org/zeromq/ZMQStreamer.class \
	org/zeromq/ZMsg.class \
	org/zeromq/ZThread$$IAttachedRunnable.class \
	org/zeromq/ZThread$$IDetachedRunnable.class \
	org/zeromq/ZThread$$ShimThread.class \
	org/zeromq/ZThread.class

$(javac_stamp): $(JZMQ_JAVA_FILES)
	$(JAVAC) $(JAVACFLAGS) -d . $(JZMQ_JAVA_FILES)
	@touch $@

$(jarfile): $(javac_stamp)
	$(JAR) cf $(JARFLAGS) $@ $(JZMQ_CLASS_FILES)

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
	$(JZMQ_H_FILES)

CLEANFILES = \
	$(JZMQ_H_FILES) \
	$(jarfile) \
	$(javac_stamp) \
	$(javah_stamp)

clean-local:
	rm -rf org

$(JZMQ_H_FILES): $(javah_stamp)
$(javah_stamp): $(javac_stamp)
	## This is the classpath in master
	## $(CLASSPATH_ENV) $(JAVAH) -jni -classpath . org.zeromq.ZMQ
	$(CLASSPATH_ENV) $(JAVAH) -jni -classpath ../java org.zeromq.ZMQ
	$(CLASSPATH_ENV) $(JAVAH) -jni -classpath ../java org.zeromq.ZCurveKeyPair
	@touch $@

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
