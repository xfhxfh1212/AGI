package com.example.jbtang.agi.dao.confirmed;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.jbtang.agi.core.CellInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ai on 16/6/14.
 */
public class ConfirmedDBManager {
    private SQLiteOpenHelper helper;
    private SQLiteDatabase db;

    public ConfirmedDBManager(Context context){
        helper = new ConfirmedDBHelper(context);
        db = helper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ConfirmedDBHelper.TABLE_NAME +
                "(id INTEGER PRIMARY KEY, earfcn INTEGER, pci INTEGER, tai INTEGER, ecgi INTEGER)");
    }
    public void add(List<CellInfo> cellInfos){
        db.beginTransaction();
        try{
            int i = 0;
            for(CellInfo cellInfo : cellInfos){
                db.execSQL("INSERT INTO " + ConfirmedDBHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?)",
                        new Object[]{i,cellInfo.earfcn,cellInfo.pci,cellInfo.tai,cellInfo.ecgi});
                i++;
            }
            db.setTransactionSuccessful();
        }finally{
            db.endTransaction();
        }
    }
    public void insert(ConfirmedDAO dao){
        db.execSQL("INSERT INTO " + ConfirmedDBHelper.TABLE_NAME + "VALUES(?,?,?,?,?)",
                new Object[]{dao.id,dao.earfcn,dao.pci,dao.tai,dao.ecgi});
    }
    public List<ConfirmedDAO> listDB(){
        String sql = "SELECT * FROM " + ConfirmedDBHelper.TABLE_NAME;
        final Cursor c = db.rawQuery(sql,new String[]{});
        List<ConfirmedDAO> ConfirmedDAOs = new ArrayList<>();
        while(c.moveToNext()){
            int id = c.getInt(c.getColumnIndex("id"));
            int earfcn = c.getInt(c.getColumnIndex("earfcn"));
            Short pci = c.getShort(c.getColumnIndex("pci"));
            Short tai = c.getShort(c.getColumnIndex("tai"));
            int ecgi = c.getInt(c.getColumnIndex("ecgi"));
            ConfirmedDAO confirmed = new ConfirmedDAO(id,earfcn,pci,tai,ecgi);
            ConfirmedDAOs.add(confirmed);
        }
        c.close();
        return ConfirmedDAOs;
    }
    public void clear(){
        db.execSQL("DELETE FROM " + ConfirmedDBHelper.TABLE_NAME);
    }
    public void closeDB(){
        db.close();
    }
}
