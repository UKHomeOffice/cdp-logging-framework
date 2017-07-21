package uk.gov.homeoffice.pontus;

import org.graylog2.syslog4j.util.SyslogUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class LogReceptorLossyTest extends AbstractSyslog4jTest {

    @Override
    protected String getServerProtocol() {
        return "udp";
    }

    @Before
    @Override
    public void setUp() {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }


    @Test
    public void testLogReceptorLossy() {
        List<String> events = new ArrayList<>();

        int i = 0;
        String message = "[TEST] " + i + " / " + System.currentTimeMillis();
        String syslogInstanceName = "instance1";
        String defaultMsgHostName = "myhost.homeoffice.gsi.gov.uk";
        String defaultAppName = "APP1";
        int defaultFacility = SyslogUtility.FACILITY_USER;
        String syslogServerHostName = "localhost";
        String procId = "17374";

        LogReceptor lossy = new LogReceptorLossy(100, syslogInstanceName, procId, defaultMsgHostName, defaultAppName,
                defaultFacility, syslogServerHostName, TEST_PORT);

        try {
            lossy.process("msgid1", LogReceptor.Severity.ALERT, null, message);
            lossy.flush();
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        events.add(message);

        SyslogUtility.sleep(1000);
        verifySendReceive(events, true);
        verifyStringInRawEvent(procId);
        verifyStringInRawEvent("myhost.homeoffice.gsi.gov.uk APP1 17374 msgid1");
    }
}