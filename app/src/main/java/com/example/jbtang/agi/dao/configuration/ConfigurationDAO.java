package com.example.jbtang.agi.dao.configuration;

import com.example.jbtang.agi.core.Status;

/**
 * Created by jbtang on 11/5/2015.
 */
public class ConfigurationDAO {
    public final String name;
    public final Status.TriggerType type;
    public final Status.TriggerSMSType smsType;
    public final Status.InsideSMSType insideSMSType;
    public final Status.SilentSMSType silentSMSType;
    public final int triggerInterval;
    public final int filterInterval;
    public final int silenceCheckTimer;
    public final int receivingAntennaNum;
    public final int totalTriggerCount;
    public final String targetPhoneNum;
    public final String smsCenter;
    private ConfigurationDAO() {
        this.name = "";
        this.type = Status.TriggerType.SMS;
        this.smsType = Status.TriggerSMSType.INSIDE;
        this.insideSMSType = Status.InsideSMSType.NORMAL;
        this.silentSMSType = Status.SilentSMSType.TYPE_ONE;
        this.triggerInterval = 0;
        this.filterInterval = 0;
        this.silenceCheckTimer = 0;
        this.receivingAntennaNum = 0;
        this.totalTriggerCount = 0;
        this.targetPhoneNum = "";
        this.smsCenter = "";
    }

    public ConfigurationDAO(String name, Status.TriggerType type, Status.TriggerSMSType smsType,Status.InsideSMSType insideSMSType,
                            Status.SilentSMSType silentSMSType, int triggerInterval, int filterInterval, int silenceCheckTimer,
                            int receivingAntennaNum, int totalTriggerCount, String targetPhoneNum, String smsCenter) {
        this.name = name;
        this.type = type;
        this.smsType = smsType;
        this.insideSMSType = insideSMSType;
        this.silentSMSType = silentSMSType;
        this.triggerInterval = triggerInterval;
        this.filterInterval = filterInterval;
        this.silenceCheckTimer = silenceCheckTimer;
        this.receivingAntennaNum = receivingAntennaNum;
        this.totalTriggerCount = totalTriggerCount;
        this.targetPhoneNum = targetPhoneNum;
        this.smsCenter = smsCenter;
    }
}
