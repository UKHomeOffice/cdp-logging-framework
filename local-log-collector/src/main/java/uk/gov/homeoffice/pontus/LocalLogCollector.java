package uk.gov.homeoffice.pontus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptTailer;
import net.openhft.chronicle.logger.ChronicleLog;
import net.openhft.chronicle.logger.ChronicleLogEvent;
import net.openhft.chronicle.logger.ChronicleLogHelper;
import net.openhft.chronicle.logger.ChronicleLogLevel;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Pair;
import org.graylog2.syslog4j.util.SyslogUtility;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Created by leo on 02/05/2017.
 */
public class LocalLogCollector {

    private static final LRUMap logReceptors = new LRUMap(100);
    private static final LRUMap uRunningMap = new LRUMap(100);
    private static final DateFormat DF = new SimpleDateFormat(ChronicleLog.DEFAULT_DATE_FORMAT);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    public static StringWriter asString(final ChronicleLogEvent event, final StringWriter writer) {
        writer.append(DF.format(event.getTimeStamp()));
        writer.append("|");
        writer.append(event.getLevel().toString());
        writer.append("|");
        writer.append(event.getThreadName());
        writer.append("|");
        writer.append(event.getLoggerName());
        writer.append("|");

        String pid = getPidFromEvent(event);
        Pair<String, String> ownerAndGroup = extractOwnerAndGroupFromPid(pid);
        writer.append(ownerAndGroup.getFirst());
        writer.append("|");

        writer.append(event.getMessage());
        writer.append("|");

        Object[] args = event.getArgumentArray();
        if (args != null && args.length > 0) {
            writer.append("|args {");
            for (int i = 0; i < args.length; i++) {
                writer.append(Objects.toString(args[i]));
                if (i != args.length - 1) {
                    writer.append(", ");
                }
            }
            writer.append("}");
        }

        final Throwable th = event.getThrowable();
        if (th != null) {
            writer.append("|exception: ");
            writer.append(th.toString());
            writer.append("\n");
            th.printStackTrace(new PrintWriter(writer));
        }

        return writer;
    }
    private static Pair<String, String> extractOwnerAndGroupFromPid(String pid) {
        try {
            Path path = new File("/proc/".concat(pid)).toPath();
            UserPrincipal owner = Files.getOwner(path);
            GroupPrincipal group = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
            return new Pair<>(owner.getName(), group.getName());
        } catch (IOException e) {
            e.printStackTrace();
            return new Pair<>("UNKNOWN", "UNKNOWN");
        }
    }

    protected static String rootPath() {
        String path = System.getProperty("java.io.tmpdir");
        String sep = System.getProperty("file.separator");

        if (!path.endsWith(sep)) {
            path += sep;
        }

        return path + "chronicle-log4j2";
    }

    public static LogReceptor.Severity getSeverity(ChronicleLogLevel chronicleLogLevel) {
        switch (chronicleLogLevel) {
            case DEBUG:
                return LogReceptor.Severity.DEBUG;
            case ERROR:
                return LogReceptor.Severity.ERROR;
            case INFO:
                return LogReceptor.Severity.INFO;
            case WARN:
                return LogReceptor.Severity.WARN;
            case TRACE:
                return LogReceptor.Severity.DEBUG;
        }
        return LogReceptor.Severity.EMERGENCY;
    }

    public static LocalLogCollectorConfig getLocalLogCollectorConfigFromYamlFile(File configFile) throws IOException {
        return OBJECT_MAPPER.readValue(configFile, LocalLogCollectorConfig.class);
    }

    public static LogReceptorInfo getLogReceptorInfoFromList(String loggerName, LocalLogCollectorConfig infoList) {
        for (int i = 0, ilen = infoList.receptorInfoList.length; i < ilen; i++) {
            LogReceptorInfo info = infoList.receptorInfoList[i];
            if (Pattern.compile(info.getRegex()).matcher(loggerName).matches()) {
                return info;
            }
        }
        return null;
    }

    public static String getAppNameFromLoggerName(String loggerName) {
        int delimiter = loggerName.indexOf("__");
        if (loggerName.indexOf("__") > 0) {
            return loggerName.substring(0, delimiter);
        }
        return "Pontus";
    }

    public static LogReceptor getLogReceptor(LocalLogCollectorConfig infoList, ChronicleLogEvent event, String pid)
            throws UnknownHostException {
        String key = event.getLoggerName().concat(pid);
        LogReceptor lr = (LogReceptor) logReceptors.get(key);
        if (lr == null) {
            String defaultMsgHostName = InetAddress.getLocalHost().getHostName();
            String loggerName = event.getLoggerName();
            LogReceptorInfo lri = getLogReceptorInfoFromList(loggerName, infoList);
            int defaultFacility = lri.getFacility();
            String defaultAppName = getAppNameFromLoggerName(loggerName);
            String syslogServerHostName = lri.getSyslogHostName();
            int port = lri.getSyslogPort();
            switch (lri.getType()) {
                case "LogReceptorLossless":
                    lr = new LogReceptorLossless(key, pid, defaultMsgHostName, defaultAppName, defaultFacility,
                            syslogServerHostName, port);
                    break;
                case "LogReceptorLossy":
                    lr = new LogReceptorLossy(100, key, pid, defaultMsgHostName, defaultAppName, defaultFacility,
                            syslogServerHostName, port);
                    break;
                case "LogReceptorEncrypted":
                    lr = new LogReceptorEncrypted(lri.getKeyStore(), lri.getTrustStore(),
                            lri.getKeyStorePasswd(), lri.getTrustStorePasswd(),
                            key, pid, defaultMsgHostName, defaultAppName, defaultFacility, syslogServerHostName, port);
                    break;
                default:
                    lr = new LogReceptorLossless(key, pid, defaultMsgHostName, defaultAppName, defaultFacility,
                            syslogServerHostName, port);
                    break;
            }
            logReceptors.put(key, lr);
        }
        return lr;

    }

    public static Map<String, Map<String, String>> getProps(String pid) {
        Map<String, Map<String, String>> retVal = (Map<String, Map<String, String>>) uRunningMap.get(pid);

        if (retVal == null) {
            retVal = new HashMap<>();
            Map<String, String> innerMap = new HashMap<>();
            Pair<String, String> ownerAndGroup = extractOwnerAndGroupFromPid(pid);

            String URUNNING_DN_SUFFIX = "DC\\=homeoffice,DC\\=GSI,DC\\=GOV,DC\\=UK";
            String strBuild = "UID\\=" + ownerAndGroup.getFirst() +
                    ",OU\\=" +
                    ownerAndGroup.getSecond() +
                    URUNNING_DN_SUFFIX;
            innerMap.put("uRunning", strBuild);
            String PEN = "12345";
            retVal.put("UserInfo@".concat(PEN), innerMap);
        }
        return retVal;
    }

    public static void checkForNewChronicles(ArrayList<Chronicle> chronicles, ArrayList<ExcerptTailer> tailers, Pattern logPattern) throws IOException {
        if (ChronicleDiscovery.checkNewChronicles()) {
            for (ExcerptTailer tailer : tailers) {
                tailer.close();
            }
            for (Chronicle chronicle : chronicles) {
                chronicle.close();
            }
            chronicles.clear();
            tailers.clear();
            ChronicleDiscovery.getTailers(chronicles, tailers, Paths.get(rootPath()), logPattern);
        }
    }

    public static void processLogsNoLoop(ArrayList<Chronicle> chronicles, ArrayList<ExcerptTailer> tailers, LocalLogCollectorConfig config, Pattern logPattern) throws IOException {
        checkForNewChronicles(chronicles, tailers, logPattern);
        long recheckCounter = 60000;
        for (int j = 0, jlen = 500; j < jlen && recheckCounter > 0; j++) {
            for (int i = 0, ilen = tailers.size(); i < ilen; i++) {
                ExcerptTailer tailer = tailers.get(i);
                if (tailer.nextIndex()) {
                    ChronicleLogEvent event = ChronicleLogHelper.decodeBinary(tailer);
                    String pid = getPidFromEvent(event);
                    String msg = event.getMessage();

                    String formattedMessage = MessageFormatter.arrayFormat(msg.substring(msg.indexOf('#') + 1),
                            event.getArgumentArray()).getMessage();

                    LogReceptor lr = getLogReceptor(config, event, pid);

                    LogReceptor.Severity sev = getSeverity(event.getLevel());

                    lr.process(event.getLoggerName(), sev, getProps(pid), formattedMessage);
                    j = 0;
                }
            }
            if (j > 0) {
                SyslogUtility.sleep(10);
            }
            recheckCounter--;
        }
    }

    public static void processLogs(LocalLogCollectorConfig conf, Pattern logPattern) {
        ArrayList<Chronicle> chronicles = new ArrayList<>();
        ArrayList<ExcerptTailer> tailers = new ArrayList<>();

        try {
            while (true) {
                processLogsNoLoop(chronicles, tailers, conf, logPattern);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPidFromEvent(ChronicleLogEvent event) {
        String msg = event.getMessage();
        return msg.substring(0, msg.indexOf('@'));
    }

    public static void main(String[] args) {
        if (ArrayUtils.getLength(args) != 1) {
            System.err.println(
                "\n\n The Local Log Collector reads logs from various shared memory areas, and redirects them to one or more syslog servers\n\nUsage: \n"+
                "\tjava -cp <jars> uk.gov.homeoffice.pontus.LocalLogCollector <yaml file>\n\n" +
                "\t The Yaml file should have following format:\n" +
                "logPatternRegex: '.*'  # This is a pattern of memory-mapped (shared memory) chronicle \n" +
                "                       # files with logs to read data from apps.\n" +
                "\n" +
                "basePath: '/tmp/log'   # This is the base directory from where to look for the memory-mapped files.\n" +
                "receptorInfoList: # each line here enables a different log pattern to be sent to different syslog servers:\n" +
                "  - { regex: '.*', type: 'LogReceptorLossless', syslogHostName: '10.229.101.111', syslogPort: 601 }\n" +
                "#  - { regex: '.*', type: 'LogReceptorLossy', syslogHostName: '10.229.101.111', syslogPort: 504 }\n" +
                "#  - { regex: '.*', type: 'LogReceptorEncrypted', syslogHostName: '10.229.101.111', syslogPort: 1999, keyStore: 'keystore path', keyStorePasswd: 'passwd', trustStore:'trustStorePath', trustStorePasswd: 'passwd'}\n" +
                "\n");
            System.exit(-1);
        }
        try {
            LocalLogCollectorConfig conf = getLocalLogCollectorConfigFromYamlFile(new File(args[0]));
            Pattern logPattern = Pattern.compile(conf.logPatternRegex);
            Path basePath = Paths.get(conf.basePath);
            ChronicleDiscovery.tailerDiscoveryThreadWrapper(basePath, logPattern);
            processLogs(conf, logPattern);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
