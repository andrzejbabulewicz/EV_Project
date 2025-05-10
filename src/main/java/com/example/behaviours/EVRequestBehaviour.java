package com.example.behaviours;

import com.example.agents.EVAgent;
import com.example.domain.ChargingPoint;
import com.example.agents.ChargingStationAgent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;


import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;



public class EVRequestBehaviour extends CyclicBehaviour {

    //private final List<ChargingPoint> chargingPoints;
    private final ChargingStationAgent csAgent;


    public EVRequestBehaviour(ChargingStationAgent csAgent) {
        super(csAgent);
        this.csAgent = csAgent;
        //this.chargingPoints = csAgent.getChargingPoints();
    }

    @Override
    public void action() {
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
            if (chosen != null) {
                // Free slot found: propose a price
                double dummyPrice = 10.0; // your pricing logic here
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.format("%d:%.2f:%s", slot, dummyPrice, chosen.getCpId()));
                System.out.println(myAgent.getLocalName() +
                        " proposing slot " + slot + " at price " + dummyPrice +
                        " on CP " + chosen.getCpId());
            } else {
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
        } else {
            block();
        }
    }
}