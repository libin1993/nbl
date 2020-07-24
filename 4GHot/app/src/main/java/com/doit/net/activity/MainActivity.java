package com.doit.net.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import android.widget.Toast;

import com.doit.net.Model.DBChannel;
import com.doit.net.Model.DBScanFcn;
import com.doit.net.Model.UCSIDBManager;
import com.doit.net.Protocol.GSMSendPackage;
import com.doit.net.Protocol.GSMSubPackage;
import com.doit.net.Sockets.NetConfig;
import com.doit.net.Sockets.OnSocketChangedListener;
import com.doit.net.Sockets.ServerSocketUtils;
import com.doit.net.Sockets.DatagramSocketUtils;
import com.doit.net.Sockets.UDPSocketUtils;
import com.doit.net.Utils.PermissionUtils;
import com.doit.net.adapter.MainTabLayoutAdapter;
import com.doit.net.base.BaseActivity;
import com.doit.net.base.BaseFragment;
import com.doit.net.bean.DeviceState;
import com.doit.net.bean.TabEntity;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.BlackBoxManger;
import com.doit.net.Event.EventAdapter;
import com.doit.net.Model.AccountManage;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.FTPManager;
import com.doit.net.Utils.LicenceUtils;
import com.doit.net.Model.PrefManage;
import com.doit.net.Model.VersionManage;
import com.doit.net.Utils.DateUtils;
import com.doit.net.Utils.FTPServer;
import com.doit.net.Utils.FileUtils;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.Utils.NetWorkUtils;
import com.doit.net.Utils.LogUtils;

import com.doit.net.fragment.AppFragment;
import com.doit.net.View.LicenceDialog;
import com.doit.net.fragment.LocationFragment;
import com.doit.net.fragment.NameListFragment;
import com.doit.net.receiver.NetworkChangeReceiver;
import com.doit.net.fragment.StartPageFragment;
import com.doit.net.fragment.UeidFragment;
import com.doit.net.ucsi.R;
import com.doit.net.Utils.BaiduAudio;
import com.doit.net.Utils.SoundUtils;
import com.doit.net.Utils.ToastUtils;
import com.flyco.tablayout.CommonTabLayout;
import com.flyco.tablayout.listener.CustomTabEntity;
import com.flyco.tablayout.listener.OnTabSelectListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


import static android.content.pm.PackageManager.PERMISSION_GRANTED;

@SuppressLint("NewApi")
public class MainActivity extends BaseActivity implements TextToSpeech.OnInitListener, EventAdapter.EventCall {
    private Activity activity = this;

    private ViewPager mViewPager;
    private List<BaseFragment> mTabs = new ArrayList<BaseFragment>();
    private CommonTabLayout tabLayout;
    private MainTabLayoutAdapter adapter;
    private List<String> listTitles = new ArrayList<>();
    private ArrayList<CustomTabEntity> mTabEntities = new ArrayList<>();

    private TextToSpeech textToSpeech; // TTS对象
    private BaiduAudio baiduAudio = null;
    private SoundUtils soundUtils = null;

    private MySweetAlertDialog mProgressDialog;
    private boolean hasSetDefaultParam = false;   //开始全部打开射频标志

    private boolean isCheckDeviceStateThreadRun = true;
    private FTPServer ftpServer = new FTPServer();

    private ImageView ivDeviceState;
    Animation viewAnit = new AlphaAnimation(0, 1);
    private ImageView ivWifiState;
    private ImageView ivBatteryLevel;
    private ImageView ivSyncError;
    private MySweetAlertDialog batteryWarningDialog = null;
    Animation batteryViewAnit = new AlphaAnimation(0, 1);


    //handler消息
//    private final int FOUND_BLACK_NAME = 0;
//    private final int SYS_RPT = 1;
    private final int TIP_MSG = 2;
    private final int SHOW_PROGRESS = 3;
    private final int CLOSE_PROGRESS = 4;
    private final int UPDATE_DEVICE_STATE = 5;
    private final int REFRESH_FILE_SYS = 6;
    private final int SPEAK = 7;
    private final int UPDATE_BATTERY = 8;
    private final int ADD_BLACKBOX = 9;
    private final int CHANGE_TAB = 10;
    private final int POWER_START = 11;
    private final int CHECK_LICENCE = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            LogUtils.log("没有读取手机权限");
        }

        initView();
        initOtherWork();
    }

    private void initOtherWork() {
        new Thread() {
            @Override
            public void run() {
                initScanFcn();
                initCustomFcn();
                checkDataDir();
                initEvent();
                initWifiChangeReceive();
                initWifiStatus();
                startCheckDeviceState();
                initNetWork();
                initSpeech();
                initFTP();
                initBlackBox();
            }
        }.start();
    }

    /**
     * 设置自定义频点
     */
    private void initCustomFcn() {
        String fddFcn = "100,350,500,1300,1506,1650,1850";
        String tddFcn = "37900,38098,38400,38544,38950,39148,39250,40936,41134";

        try {
            DbManager dbManager = UCSIDBManager.getDbManager();
            DBChannel fddChannel = dbManager.selector(DBChannel.class)
                    .where("fcn", "=", fddFcn)
                    .findFirst();
            if (fddChannel == null) {
                dbManager.save(new DBChannel(NetConfig.FDD_IP,fddFcn, 1, 1));
            }


            DBChannel tddChannel = dbManager.selector(DBChannel.class)
                    .where("fcn", "=", tddFcn)
                    .findFirst();
            if (tddChannel == null) {
                dbManager.save(new DBChannel(NetConfig.TDD_IP,tddFcn, 1, 1));
            }

        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置默认扫网频点
     */
    private void initScanFcn() {
        int[] fcnArr = new int[]{100,350,500,1300,1506,1650,1850,37900,38098,38400,38544,38950,39148,39250,40936,41134};
        try {
            for (int i : fcnArr) {
                DbManager dbManager = UCSIDBManager.getDbManager();
                DBScanFcn scanFcn = dbManager.selector(DBScanFcn.class)
                        .where("fcn", "=", i)
                        .findFirst();
                if (scanFcn == null) {
                    dbManager.save(new DBScanFcn(i, 1, 1));
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }


    /**
     * wifi状态
     */
    private void initWifiStatus() {
        NetworkInfo wifiNetInfo = ((ConnectivityManager) activity.
                getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        String ssid = NetWorkUtils.getWifiSSID(activity);

        if (wifiNetInfo.isConnected()) {
            LogUtils.log("wifi state change——connected");
            if (CacheManager.deviceState.getDeviceState().equals(DeviceState.WIFI_DISCONNECT))  //只有从wifi未连接到连接才出现这种状态
                CacheManager.deviceState.setDeviceState(DeviceState.WAIT_SOCKET);
        } else {
            LogUtils.log("wifi state change——disconnected");
        }
    }


    /**
     * 黑匣子
     */
    private void initBlackBox() {

        BlackBoxManger.setCurrentAccount(AccountManage.getCurrentLoginAccount());
        BlackBoxManger.initBlx();

    }

    /**
     * socket
     */
    private void initNetWork() {

        ServerSocketUtils.getInstance().startTCP(new OnSocketChangedListener() {
            @Override
            public void onConnect() {
                hasSetDefaultParam = false;
            }

            @Override
            public void onDisconnect() {
                CacheManager.deviceState.setDeviceState(DeviceState.ON_INIT);
            }
        });
    }

    /**
     * FTP
     */
    private void initFTP() {
        File f = new File(FileUtils.ROOT_PATH);
        if (!f.exists())
            f.mkdir();

        ftpServer.copyConfigFile(R.raw.users, FileUtils.ROOT_PATH + "users.properties", getBaseContext());
        ftpServer.init();
        ftpServer.startFTPServer();
    }

    private void initProgressDialog() {
        mProgressDialog = new MySweetAlertDialog(activity, MySweetAlertDialog.PROGRESS_TYPE);
        mProgressDialog.setTitleText("Loading...");
        mProgressDialog.setCancelable(false);
    }

    /**
     * 语音
     */
    private void initSpeech() {
        PrefManage.getPlayType();
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setSpeechRate(1.8f);
        baiduAudio = new BaiduAudio(this);
    }

    /**
     * 注册监听wifi状态广播
     */
    private void initWifiChangeReceive() {
        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    private void initView() {
        setOverflowShowingAlways();

        mViewPager = findViewById(R.id.vpTabPage);
        tabLayout = findViewById(R.id.tablayout);

        ivWifiState = findViewById(R.id.ivWifiState);
        ivWifiState.setOnClickListener(wifiSystemSetting);


        ivDeviceState = findViewById(R.id.ivDeviceState);


        ivSyncError = findViewById(R.id.ivSyncError);

        ivBatteryLevel = findViewById(R.id.ivBatteryLevel);

        initTabs();
        initProgressDialog();
    }

    private void initEvent() {
        EventAdapter.register(EventAdapter.FOUND_BLACK_NAME, this);
        EventAdapter.register("TIP_MSG", this);
        EventAdapter.register("SYS_RPT", this);
        EventAdapter.register("SHOW_PROGRESS", this);
        EventAdapter.register("CLOSE_PROGRESS", this);
        EventAdapter.register(EventAdapter.SPEAK, this);
        EventAdapter.register(EventAdapter.UPDATE_FILE_SYS, this);
        EventAdapter.register(EventAdapter.UPDATE_BATTERY, this);
        EventAdapter.register(EventAdapter.ADD_BLACKBOX, this);
        EventAdapter.register(EventAdapter.CHANGE_TAB, this);
        EventAdapter.register(EventAdapter.WIFI_CHANGE, this);
        EventAdapter.register(EventAdapter.POWER_START, this);

        EventAdapter.register(EventAdapter.HEARTBEAT_RPT, this);
    }

    /**
     * 创建文件缓存路径
     */
    private void checkDataDir() {

        File file = new File(FileUtils.ROOT_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }

        String upgradePath = FileUtils.ROOT_PATH + "upgrade";
        file = new File(upgradePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        EventAdapter.call(EventAdapter.UPDATE_FILE_SYS, upgradePath);

        String exportPath = FileUtils.ROOT_PATH + "export";
        file = new File(exportPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        EventAdapter.call(EventAdapter.UPDATE_FILE_SYS, exportPath);
    }


    View.OnClickListener wifiSystemSetting = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        }
    };

    /**
     * @param text  语音播报
     */
    public void speak(String text) {
        if (PrefManage.play_type == 0) {
            textToSpeech.setPitch(1f);// 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
            textToSpeech.setSpeechRate(0.7f);
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
        } else if (PrefManage.play_type == 1) {
            baiduAudio.speak(text);
        } else if (PrefManage.play_type == 2) {
            text = text.replaceAll("-", "").replaceAll("负", "");
            for (int i = 0; i < text.length(); i++) {
                soundUtils.play(text.substring(i, i + 1));
            }
        }
    }

    private void clearDataDir() {
        AccountManage.deleteAccountFile();
    }

    private void initTabs() {
        List<Integer> listSelectIcon = new ArrayList<>();
        List<Integer> listUnselectIcon = new ArrayList<>();

        if (VersionManage.isArmyVer()) {
            listTitles.add("侦码");
            if (CacheManager.getLocMode()){
                listTitles.add("搜寻");
            }
            listTitles.add("设置");

            listSelectIcon.add(R.drawable.detect_lable_select);
            if (CacheManager.getLocMode()){
                listSelectIcon.add(R.drawable.location_lable_select);
            }
            listSelectIcon.add(R.drawable.setting_lable_select);

            listUnselectIcon.add(R.drawable.detect_lable_unselect);
            if (CacheManager.getLocMode()){
                listUnselectIcon.add(R.drawable.location_lable_unselect);
            }
            listUnselectIcon.add(R.drawable.setting_lable_unselect);

            mTabs.add(new StartPageFragment());
            if (CacheManager.getLocMode()){
                mTabs.add(new LocationFragment());
            }
            mTabs.add(new AppFragment());
        } else if (VersionManage.isPoliceVer()) {
            listTitles.add("侦码");
            if (CacheManager.getLocMode()){
                listTitles.add("搜寻");
            }
            listTitles.add("名单");
            listTitles.add("设置");

            listSelectIcon.add(R.drawable.detect_lable_select);
            if (CacheManager.getLocMode()){
                listSelectIcon.add(R.drawable.location_lable_select);
            }
            listSelectIcon.add(R.mipmap.name_lable_select);
            listSelectIcon.add(R.drawable.setting_lable_select);

            listUnselectIcon.add(R.drawable.detect_lable_unselect);
            if (CacheManager.getLocMode()){
                listUnselectIcon.add(R.drawable.location_lable_unselect);
            }
            listUnselectIcon.add(R.mipmap.name_lable_unselect);
            listUnselectIcon.add(R.drawable.setting_lable_unselect);

            mTabs.add(new StartPageFragment());
            if (CacheManager.getLocMode()){
                mTabs.add(new LocationFragment());
            }
            mTabs.add(new NameListFragment());
            mTabs.add(new AppFragment());
        }

        for (int i = 0; i < listTitles.size(); i++) {
            mTabEntities.add(new TabEntity(listTitles.get(i), listSelectIcon.get(i), listUnselectIcon.get(i)));
        }

        adapter = new MainTabLayoutAdapter(getSupportFragmentManager(), mTabs, listTitles);
        mViewPager.setOffscreenPageLimit(mTabEntities.size());
        mViewPager.setAdapter(adapter);

        tabLayout.setTabData(mTabEntities);

        tabLayout.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                mViewPager.setCurrentItem(position);
            }

            @Override
            public void onTabReselect(int position) {
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tabLayout.setCurrentTab(position);
                mTabs.get(position).onFocus();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mViewPager.setCurrentItem(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new MySweetAlertDialog(this, MySweetAlertDialog.WARNING_TYPE)
                    .setTitleText(getString(R.string.exit_app))
                    .setContentText(getString(R.string.tip_07))
                    .showCancelButton(true)
                    .setCancelText(getString(R.string.cancel))
                    .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(MySweetAlertDialog sDialog) {
                            /* 可以选择在onDestroy里做回收工作，但是在这里更稳 */
                            appExit();
                            //finish();   //finish()会导致onDestroy()被调用，exit不会
                            System.exit(0);
                        }
                    })
                    .show();
        }
        return true;
    }


    //程序退出停止掉定位
    private void appExit() {

        if (CacheManager.getLocState()) {
            CacheManager.stopCurrentLoc();
        }


//        BlackBoxManger.uploadCurrentBlxFile(); //会卡顿一段时间，体验很差
        clearDataDir();

        unregisterReceiver(networkChangeReceiver);

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (baiduAudio != null) {
            baiduAudio.release();
        }

        isCheckDeviceStateThreadRun = false;

        LogUtils.unInitLog();
        finish();
        System.exit(0);
    }

    private void startCheckDeviceState() {
        new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                while (isCheckDeviceStateThreadRun) {
                    Message message = mHandler.obtainMessage();
                    message.what = UPDATE_DEVICE_STATE;
                    mHandler.sendMessage(message);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * 不显示menu菜单键
     */
    private void setOverflowShowingAlways() {
        try {
            // true if a permanent menu key is present, false otherwise.
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class
                    .getDeclaredField("sHasPermanentMenuKey");
            menuKeyField.setAccessible(true);
            menuKeyField.setBoolean(config, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private NetworkChangeReceiver networkChangeReceiver;


    /**
     * 替换首页
     */
    private void turnToUeidPage() {
        mTabs.set(0, new UeidFragment());
        adapter.exchangeFragment();

    }

    @Override
    public void onInit(int status) {
        LogUtils.log("TextToSpeech status=" + status);
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.CHINESE);
            LogUtils.log("TextToSpeech result=" + result);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, R.string.tip_08, Toast.LENGTH_SHORT).show();
                PrefManage.setPlayType(1);
                PrefManage.supportPlay = false;
            } else {
                PrefManage.supportPlay = true;
            }
        }
    }

    /**
     * @param filePath 刷新文件系统
     */
    public void notifyUpdateFileSystem(String filePath) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);

        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        this.getApplication().sendBroadcast(intent);
    }

    /**
     * wifi状态返回
     */
    private void wifiChangeEvent() {
        NetworkInfo wifiNetInfo = ((ConnectivityManager) activity.
                getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (wifiNetInfo.isConnected()) {
            LogUtils.log("wifi state change——connected");
            CacheManager.isWifiConnected = true;
            if (CacheManager.deviceState.getDeviceState().equals(DeviceState.WIFI_DISCONNECT)) {
                CacheManager.deviceState.setDeviceState(DeviceState.WAIT_SOCKET);
            } //只有从wifi未连接到连接才出现这种状态

//            initUDP();  //重连wifi后udp发送ip、端口


        } else {
            CacheManager.isWifiConnected = false;
            LogUtils.log("wifi state change——disconnected");
            CacheManager.deviceState.setDeviceState(DeviceState.WIFI_DISCONNECT);
            CacheManager.resetState();

        }
    }

    /**
     * 创建DatagramSocket
     */
    private void initGSM(){

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                GSMSendPackage sendPackage = new GSMSendPackage();
                sendPackage.setMsgNumber((byte) 0x01);
                sendPackage.setCarrierInstruction((byte)0x00);
                sendPackage.setMsgParameter((byte)0x00);

                GSMSubPackage subPackage = new GSMSubPackage();

                subPackage.setSubMsgNumber((short) 0x0001);

                byte[] a = new byte[20];
                subPackage.setSubMsgContent(a);

                sendPackage.setMsgSubContent(subPackage.getMsgContent());
                UDPSocketUtils.getInstance().sendData(sendPackage.getMsgContent());
            }
        };

        timer.schedule(timerTask,
                0,
                5000);//周期时间

    }


    /**
     * 创建DatagramSocket
     */
    private void initUDP(){

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!CacheManager.deviceState.getDeviceState().equals(DeviceState.NORMAL)) {
                    sendData();
                }else {
                    DatagramSocketUtils.getInstance().closeSocket();
                    timer.cancel();
                }
            }
        };

        timer.schedule(timerTask,
                0,
                5000);//周期时间

    }

    /**
     *  UDP发送手机ip、端口，让设备TCP连接
     */
    public void sendData() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ip", NetWorkUtils.getWIFILocalIpAddress());
            jsonObject.put("port", NetConfig.LOCAL_PORT);
            jsonObject.put("id", DatagramSocketUtils.SEND_LOCAL_IP);
            jsonObject.put("ok", true);

            String data = jsonObject.toString();
            DatagramSocketUtils.getInstance().sendData(data);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    /**
     * @param deviceState 状态栏
     */
    private void updateStatusBar(String deviceState) {
        switch (deviceState) {
            case DeviceState.WIFI_DISCONNECT:
                ivWifiState.setImageDrawable(getDrawable(R.drawable.wifi_disconnect));

                ivBatteryLevel.clearAnimation();
                batteryViewAnit.cancel();
                ivBatteryLevel.setVisibility(View.GONE);

                viewAnit.cancel();
                ivDeviceState.clearAnimation();
                ivDeviceState.setVisibility(View.GONE);
                break;

            case DeviceState.WAIT_SOCKET:
                ivWifiState.setImageDrawable(getDrawable(R.drawable.wifi_connect));

                ivBatteryLevel.clearAnimation();
                batteryViewAnit.cancel();
                ivBatteryLevel.setVisibility(View.GONE);

                viewAnit.cancel();
                ivDeviceState.clearAnimation();
                ivDeviceState.setVisibility(View.GONE);

                //ivSyncError.setVisibility(View.GONE);
                break;

            case DeviceState.ON_INIT:
                ivWifiState.setImageDrawable(getDrawable(R.drawable.wifi_connect));
                ivBatteryLevel.setVisibility(View.GONE);

                ivDeviceState.setImageDrawable(getDrawable(R.drawable.small_device_icon));
                ivDeviceState.setVisibility(View.VISIBLE);
                if (!viewAnit.hasStarted() || viewAnit.hasEnded()) {
                    viewAnit.setDuration(900);   //时间毫秒
                    viewAnit.setInterpolator(new LinearInterpolator());
                    viewAnit.setRepeatMode(Animation.REVERSE);   //播放次序为倒叙
                    viewAnit.setRepeatCount(-1);   //无限
                    ivDeviceState.startAnimation(viewAnit);
                }

                break;

            case DeviceState.NORMAL:
                ivWifiState.setImageDrawable(getDrawable(R.drawable.wifi_connect));
                ivBatteryLevel.setVisibility(View.VISIBLE);
                ivDeviceState.setVisibility(View.VISIBLE);
                viewAnit.cancel();
                ivDeviceState.setImageDrawable(getDrawable(R.drawable.small_device_icon));
                ivDeviceState.clearAnimation();

                //ivSyncError.setVisibility(View.GONE);
                break;
            default:
        }
    }

    /**
     * 低电量提醒
     */
    private void lowBatteryWarnning() {
        if (batteryWarningDialog == null) {
            batteryWarningDialog = new MySweetAlertDialog(this, MySweetAlertDialog.WARNING_TYPE)
                    .setTitleText(getString(R.string.low_power))
                    .setContentText(getString(R.string.low_power_warning))
                    .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(MySweetAlertDialog mySweetAlertDialog) {
                            batteryWarningDialog.dismiss();
                        }
                    });
        }

        EventAdapter.call(EventAdapter.SPEAK, "电池电量过低");

        if (!batteryWarningDialog.isShowing()) {
            batteryWarningDialog.show();
        }

    }

    /**
     * @param voltage  设备电量
     */
    private void processBattery(int voltage) {
        final int LEVEL1 = 9112;
        final int LEVEL2 = 9800;
        final int LEVEL3 = 10380;
        final int LEVEL4 = 11026;
        final int LEVEL5 = 11550;

        if (voltage <= 0)
            return;

        if (voltage >= LEVEL5) {
            ivBatteryLevel.setImageDrawable(getResources().getDrawable(R.drawable.battery_level6));
            //batteryViewAnit.cancel();
            ivBatteryLevel.clearAnimation();
        } else if (voltage >= LEVEL4) {
            ivBatteryLevel.setImageDrawable(getResources().getDrawable(R.drawable.battery_level5));
            ivBatteryLevel.clearAnimation();
            //batteryViewAnit.cancel();
        } else if (voltage >= LEVEL3) {
            ivBatteryLevel.setImageDrawable(getResources().getDrawable(R.drawable.battery_level4));
            batteryViewAnit.cancel();
            ivBatteryLevel.clearAnimation();
        } else if (voltage >= LEVEL2) {
            ivBatteryLevel.setImageDrawable(getResources().getDrawable(R.drawable.battery_level3));
            batteryViewAnit.cancel();
            ivBatteryLevel.clearAnimation();
        } else if (voltage >= LEVEL1) {
            ivBatteryLevel.setImageDrawable(getResources().getDrawable(R.drawable.battery_level2));
            batteryViewAnit.cancel();
            ivBatteryLevel.clearAnimation();
        } else {
            ivBatteryLevel.setImageDrawable(getResources().getDrawable(R.drawable.battery_level1));

            if (!batteryViewAnit.hasStarted() || batteryViewAnit.hasEnded()) {
                batteryViewAnit.setDuration(900);   //时间毫秒
                batteryViewAnit.setInterpolator(new LinearInterpolator());
                batteryViewAnit.setRepeatMode(Animation.REVERSE);   //播放次序为倒叙
                batteryViewAnit.setRepeatCount(-1);   //无限
                //batteryViewAnit.start();
                ivBatteryLevel.startAnimation(batteryViewAnit);
            }

            lowBatteryWarnning();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LicenceDialog.CAMERA_REQUEST_CODE && PermissionUtils.getInstance().
                hasPermission(MainActivity.this, Manifest.permission.CAMERA)) {
            startActivity(new Intent(MainActivity.this, ScanCodeActivity.class));

        }

    }

    @Override
    public void call(String key, final Object val) {
        if ("TIP_MSG".equals(key)) {
            Message msg = new Message();
            msg.what = TIP_MSG;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.SHOW_PROGRESS.equals(key)) {
            Message msg = new Message();
            msg.what = SHOW_PROGRESS;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.CLOSE_PROGRESS.equals(key)) {
            Message msg = new Message();
            msg.what = CLOSE_PROGRESS;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.UPDATE_FILE_SYS.equals(key)) {
            Message msg = new Message();
            msg.what = REFRESH_FILE_SYS;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.SPEAK.equals(key)) {
            Message msg = new Message();
            msg.what = SPEAK;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.UPDATE_BATTERY.equals(key)) {
            Message msg = new Message();
            msg.what = UPDATE_BATTERY;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.ADD_BLACKBOX.equals(key)) {
            Message msg = new Message();
            msg.what = ADD_BLACKBOX;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.CHANGE_TAB.equals(key)) {
            Message msg = new Message();
            msg.what = CHANGE_TAB;
            msg.obj = val;
            mHandler.sendMessage(msg);
        } else if (EventAdapter.WIFI_CHANGE.equals(key)) {
            wifiChangeEvent();
        } else if (EventAdapter.POWER_START.equals(key)) {
            mHandler.sendEmptyMessage(POWER_START);
        } else if (EventAdapter.HEARTBEAT_RPT.equals(key)) {
            ProtocolManager.getEquipAndAllChannelConfig();

            if (!hasSetDefaultParam && CacheManager.deviceList.size() > 1) {
                hasSetDefaultParam = true;

                ProtocolManager.setActiveMode();

                if (!CacheManager.getLocState()){
                    ProtocolManager.clearImsi();
                    ProtocolManager.setDefaultFcn();
                }


                ProtocolManager.setNowTime();
                //是否设置开机自动扫网
//                if (PrefManage.getBoolean(PrefManage.AUTO_SCAN_FCN, false)){
//                    ProtocolManager.getNetworkParams();
//                }

                if (CacheManager.hasPressStartButton()) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (!CacheManager.getLocState()){
                                ProtocolManager.openAllRf();
                            }
                        }
                    }, 5000);
                }

            }

        }
    }

    /**
     * 校验设备证书是否存在，若不存在，生成30天证书上传；若存在，从设备下载证书校验是否过期
     */
    private void checkLicence() {
        new Thread() {
            public void run() {
                try {
                    FTPManager.getInstance().connect();
                    boolean isFileExist = FTPManager.getInstance().downloadFile(FileUtils.ROOT_PATH,
                            LicenceUtils.LICENCE_FILE_NAME);
                    if (isFileExist) {
                        //从设备下载证书校验
                        LicenceUtils.authorizeCode = FileUtils.getInstance().fileToString(
                                FileUtils.ROOT_PATH + LicenceUtils.LICENCE_FILE_NAME);
                        mHandler.sendEmptyMessage(CHECK_LICENCE);
                        FileUtils.getInstance().deleteFile(FileUtils.ROOT_PATH
                                + LicenceUtils.LICENCE_FILE_NAME);
                    } else {
                        //设备无证书，默认生成30天证书上传设备
                        String licence = LicenceUtils.createLicence();
                        LicenceUtils.authorizeCode = licence;
                        boolean isFinish = FileUtils.getInstance().stringToFile(licence,
                                FileUtils.ROOT_PATH + LicenceUtils.LICENCE_FILE_NAME);
                        if (isFinish) {
                            boolean isUploaded = FTPManager.getInstance().uploadFile(FileUtils.ROOT_PATH,
                                    LicenceUtils.LICENCE_FILE_NAME);
                            if (isUploaded) {
                                FileUtils.getInstance().deleteFile(FileUtils.ROOT_PATH
                                        + LicenceUtils.LICENCE_FILE_NAME);
                            }

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * @return 验证授权码
     */
    private boolean checkAuthorize() {

        if (TextUtils.isEmpty(LicenceUtils.authorizeCode)) {
            ToastUtils.showMessageLong("App未授权，请联系管理员。");
            LicenceDialog licenceDialog = new LicenceDialog(this,"退出");
            licenceDialog.setOnCloseListener(new LicenceDialog.OnCloseListener() {
                @Override
                public void onClose() {
                    appExit();
                }
            });
            licenceDialog.show();
            return false;
        }

        //以下判断是否过期
        String dueTime = LicenceUtils.getDueTime();
        long longDueTime = DateUtils.convert2long(dueTime, DateUtils.LOCAL_DATE_DAY);
        long nowTime = System.currentTimeMillis();
        if (nowTime >= longDueTime) {
            ToastUtils.showMessageLong("授权已过期，请联系管理员");
            LicenceDialog licenceDialog = new LicenceDialog(this,"退出");
            licenceDialog.setOnCloseListener(new LicenceDialog.OnCloseListener() {
                @Override
                public void onClose() {
                    appExit();
                }
            });
            licenceDialog.show();

            return false;
        } else {
            int dueDay = (int) ((longDueTime - nowTime) / (24 * 60 * 60 * 1000L));
            if (dueDay <= 7) {
                ToastUtils.showMessageLong("授权码还剩" + dueDay + "天到期，请联系管理员");
            }

            return true;
        }

    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TIP_MSG) {
                String tip = msg.obj.toString();
                ToastUtils.showMessageLong( tip);
            } else if (msg.what == SHOW_PROGRESS) {
                int dialogKeepTime = 5000;
                if (msg.obj != null) {
                    dialogKeepTime = (int) msg.obj;
                }
                mProgressDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                    }
                }, dialogKeepTime);
            } else if (msg.what == CLOSE_PROGRESS) {
                try {
                    mProgressDialog.dismiss();
                } catch (Exception e) {
                }
            } else if (msg.what == UPDATE_DEVICE_STATE) {
                try {
                    updateStatusBar(CacheManager.deviceState.getDeviceState());
                } catch (Exception e) {
                }
            } else if (msg.what == REFRESH_FILE_SYS) {
                notifyUpdateFileSystem(msg.obj.toString());
            } else if (msg.what == SPEAK) {
                speak((String) msg.obj);
            } else if (msg.what == UPDATE_BATTERY) {
                processBattery((int) msg.obj);
            } else if (msg.what == ADD_BLACKBOX) {
                BlackBoxManger.recordOperation((String) msg.obj);
            } else if (msg.what == CHANGE_TAB) {
                mViewPager.setCurrentItem((int) msg.obj, true);
            } else if (msg.what == POWER_START) {
                turnToUeidPage();
            } else if (msg.what == CHECK_LICENCE) {
                checkAuthorize();
            }
        }
    };
}