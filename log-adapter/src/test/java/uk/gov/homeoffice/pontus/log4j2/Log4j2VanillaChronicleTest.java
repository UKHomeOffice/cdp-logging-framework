/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.homeoffice.pontus.log4j2;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptTailer;
import net.openhft.chronicle.logger.ChronicleLog;
import net.openhft.chronicle.logger.ChronicleLogEvent;
import net.openhft.chronicle.logger.ChronicleLogHelper;
import net.openhft.chronicle.logger.ChronicleLogLevel;
import net.openhft.lang.io.IOTools;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.*;

public class Log4j2VanillaChronicleTest extends Log4j2TestBase {

    @Test
    public void testVanillaAppender() throws IOException {
        final String testId = "binary-vanilla-chronicle";
        final String threadId = testId + "-th";
        final long timestamp = System.currentTimeMillis();

        IOTools.deleteDir(basePath(testId));
        final Logger logger = LoggerFactory.getLogger(testId);

        Thread.currentThread().setName(threadId);

        for (ChronicleLogLevel level : LOG_LEVELS) {
            log(logger, level, "level is {}", level);
        }

        Chronicle chronicle = getVanillaChronicle(testId);
        ExcerptTailer tailer = chronicle.createTailer().toStart();
        ChronicleLogEvent evt;

        for (ChronicleLogLevel level : LOG_LEVELS) {
            assertTrue(tailer.nextIndex());
            evt = ChronicleLogHelper.decodeBinary(tailer);
            assertNotNull(evt);
            assertEquals(evt.getVersion(), ChronicleLog.VERSION);
            assertTrue(evt.getTimeStamp() >= timestamp);
            assertEquals(level, evt.getLevel());
            assertEquals(threadId, evt.getThreadName());
            assertEquals(testId, evt.getLoggerName());
            assertEquals(PontusLog4j2Appender.uRunning.concat("level is {}"), evt.getMessage());
            assertNotNull(evt.getArgumentArray());
            assertEquals(1, evt.getArgumentArray().length);
            assertEquals(level, evt.getArgumentArray()[0]);

            tailer.finish();
        }

        logger.debug("Throwable test 1", new UnsupportedOperationException());
        logger.debug("Throwable test 2", new UnsupportedOperationException("Exception message"));

        assertTrue(tailer.nextIndex());
        evt = ChronicleLogHelper.decodeBinary(tailer);
        assertEquals(PontusLog4j2Appender.uRunning.concat("Throwable test 1"), evt.getMessage());
        assertNotNull(evt.getThrowable());
        assertTrue(evt.getThrowable() instanceof UnsupportedOperationException);
        assertNull(evt.getThrowable().getMessage());

        assertTrue(tailer.nextIndex());
        evt = ChronicleLogHelper.decodeBinary(tailer);
        assertEquals(PontusLog4j2Appender.uRunning.concat("Throwable test 2"), evt.getMessage());
        assertNotNull(evt.getThrowable());
        assertTrue(evt.getThrowable() instanceof UnsupportedOperationException);
        assertEquals("Exception message", evt.getThrowable().getMessage());

        tailer.close();
        chronicle.close();

        IOTools.deleteDir(basePath(testId));
    }
}
