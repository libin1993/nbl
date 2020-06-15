package com.doit.net.Protocol;

import java.util.Arrays;

/**
 * Author：Libin on 2020/6/9 15:44
 * Email：1993911441@qq.com
 * Describe：数据包
 */
public class LTEPackage {
    private byte[] magic;  //默认：00 FF FF 00   4字节
    private int id;     //自增序列   4字节
    private int dataLength;    //真实数据长度（消息内容部分）   4字节
    private byte[] cipherLength = new byte[4];   //密文长度    4字节
    private byte[] crc = new byte[4];   //校验码     4字节
    private byte[] deviceName = new byte[16];    //采集设备（板卡）编号  16字节
    private int timestamp;  //时间戳   4位
    private byte[] reserve = new byte[8];  //预留字段   8字节
    private String msgType;   //消息类型  2字节
    private String msgCode;    //协议代码    2字节
    private String packageContent;  //包内容   可变长度

    private String ip;
    public static final int HEAD_SIZE = 52;  //包头长度  52字节

    public LTEPackage() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public byte[] getMagic() {
        return magic;
    }

    public void setMagic(byte[] magic) {
        this.magic = magic;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public byte[] getCipherLength() {
        return cipherLength;
    }

    public void setCipherLength(byte[] cipherLength) {
        this.cipherLength = cipherLength;
    }

    public byte[] getCrc() {
        return crc;
    }

    public void setCrc(byte[] crc) {
        this.crc = crc;
    }

    public byte[] getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(byte[] deviceName) {
        this.deviceName = deviceName;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getReserve() {
        return reserve;
    }

    public void setReserve(byte[] reserve) {
        this.reserve = reserve;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public String getPackageContent() {
        return packageContent;
    }

    public void setPackageContent(String packageContent) {
        this.packageContent = packageContent;
    }

    @Override
    public String toString() {
        return "LTEPackage{" +
                "magic=" + Arrays.toString(magic) +
                ", id=" + id +
                ", dataLength=" + dataLength +
                ", cipherLength=" + Arrays.toString(cipherLength) +
                ", crc=" + Arrays.toString(crc) +
                ", deviceName=" + Arrays.toString(deviceName) +
                ", timestamp=" + timestamp +
                ", reserve=" + Arrays.toString(reserve) +
                ", msgType='" + msgType + '\'' +
                ", msgCode='" + msgCode + '\'' +
                ", packageContent='" + packageContent + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
