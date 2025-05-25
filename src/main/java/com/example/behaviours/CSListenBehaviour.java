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


import java.util.*;


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
        double basePrice = csAgent.getBasePrice();
        boolean was_offered = false;


        MessageTemplate temp1 = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        ACLMessage msg = myAgent.receive(temp1);
        //ACLMessage msg = myAgent.receive();
        if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
            String content = msg.getContent().trim();
            int slot;
            try {

                slot = Integer.parseInt(content) + csAgent.realTime;
            } catch (NumberFormatException e) {
                // Malformed request: reject outright
                ACLMessage err = msg.createReply();
                err.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                err.setContent("Invalid slot number: " + content);
                myAgent.send(err);
                return;
            }

            System.out.println("[" + myAgent.getLocalName() + "]" + " received request for slot " + slot);

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
                double price = csAgent.calculateFactoredPrice(slot);
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.format(Locale.US,"%d:%.2f:%s", slot, price, chosen.getCpId()));
                System.out.printf("[%s] proposing slot %d at price $%.2f on [%s]\n",myAgent.getLocalName(), slot, price, chosen.getCpId());
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
                double price = csAgent.calculateFactoredPrice(slot);
                reply.setContent(String.format(Locale.US ,"%d:%.2f:%s", slot, price, sj.toString()));
                System.out.println("[" + myAgent.getLocalName() + "]" +
                        " rejecting slot " + slot + "; occupied by [" + sj + "]");
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
                    System.out.println("[" + getAgent().getLocalName() + "]" +
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

        }
        else if(msg!=null && msg.getPerformative() == ACLMessage.INFORM && Objects.equals(msg.getConversationId(), "csInform"))
        {
            //System.out.printf("%s: received confirmation\n", myAgent.getLocalName());
            ACLMessage sth = msg.createReply();
            String[] parts = msg.getContent().split(":");
            int slot_no = Integer.parseInt(parts[0]);
            String name = parts[1];
            String cpName = parts[2].trim();

            AID senderAID = msg.getSender();

           //for(ChargingPoint cp : csAgent.chargingPoints)
            for(int i=0;i<csAgent.chargingPoints.size();i++)
            {
                //System.out.printf("%s: is in the loop, charging point: %s, cpname received: %s\n", myAgent.getLocalName(), csAgent.chargingPoints.get(i).getCpId(), cpName);
                if(csAgent.chargingPoints.get(i).getCpId().equals(cpName))
                {
                    //System.out.printf("%s: ????????????????\n", myAgent.getLocalName());
                    AID Aid = new AID(name, AID.ISLOCALNAME);

                    csAgent.chargingPoints.get(i).chargingQueue[slot_no] = Aid;

                    ACLMessage msgConfirm = msg.createReply();
//                    msgConfirm.setSender(senderAID);
//                    msgConfirm.setSender(Aid);

                    msgConfirm.setPerformative(ACLMessage.CONFIRM);
                    msgConfirm.setContent("confirmed");
                    myAgent.send(msgConfirm);
                }
            }

        }
        else
        {
            block();
        }
    }
}