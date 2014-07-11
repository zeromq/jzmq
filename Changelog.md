# Changelog

## v3.1.0
 * Update scm tags in pom.xml
 * Fix issue where release was failing because a circular reference
 * Added method ZMQ.Socket.setProbeRouter(), issue #333
 * exposed plaintext gss sock opt
 * Change version to 3.1.0-SNAPSHOT
 * Change artifactId back to jzmq
 * bug fix: non blocking recvByteBuffer throws exception when there are no data to receive
 * Fix issue #328 - Expose public set_sockopt for ZMQ_REQ_RELAXED and ZMQ_REQ_CORRELATE
 * Ignore .checkstyle
 * Fix issue #325 - Able to build latest JZMQ with libzmq 2.2.0
 * Use inline instead of static inline since it's behaviour is undefined
 * Update Context.cpp formatting
 * Fix issue #323 - Add get and set max socket options on Context
 * Fixed java.nio.Buffer.position method being called as void.
 * ZMQ_GSSAPI_PRINCIPAL and ZMQ_GSSAPI_SERVICE_PRINCIPAL are available since 4.1.0
 * Raise exception if zmq_poll returns error.
 * principle->principal
 * exposing ZAPRequest members
 * gss auth callback moved to zauth for implementation override
 * added new gss config options
 * zap interprets gss zap, by default allow all clients
 * prefer boolean to int
 * support for gss mech
 * improve send performance
 * fix error on address part in zmq version >= 4.0.0
 * Add support for ZMQ monitors
 * Fix multiple equal case statements can be enabled at the same time when setting value.
 * fix other duplicate case statement
 * Fix multiple equal case statements can be enabled at the same time.
 * zauth missing from makefile
 * Add support for ZMQ_IMMEDIATE and ZMQ_DELAY_ATTACH_ON_CONNECT socket options.
 * Add PLAIN security auth
 * Fix javadoc errors
 * Put ZAuth back in the package
 * adding more build depends tools
 * pkg-config is needed because of autogen.sh
 * executing autogen.sh so configure script is created and the package can be built
 * Fix missing definition of the ZMQ_CONFLATE option in native methods.
 * Fix type of the ZMQ_CONFLATE option in native method.
 * Fix regression with issue #283
 * Restore the JZMQ_CLASS_FILES vairable.
 * Fixed perf failing to build multithreaded
 * Makefiles can now be executed multithreaded
 * Fixed 'No rule to make target' error in makefile
 * Added missing .class files with a '$' in them to JAR. Removed makefile duplicate
 * Include all class files in the resulting jar.
 * Add missing java classes to make files.
 * Add missing getConflate method to get value of the ZMQ_CONFLATE option.
 * fix jni getLongSockopt
 * Add the ZMQ_CONFLATE option.
 * Fix issue #283 - Add libsodium to travis-ci build
 * Imported Visual Studio solution into Visual Studio 2012. Copied config.hpp into src/main/c++/.
 * don't catch exceptions on junits
 * pushd/popd not working
 * add version to gpg and surefire plugins to avoid mvn warning
 * make jar with right folder structure
 * Fix CMake Error at CMakeLists.txt:204 (add_library): Cannot find source file: ....
 * Remove and ignore INSTALL
 * Fix regression when I rebased
 * Remove config.hpp.in
 * Add Trevor Bernard to contributors
 * More cleaning
 * Make Context instance variable private
 * More cleanup
 * Spring cleaning JNI
 * Clean up jmethodID names
 * Clean up jmethodID and jfieldID names
 * Update license in c++ files and headers
 * Ignore generated files
 * Move perf and cpp into the structure
 * Move perf into src
 * Use standard maven project structure
 * Revert "updated INSTALL and src/config.hpp.in from latest autogen(?)"
 * Use PUSH/PULL for throughput perf teset
 * added darwin subfolder to catch jni_md.h automatically
 * added support for MacOSX and Oracle's Java
 * updated INSTALL and src/config.hpp.in from latest autogen(?)
 * Revert "config/* is on .gitignore, so config/ax_jni_include_dir.m4 shan't be commited"
 * config/* is on .gitignore, so config/ax_jni_include_dir.m4 shan't be commited
 * Fixed broken builds for zmq < 4.0.0
 * add self to authors...
 * cleanup, stubs for other mechs
 * null allows all but blacklisted through
 * ZAuth with null mechanism support
 * Add acknowledgements section
 * Fix issue #259 - Set socket identity now works with 4.0.x
 * Depend on libzmq-dev or libzmq3-dev
 * Fix issue #257 - recvByteBuffer fails if message length is bigger than ByteBuffer limit
 * Update to 4.0
 * Update pom.xml to reflect 4.0
 * [maven-release-plugin] prepare release v3.0.1
 * Fix issue # 253 - Load jzmq in Socket class
 * Fix issue # 253 - Load jzmq in Socket class
 * [maven-release-plugin] prepare for next development iteration
 * [maven-release-plugin] prepare release v3.0.0
 * Prepare code for release
 * Backport Fixes for #250 and #252
 * Fix recvByteBuffer unit test
 * Improvements
 * Fix issue #249 - Take position into account as an offset for send and recv
 * Ignore emacs files

## v3.0.1

## v2.1.1 - February 16, 2013

* Add zero copy API to send and recv
* Remove asserts from get_context JNI
* Add ZLoop support
* Poller rewrite
* No longer c assert when trying to write to a closed Socket
* Add a continuous integration support through travis-ci

## v2.1.0 - February 12, 2013

* First release to Maven Central
