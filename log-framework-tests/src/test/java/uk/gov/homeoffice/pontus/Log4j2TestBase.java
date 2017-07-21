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

package uk.gov.homeoffice.pontus;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ChronicleQueueBuilder;
import net.openhft.chronicle.logger.ChronicleLogLevel;
import net.openhft.lang.io.Bytes;
import net.openhft.lang.io.serialization.BytesMarshallable;
import net.openhft.lang.model.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class Log4j2TestBase extends AbstractSyslog4jTest {
    protected static final ChronicleLogLevel[] LOG_LEVELS = ChronicleLogLevel.values();

    static {
        // LPPM - THIS IS NEEDED to run some of these tesets on Windows; there is an assertion
        // on VanillaChronicle.java that fails on Windows:
        // assert (appenderThreadId & THREAD_ID_MASK) == appenderThreadId : "appenderThreadId: " + appenderThreadId;

        System.getProperties().setProperty("os.max.pid.bits", "32");
    }

    protected static String rootPath() {
        String path = System.getProperty("java.io.tmpdir");
        String sep = System.getProperty("file.separator");

        if (!path.endsWith(sep)) {
            path += sep;
        }

        return path + "chronicle-log4j2";
    }

    protected static String basePath(String type) {
        return rootPath()
                + File.separator
                + type;
    }

    protected static void log(Logger logger, ChronicleLogLevel level, String fmt, Object... args) {
        switch (level) {
            case TRACE:
                logger.trace(fmt, args);
                break;
            case DEBUG:
                logger.debug(fmt, args);
                break;
            case INFO:
                logger.info(fmt, args);
                break;
            case WARN:
                logger.warn(fmt, args);
                break;
            case ERROR:
                logger.error(fmt, args);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * @param type
     * @return
     */
    protected Chronicle getVanillaChronicle(String type) throws IOException {
        return ChronicleQueueBuilder.vanilla(basePath(type)).build();
    }

    @Override
    protected String getServerProtocol() {
        return null;
    }
}
