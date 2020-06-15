package com.doit.net.Protocol;


import com.doit.net.application.MyApplication;
import com.doit.net.Data.LTESendManager;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.UtilDataFormatChange;
import com.doit.net.Utils.ToastUtils;

/**
 * Created by Zxc on 2018/10/18.
 */
public class LTE_PT_SYSTEM {
    public static final byte PT_SYSTEM = 0x04;

    public static final byte SYSTEM_REBOOT = 0x01;    //重启
    public static final byte SYSTEM_REBOOT_ACK = 0x02;
    public static final byte SYSTEM_UPGRADE = 0x03;  //设备升级
    public static final byte SYSTEM_UPGRADE_ACK = 0x04;
    public static final byte SYSTEM_GET_LOG = 0x09;  //获取设备日志
    public static final byte SYSTEM_GET_LOG_ACK = 0x0a;
    public static final byte SYSTEM_SET_DATETIME = 0x0d;  //设置时间
    public static final byte SYSTEM_SET_DATETIME_ASK = 0x0e;

    public static boolean commonSystemMsg(byte sysSubType) {
        com.doit.net.Protocol.LTESendPackage sendPackage = new com.doit.net.Protocol.LTESendPackage();
        //设置Sequence ID
        sendPackage.setPackageSequence(com.doit.net.Protocol.LTEProtocol.getSequenceID());
        //设置Session ID
        sendPackage.setPackageSessionID(com.doit.net.Protocol.LTEProtocol.getSessionID());
        //设置EquipType
        sendPackage.setPackageEquipType(com.doit.net.Protocol.LTEProtocol.equipType);
        //设置预留
        sendPackage.setPackageReserve((byte)0xFF);
        //设置主类型
        sendPackage.setPackageMainType(PT_SYSTEM);
        //设置子类型
        sendPackage.setPackageSubType(sysSubType);
        //设置校验位
        sendPackage.setPackageCheckNum(sendPackage.getCheckNum());

        //获取整体的包
        byte[] tempSendBytes=sendPackage.getPackageContent();
        return LTESendManager.sendData(tempSendBytes);
    }

    public static boolean setSystemParam(byte sysSubType, String paramContent) {
        LTESendPackage sendPackage=new LTESendPackage();
        //设置Sequence ID
        sendPackage.setPackageSequence(LTEProtocol.getSequenceID());
        //设置Session ID
        sendPackage.setPackageSessionID(LTEProtocol.getSessionID());
        //设置EquipType
        sendPackage.setPackageEquipType(LTEProtocol.equipType);
        //设置预留
        sendPackage.setPackageReserve((byte)0xFF);
        //设置主类型
        sendPackage.setPackageMainType(PT_SYSTEM);
        //设置子类型
        sendPackage.setPackageSubType(sysSubType);
        sendPackage.setByteSubContent(UtilDataFormatChange.stringtoBytesForASCII(paramContent));

        //设置校验位
        sendPackage.setPackageCheckNum(sendPackage.getCheckNum());

        //获取整体的包
        byte[] tempSendBytes=sendPackage.getPackageContent();
        return LTESendManager.sendData(tempSendBytes);
    }


    //处理系统内回复
    public static void processCommonSysResp(LTEReceivePackage receivePackage) {
        String respcContent = UtilDataFormatChange.bytesToString(receivePackage.getByteSubContent(),0);
        switch (receivePackage.getPackageSubType()){
            case LTE_PT_SYSTEM.SYSTEM_SET_DATETIME_ASK:
                LogUtils.log("设置时间成功");
                break;

            case LTE_PT_SYSTEM.SYSTEM_REBOOT_ACK:
                LogUtils.log("parseSystemReboot:" + respcContent);
                if (respcContent.charAt(0) == '0'){
                    LogUtils.log("设备正在重启");
//                    ToastUtils.showMessageLong(MyApplication.mContext,"设备正在重启");
                }else{
                    ToastUtils.showMessage(MyApplication.mContext,"设备重启失败");
                }
                break;

            case SYSTEM_UPGRADE_ACK:
                if (respcContent.charAt(0) == '0') {
                    LogUtils.log("升级包校验成功");
                    ToastUtils.showMessageLong(MyApplication.mContext, "升级包校验成功，请等待设备加载升级包（完整加载需要8-10分钟）");
                }else if (respcContent.charAt(0) == '1'){
                    LogUtils.log("升级包校验失败");
                    ToastUtils.showMessageLong(MyApplication.mContext, "升级包校验错误，请检查安装包");
                }
                break;

            case SYSTEM_GET_LOG_ACK:
                if (respcContent.charAt(0) == '0') {
                    LogUtils.log("获取设备日志成功");
                    ToastUtils.showMessageLong(MyApplication.mContext, "获取设备日志成功，保存在\"手机存储/4GHotspot/log\"目录下");
                }else if (respcContent.charAt(0) == '1'){
                    LogUtils.log("获取设备日志失败");
                    ToastUtils.showMessageLong(MyApplication.mContext, "获取设备日志失败");
                }
                break;

            default:
                break;
        }
    }
}
