package com.doit.net.activity;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.os.Bundle;

import com.doit.net.View.ChannelsDialog;
import com.doit.net.View.SetScanFcnDialog;
import com.doit.net.View.SystemSetupDialog;
import com.doit.net.adapter.UserChannelListAdapter;
import com.doit.net.base.BaseActivity;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.Utils.LogUtils;
import com.doit.net.ucsi.R;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.doit.net.bean.LteChannelCfg;
import com.doit.net.Model.BlackBoxManger;
import com.doit.net.Event.EventAdapter;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.CacheManager;

import com.doit.net.Utils.LSettingItem;
import com.doit.net.Utils.ToastUtils;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ListHolder;
import com.orhanobut.dialogplus.OnItemClickListener;

import java.util.Timer;
import java.util.TimerTask;


/**
 * 设备参数
 */
public class DeviceParamActivity extends BaseActivity implements EventAdapter.EventCall {
    private final Activity activity = this;
    private LinearLayout layoutChannelList;


    private Button btSetChannelCfg;
    private Button btUpdateTac;
    private Button btRebootDevice;
    private Button btRefreshParam;
    private Button btnScanFcn;
    private long lastRefreshParamTime = 0; //防止频繁刷新参数

    private RadioGroup rgPowerLevel;
    private RadioButton rbPowerHigh;
    private RadioButton rbPowerMedium;
    private RadioButton rbPowerLow;
    private final int POWER_LEVEL_HIGH = 40;
    private final int POWER_LEVEL_MEDIUM = 25;
    private final int POWER_LEVEL_LOW = 10;
    private RadioButton lastPowerPress;

    private RadioGroup rgDetectCarrierOperate;
    private RadioButton rbDetectAll;
    private RadioButton rbCTJ;
    private RadioButton rbCTU;
    private RadioButton rbCTC;
    private RadioButton lastDetectCarrierOperatePress;

    private CheckBox cbRFSwitch;

    private MySweetAlertDialog mProgressDialog;
    private Handler checkHandler = new Handler();
    private boolean refreshViewEnable = true;   //在设置需要长时间回馈的参数时，禁止界面更新

    //handler消息
    private final int UPDATE_VIEW = 0;
    private final int SHOW_PROGRESS = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_param);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        intView();
        initEvent();
    }

    private void initEvent() {
        EventAdapter.register(EventAdapter.REFRESH_DEVICE,this);
    }

    private void intView() {

        btSetChannelCfg = findViewById(R.id.btSetChannelCfg);
        btSetChannelCfg.setOnClickListener(setChannelCfgClick);
        btnScanFcn = findViewById(R.id.btn_scan_fcn);
        btnScanFcn.setOnClickListener(setScanFcnClick);
        btUpdateTac = findViewById(R.id.btUpdateTac);
        btUpdateTac.setOnClickListener(updateTacClick);
        btRebootDevice = findViewById(R.id.btRebootDevice);
        btRebootDevice.setOnClickListener(rebootDeviceClick);
        btRefreshParam = findViewById(R.id.btRefreshParam);
        btRefreshParam.setOnClickListener(refreshParamClick);

        rgPowerLevel = findViewById(R.id.rgPowerLevel);
        rbPowerHigh = findViewById(R.id.rbPowerHigh);
        rbPowerMedium = findViewById(R.id.rbPowerMedium);
        rbPowerLow = findViewById(R.id.rbPowerLow);
        lastPowerPress = rbPowerHigh;
        rgPowerLevel.setOnCheckedChangeListener(powerLevelListener);

        rgDetectCarrierOperate = findViewById(R.id.rgDetectCarrierOperate);
        rbDetectAll = findViewById(R.id.rbDetectAll);
        rbCTJ = findViewById(R.id.rbCTJ);
        rbCTU = findViewById(R.id.rbCTU);
        rbCTC = findViewById(R.id.rbCTC);
        lastDetectCarrierOperatePress = rbDetectAll;
        rgDetectCarrierOperate.setOnCheckedChangeListener(detectCarrierOperateListener);

        cbRFSwitch = findViewById(R.id.cbRFSwitch);
        cbRFSwitch.setOnCheckedChangeListener(rfCheckChangeListener);

        layoutChannelList = findViewById(R.id.id_channel_list);

        mProgressDialog = new MySweetAlertDialog(activity, MySweetAlertDialog.PROGRESS_TYPE);
        mProgressDialog.setTitleText("Loading...");
        mProgressDialog.setCancelable(false);
    }

    View.OnClickListener setCellParamClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(activity)) {
                return;
            }
            new SystemSetupDialog(activity).show();
        }
    };

    View.OnClickListener setChannelCfgClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(activity)) {
                return;
            }

            new ChannelsDialog(activity).show();
        }
    };

    View.OnClickListener setScanFcnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(activity)) {
                return;
            }

            new SetScanFcnDialog(activity).show();
        }
    };

    View.OnClickListener updateTacClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(activity)) {
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

            //ToastUtils.showMessage(GameApplication.appContext,"下发更新TAC成功");
            EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CHANNEL_TAG);
        }
    };

    View.OnClickListener rebootDeviceClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(activity)) {
                return;
            }

            new MySweetAlertDialog(activity, MySweetAlertDialog.WARNING_TYPE)
                    .setTitleText("设备重启")
                    .setContentText("确定重启设备")
                    .setCancelText(getString(R.string.cancel))
                    .setConfirmText(getString(R.string.sure))
                    .showCancelButton(true)
                    .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {

                        @Override
                        public void onClick(MySweetAlertDialog sweetAlertDialog) {
                            sweetAlertDialog.dismiss();
                            ProtocolManager.reboot();
                            ToastUtils.showMessage("设备即将重启");
                        }
                    })
                    .show();
        }
    };

    View.OnClickListener refreshParamClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(activity)) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRefreshParamTime > 20 * 1000) {
                ProtocolManager.getEquipAndAllChannelConfig();
                lastRefreshParamTime = currentTime;
                ToastUtils.showMessage("下发查询参数成功！");
            } else {
                ToastUtils.showMessage("请勿频繁刷新参数！");
            }
        }
    };

    RadioGroup.OnCheckedChangeListener powerLevelListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (!(group.findViewById(checkedId).isPressed())) {
                return;
            }

            if (!CacheManager.checkDevice(activity)) {
                lastPowerPress.setChecked(true);
                return;
            }

            refreshViewEnable = false;

            if (CacheManager.getLocState()) {
                ToastUtils.showMessage("当前正在搜寻中，请留意功率变动是否对其产生影响！");
            } else {
                ToastUtils.showMessageLong("功率设置已下发，请等待其生效");
            }

            switch (checkedId) {
                case R.id.rbPowerHigh:
                    //ProtocolManager.setAllPower(String.valueOf(-5*POWER_LEVEL_HIGH));
                    setPowerLevel(POWER_LEVEL_HIGH);
                    lastPowerPress = rbPowerHigh;
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.SET_ALL_POWER + "高");
                    break;

                case R.id.rbPowerMedium:
                    setPowerLevel(POWER_LEVEL_MEDIUM);
                    lastPowerPress = rbPowerMedium;
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.SET_ALL_POWER + "中");
                    break;

                case R.id.rbPowerLow:
                    setPowerLevel(POWER_LEVEL_LOW);
                    lastPowerPress = rbPowerLow;
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.SET_ALL_POWER + "低");
                    break;
            }

            showProcess(8000);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ProtocolManager.getEquipAndAllChannelConfig();
                    refreshViewEnable = true;
                }
            }, 8000);
        }
    };

    RadioGroup.OnCheckedChangeListener detectCarrierOperateListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (!(group.findViewById(checkedId).isPressed())) {
                return;
            }

            if (!CacheManager.checkDevice(activity)) {
                lastDetectCarrierOperatePress.setChecked(true);
                return;
            }

            if (CacheManager.getLocState()) {
                ToastUtils.showMessage("当前正在定位中，无法切换侦码制式！");
                lastDetectCarrierOperatePress.setChecked(true);
                return;
            } else {
                ToastUtils.showMessageLong("侦码制式设置已下发，请等待其生效");
            }

            refreshViewEnable = false;

            switch (checkedId) {
                case R.id.rbDetectAll:
                    ProtocolManager.setDetectCarrierOpetation("detect_all");
                    lastDetectCarrierOperatePress = rbDetectAll;
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CHANGE_DETTECT_OPERATE + "所有");
                    break;

                case R.id.rbCTJ:
                    ProtocolManager.setDetectCarrierOpetation("detect_ctj");
                    lastDetectCarrierOperatePress = rbCTJ;
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CHANGE_DETTECT_OPERATE + "移动");
                    break;

                case R.id.rbCTU:
                    ProtocolManager.setDetectCarrierOpetation("detect_ctu");
                    lastDetectCarrierOperatePress = rbCTU;
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CHANGE_DETTECT_OPERATE + "联通");
                    break;

                case R.id.rbCTC:
                    ProtocolManager.setDetectCarrierOpetation("detect_ctc");
                    lastDetectCarrierOperatePress = rbCTC;
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CHANGE_DETTECT_OPERATE + "电信");
                    break;
            }

            showProcess(10000);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ProtocolManager.getEquipAndAllChannelConfig();
                    refreshViewEnable = true;
                }
            }, 10000);
        }
    };


    CompoundButton.OnCheckedChangeListener rfCheckChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (!compoundButton.isPressed()) {
                return;
            }

            if (!CacheManager.checkDevice(activity)) {
                cbRFSwitch.setChecked(!isChecked);
                return;
            }

            refreshViewEnable = false;

            if (isChecked) {
                ProtocolManager.openAllRf();
                ToastUtils.showMessageLong(R.string.rf_open);
                EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.OPEN_ALL_RF);
                showProcess(6000);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        refreshViewEnable = true;
                        //refreshViews();
                        ProtocolManager.getEquipAndAllChannelConfig();
                    }
                }, 6000);
            } else {
                if (CacheManager.getLocState()) {
                    new MySweetAlertDialog(activity, MySweetAlertDialog.WARNING_TYPE)
                            .setTitleText("提示")
                            .setContentText("当前正在搜寻，确定关闭吗？")
                            .setCancelText(getString(R.string.cancel))
                            .setConfirmText(getString(R.string.sure))
                            .showCancelButton(true)
                            .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {

                                @Override
                                public void onClick(MySweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismiss();
                                    ProtocolManager.closeAllRf();
                                    ToastUtils.showMessage(R.string.rf_close);
                                    showProcess(6000);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            refreshViewEnable = true;
                                            //refreshViews();
                                            ProtocolManager.getEquipAndAllChannelConfig();
                                        }
                                    }, 6000);
                                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CLOSE_ALL_RF);
                                }
                            })
                            .show();
                } else {
                    ProtocolManager.closeAllRf();
                    ToastUtils.showMessageLong( R.string.rf_close);
                    showProcess(6000);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            refreshViewEnable = true;
                            //refreshViews();
                            ProtocolManager.getEquipAndAllChannelConfig();
                        }
                    }, 6000);
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CLOSE_ALL_RF);
                }

            }
        }
    };

    public void setPowerLevel(int powerLevel) {
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        for (LteChannelCfg channel : CacheManager.getChannels()) {

            LogUtils.log("下发功率设置：" + "ip:" + channel.getIp() + ";PA:" + powerLevel);
            ProtocolManager.setPa(channel.getIp(), "" + powerLevel);
        }
    }

    public void refreshViews() {
        if (!refreshViewEnable)
            return;

//        refreshDetectOperation();
        refreshPowerLevel();
        refreshRFSwitch();
        refreshChannels();
//        channelListViewAdapter.setChannels(CacheManager.getChannels());
    }

    private void refreshRFSwitch() {
        boolean rfState = false;

        for (LteChannelCfg channel : CacheManager.getChannels()) {
            if (channel.isRFState()) {
                rfState = true;
                break;
            }
        }

        cbRFSwitch.setChecked(rfState);
    }

    private void refreshDetectOperation() {
        if (CacheManager.getChannels().size() > 0) {
            String firstPlnm = CacheManager.getChannels().get(0).getPlmn();  //以第一个作为参考

            if (firstPlnm.contains("46000") && firstPlnm.contains("46001") && firstPlnm.contains("46011")) {
                rbDetectAll.setChecked(true);
            } else if (firstPlnm.equals("46000,46000,46000")) {
                rbCTJ.setChecked(true);
            } else if (firstPlnm.equals("46001,46001,46001")) {
                rbCTU.setChecked(true);
            } else if (firstPlnm.equals("46011,46011,46011")) {
                rbCTC.setChecked(true);
            }
        }
    }

    private void refreshPowerLevel() {
        //定位下有不同频点的功率变动，故不刷新&& !CacheManager.getLocState()
        if (CacheManager.isDeviceOk()) {
            int powerLevel = Integer.parseInt(CacheManager.getChannels().get(0).getPa());

            if (powerLevel <= 10) {
                rbPowerLow.setChecked(true);
            } else if (powerLevel <= 25) {
                rbPowerMedium.setChecked(true);
            } else {
                rbPowerHigh.setChecked(true);
            }

        }

    }

    private void showProcess(int keepTime) {
        Message msg = new Message();
        msg.what = SHOW_PROGRESS;
        msg.obj = keepTime;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onResume() {
        refreshViews();
        super.onResume();
    }


    private void refreshChannels() {
        layoutChannelList.removeAllViews();
        if (!CacheManager.isDeviceOk()) {
            return;
        }

        for (int i = 0; i < CacheManager.channels.size(); i++) {
            LteChannelCfg cfg = CacheManager.getChannels().get(i);
            //String tac = CacheManager.getTac(cfg.getIdx());

            LSettingItem item = new LSettingItem(activity);
            String leftText = "通道：" + cfg.getBand() + "\n" +
                    "频点：[" + cfg.getFcn() + "]";
            item.setRightStyle(3);

            item.switchRightStyle(3);
            item.setMaxLines(2);
            item.setLeftIconVisible(View.GONE);
            item.setClickItemChangeState(false);
            item.setChecked(cfg.isRFState());

            item.setLeftText(leftText);
            item.isShowDivider(true);


            item.setOnLSettingCheckedChange(new LSettingItem.OnLSettingItemClick() {
                @Override
                public void click(LSettingItem item) {
                    if (!CacheManager.checkDevice(activity)) {
                        if (item.isChecked()) {
                            item.setChecked(false);
                        } else {
                            item.setChecked(true);
                        }
                        return;
                    }

                    if (CacheManager.getLocState()) {
                        ToastUtils.showMessageLong( "当前正在搜寻中，请确认通道射频变动是否对其产生影响！");
//                        item.setChecked(!item.isChecked());
//                        return;
                    }

                    showProcess(0);
                    if (item.isChecked()) {
                        item.setChecked(true);
                        ProtocolManager.openRf(cfg.getIp());
                    } else {
                        item.setChecked(false);
                        ProtocolManager.closeRf(cfg.getIp());
                    }
                }
            });

            layoutChannelList.addView(item);

        }
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_VIEW) {
                LogUtils.log("设备参数页面已更新。");
                refreshViews();
            } else if (msg.what == SHOW_PROGRESS) {
                int dialogKeepTime = 5000;
                if (msg.obj != null && (int) msg.obj != 0) {
                    dialogKeepTime = (int) msg.obj;
                }
                mProgressDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                    }
                }, dialogKeepTime);
            }
        }
    };



    @Override
    public void call(String key, Object val) {
        switch (key){
            case EventAdapter.REFRESH_DEVICE:
                mHandler.sendEmptyMessage(UPDATE_VIEW);
                break;
        }
    }
}

