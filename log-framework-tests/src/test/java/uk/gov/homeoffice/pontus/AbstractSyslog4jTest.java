package uk.gov.homeoffice.pontus;

import org.graylog2.syslog4j.Syslog;
import org.graylog2.syslog4j.server.SyslogServer;
import org.graylog2.syslog4j.server.SyslogServerEventIF;
import org.graylog2.syslog4j.server.SyslogServerIF;
import org.graylog2.syslog4j.server.SyslogServerSessionEventHandlerIF;
import org.graylog2.syslog4j.server.impl.net.AbstractNetSyslogServerConfig;
import org.graylog2.syslog4j.util.SyslogUtility;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractSyslog4jTest {

    public static final int TEST_PORT = 10514;

    protected SyslogServerIF server = null;
    protected List<String> recordedEvents = new ArrayList<>();
    protected List<String> rawRecordedEvents = new ArrayList<>();
    protected SyslogServerSessionEventHandlerIF recorderEventHandler = new SyslogServerSessionEventHandlerIF() {
        private static final long serialVersionUID = 8040266564168724L;

        public void initialize(SyslogServerIF syslogServer) {
        }

        public Object sessionOpened(SyslogServerIF syslogServer, SocketAddress socketAddress) {
            return null;
        }

        @Override
        public void event(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress, SyslogServerEventIF event) {
            String recordedEvent = SyslogUtility.newString(syslogServer.getConfig(), event.getRaw());
            rawRecordedEvents.add(recordedEvent);
            recordedEvent = recordedEvent.substring(recordedEvent.toUpperCase().indexOf("[TEST]"));
            recordedEvents.add(recordedEvent);
        }

        @Override
        public void exception(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress, Exception exception) {
            fail(exception.toString());
        }

        @Override
        public void sessionClosed(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress, boolean timeout) {
        }

        @Override
        public void destroy(SyslogServerIF syslogServer) {
        }
    };

    protected abstract String getServerProtocol();

    protected void startServerThread(String protocol) {
        this.server = SyslogServer.getInstance(protocol);

        AbstractNetSyslogServerConfig config = (AbstractNetSyslogServerConfig) this.server.getConfig();
        config.setPort(TEST_PORT);
        config.addEventHandler(this.recorderEventHandler);

        this.server = SyslogServer.getThreadedInstance(protocol);
        SyslogUtility.sleep(1000);
    }

    public void setUp() {
        recordedEvents.clear();
        rawRecordedEvents.clear();

        String protocol = getServerProtocol();
        startServerThread(protocol);
        SyslogUtility.sleep(100);
    }

    protected void verifyStringInRawEvent(String str) {
        boolean contains = false;
        for (String evnt : rawRecordedEvents) {
            if (evnt.contains(str)) {
                contains = true;
            }
        }
        assertTrue(contains);
    }


    protected void verifySendReceive(List events, boolean sort) {
        if (sort) {
            Collections.sort(events);
        }

        if (sort) {
            Collections.sort(recordedEvents);
        }

        for (int i = 0; i < events.size(); i++) {
            String sentEvent = (String) events.get(i);
            String recordedEvent = recordedEvents.get(i);

            if (!sentEvent.equals(recordedEvent)) {
                System.out.println("SENT: " + sentEvent);
                System.out.println("RCVD: " + recordedEvent);

                fail("Sent and recorded events do not match");
            }
        }
    }

    public void tearDown() {
        recordedEvents.clear();
        rawRecordedEvents.clear();

        Syslog.shutdown();
        SyslogUtility.sleep(100);
        SyslogServer.shutdown();
        SyslogUtility.sleep(100);
        Syslog.initialize();
        SyslogServer.initialize();
    }
}
