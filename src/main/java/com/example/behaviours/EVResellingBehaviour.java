package com.example.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import com.example.agents.EVAgent;

public class EVResellingBehaviour extends CyclicBehaviour {
    private final EVAgent evAgent;

    public EVResellingBehaviour(EVAgent evAgent) {
        super(evAgent);
        this.evAgent = evAgent;
    }

    @Override
    public void action() {
        // Listen only for CFP messages (negotiation start)
        ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
        if (msg != null) {
            AID buyer = msg.getSender();
            String content = msg.getContent().trim();
            double offer;

            try {
                offer = Double.parseDouble(content);
            } catch (NumberFormatException e) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                reply.setContent("Invalid offer format");
                myAgent.send(reply);
                return;
            }

            double minPrice = evAgent.getSlotPricePaid();
            double maxPrice = minPrice * 2;
            double chargeFactor = (100 - evAgent.getBatteryLevel()) / 100.0;
            double firstBid = minPrice + (maxPrice - minPrice) * chargeFactor;

            System.out.printf("[%s] Received CFP from %s: %.2f, calculated first bid: %.2f\n",
                    myAgent.getLocalName(), buyer.getLocalName(), offer, firstBid);

            ACLMessage reply = msg.createReply();

            if (offer >= minPrice) {
                // Round 2: buyer already made an acceptable offer, accept immediately
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent("Deal accepted for price: " + offer);
                myAgent.send(reply);
                System.out.println("Accepted offer from " + buyer.getLocalName());
                // You could now release the slot etc.
            } else {
                // Round 1: reply with a counter-proposal
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.valueOf(firstBid));
                myAgent.send(reply);
                System.out.println("Proposed counter-offer: " + firstBid);
            }
        } else {
            block();
        }
    }
}
