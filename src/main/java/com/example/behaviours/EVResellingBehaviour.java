package com.example.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import com.example.agents.EVAgent;

import java.util.Locale;

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


            if (offer < minPrice) {
                // Too low, reject immediately
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                reply.setContent("Offer below minimum: " + offer);
                myAgent.send(reply);
                System.out.println("Refused low offer from " + buyer.getLocalName() + ": " + offer);
            } else if (Math.abs(offer - firstBid) < 0.01) {
                // Round 2: buyer matched our counter
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent("Accepted at counter price: " + offer);
                myAgent.send(reply);
                System.out.println("Accepted counter-offer from " + buyer.getLocalName());
            } else {
                // Round 1: propose a counter-offer
                double minAfterFirstBid = minPrice + 0.25 * (offer - minPrice); // conservative bump
                evAgent.setMinAfterFirstBid(minAfterFirstBid); // store for use in next round

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.format(Locale.US, "%.2f", firstBid));
                myAgent.send(reply);
                System.out.println("Proposed counter: " + firstBid + ", will accept >= " + minAfterFirstBid);
            }
        } else {
            block();
        }
    }
}
