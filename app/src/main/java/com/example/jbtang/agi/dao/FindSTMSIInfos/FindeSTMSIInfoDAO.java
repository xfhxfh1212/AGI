package com.example.jbtang.agi.dao.FindSTMSIInfos;

/**
 * Created by ai on 16/6/14.
 */
public class FindeSTMSIInfoDAO {
    public String stmsi;
    public String count;
    public String time;
    public String pci;
    public String earfcn;
    private FindeSTMSIInfoDAO(){
        this.stmsi = "";
        this.count = "";
        this.time = "";
        this.pci = "";
        this.earfcn = "";
    }
    public FindeSTMSIInfoDAO(String stmsi, String count, String time, String pci, String earfcn){
        this.stmsi = stmsi;
        this.count = count;
        this.time = time;
        this.pci = pci;
        this.earfcn = earfcn;
    }
}
