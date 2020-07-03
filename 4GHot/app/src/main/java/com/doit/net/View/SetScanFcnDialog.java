package com.doit.net.View;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.doit.net.Model.PrefManage;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.ucsi.R;

/**
 * Author：Libin on 2020/7/3 09:23
 * Email：1993911441@qq.com
 * Describe：
 */
public class SetScanFcnDialog extends Dialog {
    private View mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mView);

    }

    public SetScanFcnDialog(Context context) {
        super(context, R.style.Theme_dialog);

        initView();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mView = inflater.inflate(R.layout.dialog_scan_fcn, null);
        setCancelable(false);
        EditText etFcn = mView.findViewById(R.id.et_scan_fcn);
        Button btnSave = mView.findViewById(R.id.btn_set_scan_fcn);
        Button btnCancel = mView.findViewById(R.id.btn_cancel_scan_fcn);

        String scanFcn = PrefManage.getString(PrefManage.SCAN_FCN,PrefManage.DEFAULT_SCAN_FCN);
        etFcn.setText(scanFcn);
        etFcn.setSelection(scanFcn.length());

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fcn = etFcn.getText().toString().trim();

                if (TextUtils.isEmpty(fcn) ) {
                    ToastUtils.showMessage("请输入扫网频点");
                    return;
                }

                ToastUtils.showMessage("开始扫描,请耐心等待...");

                PrefManage.setString(PrefManage.SCAN_FCN,fcn);

                ProtocolManager.getNetworkParams(fcn);

                dismiss();
            }
        });
    }
}

