package com.doit.net.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;

import com.doit.net.Event.EventAdapter;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.ucsi.R;

/**
 * Created by wiker on 2016/4/29.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    public static boolean isShow = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!wifiNetInfo.isConnected() && !isShow) {
            MySweetAlertDialog dialog = new MySweetAlertDialog(context, MySweetAlertDialog.WARNING_TYPE);
            dialog.setTitleText(context.getString(R.string.wifi_error));
            dialog.setContentText(context.getString(R.string.tip_10));
            dialog.show();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    isShow = false;
                }
            });

            isShow = true;
            EventAdapter.call(EventAdapter.STOP_LOC);
            LogUtils.log("Wifi断开");
            CacheManager.resetState();
        }

        EventAdapter.call(EventAdapter.WIFI_CHANGE);

    }
}
