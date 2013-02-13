/*
    Copyright (c) 2007-2010 iMatix Corporation

    This file is part of 0MQ.

    0MQ is free software; you can redistribute it and/or modify it under
    the terms of the Lesser GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    0MQ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Lesser GNU General Public License for more details.

    You should have received a copy of the Lesser GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <assert.h>

#include <zmq.h>

#include "jzmq.hpp"
#include "util.hpp"
#include "org_zeromq_ZMQ.h"

extern void *get_socket (JNIEnv *env,
                         jobject obj,
                         int do_assert);
/**
 * Called by Java's ZMQ::version_full().
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_version_1full (JNIEnv *env,
                                                          jclass cls)
{
    return ZMQ_VERSION;
}

/*
 * Called by Java's ZMQ::version_major().
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_version_1major (JNIEnv *env,
                                                          jclass cls)
{
    return ZMQ_VERSION_MAJOR;
}

/*
 * Called by Java's ZMQ::version_minor().
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_version_1minor (JNIEnv *env,
                                                          jclass cls)
{
    return ZMQ_VERSION_MINOR;
}

/*
 * Called by Java's ZMQ::version_patch().
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_version_1patch (JNIEnv *env,
                                                          jclass cls)
{
    return ZMQ_VERSION_PATCH;
}

/*
 * Called by Java's ZMQ::version_make().
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_make_1version (JNIEnv *env,
                                                          jclass cls,
                                                          jint major,
                                                          jint minor,
                                                          jint patch)
{
    return ZMQ_MAKE_VERSION(major, minor, patch);
}

/**
 * Called by Java's ZMQ::ENOTSUP().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_ENOTSUP (JNIEnv *env,
                                                     jclass cls)
{
    return ENOTSUP;
}

/**
 * Called by Java's ZMQ::EPROTONOSUPPORT().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_EPROTONOSUPPORT (JNIEnv *env,
                                                             jclass cls)
{
    return EPROTONOSUPPORT;
}

/**
 * Called by Java's ZMQ::ENOBUFS().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_ENOBUFS (JNIEnv *env,
                                                     jclass cls)
{
    return ENOBUFS;
}

/**
 * Called by Java's ZMQ::ENETDOWN().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_ENETDOWN (JNIEnv *env,
                                                      jclass cls)
{
    return ENETDOWN;
}

/**
 * Called by Java's ZMQ::EADDRINUSE().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_EADDRINUSE (JNIEnv *env,
                                                        jclass cls)
{
    return EADDRINUSE;
}

/**
 * Called by Java's ZMQ::EADDRNOTAVAIL().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_EADDRNOTAVAIL (JNIEnv *env,
                                                           jclass cls)
{
    return EADDRNOTAVAIL;
}

/**
 * Called by Java's ZMQ::ECONNREFUSED().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_ECONNREFUSED (JNIEnv *env,
                                                          jclass cls)
{
    return ECONNREFUSED;
}

/**
 * Called by Java's ZMQ::EINPROGRESS().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_EINPROGRESS (JNIEnv *env,
                                                         jclass cls)
{
    return EINPROGRESS;
}

/**
 * Called by Java's ZMQ::EHOSTUNREACH().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_EHOSTUNREACH (JNIEnv *env,
                                                         jclass cls)
{
    return EHOSTUNREACH;
}

/**
 * Called by Java's ZMQ::EMTHREAD().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_EMTHREAD (JNIEnv *env,
                                                      jclass cls)
{
    return EMTHREAD;
}

/**
 * Called by Java's ZMQ::EFSM().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_EFSM (JNIEnv *env,
                                                  jclass cls)
{
    return EFSM;
}

/**
 * Called by Java's ZMQ::ENOCOMPATPROTO().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_ENOCOMPATPROTO (JNIEnv *env,
                                                            jclass cls)
{
    return ENOCOMPATPROTO;
}

/**
 * Called by Java's ZMQ::ETERM().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_ETERM (JNIEnv *env,
                                                   jclass cls)
{
    return ETERM;
}

/**
 * Called by Java's ZMQ::ENOTSOCK().
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_ENOTSOCK (JNIEnv *env,
                                                      jclass cls)
{
    return ENOTSOCK;
}

/**
 * Called by Java's ZMQ::proxy().
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_run_1proxy (JNIEnv *env,
                                                   jclass cls,
                                                   jobject frontend_,
                                                   jobject backend_,
                                                   jobject capture_)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,2)
    void *frontend = get_socket (env, frontend_, true);
    void *backend = get_socket (env, backend_, true);
    void *capture = NULL;
    if (capture_ != NULL)
        capture = get_socket (env, capture_, true);

    zmq_proxy (frontend, backend, capture);
#endif
}
