jarfile = zmq-perf.jar
jardir = $(datadir)/perf

JZMQ_PERF_JAVA_FILES = \
	local_lat.java \
	local_thr.java \
	remote_lat.java \
	remote_thr.java


JZMQ_PERF_CLASS_FILES = *.class

$(jarfile): 
	$(JAVAC) $(JAVACFLAGS) -classpath $(top_builddir)/src/main/java -d . $(JZMQ_PERF_JAVA_FILES)
	$(JAR) cf $(JARFLAGS) $@ $(JZMQ_PERF_CLASS_FILES)

jar_DATA = $(jarfile)

CLEANFILES = \
	-rf org \
	$(JZMQ_PERF_CLASS_FILES) \
	$(jarfile)
