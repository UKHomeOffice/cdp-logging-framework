package uk.gov.homeoffice.pontus;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptTailer;
import net.openhft.chronicle.logger.ChronicleLogEvent;
import net.openhft.chronicle.logger.ChronicleLogHelper;
import net.openhft.chronicle.logger.ChronicleLogLevel;
import net.openhft.lang.io.IOTools;
import org.apache.commons.collections.map.LRUMap;
import org.graylog2.syslog4j.util.SyslogUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

@PrepareForTest(EndToEndTests.class)
@RunWith(PowerMockRunner.class)
public class EndToEndTests extends Log4j2TestBase {

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
    public void testParallelLocalCollector() throws Exception {
        IOTools.deleteDir(basePath("binary-vanilla-chronicle-async-1"));
        IOTools.deleteDir(basePath("binary-vanilla-chronicle-async-2"));
        Thread.sleep(1000);

        Thread.currentThread().setName("parent-id");

        Thread firstLogger = new Thread(new AsyncLogger("binary-vanilla-chronicle-async-1"));
        Thread secondLogger = new Thread(new AsyncLogger("binary-vanilla-chronicle-async-2"));
        firstLogger.start();
        secondLogger.start();
        firstLogger.join();
        secondLogger.join();

        ArrayList<Chronicle> chronicles = new ArrayList<>();
        ArrayList<ExcerptTailer> tailers = new ArrayList<>();

        Pattern logPattern = Pattern.compile("binary-vanilla-chronicle-async-.*");
        ChronicleDiscovery.getTailers(chronicles, tailers, Paths.get(rootPath()), logPattern);

        String defaultMsgHostName = "myhost3.homeoffice.gsi.gov.uk";
        String defaultAppName = "APP3";
        int defaultFacility = SyslogUtility.FACILITY_USER;
        String syslogServerHostName = "localhost";

        LRUMap logReceptors = new LRUMap(100);

        List<String> messagesSent = new ArrayList<>();

        for (ExcerptTailer tailer : tailers) {
            for (int i = 0; i < 5; i++) {
                tailer.nextIndex();
                ChronicleLogEvent event = ChronicleLogHelper.decodeBinary(tailer);
                String msg = event.getMessage();
                String pid = msg.substring(0, msg.indexOf('@'));
                LogReceptor lr = (LogReceptor) logReceptors.get(pid);
                if (lr == null) {
                    lr = new LogReceptorLossless(pid, pid, defaultMsgHostName, defaultAppName, defaultFacility, syslogServerHostName, TEST_PORT);
                    logReceptors.put(pid, lr);
                    SyslogUtility.sleep(100);
                }
                String formattedMessage = MessageFormatter.arrayFormat(msg.substring(msg.indexOf('#') + 1),
                        event.getArgumentArray()).getMessage();

                LogReceptor.Severity sev = LocalLogCollector.getSeverity(event.getLevel());
                lr.process(event.getLoggerName(), sev, null, formattedMessage);
                messagesSent.add(formattedMessage.substring(formattedMessage.indexOf("[TEST]")));
                assertTrue(formattedMessage.contains(Integer.toString(i)));
            }
        }
        SyslogUtility.sleep(10000);
        verifySendReceive(messagesSent, false);
    }

    @Test
    public void testProcessor() throws Exception {
        IOTools.deleteDir(basePath("binary-vanilla-chronicle-async-1"));
        IOTools.deleteDir(basePath("binary-vanilla-chronicle-async-2"));
        Thread.sleep(1000);

        Thread.currentThread().setName("parent-id");

        Thread firstLogger = new Thread(new AsyncLogger("binary-vanilla-chronicle-async-1"));
        Thread secondLogger = new Thread(new AsyncLogger("binary-vanilla-chronicle-async-2"));
        firstLogger.start();
        secondLogger.start();
        firstLogger.join();
        secondLogger.join();

        String doubleSlashRootDir = rootPath().replaceAll(Pattern.quote("\\"), "\\\\\\\\");

        try (PrintWriter out = new PrintWriter("filename.txt")) {
            out.println("logPatternRegex: \"binary-vanilla-chronicle-async-.*\"");
            out.println("basePath: \"" + doubleSlashRootDir + "\"");
            out.println("receptorInfoList:");
            out.println("  -");
            out.println("    regex: .*");
            out.println("    type: LogReceptorLossless");
            out.println("    syslogHostName: localhost");
            out.println("    syslogPort: " + TEST_PORT);
        }

        LocalLogCollectorConfig conf = LocalLogCollector.getLocalLogCollectorConfigFromYamlFile(new File("filename.txt"));

        assertEquals(rootPath(), (conf.basePath));
        assertEquals("binary-vanilla-chronicle-async-.*", (conf.logPatternRegex));

        assertTrue(conf.receptorInfoList.length > 0);
        assertEquals(".*", (conf.receptorInfoList[0].getRegex()));
        assertEquals("LogReceptorLossless", (conf.receptorInfoList[0].getType()));
        assertEquals("localhost", (conf.receptorInfoList[0].getSyslogHostName()));
        assertEquals((TEST_PORT), (conf.receptorInfoList[0].getSyslogPort()));

        ArrayList<Chronicle> chronicles = new ArrayList<>();
        ArrayList<ExcerptTailer> tailers = new ArrayList<>();

        Pattern logPattern = Pattern.compile(conf.logPatternRegex);

        SyslogUtility.sleep(2000);

        ChronicleDiscovery.checkForNewFiles(Paths.get(conf.basePath), logPattern);
        try {
            LocalLogCollector.processLogsNoLoop(chronicles, tailers, conf, logPattern);
        } catch (Exception e) {
            fail(e.toString());
        }
        SyslogUtility.sleep(2000);

        assertTrue(super.rawRecordedEvents.size() > 0);
    }


    /**
     * Logger name and thread have the same ID. Good idea? Does it mean anything?
     */
    class AsyncLogger implements Runnable {

        private final String loggerName;

        public AsyncLogger(String loggerName) {
            this.loggerName = loggerName;
        }

        @Override
        public void run() {
            final Logger logger = LoggerFactory.getLogger(this.loggerName);
            Thread.currentThread().setName(this.loggerName);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 5; i++) {
                log(logger, ChronicleLogLevel.ERROR, "[TEST]Message number {} produced from log in thread {}",
                        Integer.toString(i), this.loggerName);
            }
        }
    }
}
