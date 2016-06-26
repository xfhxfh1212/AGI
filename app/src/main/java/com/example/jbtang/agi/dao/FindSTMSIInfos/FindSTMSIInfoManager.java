package com.example.jbtang.agi.dao.FindSTMSIInfos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.jbtang.agi.service.FindSTMSI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ai on 16/6/14.
 */
public class FindSTMSIInfoManager {
    private SQLiteOpenHelper helper;
    private SQLiteDatabase db;

    public FindSTMSIInfoManager(Context context) {
        helper = new FindSTMSIInfoDBHelper(context);
        db = helper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FindSTMSIInfoDBHelper.TABLE_NAME +
                "(stmsi TEXT PRIMARY KEY, count TEXT, time TEXT, pci TEXT, earfcn TEXT)");
    }

    public void add(List<FindSTMSI.CountSortedInfo> cellInfos) {
        db.beginTransaction();
        try {
            for (FindSTMSI.CountSortedInfo countSortedInfo : cellInfos) {
                db.execSQL("INSERT INTO " + FindSTMSIInfoDBHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?)",
                        new Object[]{countSortedInfo.stmsi, countSortedInfo.count, countSortedInfo.time, countSortedInfo.pci, countSortedInfo.earfcn});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insert(FindeSTMSIInfoDAO dao) {
        db.execSQL("INSERT INTO " + FindSTMSIInfoDBHelper.TABLE_NAME + "VALUES(?,?,?,?,?)",
                new Object[]{dao.stmsi, dao.count, dao.time, dao.pci, dao.earfcn});
    }

    public List<FindeSTMSIInfoDAO> listDB() {
        String sql = "SELECT * FROM " + FindSTMSIInfoDBHelper.TABLE_NAME;
        final Cursor c = db.rawQuery(sql, new String[]{});
        List<FindeSTMSIInfoDAO> findeSTMSIInfoDAOs = new ArrayList<>();
        while (c.moveToNext()) {
            String stmsi = c.getString(c.getColumnIndex("stmsi"));
            String count = c.getString(c.getColumnIndex("count"));
            String time = c.getString(c.getColumnIndex("time"));
            String pci = c.getString(c.getColumnIndex("pci"));
            String earfcn = c.getString(c.getColumnIndex("earfcn"));
            FindeSTMSIInfoDAO findeSTMSIInfoDAO = new FindeSTMSIInfoDAO(stmsi, count, time, pci, earfcn);
            findeSTMSIInfoDAOs.add(findeSTMSIInfoDAO);
        }
        c.close();
        return findeSTMSIInfoDAOs;
    }

    public void clear() {
        db.execSQL("DELETE FROM " + FindSTMSIInfoDBHelper.TABLE_NAME);
    }

    public void closeDB() {
        db.close();
    }
}
