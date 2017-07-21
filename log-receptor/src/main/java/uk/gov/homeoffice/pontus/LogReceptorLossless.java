package uk.gov.homeoffice.pontus;

import org.graylog2.syslog4j.impl.net.tcp.TCPNetSyslogConfig;

public class LogReceptorLossless extends LogReceptor {
    private TCPNetSyslogConfig conf = new TCPNetSyslogConfig();

    public LogReceptorLossless(int solingerSeconds, int writeRetries, String syslogInstanceName, String procId,
                               String defaultMsgHostName, String defaultAppName, int defaultFacility,
                               String syslogServerHostName, int port) {

        conf.setPersistentConnection(true);
        conf.setKeepAlive(true);
        conf.setReuseAddress(true);
        conf.setWriteRetries(writeRetries);
        conf.setSoLingerSeconds(solingerSeconds);
        super.init(conf, procId, syslogInstanceName, defaultMsgHostName, defaultAppName, defaultFacility,
                syslogServerHostName, port);
    }

    public LogReceptorLossless(String syslogInstanceName, String procId, String defaultMsgHostName,
                               String defaultAppName, int defaultFacility, String syslogServerHostName, int port) {
        this(10, 10, syslogInstanceName, procId, defaultMsgHostName, defaultAppName, defaultFacility,
                syslogServerHostName, port);
    }
}
