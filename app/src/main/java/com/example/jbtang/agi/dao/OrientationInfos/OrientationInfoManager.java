package com.example.jbtang.agi.dao.OrientationInfos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.jbtang.agi.service.OrientationFinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ai on 16/6/14.
 */
public class OrientationInfoManager {
    private SQLiteOpenHelper helper;
    private SQLiteDatabase db;

    public OrientationInfoManager(Context context) {
        helper = new OrientationInfoDBHelper(context);
        db = helper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + OrientationInfoDBHelper.TABLE_NAME +
                "(num INTEGER PRIMARY KEY, pusch TEXT, pci TEXT, earfcn TEXT, time TEXT)");
    }

    public void add(List<OrientationFinding.OrientationInfo> orientationInfos) {
        db.beginTransaction();
        try {
            int i = 0;
            for (OrientationFinding.OrientationInfo orientationInfo : orientationInfos) {
                db.execSQL("INSERT INTO " + OrientationInfoDBHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?)",
                        new Object[]{i,String.valueOf(orientationInfo.PUSCHRsrp), orientationInfo.pci, orientationInfo.earfcn, orientationInfo.timeStamp});
            i++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insert(OrientationInfoDAO dao) {
        db.execSQL("INSERT INTO " + OrientationInfoDBHelper.TABLE_NAME + "VALUES(?,?,?,?,?)",
                new Object[]{dao.num, dao.pusch, dao.pci, dao.earfcn, dao.time});
    }

    public List<OrientationInfoDAO> listDB() {
        String sql = "SELECT * FROM " + OrientationInfoDBHelper.TABLE_NAME;
        final Cursor c = db.rawQuery(sql, new String[]{});
        List<OrientationInfoDAO> orientationInfoDAOs = new ArrayList<>();
        while (c.moveToNext()) {
            int num = c.getInt(c.getColumnIndex("num"));
            String pusch = c.getString(c.getColumnIndex("pusch"));
            String pci = c.getString(c.getColumnIndex("pci"));
            String earfcn = c.getString(c.getColumnIndex("earfcn"));
            String time = c.getString(c.getColumnIndex("time"));
            OrientationInfoDAO orientationInfoDAO = new OrientationInfoDAO(num, pusch, pci, earfcn, time);
            orientationInfoDAOs.add(orientationInfoDAO);
        }
        c.close();
        return orientationInfoDAOs;
    }

    public void clear() {
        db.execSQL("DELETE FROM " + OrientationInfoDBHelper.TABLE_NAME);
    }

    public void closeDB() {
        db.close();
    }
}
