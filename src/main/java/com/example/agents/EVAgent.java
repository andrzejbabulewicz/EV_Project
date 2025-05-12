package com.example.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import lombok.Getter;
import lombok.Setter;
import com.example.behaviours.EVGetStationsBehaviour;
import com.example.behaviours.EVRoamBehaviour;
import com.example.domain.chargerTypes;
import com.example.domain.Map.*;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

import java.util.*;

import static java.lang.Thread.sleep;

//import static jdk.jfr.internal.EventWriterKey.block;

@Getter
public class EVAgent extends Agent {


    private chargerTypes type;

    // EV specification
    private double batteryPerKm;
    private double batteryLevel;
    private double maxBatteryLevel;

    @Setter public double minAfterFirstBid;

    // Total money
    private double totalMoney;

    // Parameters when buying
    private double chargingUrgency; // 0.0 - 1.0 predefined

    @Setter private double sumOfPrices = 0;
    @Setter private double pricesCount = 0;
    @Setter private double meanPrice = 0;

    private Station currentLocation;

    @Setter private Station currentCommunication;
    @Setter private AID currentCommunicationAid;
    
    private java.util.Map<Station, Integer> sortedStations = new HashMap<>();
    @Setter private int stationIndex = 0;

    // CP info when in queue
    @Setter private int slot;
    @Getter private int slotPricePaid;
    @Setter private String cpId;
    @Setter private double chargingPrice;

    @Setter private List<AID> evInQueue = new ArrayList<>();

    private List<AID> stations = new ArrayList<>();
    public List<AID> getStations() {
        return stations;
    }
    @Override
    protected void setup() {

        try
        {
            System.out.println(getLocalName() + ": you have 20s for setting up sniffer");
            sleep(20000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        System.out.printf("[%s] I'm ready to charge my battery!%n", this.getLocalName());

        final Object[] args = getArguments();
        type = (chargerTypes) args[0];
        batteryPerKm = (double) args[1];
        batteryLevel = (double) args[2];
        maxBatteryLevel = (double) args[3];
        currentLocation = (Station) args[4];
        totalMoney = (double) args[5];
        chargingUrgency = (double) args[6];

        System.out.println(getLocalName() + " started at location: " + currentLocation);

        addBehaviour(new EVGetStationsBehaviour(this));
        System.out.printf("[%s] begins roaming.%n", this.getLocalName());
    }

    protected void takeDown() {
        System.out.println(getLocalName() + " terminating.");
    }

    public void travel(Road road) {
        // Traveling between ChargingStations
        // Dijkstra Algorithm
        batteryLevel -= batteryPerKm * road.distance();
        currentLocation = road.from().name() == currentLocation.name() ? road.to() : road.from();

        System.out.println(getLocalName() + " traveling to " + currentLocation.name());
        try {
            sleep(road.distance() * 500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(getLocalName() + " arrived at " + currentLocation.name());
    }

    public void travelToCp(Station station) {
        // After getting a spot, travel to it
        int distance = sortedStations.get(station);
        batteryLevel -= batteryPerKm * distance;
        currentLocation = getStationAtIndex(stationIndex);
        sortedStations.clear();

        System.out.println(getLocalName() + " traveling to " + currentLocation.name() + " to charge");
        try {
            sleep(distance * 500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(getLocalName() + " arrived at " + currentLocation.name() + " to charge");

    }

    public void charge(int time) {
        // Charge and stop listening for negotiations
        // After that start roaming
        double dummyPower = 2; // Charging per minute
        batteryLevel += time * dummyPower;
        if (batteryLevel > maxBatteryLevel)
            batteryLevel = maxBatteryLevel;
        try {
            sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sortStations(Station currentStation) {
        // Distance map initialized with infinity, except the current station (0)
        java.util.Map<Station, Integer> distances = new HashMap<>();
        for (Station station : com.example.domain.Map.getStations()) {
            distances.put(station, Integer.MAX_VALUE);
        }
        distances.put(currentStation, 0);

        // Priority queue to select the station with the smallest tentative distance
        PriorityQueue<Map.Entry<Station, Integer>> pq = new PriorityQueue<>(
                java.util.Map.Entry.comparingByValue()
        );
        pq.offer(java.util.Map.entry(currentStation, 0));

        while (!pq.isEmpty()) {
            java.util.Map.Entry<Station, Integer> currentEntry = pq.poll();
            Station current = currentEntry.getKey();
            int currentDistance = currentEntry.getValue();

            // Check neighbors
            for (Road road : com.example.domain.Map.getRoads()) {
                if (road.from().equals(current)) {
                    Station neighbor = road.to();
                    int newDist = currentDistance + road.distance();
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        pq.offer(java.util.Map.entry(neighbor, newDist));
                    }
                }
            }
        }

        // Sort the distances by value (distance)
        sortedStations =  distances.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByValue())
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    public Station getStationAtIndex(int index) {
        List<Station> keys = new ArrayList<>(sortedStations.keySet());
        if (index < 0 || index >= keys.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + keys.size());
        }
        return keys.get(index);
    }

    public boolean askNextStation() {
        // change current station to next closest
        if(stationIndex < sortedStations.size() - 1) {
            currentCommunication = getStationAtIndex(++stationIndex);
            currentCommunicationAid = new AID(currentCommunication.name(), AID.ISLOCALNAME);
            return true;
        }
        else {
            stationIndex = 0;
            currentCommunication = getStationAtIndex(stationIndex);
            currentCommunicationAid = new AID(currentCommunication.name(), AID.ISLOCALNAME);
            return false;
        }
    }

    public void sumNextPrice(double price) {
        sumOfPrices += price;
        pricesCount++;
    }

    public void calculateMeanPrice() {
        if (pricesCount == 0)
            return;
        meanPrice = sumOfPrices / pricesCount;
        sumOfPrices = 0;
        pricesCount = 0;
    }

    public double getBatteryRatio() {
        return batteryLevel / maxBatteryLevel;
    }
}
