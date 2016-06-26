package com.example.jbtang.agi.dao.confirmed;

/**
 * Created by ai on 16/6/14.
 */
public class ConfirmedDAO {
    public final int id;
    public final int earfcn;
    public final Short pci;
    public final Short tai;
    public final int ecgi;
    private ConfirmedDAO(){
        this.id = 0;
        this.earfcn = 0;
        this.pci = 0;
        this.tai = 0;
        this.ecgi = 0;
    }
    public ConfirmedDAO(int id, int earfcn,Short pci,Short tai,int ecgi){
        this.id = id;
        this.earfcn = earfcn;
        this.pci = pci;
        this.tai = tai;
        this.ecgi = ecgi;
    }
}
