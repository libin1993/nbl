package com.doit.net.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.doit.net.bean.LteChannelCfg;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.ucsi.R;

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
        EditText fcn;
        EditText pa;
        EditText etRxGain;
        EditText etPeriod;
        Button saveBtn;
        CheckBox cbGPS;
        CheckBox cbCNM;
        EditText etGPS;

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.doit_layout_channel_item, null);
            holder.title = convertView.findViewById(R.id.title_text);
            holder.fcn = convertView.findViewById(R.id.editText_fcn);
            holder.pa = convertView.findViewById(R.id.editText_pa);

            holder.etRxGain = convertView.findViewById(R.id.et_gain);
            holder.etPeriod = convertView.findViewById(R.id.et_period);
            holder.saveBtn = convertView.findViewById(R.id.button_save);
            holder.cbGPS = convertView.findViewById(R.id.cb_gps);
            holder.cbCNM = convertView.findViewById(R.id.cb_cnm);
            holder.etGPS = convertView.findViewById(R.id.et_frm_ofs);

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

        holder.fcn.setText(cfg.getFcn() == null ? "" : "" + cfg.getFcn());
        holder.pa.setText(cfg.getPa() == null ? "" : "" + cfg.getPa());
        holder.etRxGain.setText(cfg.getRxGain() == null ? "" : "" + cfg.getRxGain());
        holder.etPeriod.setText(cfg.getPollTmr() == null ? "" : "" + cfg.getPollTmr());
        holder.etGPS.setText(cfg.getFrmOfs() == null ? "" : "" + cfg.getFrmOfs());
        holder.cbCNM.setChecked("1".equals(cfg.getCnm()));
        holder.cbGPS.setChecked("1".equals(cfg.getGps()));

        holder.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fcn = holder.fcn.getText().toString().trim();
                String pa = holder.pa.getText().toString().trim();
                String rxGain = holder.etRxGain.getText().toString().trim();
                String pollTmr = holder.etPeriod.getText().toString().trim();
                String frmOfs = holder.etGPS.getText().toString().trim();
                String gps = holder.cbGPS.isChecked() ? "1":"0";
                String cnm = holder.cbCNM.isChecked() ? "1":"0";
                //不为空校验正则，为空不上传
//                if (!TextUtils.isEmpty(fcn)){
//                    if (!FormatUtils.getInstance().matchFCN(fcn)){
//                        ToastUtils.showMessage(mContext,"FCN格式输入有误,请检查");
//                        return;
//                    }
//                }

                CacheManager.fcnMap.put(cfg.getIp(),fcn);

                ToastUtils.showMessage( R.string.tip_15);

                ProtocolManager.setPa(cfg.getIp(), pa);
                ProtocolManager.setFcn(cfg.getIp(), fcn, pollTmr);
                ProtocolManager.setSync(cfg.getIp(), gps, frmOfs,cnm);
                ProtocolManager.setChannel(cfg.getIp(), null, null, rxGain, null, null, null);

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
