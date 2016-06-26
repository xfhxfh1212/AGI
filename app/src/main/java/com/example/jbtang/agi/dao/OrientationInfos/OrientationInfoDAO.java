package com.example.jbtang.agi.dao.OrientationInfos;

/**
 * Created by ai on 16/6/14.
 */
public class OrientationInfoDAO {
    public int num;
    public String pusch;
    public String pci;
    public String earfcn;
    public String time;
    private OrientationInfoDAO(){
        this.num = 0;
        this.pusch = "";
        this.time = "";
        this.pci = "";
        this.earfcn = "";
    }
    public OrientationInfoDAO(int num, String pusch,String pci,String earfcn,String time){
        this.num = num;
        this.pusch = pusch;
        this.time = time;
        this.pci = pci;
        this.earfcn = earfcn;
    }
}
