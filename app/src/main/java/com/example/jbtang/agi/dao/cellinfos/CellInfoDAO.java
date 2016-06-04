package com.example.jbtang.agi.dao.cellinfos;

/**
 * Created by ai on 16/5/11.
 */
public class CellInfoDAO {
    public final int id;
    public final int earfcn;
    public final Short pci;
    public final Short tai;
    public final int ecgi;
    private CellInfoDAO(){
        this.id = 0;
        this.earfcn = 0;
        this.pci = 0;
        this.tai = 0;
        this.ecgi = 0;
    }
    public CellInfoDAO(int id, int earfcn,Short pci,Short tai,int ecgi){
        this.id = id;
        this.earfcn = earfcn;
        this.pci = pci;
        this.tai = tai;
        this.ecgi = ecgi;
    }
}
