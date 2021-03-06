package com.example.jbtang.agi.core;

/**
 * Created by jbtang on 10/28/2015.
 */
public class CellInfo {
    public static final String NULL_VALUE = "N/A";
    public Integer earfcn;
    public Short pci;
    public Short tai;
    public Integer ecgi;
    public Float sinr;
    public Float rsrp;
    public boolean isChecked;

    public CellInfo() {
        this.earfcn = Integer.MAX_VALUE;
        this.pci = Short.MAX_VALUE;
        this.tai = Short.MAX_VALUE;
        this.ecgi = Integer.MAX_VALUE;
        this.sinr = Float.NaN;
        this.rsrp = Float.NaN;
        this.isChecked = false;
    }

    public Status.BoardType toBoardType() {
        if ((0 <= earfcn && earfcn <= 599) || (1200 <= earfcn && earfcn <= 1949)) {
            return Status.BoardType.FDD;
        }
        if ((37750 <= earfcn && earfcn <= 39649) || (39650 <= earfcn && earfcn <= 41589)) {
            return Status.BoardType.TDD;
        }
        throw new IllegalArgumentException("Invalid earfcn!!!");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        final CellInfo other = (CellInfo) o;
        return earfcn.equals(other.earfcn) && pci.equals(other.pci);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + earfcn;
        result = prime * result + pci;
        return result;
    }
}
