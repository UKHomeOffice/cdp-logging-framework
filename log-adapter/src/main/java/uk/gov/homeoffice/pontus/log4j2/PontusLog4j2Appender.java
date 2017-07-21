package uk.gov.homeoffice.pontus.log4j2;

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
import net.openhft.chronicle.logger.ChronicleLogWriter;
import net.openhft.chronicle.logger.VanillaLogAppenderConfig;
import net.openhft.lang.model.constraints.NotNull;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.lang.management.ManagementFactory;

@Plugin(
        name = "PontusLog4j2Appender",
        category = "Core",
        elementType = "appender",
        printObject = true)
public class PontusLog4j2Appender extends net.openhft.chronicle.logger.log4j2.BinaryVanillaChronicleAppender {

    public static final String uRunning = ManagementFactory.getRuntimeMXBean().getName()+"#";;



    public PontusLog4j2Appender(
            final String name, final Filter filter, final String path, final VanillaLogAppenderConfig config) {
        super(name, filter, path, config);
    }

    @PluginFactory
    public static PontusLog4j2Appender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("path") final String path,
            @PluginAttribute("includeCallerData") final String includeCallerData,
            @PluginAttribute("includeMappedDiagnosticContext") final String includeMappedDiagnosticContext,
            @PluginElement("vanillaChronicleConfig") final VanillaChronicleCfg chronicleConfig,
            @PluginElement("filter") final Filter filter) {
        if (name == null) {
            LOGGER.error("No name provided for PontusLog4j1Appender");
            return null;
        }

        if (path == null) {
            LOGGER.error("No path provided for PontusLog4j1Appender");
            return null;
        }

        final PontusLog4j2Appender appender = new PontusLog4j2Appender(name, filter, path,
                chronicleConfig);

        if (includeCallerData != null) {
            appender.setIncludeCallerData("true".equalsIgnoreCase(includeCallerData));
        }
        if (includeMappedDiagnosticContext != null) {
            appender.setIncludeMappedDiagnosticContext("true".equalsIgnoreCase(includeMappedDiagnosticContext));
        }
        return appender;
    }

    @Override
    public void doAppend(@NotNull final LogEvent event, @NotNull final ChronicleLogWriter writer) {
        writer.write(
                toChronicleLogLevel(event.getLevel()),
                event.getTimeMillis(),
                event.getThreadName(),
                event.getLoggerName(),
                uRunning.concat(event.getMessage().getFormat()),
                event.getThrown(),
                event.getMessage().getParameters()
        );
    }
}

