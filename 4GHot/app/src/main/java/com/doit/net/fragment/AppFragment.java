package com.doit.net.fragment;

import com.doit.net.Event.EventAdapter;
import com.doit.net.Utils.FTPServer;
import com.doit.net.Utils.FileUtils;
import com.doit.net.Utils.FormatUtils;
import com.doit.net.Utils.NetWorkUtils;
import com.doit.net.View.ClearHistoryTimeDialog;
import com.doit.net.activity.CustomFcnActivity;
import com.doit.net.activity.DeviceParamActivity;
import com.doit.net.activity.HistoryListActivity;
import com.doit.net.activity.JustForTest;
import com.doit.net.View.LicenceDialog;
import com.doit.net.activity.SystemSettingActivity;
import com.doit.net.activity.UserManageActivity;
import com.doit.net.activity.WhitelistManagerActivity;
import com.doit.net.activity.BlackBoxActivity;
import com.doit.net.Model.VersionManage;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.doit.net.base.BaseFragment;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.AccountManage;
import com.doit.net.Model.CacheManager;
import com.doit.net.Model.PrefManage;
import com.doit.net.Utils.LicenceUtils;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.StringUtils;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.Utils.LSettingItem;
import com.doit.net.bean.DeviceInfo;
import com.doit.net.ucsi.R;

import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AppFragment extends BaseFragment implements EventAdapter.EventCall {


    @ViewInject(R.id.tvLoginAccount)
    private TextView tvLoginAccount;

    @ViewInject(R.id.btClearUeid)
    private LSettingItem btClearUeid;

    @ViewInject(R.id.tvLocalImsi)
    private LSettingItem tvLocalImsi;

    @ViewInject(R.id.tvSupportVoice)
    private LSettingItem tvSupportVoice;

    @ViewInject(R.id.tvVersion)
    private LSettingItem tvVersion;

    @ViewInject(R.id.btSetWhiteList)
    private LSettingItem btSetWhiteList;

    @ViewInject(R.id.btUserManage)
    private LSettingItem btUserManage;

    @ViewInject(R.id.btBlackBox)
    private LSettingItem btBlackBox;

    @ViewInject(R.id.btn_history_view)
    private LSettingItem historyItem;

    @ViewInject(R.id.btWifiSetting)
    private LSettingItem btWifiSetting;

    @ViewInject(R.id.btDeviceParam)
    private LSettingItem btDeviceParam;


    @ViewInject(R.id.btDeviceFcn)
    private LSettingItem btDeviceFcn;

    @ViewInject(R.id.btDeviceInfoAndUpgrade)
    private LSettingItem btDeviceInfoAndUpgrade;

    @ViewInject(R.id.tvSystemSetting)
    private LSettingItem tvSystemSetting;

    @ViewInject(R.id.btAuthorizeCodeInfo)
    private LSettingItem btAuthorizeCodeInfo;

    @ViewInject(R.id.tvTest)
    private LSettingItem just4Test;

    private ListView lvPackageList;
    private LinearLayout layoutUpgradePackage;
    private PopupWindow mPopupWindow;
//    private PopupWindow mPwProgress;
//    private ProgressBar pbFirstEquip;
//    private TextView tvFirstIp;
//    private TextView tvFirstProgress;
//    private ProgressBar pbSecondEquip;
//    private TextView tvSecondIp;
//    private TextView tvSecondProgress;

    private long fileSize; //升级包大小

    private MySweetAlertDialog mProgressDialog;


    //handler消息
    private final int EXPORT_SUCCESS = 0;
    private final int EXPORT_ERROR = -1;
    private final int SYSTEM_UPDATE = 1;

    public AppFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.doit_layout_app, container, false);

        EventAdapter.register(EventAdapter.UPGRADE_STATUS, this);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvLoginAccount.setText(AccountManage.getCurrentLoginAccount());

        if (VersionManage.isPoliceVer()) {
            btSetWhiteList.setVisibility(View.GONE);
        } else if (VersionManage.isArmyVer()) {
            btSetWhiteList.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
                @Override
                public void click(LSettingItem item) {
                    startActivity(new Intent(getActivity(), WhitelistManagerActivity.class));
                }
            });
        }

        historyItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), HistoryListActivity.class));
            }
        });

        btClearUeid.setmOnLSettingItemClick(clearHistoryListener);

        btUserManage.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), UserManageActivity.class));
            }
        });

        if ((VersionManage.isPoliceVer())) {
            btBlackBox.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
                @Override
                public void click(LSettingItem item) {
                    startActivity(new Intent(getActivity(), BlackBoxActivity.class));
                }
            });
        }

        btWifiSetting.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            }
        });

        tvSystemSetting.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), SystemSettingActivity.class));
            }
        });

        btDeviceParam.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), DeviceParamActivity.class));
            }
        });

        btDeviceFcn.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getContext()))
                    return;

                startActivity(new Intent(getActivity(), CustomFcnActivity.class));
            }
        });

        btDeviceInfoAndUpgrade.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                showDeviceInfoDialog();
            }
        });

        btAuthorizeCodeInfo.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getContext()))
                    return;

                if (!TextUtils.isEmpty(LicenceUtils.authorizeCode)) {
                    LicenceDialog licenceDialog = new LicenceDialog(getActivity());
                    licenceDialog.show();
                } else {
                    ToastUtils.showMessage("获取机器码中，请稍等");
                }

            }
        });

        just4Test.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), JustForTest.class));
            }
        });

        String imsi = getImsi();
        tvLocalImsi.setRightText(imsi);
        tvLocalImsi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取剪贴板管理器：
                ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                // 创建普通字符型ClipData
                ClipData mClipData = ClipData.newPlainText("Label", imsi);
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
            }
        });

        if (PrefManage.supportPlay) {
            tvSupportVoice.setRightText("支持");
        } else {
            tvSupportVoice.setRightText("不支持");
        }


        initProgressDialog();
        tvVersion.setRightText(VersionManage.getVersionName(getContext()));

        if (AccountManage.getCurrentPerLevel() >= AccountManage.PERMISSION_LEVEL2) {
            btUserManage.setVisibility(View.VISIBLE);
            if (VersionManage.isPoliceVer()) {    //军队版本不使用黑匣子
                btBlackBox.setVisibility(View.VISIBLE);
            }
            btClearUeid.setVisibility(View.VISIBLE);
        }

        if (AccountManage.getCurrentPerLevel() >= AccountManage.PERMISSION_LEVEL3) {
            just4Test.setVisibility(View.GONE);
            tvSystemSetting.setVisibility(View.VISIBLE);
        }
    }

    private void initProgressDialog() {
        mProgressDialog = new MySweetAlertDialog(getContext(), MySweetAlertDialog.PROGRESS_TYPE);
        mProgressDialog.setTitleText("升级包正在加载，请耐心等待...");
        mProgressDialog.setCancelable(false);
    }

    private void showDeviceInfoDialog() {
        if (!CacheManager.checkDevice(getContext()))
            return;

        StringBuilder ips = new StringBuilder();
        StringBuilder fws = new StringBuilder();

        for (DeviceInfo deviceInfo : CacheManager.deviceList) {
            ips.append(deviceInfo.getIp()).append("\r\n");
            fws.append(deviceInfo.getFw()).append("\r\n");
        }


        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_device_info, null);
        mPopupWindow = new PopupWindow(dialogView, FormatUtils.getInstance().dip2px(300), ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvDeviceIP = dialogView.findViewById(R.id.tvDeviceIP);
        tvDeviceIP.setText(ips.toString());
        TextView tvSwVersion = dialogView.findViewById(R.id.tvSwVersion);
        tvSwVersion.setText(fws.toString());
        Button btDeviceUpgrade = dialogView.findViewById(R.id.btDeviceUpgrade);
        btDeviceUpgrade.setOnClickListener(upgradeListener);
        lvPackageList = dialogView.findViewById(R.id.lvPackageList);
        layoutUpgradePackage = dialogView.findViewById(R.id.layoutUpgradePackage);

        //设置Popup具体参数
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
        mPopupWindow.setFocusable(true);//点击空白，popup不自动消失
        mPopupWindow.setTouchable(true);//popup区域可触摸
        mPopupWindow.setOutsideTouchable(true);//非popup区域可触摸
        mPopupWindow.showAtLocation(getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);

    }

    @SuppressLint("MissingPermission")
    private String getImsi() {
        TelephonyManager telManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return StringUtils.defaultIfBlank(telManager.getSubscriberId(), getString(R.string.no_sim_card));
    }

    @SuppressLint("MissingPermission")
    private String getImei() {
        TelephonyManager telManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return StringUtils.defaultIfBlank(telManager.getDeviceId(), getString(R.string.no));
    }

    LSettingItem.OnLSettingItemClick clearHistoryListener = new LSettingItem.OnLSettingItemClick() {
        @Override
        public void click(LSettingItem item) {
            ClearHistoryTimeDialog clearHistoryTimeDialog = new ClearHistoryTimeDialog(getActivity());
            clearHistoryTimeDialog.show();
        }
    };


    private String getPackageMD5(String FilePath) {
        BigInteger bi = null;
        try {
            byte[] buffer = new byte[8192];
            int len = 0;
            MessageDigest md = MessageDigest.getInstance("MD5");
            File f = new File(FilePath);
            FileInputStream fis = new FileInputStream(f);
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            fis.close();
            byte[] b = md.digest();
            bi = new BigInteger(1, b);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bi.toString(16);
    }

    View.OnClickListener upgradeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            File file = new File(FileUtils.ROOT_PATH);
            if (!file.exists()) {
                ToastUtils.showMessageLong("未找到升级包，请确认已将升级包放在\"手机存储/"+FileUtils.ROOT_DIRECTORY+"/\"目录下");
                return;
            }

            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                ToastUtils.showMessageLong("未找到升级包，请确认已将升级包放在\"手机存储/"+FileUtils.ROOT_DIRECTORY+"\"目录下");
                return;
            }

            List<String> fileList = new ArrayList<>();
            String tmpFileName = "";
            for (int i = 0; i < files.length; i++) {
                tmpFileName = files[i].getName();
                //UtilBaseLog.printLog("获取升级包：" + tmpFileName);
                if (tmpFileName.endsWith(".img") && getFileSize(FileUtils.ROOT_PATH + "/" + tmpFileName)>2000000)
                    fileList.add(tmpFileName);
            }
            if (fileList.size() == 0) {
                ToastUtils.showMessageLong("文件错误，升级包必须是以\".img\"为后缀的文件");
                return;
            }

            layoutUpgradePackage.setVisibility(View.VISIBLE);
            ArrayAdapter upgradePackageAdapter = new ArrayAdapter<String>(getContext(), R.layout.comman_listview_text, fileList);
            lvPackageList.setAdapter(upgradePackageAdapter);
            lvPackageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String choosePackage = fileList.get(position);
                    LogUtils.log("选择升级包：" + choosePackage);

                    new MySweetAlertDialog(getContext(), MySweetAlertDialog.WARNING_TYPE)
                            .setTitleText("提示")
                            .setContentText("选择的升级包为：" + choosePackage + ", 确定升级吗？")
                            .setCancelText(getContext().getString(R.string.cancel))
                            .setConfirmText(getContext().getString(R.string.sure))
                            .showCancelButton(true)
                            .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(MySweetAlertDialog sweetAlertDialog) {

                                    if (mPopupWindow != null && mPopupWindow.isShowing()) {
                                        mPopupWindow.dismiss();
                                    }
                                    ProtocolManager.systemUpgrade(NetWorkUtils.getWIFILocalIpAddress(), FTPServer.FTP_PORT + "",
                                            FTPServer.USERNAME, FTPServer.PASSWORD, choosePackage);

//                                    getFileSize(FTP_SERVER_PATH + "/" + choosePackage);
//                                    updateProgress();

                                    sweetAlertDialog.dismiss();
                                    mProgressDialog.show();
                                }
                            }).show();

                }
            });
        }
    };

    /**
     * 获取文件大小
     */
    private long getFileSize(String filePath) {

        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            fileSize = fis.available();
            LogUtils.log("文件大小"+fileSize);
            return fileSize;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;

    }

    /**
     * 升级进度
     */
//    private void updateProgress() {
//        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_popup_update, null);
//        mPwProgress = new PopupWindow(dialogView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//
//        //设置Popup具体参数
//        mPwProgress.setOutsideTouchable(false);//非popup区域可触摸
//        mPwProgress.setTouchable(true);//popup区域可触摸
//        mPwProgress.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
//        mPwProgress.showAtLocation(getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
//
//        pbFirstEquip = dialogView.findViewById(R.id.pb_first_equip);
//        tvFirstIp = dialogView.findViewById(R.id.tv_first_ip);
//        tvFirstProgress = dialogView.findViewById(R.id.tv_first_progress);
//
//        pbSecondEquip = dialogView.findViewById(R.id.pb_second_equip);
//        tvSecondIp = dialogView.findViewById(R.id.tv_second_ip);
//        tvSecondProgress = dialogView.findViewById(R.id.tv_second_progress);
//
//
//        List<String> ipList = new ArrayList<>(LTEDataParse.set.keySet());
//
//        tvFirstIp.setText(ipList.get(0));
//        tvSecondIp.setText(ipList.get(1));
//    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == EXPORT_SUCCESS) {
                new SweetAlertDialog(getActivity(), SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("导出成功")
                        .setContentText("文件导出在：" + msg.obj)
                        .show();
            } else if (msg.what == EXPORT_ERROR) {
                new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("导出失败")
                        .setContentText("失败原因：" + msg.obj)
                        .show();
            } else if (msg.what == SYSTEM_UPDATE) {
//                if (mPwProgress == null || !mPwProgress.isShowing()) {
//                    return;
//                }
//                String content = (String) msg.obj;
//                String ip = content.split(",")[0];
//                int progress = (int) (Long.parseLong(content.split(",")[1]) * 100 / fileSize);
//                String firstIp = tvFirstIp.getText().toString().trim();
//                String secondIp = tvSecondIp.getText().toString().trim();
//
//                if (ip.equals(firstIp)) {
//                    pbFirstEquip.setProgress(progress);
//                    tvFirstProgress.setText(progress + "%");
//                } else {
//                    pbSecondEquip.setProgress(progress);
//                    tvSecondProgress.setText(progress + "%");
//                }

                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
            }
        }
    };


    @Override
    public void call(String key, Object val) {
        if (key.equals(EventAdapter.UPGRADE_STATUS)) {
            Message msg = new Message();
            msg.what = SYSTEM_UPDATE;
            msg.obj = val;
            mHandler.sendMessage(msg);
        }
    }
}
