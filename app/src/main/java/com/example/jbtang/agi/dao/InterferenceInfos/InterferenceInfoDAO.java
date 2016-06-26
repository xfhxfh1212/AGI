package com.example.jbtang.agi.dao.InterferenceInfos;

/**
 * Created by ai on 16/6/19.
 */
public class InterferenceInfoDAO {
    public String stmsi;
    public String count;
    public String time;
    public String pci;
    public String earfcn;
    private InterferenceInfoDAO(){
        this.stmsi = "";
        this.count = "";
        this.time = "";
        this.pci = "";
        this.earfcn = "";
    }
    public InterferenceInfoDAO(String stmsi, String count, String time, String pci, String earfcn){
        this.stmsi = stmsi;
        this.count = count;
        this.time = time;
        this.pci = pci;
        this.earfcn = earfcn;
    }
}
