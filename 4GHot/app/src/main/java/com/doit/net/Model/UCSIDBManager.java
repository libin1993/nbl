package com.doit.net.Model;

import android.os.Environment;

import com.doit.net.Utils.LogUtils;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.File;

/**
 * Created by wiker on 2016/4/
 * Modeified by zxc
 */
public class UCSIDBManager {

    public static DbManager.DaoConfig daoConfig = new DbManager.DaoConfig()
            .setDbName("ucsi.db")
            // 不设置dbDir时, 默认存储在app的私有目录.
            //.setDbDir(new File("/sdcard/.doit")) // "sdcard"的写法并非最佳实践, 这里为了简单, 先这样写了.
            //.setDbDir(new File(EXPORT_FILE_PATH)) // "sdcard"的写法并非最佳实践, 这里为了简单, 先这样写了.
            .setDbVersion(7)
            .setDbOpenListener(new DbManager.DbOpenListener() {
                @Override
                public void onDbOpened(DbManager db) {
                    // 开启WAL, 对写入加速提升巨大
                    db.getDatabase().enableWriteAheadLogging();
                }
            })
            .setDbUpgradeListener(new DbManager.DbUpgradeListener() {
                @Override
                public void onUpgrade(DbManager db, int oldVersion, int newVersion) {
                    // db.addColumn(...);
                    // db.dropTable(...);
                    // ...
                    // or
                    // db.dropDb();
                    try {
                        db.addColumn(DBUeidInfo.class,"ip");//新增ip字段
                    } catch (DbException e) {
                        e.printStackTrace();
                    }

                }
            });


    public static DbManager DB = x.getDb(daoConfig);
    public static DbManager getDbManager(){
        return DB;
    }

    public static void saveUeidToDB(String imsi,String msisdn, String tmsi, long createDate, String longitude, String latitude,String fcn){
        try {
            DB.save(new DBUeidInfo(imsi, msisdn, tmsi, createDate, longitude, latitude,fcn));
        } catch (DbException e) {
            e.printStackTrace();
            LogUtils.log("采集保存失败："+e.toString());
        }
    }

}
