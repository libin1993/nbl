package com.doit.net.Data;

import android.text.TextUtils;

import com.doit.net.Event.EventAdapter;
import com.doit.net.Event.UIEventManager;
import com.doit.net.Model.CacheManager;
import com.doit.net.Protocol.LTEMsgCode;
import com.doit.net.Protocol.LTEPackage;
import com.doit.net.Protocol.LTESendManager;
import com.doit.net.Protocol.LTE_PT_LOGIN;
import com.doit.net.Protocol.LTE_PT_PARAM;
import com.doit.net.Protocol.LTE_PT_SYSTEM;
import com.doit.net.Protocol.LTEReceivePackage;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.UtilDataFormatChange;
import com.doit.net.bean.DeviceState;
import com.doit.net.bean.LteCellConfig;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.bean.LteEquipConfig;

/**
 * Created by Zxc on 2018/10/18.
 */
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class LTEDataParse {
    //将字节数暂存
    private ArrayList<Byte> listReceiveBuffer = new ArrayList<Byte>();
    //包头的长度
    private short packageHeadLength = 52;

    public static Set<String> set = new HashSet<>();


    //解析数据
//    public synchronized void parseData(String ip, String port, byte[] bytesReceived, int receiveCount) {
//        //将接收到数据存放在列表中
//        for (int i = 0; i < receiveCount; i++) {
//            listReceiveBuffer.add(bytesReceived[i]);
//        }
//        //得到当前缓存中的长度
//        int listReceiveCount = listReceiveBuffer.size();
//
//        //如果缓存长度小于12说明最小包都没有收完整
//        if (listReceiveCount >= packageHeadLength) {
//            parseData(listReceiveCount);
//        }
//
//    }

    //解析数据
    public synchronized void parseData(String ip, byte[] bytesReceived, int receiveCount) {
        //将接收到数据存放在列表中
        for (int i = 0; i < receiveCount; i++) {
            listReceiveBuffer.add(bytesReceived[i]);
        }
        //得到当前缓存中的长度
        int listReceiveCount = listReceiveBuffer.size();

        //如果缓存长度小于12说明最小包都没有收完整
        if (listReceiveCount >= packageHeadLength) {
            parseData(ip, listReceiveCount);
        }

    }

    //将收到的字节数解析并且组成一个整包
    private void parseData(String ip, int listReceiveCount) {
        //循环读取包
        for (int i = 0; i < listReceiveCount; i++) {
            if (listReceiveBuffer.size() < packageHeadLength) {
                LogUtils.log("GTW丢包-丢字节");
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

            //获取缓存列表中的数据
            listReceiveCount = listReceiveBuffer.size();
            //如果有余下的字节数,则说明有余包
            if (listReceiveCount >= packageHeadLength) {
                LogUtils.log("LTE余下的字节数:" + listReceiveCount);
                i = -1;
            } else {
                break;
            }
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
        CacheManager.deviceName = tempDeviceName;  //全局

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


//        LTEReceivePackage receivePackage = new LTEReceivePackage();
//
//        //第一步取出包的长度
//        short packageLength = getShortData(tempPackage[0], tempPackage[1]);
//        receivePackage.setPackageLength(packageLength);
//        //UtilBaseLog.printLog("LTE收到(packageLength):"+receivePackage.getPackageLength());
//
//        //第二步取出CheckNum
//        short packageCheckNum = getShortData(tempPackage[2], tempPackage[3]);
//        receivePackage.setPackageCheckNum(packageCheckNum);
//        //UtilBaseLog.printLog("LTE收到(packageCheckNum):"+packageCheckNum);
//
//        //第三步取出序号
//        short packageSequence = getShortData(tempPackage[4], tempPackage[5]);
//        receivePackage.setPackageSequence(packageSequence);
//        //UtilBaseLog.printLog("LTE收到(packageSequence):"+packageSequence);
//
//        //第四步取出SessionID
//        short packageSessionID = getShortData(tempPackage[6], tempPackage[7]);
//        receivePackage.setPackageSessionID(packageSessionID);
//        //UtilBaseLog.printLog("LTE收到(packageSessionID):"+receivePackage.getPackageSessionID());
//
//        //第五步取出主协议类型EquipType
//        byte packageEquipType = tempPackage[8];
//        receivePackage.setPackageEquipType(packageEquipType);
//        //UtilBaseLog.printLog("LTE收到(packageEquipType):"+receivePackage.getPackageEquipType());
//
//        //第六步取出预留类型Reserve
//        byte packageReserve = tempPackage[9];
//        receivePackage.setPackageReserve(packageReserve);
//        //UtilBaseLog.printLog("LTE收到(packageReserve):"+receivePackage.getPackageReserve());
//
//        //第七步取出主协议类型MainType
//        byte packageMainType = tempPackage[10];
//        receivePackage.setPackageMainType(packageMainType);
//        //UtilBaseLog.printLog("LTE收到(packageMainType):"+receivePackage.getPackageMainType());
//
//        //第八步取出主协议类型Type
//        byte packageSubType = tempPackage[11];
//        receivePackage.setPackageSubType(packageSubType);
//        LogUtils.log("LTE收到packageMainType:" + packageMainType + ";  packageSubType:" + receivePackage.getPackageSubType());
//
//        //第九部取出内容
//        //1.计算子协议内容包的长度
//        int subPacketLength = packageLength - packageHeadLength;
//        byte[] byteSubContent = new byte[subPacketLength];
//        //2.取出子协议内容
//        if (subPacketLength > 0) {
//            for (int j = 0; j < byteSubContent.length; j++) {
//                byteSubContent[j] = tempPackage[packageHeadLength + j];
//            }
//        }
//        receivePackage.setByteSubContent(byteSubContent);
//
//        //实时解析协议
//        realTimeResponse(receivePackage);
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
                    case LTEMsgCode.SendCode.SET_COLLECT_MODE:
                        processMode(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.SET_RF:
                        processRF(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.GET_PARAM:
                        processGetParam(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.SET_PARAM:
                        processSetParam(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.SET_POWER:
                        processSetPa(ltePackage);
                        break;
                    case LTEMsgCode.SendCode.SET_POLL_EARFCN:
                        processSetFcn(ltePackage);
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
//            if (CacheManager.getLocState()) {
//                if (imsi.equals(CacheManager.getCurrentLoction().getImsi())) {
//                    EventAdapter.call(EventAdapter.LOCATION_RPT, rssi);
//                }
//            }

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

        UIEventManager.call(UIEventManager.KEY_REFRESH_DEVICE);
        UIEventManager.call(UIEventManager.KEY_RF_STATUS);
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

//            EventAdapter.call(EventAdapter.SHIELD_RPT, imsi+":"+rssi);
        }

    }

    /**
     * @param ltePackage 处理基站上报心跳
     */
    public void processHeartBeat(LTEPackage ltePackage) {
        LTESendManager.sendData(ltePackage.getIp(), LTEMsgCode.Type.APP_ACK, LTEMsgCode.RptCode.RPT_HEART_BEAT, null); //心跳回复

        CacheManager.deviceState.setDeviceState(DeviceState.NORMAL);

        set.add(ltePackage.getIp());
        UIEventManager.call(UIEventManager.KEY_HEARTBEAT_RPT);

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
            }
        }
        if (!TextUtils.isEmpty(band) && !TextUtils.isEmpty(rfState)) {
            CacheManager.updateRFState(band, "1".equals(rfState));
        }

    }




    /**
     * @param ltePackage 设置采集模式回复
     */
    public void processMode(LTEPackage ltePackage) {
        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("采集模式设置失败");
            return;
        }
        String content = ltePackage.getPackageContent().split("\r\n")[0];
        if ("0".equals(content)) {
            LogUtils.log("采集模式设置成功");
        } else {
            LogUtils.log("采集模式设置失败");
        }

    }

    /**
     * @param ltePackage 射频开关回复
     */
    public void processRF(LTEPackage ltePackage) {

        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("射频设置失败");
            return;
        }
        String content = ltePackage.getPackageContent().split("\r\n")[0];
        if ("0".equals(content)) {
            LogUtils.log("射频设置成功");
        } else {
            LogUtils.log("射频设置失败");
        }

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
     * @param ltePackage 设置功率回复
     */
    public void processSetPa(LTEPackage ltePackage) {
        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("设置功率失败");
            return;
        }
        String content = ltePackage.getPackageContent().split("\r\n")[0];
        if ("0".equals(content)) {
            LogUtils.log("设置功率成功");
            ProtocolManager.getEquipAndAllChannelConfig();
        } else {
            LogUtils.log("设置功率失败");
        }

    }

    /**
     * @param ltePackage 设置频点回复
     */
    public void processSetFcn(LTEPackage ltePackage) {
        if (TextUtils.isEmpty(ltePackage.getPackageContent())) {
            LogUtils.log("设置频点失败");
            return;
        }
        String content = ltePackage.getPackageContent().split("\r\n")[0];
        if ("0".equals(content)) {
            LogUtils.log("设置频点成功");
            ProtocolManager.getEquipAndAllChannelConfig();
        } else {
            LogUtils.log("设置频点失败");
        }

    }


    //实时回复协议
    public void realTimeResponse(LTEReceivePackage receivePackage) {
        switch (receivePackage.getPackageMainType()) {

            case LTE_PT_LOGIN.PT_LOGIN:
                LTE_PT_LOGIN.loginResp(receivePackage);
                break;

            case LTE_PT_SYSTEM.PT_SYSTEM:
                switch (receivePackage.getPackageSubType()) {
                    case LTE_PT_SYSTEM.SYSTEM_REBOOT_ACK:
                    case LTE_PT_SYSTEM.SYSTEM_SET_DATETIME_ASK:
                    case LTE_PT_SYSTEM.SYSTEM_UPGRADE_ACK:
                    case LTE_PT_SYSTEM.SYSTEM_GET_LOG_ACK:
                        LTE_PT_SYSTEM.processCommonSysResp(receivePackage);
                        break;
                }

                break;
            case LTE_PT_PARAM.PT_PARAM:
                switch (receivePackage.getPackageSubType()) {
                    case LTE_PT_PARAM.PARAM_SET_ENB_CONFIG_ACK:
                    case LTE_PT_PARAM.PARAM_SET_CHANNEL_CONFIG_ACK:
                    case LTE_PT_PARAM.PARAM_SET_CHANNEL_ON_ACK:
                    case LTE_PT_PARAM.PARAM_SET_BLACK_NAMELIST_ACK:
                    case LTE_PT_PARAM.PARAM_SET_RT_IMSI_ACK:
                    case LTE_PT_PARAM.PARAM_SET_CHANNEL_OFF_ACK:
                    case LTE_PT_PARAM.PARAM_SET_FTP_CONFIG_ACK:
                    case LTE_PT_PARAM.PARAM_CHANGE_TAG_ACK:
                    case LTE_PT_PARAM.PARAM_SET_NAMELIST_ACK:
                    case LTE_PT_PARAM.PARAM_CHANGE_BAND_ACK:
                    case LTE_PT_PARAM.PARAM_SET_SCAN_FREQ_ACK:
                    case LTE_PT_PARAM.PARAM_SET_FAN_ACK:
                    case LTE_PT_PARAM.PPARAM_SET_LOC_IMSI_ACK:
                    case LTE_PT_PARAM.PARAM_SET_ACTIVE_MODE_ACK:
                    case LTE_PT_PARAM.PARAM_RPT_UPGRADE_STATUS:
                        LogUtils.log("设置回复:" + UtilDataFormatChange.bytesToString(receivePackage.getByteSubContent(), 0));
                        LTE_PT_PARAM.processSetResp(receivePackage);
                        break;

                    case LTE_PT_PARAM.PARAM_GET_ENB_CONFIG_ACK:
                        LTE_PT_PARAM.processEnbConfigQuery(receivePackage);
                        break;

                    case LTE_PT_PARAM.PARAM_GET_ACTIVE_MODE_ASK:
                        LogUtils.log("工作模式查询:" + UtilDataFormatChange.bytesToString(receivePackage.getByteSubContent(), 0));
                        break;

                    case LTE_PT_PARAM.PARAM_RPT_HEATBEAT:
                        LTE_PT_PARAM.processRPTHeartbeat(receivePackage);
                        break;

                    case LTE_PT_PARAM.PARAM_GET_NAMELIST_ACK:
                        LTE_PT_PARAM.processNamelistQuery(receivePackage);
                        break;
                    case LTE_PT_PARAM.PARAM_RPT_BLACK_NAME:
                        LTE_PT_PARAM.processRptBlackName(receivePackage);
                        break;

                    case LTE_PT_PARAM.PARAM_SET_SCAN_FREQ:
                        LTE_PT_PARAM.processRPTHeartbeat(receivePackage);
                        break;

                    case LTE_PT_PARAM.PARAM_RPT_SCAN_FREQ:
                        LTE_PT_PARAM.processRPTFreqScan(receivePackage);
                        break;
                    case LTE_PT_PARAM.RPT_SRSP_GROUP:
                        LTE_PT_PARAM.processLocRpt(receivePackage);
                        break;
                }
                break;
        }
    }


    public void clearReceiveBuffer() {
        LogUtils.log("clearReceiveBuffer... ...");
        listReceiveBuffer.clear();
    }
}
