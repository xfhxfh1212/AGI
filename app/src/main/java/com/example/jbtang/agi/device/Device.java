package com.example.jbtang.agi.device;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.MsgSendHelper;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.core.TCPClient;
import com.example.jbtang.agi.messages.GetFrequentlyUsedMsg;
import com.example.jbtang.agi.messages.MessageDispatcher;
import com.example.jbtang.agi.messages.base.MsgHeader;
import com.example.jbtang.agi.messages.base.MsgTypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Date;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

/**
 * Base device class
 * Created by jbtang on 9/29/2015.
 */
public class Device {
    private String name;
    private TCPClient client;
    protected String IP;
    private int messagePort;
    private int dataPort;
    protected Status.DeviceStatus status;
    private InputStream ackIn;
    private InputStream dataIn;
    private Date receiveTime;
    private Date currentTime;
    protected boolean checkStatusStart;
    private Handler myHandler;

    private static final int Data_RECEIVE_BUFFER_SIZE = 10240;
    private static final int Message_RECEIVE_BUFFER_SIZE = 256;
    protected static final String TAG = "Device";

    private static final String REBOOT_USERNAME = "root";
    private static final String REBOOT_PASSWORD = "13M1877";
    private static final int REBOOT_PORT = 22;
    private static final String REBOOT_CMD = "reboot";

    public boolean isStartAgain() {
        return startAgain;
    }

    public void setStartAgain(boolean startAgain) {
        this.startAgain = startAgain;
    }

    private boolean startAgain;
    //private int index;

//    public int getIndex() {
//        return index;
//    }
//    public void setIndex(int index) {
//        this.index = index;
//    }

    public String getName() {
        return name;
    }

    public int getMessagePort() {
        return messagePort;
    }

    public int getDataPort() {
        return dataPort;
    }

    public String getIP() {
        return IP;
    }

    public boolean isConnected() {
        return (status == Status.DeviceStatus.IDLE || status == Status.DeviceStatus.WORKING);
    }

    public boolean isWorking() {
        return status == Status.DeviceStatus.WORKING;
    }

    public Status.DeviceStatus getStatus() {
        return status;
    }

    public Device(String name, String IP, int dataPort, int messagePort) {
        this.name = name;
        this.IP = IP;
        this.dataPort = dataPort;
        this.messagePort = messagePort;
        this.status = Status.DeviceStatus.DISCONNECTED;
        this.checkStatusStart = false;
        this.startAgain = false;
        //this.index = 0;
    }

    public void connect() throws Exception {
        if (status != Status.DeviceStatus.DISCONNECTED) {
            return;
        }
        this.myHandler = new myHandler(Device.this);
        if (client == null) {
            client = new TCPClient(IP, dataPort, messagePort, myHandler);
            Global.ThreadPool.cachedThreadPool.execute(client);
            Thread.sleep(200);
            messageReceive();
            dataReceive();
        }
        if (status == Status.DeviceStatus.DISCONNECTED) {
            send(GetFrequentlyUsedMsg.getDeviceStateMsg);
        }
        Thread.sleep(100);
        if (status != Status.DeviceStatus.DISCONNECTED) {
            this.currentTime = new Date();
            this.receiveTime = new Date();
            if (!checkStatusStart) {
                checkStatusStart = true;
                Global.ThreadPool.cachedThreadPool.execute(new CheckStatusRunnable());
            }
        }
    }

    public void disconnect() throws Exception {
        checkStatusStart = false;
        if (client == null || status == Status.DeviceStatus.DISCONNECTED) {
            return;
        }
        if (status == Status.DeviceStatus.WORKING) {
            send(GetFrequentlyUsedMsg.protocalTraceRelMsg);
            Thread.sleep(200);
        }
        dispose();
    }

    private class CheckStatusRunnable implements Runnable {
        @Override
        public void run() {
            while (checkStatusStart) {
                currentTime = new Date();
                long time = (currentTime.getTime() - receiveTime.getTime()) / 1000;
                Log.e(TAG, "time:" + String.valueOf(time));
                if (time > 7) {
                    status = Status.DeviceStatus.DISCONNECTING;
                    if (time > 13) {
                        dispose();
                        return;
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void send(byte[] bytes) {
        if (client != null) {
            Message msg = new Message();
            msg.what = Status.HandlerMsgStatus.IN_TO_TCPCLIENT.ordinal();
            msg.obj = bytes;
            client.send(msg);
        }
    }

    static class myHandler extends Handler {
        private final WeakReference<Device> mOuter;

        public myHandler(Device device) {
            mOuter = new WeakReference<>(device);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Status.HandlerMsgStatus.TCPCLIENT_FAILED.ordinal()) {
                Log.e(TAG, "TCPClient exception.");
                mOuter.get().dispose();
            }
        }
    }

    private void messageReceive() throws IOException {
        ackIn = client.getMessageStream();
        if (ackIn == null) {
            return;
        }
        Global.ThreadPool.cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[Message_RECEIVE_BUFFER_SIZE];
                while (true) {
                    try {
                        if (ackIn.read(buffer, 0, MsgHeader.byteArrayLen) == -1) {
                            continue;
                        }
                        MsgHeader header = new MsgHeader(MsgSendHelper.getSubByteArray(buffer, 0, MsgHeader.byteArrayLen));
                        if (header.getMsgType() == 0xffff) {
                            Log.d(TAG, "strange ACK!");
                            continue;
                        }
                        changeStatus(header.getMsgType());
                        int bodyCount = header.getMsgLen() * 4;
                        if (ackIn.read(buffer, 12, bodyCount) == -1) {
                            continue;
                        }
                        int messageLen = bodyCount + 12;
                        byte[] dataBuffer = MsgSendHelper.getSubByteArray(buffer, 0, messageLen);
                        Log.d(TAG, "Status : -----------" + status.name());
                        Log.d(TAG, String.format("Device name: %s Message type: %x length: %d", name, header.getMsgType(), messageLen));
                        Log.d(TAG, "Receive form message port : " + convertByteToString(dataBuffer));
                        receiveTime = new Date();
                    } catch (Exception e) {
                        Log.d(TAG, "Message receive exception.", e);
                        if (ackIn == null) {
                            return;
                        }
                    }
                }
            }
        });
    }

    private void dataReceive() throws IOException {
        dataIn = client.getDataStream();
        if (dataIn == null) {
            return;
        }
        Global.ThreadPool.cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[Data_RECEIVE_BUFFER_SIZE];
                while (true) {
                    try {
                        if (dataIn.read(buffer, 0, MsgHeader.byteArrayLen) == -1) {
                            continue;
                        }
                        MsgHeader header = new MsgHeader(MsgSendHelper.getSubByteArray(buffer, 0, MsgHeader.byteArrayLen));
                        changeStatus(header.getMsgType());
                        int bodyCount = header.getMsgLen() * 4;
                        if (dataIn.read(buffer, 12, bodyCount) == -1) {
                            continue;
                        }
                        int messageLen = bodyCount + 12;
                        byte[] dataBuffer = MsgSendHelper.getSubByteArray(buffer, 0, messageLen);
                        Log.d(TAG, "Status : -----------" + status.name());
                        Log.d(TAG, String.format("Device: %s Message type: %x Length: %d", name, header.getMsgType(), messageLen));
                        Log.d(TAG, "Receive form data port : " + convertByteToString(dataBuffer));
                        receiveTime = new Date();
                        if (header.getMsgType() != 0x8002) {
                            MessageDispatcher.getInstance().Dispatch(new Global.GlobalMsg(name, header.getMsgType(), dataBuffer));
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Data receive exception.", e);
                        if (dataIn == null) {
                            return;
                        }
                    }
                }
            }
        });
    }


    private void changeStatus(int msgType) {
        switch (status) {
            case DISCONNECTING:
            case DISCONNECTED:
                status = Status.DeviceStatus.IDLE;
                break;
            case IDLE:
                if (msgType == MsgTypes.AG_PC_PROTOCOL_TRACE_REQ_ACK_MSG_TYPE ||
                        msgType == MsgTypes.L2P_AG_UE_CAPTURE_IND_MSG_TYPE ||
                        msgType == MsgTypes.L1_AG_PHY_COMMEAS_IND_MSG_TYPE) {
                    status = Status.DeviceStatus.WORKING;
                }
                break;
            case WORKING:
                if (msgType == MsgTypes.AG_PC_PROTOCOL_TRACE_REL_ACK_MSG_TYPE ||
                        msgType == MsgTypes.L1_AG_PROTOCOL_TRACE_REL_ACK_MSG_TYPE) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {

                    }
                    status = Status.DeviceStatus.IDLE;
                }
                break;
            default:
                break;
        }
    }



    public void dispose() {
        //closeReceiveThread();
        closeACKInputStream();
        closeDataInputStream();
        closeTCPClient();
        this.status = Status.DeviceStatus.DISCONNECTED;
        this.startAgain = false;
        this.checkStatusStart = false;
    }

    /*private void closeReceiveThread() {
        if (messageRevThread.isAlive()) {
            messageRevThread.interrupt();
        }
        messageRevThread = null;
        if (dataRevThread.isAlive()) {
            dataRevThread.interrupt();
        }
        dataRevThread = null;
    }*/

    private void closeACKInputStream() {
        if (ackIn != null) {
            try {
                ackIn.close();
                ackIn = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeDataInputStream() {
        if (dataIn != null) {
            try {
                dataIn.close();
                dataIn = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeTCPClient() {
        if (client != null) {
            client.dispose();
            client = null;
        }
    }

    private String convertByteToString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.valueOf(b)).append(", ");
        }
        return builder.toString();
    }

}
