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
#include <string.h>
#include <zmq.h>

#include "jzmq.hpp"
#include "util.hpp"
#include "org_zeromq_ZMQ_Socket.h"

static jfieldID  socketHandleFID;
static jmethodID contextHandleMID;
static jmethodID limitMID;
static jmethodID positionMID;
static jmethodID setPositionMID;

static zmq_msg_t* do_read(JNIEnv *env, jobject obj, zmq_msg_t *message, int flags);

JNIEXPORT void JNICALL
Java_org_zeromq_ZMQ_00024Socket_nativeInit (JNIEnv *env, jclass c)
{
    jclass bbcls = env->FindClass("java/nio/ByteBuffer");
    limitMID = env->GetMethodID(bbcls, "limit", "()I");
    positionMID = env->GetMethodID(bbcls, "position", "()I");
    setPositionMID = env->GetMethodID(bbcls, "position", "(I)Ljava/nio/Buffer;");
    env->DeleteLocalRef(bbcls);

    jclass contextcls = env->FindClass("org/zeromq/ZMQ$Context");
    contextHandleMID = env->GetMethodID(contextcls, "getContextHandle", "()J");
    env->DeleteLocalRef(contextcls);
    socketHandleFID = env->GetFieldID(c, "socketHandle", "J");
}

inline void *get_socket (JNIEnv *env, jobject obj)
{
    return (void*) env->GetLongField (obj, socketHandleFID);
}

inline void put_socket (JNIEnv *env, jobject obj, void *s)
{
    env->SetLongField (obj, socketHandleFID, (jlong) s);
}

inline void *fetch_context (JNIEnv *env, jobject context)
{
    return (void*) env->CallLongMethod (context, contextHandleMID);
}

static
zmq_msg_t *do_read(JNIEnv *env, jobject obj, zmq_msg_t *message, int flags)
{
    void *socket = get_socket (env, obj);

    int rc = zmq_msg_init (message);
    if (rc != 0) {
        raise_exception (env, zmq_errno());
        return NULL;
    }

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    rc = zmq_recvmsg (socket, message, flags);
#else
    rc = zmq_recv (socket, message, flags);
#endif
    int err = zmq_errno();
    if (rc < 0 && err == EAGAIN) {
        rc = zmq_msg_close (message);
        err = zmq_errno();
        if (rc != 0) {
            raise_exception (env, err);
            return NULL;
        }
        return NULL;
    }
    if (rc < 0) {
        raise_exception (env, err);
        rc = zmq_msg_close (message);
        err = zmq_errno();
        if (rc != 0) {
            raise_exception (env, err);
            return NULL;
        }
        return NULL;
    }
    return message;
}

JNIEXPORT void JNICALL
Java_org_zeromq_ZMQ_00024Socket_construct (JNIEnv *env, jobject obj, jobject context, jint type)
{
    void *s = get_socket (env, obj);
    if (s)
        return;

    void *c = fetch_context (env, context);
    if (c == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    s = zmq_socket (c, type);
    int err = zmq_errno();

    if (s == NULL) {
        raise_exception (env, err);
        return;
    }
    put_socket(env, obj, s);
}

/**
 * Called to destroy a Java Socket object.
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_destroy (JNIEnv *env, jobject obj)
{
    void *s = get_socket (env, obj);
    if (! s)
        return;

    int rc = zmq_close (s);
    int err = zmq_errno();
    s = NULL;
    put_socket (env, obj, s);

    if (rc != 0) {
        raise_exception (env, err);
        return;
    }
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_00024Socket_getLongSockopt (JNIEnv *env, jobject obj, jint option)
{

	
	switch (option) {
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    case ZMQ_BACKLOG:
    case ZMQ_MAXMSGSIZE:
    case ZMQ_SNDHWM:
    case ZMQ_RCVHWM:
    case ZMQ_MULTICAST_HOPS:
#else
    case ZMQ_HWM:
    case ZMQ_SWAP:
    case ZMQ_MCAST_LOOP:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,2,0)
    case ZMQ_RCVTIMEO:
    case ZMQ_SNDTIMEO:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,10)
    case ZMQ_RECONNECT_IVL:
    case ZMQ_RECONNECT_IVL_MAX:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,0)
    case ZMQ_TYPE:
    case ZMQ_FD:
    case ZMQ_EVENTS:
    case ZMQ_LINGER:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0)
    case ZMQ_TCP_KEEPALIVE:
    case ZMQ_TCP_KEEPALIVE_IDLE:
    case ZMQ_TCP_KEEPALIVE_CNT:
    case ZMQ_TCP_KEEPALIVE_INTVL:
    case ZMQ_IPV4ONLY:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0) && ZMQ_VERSION < ZMQ_MAKE_VERSION(4,0,0)
    case ZMQ_DELAY_ATTACH_ON_CONNECT:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    case ZMQ_CONFLATE:
    case ZMQ_PLAIN_SERVER:
    case ZMQ_IMMEDIATE:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,1,0)
    case ZMQ_ROUTER_HANDOVER:
#endif		    
    case ZMQ_AFFINITY:
    case ZMQ_RATE:
    case ZMQ_RECOVERY_IVL:
    case ZMQ_SNDBUF:
    case ZMQ_RCVBUF:
    case ZMQ_RCVMORE:
        {
            void *s = get_socket (env, obj);
            jlong ret = 0;
            int rc = 0;
            int err = 0;
            if (
                   (option == ZMQ_AFFINITY)
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,0)
                || (option == ZMQ_FD)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
                || (option == ZMQ_MAXMSGSIZE)
#endif
            ) {
                int64_t optval = 0; 
                size_t optvallen = sizeof(optval);
                rc = zmq_getsockopt (s, option, &optval, &optvallen);
                ret = (jlong) optval;
            } else
            {
                int optval = 0;
                size_t optvallen = sizeof(optval);
                rc = zmq_getsockopt (s, option, &optval, &optvallen);
                ret = (jlong) optval;
            }
            err = zmq_errno();

            if (rc != 0) {
                raise_exception (env, err);
                return 0L;
            }
            return ret;
        }
    default:
        raise_exception (env, EINVAL);
        return 0L;
    }
}

/**
 * Called by Java's Socket::getBytesSockopt(int option).
 */
JNIEXPORT jbyteArray JNICALL Java_org_zeromq_ZMQ_00024Socket_getBytesSockopt (JNIEnv *env,
                                                                              jobject obj,
                                                                              jint option)
{
    switch (option) {
    case ZMQ_IDENTITY:
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0)
    case ZMQ_LAST_ENDPOINT:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    case ZMQ_PLAIN_USERNAME:
    case ZMQ_PLAIN_PASSWORD:
#endif
        {
            void *s = get_socket (env, obj);

            // Warning: hard-coded limit here.
            char optval[1024];
            size_t optvallen = 1024;
            int rc = zmq_getsockopt (s, option, optval, &optvallen);
            int err = zmq_errno();
            if (rc != 0) {
                raise_exception (env, err);
                return env->NewByteArray (0);
            }

            jbyteArray array = env->NewByteArray (optvallen);
            if (array == NULL) {
                raise_exception (env, EINVAL);
                return env->NewByteArray(0);
            }

            env->SetByteArrayRegion (array, 0, optvallen, (jbyte*) optval);
            return array;
        }
    default:
        raise_exception (env, EINVAL);
        return env->NewByteArray(0);
    }
}

/**
 * Called by Java's Socket::setLongSockopt(int option, long value).
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_setLongSockopt (JNIEnv *env,
                                                                       jobject obj,
                                                                       jint option,
                                                                       jlong value)
{
    switch (option) {
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    case ZMQ_BACKLOG:
    case ZMQ_MAXMSGSIZE:
    case ZMQ_SNDHWM:
    case ZMQ_RCVHWM:
    case ZMQ_MULTICAST_HOPS:
#else
    case ZMQ_HWM:
    case ZMQ_SWAP:
    case ZMQ_MCAST_LOOP:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,2,0)
    case ZMQ_RCVTIMEO:
    case ZMQ_SNDTIMEO:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,10)
    case ZMQ_RECONNECT_IVL:
    case ZMQ_RECONNECT_IVL_MAX:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,0)
    case ZMQ_LINGER:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0)
    case ZMQ_TCP_KEEPALIVE:
    case ZMQ_TCP_KEEPALIVE_IDLE:
    case ZMQ_TCP_KEEPALIVE_CNT:
    case ZMQ_TCP_KEEPALIVE_INTVL:
    case ZMQ_IPV4ONLY:
    case ZMQ_ROUTER_MANDATORY:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0) && ZMQ_VERSION < ZMQ_MAKE_VERSION(4,0,0)
    case ZMQ_DELAY_ATTACH_ON_CONNECT:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,2)
    case ZMQ_XPUB_VERBOSE:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    case ZMQ_CONFLATE:
    case ZMQ_PLAIN_SERVER:
    case ZMQ_IMMEDIATE:
    case ZMQ_REQ_RELAXED:
    case ZMQ_REQ_CORRELATE:
    case ZMQ_PROBE_ROUTER:
    case ZMQ_CURVE_SERVER:
#endif
    case ZMQ_AFFINITY:
    case ZMQ_RATE:
    case ZMQ_RECOVERY_IVL:
    case ZMQ_ROUTER_HANDOVER:	
    case ZMQ_SNDBUF:
    case ZMQ_RCVBUF:
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,1,0)
    case ZMQ_GSSAPI_SERVER:
    case ZMQ_GSSAPI_PLAINTEXT:
#endif
        {
            void *s = get_socket (env, obj);
            int rc = 0;
            int err = 0;

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,0)
            if(
                (option == ZMQ_LINGER)
                || (option == ZMQ_RATE)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,10)
                || (option == ZMQ_RECONNECT_IVL)
                || (option == ZMQ_RECONNECT_IVL_MAX)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,2,0)
                || (option == ZMQ_SNDTIMEO)
                || (option == ZMQ_RCVTIMEO)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0)
                || (option == ZMQ_TCP_KEEPALIVE)
                || (option == ZMQ_TCP_KEEPALIVE_IDLE)
                || (option == ZMQ_TCP_KEEPALIVE_CNT)
                || (option == ZMQ_TCP_KEEPALIVE_INTVL)
                || (option == ZMQ_IPV4ONLY)
                || (option == ZMQ_ROUTER_MANDATORY)
                || (option == ZMQ_DELAY_ATTACH_ON_CONNECT)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,2)
                || (option == ZMQ_XPUB_VERBOSE)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
                || (option == ZMQ_SNDBUF)
                || (option == ZMQ_RCVBUF)
                || (option == ZMQ_SNDHWM)
                || (option == ZMQ_RCVHWM)
                || (option == ZMQ_RECOVERY_IVL)
                || (option == ZMQ_BACKLOG)
                || (option == ZMQ_MULTICAST_HOPS)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
                || (option == ZMQ_CONFLATE)
                || (option == ZMQ_PLAIN_SERVER)
                || (option == ZMQ_IMMEDIATE)
                || (option == ZMQ_REQ_RELAXED)
                || (option == ZMQ_REQ_CORRELATE)
                || (option == ZMQ_PROBE_ROUTER)
                || (option == ZMQ_CURVE_SERVER)	
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,1,0)
                || (option == ZMQ_GSSAPI_SERVER)
                || (option == ZMQ_GSSAPI_PLAINTEXT)
		|| (option == ZMQ_ROUTER_HANDOVER)
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(2,1,0)
            ) {
                int ival = (int) value;
                size_t optvallen = sizeof(ival);
                rc = zmq_setsockopt (s, option, &ival, optvallen);
            } else
#endif
            {
                uint64_t optval = (uint64_t) value;
                size_t optvallen = sizeof(optval);
                rc = zmq_setsockopt (s, option, &optval, optvallen);
            }
            err = zmq_errno();

            if (rc != 0 && err != ETERM) {
                raise_exception (env, err);
            }
            return;
        }
    default:
        raise_exception (env, EINVAL);
        return;
    }
}

/**
 * Called by Java's Socket::setBytesSockopt(int option, byte[] value).
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_setBytesSockopt (JNIEnv *env,
                                                                        jobject obj,
                                                                        jint option,
                                                                        jbyteArray value)
{
    switch (option) {
    case ZMQ_IDENTITY:
    case ZMQ_SUBSCRIBE:
    case ZMQ_UNSUBSCRIBE:
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    case ZMQ_ZAP_DOMAIN:
    case ZMQ_PLAIN_USERNAME:
    case ZMQ_PLAIN_PASSWORD:
    case ZMQ_CURVE_PUBLICKEY:
    case ZMQ_CURVE_SECRETKEY:
    case ZMQ_CURVE_SERVERKEY:
#endif
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,1,0)
    case ZMQ_GSSAPI_PRINCIPAL:
    case ZMQ_GSSAPI_SERVICE_PRINCIPAL:
#endif
        {
            if (value == NULL) {
                raise_exception (env, EINVAL);
                return;
            }

            void *s = get_socket (env, obj);

            jbyte *optval = env->GetByteArrayElements (value, NULL);
            if (! optval) {
                raise_exception (env, EINVAL);
                return;
            }
            size_t optvallen = env->GetArrayLength (value);
            int rc = zmq_setsockopt (s, option, optval, optvallen);
            int err = zmq_errno();
            env->ReleaseByteArrayElements (value, optval, 0);
            if (rc != 0) {
                raise_exception (env, err);
            }

            return;
        }
    default:
        raise_exception (env, EINVAL);
        return;
    }
}

/**
 * Called by Java's Socket::bind(String addr).
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_bind (JNIEnv *env,
                                                             jobject obj,
                                                             jstring addr)
{
    void *s = get_socket (env, obj);

    if (addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    const char *c_addr = env->GetStringUTFChars (addr, NULL);
    if (c_addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    int rc = zmq_bind (s, c_addr);
    int err = zmq_errno();
    env->ReleaseStringUTFChars (addr, c_addr);

    if (rc != 0) {
        raise_exception (env, err);
        return;
    }
}

/**
 * Called by Java's Socket::unbind(String addr).
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_unbind (JNIEnv *env,
                                                               jobject obj,
                                                               jstring addr)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0)
    void *s = get_socket (env, obj);

    if (addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    const char *c_addr = env->GetStringUTFChars (addr, NULL);
    if (c_addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    int rc = zmq_unbind (s, c_addr);
    int err = zmq_errno();
    env->ReleaseStringUTFChars (addr, c_addr);

    if (rc != 0) {
        raise_exception (env, err);
        return;
    }
#endif
}

/**
 * Called by Java's Socket::connect(String addr).
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_connect (JNIEnv *env,
                                                                jobject obj,
                                                                jstring addr)
{
    void *s = get_socket (env, obj);

    if (addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    const char *c_addr = env->GetStringUTFChars (addr, NULL);
    if (c_addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    int rc = zmq_connect (s, c_addr);
    int err = zmq_errno();
    env->ReleaseStringUTFChars (addr, c_addr);

    if (rc != 0) {
        raise_exception (env, err);
        return;
    }
}

/**
 * Called by Java's Socket::disconnect(String addr).
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_disconnect (JNIEnv *env,
                                                                   jobject obj,
                                                                   jstring addr)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0)
    void *s = get_socket (env, obj);

    if (addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    const char *c_addr = env->GetStringUTFChars (addr, NULL);
    if (c_addr == NULL) {
        raise_exception (env, EINVAL);
        return;
    }

    int rc = zmq_disconnect (s, c_addr);
    int err = zmq_errno();
    env->ReleaseStringUTFChars (addr, c_addr);

    if (rc != 0) {
        raise_exception (env, err);
        return;
    }
#endif
}

typedef struct _jzmq_zerocopy_t {
    JNIEnv *env;
    jobject ref_buffer;
} jzmq_zerocopy_t;

static
void s_delete_ref (void *ptr, void *hint)
{
    jzmq_zerocopy_t *free_hint = (jzmq_zerocopy_t *)hint;
    free_hint->env->DeleteGlobalRef(free_hint->ref_buffer);
    delete free_hint;
}

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
static
jboolean s_zerocopy_init (JNIEnv *env, zmq_msg_t *message, jobject obj, jint length)
{
    jobject ref_buffer = env->NewGlobalRef(obj);
    jzmq_zerocopy_t *free_hint = new jzmq_zerocopy_t;

    free_hint->env = env;
    free_hint->ref_buffer = ref_buffer;

    jbyte* buf = (jbyte*) env->GetDirectBufferAddress(ref_buffer);

    int rc = zmq_msg_init_data (message, buf, length, s_delete_ref, free_hint);
    if (rc != 0) {
        int err = zmq_errno();
        raise_exception (env, err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}
#endif

JNIEXPORT jboolean JNICALL
Java_org_zeromq_ZMQ_00024Socket_sendZeroCopy (JNIEnv *env,
                                              jobject obj,
                                              jobject buffer,
                                              jint length,
                                              jint flags)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    int rc = 0;
    void *sock = get_socket (env, obj);

    // init the message
    zmq_msg_t message;
    jboolean retval = s_zerocopy_init (env, &message, buffer, length);
    if (retval == JNI_FALSE)
        return JNI_FALSE;

    rc = zmq_sendmsg (sock, &message, flags);
    if (rc == -1) {
        int err = zmq_errno();
        zmq_msg_close (&message);
        raise_exception (env, err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT
jint JNICALL
Java_org_zeromq_ZMQ_00024Socket_sendByteBuffer (JNIEnv *env, jobject obj, jobject buffer, jint flags)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    jbyte* buf = (jbyte*) env->GetDirectBufferAddress(buffer);
    if(buf == NULL)
        return -1;

    void *sock = get_socket (env, obj);

    int lim = env->CallIntMethod(buffer, limitMID);
    int pos = env->CallIntMethod(buffer, positionMID);
    int rem = pos <= lim ? lim - pos : 0;

    int rc = zmq_send(sock, buf + pos, rem, flags);

    if (rc > 0)
        env->CallVoidMethod(buffer, setPositionMID, pos + rc);

    if (rc == -1) {
        int err = zmq_errno();
        raise_exception (env, err);
        return -1;
    }
    return rc;
#else
    return JNI_FALSE;
#endif
}

/**
 * Called by Java's Socket::send(byte [] msg, int offset, int flags).
 */
JNIEXPORT jboolean JNICALL Java_org_zeromq_ZMQ_00024Socket_send (JNIEnv *env,
                                                                 jobject obj,
                                                                 jbyteArray msg,
                                                                 jint offset,
                                                                 jint length,
                                                                 jint flags)
{
    void *s = get_socket (env, obj);

    if (length < 0) {
        raise_exception(env, EINVAL);
        return JNI_FALSE;
    }

    zmq_msg_t message;
    int rc = zmq_msg_init_size (&message, length);
    int err = zmq_errno();
    if (rc != 0) {
        raise_exception (env, err);
        return JNI_FALSE;
    }
    
    void* pd = zmq_msg_data (&message);
    env->GetByteArrayRegion(msg, offset, length, (jbyte*) pd);
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    rc = zmq_sendmsg (s, &message, flags);
#else
    rc = zmq_send (s, &message, flags);
#endif
    err = zmq_errno();

    if (rc < 0 && err == EAGAIN) {
        rc = zmq_msg_close (&message);
        err = zmq_errno();
        if (rc != 0) {
            raise_exception (env, err);
            return JNI_FALSE;
        }
        return JNI_FALSE;
    }
    
    if (rc < 0) {
        raise_exception (env, err);
        rc = zmq_msg_close (&message);
        err = zmq_errno();
        if (rc != 0) {
            raise_exception (env, err);
            return JNI_FALSE;
        }
        return JNI_FALSE;
    }

    rc = zmq_msg_close (&message);
    err = zmq_errno();
    if (rc != 0) {
        raise_exception (env, err);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

inline void setByteBufferPosition(JNIEnv *env, jobject buffer, jint position) {
	jclass cls = env->GetObjectClass(buffer);
	jmethodID positionHandle = env->GetMethodID(cls, "position", "(I)Ljava/nio/Buffer;");
	env->DeleteLocalRef(cls);
	env->CallVoidMethod(buffer, positionHandle, position);
}

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_00024Socket_recvZeroCopy (JNIEnv *env,
                                              jobject obj,
                                              jobject buffer,
                                              jint length,
                                              jint flags)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    jbyte* buf = (jbyte*) env->GetDirectBufferAddress(buffer);

    if(buf == NULL)
        return -1;

    void* sock = get_socket (env, obj);
    int rc = zmq_recv(sock, buf, length, flags);
    if (rc > 0) {
        int newpos = rc > length ? length : rc;
        setByteBufferPosition(env, buffer, newpos);
    }
    if(rc == -1) {
        int err = zmq_errno();
        if(err != EAGAIN) {
            raise_exception (env, err);
            return 0;
        }
    }
    return rc;
#else
    return -1;
#endif
} 

JNIEXPORT
jint JNICALL
Java_org_zeromq_ZMQ_00024Socket_recvByteBuffer (JNIEnv *env, jobject obj, jobject buffer, jint flags)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    jbyte *buf = (jbyte*) env->GetDirectBufferAddress(buffer);
    if(buf == NULL)
        return -1;

    void *sock = get_socket (env, obj);

    int lim = env->CallIntMethod(buffer, limitMID);
    int pos = env->CallIntMethod(buffer, positionMID);
    int rem = pos <= lim ? lim - pos : 0;

    int read = zmq_recv(sock, buf + pos, rem, flags);
    if (read > 0) {
        read = read > rem ? rem : read;
        env->CallObjectMethod(buffer, setPositionMID, read + pos);
        return read;
    }
    else if(read == -1) {
        int err = zmq_errno();
        if(err != EAGAIN) {
            raise_exception (env, err);
            return 0;
        }
    }
    return read;
#else
    return JNI_FALSE;
#endif
}

/**
 * Called by Java's Socket::recv(byte[] buffer, int offset, int len, int flags).
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_00024Socket_recv___3BIII (JNIEnv *env, 
                                                                     jobject obj, 
                                                                     jbyteArray buff, 
                                                                     jint offset, 
                                                                     jint len, 
                                                                     jint flags)
{
    zmq_msg_t message;
    if (!do_read(env,obj,&message,flags)) {
        return -1;
    }
    // No errors are defined for these two functions. Should they?
    int sz = zmq_msg_size (&message);
    void* pd = zmq_msg_data (&message);
    
    int stored = sz > len ? len : sz;
    env->SetByteArrayRegion (buff, offset, stored, (jbyte*) pd);

    int rc = zmq_msg_close(&message);
    if(rc == -1) {
        int err = zmq_errno();
        raise_exception (env, err);
        return -1;
    } 
    return stored;
}


/**
 * Called by Java's Socket::recv(int flags).
 */
JNIEXPORT jbyteArray JNICALL Java_org_zeromq_ZMQ_00024Socket_recv__I (JNIEnv *env,
                                                                      jobject obj,
                                                                      jint flags)
{
    zmq_msg_t message;
    if (!do_read(env,obj,&message,flags)) {
        return NULL;
    }
    // No errors are defined for these two functions. Should they?
    int sz = zmq_msg_size (&message);
    void* pd = zmq_msg_data (&message);

    jbyteArray data = env->NewByteArray (sz);
    if (! data) {
        raise_exception (env, EINVAL);
        return NULL;
    }

    env->SetByteArrayRegion (data, 0, sz, (jbyte*) pd);

    int rc = zmq_msg_close(&message);
    if(rc == -1) {
        int err = zmq_errno();
        raise_exception (env, err);
        return NULL;
    } 
    return data;
}

JNIEXPORT jboolean JNICALL Java_org_zeromq_ZMQ_00024Socket_monitor (JNIEnv *env,
                                                                    jobject obj,
                                                                    jstring addr,
                                                                    jint events)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,0)
    void *socket = get_socket (env, obj);

    const char *c_addr = addr ? env->GetStringUTFChars (addr, NULL) : NULL;

    int rc = zmq_socket_monitor(socket , c_addr, events);
    int err = rc < 0 ? zmq_errno() : 0;

    env->ReleaseStringUTFChars (addr, c_addr);

    if (rc < 0) {
        raise_exception (env, err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}
