package com.example.jbtang.agi.device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manage all the active devices
 * Created by jbtang on 10/13/2015.
 */
public class DeviceManager {
    private Map<String, MonitorDevice> conDevices;
    private List<MonitorDevice> allDevices;
    private static final DeviceManager instance = new DeviceManager();

    private DeviceManager() {
        allDevices = new ArrayList<>();
        conDevices = new TreeMap<>();
    }

    public static DeviceManager getInstance() {
        return instance;
    }

    public List<MonitorDevice> getDevices() {
        return new ArrayList<>(conDevices.values());
    }

    public MonitorDevice getDevice(String name) {
        return conDevices.get(name);
    }

    public void add(MonitorDevice device) {
        conDevices.put(device.getName(), device);
    }

    public void remove(String name) {
        conDevices.remove(name);
    }

    public List<MonitorDevice> getAllDevices() {
        return allDevices;
    }

    public MonitorDevice getFromAll(String name) {
        for(MonitorDevice device : allDevices) {
            if(device.getName().equals(name))
                return device;
        }
        return null;
    }

    public void addToAll(MonitorDevice device) {
        allDevices.add(device);
    }

    public void removeFromAll(String name) {
        for(MonitorDevice device : allDevices){
            if(device.getName().equals(name)) {
                allDevices.remove(device);
                return;
            }
        }
    }
}
