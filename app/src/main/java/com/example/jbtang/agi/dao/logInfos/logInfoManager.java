package com.example.jbtang.agi.dao.logInfos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.jbtang.agi.core.Global;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ai on 16/8/2.
 */
public class LogInfoManager {
    private SQLiteOpenHelper helper;
    private SQLiteDatabase db;

    public LogInfoManager(Context context) {
        helper = new LogInfoDBHelper(context);
        db = helper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + LogInfoDBHelper.TABLE_NAME +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, userName TEXT, startTime TEXT, endTime TEXT, " +
                "longitude TEXT, latitude TEXT, phone TEXT, targetSTMSI TEXT, findStartTime TEXT, findEndTime TEXT)");
    }

    public void addLogInfo() {
//        db.beginTransaction();
//        try {
        db.execSQL("INSERT INTO " + LogInfoDBHelper.TABLE_NAME + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                new Object[]{null, Global.LogInfo.userName, Global.LogInfo.startTime, Global.LogInfo.endTime, Global.LogInfo.longitude,
                        Global.LogInfo.latitude, Global.LogInfo.phone, Global.LogInfo.targetSTMSI, Global.LogInfo.findStartTime, Global.LogInfo.findEndTime});
        Log.e("Test","insert into loginfo");
//     db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
    }

    public List<LogInfoDAO> listDB() {
        String sql = "SELECT * FROM " + LogInfoDBHelper.TABLE_NAME;
        final Cursor c = db.rawQuery(sql, new String[]{});
        List<LogInfoDAO> logInfoDAOs = new ArrayList<>();
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndex("id"));
            String userName = c.getString(c.getColumnIndex("userName"));
            String startTime = c.getString(c.getColumnIndex("startTime"));
            String endTime = c.getString(c.getColumnIndex("endTime"));
            String longitude = c.getString(c.getColumnIndex("longitude"));
            String latitude = c.getString(c.getColumnIndex("latitude"));
            String phone = c.getString(c.getColumnIndex("phone"));
            String targetSTMSI = c.getString(c.getColumnIndex("targetSTMSI"));
            String findStartTime = c.getString(c.getColumnIndex("findStartTime"));
            String findEndTime = c.getString(c.getColumnIndex("findEndTime"));
            LogInfoDAO logInfoDAO = new LogInfoDAO(id, userName, startTime, endTime, longitude, latitude, phone, targetSTMSI, findStartTime, findEndTime);
            logInfoDAOs.add(logInfoDAO);
        }
        c.close();
        return logInfoDAOs;
    }

    public void clear() {
        db.execSQL("DELETE FROM " + LogInfoDBHelper.TABLE_NAME);
    }

    public void closeDB() {
        db.close();
    }
}
