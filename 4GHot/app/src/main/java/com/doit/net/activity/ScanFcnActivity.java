package com.doit.net.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.doit.net.Model.DBScanFcn;
import com.doit.net.Model.PrefManage;
import com.doit.net.Model.UCSIDBManager;
import com.doit.net.Protocol.ProtocolManager;
import com.doit.net.Utils.MySweetAlertDialog;
import com.doit.net.Utils.ToastUtils;
import com.doit.net.base.BaseActivity;
import com.doit.net.ucsi.R;

import org.xutils.DbManager;
import org.xutils.db.Selector;
import org.xutils.ex.DbException;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Author：Libin on 2020/7/13 10:10
 * Email：1993911441@qq.com
 * Describe：扫网频点
 */
public class ScanFcnActivity extends BaseActivity {
    @BindView(R.id.cb_scn_fcn)
    CheckBox cbScnFcn;
    @BindView(R.id.et_input_fcn)
    EditText etInputFcn;
    @BindView(R.id.btn_add_fcn)
    Button btnAddFcn;
    @BindView(R.id.rv_scan_fcn)
    RecyclerView rvScanFcn;
    @BindView(R.id.cb_select_all_fcn)
    CheckBox cbSelectAllFcn;
    @BindView(R.id.btn_save_fcn)
    Button btnSaveFcn;
    @BindView(R.id.btn_delete_fcn)
    Button btnDeleteFcn;


    private BaseQuickAdapter<DBScanFcn, BaseViewHolder> adapter;
    private List<DBScanFcn> dataList = new ArrayList<>();
    private DbManager dbManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_fcn);
        ButterKnife.bind(this);
        dbManager = UCSIDBManager.getDbManager();
        initView();
        initData();
    }

    private void initView() {
        cbScnFcn.setChecked(PrefManage.getBoolean(PrefManage.AUTO_SCAN_FCN, false));
        cbScnFcn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefManage.setBoolean(PrefManage.AUTO_SCAN_FCN, isChecked);

                ToastUtils.showMessage("设置成功，下次启动APP生效");
            }
        });

        rvScanFcn.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BaseQuickAdapter<DBScanFcn, BaseViewHolder>(R.layout.layout_scn_fcn_item, dataList) {

            @Override
            protected void convert(BaseViewHolder helper, DBScanFcn item) {
                ImageView ivCheck = helper.getView(R.id.iv_select_scan_fcn);
                if (item.isCheck() == 1) {
                    ivCheck.setImageResource(R.mipmap.ic_fcn_checked);
                } else {
                    ivCheck.setImageResource(R.mipmap.ic_fcn_normal);
                }

                helper.setText(R.id.tv_scan_fcn, item.getFcn() + "");

                helper.addOnClickListener(R.id.ll_delete_scan_fcn);
                helper.addOnClickListener(R.id.iv_select_scan_fcn);
            }

        };

        rvScanFcn.setAdapter(adapter);

        adapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                switch (view.getId()) {
                    case R.id.ll_delete_scan_fcn:
                        deleteFcn(position);
                        break;
                    case R.id.iv_select_scan_fcn:
                        checkFcn(position);
                        break;
                }

            }
        });

        cbSelectAllFcn.setOnCheckedChangeListener(onCheckedChangeListener);

        etInputFcn.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    //添加
                    addFcn();
                    return true;
                }

                return false;
            }
        });
    }

    /**
     * 选中、取消扫网频点
     */
    private void checkFcn(int position) {
        DBScanFcn scanFcn = dataList.get(position);
        scanFcn.setCheck(scanFcn.isCheck() == 1 ? 0 : 1);

        try {
            dbManager.update(scanFcn, "is_check");
        } catch (DbException e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();

        initStatus();


    }

    //全选按钮点击
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            for (DBScanFcn scanFcn : dataList) {
                scanFcn.setCheck(isChecked ? 1 : 0);

                try {
                    dbManager.update(scanFcn, "is_check");
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }
            adapter.notifyDataSetChanged();
        }
    };

    /**
     * 查询数据库
     */
    private void initData() {
        dataList.clear();
        try {
            List<DBScanFcn> all = dbManager.selector(DBScanFcn.class)
                    .where("status", "=", 1)
                    .orderBy("fcn").findAll();
            dataList.addAll(all);
        } catch (DbException e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();

        initStatus();
    }

    /**
     * 更新全选按钮状态
     */
    private void initStatus() {
        int count = getCheckCount();
        //更改状态时取消监听
        cbSelectAllFcn.setOnCheckedChangeListener(null);
        cbSelectAllFcn.setChecked(count > 0 && count == dataList.size());

        cbSelectAllFcn.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    /**
     * @return 选中数量
     */
    private int getCheckCount(){
        int checkCount = 0;
        for (DBScanFcn scanFcn : dataList) {
            if (scanFcn.isCheck() == 1) {
                checkCount++;
            }
        }

        return checkCount;

    }

    /**
     * 新增频点
     */
    private void addFcn(){
        String fcnStr = etInputFcn.getText().toString().trim();
        if (TextUtils.isEmpty(fcnStr)){
            ToastUtils.showMessage("请输入频点");
            return;
        }
        int fcn = Integer.parseInt(fcnStr);
        if (fcn == 0){
            ToastUtils.showMessage("请输入有效频点");
            return;
        }

        try {
            DBScanFcn scanFcn = dbManager.selector(DBScanFcn.class).where("fcn", "=", fcn).findFirst();
            if (scanFcn == null){  //无则新增
                dbManager.save(new DBScanFcn(fcn,1,1));
            }else {
                if (scanFcn.getStatus() ==1){
                    ToastUtils.showMessage("已存在相同频点");  //已存在
                    return;
                }else {       //已删除则修改
                    scanFcn.setCheck(1);
                    scanFcn.setStatus(1);
                    dbManager.update(scanFcn);
                }
            }


            ToastUtils.showMessage("添加成功");
            initData();

        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除频点
     */
    private void deleteFcn(int position){

        new MySweetAlertDialog(this, MySweetAlertDialog.WARNING_TYPE)
                .setTitleText("删除频点")
                .setContentText("确定要删除频点吗？")
                .showCancelButton(true)
                .setCancelText(getString(R.string.cancel))
                .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(MySweetAlertDialog sDialog) {
                        sDialog.dismiss();

                        DBScanFcn scanFcn = dataList.get(position);
                        scanFcn.setStatus(0);
                        scanFcn.setCheck(0);
                        try {
                            dbManager.update(scanFcn);
                        } catch (DbException e) {
                            e.printStackTrace();
                        }
                        dataList.remove(scanFcn);
                        adapter.notifyDataSetChanged();
                        initStatus();
                        ToastUtils.showMessage("删除成功");
                    }
                })
                .show();

    }

    /**
     * 删除频点
     */
    private void deleteFcn(){
        if (getCheckCount() == 0){
            ToastUtils.showMessage("请选择频点");
            return;
        }
        new MySweetAlertDialog(this, MySweetAlertDialog.WARNING_TYPE)
                .setTitleText("删除频点")
                .setContentText("确定要删除频点吗？")
                .showCancelButton(true)
                .setCancelText(getString(R.string.cancel))
                .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(MySweetAlertDialog sDialog) {
                        sDialog.dismiss();
                        for (DBScanFcn scanFcn : dataList) {
                            if (scanFcn.isCheck() == 1){
                                scanFcn.setStatus(0);
                                scanFcn.setCheck(0);
                                try {
                                    dbManager.update(scanFcn);
                                } catch (DbException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        ToastUtils.showMessage("删除成功");
                        initData();
                    }
                })
                .show();

    }

    /**
     * 扫网
     */
    private void scanFcn() {
        if (getCheckCount() ==0){
            ToastUtils.showMessage("请选择扫网频点");
        }
        ProtocolManager.getNetworkParams();
    }



    @OnClick({R.id.btn_add_fcn, R.id.btn_save_fcn,R.id.btn_delete_fcn})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_add_fcn:
                addFcn();
                break;
            case R.id.btn_save_fcn:
                scanFcn();
                break;
            case R.id.btn_delete_fcn:
                deleteFcn();
                break;
        }
    }


}
