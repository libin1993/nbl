package com.doit.net.bean;

/**
 * Author：Libin on 2020/7/1 17:11
 * Email：1993911441@qq.com
 * Describe：
 */
public class ReportBean {
    private String ip;
    private String imsi;
    private String rssi;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public ReportBean(String ip, String imsi, String rssi) {
        this.ip = ip;
        this.imsi = imsi;
        this.rssi = rssi;
    }
}
