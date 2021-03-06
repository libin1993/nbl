package com.doit.net.fragment;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.util.Attributes;
import com.doit.net.adapter.UeidListViewAdapter;
import com.doit.net.base.BaseFragment;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.bean.ReportBean;
import com.doit.net.bean.UeidBean;
import com.doit.net.Event.EventAdapter;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.BlackBoxManger;
import com.doit.net.Model.CacheManager;
import com.doit.net.Model.ImsiMsisdnConvert;
import com.doit.net.Model.UCSIDBManager;
import com.doit.net.Model.WhiteListInfo;
import com.doit.net.Utils.DateUtils;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.Utils.LogUtils;
import com.doit.net.Utils.UtilOperator;
import com.doit.net.ucsi.R;

import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class RealTimeUeidRptFragment extends BaseFragment implements  EventAdapter.EventCall {
    private ListView mListView;
    private UeidListViewAdapter mAdapter;
    private Button btClearRealtimeUeid;

    private TextView tvRealtimeCTJCount;
    private TextView tvRealtimeCTUCount;
    private TextView tvRealtimeCTCCount;
    private int realtimeCTJCount = 0;
    private int realtimeCTUCount = 0;
    private int realtimeCTCCount = 0;

    private CheckBox cbDetectSwitch;

//    private PopupWindow ueidItemPop;
//    View ueidItemPopView;
//    private TextView tvGetTelNumber;
//    private UeidBean selectedUeidItem = null;
    private long lastSortTime = 0;  //为了防止频繁上报排序导致列表错乱，定时排序一次

    //handler消息
    private final int UEID_RPT = 1;
    private final int SHIELD_RPT = 2;
    private final int RF_STATUS_RPT = 3;


    public RealTimeUeidRptFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.doit_layout_ueid_list, container, false);
        mListView = rootView.findViewById(R.id.listview);
        btClearRealtimeUeid = rootView.findViewById(R.id.button_clear);
        btClearRealtimeUeid.setOnClickListener(clearListener);

        tvRealtimeCTJCount = rootView.findViewById(R.id.tvCTJCount);
        tvRealtimeCTUCount = rootView.findViewById(R.id.tvCTUCount);
        tvRealtimeCTCCount = rootView.findViewById(R.id.tvCTCCount);
        cbDetectSwitch = rootView.findViewById(R.id.cbDetectSwitch);
        initView();

        EventAdapter.register(EventAdapter.RF_STATUS_RPT, this);
        EventAdapter.register(EventAdapter.UEID_RPT, this);
        EventAdapter.register(EventAdapter.SHIELD_RPT, this);

        return rootView;
    }

    private void initView() {


        mAdapter = new UeidListViewAdapter(getActivity());
        mListView.setAdapter(mAdapter);
        mAdapter.setMode(Attributes.Mode.Single);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((SwipeLayout) (mListView.getChildAt(position - mListView.getFirstVisiblePosition()))).open(true);
                ((SwipeLayout) (mListView.getChildAt(position - mListView.getFirstVisiblePosition()))).setClickToClose(true);
            }
        });
//        mAdapter.setOnItemLongClickListener(new UeidListViewAdapter.onItemLongClickListener() {
//            @Override
//            public void onItemLongClick(MotionEvent motionEvent, int position) {
//                selectedUeidItem = CacheManager.realtimeUeidList.get(position);
//                showListPopWindow(mListView, calcPopWindowPosX((int) motionEvent.getX()), calcPopWindowPosY((int) motionEvent.getY()));
//            }
//        });

//        ueidItemPopView = LayoutInflater.from(getActivity()).inflate(R.layout.realtime_ueid_pop_window, null);
//        ueidItemPop = new PopupWindow(ueidItemPopView, getResources().getDisplayMetrics().widthPixels / 3,
//                LinearLayout.LayoutParams.WRAP_CONTENT, true);   //宽度和屏幕成比例
//        ueidItemPop.setContentView(ueidItemPopView);
//        ueidItemPop.setBackgroundDrawable(new ColorDrawable());  //据说不设在有些情况下会关不掉
//        tvGetTelNumber = ueidItemPopView.findViewById(R.id.tvGetTelNumber);
//        tvGetTelNumber.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                LogUtils.log("点击了：" + selectedUeidItem.getImsi());
//                if (!ImsiMsisdnConvert.isAuthenticated()) {
//                    ToastUtils.showMessageLong("尚未通过认证，请先进入“号码翻译设置”进行认证");
//                    return;
//                }
//
//                new Thread() {
//                    @Override
//                    public void run() {
//                        ImsiMsisdnConvert.requestConvertImsiToMsisdn(getContext(), selectedUeidItem.getImsi());
//                        //ImsiMsisdnConvert.queryImsiConvertMsisdnRes(getContext(), selectedUeidItem.getImsi());
//                        //ToastUtils.showMessageLong(getContext(), requestRes);
//                    }
//                }.start();
//
//
//                ueidItemPop.dismiss();
//            }
//        });

        cbDetectSwitch.setOnCheckedChangeListener(null);
        cbDetectSwitch.setChecked(CacheManager.isDeviceOk());
        cbDetectSwitch.setOnCheckedChangeListener(rfDetectSwichtListener);
    }

    CompoundButton.OnCheckedChangeListener rfDetectSwichtListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

            if (!compoundButton.isPressed()) {
                return;
            }

            if (!CacheManager.checkDevice(getContext())) {
                cbDetectSwitch.setChecked(!isChecked);
                return;
            }

            if (isChecked) {
                ProtocolManager.openAllRf();
                ToastUtils.showMessageLong( R.string.all_rf_open);
                EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.OPEN_ALL_RF);
                EventAdapter.call(EventAdapter.SHOW_PROGRESS, 6000);
            } else {
                if (CacheManager.getLocState()) {
                    new MySweetAlertDialog(getContext(), MySweetAlertDialog.WARNING_TYPE)
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
                                    ToastUtils.showMessage(R.string.all_rf_close);
                                    EventAdapter.call(EventAdapter.SHOW_PROGRESS, 6000);
                                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CLOSE_ALL_RF);
                                }
                            })
                            .show();
                } else {
                    ProtocolManager.closeAllRf();
                    ToastUtils.showMessageLong( R.string.all_rf_close);
                    EventAdapter.call(EventAdapter.SHOW_PROGRESS, 6000);
                    EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.CLOSE_ALL_RF);
                }
            }
        }
    };


//    private void showListPopWindow(View anchorView, int posX, int posY) {
//        ueidItemPop.showAtLocation(anchorView, Gravity.TOP | Gravity.START, posX, posY);
//    }
//
//    private int calcPopWindowPosY(int eventY) {
//        int listviewHeight = mListView.getResources().getDisplayMetrics().heightPixels;
//        ueidItemPop.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//        int popWinHeight = ueidItemPop.getContentView().getMeasuredHeight();
//
//        boolean isNeedShowUpward = (eventY + popWinHeight > listviewHeight);  //超过范围就向上显示
//        if (isNeedShowUpward) {
//            return eventY - popWinHeight;
//        } else {
//            return eventY;
//        }
//    }
//
//    private int calcPopWindowPosX(int eventX) {
//        int listviewWidth = mListView.getResources().getDisplayMetrics().widthPixels;
//        ueidItemPop.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//        int windowWidth = ueidItemPop.getContentView().getMeasuredWidth();
//
//        boolean isShowLeft = (eventX + windowWidth > listviewWidth);  //超过屏幕的话就向左边显示
//        if (isShowLeft) {
//            return eventX - windowWidth;
//        } else {
//            return eventX;
//        }
//    }

    private void addShildRptList(String imsi, String srsp,String fcn) {
        //不再过滤，需要额外显示
//        if (isWhitelist(imsi)){
//            UtilBaseLog.printLog("忽略白名单："+imsi);
//            return;
//        }

        LogUtils.log("IMSI：" + imsi + "强度：" + srsp);
        for (int i = 0; i < CacheManager.realtimeUeidList.size(); i++) {
            if (CacheManager.realtimeUeidList.get(i).getImsi().equals(imsi)) {
                int times = CacheManager.realtimeUeidList.get(i).getRptTimes();
                if (times > 1000) {
                    times = 0;
                }
                CacheManager.realtimeUeidList.get(i).setRptTimes(times + 1);
                //场强处理，
                int rssi = 130 - Integer.parseInt(srsp);
                if (rssi < 0) {
                    rssi = 0;
                }

                if (rssi > 100) {
                    rssi = 100;
                }
                CacheManager.realtimeUeidList.get(i).setSrsp("" + rssi);
                CacheManager.realtimeUeidList.get(i).setFcn(fcn);

                CacheManager.realtimeUeidList.get(i).setRptTime(DateUtils.convert2String(new Date().getTime(), DateUtils.LOCAL_DATE));

                return;
            }
        }

        UeidBean newUeid = new UeidBean();
        newUeid.setImsi(imsi);
        newUeid.setNumber("");
        newUeid.setSrsp("" + (130 - Integer.parseInt(srsp)));
        newUeid.setRptTime(DateUtils.convert2String(new Date().getTime(), DateUtils.LOCAL_DATE));
        newUeid.setRptTimes(1);
        newUeid.setFcn(fcn);
        CacheManager.realtimeUeidList.add(newUeid);

        UCSIDBManager.saveUeidToDB(imsi, ImsiMsisdnConvert.getMsisdnFromLocal(imsi), "",
                new Date().getTime(), "", "",fcn);
    }

    private boolean isWhitelist(String imsi) {
        DbManager dbManager = UCSIDBManager.getDbManager();
        long count = 0;
        try {
            count = dbManager.selector(WhiteListInfo.class)
                    .where("imsi", "=", imsi).count();
        } catch (DbException e) {
            e.printStackTrace();
        }

        return count > 0;
    }


    private void updateUeidCntInOperator(List<UeidBean> realtimeUeidList) {
        realtimeCTJCount = 0;
        realtimeCTUCount = 0;
        realtimeCTCCount = 0;

        for (int i = 0; i < realtimeUeidList.size(); i++) {
            switch (UtilOperator.getOperatorName(realtimeUeidList.get(i).getImsi())) {
                case "CTJ":
                    realtimeCTJCount++;
                    break;
                case "CTU":
                    realtimeCTUCount++;
                    break;
                case "CTC":
                    realtimeCTCCount++;
                    break;
                default:
                    break;
            }
        }
    }


    private void updateView() {
        if (mAdapter != null) {
            mAdapter.refreshData();
        }

        updateUeidCntInOperator(CacheManager.realtimeUeidList);

        tvRealtimeCTJCount.setText(String.valueOf(realtimeCTJCount));
        tvRealtimeCTUCount.setText(String.valueOf(realtimeCTUCount));
        tvRealtimeCTCCount.setText(String.valueOf(realtimeCTCCount));
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UEID_RPT:
                    if (CacheManager.currentWorkMode.equals("2"))  //管控模式忽略ftp上报的
                        return;

                    //确保到达这里的采集数据已经去过重，并存到数据库了
                    List<UeidBean> listUeid = (List<UeidBean>) msg.obj;
                    CacheManager.addRealtimeUeidList(listUeid);

                    /* 对于同步完成之前的上传数据，保存数据库但不显示 */
                    if (!CacheManager.isDeviceOk())
                        return;

                    updateView();
                    break;
                case SHIELD_RPT:
                    ReportBean reportBean = (ReportBean) msg.obj;
                    if (reportBean == null)
                        return;

                    addShildRptList(reportBean.getImsi(), reportBean.getRssi(),reportBean.getFcn());
                    sortRealtimeRpt();
                    updateView();
                    break;
                case RF_STATUS_RPT:
                    isRFOpen();
                    break;

            }
        }
    };


    //根据强度排序
    private void sortRealtimeRpt() {
        if (!CacheManager.currentWorkMode.equals("2"))
            return;

        if (new Date().getTime() - lastSortTime >= 3000) {
            Collections.sort(CacheManager.realtimeUeidList, new Comparator<UeidBean>() {
                public int compare(UeidBean o1, UeidBean o2) {
                    return Integer.valueOf(o2.getSrsp()).compareTo(Integer.valueOf(o1.getSrsp()));
                }
            });

            lastSortTime = new Date().getTime();
        }
    }


    /**
     * 开启射频耗时操作,此时射频还未收到设备射频开启回复
     */
    @Override
    public void onResume() {
        super.onResume();
        isRFOpen();
    }

    /**
     * 射频是否开启
     */
    private void isRFOpen() {
        boolean rfState = false;

        for (LteChannelCfg channel : CacheManager.getChannels()) {
            if (channel.isRFState()) {
                rfState = true;
                break;
            }
        }

        cbDetectSwitch.setOnCheckedChangeListener(null);
        cbDetectSwitch.setChecked(rfState);
        cbDetectSwitch.setOnCheckedChangeListener(rfDetectSwichtListener);
    }


    View.OnClickListener clearListener = new View.OnClickListener() {
        @Override
        public synchronized void onClick(View v) {
            CacheManager.realtimeUeidList.clear();
            lastSortTime = new Date().getTime();
            updateView();
        }
    };

    @Override
    public void call(String key, Object val) {
        switch (key) {
            case EventAdapter.SHIELD_RPT:
                Message msg = new Message();
                msg.what = SHIELD_RPT;
                msg.obj = val;
                mHandler.sendMessage(msg);
                break;
            case EventAdapter.UEID_RPT:
                Message message = new Message();
                message.what = UEID_RPT;
                message.obj = val;
                mHandler.sendMessage(message);
                break;
            case EventAdapter.RF_STATUS_RPT:
                mHandler.sendEmptyMessage(RF_STATUS_RPT);
                break;
        }
    }
}
