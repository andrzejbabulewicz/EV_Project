package com.example.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import lombok.Getter;
import lombok.Setter;
import com.example.behaviours.EVGetStationsBehaviour;
import com.example.behaviours.EVRoamBehaviour;
import com.example.domain.chargerTypes;
import com.example.domain.Map.Station;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import java.util.List;
import java.util.Random;

import java.util.ArrayList;

import static jdk.jfr.internal.EventWriterKey.block;

@Getter
public class EVAgent extends Agent {


    private chargerTypes type;
    private double batteryLevel;
    private double maxBatteryLevel;

    // Total money
    private double totalMoney;
    private double currentMaxBid;

    // Behaviour states
    @Setter private boolean isRoaming = true;
    @Setter private boolean wantsToCharge = false;
    @Setter private boolean isWaiting = false;
    @Setter private boolean isCharging = false;

    // Communication states
    @Setter private boolean isNegotiating = false;
    @Setter private boolean isAskingCS = false;
    @Setter private boolean isAskingEV = false;
    @Setter private boolean isAnsweringEV = false;

    private Station currentLocation;
    @Setter private Station currentCommunication;
    @Setter private AID currentCommunicationAid;
    private List<Station> refusedStations = new ArrayList<>();


    private List<AID> stations = new ArrayList<>();
    public List<AID> getStations() {
        return stations;
    }
    @Override
    protected void setup() {

        try
        {
            System.out.println(getLocalName() + ": you have 20s for setting up sniffer");
            Thread.sleep(20000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        System.out.printf("[%s] I'm ready to charge my battery!%n", this.getLocalName());

        final Object[] args = getArguments();
        type = (chargerTypes) args[0];
        batteryLevel = (double) args[1];
        maxBatteryLevel = (double) args[2];
        currentLocation = (Station) args[3];

        System.out.println(getLocalName() + " started at location: " + currentLocation);

        addBehaviour(new EVGetStationsBehaviour(this));
        addBehaviour(new EVSendRequestBehaviour());
        addBehaviour(new EVRoamBehaviour(this));
        System.out.printf("[%s] begins roaming.%n", this.getLocalName());
    }

    protected void takeDown() {
        System.out.println(getLocalName() + " terminating.");
    }

    public void travel(Station from, Station to) {
        // Traveling between ChargingStations
        // Dijkstra Algorithm
    }

    public void charge(int time) {
        // Charge and stop listening for negotiations
        // After that start roaming
    }

    public void lookForOtherStation() {
        // If the current station is full and no EV agrees to sell their spot
        // Search through stations that were not asked before
    }



    private class EVSendRequestBehaviour extends CyclicBehaviour {
        private int step = 0;
        private ACLMessage replyMsg;
        private Random random = new Random();

        @Override
        public void action() {
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
                    // Check if there is any available station.
                    if (stations == null || stations.isEmpty()) {
                        System.out.println(getLocalName() + ": No available stations found, waiting...");
                        block(); // wait 1s and then try again

                    }
                    // Pick a random station.
                    else {
                        int index = random.nextInt(stations.size());
                        AID target = stations.get(index);

                        // Create and send a dummy REQUEST.
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        request.addReceiver(target);
                        request.setContent("Dummy request from " + getLocalName());
                        send(request);
                        System.out.println(getLocalName() + " sent request to " + target.getLocalName());
                        step = 1;



                        // Wait for a reply.
                        replyMsg = receive();
                        if (replyMsg != null) {
                            System.out.println(getLocalName() + " received reply: " + replyMsg.getContent());
                            step = 2;
                        } else {
                            block(100); // not yet received, check again after 100ms
                        }
                    }



                    // Wait 1 second before repeating the sequence.
                    block(1000);
                    step = 0;

            }
        }

}
