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
#include "stdio.h"
#include "jzmq.hpp"
#include "util.hpp"
#include "org_zeromq_ZMQ_Curve.h"


JNIEXPORT jobject JNICALL 
Java_org_zeromq_ZMQ_00024Curve_generateKeyPair(JNIEnv *env, jclass cls)
{
    char public_key [41];
    char secret_key [41];

    int rc = zmq_curve_keypair (public_key, secret_key);
    int err = zmq_errno ();

    if (0 != rc) {
        raise_exception (env, err);
        return NULL;
    }

    jstring pk = env->NewStringUTF (public_key);
    jstring sk = env->NewStringUTF (secret_key);

    jclass clz = env->FindClass ("org/zeromq/ZMQ$Curve$KeyPair");
    jmethodID midInit = env->GetMethodID (clz, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
    jobject result = env->NewObject (clz, midInit, pk, sk);

    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_org_zeromq_ZMQ_00024Curve_z85Decode(JNIEnv *env, jclass cls, jstring key)
{
    const char *in_key = env->GetStringUTFChars(key, NULL);
    uint8_t out_key [32];

    if (NULL == zmq_z85_decode (out_key, in_key)) {
        return NULL;
    }

    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0 , 32, reinterpret_cast<jbyte*>(out_key));
    return result;
}

JNIEXPORT jstring JNICALL
Java_org_zeromq_ZMQ_00024Curve_z85Encode(JNIEnv *env, jclass cls, jbyteArray key)
{
    const uint8_t *byte_key = reinterpret_cast<uint8_t*>(env->GetByteArrayElements(key, NULL));
    char string_key [41];

    if (NULL == zmq_z85_encode (string_key, byte_key, 32)) {
        return NULL;
    }

    return env->NewStringUTF (string_key);
}
