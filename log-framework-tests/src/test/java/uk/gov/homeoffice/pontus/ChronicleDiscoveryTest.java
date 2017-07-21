package uk.gov.homeoffice.pontus;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptTailer;
import net.openhft.chronicle.logger.ChronicleLogEvent;
import net.openhft.chronicle.logger.ChronicleLogHelper;
import net.openhft.chronicle.logger.ChronicleLogLevel;
import net.openhft.lang.io.IOTools;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class ChronicleDiscoveryTest extends Log4j2TestBase {

    private void checkTailer(ExcerptTailer tailer1) {
        boolean hasMoreData = false;
        boolean firstTime = true;
        do {
            hasMoreData = tailer1.nextIndex();

            if (firstTime) {
                assertTrue(hasMoreData);
                firstTime = false;
            }
            if (hasMoreData) {
                ChronicleLogEvent evt1 = ChronicleLogHelper.decodeBinary(tailer1);
                StringWriter writer = new StringWriter();
                LocalLogCollector.asString(evt1, writer);
                assertTrue(writer.toString().length() > 0);
            }
//        String user = ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
//        assertTrue(writer.toString().contains(user));
        } while (hasMoreData);

    }

    @Test
    public void getTailers() throws Exception {

        final String testId = "binary-vanilla-chronicle-disc-test";

        final String testId1 = testId + 1;
        final String testId2 = testId + 2;
        final String testId3 = testId + 3;
        final String threadId = testId + "-th";


        final long timestamp = System.currentTimeMillis();
        IOTools.deleteDir(basePath(testId1));
        IOTools.deleteDir(basePath(testId2));
        IOTools.deleteDir(basePath(testId3));

        final Logger logger1 = LoggerFactory.getLogger(testId1);
        final Logger logger2 = LoggerFactory.getLogger(testId2);
        final Logger logger3 = LoggerFactory.getLogger(testId3);

        Thread.currentThread().setName(threadId);

        for (ChronicleLogLevel level : LOG_LEVELS) {
            log(logger1, level, "level1 is {}", level);
            log(logger2, level, "level2 is {}", level);
            log(logger3, level, "level3 is {}", level);
        }

        ArrayList<Chronicle> chronicles = new ArrayList<>();
        ArrayList<ExcerptTailer> tailers = new ArrayList<>();


        ChronicleDiscovery.getTailers(chronicles, tailers, Paths.get(rootPath()), Pattern.compile(testId + ".*"));

        assertTrue(chronicles.size() > 0);
        assertTrue(chronicles.size() == tailers.size());
//
//    for (ChronicleLogLevel level : LOG_LEVELS) {
//      log(logger1, level, "level1 is {}", level);
//      log(logger2, level, "level2 is {}", level);
//      log(logger3, level, "level3 is {}", level);
//    }

        ArrayList<Chronicle> chronicles2 = new ArrayList<>();
        ArrayList<ExcerptTailer> tailers2 = new ArrayList<>();


        ChronicleDiscovery.getTailers(chronicles2, tailers2, Paths.get(rootPath()));

        assertTrue(chronicles2.size() >= chronicles.size());
        assertTrue(chronicles2.size() == tailers2.size());

        for (int i = 0, ilen = chronicles.size(); i < ilen; i++) {
            Chronicle chronicle1 = chronicles.get(i);
            ExcerptTailer tailer1 = tailers.get(i);
            checkTailer(tailer1);
        }


    }
//
//  @Test
//  public void getTailersSingleMatch() throws Exception {
//
//    final String testId = "binary-vanilla-chronicle";
//
//    final String testId1 = testId;
//    final String threadId = testId + "-th";
//    Thread.currentThread().setName(threadId);
//
//    IOTools.deleteDir(basePath(testId1));
//
//    final long timestamp = System.currentTimeMillis();
//
//    final Logger logger1 = LoggerFactory.getLogger(testId1);
//    for (ChronicleLogLevel level : LOG_LEVELS) {
//      log(logger1, level, "level1 is {}", level);
//    }
//    ArrayList<Chronicle> chronicles = new ArrayList<>();
//    ArrayList<ExcerptTailer> tailers = new ArrayList<>();
//
//    ChronicleDiscovery.getTailers(chronicles, tailers, Paths.get(rootPath()), Pattern.compile(testId));
//
//    assertTrue(chronicles.size() > 0);
//    assertTrue(chronicles.size() == tailers.size());
//
//
//    for (int i = 0, ilen = chronicles.size(); i < ilen; i++) {
//      Chronicle chronicle1 = chronicles.get(i);
//
////      Chronicle c2 = getVanillaChronicle(testId1);
////      ExcerptTailer et2 = c2.createTailer();
////      assertTrue(et2.nextIndex());
//      ExcerptTailer tailer1 = tailers.get(i);
////      assertTrue(tailer1.nextIndex());
//
//      checkTailer(tailer1);
//
//    }
//
//  }
}