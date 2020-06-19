package com.doit.net.Data;

import android.text.TextUtils;

import com.doit.net.Event.EventAdapter;
import com.doit.net.Model.CacheManager;
import com.doit.net.Protocol.LTEMsgCode;
import com.doit.net.Protocol.LTEPackage;
import com.doit.net.Protocol.LTESendManager;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.UtilDataFormatChange;
import com.doit.net.bean.DeviceState;
import com.doit.net.bean.LteChannelCfg;

/**
 * Created by Zxc on 2018/10/18.
 */
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LTEDataParse {
    //将字节数暂存
    private ArrayList<Byte> listReceiveBuffer = new ArrayList<Byte>();
    //包头的长度
    private short packageHeadLength = 52;


    //解析数据
    public synchronized void parseData(String ip, byte[] bytesReceived, int receiveCount) {
        //将接收到数据存放在列表中
        for (int i = 0; i < receiveCount; i++) {
            listReceiveBuffer.add(bytesReceived[i]);
        }

        while (true){
            //得到当前缓存中的长度
            int listReceiveCount = listReceiveBuffer.size();

            //如果缓存长度小于52说明最小包都没有收完整
            if (listReceiveCount < packageHeadLength) {
                break;
            }

            //取出长度
            byte[] contentLength = {listReceiveBuffer.get(8), listReceiveBuffer.get(9), listReceiveBuffer.get(10), listReceiveBuffer.get(11)};
            int contentLen = UtilDataFormatChange.byteToInt(contentLength) + 48;


            LogUtils.log("分包大小：" + listReceiveBuffer.size() + "," + contentLen);
            //判断缓存列表中的数据是否达到一个包的数据
            if (listReceiveBuffer.size() < contentLen) {
                LogUtils.log("LTE没有达到整包数:");
                break;
            }

            byte[] tempPackage = new byte[contentLen];
            //取出一个整包
            for (int j = 0; j < contentLen; j++) {
                tempPackage[j] = listReceiveBuffer.get(j);
            }

            //删除内存列表中的数据
            for (int j = 0; j < contentLen; j++) {
                listReceiveBuffer.remove(0);
            }

            //解析包
            parsePackageData(ip, tempPackage);
        }

    }


    //解析成包数据
    private void parsePackageData(String ip, byte[] tempPackage) {
        if (tempPackage.length < 52)
            return;

        LTEPackage ltePackage = new LTEPackage();

        //magic  默认
        byte[] tempMagic = new byte[4];
        System.arraycopy(tempPackage, 0, tempMagic, 0, 4);
        ltePackage.setMagic(tempMagic);
        CacheManager.magic = tempMagic;  //magic全局


        //自增序列
        byte[] tempId = new byte[4];
        System.arraycopy(tempPackage, 4, tempId, 0, 4);
        int id = UtilDataFormatChange.byteToInt(tempId);
        ltePackage.setId(id);

        //真实数据长度（消息内容部分）
        byte[] tempDataLength = new byte[4];
        System.arraycopy(tempPackage, 8, tempDataLength, 0, 4);
        int dataLength = UtilDataFormatChange.byteToInt(tempDataLength);
        ltePackage.setDataLength(dataLength);

        //密文长度
        byte[] tempCipherLength = new byte[4];
        System.arraycopy(tempPackage, 12, tempCipherLength, 0, 4);
        ltePackage.setCipherLength(tempCipherLength);

        //crc
        byte[] tempCrc = new byte[4];
        System.arraycopy(tempPackage, 16, tempCrc, 0, 4);
        ltePackage.setCipherLength(tempCrc);

        //采集设备（板卡）编号
        byte[] tempDeviceName = new byte[16];
        System.arraycopy(tempPackage, 20, tempDeviceName, 0, 16);
        ltePackage.setDeviceName(tempDeviceName);


        //时间戳
        byte[] tempTimestamp = new byte[4];
        System.arraycopy(tempPackage, 36, tempTimestamp, 0, 4);
        int timestamp = UtilDataFormatChange.byteToInt(tempTimestamp);
        ltePackage.setTimestamp(timestamp);

        //消息类型
        byte[] tempMsgType = new byte[2];
        System.arraycopy(tempPackage, 48, tempMsgType, 0, 2);
        String msgType = new String(tempMsgType, StandardCharsets.US_ASCII);
        ltePackage.setMsgType(msgType);


        //协议代码
        byte[] tempMsgCode = new byte[2];
        System.arraycopy(tempPackage, 50, tempMsgCode, 0, 2);
        String msgCode = new String(tempMsgCode, StandardCharsets.US_ASCII);
        ltePackage.setMsgCode(msgCode);

        //包内容
        byte[] tempPackageContent = new byte[dataLength - 4];
        System.arraycopy(tempPackage, 52, tempPackageContent, 0, dataLength - 4);
        String packageContent = new String(tempPackageContent, StandardCharsets.US_ASCII);
        ltePackage.setPackageContent(packageContent);

        ltePackage.setIp(ip);


        LogUtils.log(ltePackage.toString());

        realTimeResponse(ltePackage);

    }


    public void realTimeResponse(LTEPackage ltePackage) {
        switch (ltePackage.getMsgType()) {
            case LTEMsgCode.Type.STATION_RPT:   //设备主动上报，APP需回复
                switch (ltePackage.getMsgCode()) {
                    case LTEMsgCode.RptCode.RPT_HEART_BEAT:   //基站上报心跳
                        processHeartBeat(ltePackage);
                        break;
                    case LTEMsgCode.RptCode.RPT_UEID:   //基站上报终端信息
                        processReport(ltePackage);
                        break;
                    case LTEMsgCode.RptCode.RPT_LOC_DATA:   //基站上报定位数据
                        processLocData(ltePackage);
                        break;
                }
                break;
            case LTEMsgCode.Type.STATION_ACK:   //设备回复
                switch (ltePackage.getMsgCode()) {
                    case LTEMsgCode.SendCode.SET_RF:
                        commonAck(ltePackage,"设置射频");
                        break;
                    case LTEMsgCode.SendCode.GET_PARAM:
                        processGetParam(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.SET_PARAM:
                        processSetParam(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.SET_POWER:
                        commonAck(ltePackage,"设置功率");
                        break;
                    case LTEMsgCode.SendCode.SET_POLL_EARFCN:
                        commonAck(ltePackage,"设置频点");
                        break;
                    case LTEMsgCode.SendCode.SET_LOCATION_MODE:
                        commonAck(ltePackage,"设置定位模式");
                        break;
                    case LTEMsgCode.SendCode.SET_LOCATION_IMSI:
                        commonAck(ltePackage,"设置定位名单(2.77)");
                        break;
                    case LTEMsgCode.SendCode.SET_BLACKLIST:
                        commonAck(ltePackage,"设置定位名单（2.22）");
                        break;
                    case LTEMsgCode.SendCode.REBOOT:
                        commonAck(ltePackage,"设备重启");
                        break;
                    case LTEMsgCode.SendCode.SET_UPGRADE:
                        processUpdate(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.SET_FALLBACK:
                        commonAck(ltePackage,"版本回退");
                        break;
                }
                break;
        }
    }

    /**
     * @param ltePackage
     * @param msg
     * 通用回复
     */
    public void commonAck(LTEPackage ltePackage,String msg){
        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log(msg+"失败");
            return;
        }
        String content = ltePackage.getPackageContent().split("\r\n")[0];
        if ("0".equals(content)) {
            LogUtils.log(msg+"成功");
        } else {
            LogUtils.log(msg+"失败");
        }
    }

    /**
     * @param ltePackage 处理基站上报设备信息
     */
    public void processReport(LTEPackage ltePackage) {
        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("上报设备信息失败");
            return;
        }

        String[] split = ltePackage.getPackageContent().split("\r\n")[0].split("\t");

        if (split.length <= 0) {
            LogUtils.log("上报设备信息失败");
            return;
        }

        String imsi = null;
        String rssi = null;
        for (String s : split) {
            String[] split1 = s.split(":");
            switch (split1[0]) {
                case "IMSI":
                    imsi = TextUtils.isEmpty(split1[1]) ? null : split1[1];
                    break;
                case "RSSI":
                    rssi = TextUtils.isEmpty(split1[1]) ? null : split1[1];
                    break;
            }
        }

        if (!TextUtils.isEmpty(imsi) && !TextUtils.isEmpty(rssi)) {
            EventAdapter.call(EventAdapter.SHIELD_RPT, imsi + ":" + rssi);
        }

    }

    /**
     * @param ltePackage 处理设备运行参数
     */
    public void processGetParam(LTEPackage ltePackage) {
        LogUtils.log("设备运行参数上报");


        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("无band");
            return;
        }

        String[] split = ltePackage.getPackageContent().split("\r\n")[0].split("\t");

        if (split.length <= 0) {
            LogUtils.log("无band");
            return;
        }

        LteChannelCfg lteChannelCfg = new LteChannelCfg();



        lteChannelCfg.setIp(ltePackage.getIp());
        for (String s : split) {
            String[] split1 = s.split(":");
            switch (split1[0]) {
                case "PCI":
                    lteChannelCfg.setPci(split1[1]);
                    break;
                case "PLMN":
                    lteChannelCfg.setPlmn(split1[1]);
                    break;
                case "TAC":   //跟踪区码
                    lteChannelCfg.setTac(split1[1]);
                    break;
                case "BAND":
                    lteChannelCfg.setBand(split1[1]);
                    break;
                case "DLARFCN":   //下行频点
                    break;
                case "ULARFCN":   //上行频点
                    break;
                case "POLLFCN":
                    lteChannelCfg.setFcn(split1[1]);
                    break;
                case "POLLTMR":
                    lteChannelCfg.setPollTmr(split1[1]);
                    break;
                case "RF":      //射频开关
                    lteChannelCfg.setRFState("1".equals(split1[1]));
                    break;
                case "PA":   //整机输出功率
                    lteChannelCfg.setPa(split1[1]);
                    break;
                case "CAP":   //采集模式

                    break;
                case "GAIN":   //功放增益
                    lteChannelCfg.setGain(split1[1]);
                    break;
                case "RXGAIN":   //功放增益
                    lteChannelCfg.setRxGain(split1[1]);
                    break;
                case "UECTRL":   //终端管控

                    break;
                case "PERIOD":   //采集周期
                    lteChannelCfg.setPeriod(split1[1]);
                    break;
                case "AUTOREM":   //开机扫描

                    break;
                case "LTEREM":   //4G 公网扫描

                    break;
                case "GPS":    //GPS 同步开关
                    lteChannelCfg.setGps(split1[1]);
                    break;
                case "CNM":     //空口同步开关
                    lteChannelCfg.setCnm(split1[1]);
                    break;
                case "GPSOFS":     //GPS 自动纠偏值上报
                    lteChannelCfg.setGpsOffset(split1[1]);
                    break;
                case "ACCMIN":
                    lteChannelCfg.setRlm(split1[1]);
                    break;
            }
        }

        CacheManager.addChannel(lteChannelCfg);

        EventAdapter.call(EventAdapter.REFRESH_DEVICE);
        EventAdapter.call(EventAdapter.RF_STATUS);
    }


    /**
     * @param ltePackage 处理定位上报数据
     */
    public void processLocData(LTEPackage ltePackage) {

        if (TextUtils.isEmpty(ltePackage.getPackageContent())){
            LogUtils.log("定位上报数据失败");
            return;
        }

        String[] split = ltePackage.getPackageContent().split("\r\n")[0].split("\t");

        if (split.length <= 0){
            LogUtils.log("定位上报数据失败");
            return;
        }

        String imsi = null;
        String rssi = null;
        for (String s : split) {
            String[] split1 = s.split(":");
            switch (split1[0]){
                case "IMSI":
                    imsi = TextUtils.isEmpty(split1[1]) ? null:split1[1];
                    break;
                case "RSSI":
                    rssi = TextUtils.isEmpty(split1[1]) ? null:split1[1];
                    break;
            }
        }

        if (!TextUtils.isEmpty(imsi) && !TextUtils.isEmpty(rssi)){
            LogUtils.log("定位上报数据："+imsi+":"+rssi);
            if (CacheManager.getLocState()) {
                if (imsi.equals(CacheManager.getCurrentLoction().getImsi())) {
                    EventAdapter.call(EventAdapter.LOCATION_RPT, rssi);
                }
            }

        }

    }

    /**
     * @param ltePackage 处理基站上报心跳
     */
    public void processHeartBeat(LTEPackage ltePackage) {


        CacheManager.deviceState.setDeviceState(DeviceState.NORMAL);


        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("无band");
            return;
        }

        String[] split = ltePackage.getPackageContent().split("\r\n")[0].split("\t");

        if (split.length <= 0) {
            LogUtils.log("无band");
            return;
        }


        String band = null;
        String rfState = null;
        for (String s : split) {
            String[] split1 = s.split(":");
            switch (split1[0]) {
                case "BAND":
                    band = split1[1];
                    break;
                case "RF":
                    rfState = split1[1];
                    break;
                case "FW":
                    CacheManager.addDevice(ltePackage.getIp(),ltePackage.getDeviceName(),split1[1]);
                    break;
            }
        }
        if (!TextUtils.isEmpty(band) && !TextUtils.isEmpty(rfState)) {
            CacheManager.updateRFState(band, "1".equals(rfState));
        }

        LTESendManager.sendData(ltePackage.getIp(), LTEMsgCode.Type.APP_ACK, LTEMsgCode.RptCode.RPT_HEART_BEAT, null); //心跳回复
        EventAdapter.call(EventAdapter.HEARTBEAT_RPT);
        EventAdapter.call(EventAdapter.RF_STATUS);
    }



    /**
     * @param ltePackage 设置基站参数回复
     */
    public void processSetParam(LTEPackage ltePackage) {
        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("设置通道失败");
            return;
        }
        String content = ltePackage.getPackageContent().split("\r\n")[0];
        if ("0".equals(content)) {
            LogUtils.log("设置通道成功");
            ProtocolManager.getEquipAndAllChannelConfig();
        } else {
            LogUtils.log("设置通道失败");
        }

    }

    /**
     * @param ltePackage 版本更新回复
     */
    public void processUpdate(LTEPackage ltePackage) {
        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("更新失败");
            return;
        }
        String content = ltePackage.getPackageContent().split("\r\n")[0];

        LogUtils.log("更新进度："+content);

        EventAdapter.call(EventAdapter.UPGRADE_STATUS,ltePackage.getIp()+","+content);
    }





    public void clearReceiveBuffer() {
        LogUtils.log("clearReceiveBuffer... ...");
        listReceiveBuffer.clear();
    }
}
