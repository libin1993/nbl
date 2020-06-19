package com.doit.net.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.doit.net.application.MyApplication;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.Event.EventAdapter;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.ucsi.R;
import com.suke.widget.SwitchButton;

import java.util.Timer;
import java.util.TimerTask;

public class ChannelListViewAdapter extends BaseAdapter {

    private Context mContext;

    public ChannelListViewAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void refreshData() {
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView title;
        SwitchButton rfButton;
        //        BootstrapEditText reLevelMin;
        EditText fcn;
        EditText pa;
        EditText ga;
        EditText rlm;
        EditText etRxGain;
        EditText etPeriod;
        Button saveBtn;

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.doit_layout_channel_item, null);
            holder.title = convertView.findViewById(R.id.title_text);
            holder.rfButton = convertView.findViewById(R.id.id_switch_rf);
//            holder.reLevelMin = (BootstrapEditText)convertView.findViewById(R.id.editText_rxlevmin);
            holder.fcn = convertView.findViewById(R.id.editText_fcn);
            holder.pa = convertView.findViewById(R.id.editText_pa);
            holder.ga = convertView.findViewById(R.id.editText_ga);
            holder.rlm = convertView.findViewById(R.id.etRLM);
            holder.etRxGain = convertView.findViewById(R.id.et_gain);
            holder.etPeriod = convertView.findViewById(R.id.et_period);
            holder.saveBtn = convertView.findViewById(R.id.button_save);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        fillValues(position, holder);
        return convertView;
    }


    public void fillValues(int position, final ViewHolder holder) {
        LteChannelCfg cfg = CacheManager.channels.get(position);

        if (cfg == null) {
            return;
        }
        holder.title.setText("通道:" + cfg.getBand());
        holder.rfButton.setChecked(cfg.isRfOpen());
//        holder.reLevelMin.setText(cfg.getRxlevmin()==null?"":""+cfg.getRxlevmin().intValue());
        holder.fcn.setText(cfg.getFcn() == null ? "" : "" + cfg.getFcn());
        holder.ga.setText(cfg.getRxGain() == null ? "" : "" + cfg.getRxGain());
        holder.pa.setText(cfg.getPa() == null ? "" : "" + cfg.getPa());
        holder.rlm.setText(cfg.getRlm() == null ? "" : "" + cfg.getRlm());
        holder.etRxGain.setText(cfg.getRxGain() == null ? "" : "" + cfg.getRxGain());
        holder.etPeriod.setText(cfg.getPollTmr() == null ? "" : "" + cfg.getPollTmr());
        holder.rfButton.setOnClickListener(new SwitchButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CacheManager.checkDevice(MyApplication.mContext)) {
                    return;
                }

                SwitchButton button = (SwitchButton) v;
                if (button.isChecked()) {
                    ProtocolManager.openRf(cfg.getIp());
                    button.setChecked(false);
                } else {
                    ProtocolManager.closeRf(cfg.getIp());
                    button.setChecked(true);
                }
            }
        });

        holder.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fcn = holder.fcn.getText().toString().trim();
                String pa = holder.pa.getText().toString().trim();
                String ga = holder.ga.getText().toString().trim();
                String rlm = holder.rlm.getText().toString().trim();
                String rxGain = holder.etRxGain.getText().toString().trim();
                String pollTmr = holder.etPeriod.getText().toString().trim();

                //不为空校验正则，为空不上传
//                if (!TextUtils.isEmpty(fcn)){
//                    if (!FormatUtils.getInstance().matchFCN(fcn)){
//                        ToastUtils.showMessage(mContext,"FCN格式输入有误,请检查");
//                        return;
//                    }
//                }


                ToastUtils.showMessage( R.string.tip_15);

                ProtocolManager.setPa(cfg.getIp(), pa);
                ProtocolManager.setFcn(cfg.getIp(), fcn, pollTmr);
                ProtocolManager.setChannel(cfg.getIp(), null, null, rxGain, null, null, null);


            }
        });
        holder.rfButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
//                if(isChecked){
//                    ProtocolManager.openRf(cfg.getIdx());
//                }else{
//                    ProtocolManager.closeRf(cfg.getIdx());
//                }
            }
        });
    }


    @Override
    public int getCount() {
        return CacheManager.channels.size();
    }

    @Override
    public Object getItem(int position) {
        return CacheManager.channels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
