package uk.gov.homeoffice.pontus.logback;


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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import net.openhft.chronicle.logger.logback.BinaryVanillaChronicleAppender;

import net.openhft.chronicle.logger.ChronicleLogWriter;

import java.lang.management.ManagementFactory;

public class PontusLogbackAppender extends BinaryVanillaChronicleAppender {
    public static final String uRunning = ManagementFactory.getRuntimeMXBean().getName() + "#";


    public PontusLogbackAppender() {
        super();
    }

    @Override
    public void doAppend(final ILoggingEvent event, final ChronicleLogWriter writer) {
        final ThrowableProxy tp = (ThrowableProxy) event.getThrowableProxy();

//        try {
            writer.write(
                    toChronicleLogLevel(event.getLevel()),
                    event.getTimeStamp(),
                    event.getThreadName(),
                    event.getLoggerName(),
//                event.getMessage(),
                    uRunning.concat(event.getFormattedMessage()),
                    tp != null ? tp.getThrowable() : null,
                    null

            );

//            writer.write(
//                    toChronicleLogLevel(event.getLevel()),
//                    event.getTimeStamp(),
//                    event.getThreadName(),
//                    event.getLoggerName(),
////                event.getMessage(),
//                    uRunning.concat(event.getMessage()),
//                    tp != null ? tp.getThrowable() : null,
//                    event.getArgumentArray()
//            );
//        }catch(Throwable t)
//        {
//            writer.write(
//                    toChronicleLogLevel(event.getLevel()),
//                    event.getTimeStamp(),
//                    event.getThreadName(),
//                    event.getLoggerName(),
////                event.getMessage(),
//                    uRunning.concat(event.getFormattedMessage())
//
//            );
////           t.printStackTrace();
//        }
    }
}
