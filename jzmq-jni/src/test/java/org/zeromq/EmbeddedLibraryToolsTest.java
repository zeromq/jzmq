package org.zeromq;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.newCapture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.*;

/**
 * Exercises basic manipulations in the EmbeddedLibraryTools for JNI loading.
 * Does not give full coverage yet as this has to be per-platform.
 */
@RunWith(PowerMockRunner.class)
public class EmbeddedLibraryToolsTest {
    private static final String origOsName = System.getProperty("os.name");

    private static final String origOsArch = System.getProperty("os.arch");


    @Test
    public void rewriteWindowsXP() {
        setOsProperties("Windows XP", "x86");
        assertPlatformIdentifierEquals("x86/Windows");
    }

    @Test
    public void rewriteWindows8() {
        // Windows 8 reported as 6.2 (http://bugs.java.com/view_bug.do?bug_id=7170169)
        setOsProperties("Windows 6.2", "x86");
        assertPlatformIdentifierEquals("x86/Windows");
    }

    @Test
    public void rewriteMacOSX() {
        setOsProperties("mac os x", "x86_64");
        assertPlatformIdentifierEquals("x86_64/Mac OS X");
    }

    @Test
    public void passThroughLinux32Bit() {
        setOsProperties("Linux", "amd64");
        assertPlatformIdentifierEquals("amd64/Linux");
    }

    @Test
    public void rewriteSpacesInPath() {
        setOsProperties("Digital Unix", "alpha");
        assertPlatformIdentifierEquals("alpha/Digital_Unix");
    }

    @Test
    @PrepareForTest(EmbeddedLibraryTools.class)
    public void multiLibLoad() throws Exception {
        mockStaticPartial(System.class, "load");
        Capture<String> loadCapture = newCapture(CaptureType.ALL);
        System.load(capture(loadCapture));
        expectLastCall().times(2);
        replayAll();
        setOsProperties("Linux", "testarch");
        assertTrue(EmbeddedLibraryTools.LOADED_EMBEDDED_LIBRARY);
        verifyAll();
        assertTrue(loadCapture.getValues().get(0).contains("libzmq"));
        assertTrue(loadCapture.getValues().get(1).contains("libjzmq"));
    }

    @Test
    @PrepareForTest(EmbeddedLibraryTools.class)
    public void libsAsProp() throws Exception {
        try {
            System.setProperty("jzmq.libs", "foo,libjzmq");
            mockStaticPartial(System.class, "load");
            Capture<String> loadCapture = newCapture(CaptureType.ALL);
            System.load(capture(loadCapture));
            expectLastCall().times(2);
            replayAll();
            setOsProperties("Linux", "testarch");
            assertTrue(EmbeddedLibraryTools.LOADED_EMBEDDED_LIBRARY);
            verifyAll();
            assertTrue(loadCapture.getValues().get(0).contains("foo"));
            assertTrue(loadCapture.getValues().get(1).contains("libjzmq"));
        } finally {
            System.clearProperty("jzmq.libs");
        }
    }


    @After
    public void resetOsProperties() {
        setOsProperties(origOsName, origOsArch);
    }

    private void assertPlatformIdentifierEquals(String expected) {
        String id = EmbeddedLibraryTools.getCurrentPlatformIdentifier();
        assertEquals(expected, id);
    }

    private void setOsProperties(String osName, String osArch) {
        System.setProperty("os.name", osName);
        System.setProperty("os.arch", osArch);
    }
}
