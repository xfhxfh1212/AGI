package com.example.jbtang.agi.dao.InterferenceInfos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import com.example.jbtang.agi.service.Interference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ai on 16/6/19.
 */
public class InterferenceInfoManager {
    private SQLiteOpenHelper helper;
    private SQLiteDatabase db;

    public InterferenceInfoManager(Context context) {
        helper = new InterferenceInfoDBHelper(context);
        db = helper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + InterferenceInfoDBHelper.TABLE_NAME +
                "(stmsi TEXT PRIMARY KEY, count TEXT, time TEXT, pci TEXT, earfcn TEXT)");
    }

    public void add(List<Interference.CountSortedInfo> cellInfos) {
        db.beginTransaction();
        try {
            for (Interference.CountSortedInfo countSortedInfo : cellInfos) {
                db.execSQL("INSERT INTO " + InterferenceInfoDBHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?)",
                        new Object[]{countSortedInfo.stmsi, countSortedInfo.count, countSortedInfo.time, countSortedInfo.pci, countSortedInfo.earfcn});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insert(InterferenceInfoDAO dao) {
        db.execSQL("INSERT INTO " + InterferenceInfoDBHelper.TABLE_NAME + "VALUES(?,?,?,?,?)",
                new Object[]{dao.stmsi, dao.count, dao.time, dao.pci, dao.earfcn});
    }

    public List<InterferenceInfoDAO> listDB() {
        String sql = "SELECT * FROM " + InterferenceInfoDBHelper.TABLE_NAME;
        final Cursor c = db.rawQuery(sql, new String[]{});
        List<InterferenceInfoDAO> InterferenceInfoDAOs = new ArrayList<>();
        while (c.moveToNext()) {
            String stmsi = c.getString(c.getColumnIndex("stmsi"));
            String count = c.getString(c.getColumnIndex("count"));
            String time = c.getString(c.getColumnIndex("time"));
            String pci = c.getString(c.getColumnIndex("pci"));
            String earfcn = c.getString(c.getColumnIndex("earfcn"));
            InterferenceInfoDAO InterferenceInfoDAO = new InterferenceInfoDAO(stmsi, count, time, pci, earfcn);
            InterferenceInfoDAOs.add(InterferenceInfoDAO);
        }
        c.close();
        return InterferenceInfoDAOs;
    }

    public void clear() {
        db.execSQL("DELETE FROM " + InterferenceInfoDBHelper.TABLE_NAME);
    }

    public void closeDB() {
        db.close();
    }
}
