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
package org.zeromq;

/**
 * Represent the sort of keypair that ZMQ uses for CURVE encryption.
 *
 * I have a sneaking suspicion that this should really inherit from
 * one of Java's existing key pair classes.
 *
 * Not really worthy of its own class, but I'm having obnoxious
 * issues with JNI trying to make it work as an inner one.
 */
public class ZCurveKeyPair {
    public byte[] publicKey;
    public byte[] privateKey;

    /**
     * Build a keypair from known public/private keys
     */
    ZCurveKeyPair(byte[] pub, byte[] priv) {
	publicKey = pub;
	privateKey = priv;
    }

    /**
     * Generate a random public/private Curve keypair
     */
    public static native ZCurveKeyPair Factory();

    /**
     * Convert a binary key from Z85 printable text
     */
    public static native String Z85Encode(byte[] key);

    /**
     * Convert Z85 printable text into its associated byte array
     */
    public static native byte[] Z85Decode (String s);
}

