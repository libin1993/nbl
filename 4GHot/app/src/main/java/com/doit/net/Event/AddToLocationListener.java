package com.doit.net.Event;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.doit.net.Model.BlackBoxManger;
import com.doit.net.Model.CacheManager;
import com.doit.net.Model.DBBlackInfo;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.Utils.UtilOperator;
import com.doit.net.bean.DeviceInfo;

/**
 * Created by wiker on 2016/4/27.
 */
public class AddToLocationListener implements View.OnClickListener
{

    private int position;
    private Context mContext;
    private String imsi;
    private String ip;
    private String remark;
//    private DBBlackInfo blackInfo;
    private String lastLocOperator; //上次定位号码制式

    public AddToLocationListener(int position, Context mContext,String imsi,String remark,String ip) {
        this.position = position;
        this.mContext = mContext;
        this.remark = remark;
        this.imsi = imsi;
        this.ip = ip;
    }


    public AddToLocationListener(int position, Context mContext, DBBlackInfo blackInfo){
        this.position = position;
        this.mContext = mContext;
        this.remark = blackInfo.getRemark();
        this.imsi = blackInfo.getImsi();

    }
    @Override
    public void onClick(View v) {
        try {
            if(!CacheManager.checkDevice(mContext)){
                return;
            }

            if (CacheManager.getLocState()){
                if (CacheManager.getCurrentLoction().getImsi().equals(imsi)){
                    ToastUtils.showMessage("该号码正在搜寻中");
                    return;
                }else{
                    EventAdapter.call(EventAdapter.SHOW_PROGRESS,8000);  //防止快速频繁更换定位目标
                    CacheManager.updateLoc(imsi,ip);
                    if (TextUtils.isEmpty(ip)){
                        ProtocolManager.openAllRf();
                    }else {
                        for (DeviceInfo deviceInfo : CacheManager.deviceList) {
                            if (deviceInfo.getIp().equals(ip)){
                                ProtocolManager.openRf(ip);
                            }else {
                                ProtocolManager.closeRf(deviceInfo.getIp());
                            }
                        }

                    }

                    CacheManager.startLoc(imsi);
                    ToastUtils.showMessage("开始新的搜寻");
                }
            }else{
                CacheManager.updateLoc(imsi,ip);
                if (TextUtils.isEmpty(ip)){
                    ProtocolManager.openAllRf();
                }else {
                    for (DeviceInfo deviceInfo : CacheManager.deviceList) {
                        if (deviceInfo.getIp().equals(ip)){
                            ProtocolManager.openRf(ip);
                        }else {
                            ProtocolManager.closeRf(deviceInfo.getIp());
                        }
                    }

                }
                CacheManager.startLoc(imsi);
                ToastUtils.showMessage("搜寻开始");
            }

            TurnToLocInterface();

            EventAdapter.call(EventAdapter.ADD_LOCATION,imsi);
            EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.START_LOCALTE_FROM_NAMELIST+imsi);
        } catch (Exception e) {
            LogUtils.log("开启搜寻失败"+e);
        }

    }

    private void TurnToLocInterface() {
        EventAdapter.call(EventAdapter.CHANGE_TAB, 1);
    }


    private void checkPower(String imsi){
        String operator = UtilOperator.getOperatorName(imsi);
        if (operator.equals(lastLocOperator)){
            return;
        }else {
            UtilOperator.checkPower(imsi);
        }

        lastLocOperator = operator;
    }
}
