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

#include <zmq.h>
#include <zmq_utils.h>
#include <assert.h>
#include "jzmq.hpp"
#include "util.hpp"
#include "org_zeromq_ZMQ_Curve.h"


JNIEXPORT jobject JNICALL
Java_org_zeromq_ZMQ_00024Curve_generateKeyPair(JNIEnv *env, jclass cls)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    char public_key [41];
    char secret_key [41];

    int rc = zmq_curve_keypair (public_key, secret_key);
    int err = zmq_errno ();

    if (0 != rc) {
        raise_exception (env, err);
        return NULL;
    }

    jstring pk = env->NewStringUTF (public_key);
    assert (pk);
    jstring sk = env->NewStringUTF (secret_key);
    assert (sk);

    jclass clz = env->FindClass ("org/zeromq/ZMQ$Curve$KeyPair");
    assert (clz);
    jmethodID midInit = env->GetMethodID (clz, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
    assert (midInit);
    jobject result = env->NewObject (clz, midInit, pk, sk);
    assert (result);

    return result;
#else
    raise_exception (env, ENOTSUP);
    return NULL;
#endif
}

JNIEXPORT jbyteArray JNICALL
Java_org_zeromq_ZMQ_00024Curve_z85Decode(JNIEnv *env, jclass cls, jstring key)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    const char *in_key = env->GetStringUTFChars (key, NULL);
    assert (in_key);

    uint8_t out_key [32];

    if (NULL == zmq_z85_decode (out_key, (char*)in_key)) {
        env->ReleaseStringUTFChars (key, in_key);
        return NULL;
    }

    env->ReleaseStringUTFChars (key, in_key);
    jbyteArray result = env->NewByteArray (32);
    env->SetByteArrayRegion (result, 0 , 32, reinterpret_cast<jbyte*>(out_key));

    return result;
#else
    raise_exception (env, ENOTSUP);
    return NULL;
#endif
}

JNIEXPORT jstring JNICALL
Java_org_zeromq_ZMQ_00024Curve_z85Encode(JNIEnv *env, jclass cls, jbyteArray key)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
    jbyte *in_key = env->GetByteArrayElements (key, NULL);
    assert (in_key);

    char string_key [41];

    if (NULL == zmq_z85_encode (string_key, reinterpret_cast<uint8_t*>(in_key), 32)) {
        env->ReleaseByteArrayElements (key, in_key, 0);
        return NULL;
    }

    env->ReleaseByteArrayElements (key, in_key, 0);
    jstring result = env->NewStringUTF (string_key);
    assert (result);

    return result;
#else
    raise_exception (env, ENOTSUP);
    return NULL;
#endif
}
