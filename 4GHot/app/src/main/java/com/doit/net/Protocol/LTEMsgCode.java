package com.doit.net.Protocol;

/**
 * Author：Libin on 2020/6/10 10:25
 * Email：1993911441@qq.com
 * Describe：
 */
public final class LTEMsgCode {

    public LTEMsgCode() {
    }

    //APP下发指令协议代码
    public final static class SendCode {
        public static final String GET_SCAN = "01";   //公网环境参数
        public static final String SET_PARAM = "02";  //设置基站运行参数
        public static final String SET_POWER = "03"; //基站功率
        public static final String SET_RF = "04";  //开关射频
        public static final String SET_COLLECT_MODE = "05";  //设置采集模式
        public static final String REBOOT = "06";
        public static final String RESET = "07";
        public static final String GET_PARAM = "08";  //获取基站运行参数
        public static final String SET_REDIRECT = "09";
        public static final String SET_BLACKLIST = "10";
        public static final String SET_UPGRADE = "11";
        public static final String SET_RPT_STATE = "12";
        public static final String SET_LOG_FTP = "13";
        public static final String GET_PA_PARAM = "14";
        public static final String SET_REFUSE_REASON = "15";
        public static final String SET_SYNC_PARAM = "16";  //设置同步
        public static final String SET_TIME = "17";
        public static final String GET_CUSTOM_PA_PARAM = "18";
        public static final String SET_FALLBACK = "19";
        public static final String REBOOT_CELL = "20";
        public static final String SET_LOCATION_MODE = "26"; //设置定位模式
        public static final String SET_POLL_EARFCN = "31";  //频点设置
        public static final String SET_DANBING = "35";
        public static final String SET_LOCATION_IMSI = "36";   //设置定位目标名单

        public SendCode() {
        }
    }

    //基站主动上报协议代码
    public final static  class RptCode {
        public static final String RPT_HEART_BEAT = "01";  //基站上报心跳
        public static final String RPT_UEID = "02";   //基站上报设备信息
        public static final String RPT_LOC_DATA = "03";  //定位数据上报
        public static final String REPORT_CUSTOM_PA_CHANGE_STATE = "04";
        public static final String RPT_FCN_PRI = "05";

        public RptCode() {
        }
    }

    //类型
    public final static class Type {
        public static final String APP_RPT = "01";    //APP给基站下发指令
        public static final String STATION_ACK = "02";  //基站回复APP
        public static final String STATION_RPT = "03";  //基站给APP上报信息
        public static final String APP_ACK = "04";     //APP回复基站

        public Type() {
        }
    }
}
