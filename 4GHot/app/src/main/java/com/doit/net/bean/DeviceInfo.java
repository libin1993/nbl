package com.doit.net.bean;

/**
 * Author：Libin on 2020/6/17 16:09
 * Email：1993911441@qq.com
 * Describe：
 */
public class DeviceInfo {
    private String ip;  //设备ip
    private byte[] deviceName = new byte[16];  //设备编号
    private String fw;  //软件版本

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public byte[] getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(byte[] deviceName) {
        System.arraycopy(deviceName, 0, this.deviceName, 0, 16);
    }

    public String getFw() {
        return fw;
    }

    public void setFw(String fw) {
        this.fw = fw;
    }
}
