package com.doit.net.bean;

/**
 * Created by wiker on 2016-08-15.
 */
public class LocationBean {

    private String imsi = "";
    private boolean isStart = false;
    private String fcn;

    public String getFcn() {
        return fcn;
    }

    public void setFcn(String fcn) {
        this.fcn = fcn;
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
