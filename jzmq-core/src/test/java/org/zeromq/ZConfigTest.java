package org.zeromq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 * Test ZConfig
 * 
 * @author dertom
 *
 */
public class ZConfigTest {

	private static final String TEST_FOLDER = "testCertFolder";
	private static ZConfig conf = new ZConfig("root",null);


	@Before
	public void init() {
		conf.putValue("/curve/public-key", "abcdefg");
		conf.putValue("/curve/secret-key", "(w3lSF/5yv&j*c&0h{4JHe(CETJSksTr.QSjcZE}");
		conf.putValue("metadata/name", "key-value tests");
		
		// create test-file with values that should be compatible but are actually not create with this implementation
		try {
			File dir = new File(TEST_FOLDER);
			if (!dir.exists()) {
				dir.mkdir();
			}
			FileWriter write = new FileWriter(TEST_FOLDER+"/test.zpl");
			write.write("1. ZPL configuration file example\n"); // should be discarded
			write.write(" # some initial comment \n"); // should be discarded
			write.write("meta\n");
			write.write("    leadingquote = \"abcde\n");
			write.write("    endingquote = abcde\"\n");
			write.write("    quoted = \"abcde\"\n");
			write.write("    singlequoted = 'abcde'\n");
			write.write("    bind = tcp://eth0:5555\n");
			write.write("    verbose = 1      #   Ask for a trace\n");
			write.write("    sub # some comment after container-name\n");  
			write.write("        fortuna = f95\n");
			write.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testPutGet(){
		assert(conf.getValue("/curve/public-key").equals("abcdefg"));
		// intentionally checking without leading /
		assert(conf.getValue("curve/secret-key").equals("(w3lSF/5yv&j*c&0h{4JHe(CETJSksTr.QSjcZE}"));
		assert(conf.getValue("/metadata/name").equals("key-value tests"));
		
		// checking default value
		assert(conf.getValue("/metadata/nothinghere","default").equals("default"));
	}
	

	@Test
	public void testLoadSave() {
		conf.save(TEST_FOLDER+"/test.cert");
		assert(isFileInPath(TEST_FOLDER,"test.cert"));
		ZConfig loadedConfig = ZConfig.load(TEST_FOLDER+"/test.cert");
		Object obj = loadedConfig.getValue("/curve/public-key");
		assert(loadedConfig.getValue("/curve/public-key").equals("abcdefg"));
		// intentionally checking without leading /
		assert(loadedConfig.getValue("curve/secret-key").equals("(w3lSF/5yv&j*c&0h{4JHe(CETJSksTr.QSjcZE}"));
		assert(loadedConfig.getValue("/metadata/name").equals("key-value tests"));
	}

	private boolean isFileInPath(String path,String filename) {
		File dir = new File(path);
		if (!dir.isDirectory()) {
			return false;
		}
		for (File file : dir.listFiles()){
			if (file.getName().equals(filename)) {
				return true;
			}
		}
		return false;
	}
	
	@Test
	public void testZPLSpecialCases() {
		// this file was generated in the init-method and tests some cases that should be processed by the loader but are not
		// created with our writer.
		ZConfig zplSpecials = ZConfig.load(TEST_FOLDER+"/test.zpl");
		// test leading quotes
		assert(zplSpecials.getValue("meta/leadingquote").equals("\"abcde"));
		// test ending quotes
		assert(zplSpecials.getValue("meta/endingquote").equals("abcde\""));
		// test full doublequoted. here the quotes should be removed
		assert(zplSpecials.getValue("meta/quoted").equals("abcde"));
		// test full singlequoted. here the quotes should be removed
		assert(zplSpecials.getValue("meta/singlequoted").equals("abcde"));
		// test no quotes tcp-pattern 
		assert(zplSpecials.getValue("meta/bind").equals("tcp://eth0:5555"));
		// test comment after value
		assert(zplSpecials.getValue("meta/verbose").equals("1"));
		// test comment after container-name
		assert(zplSpecials.pathExists("meta/sub"));
	}	

	
	@After
	public void cleanup() {
		TestUtils.cleanupDir(TEST_FOLDER);
	}
	
}
