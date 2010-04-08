jarfile = zmq.jar
jardir = $(datadir)/java

$(jarfile): $(dist_noinst_JAVA)
	$(JAR) cf $(JARFLAGS) $@ org/zeromq/*.class

jar_DATA = $(jarfile)

dist_noinst_JAVA = \
	org/zeromq/ZMQ.java

lib_LTLIBRARIES = libjzmq.la
libjzmq_la_SOURCES = \
	ZMQ.cpp \
	Context.cpp \
	Socket.cpp \
	Poller.cpp \
	util.cpp

nodist_libjzmq_la_SOURCES = \
	org_zeromq_ZMQ.h \
	org_zeromq_Context.h \
	org_zeromq_Socket.h \
	org_zeromq_Poller.h

libjzmq_la_CXXFLAGS = -Wall
libjzmq_la_LDFLAGS = -version-info @JLTVER@

BUILT_SOURCES = \
	org/zeromq/ZMQ.class \
	org/zeromq/ZMQ$$Context.class \
	org_zeromq_Context.h \
	org/zeromq/ZMQ$$Socket.class \
	org_zeromq_Socket.h \
	org/zeromq/ZMQ$$Poller.class \
	org_zeromq_Poller.h

CLEANFILES = \
	org/zeromq/ZMQ.class \
	org/zeromq/ZMQ$$Context.class \
	org_zeromq_Context.h \
	org/zeromq/ZMQ$$Socket.class \
	org_zeromq_Socket.h \
	org/zeromq/ZMQ$$Poller.class \
	org_zeromq_Poller.h \
	zmq.jar

$(srcdir)/ZMQ.cpp: \
	org_zeromq_ZMQ.h \
	org_zeromq_ZMQ_Context.h \
	org_zeromq_ZMQ_Socket.h \
	org_zeromq_ZMQ_Poller.h

org_zeromq_ZMQ.h org_zeromq_ZMQ_Context.h org_zeromq_ZMQ_Socket.h org_zeromq_ZMQ_Poller.h: org/zeromq/ZMQ.class
	$(CLASSPATH_ENV) $(JAVAH) -jni -classpath . org.zeromq.ZMQ

./org/zeromq/ZMQ.class: classdist_noinst.stamp

$(srcdir)/util.cpp: util.hpp

dist-hook:
	-rm $(distdir)/*.h

