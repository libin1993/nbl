package com.doit.net.View;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.doit.net.Sockets.NetConfig;
import com.doit.net.Utils.FormatUtils;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.ucsi.R;

/**
 * Author：Libin on 2020/5/22 17:05
 * Email：1993911441@qq.com
 * Describe：添加频点
 */
public class AddFcnDialog extends Dialog {
    private Context mContext;
    private String mIP;
    private String mFcn;
    private String mTitle;
    private View mView;
    private OnConfirmListener mOnConfirmListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mView);

    }

    public AddFcnDialog(Context context, String title, String ip, String fcn, OnConfirmListener onConfirmListener) {
        super(context, R.style.Theme_dialog);
        mContext = context;
        mOnConfirmListener = onConfirmListener;
        mIP = ip;
        mFcn = fcn;
        mTitle = title;
        initView();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mView = inflater.inflate(R.layout.dialog_add_fcn, null);
        setCancelable(false);
        TextView tvTitle = mView.findViewById(R.id.tv_dialog_title);
        EditText etFcn = mView.findViewById(R.id.et_fcn1);
        Button btnSave = mView.findViewById(R.id.btn_save);
        Button btnCancel = mView.findViewById(R.id.btn_cancel);

        tvTitle.setText(mTitle);
        if (!TextUtils.isEmpty(mFcn)) {
            etFcn.setText(mFcn);
            etFcn.setSelection(mFcn.length());
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = etFcn.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    ToastUtils.showMessage("请输入有效内容");
                    return;
                }

                String[] split = content.split(",");

                String fcn="";
                for (int i = 0; i < split.length; i++) {
                    if (!TextUtils.isEmpty(split[i])) {
                        long intFcn = Long.parseLong(split[i]);
                        if (NetConfig.FDD_IP.equals(mIP)) {
                            if (intFcn < 0 || intFcn > 1949) {
                                ToastUtils.showMessage("请输入1-1949范围内数字");
                                return;
                            }
                        } else {
                            if (intFcn < 37750 || intFcn > 41589) {
                                ToastUtils.showMessage("请输入37750-41589范围内数字");
                                return;
                            }
                        }

                        if (i == split.length-1){
                            fcn += split[i];
                        }else {
                            fcn += split[i]+",";
                        }

                    }
                }
                if (TextUtils.isEmpty(fcn)){
                    ToastUtils.showMessage("请输入有效内容");
                    return;
                }

                if (mOnConfirmListener != null) {
                    mOnConfirmListener.onConfirm(fcn);
                }
                dismiss();
            }
        });
    }


    public interface OnConfirmListener {
        void onConfirm(String value);
    }
}
