package com.example.jbtang.agi.dao.logInfos;

/**
 * Created by ai on 16/8/2.
 */
public class LogInfoDAO {
    public int id;
    public String userName;
    public String startTime;
    public String endTime;
    public String longitude;
    public String latitude;
    public String phone;
    public String targetSTMSI;
    public String findStartTime;
    public String findEndTime;

    public LogInfoDAO(int id, String userName, String startTime, String endTime, String longitude, String latitude, String phone, String targetSTMSI, String findStartTime, String findEndTime) {
        this.id = id;
        this.userName = userName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.longitude = longitude;
        this.latitude = latitude;
        this.phone = phone;
        this.targetSTMSI = targetSTMSI;
        this.findStartTime = findStartTime;
        this.findEndTime = findEndTime;
    }

}
