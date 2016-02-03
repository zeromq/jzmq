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
#include "org_zeromq_ZMQ_Poller.h"

static jfieldID field_channel;
static jfieldID field_socket;
static jfieldID field_events;
static jfieldID field_revents;

static void *fetch_socket (JNIEnv *env, jobject socket);
static int fetch_socket_fd (JNIEnv *env, jobject socket);

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_00024Poller_run_1poll (JNIEnv *env, jclass cls, jobjectArray socket_0mq, jint count, jlong timeout)
{
    int ls = (int) count;
    if (ls <= 0) {
        return 0;
    }
    
    int ls_0mq = 0;

    if (socket_0mq)
        ls_0mq = env->GetArrayLength (socket_0mq);

    if (ls > ls_0mq) {
        return 0;
    }

    zmq_pollitem_t *pitem = new zmq_pollitem_t [ls];
    short pc = 0;
    int rc = 0;

    // Add 0MQ sockets.  Array containing them can be "sparse": there
    // may be null elements.  The count argument has the real number
    // of valid sockets in the array.
    for (int i = 0; i < ls_0mq; ++i) {
        jobject s_0mq = env->GetObjectArrayElement (socket_0mq, i);
        if (!s_0mq)
            continue;
        void *s = fetch_socket (env, s_0mq);
        int fd = (s == NULL) ? fetch_socket_fd (env, s_0mq) : 0;

        if (s == NULL && fd < 0) {
            raise_exception (env, EINVAL);
            continue;
        }

        env->SetIntField (s_0mq, field_revents, 0);

        pitem [pc].socket = s;
        pitem [pc].fd = fd;
        pitem [pc].events = env->GetIntField (s_0mq, field_events);
        pitem [pc].revents = 0;
        ++pc;

        env->DeleteLocalRef (s_0mq);
    }

    // Count of non-null sockets must be equal to passed-in arg.
    if (pc == ls) {
        pc = 0;
        long tout = (long) timeout;
        rc = zmq_poll (pitem, ls, tout);
    }

    //  Set 0MQ results.
    if (rc > 0 && ls > 0) {
        for (int i = 0; i < ls_0mq; ++i) {
            jobject s_0mq = env->GetObjectArrayElement (socket_0mq, i);
            if (!s_0mq)
                continue;
            env->SetIntField (s_0mq, field_revents, pitem [pc].revents);
            ++pc;
            env->DeleteLocalRef (s_0mq);
        }
    }
    else if (rc < 0)
    {
        raise_exception (env, zmq_errno());
    }

    delete [] pitem;
    return rc;
}

/**
 * Get the value of socketHandle for the specified Java Socket.
 */
static void* fetch_socket (JNIEnv *env, jobject item){

    static jmethodID get_socket_handle_mid = NULL;

    jclass cls;
    if (field_socket == NULL) {
        cls = env->GetObjectClass (item);
        assert (cls);
        field_channel = env->GetFieldID (cls, "channel", "Ljava/nio/channels/SelectableChannel;");
        field_socket = env->GetFieldID (cls, "socket", "Lorg/zeromq/ZMQ$Socket;");
        field_events = env->GetFieldID (cls, "events", "I");
        field_revents = env->GetFieldID (cls, "revents", "I");
        env->DeleteLocalRef (cls);
    }
    jobject socket = env->GetObjectField (item, field_socket);
    if (socket == NULL)
        return NULL;

    if (get_socket_handle_mid == NULL) {
        jclass cls = env->GetObjectClass (socket);
        assert (cls);
        get_socket_handle_mid = env->GetMethodID (cls,
            "getSocketHandle", "()J");
        env->DeleteLocalRef (cls);
        assert (get_socket_handle_mid);
    }
  
    void *s = (void*) env->CallLongMethod (socket, get_socket_handle_mid);
    if (env->ExceptionCheck ()) {
        s = NULL;
    }
  
    return s;
}
/**
 * Get the file descriptor id of java.net.Socket.
 * returns 0 if socket is not a SelectableChannel
 * returns the file descriptor id or -1 on an error
 */
static int fetch_socket_fd (JNIEnv *env, jobject item){

    jclass cls;
    jfieldID fid;
    jobject channel = env->GetObjectField (item, field_channel);
    if (channel == NULL)
        return -1;

    cls = env->GetObjectClass (channel);
    assert (cls);

    fid = env->GetFieldID (cls, "fdVal", "I");
    env->DeleteLocalRef (cls);
    if (fid == NULL)
        return -1;

    /* return the descriptor */
    int fd = env->GetIntField (channel, fid);

    return fd;
}
