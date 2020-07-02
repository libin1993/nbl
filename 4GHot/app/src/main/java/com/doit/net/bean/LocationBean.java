package com.doit.net.bean;

/**
 * Created by wiker on 2016-08-15.
 */
public class LocationBean {

    private String imsi = "";
    private boolean isStart = false;
    private String ip;

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

    public boolean isLocateStart() {
        return isStart;
    }

    public void setLocateStart(boolean start) {
        this.isStart = start;
    }
}
