package uk.gov.homeoffice.pontus;


import org.graylog2.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;

public class LogReceptorEncrypted extends LogReceptor {

    private SSLTCPNetSyslogConfig conf = new SSLTCPNetSyslogConfig();

    public LogReceptorEncrypted(String keyStore, String trustStore, String keystorePass, String trustStorePass,
                                String syslogInstanceName, String procId, String defaultMsgHostName,
                                String defaultAppName, int defaultFacility, String syslogServerHostName, int port, boolean threaded) {
        conf.setKeyStore(keyStore);
        conf.setKeyStorePassword(keystorePass);
        conf.setTrustStore(trustStore);
        conf.setTrustStorePassword(trustStorePass);
        conf.setThreaded(threaded);
        super.init(conf, procId, syslogInstanceName, defaultMsgHostName, defaultAppName, defaultFacility,
                syslogServerHostName, port);
    }
}
