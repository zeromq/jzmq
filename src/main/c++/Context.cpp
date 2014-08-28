/*
    Copyright (c) 2007-2013 Contributors as noted in the AUTHORS file

    This file is part of 0MQ.

    0MQ is free software; you can redistribute it and/or modify it under
    the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    0MQ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <assert.h>

#include <zmq.h>

#include "jzmq.hpp"
#include "util.hpp"
#include "org_zeromq_ZMQ_Context.h"

static jfieldID contextptrFID;

static void ensure_context (JNIEnv *env, jobject obj);
static void *get_context (JNIEnv *env, jobject obj);
static void put_context (JNIEnv *env, jobject obj, void *s);


/**
 * Called to construct a Java Context object.
 */
JNIEXPORT void JNICALL
Java_org_zeromq_ZMQ_00024Context_construct (JNIEnv *env, jobject obj, jint io_threads)
{
    void *c = get_context (env, obj);
    if (c)
        return;

    c = zmq_init (io_threads);
    int err = zmq_errno();
    put_context (env, obj, c);

    if (c == NULL) {
        raise_exception (env, err);
        return;
    }
}

/**
 * Called to destroy a Java Context object.
 */
JNIEXPORT void JNICALL
Java_org_zeromq_ZMQ_00024Context_destroy (JNIEnv *env, jobject obj) {
    void *c = get_context (env, obj);
    if (! c)
        return;

    int rc = zmq_term (c);
    int err = zmq_errno();
    c = NULL;
    put_context (env, obj, c);

    if (rc != 0) {
        raise_exception (env, err);
        return;
    }
}

JNIEXPORT jboolean JNICALL
Java_org_zeromq_ZMQ_00024Context_setMaxSockets (JNIEnv * env, jobject obj, jint maxSockets)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    void *c = get_context (env, obj);
    if (! c)
        return JNI_FALSE;
    int result = zmq_ctx_set (c, ZMQ_MAX_SOCKETS, maxSockets);
    return result == 0;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_00024Context_getMaxSockets (JNIEnv *env, jobject obj)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    void *c = get_context (env, obj);
    if (! c)
        return -1;

    return zmq_ctx_get (c, ZMQ_MAX_SOCKETS);
#else
    return -1;
#endif
}

/**
 * Make sure we have a valid pointer to Java's Context::contextHandle.
 */
static void ensure_context (JNIEnv *env, jobject obj)
{
    if (contextptrFID == NULL) {
        jclass cls = env->GetObjectClass (obj);
        assert (cls);
        contextptrFID = env->GetFieldID (cls, "contextHandle", "J");
        assert (contextptrFID);
        env->DeleteLocalRef (cls);
    }
}

/**
 * Get the value of Java's Context::contextHandle.
 */
static void *get_context (JNIEnv *env, jobject obj)
{
    ensure_context (env, obj);
    void *s = (void*) env->GetLongField (obj, contextptrFID);

    return s;
}

/**
 * Set the value of Java's Context::contextHandle.
 */
static void put_context (JNIEnv *env, jobject obj, void *s)
{
    ensure_context (env, obj);
    env->SetLongField (obj, contextptrFID, (jlong) s);
}
