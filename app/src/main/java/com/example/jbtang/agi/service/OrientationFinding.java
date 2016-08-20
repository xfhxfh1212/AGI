package com.example.jbtang.agi.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.CellInfo;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.MsgSendHelper;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.device.Device;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.messages.MessageDispatcher;
import com.example.jbtang.agi.messages.ag2pc.MsgCRS_RSRPQI_INFO;
import com.example.jbtang.agi.messages.ag2pc.MsgL1_PHY_COMMEAS_IND;
import com.example.jbtang.agi.messages.ag2pc.MsgL1_PROTOCOL_DATA;
import com.example.jbtang.agi.messages.ag2pc.MsgL1_UL_UE_MEAS;
import com.example.jbtang.agi.messages.ag2pc.MsgL2P_AG_CELL_CAPTURE_IND;
import com.example.jbtang.agi.messages.base.MsgTypes;
import com.example.jbtang.agi.trigger.SMSTrigger;
import com.example.jbtang.agi.trigger.Trigger;
import com.example.jbtang.agi.ui.OrientationFindingActivity;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by jbtang on 11/7/2015.
 */
public class OrientationFinding {
    private static final String TAG = "OrientationFinding";
    private static final Integer UPLINK = 0;
    private static final Integer MAX_RSRP = -45;
    private static final Integer MIN_RSRP = -115;
    private static final double SINR_THRESHOLD = 3.0D;

    private static OrientationFinding instance = new OrientationFinding();
    public static final int PUCCH = 7;
    public static final int PUSCH = 5;
    private static final int COUNT_INTERVAL_CONTINUE = 5;//连续触发上报间隔
    private static final int COUNT_INTERVAL_SINGLE = 5;//单次触发上报间隔
    private static final int RESULT_QUEUE_MAX_LEN = 25;
    private Activity currentActivity;
    private myHandler handler;
    private Trigger trigger;
    private Queue<UEInfo> ueInfoQueue;
    private RsrpResult result;
    private Map<String, List<Float>> cellRSRPMap;
    private Future future;
    //private boolean needToCount;
    private Handler outHandler;
    private Task task;
    private Map<String, Timer> timerMap;
    public String targetStmsi;
    private Button startBtn;
    private String currentDevice;
    private int orientationType;

    public String getCurrentDevice() {
        return this.currentDevice;
    }


    public void setOutHandler(Handler handler) {
        this.outHandler = handler;
    }

    static class UEInfo {
        public int chType;
        public double rsrp;
        public int sinr;
    }

    public static class OrientationInfo {
        public double PUSCHRsrp;
        public double PUCCHRsrp;
        //public Float CellRsrp;
        public String pci;
        public String earfcn;
        public String timeStamp;

        private int standardPusch = Integer.MAX_VALUE;
        private int standardPucch = Integer.MAX_VALUE;

        public int getStandardPusch() {
            if (standardPusch == Integer.MAX_VALUE) {
                standardPusch = format(PUSCHRsrp);
            }
            return standardPusch;
        }

        public int getStandardPucch() {
            if (standardPucch == Integer.MAX_VALUE) {
                standardPucch = format(PUCCHRsrp);
            }
            return standardPucch;
        }

        private static final int STANDARDED_MIN_RSRP = 10;
        private static final int STANDARDED_MAX_RSRP = 100;
        private static final double MIN_RSRP = -115D;
        private static final double MAX_RSRP = -55D;

        private static int format(double rsrp) {
            if (Double.isNaN(rsrp)) {
                return STANDARDED_MIN_RSRP;
            }
            if (rsrp > MAX_RSRP) {
                return STANDARDED_MAX_RSRP;
            }
            return STANDARDED_MIN_RSRP + (int) ((rsrp - MIN_RSRP) / (MAX_RSRP - MIN_RSRP) * (STANDARDED_MAX_RSRP - STANDARDED_MIN_RSRP));
        }
    }

    private OrientationFinding() {
        handler = new myHandler(this);
        result = new RsrpResult(RESULT_QUEUE_MAX_LEN);
        cellRSRPMap = new HashMap<>();
        trigger = SMSTrigger.getInstance();
        //needToCount = false;
        ueInfoQueue = new LinkedList<>();
        timerMap = new HashMap<>();

    }

    public static OrientationFinding getInstance() {
        return instance;
    }

    static class myHandler extends Handler {
        private final WeakReference<OrientationFinding> mOuter;

        public myHandler(OrientationFinding orientationFinding) {
            mOuter = new WeakReference<>(orientationFinding);
        }

        @Override
        public void handleMessage(Message msg) {
            Global.GlobalMsg globalMsg = (Global.GlobalMsg) msg.obj;
            switch (msg.what) {
                case MsgTypes.L1_AG_PROTOCOL_DATA_MSG_TYPE:
                    mOuter.get().resolveProtocolDataMsg(globalMsg);
                    Log.e(TAG, "L1_AG_PROTOCOL_DATA_MSG_TYPE!!!!!!!!!!!!!!!!!!!!!!!");
                    break;
                case MsgTypes.L1_PHY_COMMEAS_IND_MSG_TYPE:
                    mOuter.get().resolvePhyCommeasIndMsg(globalMsg);
                    Log.e(TAG, "L1_PHY_COMMEAS_IND_MSG_TYPE captured!!!!!!!!!!!!!!!!!!!!!!!");
                    break;
                case MsgTypes.L2P_AG_UE_CAPTURE_IND_MSG_TYPE:
                    Log.e(TAG, "L2P_AG_UE_CAPTURE_IND_MSG_TYPE captured!!!!!!!!!!!!!!!!!!!!!!!");
                    //mOuter.get().stopCount();
                    break;
                case MsgTypes.L2P_AG_UE_RELEASE_IND_MSG_TYPE:
                    Log.e(TAG, "L2P_AG_UE_RELEASE_IND_MSG_TYPE released!!!!!!!!!!!!!!!!!!!!!!!!");
                    if (mOuter.get().orientationType == R.id.orientation_find_trigger_single) {
                        mOuter.get().stopCount();
                    }
                    break;
                case MsgTypes.L2P_AG_CELL_CAPTURE_IND_MSG_TYPE:
                    mOuter.get().resolveCellCaptureMsg(globalMsg);
                    Log.e(TAG, "L2P_AG_CELL_CAPTURE_IND_MSG_TYPE captured!!!!!!!!!!!!!!!!!!!!!!!");
                    break;
                default:
                    break;
            }
        }
    }

    private void startCount() {
        //needToCount = true;
    }

    private void stopCount() {
        //needToCount = false;

        ueInfoQueue.clear();
        result.clear();

//        for(Map.Entry<String,List<Float>> entry : cellRSRPMap.entrySet()) {
//            entry.getValue().clear();
//        }
    }

    public void start(Activity activity) {
        currentActivity = activity;
        //DeviceManager.getInstance().getDevices().get(0).setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
        MessageDispatcher.getInstance().RegisterHandler(handler);
        Log.d(TAG, String.format("================== Orientation find stmsi : %s ====================", Global.TARGET_STMSI));
        Log.d(TAG, MsgSendHelper.convertBytesToString(Global.TARGET_STMSI.getBytes()));
        trigger.start(activity, Status.Service.ORIENTATION);
        startBtn = (Button) activity.findViewById(R.id.orientation_find_start);
        orientationType = ((RadioGroup) currentActivity.findViewById(R.id.orientation_find_trigger_type)).getCheckedRadioButtonId();
        task = new Task();
        if (orientationType == R.id.orientation_find_trigger_continue) {
            future = Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(task, COUNT_INTERVAL_CONTINUE, COUNT_INTERVAL_CONTINUE, TimeUnit.SECONDS);
        } else {
            future = Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(task, COUNT_INTERVAL_SINGLE, COUNT_INTERVAL_SINGLE, TimeUnit.SECONDS);
        }

        cellRSRPMap.clear();
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            if (device.getCellInfo() != null) {
                timerMap.put(device.getName(), new Timer());
                cellRSRPMap.put(device.getName(), new ArrayList<Float>());
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
            temDevice.startMonitor(Status.Service.ORIENTATION);
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
        timerMap.get(deviceName).cancel();
        timerMap.remove(deviceName);
//        timerMap.get(deviceName).cancel();
//        timerMap.put(deviceName, new Timer());
//        timerMap.get(deviceName).schedule(new MyTimerTask(deviceName), 15000);
        CellInfo cellInfo = temDevice.getCellInfo();
        String nextDeviceName = "";
        for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            if (!device.getIsReadyToMonitor() && device.getType() == temDevice.getType() && device.isReady()) {
                device.setCellInfo(cellInfo);
                timerMap.put(device.getName(), new Timer());
                timerMap.get(device.getName()).schedule(new MyTimerTask(device.getName()), 15000);
                cellRSRPMap.put(device.getName(), new ArrayList<Float>());
                device.startMonitor(Status.Service.ORIENTATION);
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
        if (future != null) {
            future.cancel(true);
        }
        for (Map.Entry<String, Timer> entry : timerMap.entrySet()) {
            entry.getValue().cancel();
            entry.setValue(null);
        }
        timerMap.clear();
        task = null;
        future = null;
        stopCount();

    }

    private void resolveProtocolDataMsg(Global.GlobalMsg globalMsg) {
        /*if (!needToCount) {
            return;
        }*/

        UEInfo ueInfo = new UEInfo();

        MsgL1_PROTOCOL_DATA msg = new MsgL1_PROTOCOL_DATA(globalMsg.getBytes());
        if (msg.getMstL1ProtocolDataHeader().getMu8Direction() == UPLINK) {
            MsgL1_UL_UE_MEAS ul_ue_meas = new MsgL1_UL_UE_MEAS(
                    MsgSendHelper.getSubByteArray(globalMsg.getBytes(), MsgL1_PROTOCOL_DATA.byteArrayLen + 100, MsgL1_UL_UE_MEAS.byteArrayLen));
            ueInfo.chType = ul_ue_meas.getMuChType();
            ueInfo.rsrp = ul_ue_meas.getMs16Power() * 0.125;
            ueInfo.sinr = (int) (ul_ue_meas.getMs8Sinr() * 0.5);
            currentDevice = globalMsg.getDeviceName();

            Log.d(TAG, String.format("**************Type = %d, RSRP = %f, SINR = %d ************ ", ueInfo.chType, ueInfo.rsrp, ueInfo.sinr));
            ueInfoQueue.add(ueInfo);
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
            if (cellRSRPMap.get(globalMsg.getDeviceName()) != null) {
                cellRSRPMap.get(globalMsg.getDeviceName()).add(crs_rsrpqi_info.getMstCrs0RsrpqiInfo().getMs16CRS_RP() * 0.125F);
            }
        }
    }

    private void resolveCellCaptureMsg(Global.GlobalMsg globalMsg) {
        MsgL2P_AG_CELL_CAPTURE_IND msg = new MsgL2P_AG_CELL_CAPTURE_IND(globalMsg.getBytes());
        Status.DeviceWorkingStatus status = msg.getMu16TAC() == 0 ? Status.DeviceWorkingStatus.ABNORMAL : Status.DeviceWorkingStatus.NORMAL;
        Float rsrp = msg.getMu16Rsrp() * 1.0F;
        int pci = msg.getMu16PCI();
        final String deviceName = globalMsg.getDeviceName();
        MonitorDevice monitorDevice = DeviceManager.getInstance().getDevice(deviceName);
        if (monitorDevice == null)
            return;
        monitorDevice.cancleCheckCellCaoture();
        monitorDevice.setWorkingStatus(status);
        monitorDevice.getCellInfo().rsrp = rsrp;
        Log.e(TAG, String.format("==========status : %s, rsrp : %f ============", status.name(), rsrp) + "PCI:" + pci);
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
                Toast.makeText(currentActivity, String.format("%d小区信号过弱！", pci), Toast.LENGTH_LONG).show();
            }
        } else {
            monitorDevice.setStartAgain(false);
        }
    }

    private boolean isCRSChType(long type) {
        return (type & 0x2000) == 0x2000;
    }

    class Task implements Runnable {
        @Override
        public void run() {
            result.log();

            Message msg = new Message();
            msg.what = 1;
            if (/*needToCount &&*/ !ueInfoQueue.isEmpty()) {
                msg.obj = getOrientationInfo();
            } else {
                msg.obj = null;
            }
            for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                if (device.getIsReadyToMonitor()) {
                    device.getCellInfo().rsrp = getCellRsrp(device.getName());
                }
            }

            outHandler.sendMessage(msg);
        }
    }

    private OrientationInfo getOrientationInfo() {
        Log.e("OrientationActivity", "ueInfoQueue size" + ueInfoQueue.size());
        result.consumeQueue(ueInfoQueue.size());
        OrientationInfo info = new OrientationInfo();
        info.timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date());
        //info.PUCCHRsrp = result.average(PUCCH);
        info.PUSCHRsrp = result.average(PUSCH);
        String currentDevice = OrientationFinding.getInstance().getCurrentDevice();
        MonitorDevice device = DeviceManager.getInstance().getDevice(currentDevice);
        if (device != null && device.getCellInfo() != null) {
            info.pci = device.getCellInfo().pci + "";
            info.earfcn = device.getCellInfo().earfcn + "";
        }
        //info.CellRsrp = getCellRsrp();
        //if(DeviceManager.getInstance().getDevices().size() > 0)
        //    DeviceManager.getInstance().getDevices().get(0).getCellInfo().rsrp = info.CellRsrp;
        Log.e(TAG, String.format("============ PUCCH = %f , PUSCH = %f, Time = %s. ==============",
                info.PUCCHRsrp, info.PUSCHRsrp, info.timeStamp));
        if (orientationType == R.id.orientation_find_trigger_continue) {
            stopCount();
        }
        return info;
    }

    private Float getCellRsrp(String deviceName) {
        Float ret = 0F;
        List<Float> cellRSRPList = cellRSRPMap.get(deviceName);
        for (int index = 0; index < cellRSRPList.size(); index++) {
            ret += cellRSRPList.get(index);
        }
        ret /= cellRSRPList.size();
        cellRSRPList.clear();
        return ret;
    }

    class RsrpResult {
        public static final double INVALID_RSRP = Double.NaN;

        private Map<Integer, Queue<UEInfo>> results;
        private int maxLen;

        public RsrpResult(int maxLen) {
            results = new HashMap<>();
            this.maxLen = maxLen;
        }

        public void consumeQueue(int count) {
            while (count-- > 0) {
                UEInfo info = ueInfoQueue.poll();
                add(info);
            }
        }

        private void add(UEInfo info) {
            if (validate(info)) {
                int type = info.chType;
                add(info, type);
            }
        }

        private void add(UEInfo info, int type) {
            if (!results.containsKey(type)) {
                results.put(type, new LinkedList<UEInfo>());
            }
            Queue<UEInfo> queue = results.get(type);
            if (queue.size() == maxLen) {
                queue.poll();
            }
            queue.add(info);
        }

        public double average(int type) {
            if (!results.containsKey(type)) {
                return INVALID_RSRP;
            }
            List<Double> rsrpList = new ArrayList<>();
            for (UEInfo info : results.get(type)) {
                rsrpList.add(info.rsrp);
            }

            Log.e("OrientationActivity", "rsrpList size" + rsrpList.size());
            double ret;
            Collections.sort(rsrpList);
            if (rsrpList.size() >= 10) {
                int from = 3;
                int to = rsrpList.size() - 2;
                ret = getAverage(rsrpList.subList(from, to));
            } else if (rsrpList.size() >= 5) {
                int from = 1;
                int to = rsrpList.size() - 1;
                ret = getAverage(rsrpList.subList(from, to));
            } else if (rsrpList.size() > 0) {
                ret = getAverage(rsrpList);
            } else {
                ret = INVALID_RSRP;
            }

            return ret;
        }

        public void clear() {
            results.clear();
        }

        private double getAverage(List<Double> list) {
            Double ret = 0.0;
            for (Double item : list) {
                ret += item;
            }
            return ret / list.size();
        }

        private boolean validate(UEInfo info) {
            if (info == null) {
                return false;
            }
            if (info.rsrp < MIN_RSRP || info.sinr < SINR_THRESHOLD) {
                return false;
            }
            if (info.rsrp > MAX_RSRP) {
                info.rsrp = MAX_RSRP;
            }
            return true;
        }

        public void log() {
            Log.d(TAG, "==============================================================");
            for (Map.Entry<Integer, Queue<UEInfo>> entry : results.entrySet()) {
                StringBuilder builder = new StringBuilder();
                builder.append(entry.getKey()).append(" : ");
                for (UEInfo info : entry.getValue()) {
                    builder.append(info.rsrp).append(", ");
                }
                Log.d(TAG, builder.toString());
            }
            Log.d(TAG, "==============================================================");
        }
    }
}
