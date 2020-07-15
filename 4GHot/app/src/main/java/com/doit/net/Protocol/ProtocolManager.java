package com.doit.net.Protocol;

import android.text.TextUtils;

import com.doit.net.Event.EventAdapter;
import com.doit.net.Model.DBChannel;
import com.doit.net.Model.DBScanFcn;
import com.doit.net.Model.UCSIDBManager;
import com.doit.net.Sockets.NetConfig;
import com.doit.net.Utils.NetWorkUtils;
import com.doit.net.Utils.UtilDataFormatChange;
import com.doit.net.Utils.UtilOperator;
import com.doit.net.application.MyApplication;
import com.doit.net.bean.DeviceInfo;
import com.doit.net.bean.FtpConfig;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.Model.BlackBoxManger;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.LogUtils;

import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wiker on 2017-06-25.
 */

public class ProtocolManager {



    public static void getEquipAndAllChannelConfig() {
        LogUtils.log("获取所有设备运行参数");
        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.GET_PARAM, null);
    }


    /**
     * 获取白名单
     */
    public static void getNameList() {
        LogUtils.log("获取白名单");
//        LTE_PT_PARAM.queryCommonParam(LTE_PT_PARAM.PARAM_GET_NAMELIST);
    }

    /**
     * 设置系统时间
     */
    public static void setNowTime() {

        LogUtils.log("当前时间：" + System.currentTimeMillis() / 1000);

        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_TIME, System.currentTimeMillis() / 1000 + "");
    }


    /**
     * 设置默认频点
     */
    public static void setDefaultFcn() {
        String fddFcn = getCheckedFcn(NetConfig.FDD_IP);
        String tddFcn = getCheckedFcn(NetConfig.TDD_IP);

        if (!TextUtils.isEmpty(fddFcn)){
            CacheManager.fcnMap.put(NetConfig.FDD_IP,fddFcn);
            setFcn(NetConfig.FDD_IP, fddFcn, "10");
        }

        if (!TextUtils.isEmpty(tddFcn)){
            CacheManager.fcnMap.put(NetConfig.TDD_IP,tddFcn);
            setFcn(NetConfig.TDD_IP, tddFcn, "10");
        }
    }

    /**
     * 获取公网环境参数请求  TDD搜网
     */
    public static void getNetworkParams() {

        String fcns ="";
        try {
            DbManager dbManager = UCSIDBManager.getDbManager();
            List<DBScanFcn> scanFcnList = dbManager.selector(DBScanFcn.class)
                    .where("is_check", "=", 1)
                    .and("status", "=", 1)
                    .findAll();
            if (scanFcnList !=null && scanFcnList.size() > 0){
                StringBuilder scanFcn = new StringBuilder();
                for (int i = 0; i < scanFcnList.size(); i++) {
                    int fcn = scanFcnList.get(i).getFcn();
                    if (i == scanFcnList.size() - 1){
                        scanFcn.append(fcn);
                    }else {
                        scanFcn.append(fcn).append(",");
                    }
                }
                fcns =  scanFcn.toString();
            }

        } catch (DbException e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(fcns)){
            return;
        }

        String ip = null;
        for (DeviceInfo deviceInfo : CacheManager.deviceList) {
            if (deviceInfo.getIp().equals(NetConfig.TDD_IP)){
                ip = deviceInfo.getIp();
                break;
            }
        }
        if (TextUtils.isEmpty(ip)){
            ip = NetConfig.FDD_IP;
        }

        List<String> params = new ArrayList<>();
        params.add("AUTOREM:0");
        params.add("LTEREM:1");
        params.add("EARFCN:"+fcns);
        params.add("GSMREM:0");
        params.add("ARFCN:");
        params.add("REMPRD:");
        params.add("AUTOCFG:0");
        String content = UtilDataFormatChange.encode(params);
        if (TextUtils.isEmpty(content)) {
            return;
        }

        LogUtils.log("获取公网环境参数：" + content);


        LTESendManager.sendData(ip,LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.GET_SCAN, content);
    }


    public static void changeTac() {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        for (LteChannelCfg channel : CacheManager.channels) {

            int tac = Integer.parseInt(channel.getTac());
            if (tac < 65535) {
                tac++;
            } else {
                tac = 1;
            }
            ProtocolManager.setChannel(channel.getIp(), null, null, null, null, String.valueOf(tac), null);
        }

    }

    /**
     * @param imsi 修改频点
     */
    public static  void exchangeFcn(String imsi){
        String fddFcn = UtilOperator.getFcn(imsi,CacheManager.fcnMap.get(NetConfig.FDD_IP));
        String tddFcn = UtilOperator.getFcn(imsi,CacheManager.fcnMap.get(NetConfig.TDD_IP));

        if (!TextUtils.isEmpty(fddFcn)){
            setFcn(NetConfig.FDD_IP,fddFcn,"10");
        }

        if (!TextUtils.isEmpty(tddFcn)){
           setFcn(NetConfig.TDD_IP,tddFcn,"10");
        }

    }




    public static void reboot() {
        if (!CacheManager.isDeviceOk()) {
            return;
        }
        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.REBOOT, null);
        EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.REBOOT_DEVICE);
    }



    public static void setDetectCarrierOpetation(String carrierOpetation) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        String plnmValue = "46000,46001,46011";
        if (carrierOpetation.equals("detect_ctj")) {
            plnmValue = "46000,46000,46000";
        } else if (carrierOpetation.equals("detect_ctu")) {
            plnmValue = "46001,46001,46001";
        } else if (carrierOpetation.equals("detect_ctc")) {
            plnmValue = "46011,46011,46011";
        }

        for (LteChannelCfg channel : CacheManager.getChannels()) {
            setChannelConfig(channel.getIdx(), "", plnmValue, "", "", "", "", "");
        }
    }


    /**
     * @param ip
     * @param band   频段
     * @param plmn
     * @param rxGain 上行增益
     * @param accmin 最小接收电平
     *               设置通道
     */
    public static void setChannel(String ip, String band, String plmn, String rxGain, String accmin, String tac, String pci) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        List<String> params = new ArrayList<>();


        if (!TextUtils.isEmpty(band)) {
            params.add("BAND:" + band);
        }

        if (!TextUtils.isEmpty(plmn)) {
            params.add("PLMN:" + plmn);
        }


        if (!TextUtils.isEmpty(rxGain)) {
            params.add("RXGAIN:" + rxGain);
        }


        if (!TextUtils.isEmpty(accmin)) {
            params.add("ACCMIN:" + accmin);
        }

        if (!TextUtils.isEmpty(tac)) {
            params.add("TAC:" + tac);
        }


        if (!TextUtils.isEmpty(pci)) {
            params.add("PCI:" + pci);
        }


        String content = UtilDataFormatChange.encode(params);
        if (TextUtils.isEmpty(content)) {
            return;
        }

        LogUtils.log("通道设置：" + content);
        LTESendManager.sendData(ip, LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_PARAM, content);
    }


    /**
     * 设置下行功率
     */
    public static void setPa(String ip, String pa) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        List<String> params = new ArrayList<>();

        if (!TextUtils.isEmpty(pa)) {
            params.add("PA:" + pa);
        }

        String content = UtilDataFormatChange.encode(params);
        if (TextUtils.isEmpty(content)) {
            return;
        }

        LogUtils.log("功率设置：" + content);
        LTESendManager.sendData(ip, LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_POWER, content);
    }


    /**
     * 设置频点
     */
    public static void setFcn(String ip, String fcn, String period) {

        List<String> params = new ArrayList<>();


        if (!TextUtils.isEmpty(fcn)) {
            params.add("POLLFCN:" + fcn);
        }

        if (!TextUtils.isEmpty(period)) {
            params.add("POLLTMR:" + period);
        }


        String content = UtilDataFormatChange.encode(params);
        if (TextUtils.isEmpty(content)) {
            return;
        }

        LogUtils.log("设置频点：" + content);
        LTESendManager.sendData(ip, LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_POLL_EARFCN, content);
    }

    /**
     * 设置同步
     */
    public static void setSync(String ip, String gps, String frmOfs,String cnm) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        List<String> params = new ArrayList<>();


        if (!TextUtils.isEmpty(gps)) {
            params.add("GPS:" + gps);
        }

        if (!TextUtils.isEmpty(frmOfs)) {
            params.add("FRMOFS:" + frmOfs);
        }

        if (!TextUtils.isEmpty(cnm)) {
            params.add("CNM:" + cnm);
        }

        String content = UtilDataFormatChange.encode(params);
        if (TextUtils.isEmpty(content)) {
            return;
        }

        LogUtils.log("同步设置：" + content);
        LTESendManager.sendData(ip, LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_SYNC_PARAM, content);
    }


    public static void setChannelConfig(String idx, String fcn, String plmn, String pa,
                                        String ga, String rxlevMin, String atuoOpenRF, String AltFcn) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        if ("".equals(idx))
            return;


        String configContent = "IDX:";
        configContent += idx;

        if (!"".equals(plmn)) {
            configContent += "@PLMN:";
            configContent += plmn;
        }

        if (!"".equals(fcn)) {
            configContent += "@FCN:";
            configContent += fcn;
        }

        if (!"".equals(pa)) {
            configContent += "@PA:";
            configContent += pa;
            //configContent += checkPa(idx, pa);
        }

        if (!"".equals(ga)) {
            configContent += "@GA:";
            configContent += ga;
        }

        if (!"".equals(rxlevMin)) {
            configContent += "@RLM:";
            configContent += rxlevMin;
        }


        if (!"".equals(atuoOpenRF)) {
            configContent += "@AUTO_OPEN:";
            configContent += atuoOpenRF;
        }

        if (!"".equals(AltFcn)) {
            configContent += "@ALT_FCN:";
            configContent += AltFcn;
        }

//        configContent += "@HOLD:";
//        configContent += HOLD_VALUE;


        LogUtils.log("设置通道: " + configContent);

        LTE_PT_PARAM.setCommonParam(LTE_PT_PARAM.PARAM_SET_CHANNEL_CONFIG, configContent);
        //BlackBoxManger.recordOperation(BlackBoxManger.SET_CHANNEL_CONFIG+configContent);
    }

    //加入定位关闭非目标运营商频点加大目标运营商频点功率策略之后，
    //这个功率判定方法不再好用，就暂时不做对功率限制做判定了
    private static String checkPa(String idx, String ga) {
        String band = getBandByIdx(idx);
        String returnGa = "";

        String[] tmpGa = ga.split(",");
        if (tmpGa == null || tmpGa.length != 3) {
            LogUtils.log("len " + tmpGa.length);
            return ga;
        }


        if (band.equals("1") || band.equals("3")) {
            for (int i = 0; i < tmpGa.length; i++) {
                if (Integer.valueOf(tmpGa[i]) > -7) {
                    returnGa += -7;
                } else {
                    returnGa += tmpGa[i];
                }
                returnGa += ",";
            }
        } else if (band.equals("38") || band.equals("40") || band.equals("41")) {
            for (int i = 0; i < tmpGa.length; i++) {
                if (Integer.valueOf(tmpGa[i]) > -1) {
                    returnGa += -1;
                } else {
                    returnGa += tmpGa[i];
                }
                returnGa += ",";
            }
        } else if (band.equals("39")) {
            for (int i = 0; i < tmpGa.length; i++) {
                if (Integer.valueOf(tmpGa[i]) > -13) {
                    returnGa += -13;
                } else {
                    returnGa += tmpGa[i];
                }
                returnGa += ",";
            }
        }

        return returnGa.substring(0, returnGa.length() - 1);
    }

    private static String getBandByIdx(String idx) {
        for (LteChannelCfg channel : CacheManager.getChannels()) {
            if (channel.getIdx().equals(idx)) {
                return channel.getBand();
            }
        }

        return "";
    }

    public static void setBlackList(String operation, String content) {
//        if(!CacheManager.isDeviceOk()){
//            return;
//        }

        //operation 1查询  2添加 3删除
        String configContent = operation + content;

        LogUtils.log("设置黑名单(中标)号码: " + configContent);

        LTE_PT_PARAM.setCommonParam(LTE_PT_PARAM.PARAM_SET_BLACK_NAMELIST, configContent);
    }

    //这个协议是用于rpt_rt_imsi而不是rpt_black_name
    public static void setRTImsi(boolean onOff) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        LogUtils.log("设置是否上报中标号码: " + (onOff ? "1" : "0"));

        LTE_PT_PARAM.setCommonParam(LTE_PT_PARAM.PARAM_SET_RT_IMSI, onOff ? "1" : "0");
    }


    public static void openAllRf() {
        LogUtils.log("开启所有射频");
        Set<String> set = new HashSet<>();

        for (LteChannelCfg channel : CacheManager.getChannels()) {
            if (!channel.isRFState()) {
                set.add(channel.getIp());
            }
        }

        for (String ip : set) {
            openRf(ip);
        }

    }

    public static void openRf(String ip) {
        LogUtils.log("开启射频ip:" + ip);
        LTESendManager.sendData(ip, LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_RF, "1");

    }

    public static void closeRf(String ip) {
        LogUtils.log("关闭射频ip:" + ip);
        LTESendManager.sendData(ip, LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_RF, "0");
    }

    public static void closeAllRf() {
        LogUtils.log("关闭所有射频");
        Set<String> set = new HashSet<>();

        for (LteChannelCfg channel : CacheManager.getChannels()) {
            if (channel.isRFState()) {
                set.add(channel.getIp());
            }
        }

        for (String ip : set) {
            closeRf(ip);
        }
    }

    public static void clearImsi() {
        LogUtils.log("清除定位名单");
        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_BLACKLIST, "BLACKLIST:000000000000000\r\n");

    }

    public static void setLocImsi(String imsi) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        LogUtils.log("设置定位名单：" + imsi);
//        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT,LTEMsgCode.SendCode.SET_LOCATION_IMSI,imsi);
        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_BLACKLIST, "BLACKLIST:" + imsi + "\r\n");
    }


    public static void setActiveMode() {
        LogUtils.log("设置定位模式");
        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_LOCATION_MODE, "2");
    }

    /**
     * 设置ftp
     */
    public static void setFTPConfig() {

        String configContent = NetWorkUtils.getWIFILocalIpAddress()
                + "#"
                + FtpConfig.ftpUser
                + "#"
                + FtpConfig.ftpPassword
                + "#"
                + FtpConfig.ftpPort
                + "#"
                + FtpConfig.ftpTimer
                + "#"
                + FtpConfig.ftpMaxSize;


        LogUtils.log("设置ftp:" + configContent);

        LTE_PT_PARAM.setCommonParam(LTE_PT_PARAM.PARAM_SET_FTP_CONFIG, configContent);
    }


    /**
     * 获取选中fcn
     */
    private static String getCheckedFcn(String ip) {
        try {
            DbManager dbManager = UCSIDBManager.getDbManager();
            DBChannel channel = dbManager.selector(DBChannel.class)
                    .where("ip", "=", ip)
                    .and("is_check", "=", 1)
                    .findFirst();
            if (channel != null) {
                return channel.getFcn();
            }

        } catch (DbException e) {
            e.printStackTrace();
        }

        return "";
    }


    public static void setFancontrol(String maxFanSpeed, String minFanSpeed, String tempThreshold) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        String configContent = "";

        if (!"".equals(minFanSpeed)) {
            configContent += "MIN_FAN:";
            configContent += minFanSpeed;
        }

        if (!"".equals(maxFanSpeed)) {
            configContent += "@MAX_FAN:";
            configContent += maxFanSpeed;
        }

        if (!"".equals(tempThreshold)) {
            configContent += "@FAN_TMPT:";
            configContent += tempThreshold;
        }

        LogUtils.log("设置风扇控制 " + configContent);

        LTE_PT_PARAM.setCommonParam(LTE_PT_PARAM.PARAM_SET_FAN, configContent);
    }

    public static void setAutoRF(boolean onOff) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }
        String ifAutoOpen = onOff ? "1" : "0";

        for (LteChannelCfg channel : CacheManager.getChannels()) {
            setChannelConfig(channel.getIdx(), "", "", "", "", "", ifAutoOpen, "");
        }
    }

    public static void scanFreq() {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        LTE_PT_PARAM.setCommonParam(LTE_PT_PARAM.PARAM_SET_SCAN_FREQ, "");
    }

    /**
     * @param ftpIP
     * @param ftpPort
     * @param username
     * @param password
     * @param fileName 固件版本升级
     */
    public static void systemUpgrade(String ftpIP, String ftpPort, String username, String password, String fileName) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        List<String> params = new ArrayList<>();


        if (!TextUtils.isEmpty(ftpIP)) {
            params.add("FTPIP:" + ftpIP);
        }

        if (!TextUtils.isEmpty(ftpPort)) {
            params.add("PORT:" + ftpPort);
        }

        if (!TextUtils.isEmpty(username)) {
            params.add("USERNAME:" + username);
        }


        if (!TextUtils.isEmpty(password)) {
            params.add("PASSWD:" + password);
        }

        if (!TextUtils.isEmpty(fileName)) {
            params.add("FWNAME:" + fileName);
        }


        String content = UtilDataFormatChange.encode(params);
        if (TextUtils.isEmpty(content)) {
            return;
        }

        LogUtils.log("版本更新：" + content);
        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_UPGRADE, content);
    }


    /**
     * 版本回退
     */
    public static void systemFallback() {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        LogUtils.log("版本回退");
        LTESendManager.sendData(LTEMsgCode.Type.APP_RPT, LTEMsgCode.SendCode.SET_FALLBACK, null);

    }

}
