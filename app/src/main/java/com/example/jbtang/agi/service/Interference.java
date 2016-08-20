package com.example.jbtang.agi.service;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.CellInfo;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.MsgSendHelper;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.messages.MessageDispatcher;
import com.example.jbtang.agi.messages.ag2pc.MsgCRS_RSRPQI_INFO;
import com.example.jbtang.agi.messages.ag2pc.MsgL1_PHY_COMMEAS_IND;
import com.example.jbtang.agi.messages.ag2pc.MsgL2P_AG_CELL_CAPTURE_IND;
import com.example.jbtang.agi.messages.ag2pc.MsgL2P_AG_UE_CAPTURE_IND;
import com.example.jbtang.agi.messages.base.MsgTypes;
import com.example.jbtang.agi.trigger.SMSTrigger;
import com.example.jbtang.agi.trigger.Trigger;

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
import java.util.concurrent.TimeUnit;

/**
 * Created by ai on 16/6/19.
 */
public class Interference {
    private static final String TAG = "Interference";
    private static final Interference instance = new Interference();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private Activity currentActivity;

    private Map<String, CountSortedInfo> sTMSI2Count;
    private List<CountSortedInfo> countSortedInfoList;
    private myHandler handler;
    private Trigger trigger;
    private Boolean isInterference;
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


    private Interference() {
        sTMSI2Count = new HashMap<>();
        countSortedInfoList = new ArrayList<>();
        handler = new myHandler(this);
        //trigger = Global.Configuration.type == Status.TriggerType.SMS? SMSTrigger.getInstance(): PhoneTrigger.getInstance();
        trigger = SMSTrigger.getInstance();
        timerMap = new HashMap<>();

    }

    public static Interference getInstance() {
        return instance;
    }

    private static int handlerCount = 0;

    static class myHandler extends Handler {
        private final WeakReference<Interference> mOuter;

        public myHandler(Interference interference) {
            mOuter = new WeakReference<>(interference);
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
                    break;
                case MsgTypes.L1_PHY_COMMEAS_IND_MSG_TYPE:
                    mOuter.get().resolvePhyCommeasIndMsg(globalMsg);
                    Log.e(TAG, "L1_PHY_COMMEAS_IND_MSG_TYPE captured!!!!!!!!!!!!!!!!!!!!!!!");
                    break;
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
        isInterference = ((CheckBox) currentActivity.findViewById(R.id.interference_environment_check)).isChecked();

        service = Status.Service.INTERFERENCE;
        trigger.start(activity, service);

        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            if (device.getCellInfo() != null) {
                timerMap.put(device.getName(), new Timer());
            }
            device.setStartAgain(false);
            device.setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
        }
        for (Map.Entry<String, Timer> entry : timerMap.entrySet()) {
            entry.getValue().schedule(new MyTimerTask(entry.getKey()), 15000);
        }
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                    if (device.getCellInfo() != null && device.getStatus() == Status.DeviceStatus.DISCONNECTED) {
                        device.setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
                        changeDevice(device.getName());
                    }
                }
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    private class MyTimerTask extends TimerTask {
        private String mName;

        public MyTimerTask(String name) {
            mName = name;
        }

        @Override
        public void run() {
            changeDevice(mName);
        }
    }

    private void changeDevice(final String deviceName) {
        MonitorDevice temDevice = DeviceManager.getInstance().getDevice(deviceName);
        if (temDevice == null)
            return;
        if(!temDevice.isStartAgain()){
            temDevice.startMonitor(Status.Service.FINDSTMIS);
            temDevice.setStartAgain(true);
            timerMap.get(deviceName).cancel();
            timerMap.remove(deviceName);
            timerMap.put(deviceName, new Timer());
            timerMap.get(deviceName).schedule(new MyTimerTask(deviceName), 15000);
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(currentActivity,  String.format("%s下行同步丢失，再次同步中...", deviceName), Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
        temDevice.reboot();
        DeviceManager.getInstance().remove(temDevice.getName());
        Log.e(TAG, "Device Status After Reboot" + temDevice.getStatus());
        timerMap.get(deviceName).cancel();
        timerMap.remove(deviceName);
        CellInfo cellInfo = temDevice.getCellInfo();
        String nextDeviceName = "";
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            if (!device.getIsReadyToMonitor() && device.getType() == temDevice.getType() && device.isReady()) {
                device.setCellInfo(cellInfo);
                timerMap.put(device.getName(), new Timer());
                timerMap.get(device.getName()).schedule(new MyTimerTask(device.getName()), 15000);
                device.startMonitor(Status.Service.FINDSTMIS);
                nextDeviceName = device.getName();
                break;
            }
        }
        temDevice.setCellInfo(null);
        final Short pci = cellInfo.pci;
        final String nextName = nextDeviceName;
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (nextName != "")
                    Toast.makeText(currentActivity, String.format("%s下行同步丢失，切换至设备%s！", deviceName, nextName), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(currentActivity, String.format("%s下行同步丢失，重新同步中...", deviceName), Toast.LENGTH_LONG).show();
                if (timerMap.isEmpty()) {
                    trigger.stop();
                    Toast.makeText(currentActivity, "搜索已停止！", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void stop() {
        trigger.stop();
        for (Map.Entry<String, Timer> entry : timerMap.entrySet()) {
            entry.getValue().cancel();
            entry.setValue(null);
        }
        timerMap.clear();
    }

    private void resolveCellCaptureMsg(Global.GlobalMsg globalMsg) {

        MsgL2P_AG_CELL_CAPTURE_IND msg = new MsgL2P_AG_CELL_CAPTURE_IND(globalMsg.getBytes());
        Status.DeviceWorkingStatus status = msg.getMu16TAC() == 0 ? Status.DeviceWorkingStatus.ABNORMAL : Status.DeviceWorkingStatus.NORMAL;
        Float rsrp = msg.getMu16Rsrp() * 1.0F;
        final String deviceName = globalMsg.getDeviceName();
        MonitorDevice monitorDevice = DeviceManager.getInstance().getDevice(deviceName);
        if (monitorDevice == null)
            return;
        monitorDevice.setWorkingStatus(status);
        monitorDevice.getCellInfo().rsrp = rsrp;
        Log.e(TAG, String.format("==========status : %s, rsrp : %f ============", status.name(), rsrp));
        Log.e("cell_capture", "mu16PCI:" + msg.getMu16PCI() + " mu16EARFCN:" + msg.getMu16EARFCN() + " mu16TAC:" + msg.getMu16TAC() + " mu16Rsrp:" + msg.getMu16Rsrp() + " mu16Rsrq:" + msg.getMu16Rsrq());
        if (timerMap.get(monitorDevice.getName()) != null)
            timerMap.get(monitorDevice.getName()).cancel();
        if (status == Status.DeviceWorkingStatus.ABNORMAL) {
            if(!monitorDevice.isStartAgain()){
                monitorDevice.startMonitor(Status.Service.FINDSTMIS);
                monitorDevice.setStartAgain(true);
                timerMap.get(deviceName).cancel();
                timerMap.remove(monitorDevice.getName());
                timerMap.put(deviceName, new Timer());
                timerMap.get(deviceName).schedule(new MyTimerTask(deviceName), 15000);
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(currentActivity,  String.format("%s下行同步丢失，再次同步中...", deviceName), Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }
            timerMap.remove(monitorDevice.getName());
            if (timerMap.isEmpty()) {
                trigger.stop();
                Toast.makeText(currentActivity, "搜索已停止！", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(currentActivity, String.format("%d小区信号过弱！", msg.getMu16PCI()), Toast.LENGTH_LONG).show();
            }
        } else {
            monitorDevice.setStartAgain(false);
        }
    }

    private void resolveUECaptureMsg(Global.GlobalMsg globalMsg) {
        try {
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
            if (stmsi.equals("")) {
                nullCount++;
                return;
            }
            difTime = (currentTime.getTime() - interferenceTime.getTime()) / 1000;
            if (isInterference && difTime < 10) {
                return;
            }
            Log.e(TAG, String.format("---------Find STMSI :%s Time :%d Type :%d-----------", stmsi, difTime, mu8EstCause));
            if (!(mu8EstCause == 0x02)) {
                return;
            }
            int count = 0;

            if (sTMSI2Count.containsKey(stmsi)) {
                count = Integer.valueOf(sTMSI2Count.get(stmsi).count);
            } else if (!isInterference) {
                for (Map.Entry<String, CountSortedInfo> entry : sTMSI2Count.entrySet()) {
                    String temStmsi = entry.getKey();
                    if (stmsi.substring(0, 2).equals(temStmsi.substring(0, 2))) {
                        int j = 0;
                        for (int i = 2; i < 10; i += 2) {
                            if (stmsi.substring(i, i + 2).equals(temStmsi.substring(i, i + 2))) {
                                j++;
                            }
                        }
                        if (j >= 2) {
                            count = Integer.valueOf(sTMSI2Count.get(temStmsi).count);
                            sTMSI2Count.remove(temStmsi);
                            break;
                        }
                    }
                }
            }
            CountSortedInfo info = new CountSortedInfo();

            info.stmsi = stmsi;
            info.count = String.valueOf(count + 1);
            info.time = DATE_FORMAT.format(new Date());
            if (DeviceManager.getInstance().getDevice(globalMsg.getDeviceName()) == null)
                return;
            info.pci = String.valueOf(DeviceManager.getInstance().getDevice(globalMsg.getDeviceName()).getCellInfo().pci);
            info.earfcn = String.valueOf(DeviceManager.getInstance().getDevice(globalMsg.getDeviceName()).getCellInfo().earfcn);
            sTMSI2Count.put(stmsi, info);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void resolvePhyCommeasIndMsg(Global.GlobalMsg globalMsg) {
        /*if (!needToCount) {
            return;
        }*/

        MsgL1_PHY_COMMEAS_IND msg = new MsgL1_PHY_COMMEAS_IND(globalMsg.getBytes());
        if (isCRSChType(msg.getMstL1PHYComentIndHeader().getMu32MeasSelect())) {
            MsgCRS_RSRPQI_INFO crs_rsrpqi_info = new MsgCRS_RSRPQI_INFO(
                    MsgSendHelper.getSubByteArray(globalMsg.getBytes(), MsgL1_PHY_COMMEAS_IND.byteArrayLen, MsgCRS_RSRPQI_INFO.byteArrayLen));
            if (globalMsg.getDeviceName() != null) {
                DeviceManager.getInstance().getDevice(globalMsg.getDeviceName()).getCellInfo().rsrp = crs_rsrpqi_info.getMstCrs0RsrpqiInfo().getMs16CRS_RP() * 0.125F;
            }
        }
    }
    private boolean isCRSChType(long type) {
        return (type & 0x2000) == 0x2000;
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
