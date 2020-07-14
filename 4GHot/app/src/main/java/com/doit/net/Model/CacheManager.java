/*
 * Copyright (C) 2011-2016 dshine.com.cn
 * All rights reserved.
 * ShangHai Dshine - http://www.dshine.com.cn
 */
package com.doit.net.Model;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.doit.net.application.MyApplication;
import com.doit.net.bean.DeviceInfo;
import com.doit.net.bean.DeviceState;
import com.doit.net.bean.LocationBean;
import com.doit.net.bean.LocationRptBean;
import com.doit.net.bean.LteCellConfig;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.bean.LteEquipConfig;
import com.doit.net.bean.Namelist;
import com.doit.net.bean.ScanFreqRstBean;
import com.doit.net.bean.UeidBean;
import com.doit.net.Event.EventAdapter;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.Utils.LogUtils;
import com.doit.net.udp.g4.bean.G4MsgChannelCfg;

import org.apache.commons.lang3.math.NumberUtils;
import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author 杨维(wiker)
 * @version 1.0
 * @date 2016-4-26 下午3:37:39
 */
public class CacheManager {

    public static List<UeidBean> realtimeUeidList = new ArrayList<>();
    public static final int MAX_REALTIME_LIST_SIZE = 300;


    public static List<LocationBean> locations = new ArrayList<>();
    public static LocationBean currentLoction = null;
    public static List<LocationRptBean> locationRpts = new ArrayList<>();

    public static Namelist namelist = new Namelist();

    public static long last_heart_time;

    public static String currentWorkMode = "2";   //0公安侦码  2军队管控

    public static DeviceState deviceState = new DeviceState();

    public static List<ScanFreqRstBean> listLastScanFreqRst = new ArrayList<>();

    public static boolean loc_mode = false;  //是否开启搜寻功能

    public static boolean isWifiConnected = false;

    private static boolean hasPressStartButton = false;  //是否已经在主页面点击开始按钮

    public static boolean checkLicense = false; //连接成功后校验证书

    private static LteCellConfig cellConfig;
    private static LteEquipConfig equipConfig;
    public static List<LteChannelCfg> channels = new ArrayList<>();
    public static List<DeviceInfo>  deviceList = new ArrayList<>();
    public static Map<String,String>  fcnMap = new HashMap<>();   //FDD、TDD默认轮询频点

    public static byte[] magic;   //协议默认字段 00 FF FF 00


    public static boolean getLocMode() {
        return loc_mode;
    }

    public static void setLocMode(boolean locMode) {
        loc_mode = locMode;
    }

    public synchronized static void addRealtimeUeidList(List<UeidBean> listUeid) {
        addToList(listUeid);

        /* 如果实时上报界面没加载就有数据上传，就会丢失数据
           所以将存储数据库操作移到processUeidRpt */
//        try {
//            DbManager dbManager = UCSIDBManager.getDbManager();
//            for (int i= 0; i < listUeid.size(); i++){
//                DBUeidInfo info = new DBUeidInfo();
//                info.setImsi(listUeid.get(i).getImsi());
//                info.setTmsi(listUeid.get(i).getTmsi());
//                //info.setCreateDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(listUeid.get(i).getRptTime()));
//                info.setCreateDate(DateUtil.convert2long(listUeid.get(i).getRptTime(), DateUtil.LOCAL_DATE));
//                info.setLongitude(listUeid.get(i).getLongitude());
//                info.setLatitude(listUeid.get(i).getLatitude());
//                dbManager.save(info);
//            }
//        } catch (DbException e) {
//            log.error("插入UEID 到数据库异常",e);
//        }
    }

    public synchronized static void addToList(List<UeidBean> listUeid) {
        if ((realtimeUeidList.size() + listUeid.size()) >= MAX_REALTIME_LIST_SIZE) {
            for (int i = 0; i < (realtimeUeidList.size() + listUeid.size() - MAX_REALTIME_LIST_SIZE); i++)
                //realtimeUeidList.remove(realtimeUeidList.size()-1);
                realtimeUeidList.remove(0);
        }

        //最新的放前面
        Collections.reverse(listUeid);
        realtimeUeidList.addAll(0, listUeid);
    }

    public static boolean hasPressStartButton() {
        return hasPressStartButton;
    }

    public static void setPressStartButtonFlag(boolean flag) {
        hasPressStartButton = flag;
    }

    public static void updateLoc(String imsi) {
        if (currentLoction == null) {
            currentLoction = new LocationBean();
        }
        PrefManage.setImsi(imsi);
        currentLoction.setImsi(imsi);
    }

    public static void setCurrentBlackList() {
        List<DBBlackInfo> listBlackList = null;
        try {
            listBlackList = UCSIDBManager.getDbManager().selector(DBBlackInfo.class).findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }

        if (listBlackList == null || listBlackList.size() == 0)
            return;

        String content = "";
        for (DBBlackInfo dbBlackInfo : listBlackList) {
            content += "#";
            content += dbBlackInfo.getImsi();
        }

        ProtocolManager.setBlackList("2", content);
    }


    public static void startLoc(String imsi) {

        ProtocolManager.setLocImsi(imsi);

        CacheManager.getCurrentLoction().setLocateStart(true);
    }



    public static String getSimIMSI(int simid) {
        TelephonyManager telephonyManager = (TelephonyManager) MyApplication.mContext.getSystemService(Context.TELEPHONY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
            return "";

        int[] subId = null;//SubscriptionManager.getSubId(simid);
        Class<?> threadClazz = null;
        threadClazz = SubscriptionManager.class;

        try {
            Method method = threadClazz.getDeclaredMethod("getSubId", int.class);
            method.setAccessible(true);
            subId = (int[]) method.invoke(null, simid);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        int sub = -1;
        if (Build.VERSION.SDK_INT >= 24) {
            sub = (subId != null) ? subId[0] : SubscriptionManager.getDefaultSubscriptionId();
        } else {
            try {
                Method method = threadClazz.getDeclaredMethod("getDefaultSubId");
                method.setAccessible(true);
                sub = (subId != null) ? subId[0] : (Integer) method.invoke(null, (Object[]) null);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        String IMSI = "";
        if (sub != -1) {
            Class clazz = telephonyManager.getClass();
            try {
                Method method = clazz.getDeclaredMethod("getSubscriberId", int.class);
                method.setAccessible(true);
                IMSI = (String) method.invoke(telephonyManager, sub);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return IMSI;
    }


    public static void clearCurrentBlackList() {
        List<DBBlackInfo> listBlackList = null;
        try {
            listBlackList = UCSIDBManager.getDbManager().selector(DBBlackInfo.class).findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }

        if (listBlackList == null || listBlackList.size() == 0)
            return;

        String content = "";
        for (DBBlackInfo dbBlackInfo : listBlackList) {
            content += "#";
            content += dbBlackInfo.getImsi();
        }

        ProtocolManager.setBlackList("3", content);
    }

    public static void stopCurrentLoc() {

        ProtocolManager.clearImsi();

        if (CacheManager.getCurrentLoction() != null)
            CacheManager.getCurrentLoction().setLocateStart(false);
        if (VersionManage.isPoliceVer()) {
            setCurrentBlackList();   //如果定位结束后短时间内黑名单连续上报，尝试延时下发
        }
    }

    public static boolean getLocState() {
        if (currentLoction == null)
            return false;

        return currentLoction.isLocateStart();
    }

    public static LocationBean getCurrentLoction() {
        return currentLoction;
    }

    public static LocationRptBean getCurrentLocRptBean() {
        if (locationRpts == null) {
            return null;
        }
        if (locationRpts.size() - 1 < 0) {
            return null;
        }
        return locationRpts.get(locationRpts.size() - 1);
    }


    /**
     * 检查设备是否连接，并提示
     *
     * @param context
     * @return
     */
    public static boolean checkDevice(Context context) {
        if (!isDeviceOk()) {
            new MySweetAlertDialog(context, MySweetAlertDialog.ERROR_TYPE)
                    .setTitleText("错误")
                    .setContentText("设备未就绪")
                    .show();
            return false;
        }
        return true;
    }

    public static boolean isDeviceOk() {
        return channels.size() > 1;
    }

    /**
     * 重置一下状态，一般设备需要重启时调用
     */
    public static void resetState() {
        LogUtils.log("reset state.");
        //deviceInfo = null;
        channels.clear();
        deviceList.clear();
        fcnMap.clear();

    }

    /**
     * socket断开,删除设备信息
     */
    public static void removeEquip(String ip) {
        LogUtils.log("断开连接ip:" + ip);
        //deviceInfo = null;
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).getIp().equals(ip)) {
                channels.remove(i);
                break;
            }
        }

        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getIp().equals(ip)){
                deviceList.remove(i);
                break;
            }
        }

    }



    public static LteCellConfig getCellConfig() {
        return cellConfig;
    }

    public static LteEquipConfig getLteEquipConfig() {
        return equipConfig;
    }

    public static List<LteChannelCfg> getChannels() {
        return channels;
    }

    public static void setCellConfig(LteCellConfig cellConfig) {
        CacheManager.cellConfig = cellConfig;
    }

    public static void setEquipConfig(LteEquipConfig equipConfig) {
        CacheManager.equipConfig = equipConfig;
    }

    public static void setNamelist(Namelist list) {
        namelist = list;
    }

    public synchronized static void addChannel(LteChannelCfg cfg) {

        for (LteChannelCfg channel : channels) {
            if (channel.getIp().equals(cfg.getIp())) {
                channel.setPci(cfg.getPci());
                channel.setBand(cfg.getBand());
                channel.setPlmn(cfg.getPlmn());
                channel.setFcn(cfg.getFcn());
                channel.setRFState(cfg.isRFState());
                channel.setPa(cfg.getPa());
                channel.setGain(cfg.getGain());
                channel.setRxGain(cfg.getRxGain());
                channel.setGpsOffset(cfg.getGpsOffset());
                channel.setGps(cfg.getGps());
                channel.setFrmOfs(cfg.getFrmOfs());
                channel.setCnm(cfg.getCnm());
                channel.setRlm(cfg.getRlm());
                channel.setPollTmr(cfg.getPollTmr());
                channel.setTac(cfg.getTac());
                return;
            }
        }

        channels.add(cfg);
        Collections.sort(channels, new Comparator<LteChannelCfg>() {
            @Override
            public int compare(LteChannelCfg lhs, LteChannelCfg rhs) {
                return NumberUtils.toInt(lhs.getPlmn()) - NumberUtils.toInt(rhs.getPlmn());
            }
        });
    }

    public synchronized static void addDevice(String ip,byte[] deviceName,String fw) {
        for (DeviceInfo deviceInfo : deviceList) {
            if (deviceInfo.getIp().equals(ip)){
                deviceInfo.setDeviceName(deviceName);
                deviceInfo.setFw(fw);
                return;
            }
        }

        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setIp(ip);
        deviceInfo.setDeviceName(deviceName);
        deviceInfo.setFw(fw);
        deviceList.add(deviceInfo);

    }

    //将RF状态更新到内存
    public synchronized static void updateRFState(String band, boolean rf) {
        //UtilBaseLog.printLog(idx + "    size:" + channels.size() + "    " + rf);
        for (LteChannelCfg channel : channels) {
            if (channel.getBand().equals(band)) {
                channel.setRFState(rf);
                break;
            }
        }
    }

    public static LteChannelCfg getChannelByIdx(String idx) {
        for (LteChannelCfg channel : channels) {
            if (channel.getIdx().equals(idx)) {
                return channel;
            }
        }
        return null;
    }

    private static Map<String, List<G4MsgChannelCfg>> userChannels = new HashMap<>();

    public static void addUserChannel(G4MsgChannelCfg cfg) {
        if (userChannels.containsKey(cfg.getIdx())) {
            userChannels.get(cfg.getIdx()).add(cfg);
        } else {
            List<G4MsgChannelCfg> list = new ArrayList<>();
            list.add(cfg);
            userChannels.put(cfg.getIdx(), list);
        }
    }

    public static void updateWhitelistToDev(Context context) {
        /*
        * 考虑到白名单数量巨大时严重影响设备使用，决定不再下发白名单给设备，只做特殊显示
        List<WhiteListInfo> listWhitelist = null;
        String content = "";
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String mobileImsi =  StringUtils.defaultIfBlank(telManager.getSubscriberId(), "");
        //UtilBaseLog.printLog("######" + telManager.getSubscriberId());
        try {
            listWhitelist = UCSIDBManager.getDbManager().selector(WhiteListInfo.class).findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }

        if (listWhitelist == null || listWhitelist.size() == 0){
            if (!"".equals(mobileImsi)){
                content = mobileImsi;
            }
            //ProtocolManager.setNamelist("", "", "", "","","block");
        }else{
            for (WhiteListInfo whiteListInfo : listWhitelist) {
                if ("".equals(whiteListInfo.getImsi()))
                    continue;

                content += whiteListInfo.getImsi();
                content += ",";
            }

            if ("".equals(mobileImsi)){
                content = content.substring(0, content.length()-1);
            }else{
                content = content+mobileImsi;
            }
        }

        ProtocolManager.setNamelist("", content, "", "","","block");
        */
    }

    public static void setHighGa(boolean on_off) {
        if (on_off) {
            for (LteChannelCfg channel : channels) {
                ProtocolManager.setPa(channel.getIp(), "40");
            }
        } else {
            for (LteChannelCfg channel : channels) {
                ProtocolManager.setPa(channel.getIp(), "10");
            }
        }
    }



    /*
        删除列表里已存在的ueid
        成功删除返回ture,没有删除（即不存在）返回false
     */
    public static synchronized boolean removeExistUeidInRealtimeList(String imsi) {
        for (int i = 0; i < realtimeUeidList.size(); i++) {
            if (realtimeUeidList.get(i).getImsi().equals(imsi)) {
                realtimeUeidList.remove(i);
                return true;
            }
        }

        return false;
    }


}
