package com.doit.net.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.doit.net.View.LocateChart;
import com.doit.net.View.LocateCircle;
import com.doit.net.base.BaseFragment;
import com.doit.net.bean.DeviceInfo;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.Event.EventAdapter;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.BlackBoxManger;
import com.doit.net.Model.CacheManager;
import com.doit.net.Model.VersionManage;
import com.doit.net.Utils.Cellular;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.UtilOperator;
import com.doit.net.bean.ReportBean;
import com.doit.net.ucsi.R;
import com.doit.net.Utils.ToastUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class LocationFragment extends BaseFragment implements EventAdapter.EventCall {
    private TextView tvLocatingImsi;
    private LocateChart vLocateChart;
    private LocateCircle vLocateCircle;
    private CheckBox cbGainSwitch;
    private CheckBox cbVoiceSwitch;
    private CheckBox cbLocSwitch;

    private List<Integer> listChartValue = new ArrayList<>();
    private final int LOCATE_CHART_X_AXIS_P_CNT = 15;       //图表横坐标点数
    private final int LOCATE_CHART_Y_AXIS_P_CNT = 25;       //图表纵坐标点数
    private String textContent = "搜寻未开始";

    private int currentSRSP = 0;
    private int lastRptSRSP = 60;//初始平滑地开始
    private static boolean isOpenVoice = true;
    private Timer speechTimer = null;
    private final int BROADCAST_PERIOD = 1900;
    private long lastLocRptTime = 0;
    private int LOC_RPT_TIMEOUT = 5 * 1000;  //多长时间没上报就开始播报“正在搜寻”
    private int UPDATE_ARFCN_TIMEOUT = 2 * 60 * 1000;  //多长时间没上报就更新频点
    private final int MAX_DEVIATION = 16;   //强度与上次上报偏差大于这个值就重新计算

    private String lastLocateIMSI = "";
    private boolean startLoc = false;  //开始定位后数据上报前，不监听射频状态

    //handler消息
    private final int UPDATE_VIEW = 0;
    private final int LOC_REPORT = 1;
    private final int STOP_LOC = 3;
    private final int REFRESH_DEVICE = 4;
    private final int RF_STATUS_LOC = 5;
    private final int ADD_LOCATION = 6;

    public LocationFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.doit_layout_location, container, false);
        tvLocatingImsi = rootView.findViewById(R.id.tvLocatingImsi);
        vLocateChart = rootView.findViewById(R.id.vLocateChart);
        vLocateCircle = rootView.findViewById(R.id.vLocateCircle);
        cbVoiceSwitch = rootView.findViewById(R.id.cbVoiceSwitch);
        cbVoiceSwitch.setOnCheckedChangeListener(voiceSwitchListener);
        cbGainSwitch = rootView.findViewById(R.id.cbGainSwitch);

        cbGainSwitch.setOnCheckedChangeListener(gainSwitchListener);
        cbLocSwitch = rootView.findViewById(R.id.cbLocSwitch);

        initView();
        initEvent();
        return rootView;
    }


    private void initEvent() {
        EventAdapter.register(EventAdapter.REFRESH_DEVICE, this);
        EventAdapter.register(EventAdapter.RF_STATUS_LOC, this);
        EventAdapter.register(EventAdapter.LOCATION_RPT, this);
        EventAdapter.register(EventAdapter.ADD_LOCATION, this);
        EventAdapter.register(EventAdapter.STOP_LOC, this);

    }

    private void initView() {

        cbLocSwitch.setOnCheckedChangeListener(rfLocSwitchListener);

        vLocateChart.setCylinderCount(LOCATE_CHART_X_AXIS_P_CNT);
        vLocateChart.setMaxPointCntInClder(LOCATE_CHART_Y_AXIS_P_CNT);
        resetLocateChartValue();

    }


    private void startSpeechBroadcastLoop() {
        if (speechTimer == null) {
            speechTimer = new Timer();
            speechTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //脱标
                    if (lastLocRptTime != 0 && (int) (System.currentTimeMillis() - lastLocRptTime) > LOC_RPT_TIMEOUT) {
                        currentSRSP = 0;
                        resetLocateChartValue();
                        refreshPage();
                        ProtocolManager.openAllRf();
                    }

                    if (currentSRSP == 0) {
                        speech("正在搜寻");
                    } else {
                        speech("" + currentSRSP);
                    }
                }
            }, 4000, BROADCAST_PERIOD);
        }

    }


    private void stopSpeechBroadcastLoop() {
        if (speechTimer != null) {
            speechTimer.cancel();
            speechTimer = null;
        }
    }

    private void refreshPage() {
        if (CacheManager.getCurrentLoction() == null) {
            return;
        }

        mHandler.sendEmptyMessage(UPDATE_VIEW);
    }

    private void updateLocateChart() {
        int[] chartDatas = new int[LOCATE_CHART_X_AXIS_P_CNT];

        for (int i = 0; i < LOCATE_CHART_X_AXIS_P_CNT; i++) {
            chartDatas[i] = listChartValue.get(i);
        }
        vLocateChart.updateChart(chartDatas);
    }

    private int correctSRSP(int srspRptValue) {
        //srsp = (srspRptValue-234)/10  旧的算法
        int srsp = 130 - srspRptValue;

        if (srsp <= 0)
            srsp = 0;

        if (srsp > 100)
            srsp = 100;

        if (Math.abs(srsp - lastRptSRSP) > MAX_DEVIATION) {
            srsp = (lastRptSRSP + srsp) / 2;
        }

        return srsp;
    }

    void startLoc() {
        if (!CacheManager.getLocState()) {
            startSpeechBroadcastLoop();
            textContent = "正在搜寻" + CacheManager.getCurrentLoction().getImsi();
            CacheManager.startLoc(CacheManager.getCurrentLoction().getImsi());
            refreshPage();

            startLoc = true;
        }
    }

    void addLocation(String imsi) {
        LogUtils.log("##########  addLocation:" + imsi + "  ###########");

        if (!"".equals(lastLocateIMSI) && !lastLocateIMSI.equals(imsi)) {   //更换目标
            textContent = "正在搜寻" + imsi;
            speech("搜寻目标更换");
            currentSRSP = 0;
            lastRptSRSP = 0;
            resetLocateChartValue();
            refreshPage();
            startSpeechBroadcastLoop();  //从停止定位的状态添加定位，故语音手动再次开启
            startLoc = true;
        }
    }


    private void stopLoc() {
        LogUtils.log("call stopLoc() in locationFragment... ...");
        if (CacheManager.getLocState()) {
            CacheManager.stopCurrentLoc();
            stopSpeechBroadcastLoop();
            textContent = "搜寻暂停：" + CacheManager.getCurrentLoction().getImsi();
            currentSRSP = 0;
            resetLocateChartValue();
        }

        lastLocRptTime = 0;

        refreshPage();
    }

    private void resetLocateChartValue() {
        listChartValue.clear();

        for (int i = 0; i < LOCATE_CHART_X_AXIS_P_CNT; i++) {
            listChartValue.add(0);
        }
    }

    void speech(String content) {
        if (isOpenVoice)
            EventAdapter.call(EventAdapter.SPEAK, content);
    }

    CompoundButton.OnCheckedChangeListener voiceSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            if (!view.isPressed())
                return;

            isOpenVoice = isChecked;
        }
    };

    CompoundButton.OnCheckedChangeListener gainSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            if (!view.isPressed())
                return;

            if (!CacheManager.checkDevice(getContext())) {
                cbGainSwitch.setChecked(!cbGainSwitch.isChecked());
                return;
            }

            if (isChecked) {
                CacheManager.setHighGa(true);
            } else {
                CacheManager.setHighGa(false);
            }

            ToastUtils.showMessageLong("增益设置已下发，请等待其生效");
            EventAdapter.call(EventAdapter.SHOW_PROGRESS, 8000);
        }
    };

    CompoundButton.OnCheckedChangeListener rfLocSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (!compoundButton.isPressed()) {
                return;
            }

            if (!CacheManager.checkDevice(getContext())) {
                cbLocSwitch.setChecked(!isChecked);
                return;
            }

            if (!isChecked) {
                if (CacheManager.getLocState()) {  //手动关闭定位，需要打开射频
                    ProtocolManager.openAllRf();
                }
                stopLoc();
                EventAdapter.call(EventAdapter.SHOW_PROGRESS, 8000);
                if (CacheManager.currentLoction != null && !CacheManager.currentLoction.getImsi().equals("")) {
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.STOP_LOCALTE + CacheManager.currentLoction.getImsi());
                }
            } else {
                if (CacheManager.currentLoction == null || CacheManager.currentLoction.getImsi().equals("")) {
                    ToastUtils.showMessage(R.string.button_loc_unstart);
                } else {

                    ProtocolManager.openAllRf();
                    startLoc();
                    EventAdapter.call(EventAdapter.SHOW_PROGRESS, 8000);
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.START_LOCALTE + CacheManager.currentLoction.getImsi());
                }
            }

        }
    };


    @Override
    public void onFocus() {
        refreshPage();
    }

    /**
     * 射频是否开启
     */
    private void isRFOpen() {

        if (!CacheManager.getLocState()) {
            return;
        }
        for (LteChannelCfg channel : CacheManager.getChannels()) {
            if (channel.isRFState()) {
                return;
            }
        }

        stopLoc();
    }

    @Override
    public void call(String key, Object val) {
        if (key.equals(EventAdapter.LOCATION_RPT)) {
            try {
                Message msg = new Message();
                msg.what = LOC_REPORT;
                msg.obj = val;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (key.equals(EventAdapter.ADD_LOCATION)) {
            try {
                Message msg = new Message();
                msg.what = ADD_LOCATION;
                msg.obj = val;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (key.equals(EventAdapter.STOP_LOC)) {
            try {
                Message msg = new Message();
                msg.what = STOP_LOC;
                msg.obj = val;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (key.equals(EventAdapter.REFRESH_DEVICE)) {
            mHandler.sendEmptyMessage(REFRESH_DEVICE);
        } else if (key.equals(EventAdapter.RF_STATUS_LOC)) {
            mHandler.sendEmptyMessage(RF_STATUS_LOC);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_VIEW) {    //设置定位名单更新界面
                tvLocatingImsi.setText(textContent);
                vLocateCircle.setValue(currentSRSP);
                updateLocateChart();

                cbLocSwitch.setOnCheckedChangeListener(null);
                if (CacheManager.getCurrentLoction().isLocateStart()) {
                    cbLocSwitch.setChecked(true);
                } else {
                    cbLocSwitch.setChecked(false);
                }
                cbLocSwitch.setOnCheckedChangeListener(rfLocSwitchListener);
            } else if (msg.what == LOC_REPORT) {   //定位实时上报
                if (CacheManager.getCurrentLoction() != null && CacheManager.getCurrentLoction().isLocateStart()) {
                    ReportBean reportBean = (ReportBean) msg.obj;
                    currentSRSP = correctSRSP(Integer.valueOf(reportBean.getRssi()));
                    if (currentSRSP == 0)
                        return;

                    lastLocRptTime = new Date().getTime();
                    lastRptSRSP = currentSRSP;

                    listChartValue.add(currentSRSP / 4);
                    listChartValue.remove(0);
                    textContent = "正在搜寻" + CacheManager.getCurrentLoction().getImsi();

                    refreshPage();

                    //定位数据上报后，只开启对应板卡射频
                    String ip = reportBean.getIp();
                    if (TextUtils.isEmpty(ip)) {
                        ProtocolManager.openAllRf();
                    } else {
                        for (DeviceInfo deviceInfo : CacheManager.deviceList) {
                            if (deviceInfo.getIp().equals(ip)) {
                                ProtocolManager.openRf(ip);
                            } else {
                                ProtocolManager.closeRf(deviceInfo.getIp());
                            }
                        }

                    }

                    startLoc = false;
                }
            } else if (msg.what == STOP_LOC) {
                stopLoc();
            } else if (msg.what == ADD_LOCATION) {
                addLocation((String) msg.obj);
            } else if (msg.what == REFRESH_DEVICE) {
                if (CacheManager.channels != null && CacheManager.channels.size() > 0) {

                    cbGainSwitch.setOnCheckedChangeListener(null);
                    boolean isHighPa = true;
                    for (LteChannelCfg channel : CacheManager.channels) {
                        int pa = Integer.parseInt(channel.getPa());
                        if (pa <= 10) {
                            isHighPa = false;
                            break;
                        }
                    }
                    cbGainSwitch.setChecked(isHighPa);
                    cbGainSwitch.setOnCheckedChangeListener(gainSwitchListener);
                }
            } else if (msg.what == RF_STATUS_LOC) {
                if (!startLoc) {
                    isRFOpen();
                }

            }
        }
    };
}