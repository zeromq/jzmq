#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.
# AC_PREREQ(2.59)
#
# Change the version number below after doing a public release.
# The version in git should reflect the *next* version planned.
# Version must be MAJOR.MINOR.PATCH otherwise things will break.
#
AC_INIT([jzmq],[4.0.0],[zeromq-dev@lists.zeromq.org])

AC_CONFIG_AUX_DIR(config)
AC_CONFIG_MACRO_DIR(config)
AC_CONFIG_HEADERS(src/main/c++/config.hpp)
AM_INIT_AUTOMAKE(tar-ustar)

#
# Libtool -version-info (ABI version)
#
# Currently 0.0.0 ("unstable"). Don't change this unless you 
# know exactly what you're doing and have read and understand
# http://www.gnu.org/software/libtool/manual/html_node/Updating-version-info.html
#
# libjzmq -version-info
JLTVER="0:0:0"
AC_SUBST(JLTVER)

# Checks for programs.
AC_PROG_LIBTOOL
#AC_PROG_RANLIB
# AC_PROG_SED
AC_PROG_AWK
AM_PROG_CC_C_O
AC_PROG_CXX
AC_LANG(C++)

# Set default CPPFLAGS for reentrant code
CPPFLAGS="-D_REENTRANT -D_THREAD_SAFE $CPPFLAGS"

# Check for zeromq library
zeromq_prefix=detect
AC_ARG_WITH([zeromq], 
    [AS_HELP_STRING([--with-zeromq=PREFIX], 
        [build with ZeroMQ library installed in PREFIX [default=autodetect]])],
    [case "x$withval" in
        xno)
            AC_MSG_ERROR([jzmq requires the ZeroMQ library])
            ;;
        xyes|x)
            ;;
        *)
            CPPFLAGS="$CPPFLAGS -I${withval}/include"
            LDFLAGS="$LDFLAGS -L${withval}/lib"
            zeromq_prefix=${withval}
            ;;
    esac ]
)
if test "x$zeromq_prefix" = "xdetect"; then
    PKG_CHECK_MODULES(
        [ZeroMQ], [libzmq], [zeromq_prefix=pkgconfig], [zeromq_prefix=])
        if test "x$zeromq_prefix" = "xpkgconfig"; then
            CPPFLAGS="$CPPFLAGS ${ZeroMQ_CFLAGS}"
            LDFLAGS="$LDFLAGS ${ZeroMQ_LIBS}"
        fi
fi

AC_ARG_ENABLE([version], 
							[AS_HELP_STRING([--enable-version], [builds with version information embedded [default=yes]])])
AM_CONDITIONAL(DISABLE_VERSION, test "x$enable_version" = "xno")

AC_CHECK_HEADER([zmq.h], [], 
    [AC_MSG_ERROR([cannot find zmq.h])])

AC_ARG_ENABLE([self-contained], 
							[AS_HELP_STRING([--enable-self-contained], [generates a library that embeds all its external dependencies [default=no]])])

AC_ARG_ENABLE([self-contained-uuid], 
							[AS_HELP_STRING([--enable-self-contained-uuid], [embeds the libuuid dependency in the shared object [default=no]])])

AC_CHECK_LIB([zmq], [zmq_init], 
						 [
						if test "x$enable_self_contained" = "xyes"; then
							LDFLAGS="$LDFLAGS -Wl,-Bstatic -Wl,-lzmq -Wl,-Bdynamic"
						else
							LIBS="-lzmq $LIBS"
						fi
						if test "x$enable_self_contained_uuid" = "xyes"; then
							LDFLAGS="$LDFLAGS -Wl,-luuid"
						fi
						],
    [AC_MSG_ERROR([cannot link with -lzmq])])

# Check for JDK
test "x$JAVAC" = x && AC_CHECK_PROGS(JAVAC, javac -encoding ut8)
test "x$JAVAC" = x && AC_MSG_ERROR([cannot find javac])
AC_SUBST(JAVAC)
AC_SUBST(JAVACFLAGS)

test "x$JAR" = x && AC_CHECK_PROGS(JAR, jar)
test "x$JAR" = x && AC_MSG_ERROR([cannot find jar])
AC_SUBST(JAR)

JAVAROOT=.
AC_SUBST(JAVAROOT)

# Checks for libraries.
AX_JNI_INCLUDE_DIR
for JNI_INCLUDE_DIR in $JNI_INCLUDE_DIRS; do
  CPPFLAGS="$CPPFLAGS -I$JNI_INCLUDE_DIR"
done
AC_SUBST(CPPFLAGS)

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_TYPE_SIZE_T

AC_OUTPUT(Makefile src/main/c++/Makefile src/main/perf/Makefile)
