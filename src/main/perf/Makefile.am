AM_JAVACFLAGS=-classpath $(top_builddir)/src/main/java

jarfile = zmq-perf.jar
jardir = $(datadir)/perf

JZMQ_PERF_JAVA_FILES = \
	local_lat.java \
	local_thr.java \
	remote_lat.java \
	remote_thr.java

$(jarfile):
	$(JAVAC) -d . $(JZMQ_PERF_JAVA_FILES)
	$(JAR) cf $(JARFLAGS) $@ *.class

jar_DATA = $(jarfile)

dist_noinst_JAVA = $(JZMQ_PERF_JAVA_FILES)

CLEANFILES = \
	*.class \
	$(jarfile)
