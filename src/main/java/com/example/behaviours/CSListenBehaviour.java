package com.example.behaviours;

import com.example.domain.ChargingPoint;
import com.example.agents.ChargingStationAgent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import java.lang.Object;
import jade.core.Agent;
import java.lang.Cloneable;


import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;



public class CSListenBehaviour extends CyclicBehaviour {

    //private final List<ChargingPoint> chargingPoints;
    private final ChargingStationAgent csAgent;


    public CSListenBehaviour(ChargingStationAgent csAgent) {
        super(csAgent);
        this.csAgent = csAgent;
        //this.chargingPoints = csAgent.getChargingPoints();
    }

    @Override
    public void action() {
        boolean was_offered = false;

        ACLMessage msg = myAgent.receive();
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
                myAgent.send(err);
                return;
            }

            System.out.println(myAgent.getLocalName() + " received request for slot " + slot);

            // Try to find a free charging point for that slot
            ChargingPoint chosen = null;
            for (ChargingPoint cp : csAgent.chargingPoints) {
                if (cp.getChargingSlots(slot) == null) {
                    chosen = cp;
                    break;
                }
            }

            ACLMessage reply = msg.createReply();
            if (chosen != null)
            {
                // Free slot found: propose a price
                int dummyPrice = 10; // your pricing logic here
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.format("%d:%d:%s", slot, dummyPrice, chosen.getCpId()));
                System.out.println(myAgent.getLocalName() +
                        " proposing slot " + slot + " at price " + dummyPrice +
                        " on CP " + chosen.getCpId());
                was_offered = true;
            }
            else
            {
                // No free slot: collect occupying AIDs
                List<AID> occupants = new ArrayList<>();
                for (ChargingPoint cp : csAgent.chargingPoints) {
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
                System.out.println(myAgent.getLocalName() +
                        " rejecting slot " + slot + "; occupied by " + sj);
            }
            myAgent.send(reply);
            if(was_offered==true)
            {
                ACLMessage confirm = myAgent.blockingReceive(
                        MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        2000
                );
                if (confirm != null) {
                    // Got confirmation—book it
                    AID evAID = confirm.getSender();
                    chosen.chargingQueue[slot] = evAID;
                    System.out.println(getAgent().getLocalName() +
                            " confirmed booking of slot " + slot +
                            " on CP " + chosen.getCpId() +
                            " for EV " + evAID.getLocalName());
                } else {
                    // No confirmation arrived in time
                    System.out.println(getAgent().getLocalName() +
                            " did not receive CONFIRM for slot " + slot +
                            "; proposal expired.");
                }
            }
        } else {
            block();
        }
    }
}