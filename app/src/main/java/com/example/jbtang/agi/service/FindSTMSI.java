package com.example.jbtang.agi.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.core.type.U32;
import com.example.jbtang.agi.core.type.U8;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.messages.MessageDispatcher;
import com.example.jbtang.agi.messages.ag2pc.MsgL2P_AG_CELL_CAPTURE_IND;
import com.example.jbtang.agi.messages.ag2pc.MsgL2P_AG_UE_CAPTURE_IND;
import com.example.jbtang.agi.messages.base.MsgTypes;
import com.example.jbtang.agi.trigger.PhoneTrigger;
import com.example.jbtang.agi.trigger.SMSTrigger;
import com.example.jbtang.agi.trigger.Trigger;
import com.example.jbtang.agi.ui.FindSTMSIActivity;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;


/**
 * Created by jbtang on 11/1/2015.
 */
public class FindSTMSI {
    private static final String TAG = "FindSTMSI";
    private static final FindSTMSI instance = new FindSTMSI();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private Activity currentActivity;

    private Map<String, CountSortedInfo> sTMSI2Count;
    private List<CountSortedInfo> countSortedInfoList;
    private myHandler handler;
    private Trigger trigger;
    private CheckBox filterCheckBox;
    private Button startBtn;
    private Status.Service service;
    public int stmsiCount;
    public int sumCount;
    public int nullCount;
    private Date interferenceTime;
    private Map<String, Timer> timerMap;
    public List<CountSortedInfo> getCountSortedInfoList() {
        countSortedInfoList.clear();
        CountSortedInfo info;
        Set<Map.Entry<String, CountSortedInfo>> sortedStmsi = getSortedSTMSI();
        for (Map.Entry<String, CountSortedInfo> entry : sortedStmsi) {
            info = entry.getValue();
            countSortedInfoList.add(info);
        }
        return countSortedInfoList;
    }


    private FindSTMSI() {
        sTMSI2Count = new HashMap<>();
        countSortedInfoList = new ArrayList<>();
        handler = new myHandler(this);
        //trigger = Global.Configuration.type == Status.TriggerType.SMS? SMSTrigger.getInstance(): PhoneTrigger.getInstance();
        trigger = SMSTrigger.getInstance();
        timerMap = new HashMap<>();

    }

    public static FindSTMSI getInstance() {
        return instance;
    }
    private static int handlerCount = 0;
    static class myHandler extends Handler {
        private final WeakReference<FindSTMSI> mOuter;

        public myHandler(FindSTMSI findSTMSI) {
            mOuter = new WeakReference<>(findSTMSI);
        }

        @Override
        public void handleMessage(Message msg) {
            Global.GlobalMsg globalMsg = (Global.GlobalMsg) msg.obj;
            switch (msg.what) {
                case MsgTypes.L2P_AG_UE_CAPTURE_IND_MSG_TYPE:
                    mOuter.get().resolveUECaptureMsg(globalMsg);
                    break;
                case MsgTypes.L2P_AG_CELL_CAPTURE_IND_MSG_TYPE:
                    mOuter.get().resolveCellCaptureMsg(globalMsg);
                default:
                    break;
            }
        }
    }

    public void start(Activity activity) {
        currentActivity = activity;
        MessageDispatcher.getInstance().RegisterHandler(handler);
        stmsiCount = 0;
        sumCount = 0;
        nullCount = 0;
        sTMSI2Count.clear();
        countSortedInfoList.clear();
        interferenceTime = new Date();
        Log.e("Test", activity.getLocalClassName());
        if(activity.getLocalClassName().equals("ui.FindSTMSIActivity")) {

            filterCheckBox = (CheckBox) activity.findViewById(R.id.find_stmsi_filter_checkbox);
            startBtn = (Button) activity.findViewById(R.id.find_stmsi_start_button);
            service = Status.Service.FINDSTMIS;
            trigger.start(activity, service);
        }else {
            service = Status.Service.INTERFERENCE;
            trigger.start(activity, service);
        }
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            timerMap.put(device.getName(), new Timer());
        }
        for(Map.Entry<String, Timer> entry : timerMap.entrySet()) {
            entry.getValue().schedule(new MyTimerTask(entry.getKey()), 10000);
        }
    }
    private class MyTimerTask extends TimerTask {
        private String mName;
        public MyTimerTask(String name) {
            mName = name;
        }
        @Override
        public void run() {
            MonitorDevice device = DeviceManager.getInstance().getDevice(mName);
            if(device.isWorking()) {
                device.reboot();
                timerMap.get(mName).cancel();
                timerMap.remove(mName);
                Toast.makeText(currentActivity, String.format("%d小区驻留失败，%s正在恢复初始状态！", device.getCellInfo().pci, mName), Toast.LENGTH_SHORT).show();
                if(timerMap.isEmpty()) {
                    trigger.stop();
                    startBtn.setEnabled(true);
                    Toast.makeText(currentActivity, "搜索已停止！", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }
    public void stop() {
        trigger.stop();
    }

    private void resolveCellCaptureMsg(Global.GlobalMsg globalMsg) {

        MsgL2P_AG_CELL_CAPTURE_IND msg = new MsgL2P_AG_CELL_CAPTURE_IND(globalMsg.getBytes());
        Status.DeviceWorkingStatus status = msg.getMu16Rsrp() == 0 ? Status.DeviceWorkingStatus.ABNORMAL : Status.DeviceWorkingStatus.NORMAL;
        Float rsrp = msg.getMu16Rsrp()*1.0F;
        MonitorDevice monitorDevice = DeviceManager.getInstance().getDevice(globalMsg.getDeviceName());
        monitorDevice.setWorkingStatus(status);
        monitorDevice.getCellInfo().rsrp = rsrp;
        Log.e(TAG, String.format("==========status : %s, rsrp : %f ============", status.name(), rsrp));
        Log.e("cell_capture", "mu16PCI:" + msg.getMu16PCI() + " mu16EARFCN:" + msg.getMu16EARFCN() + " mu16TAC:" + msg.getMu16TAC() + " mu16Rsrp:" + msg.getMu16Rsrp() + " mu16Rsrq:" + msg.getMu16Rsrq());
        timerMap.get(monitorDevice.getName()).cancel();
        if(status == Status.DeviceWorkingStatus.ABNORMAL) {
            timerMap.remove(monitorDevice.getName());
            if (timerMap.isEmpty()) {
                trigger.stop();
                startBtn.setEnabled(true);
                Toast.makeText(currentActivity, "搜索已停止！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(currentActivity, String.format("%d小区驻留失败！",msg.getMu16PCI()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resolveUECaptureMsg(Global.GlobalMsg globalMsg) {
        stmsiCount++;
        sumCount++;
        long difTime;
        Date currentTime = new Date();

        MsgL2P_AG_UE_CAPTURE_IND msg = new MsgL2P_AG_UE_CAPTURE_IND(globalMsg.getBytes());
        String stmsi = "";
        byte mec = 0;
        int mu8EstCause = 0;
        if ((msg.getMstUECaptureInfo().getMu8UEIDTypeFlg() & 0x20) == 0x20) {
            mec = msg.getMstUECaptureInfo().getMau8GUTIDATA()[5].getBytes()[0];
            byte[] stmsiBytes = new byte[4];
            stmsiBytes[3] = msg.getMstUECaptureInfo().getMau8GUTIDATA()[6].getBytes()[0];
            stmsiBytes[2] = msg.getMstUECaptureInfo().getMau8GUTIDATA()[7].getBytes()[0];
            stmsiBytes[1] = msg.getMstUECaptureInfo().getMau8GUTIDATA()[8].getBytes()[0];
            stmsiBytes[0] = msg.getMstUECaptureInfo().getMau8GUTIDATA()[9].getBytes()[0];
            mu8EstCause = msg.getMstUECaptureInfo().getMu8Pading1();
            stmsi = new StringBuilder().append(padLeft(String.format("%X", mec), "0", 2))
                    .append(padLeft(String.format("%X", stmsiBytes[0]), "0", 2))
                    .append(padLeft(String.format("%X", stmsiBytes[1]), "0", 2))
                    .append(padLeft(String.format("%X", stmsiBytes[2]), "0", 2))
                    .append(padLeft(String.format("%X", stmsiBytes[3]), "0", 2))
                    .toString();
        }
        if(stmsi.equals("")) {
            nullCount++;
            return;
        }
        if(service == Status.Service.FINDSTMIS) {
            difTime = (currentTime.getTime() - Global.sendTime.getTime()) / 1000;
            if (difTime > Global.Configuration.filterInterval) {
                return;
            }
        }else {
            difTime = (currentTime.getTime() - interferenceTime.getTime()) / 1000;
            if(difTime < 8) {
                return;
            }
        }
        Log.e(TAG, String.format("---------Find STMSI :%s Time :%d Type :%d-----------", stmsi,difTime ,mu8EstCause));
        if (!(mu8EstCause == 0x02)) {
            return;
        }
        if(service == Status.Service.FINDSTMIS && filterCheckBox.isChecked() && Global.filterStmsiMap.containsKey(stmsi))
            return;
        int count = sTMSI2Count.containsKey(stmsi) ? Integer.valueOf(sTMSI2Count.get(stmsi).count) : 0;
        CountSortedInfo info = new CountSortedInfo();

        info.stmsi = stmsi;
        info.count = String.valueOf(count + 1);
        info.time = DATE_FORMAT.format(new Date());
        info.pci = String.valueOf(DeviceManager.getInstance().getDevice(globalMsg.getDeviceName()).getCellInfo().pci);
        info.earfcn =String.valueOf(DeviceManager.getInstance().getDevice(globalMsg.getDeviceName()).getCellInfo().earfcn);
        sTMSI2Count.put(stmsi, info);
    }

    private Set<Map.Entry<String, CountSortedInfo>> getSortedSTMSI() {
        Set<Map.Entry<String, CountSortedInfo>> sortedSTMSI = new TreeSet<>(
                new Comparator<Map.Entry<String, CountSortedInfo>>() {
                    public int compare(Map.Entry<String, CountSortedInfo> o1, Map.Entry<String, CountSortedInfo> o2) {
                        Integer d1 = new Integer(o1.getValue().count);
                        Integer d2 = new Integer(o2.getValue().count);
                        int r = d2.compareTo(d1);
                        if (r != 0) {
                            return r;
                        } else {
                            return o2.getKey().compareTo(o1.getKey());
                        }
                    }
                }
        );
        sortedSTMSI.addAll(sTMSI2Count.entrySet());
        return sortedSTMSI;

    }

    private String padLeft(String src, String pad, int len) {
        StringBuilder builder = new StringBuilder();
        len -= src.length();
        while (len-- > 0) {
            builder.append(pad);
        }
        builder.append(src);
        return builder.toString();
    }

    public static class CountSortedInfo {
        public String stmsi;
        public String count;
        public String time;
        public String pci;
        public String earfcn;
    }

}