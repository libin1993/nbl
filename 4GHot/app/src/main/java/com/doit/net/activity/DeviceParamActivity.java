package com.doit.net.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.os.Bundle;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.doit.net.Utils.FormatUtils;
import com.doit.net.Utils.ScreenUtils;
import com.doit.net.View.ChannelsDialog;
import com.doit.net.View.SetScanFcnDialog;
import com.doit.net.View.SystemSetupDialog;
import com.doit.net.adapter.ChannelListViewAdapter;
import com.doit.net.adapter.UserChannelListAdapter;
import com.doit.net.base.BaseActivity;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.Utils.LogUtils;
import com.doit.net.ucsi.R;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
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

import static java.security.AccessController.getContext;


/**
 * 设备参数
 */
public class DeviceParamActivity extends BaseActivity implements EventAdapter.EventCall {

    private LinearLayout layoutChannelList;


    private Button btSetChannelCfg;
    private Button btUpdateTac;
    private Button btRebootDevice;
    private Button btRefreshParam;

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

        mProgressDialog = new MySweetAlertDialog(DeviceParamActivity.this, MySweetAlertDialog.PROGRESS_TYPE);
        mProgressDialog.setTitleText("Loading...");
        mProgressDialog.setCancelable(false);
    }

    View.OnClickListener setCellParamClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
                return;
            }
            new SystemSetupDialog(DeviceParamActivity.this).show();
        }
    };

    View.OnClickListener setChannelCfgClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
                return;
            }

            setChannel();

        }
    };

    /**
     * 设置通道
     */
    private void setChannel() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.doit_layout_channels_dialog, null);
        PopupWindow popupWindow = new PopupWindow(dialogView, ScreenUtils.getInstance()
                .getScreenWidth(DeviceParamActivity.this)-FormatUtils.getInstance().dip2px(40),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        RecyclerView rvChannel = dialogView.findViewById(R.id.rv_channel);
        Button btnCancel = dialogView.findViewById(R.id.button_cancel);

        //设置Popup具体参数
        popupWindow.setFocusable(true);//点击空白，popup不自动消失
        popupWindow.setTouchable(true);//popup区域可触摸
        popupWindow.setOutsideTouchable(false);//非popup区域可触摸
        popupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
        popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);


        rvChannel.setLayoutManager(new LinearLayoutManager(this));
        BaseQuickAdapter<LteChannelCfg, BaseViewHolder> adapter = new BaseQuickAdapter<LteChannelCfg,
                BaseViewHolder>(R.layout.doit_layout_channel_item, CacheManager.getChannels()) {
            @Override
            protected void convert(BaseViewHolder helper, LteChannelCfg item) {
                helper.setText(R.id.title_text, "通道:" + item.getBand());

                helper.setText(R.id.editText_fcn, item.getFcn() == null ? "" : "" + item.getFcn());
                helper.setText(R.id.editText_pa, item.getPa() == null ? "" : "" + item.getPa());

                helper.setText(R.id.et_gain,item.getRxGain() == null ? "" : "" + item.getRxGain());
                helper.setText(R.id.et_period,item.getPollTmr() == null ? "" : "" + item.getPollTmr());
                helper.setText(R.id.et_frm_ofs,item.getFrmOfs() == null ? "" : "" + item.getFrmOfs());
                CheckBox cbGps = helper.getView(R.id.cb_gps);
                CheckBox cbCnm = helper.getView(R.id.cb_cnm);
                cbGps.setChecked("1".equals(item.getCnm()));
                cbCnm.setChecked("1".equals(item.getGps()));


                helper.addOnClickListener(R.id.button_save);
            }
        };
        rvChannel.setAdapter(adapter);

        adapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {

                if (view.getId() == R.id.button_save) {
                    if (!CacheManager.checkDevice(DeviceParamActivity.this)){
                        return;
                    }
                    BaseViewHolder viewHolder = (BaseViewHolder) rvChannel.findViewHolderForLayoutPosition(position);
                    if (viewHolder ==null){
                        return;
                    }

                    EditText etFcn = (EditText) viewHolder.getView( R.id.editText_fcn);
                    EditText etPa = (EditText) viewHolder.getView( R.id.editText_pa);
                    EditText etRxGain = (EditText) viewHolder.getView( R.id.et_gain);
                    EditText etPeriod = (EditText)viewHolder.getView(R.id.et_period);
                    EditText etFrmOfs= (EditText) viewHolder.getView( R.id.et_frm_ofs);
                    CheckBox cbGps = (CheckBox)viewHolder.getView(R.id.cb_gps);
                    CheckBox cbCnm = (CheckBox) viewHolder.getView(R.id.cb_cnm);

                    String fcn = etFcn.getText().toString().trim();
                    String pa = etPa.getText().toString().trim();
                    String rxGain = etRxGain.getText().toString().trim();
                    String pollTmr = etPeriod.getText().toString().trim();
                    String frmOfs = etFrmOfs.getText().toString().trim();
                    String gps = cbGps.isChecked() ? "1":"0";
                    String cnm = cbCnm.isChecked() ? "1":"0";


                    CacheManager.fcnMap.put(CacheManager.channels.get(position).getIp(),fcn);

                    ToastUtils.showMessage( R.string.tip_15);

                    ProtocolManager.setPa(CacheManager.channels.get(position).getIp(), pa);
                    ProtocolManager.setFcn(CacheManager.channels.get(position).getIp(), fcn, pollTmr);
                    ProtocolManager.setSync(CacheManager.channels.get(position).getIp(), gps, frmOfs,cnm);
                    ProtocolManager.setChannel(CacheManager.channels.get(position).getIp(), null, null, rxGain, null, null, null);
                }
            }
        });


        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }

    View.OnClickListener setScanFcnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
                return;
            }

            new SetScanFcnDialog(DeviceParamActivity.this).show();
        }
    };

    View.OnClickListener updateTacClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
                return;
            }

            ProtocolManager.changeTac();

            EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CHANNEL_TAG);
        }
    };

    View.OnClickListener rebootDeviceClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
                return;
            }

            new MySweetAlertDialog(DeviceParamActivity.this, MySweetAlertDialog.WARNING_TYPE)
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
            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
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

            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
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

            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
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

            if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
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
                    new MySweetAlertDialog(DeviceParamActivity.this, MySweetAlertDialog.WARNING_TYPE)
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

            LSettingItem item = new LSettingItem(DeviceParamActivity.this);
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
                    if (!CacheManager.checkDevice(DeviceParamActivity.this)) {
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

