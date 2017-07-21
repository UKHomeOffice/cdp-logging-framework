package uk.gov.homeoffice.pontus;

import org.graylog2.syslog4j.util.SyslogUtility;

public class LogReceptorInfo {
    private String regex;
    private String type;
    private String syslogHostName;
    private int facility = SyslogUtility.FACILITY_USER;
    private int syslogPort;
    private String keyStore;
    private String keyStorePasswd;
    private String trustStore;
    private String trustStorePasswd;

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSyslogHostName() {
        return syslogHostName;
    }

    public void setSyslogHostName(String syslogHostName) {
        this.syslogHostName = syslogHostName;
    }

    public int getFacility() {
        return facility;
    }

    public void setFacility(int facility) {
        this.facility = facility;
    }

    public int getSyslogPort() {
        return syslogPort;
    }

    public void setSyslogPort(int syslogPort) {
        this.syslogPort = syslogPort;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePasswd() {
        return keyStorePasswd;
    }

    public void setKeyStorePasswd(String keyStorePasswd) {
        this.keyStorePasswd = keyStorePasswd;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePasswd() {
        return trustStorePasswd;
    }

    public void setTrustStorePasswd(String trustStorePasswd) {
        this.trustStorePasswd = trustStorePasswd;
    }
}
