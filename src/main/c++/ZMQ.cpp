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
// Needed for the curve pieces. Seems like a strong indicator that their
// equivalents belong in util.cpp here.
#include <zmq_utils.h>

#include "jzmq.hpp"
#include "org_zeromq_ZMQ.h"

static void *get_socket (JNIEnv *env, jobject obj)
{
    jclass cls = env->GetObjectClass(obj);
    jfieldID socketHandleFID = env->GetFieldID (cls, "socketHandle", "J");
    env->DeleteLocalRef(cls);
    return (void*) env->GetLongField (obj, socketHandleFID);
}

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_version_1full (JNIEnv *env, jclass cls)
{
    return ZMQ_VERSION;
}

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_version_1major (JNIEnv *env, jclass cls)
{
    return ZMQ_VERSION_MAJOR;
}

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_version_1minor (JNIEnv *env, jclass cls)
{
    return ZMQ_VERSION_MINOR;
}

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_version_1patch (JNIEnv *env, jclass cls)
{
    return ZMQ_VERSION_PATCH;
}

JNIEXPORT jint JNICALL
Java_org_zeromq_ZMQ_make_1version (JNIEnv *env, jclass cls, jint major, jint minor, jint patch)
{
    return ZMQ_MAKE_VERSION(major, minor, patch);
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_ENOTSUP (JNIEnv *env, jclass cls)
{
    return ENOTSUP;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EPROTONOSUPPORT (JNIEnv *env, jclass cls)
{
    return EPROTONOSUPPORT;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_ENOBUFS (JNIEnv *env, jclass cls)
{
    return ENOBUFS;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_ENETDOWN (JNIEnv *env, jclass cls)
{
    return ENETDOWN;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EADDRINUSE (JNIEnv *env, jclass cls)
{
    return EADDRINUSE;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EADDRNOTAVAIL (JNIEnv *env, jclass cls)
{
    return EADDRNOTAVAIL;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_ECONNREFUSED (JNIEnv *env, jclass cls)
{
    return ECONNREFUSED;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EINPROGRESS (JNIEnv *env, jclass cls)
{
    return EINPROGRESS;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EHOSTUNREACH (JNIEnv *env, jclass cls)
{
    return EHOSTUNREACH;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EMTHREAD (JNIEnv *env, jclass cls)
{
    return EMTHREAD;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EFSM (JNIEnv *env, jclass cls)
{
    return EFSM;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_ENOCOMPATPROTO (JNIEnv *env, jclass cls)
{
    return ENOCOMPATPROTO;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_ETERM (JNIEnv *env, jclass cls)
{
    return ETERM;
}

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_ENOTSOCK (JNIEnv *env, jclass cls)
{
    return ENOTSOCK;
}

JNIEXPORT void JNICALL
Java_org_zeromq_ZMQ_run_1proxy (JNIEnv *env, jclass cls, jobject frontend_, jobject backend_, jobject capture_)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(3,2,2)
    void *frontend = get_socket (env, frontend_);
    void *backend = get_socket (env, backend_);
    void *capture = NULL;
    if (capture_ != NULL)
        capture = get_socket (env, capture_);
    zmq_proxy (frontend, backend, capture);
#endif
}

JNIEXPORT jobject JNICALL Java_org_zeromq_ZMQ_curveKeyPairFactory
  (JNIEnv *env, jclass cls)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
  char publicKey[41];
  char privateKey[41];
  int rc = zmq_curve_keypair(publicKey, privateKey);
  if(rc == 0)
    {
      const jbyte* transitive((const jbyte*)publicKey);
      jbyteArray outPublicKey(env->NewByteArray(40));  
      env->SetByteArrayRegion(outPublicKey, 0, 40, transitive); 

      jbyteArray outPrivateKey(env->NewByteArray(40));
      transitive = (const jbyte*)privateKey;
      env->SetByteArrayRegion(outPrivateKey, 0, 40, transitive);

      // TODO: Need to make a new instance of cls, passing in public/private keys as parameters.
      return NULL;
      //assert(false, "Finish writing this");
    }
  else
    {
      // FIXME: How are errors being handled at this level?
      return NULL;
      //assert(false, "Failed to generate key pair");
    }
#else
  // This seems like a poor way to handle this situation
  //assert(false, "No Curve before version 4");
  return NULL;
#endif
}

JNIEXPORT jstring JNICALL Java_org_zeromq_ZMQ_Z85Encode
  (JNIEnv *env, jclass cls, jbyteArray src)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
  jstring result;

#if false
  // This won't work: src is really a byte[], not a Byte[]
  const char* c_src(env->GetStringUTFChars(src));
#else
  const jbyte* bytes(env->GetByteArrayElements(src));
  const char* c_src((const char*)bytes);
#endif
  
  // No finally in C++. RAII says I have to build a class which frees
  // up resources in the destructor.
  try {
    int src_len(strlen(c_src));

    // Destination length must be source length*1.25 + 1 for the NULL terminator
    int src_len_mod_4(src_len % 4);
    // Source must be an even multiple of 4 bytes
    assert(src_len_mod_4 == src_len / 4);
    int dst_len(src_len_mod_4 * 5 + 1);
    char* c_dst(new char[dst_len]);
    try {
      const char* encoded(zmq_z85_encode(c_dst, c_src, src_len));
      if(NULL == encoded) {
	//assert(false, "Handle errors");
	return NULL;
      }
      else {
	result = env->NewStringUTF(c_dst);
      }
    }
    finally {
      delete c_dst;
    }
  finally {
    env->ReleaseByteArrayElements(src, bytes, JNI_ABORT);
  }

  return result;
#else
  // This seems like a poor way to handle this situation
  //assert(false, "No Curve before version 4");
  return NULL;
#endif
}

JNIEXPORT jbyteArray JNICALL Java_org_zeromq_ZMQ_Z85Decode
  (JNIEnv *env, jclass cls, jstring src)
{
#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
  jbyteArray result;

  const char* c_src(env->GetStringUTFChars(src));
  try {
    int src_len(strlen(c_src));
    int src_len_mod_5(src_len % 5);
    assert(src_len_mod_5 == src_len/5);
    int dst_len(src_len_mod_5 * 4);
    char* dst(new char[dst_len]);
    try
      {
	char* decoded(zmq_85_decode(src, dst));
	if(NULL == decoded)
	  {
	    assert(false, "Decoding failed");
	  }
	else
	  {
	    result = env->NewByteArray(dst_len);
	    env->SetByteArrayRegion(result, 0, dst_len, decoded);
	  }
      }
    finally
      {
	delete dst;
      }
  }
  finally {
    (*env)->ReleaseStringUTFChars(env, src, c_src);
  }

  return result;
#else
  // This seems like a poor way to handle this situation
  //assert(false, "No Curve before version 4");
  return NULL;
#endif
}

