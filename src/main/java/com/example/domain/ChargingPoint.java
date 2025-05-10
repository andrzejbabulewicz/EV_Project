package com.example.domain;

import jade.core.AID;

import java.util.ArrayList;
import java.util.List;

//import com.example.domain.chargerTypes;

public class ChargingPoint {
    private String cpId;
    private boolean isOccupied;

    private chargerTypes type;
    //private List<ScheduledCharging> chargingQueue = new ArrayList<>();
    public static final int NUM_HOURS = 2;
    public static final int SLOT_DURATION = 20;

    public int getSlotNo() {
        int tmp = NUM_HOURS*60;
        int slotsNo = tmp/SLOT_DURATION;

        return slotsNo;
    }

    public AID[] chargingQueue = new AID[getSlotNo()];

    public ChargingPoint(String cpId, boolean isOccupied) {
        this.cpId = cpId;
        this.isOccupied = isOccupied;
    }

    public AID getChargingSlots(int slot)
    {
        return chargingQueue[slot];
    }
    public String getCpId() {
        return cpId;
    }

    public void setCpId(String cpId) {
        this.cpId = cpId;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

//    public AID<ScheduledCharging> getChargingQueue() {
//        return new ArrayList<>(chargingQueue);
//    }

    public void addScheduledCharging(AID evId, int slot)
    {
        chargingQueue[slot] = evId;
        System.out.println(cpId + " scheduled session at slot: " + slot);
    }


    public void removeScheduledCharging(String evId, int slot)
    {
        chargingQueue[slot] = null;
        System.out.println(cpId + " removed scheduled session for EV: " + evId);
    }


    public static class ScheduledCharging {
        private String evId;
        private long startTime;
        private long endTime;

        public ScheduledCharging(String evId, long startTime, long endTime) {
            this.evId = evId;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getEvId() {
            return evId;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            return "ScheduledCharging{" +
                    "evId='" + evId + '\'' +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    '}';
        }
    }
}
