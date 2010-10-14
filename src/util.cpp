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

#include "util.hpp"

/**
 * Raise an exception that includes 0MQ's error message.
 */
void raise_exception (JNIEnv *env, int err)
{
    //  Get exception class.
    jclass exception_class = env->FindClass ("java/lang/Exception");
    assert (exception_class);

    //  Get text description of the exception.
    const char *err_desc = zmq_strerror (err);

    // Include the error number in the exception text.
    char err_msg[512];
    sprintf(err_msg, "%d - 0x%x - %s", err, err, err_desc);

    //  Raise the exception.
    int rc = env->ThrowNew (exception_class, err_msg);
    env->DeleteLocalRef (exception_class);

    assert (rc == 0);
}
