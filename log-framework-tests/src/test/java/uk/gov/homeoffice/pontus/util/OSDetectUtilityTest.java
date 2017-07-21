package uk.gov.homeoffice.pontus.util;

import junit.framework.TestCase;
import org.graylog2.syslog4j.util.OSDetectUtility;

public class OSDetectUtilityTest extends TestCase {
    public void testOSDetectUtility() {
        boolean unix = OSDetectUtility.isUnix();
        boolean windows = OSDetectUtility.isWindows();

        assertTrue(unix ^ windows);
    }
}
