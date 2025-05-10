package com.example.domain;

import java.util.ArrayList;
import java.util.List;

//import com.example.domain.chargerTypes;

public class ChargingPoint {
    private String cpId;
    private boolean isOccupied;
    private chargerTypes type;
    private List<ScheduledCharging> chargingQueue = new ArrayList<>();

    public ChargingPoint(String cpId, boolean isOccupied) {
        this.cpId = cpId;
        this.isOccupied = isOccupied;
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

    public List<ScheduledCharging> getChargingQueue() {
        return new ArrayList<>(chargingQueue);
    }

    public void addScheduledCharging(String evId, long startTime, long endTime) {
        ScheduledCharging session = new ScheduledCharging(evId, startTime, endTime);
        chargingQueue.add(session);
        System.out.println(cpId + " scheduled session: " + session);
    }

    public void removeScheduledCharging(String evId) {
        chargingQueue.removeIf(session -> session.getEvId().equals(evId));
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
