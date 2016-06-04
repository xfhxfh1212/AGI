package com.example.jbtang.agi.dao.cellinfos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.jbtang.agi.core.CellInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by ai on 16/5/11.
 */
public class CellInfoDBManager {
    private SQLiteOpenHelper helper;
    private SQLiteDatabase db;

    public CellInfoDBManager(Context context){
        helper = new CellInfoDBHelper(context);
        db = helper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + CellInfoDBHelper.TABLE_NAME +
                "(id INTEGER PRIMARY KEY, earfcn INTEGER, pci INTEGER, tai INTEGER, ecgi INTEGER)");
    }
    public void add(List<CellInfo> cellInfos){
        db.beginTransaction();
        try{
            int i = 0;
            for(CellInfo cellInfo : cellInfos){
                db.execSQL("INSERT INTO " + CellInfoDBHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?)",
                        new Object[]{i,cellInfo.earfcn,cellInfo.pci,cellInfo.tai,cellInfo.ecgi});
                i++;
            }
            db.setTransactionSuccessful();
        }finally{
            db.endTransaction();
        }
    }
    public void insert(CellInfoDAO dao){
        db.execSQL("INSERT INTO " + CellInfoDBHelper.TABLE_NAME + "VALUES(?,?,?,?,?)",
                new Object[]{dao.id,dao.earfcn,dao.pci,dao.tai,dao.ecgi});
    }
    public List<CellInfoDAO> listDB(){
        String sql = "SELECT * FROM " + CellInfoDBHelper.TABLE_NAME;
        final Cursor c = db.rawQuery(sql,new String[]{});
        List<CellInfoDAO> cellInfoDAOs = new ArrayList<>();
        while(c.moveToNext()){
            int id = c.getInt(c.getColumnIndex("id"));
            int earfcn = c.getInt(c.getColumnIndex("earfcn"));
            Short pci = c.getShort(c.getColumnIndex("pci"));
            Short tai = c.getShort(c.getColumnIndex("tai"));
            int ecgi = c.getInt(c.getColumnIndex("ecgi"));
            CellInfoDAO cellInfo = new CellInfoDAO(id,earfcn,pci,tai,ecgi);
            cellInfoDAOs.add(cellInfo);
        }
        c.close();
        return cellInfoDAOs;
    }
    public void clear(){
        db.execSQL("DELETE FROM " + CellInfoDBHelper.TABLE_NAME);
    }
    public void closeDB(){
        db.close();
    }
}
