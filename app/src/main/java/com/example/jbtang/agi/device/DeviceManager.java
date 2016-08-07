package com.example.jbtang.agi.device;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manage all the active devices
 * Created by jbtang on 10/13/2015.
 */
public class DeviceManager {
    private List<MonitorDevice> conDevices;
    private List<MonitorDevice> allDevices;
    private static final DeviceManager instance = new DeviceManager();

    private DeviceManager() {
        allDevices = new ArrayList<>();
        conDevices = new ArrayList<>();
    }

    public static DeviceManager getInstance() {
        return instance;
    }

    public List<MonitorDevice> getDevices() {
        return conDevices;
    }

    public MonitorDevice getDevice(String name) {
        for (MonitorDevice device : conDevices) {
            if (device.getName().equals(name)) {
                return device;
            }
        }
        return null;
    }

    public void add(MonitorDevice device) {
        if (DeviceManager.getInstance().getDevice(device.getName()) == null) {
            conDevices.add(device);
        }
    }

    public void remove(String name) {
        for (MonitorDevice device : conDevices) {
            if (device.getName().equals(name)) {
                conDevices.remove(device);
                return;
            }
        }
    }

    public List<MonitorDevice> getAllDevices() {
        return allDevices;
    }

    public MonitorDevice getFromAll(String name) {
        for (MonitorDevice device : allDevices) {
            if (device.getName().equals(name))
                return device;
        }
        return null;
    }

    public void addToAll(MonitorDevice device) {
        for (int i = 0; i < allDevices.size(); i++) {
            if (allDevices.get(i).getName().equals(device.getName())) {
                allDevices.set(i, device);
                Log.e("Device","replace");
                return;
            }
        }
        Log.e("Device","not replace");
        allDevices.add(device);
    }

    public void removeFromAll(String name) {
        for (MonitorDevice device : allDevices) {
            if (device.getName().equals(name)) {
                allDevices.remove(device);
                return;
            }
        }
    }
}
