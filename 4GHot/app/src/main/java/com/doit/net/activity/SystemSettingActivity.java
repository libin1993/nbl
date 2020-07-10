package com.doit.net.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.doit.net.Utils.FileUtils;
import com.doit.net.base.BaseActivity;
import com.doit.net.Model.AccountManage;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.FTPManager;
import com.doit.net.Model.PrefManage;
import com.doit.net.Utils.LSettingItem;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.ucsi.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static cn.pedant.SweetAlert.SweetAlertDialog.WARNING_TYPE;

public class SystemSettingActivity extends BaseActivity {
    private Activity activity = this;
    public static String LOC_PREF_KEY = "LOC_PREF_KEY";

    private LSettingItem tvOnOffLocation;
    private LSettingItem tvGeneralAdmin;
    //private SettingItemClickEvent settingItemLocSwitch = new SettingItemClickEvent();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_setting);

        tvOnOffLocation = findViewById(R.id.tvOnOffLocation);
        tvOnOffLocation.setOnLSettingCheckedChange(settingItemLocSwitch);
        tvOnOffLocation.setmOnLSettingItemClick(settingItemLocSwitch);  //点击该行开关以外地方也会切换开关，故覆盖其回调

//        tvIfAutoOpenRF = findViewById(R.id.tvIfAutoOpenRF);
//        tvIfAutoOpenRF.setOnLSettingCheckedChange(settingItemAutoRFSwitch);
//        tvIfAutoOpenRF.setmOnLSettingItemClick(settingItemAutoRFSwitch);

        tvGeneralAdmin = findViewById(R.id.tvGeneralAdmin);
        tvGeneralAdmin.setmOnLSettingItemClick(generalAdminAccount);



        if (PrefManage.getBoolean(LOC_PREF_KEY, true)){
            tvOnOffLocation.setChecked(true);
        }else{
            tvOnOffLocation.setChecked(false);
        }



    }

    private LSettingItem.OnLSettingItemClick settingItemLocSwitch = new LSettingItem.OnLSettingItemClick(){
        @Override
        public void click(LSettingItem item) {
            if (tvOnOffLocation.isChecked()) {
                PrefManage.setBoolean(LOC_PREF_KEY, true);
            }else{
                PrefManage.setBoolean(LOC_PREF_KEY, false);
            }

            ToastUtils.showMessage("设置成功，重新登陆生效。");
        }
    };

//    private LSettingItem.OnLSettingItemClick settingItemAutoRFSwitch = new LSettingItem.OnLSettingItemClick(){
//        @Override
//        public void click(LSettingItem item) {
//            if(!CacheManager.checkDevice(activity)){
//                tvIfAutoOpenRF.setChecked(!tvIfAutoOpenRF.isChecked());
//                return;
//            }
//
//            ProtocolManager.setAutoRF(tvIfAutoOpenRF.isChecked());
//
//            ToastUtils.showMessage(activity, "下次开机生效");
//        }
//    };


    private LSettingItem.OnLSettingItemClick generalAdminAccount = new LSettingItem.OnLSettingItemClick(){
        @Override
        public void click(LSettingItem item) {
            generalAdmin();
        }
    };

    private void generalAdmin() {
        final String accountFullPath = FileUtils.ROOT_PATH+"FtpAccount/";
        final String accountFileName = "account";


        File namelistFile = new File(accountFullPath+accountFileName);
        if (namelistFile.exists()){
            namelistFile.delete();
        }

        BufferedWriter bufferedWriter = null;
        try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(accountFullPath+accountFileName,true)));
                bufferedWriter.write("admin"+","+"admin"+ "," + AccountManage.getAdminRemark()+"\n");
                bufferedWriter.flush();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if(bufferedWriter != null){
                try {
                    bufferedWriter.close();
                } catch (IOException e) {}
            }
        }

        new Thread() {
            public void run() {
                try {
                    FTPManager.getInstance().connect();
                    if (FTPManager.getInstance().uploadFile(accountFullPath, accountFileName)){
                        ToastUtils.showMessage( "生成管理员账号成功");
                    }else {
                        ToastUtils.showMessage("生成管理员账号出错");
                    }
                    AccountManage.deleteAccountFile();
                } catch (Exception e) {
                    ToastUtils.showMessage( "生成管理员账号出错");
                    e.printStackTrace();
                }
            }
        }.start();
    }




    View.OnClickListener resetFreqScanFcnClikListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(!CacheManager.checkDevice(activity))
                return;

            new SweetAlertDialog(activity, WARNING_TYPE)
                    .setTitleText("提示")
                    .setContentText("开机搜网列表将被重置，确定吗?")
                    .setCancelText(activity.getString(R.string.cancel))
                    .setConfirmText(activity.getString(R.string.sure))
                    .showCancelButton(true)
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            resetFreqScanFcn();
                            sweetAlertDialog.dismiss();
                        }
                    }).show();


        }
    };

    private void resetFreqScanFcn() {
        String band1Fcns = "100,375,400";
        String band3Fcns = "1300,1506,1650,1825";
        String band38Fcns = "37900,38098,38200";
        String band39Fcns = "38400,38544,38300";
        String band40Fcns = "38950,39148,39300";
        String tmpAllFcns = "";
        String[] tmpSplitFcn;

        for (LteChannelCfg channel : CacheManager.getChannels()) {
            switch (channel.getBand()) {
                case "1":
                    ProtocolManager.setChannelConfig(channel.getIdx(), "", "", "", "", "", "", band1Fcns);
                    break;

                case "3":
                    ProtocolManager.setChannelConfig(channel.getIdx(), "", "", "", "", "", "", band3Fcns);
                    break;
                case "38":
                    ProtocolManager.setChannelConfig(channel.getIdx(), "", "", "", "", "", "", band38Fcns);
                    break;

                case "39":
                    ProtocolManager.setChannelConfig(channel.getIdx(), "", "", "", "", "", "", band39Fcns);
                    break;

                case "40":
                    ProtocolManager.setChannelConfig(channel.getIdx(), "", "", "", "", "", "", band40Fcns);
                    break;

                default:
                    break;
            }
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* 为解决从后台切回来之后重新打开启动屏及登录界面问题，需要设置点击子activity时强制打开MainActivity
        * 否则会出现在子activity点击返回直接将app切到后台(为防止mainActivity重复加载，已将其设置为singleTop启动) */
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                startActivity(new Intent(this, MainActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
