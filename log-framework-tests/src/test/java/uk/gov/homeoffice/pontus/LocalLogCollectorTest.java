package uk.gov.homeoffice.pontus;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptTailer;
import net.openhft.chronicle.logger.ChronicleLogEvent;
import net.openhft.chronicle.logger.ChronicleLogHelper;
import net.openhft.chronicle.logger.ChronicleLogLevel;
import net.openhft.lang.io.IOTools;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;

@PrepareForTest(LocalLogCollector.class)
@RunWith(PowerMockRunner.class)
public class LocalLogCollectorTest extends Log4j2TestBase {

    @Test
    public void testLocalCollector() throws Exception {
        final String testId = "binary-vanilla-chronicle";
        final String threadId = testId + "-th";
        final Logger logger = LoggerFactory.getLogger(testId);
        Thread.currentThread().setName(threadId);

        Chronicle chronicle = getVanillaChronicle(testId);
        ExcerptTailer tailer = chronicle.createTailer().toStart();
        ChronicleLogEvent event;

        for (ChronicleLogLevel level : LOG_LEVELS) {
            log(logger, level, "level is {}", level);
        }

        tailer.nextIndex();
        event = ChronicleLogHelper.decodeBinary(tailer);

        PowerMockito.spy(LocalLogCollector.class);
        String pid = LocalLogCollector.getPidFromEvent(event);
        PowerMockito.doReturn(new Pair<>("jbloggs", "jbloggsgrp")).when(LocalLogCollector.class, "extractOwnerAndGroupFromPid", pid);
        StringWriter writer = new StringWriter();
        LocalLogCollector.asString(event, writer);
        Assert.assertTrue(writer.toString().contains("jbloggs"));
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

        for (ExcerptTailer tailer : tailers) {
            for (int i = 0; i < 5; i++) {
                tailer.nextIndex();
                ChronicleLogEvent event = ChronicleLogHelper.decodeBinary(tailer);
                String formattedMessage = MessageFormatter.arrayFormat(event.getMessage(),
                        event.getArgumentArray()).getMessage();
                Assert.assertTrue(formattedMessage.contains(Integer.toString(i)));
            }
        }
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
                log(logger, ChronicleLogLevel.ERROR, "Message number {} produced from log in thread {}",
                        Integer.toString(i), this.loggerName);
            }
        }
    }
}
