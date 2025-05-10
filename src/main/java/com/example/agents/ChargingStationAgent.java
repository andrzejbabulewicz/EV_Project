package com.example.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.example.domain.ChargingPoint;

public class ChargingStationAgent extends Agent {
    private AID stationId;
    private String location;
    private List<ChargingPoint> chargingPoints;



    protected void setup() {
        stationId = getAID();

        try
        {
            System.out.println(getLocalName() + ": you have 20s for setting up sniffer");
            Thread.sleep(20000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        // Expected parameters: [location (String), chargingPoints (List<ChargingPoint>)]
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            //location = (String) args[0];
            chargingPoints = (List<ChargingPoint>) args[1];
        } else {
            location = "Undefined";
            chargingPoints = null;
        }
        System.out.println(getLocalName() + " started at location: " + location);
        if (chargingPoints != null) {
            System.out.println(getLocalName() + " initialized with " + chargingPoints.size() + " charging point(s).");
        } else {
            System.out.println(getLocalName() + " has no charging points set.");
        }
        registerInDF();

        addBehaviour(new EVRequestBehaviour());
    }
    private void registerInDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ChargingStation");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " successfully registered in DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }


    protected void takeDown() {
        System.out.println(getLocalName() + " terminating.");
    }

    protected void bookSlot()
    {
        /*
         * booking a given spot
         */
    }

    protected void removeBooking()
    {
        /*
         * removing a given booking without returning the money
         */
    }

    protected void processPayment()
    {
        /*
         * payment logic
         */
    }


    private class EVRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                String content = msg.getContent().trim();
                int slot;
                try {
                    slot = Integer.parseInt(content);
                } catch (NumberFormatException e) {
                    // Malformed request: reject outright
                    ACLMessage err = msg.createReply();
                    err.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    err.setContent("Invalid slot number: " + content);
                    send(err);
                    return;
                }

                System.out.println(getLocalName() + " received request for slot " + slot);

                // Try to find a free charging point for that slot
                ChargingPoint chosen = null;
                for (ChargingPoint cp : chargingPoints) {
                    if (cp.getChargingSlots(slot) == null) {
                        chosen = cp;
                        break;
                    }
                }

                ACLMessage reply = msg.createReply();
                if (chosen != null) {
                    // Free slot found: propose a price
                    double dummyPrice = 10.0; // your pricing logic here
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.format("%d:%.2f:%s", slot, dummyPrice, chosen.getCpId()));
                    System.out.println(getLocalName() +
                            " proposing slot " + slot + " at price " + dummyPrice +
                            " on CP " + chosen.getCpId());
                } else {
                    // No free slot: collect occupying AIDs
                    List<AID> occupants = new ArrayList<>();
                    for (ChargingPoint cp : chargingPoints) {
                        AID ev = cp.getChargingSlots(slot);
                        if (ev != null && !occupants.contains(ev)) {
                            occupants.add(ev);
                        }
                    }
                    reply.setPerformative(ACLMessage.REFUSE);
                    // join occupant AIDs by comma
                    StringJoiner sj = new StringJoiner(",");
                    for (AID evAid : occupants) {
                        sj.add(evAid.getLocalName());
                    }
                    reply.setContent(String.format("%d:%s", slot, sj.toString()));
                    System.out.println(getLocalName() +
                            " rejecting slot " + slot + "; occupied by " + sj);
                }
                send(reply);
            } else {
                block();
            }
        }
    }

}
