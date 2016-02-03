#include <assert.h>
#include <zmq.h>
#include <cstdlib>
#include <cstring>

#include "jzmq.hpp"
#include "util.hpp"
#include "org_zeromq_ZMQ_Event.h"

static jmethodID constructor;

JNIEXPORT void JNICALL
Java_org_zeromq_ZMQ_00024Event_nativeInit (JNIEnv *env, jclass cls)
{
    constructor = env->GetMethodID(cls, "<init>", "(IILjava/lang/String;)V");
    assert(constructor);
}

static zmq_msg_t*
read_msg(JNIEnv *env, void *socket, zmq_msg_t *msg, int flags)
{
    int rc = zmq_msg_init (msg);
    if (rc != 0) {
        raise_exception (env, zmq_errno());
        return NULL;
    }

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,0,0)
    rc = zmq_recvmsg (socket, msg, flags);
#else
    rc = zmq_recv (socket, msg, flags);
#endif
    int err = zmq_errno();
    if (rc < 0 && err == EAGAIN) {
        rc = zmq_msg_close (msg);
        err = zmq_errno();
        if (rc != 0) {
            raise_exception (env, err);
            return NULL;
        }
        return NULL;
    }
    if (rc < 0) {
        raise_exception (env, err);
        rc = zmq_msg_close (msg);
        err = zmq_errno();
        if (rc != 0) {
            raise_exception (env, err);
            return NULL;
        }
        return NULL;
    }
    return msg;
}

JNIEXPORT jobject JNICALL
Java_org_zeromq_ZMQ_00024Event_recv (JNIEnv *env, jclass cls, jlong socket, jint flags)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,2)
    zmq_msg_t event_msg;

    // read event message
    if (!read_msg(env, (void *) socket, &event_msg, flags))
        return NULL;

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    assert (zmq_msg_more(&event_msg) != 0);

    uint16_t event;
    int32_t value;
    // copy event data 
    char *data = (char *) zmq_msg_data(&event_msg);
    memcpy(&event, data, sizeof(event));
    memcpy(&value, data + sizeof(event), sizeof(value));

    if (zmq_msg_close(&event_msg) < 0) {
        raise_exception(env, zmq_errno());
        return NULL;
    }

    char addr[1025];
    char *paddr;
    zmq_msg_t addr_msg;

    // read address message
    if (!read_msg(env, (void *) socket, &addr_msg, flags))
        return NULL;
    assert (zmq_msg_more(&addr_msg) == 0);

    // copy the address string
    const size_t len = zmq_msg_size(&addr_msg);

    paddr = (char *)(len >= sizeof(addr) ? malloc(len + 1) : &addr);
    memcpy(paddr, zmq_msg_data(&addr_msg), len);
    *(paddr + len) = '\0';

    if (zmq_msg_close(&addr_msg) < 0) {
        raise_exception(env, zmq_errno());
        return NULL;
    }

    jstring address = env->NewStringUTF(paddr);
    if (len >= sizeof(addr))
        free(paddr);
    assert(address);

    return env->NewObject(cls, constructor, event, value, address);
#else
    assert (zmq_msg_more(&event_msg) == 0);

    zmq_event_t event;
    // copy event data to event struct
    memcpy (&event, zmq_msg_data (&event_msg), sizeof(event));

    if (zmq_msg_close(&event_msg) < 0) {
        raise_exception(env, zmq_errno());
        return NULL;
    }

    // the addr part is a pointer to a c string that libzmq might have already called free on
    // it is not to be trusted so better not use it at all
    switch (event.event) {
    case ZMQ_EVENT_CONNECTED:
        return env->NewObject(cls, constructor, event.event, event.data.connected.fd, NULL);
    case ZMQ_EVENT_CONNECT_DELAYED:
        return env->NewObject(cls, constructor, event.event, event.data.connect_delayed.err, NULL);
    case ZMQ_EVENT_CONNECT_RETRIED:
        return env->NewObject(cls, constructor, event.event, event.data.connect_retried.interval, NULL);
    case ZMQ_EVENT_LISTENING:
        return env->NewObject(cls, constructor, event.event, event.data.listening.fd, NULL);
    case ZMQ_EVENT_BIND_FAILED:
        return env->NewObject(cls, constructor, event.event, event.data.bind_failed.err, NULL);
    case ZMQ_EVENT_ACCEPTED:
        return env->NewObject(cls, constructor, event.event, event.data.accepted.fd, NULL);
    case ZMQ_EVENT_ACCEPT_FAILED:
        return env->NewObject(cls, constructor, event.event, event.data.accept_failed.err, NULL);
    case ZMQ_EVENT_CLOSED:
        return env->NewObject(cls, constructor, event.event, event.data.closed.fd, NULL);
    case ZMQ_EVENT_CLOSE_FAILED:
        return env->NewObject(cls, constructor, event.event, event.data.close_failed.err, NULL);
    case ZMQ_EVENT_DISCONNECTED:
        return env->NewObject(cls, constructor, event.event, event.data.disconnected.fd, NULL);
    default:
        return NULL;
    }
#endif // ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
#else
    return NULL;
#endif // ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,2)
}
