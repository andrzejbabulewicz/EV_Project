package com.example.agents;

import com.example.behaviours.EVResellingBehaviour;
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

    // Parameter for negotiation
    private double chargingUrgency; // 0.0 - 1.0 predefined

    // For initial bid
    @Setter private double sumOfPrices = 0;
    @Setter private double pricesCount = 0;
    @Setter private double meanPrice = 0;

    // Physical location
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

    @Getter
    private List<AID> stations = new ArrayList<>();

    @Setter int slotToRequest = 1;
    @Setter boolean tooLateForNegotiation = false;

    @Setter public int noOfTrials = 0;
    @Setter public int noOfDirectPurchases = 0;
    @Setter public int noOfPostponedPurchases = 0;
    @Setter public int noOfNegotiations = 0;
    @Setter public int noOfNegotiationsFailed = 0;
    @Setter public int noOfNegotiationsSucceeded = 0;
    @Setter public boolean didIncreaseSlot = false;
    @Setter public boolean didWriteToFile = false;

    @Setter public double totalFrustration = 0;

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

    protected void takeDown()
    {
        setDidWriteToFile(true);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("simulation_results_EV.csv", true))) {

            // Write header line with parameters
            //writer.write("EV_no,total_trials,negotiations,direct_purchases,negot_purchases,failed_purchases\n");
            writer.write(String.format(Locale.US,"%s,%d,%d,%d,%d,%d,%.2f\n",getLocalName(),
                    getNoOfTrials(),getNoOfNegotiations(),getNoOfDirectPurchases(),
                    getNoOfNegotiationsSucceeded(),getNoOfPostponedPurchases(),totalFrustration));

        } catch (IOException e) {
            System.err.println("Failed to write header to results file: " + e.getMessage());
        }
        System.out.println(getLocalName() + " terminating.");
    }

    public void increaseFrustration()
    {
        totalFrustration += chargingUrgency * slotToRequest;
    }

    public void travel(Road road) {
        // Traveling between ChargingStations
        // Dijkstra Algorithm
        batteryLevel -= batteryPerKm * road.distance();
        currentLocation = road.from().name() == currentLocation.name() ? road.to() : road.from();

        System.out.println("[" + getLocalName() + "]" + " traveling to " + currentLocation.name());
        try {
            sleep(road.distance() * 500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[" + getLocalName() + "]" + " arrived at " + currentLocation.name());
    }

    public void travelToCp(Station station) {
        // After getting a spot, travel to it
        long distance = sortedStations.get(station);
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

        sortStations(currentLocation);
    }

    public void charge(int time) {
        // Charge and stop listening for negotiations
        // After that start roaming
        double dummyPower = 2; // Charging per minute
        batteryLevel += time * dummyPower;
        if (batteryLevel > maxBatteryLevel)
            batteryLevel = maxBatteryLevel;
        try {
            sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        increaseFrustration();

        tooLateForNegotiation = false;
        slotToRequest=1;
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

        // Sort the distances by distance
        sortedStations = distances.entrySet().stream()
                .filter(entry -> entry.getValue() <= batteryLevel * batteryPerKm)
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
        this.meanPrice = sumOfPrices / pricesCount;
        sumOfPrices = 0;
        pricesCount = 0;
    }

    public double getBatteryRatio() {
        return batteryLevel / maxBatteryLevel;
    }
}
