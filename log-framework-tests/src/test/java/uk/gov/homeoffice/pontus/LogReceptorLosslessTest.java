package uk.gov.homeoffice.pontus;

import org.graylog2.syslog4j.util.SyslogUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class LogReceptorLosslessTest extends AbstractSyslog4jTest {

    @Override
    protected String getServerProtocol() {
        return "tcp";
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
    public void testLogReceptorLossless() {
        List<String> events = new ArrayList<>();

        int i = 0;
        String message = "[TEST] " + i + " / " + System.currentTimeMillis();
        String syslogInstanceName = "instance1";
        String defaultMsgHostName = "myhost2.homeoffice.gsi.gov.uk";
        String defaultAppName = "APP2";
        int defaultFacility = SyslogUtility.FACILITY_USER;
        String syslogServerHostName = "localhost";
        String procId = "17375";

        LogReceptor lossless = new LogReceptorLossless(syslogInstanceName, procId, defaultMsgHostName, defaultAppName,
                defaultFacility, syslogServerHostName, TEST_PORT);

        try {
            lossless.process("msgid2", LogReceptor.Severity.ALERT, null, message);
            lossless.flush();
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        events.add(message);

        SyslogUtility.sleep(1000);

        verifySendReceive(events, true);
        verifyStringInRawEvent(procId);
        verifyStringInRawEvent("myhost2.homeoffice.gsi.gov.uk APP2 17375 msgid2");
    }
}