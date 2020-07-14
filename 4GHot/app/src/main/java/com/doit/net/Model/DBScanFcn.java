package com.doit.net.Model;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * Author：Libin on 2020/7/13 09:56
 * Email：1993911441@qq.com
 * Describe：扫网频点
 */

@Table(name = "scan_fcn")
public class DBScanFcn {
    @Column(name = "id", isId = true)
    private int id;

    @Column(name = "fcn")
    private int fcn;

    @Column(name = "is_check")
    private int isCheck;

    @Column(name = "status")
    private int status;

    public DBScanFcn(int fcn, int isCheck, int status) {
        this.fcn = fcn;
        this.isCheck = isCheck;
        this.status = status;
    }

    public DBScanFcn() {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFcn() {
        return fcn;
    }

    public void setFcn(int fcn) {
        this.fcn = fcn;
    }

    public int isCheck() {
        return isCheck;
    }

    public void setCheck(int isCheck) {
        this.isCheck = isCheck;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
