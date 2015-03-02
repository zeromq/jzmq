package org.zeromq;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Exercises basic manipulations in the EmbeddedLibraryTools for JNI loading.
 * Does not give full coverage yet as this has to be per-platform.
 */
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
        setOsProperties("Mac OS X", "x86_64");
        assertPlatformIdentifierEquals("x86_64/Darwin");
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