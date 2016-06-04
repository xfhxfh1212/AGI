package com.example.jbtang.agi.device;

import android.os.AsyncTask;
import android.util.Log;

import com.example.jbtang.agi.core.CellInfo;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.messages.GenProtocolTraceMsg;
import com.example.jbtang.agi.messages.GetFrequentlyUsedMsg;
import com.example.jbtang.agi.service.OrientationFinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

/**
 * Created by jbtang on 11/1/2015.
 */
public class MonitorDevice extends Device {

    public static final String DEVICE_NAME_PREFIX = "AGI";
    public static final int DATA_PORT = 3333;
    public static final int MESSAGE_PORT = 3334;

    private static final String REBOOT_USERNAME = "root";
    private static final String REBOOT_PASSWORD = "13M1877";
    private static final int REBOOT_PORT = 22;
    private static final String REBOOT_CMD = "reboot";

    private CellInfo cellInfo;
    private boolean isReadyToMonitor;
    private Status.BoardType type;
    private Status.DeviceWorkingStatus workingStatus;
    private Status.PingResult pingStatus;
    private boolean pingStart;
    private Timer timer;

    public Status.PingResult getPingStatus() {
        return pingStatus;
    }

    public Status.DeviceWorkingStatus getWorkingStatus() {
        return workingStatus;
    }

    public void setWorkingStatus(Status.DeviceWorkingStatus workingStatus) {
        this.workingStatus = workingStatus;
    }

    public Status.BoardType getType() {
        return type;
    }

    public CellInfo getCellInfo() {
        return cellInfo;
    }

    public void setCellInfo(CellInfo cellInfo) {
        this.cellInfo = cellInfo;
        if(cellInfo == null) {
            this.isReadyToMonitor = false;
        } else {
            this.isReadyToMonitor = true;
        }
    }

    public MonitorDevice(String name, String IP, Status.BoardType type) {
        super(name, IP, DATA_PORT, MESSAGE_PORT);
        this.isReadyToMonitor = false;
        this.type = type;
        this.workingStatus = Status.DeviceWorkingStatus.ABNORMAL;
        this.pingStatus = Status.PingResult.FAILED;
        if(!pingStart) {
            pingStart = true;
            Global.ThreadPool.cachedThreadPool.execute(new NetPing());
        }
    }

    public boolean isReady() {
        if (pingStatus == Status.PingResult.FAILED) {
            return false;
        }
        if (status == Status.DeviceStatus.DISCONNECTED || status == Status.DeviceStatus.DISCONNECTING) {
            return false;
        }
        if (status == Status.DeviceStatus.WORKING) {
            Log.d(TAG, "------------------------ Restart ---------------------");
            send(GetFrequentlyUsedMsg.protocalTraceRelMsg);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
        }
        return status == Status.DeviceStatus.IDLE;
    }

    public void startMonitor(Status.Service service) {
        if (!isReady()) {
            return;
        }
        if (!validateCellInfo()) {
            return;
        }
        switch (service) {
            case FINDSTMIS:
                send(GenProtocolTraceMsg.gen((byte) 2, cellInfo.earfcn, cellInfo.pci, new byte[]{}));
                break;
            case ORIENTATION:
                byte[] stmsi = OrientationFinding.getInstance().targetStmsi.getBytes();
                if (!validateSTMSI(stmsi)) {
                    return;
                }
                send(GenProtocolTraceMsg.gen((byte) 0, cellInfo.earfcn, cellInfo.pci, stmsi));
                break;
            default:
                break;
        }

    }

    private boolean validateCellInfo() {
        if (cellInfo == null || cellInfo.earfcn <= 0 || cellInfo.pci <= 0) {
            Log.e(TAG, "Invalid Cell Info!");
            return false;
        }
        return true;
    }

    private boolean validateSTMSI(byte[] bytes) {
        if (bytes.length != 10) {
            Log.e(TAG, "Invalid stmsi!");
            return false;
        }
        return true;
    }

    public void stopMonitor() {
        if (status != Status.DeviceStatus.WORKING) {
            return;
        }
        send(GetFrequentlyUsedMsg.protocalTraceRelMsg);
    }
    public boolean getIsReadyToMonitor(){
        return this.isReadyToMonitor;
    }
    public void setIsReadyToMonitor(boolean isReadyToMonitor) {
        this.isReadyToMonitor = isReadyToMonitor;
    }

    private void ping() {
        //pingStatus = Status.PingResult.FAILED;
        Process p;
        try {
            p = Runtime.getRuntime().exec("ping -c 1 -w 1000 " + IP);
            int status = p.waitFor();
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line);
            }
            Log.i(TAG, "Return ============" + builder.toString());
            if (status == 0) {
                pingStatus = Status.PingResult.SUCCEED;
            } else {
                pingStatus = Status.PingResult.FAILED;
            }
            Log.i(TAG, "++++++++++++++++++ status: " + status + "pingstatus: " + pingStatus);
            //Log.d("changeDevice", "DeviceMonitor  devicename:" + this.getName() + " hashcode:" + this.hashCode());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class NetPing implements Runnable {
        @Override
        public void run() {
            while (pingStart) {
                ping();
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }



    public void release() {
        try {
            disconnect();
            pingStart = false;
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to release device[%s].", IP));
            e.printStackTrace();
        }

    }

    public void checkCellCapture() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.e(TAG,"重启设备");
                reboot();
                //cancleCheckCellCaoture();
            }
        };
        timer.schedule(timerTask,10000);
        Log.e(TAG,"开始计时");
    }
    public void cancleCheckCellCaoture() {
        if(timer != null) {
            timer.cancel();
            timer = null;
            Log.e(TAG,"计时取消");
        }
    }
}
