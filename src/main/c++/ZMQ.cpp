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

#include <cstring>

#include <zmq.h>
#include <zmq_utils.h>

#include "jzmq.hpp"
#include "org_zeromq_ZMQ.h"
#include "org_zeromq_ZCurveKeyPair.h"
#include "util.hpp"

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

JNIEXPORT jlong JNICALL
Java_org_zeromq_ZMQ_EAGAIN (JNIEnv *env, jclass cls)
{
    return EAGAIN;
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

JNIEXPORT jobject JNICALL
Java_org_zeromq_ZCurveKeyPair_Factory
  (JNIEnv *env, jclass cls)
{
  jobject result(NULL);

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
  char publicKey[41];
  char privateKey[41];
  // FIXME: DEBUG only: Make these string-safe. 
  publicKey[40] = 0;
  privateKey[40] = 0;

  int rc = zmq_curve_keypair(publicKey, privateKey);
  
  if(rc == 0) {
      const jbyte* j_key(reinterpret_cast<const jbyte*>(publicKey));
      jbyteArray outPublicKey(env->NewByteArray(40));  
      env->SetByteArrayRegion(outPublicKey, 0, 40, j_key); 

      jbyteArray outPrivateKey(env->NewByteArray(40));
      j_key = reinterpret_cast<const jbyte*>(privateKey);
      env->SetByteArrayRegion(outPrivateKey, 0, 40, j_key);

      if(cls != NULL)
	{
	  jmethodID ctor(env->GetMethodID(cls, "<init>", "([B[B)V"));
	  if(ctor != NULL)
	    {
	      result = env->NewObject(cls, ctor, outPublicKey, outPrivateKey);
	    }
	}

      env->DeleteLocalRef(cls);
      // Q: Do I need to release my handle to the byte arrays?
      // A: According to Stack Overflow, this doesn't work:
      //env->ReleaseByteArrayElements(outPublicKey, (jbyte*)outPublicKey, 0);
      // But this does:
      env->DeleteLocalRef(outPrivateKey);
      env->DeleteLocalRef(outPublicKey);
      // TODO: RAII. Really need a class to hold those and release them
      // during its destructor.
    }
  else {
      // Q: How are errors being handled at this level?
      // A: Seems to just be a matter of returning NULL.
      // TODO: A better question might be "How *should*
      // errors be handled at this level?"
      printf("Curve key creation failed. Error Code: %d\n", rc);
    }

  if(!result) {
      // TODO: This really isn't a 0mq error, which is what raise_exception
      // generates.
      raise_exception(env, -1);
    }
#else
  // This seems like a poor way to handle this situation
  //assert(false, "No Curve before version 4");
  // In all honesty, should be raising an exception.
#endif
  return result;
}

struct JavaByteArrayWrapper
// Because C++ doesn't offer try/finally
// Realistically, this should be a template.
// OTOH...I'm shocked that google didn't turn up something exactly along these lines.
{
public:
  jbyteArray _src;
  jbyte* _bytes;
  uint8_t* _binary;
  JNIEnv* _env;

  JavaByteArrayWrapper(JNIEnv* env, jbyteArray source)
    : _src(source),
      _bytes(env->GetByteArrayElements(source, NULL)),
      _binary(reinterpret_cast<uint8_t*>(_bytes)),
      _env(env)
  {}

  virtual ~JavaByteArrayWrapper()
  {
#if false
    // I don't think I want to abort this.
    _env->ReleaseByteArrayElements(_src, _bytes, JNI_ABORT);
#else
    // According to the docs, this should commit changes and free the buffer
    _env->ReleaseByteArrayElements(_src, _bytes, 0);
#endif
  }
};

class AutoString
{
public:
  char* buffer;

  AutoString(int len)
    : buffer(new char[len])
  {
    // Because strings should be NULL-terminated.
    // This probably doesn't actually matter except for logging, but
    // it's better to be safe than sorry.
    buffer[len-1] = 0;
  }

  virtual ~AutoString()
  {
    delete buffer;
  }
};

JNIEXPORT jstring JNICALL Java_org_zeromq_ZCurveKeyPair_Z85Encode
  (JNIEnv *env, jclass cls, jbyteArray src)
{
  jstring result;

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
  JavaByteArrayWrapper wrapper(env, src);

  int src_len(env->GetArrayLength(src));

  // Destination length must be source length*1.25 + 1 for the NULL terminator
  int src_len_mod_4(src_len % 4);
  // Which means that source must be an even multiple of 4 bytes
  if (0 == src_len_mod_4) {
    int dst_len(((src_len / 4) * 5) + 1);
    AutoString c_dst(dst_len);

    const char* encoded(zmq_z85_encode(c_dst.buffer, wrapper._binary, src_len));
    if(NULL != encoded) {
      result = env->NewStringUTF(c_dst.buffer);
    }
  }
#else
  // This seems like a poor way to handle this situation
  //assert(false, "No Curve before version 4");
#endif

  return result;
}

struct UtfWrapper
{
public:
  jsize _length;
  // The constant buffer that java gave us
  const char* _utf;
  // Needed because zmq_z85_decode might mutate it. It really shouldn't, but...
  char* _s;
  jstring _src;
  JNIEnv* _env;

  UtfWrapper(JNIEnv* env, jstring src)
    : _length(env->GetStringUTFLength(src)),
      _utf(env->GetStringUTFChars(src, NULL)),
      _s(new char[_length+1]),
      _src(src),
      _env(env)
  {
    memcpy(_s, _utf, _length);
#if false
    // NULL-terminate the string. This seems like it must be the wrong thing to
    // do, but I'm getting errors without it.
    // And warnings about pointer conversion with it.
    _s[_length] = NULL;
#else
    // Surely there's a "right thing" to do here.
    _s[_length] = 0;
#endif
  }

  virtual ~UtfWrapper()
  {
    _env->ReleaseStringUTFChars(_src, _utf);
    delete _s;
  }
};

JNIEXPORT jbyteArray JNICALL Java_org_zeromq_ZCurveKeyPair_Z85Decode
  (JNIEnv *env, jclass cls, jstring src)
{
  jbyteArray result(NULL);

#if ZMQ_VERSION >= ZMQ_MAKE_VERSION(4,0,0)
  UtfWrapper str(env, src);

  int src_len(strlen(str._s));
  
  // Must be a multiple of 5 in length
  int src_len_mod_5(src_len % 5);
  if(0 == src_len_mod_5) {
    int dst_len((src_len / 5) * 4);
    AutoString dst(dst_len);
    uint8_t* decoded(zmq_z85_decode(reinterpret_cast<uint8_t*>(dst.buffer), str._s));
    if(NULL != decoded) {
      result = env->NewByteArray(dst_len);
      jbyte* j_decoded(reinterpret_cast<jbyte*>(decoded));
      env->SetByteArrayRegion(result, 0, dst_len, j_decoded);
    }
  }
#else
  // This seems like a poor way to handle this situation
  // Especially because assert isn't actually defined here.
  assert(false, "No Curve before version 4");
#endif
  return result;
}

