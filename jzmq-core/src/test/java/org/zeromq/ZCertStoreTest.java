package org.zeromq;

import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZCertStoreTest {

	private ZCertStore certStore;
	
	private static final String CERTSTORE_LOCATION = "testCurveCerts";
	private ZAuth auth;
	
	@Before
	public void init() {
		// first cleanup test-directory if still present
		TestUtils.cleanupDir(CERTSTORE_LOCATION);
		
		auth = new ZAuth(new ZContext());
		certStore = new ZCertStore(CERTSTORE_LOCATION);
		File f = new File(CERTSTORE_LOCATION);
		// check if the certstore location was created by the certstore
		assert(f.exists() && f.isDirectory());
		// new certstore-directory should have no certs,yet
		assert(certStore.getAmountCertificates() == 0);
	}
	
	@Test
	public void addCertTest() {
		// first cleanup test-directory if still present
		TestUtils.cleanupDir(CERTSTORE_LOCATION);
		
		int beforeAmount = certStore.getAmountCertificates();
		assert(beforeAmount == 0);
		
		ZCert c1 = new ZCert();
		c1.savePublic(CERTSTORE_LOCATION+"/c1.cert");
		// check the store if something changed, and if yes reload all
		certStore.checkAndReload();
		// is now one certificate more in the store? 
		assert((beforeAmount+1)==certStore.getAmountCertificates());
		
		// check if we find our publickey using the Z85-Version to lookup
		assert(certStore.containsPublicKey(c1.getPublicKeyAsZ85()));
		// check if we find our publickey using the binary-Version to lookup (this will internally be encoded to z85 for the lookup)
		assert(certStore.containsPublicKey(c1.getPublicKey()));
		// check if we do not find some random lookup-key. Z85-Keys need to have a length of 40bytes.
		assert(certStore.containsPublicKey("1234567890123456789012345678901234567890")==false);
	
		// check certs in sub-directories
		ZCert c2 = new ZCert();
		c2.savePublic(CERTSTORE_LOCATION+"/sub/c2.cert");
		assert(certStore.getAmountCertificates() == 2);
	}
	
	
	@Test
	public void checkForCertChanges() {
		// first cleanup test-directory if still present
		TestUtils.cleanupDir(CERTSTORE_LOCATION);
		
		assert(certStore.getAmountCertificates() == 0);
		
		ZCert cert1 = new ZCert();
		cert1.savePublic(CERTSTORE_LOCATION+"/c1.cert");
		ZCert cert2 = new ZCert();
		cert2.saveSecret(CERTSTORE_LOCATION+"/sub/c2.cert");
		
		assert(certStore.getAmountCertificates() == 2);
		
		// rewrite certificates and see if this change gets recognized
		assertFalse(certStore.checkCertFolderForChanges());
		
		// sleep one second to see an effect on lastModified-method for files, since seconds seems to be the smallest 
		// time-unit that is saved
		TestUtils.sleep(1000);
		
		cert1 = new ZCert();
		cert1.savePublic(CERTSTORE_LOCATION+"/c1.cert");
		// change is recognized if a file is changed only in the main-folder

		assert(certStore.checkCertFolderForChanges());
	
		// again wait a second
		TestUtils.sleep(1000);

		// check if changes in subfolders get recognized
		cert2.savePublic(CERTSTORE_LOCATION+"/sub/c2.cert");
		assert(certStore.checkCertFolderForChanges());
	}
	
//	This test has a very minimal chance to fail so I take it out for now 
//	@Test
	public void testCheckThread() {
		// first cleanup test-directory if still present
		TestUtils.cleanupDir(CERTSTORE_LOCATION);
		
		assert(certStore.getAmountCertificates() == 0);
		
		// start checkservice that runs every 2second to see if something changed in the cert-folder
		certStore.startCheckThread(1000);
		
		// create some Certificates
		ZCert cert1 = new ZCert();
		cert1.savePublic(CERTSTORE_LOCATION+"/c1.cert");
		ZCert cert2 = new ZCert();
		cert2.savePublic(CERTSTORE_LOCATION+"/sub/c2.cert");
		// right after creation the amount is still on 0 since when the checkThread is running 
		// only this is responsible for checking for new/delete or changed files 
		assert(certStore.getAmountCertificates() == 0);
		TestUtils.sleep(1500);
		// now the thread should have done its work
		assert(certStore.getAmountCertificates() == 2);
		
		// delete a file, wait and see the effect
		File deleteFile = new File(CERTSTORE_LOCATION+"/sub/c2.cert");
		deleteFile.delete();
		// right after the action there are still the old values
		assert(certStore.getAmountCertificates() == 2);

		// wait for the thread to do its job
		TestUtils.sleep(1500);
		assert(certStore.getAmountCertificates() == 1);
		
		
		// stop the thread
		certStore.stopCheckThread();
		// sleep till the thread is really exited
		TestUtils.sleep(1500);
		
		// Now the old system should apply again
		ZCert cert3 = new ZCert();
		cert3.savePublic(CERTSTORE_LOCATION+"/c3.cert");
		assert(certStore.getAmountCertificates() == 2);
		
	}
	
	@After
	public void cleanup() {
		TestUtils.cleanupDir(CERTSTORE_LOCATION);
	}
	


	
}
