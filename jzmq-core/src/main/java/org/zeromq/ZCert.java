package org.zeromq;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal ZCert-class to handle public and secret keys
 * 
 * @author thomas (dot) trocha (at) gmail (dot) com
 *
 */
public class ZCert {
	private byte[] public_key;//  Public key in binary
	private byte[] secret_key;//  Secret key in binary
	private String public_txt;//  Public key in Z85 text
	private String secret_txt;//  Secret key in Z85 text
	private Map<String,String> metadata=new HashMap<String, String>(); //  Certificate metadata
	
	public ZCert(String publicKey) {
		if (publicKey.length()==32) { // in binary-format
			public_key = publicKey.getBytes();
			public_txt = ZMQ.Curve.z85Encode(public_key);
		} else { // Z85-Coded
			public_key = ZMQ.Curve.z85Decode(publicKey);
			public_txt = publicKey;
		}
	}
	
	public ZCert() {
		ZMQ.Curve.KeyPair keypair = ZMQ.Curve.generateKeyPair();
		public_key = ZMQ.Curve.z85Decode(keypair.publicKey);
		public_txt = keypair.publicKey;
		secret_key = ZMQ.Curve.z85Decode(keypair.secretKey);
		secret_txt = keypair.secretKey;
	}
	
	
	
	public byte[] getPublicKey() {
		return public_key;
	}

	public byte[] getSecretKey() {
		return secret_key;
	}

	public String getPublicKeyAsZ85() {
		return public_txt;
	}


	public String getSecretKeyAsZ85() {
		return secret_txt;
	}

	public void setMeta(String key,String value) {
		metadata.put(key, value);
	}

	private void metaToZConfig(Map<String,String> meta,ZConfig zconf) {
		for (String key : meta.keySet()) {
			zconf.putValue("metadata/"+key, meta.get(key));
		}
	}

	/**
	 * Save the public-key to disk
	 * @param filename
	 */
	public void savePublic(String filename) {
		ZConfig zconf = new ZConfig("root",null);
		metaToZConfig(metadata, zconf);
		zconf.addComment("   ZeroMQ CURVE Public Certificate");
		zconf.addComment("   Exchange securely, or use a secure mechanism to verify the contents");
		zconf.addComment("   of this file after exchange. Store public certificates in your home");
		zconf.addComment("   directory, in the .curve subdirectory.");
		zconf.putValue("/curve/public-key", public_txt);
		zconf.save(filename);
	}
	
	/**
	 * save the public- and secret-key to disk
	 * @param filename
	 */
	public void saveSecret(String filename) {
		ZConfig zconf = new ZConfig("root",null);
		metaToZConfig(metadata, zconf);
		zconf.addComment("   ZeroMQ CURVE **Secret** Certificate");
		zconf.addComment("   DO NOT PROVIDE THIS FILE TO OTHER USERS nor change its permissions.");
		zconf.putValue("/curve/public-key", public_txt);
		zconf.putValue("/curve/secret-key", secret_txt);
		zconf.save(filename);
	}
}
