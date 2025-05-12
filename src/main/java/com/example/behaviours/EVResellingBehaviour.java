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
        MessageTemplate temp1 = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
        MessageTemplate temp2 = MessageTemplate.or(temp1, MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msg = myAgent.receive(MessageTemplate.or(temp2, MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)));

        if (msg != null) {


            if(msg.getPerformative() == ACLMessage.INFORM) {
                evAgent.charge(20);
                System.out.println(evAgent.getLocalName() + " charged at " + evAgent.getCurrentLocation());
                evAgent.addBehaviour(new EVRoamBehaviour(evAgent));
                evAgent.removeBehaviour(this);
            }

            else if(msg.getPerformative()==ACLMessage.PROPOSE || msg.getPerformative()==ACLMessage.CFP)
            {
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
                double minPrice = evAgent.getChargingPrice();
                double maxPrice = minPrice * 2;
                double chargeFactor = (100 - evAgent.getBatteryLevel()) / 100.0;

                System.out.printf("----------------min price: %.2f, max price: %.2f, charge factor: %.2f\n", minPrice, maxPrice, chargeFactor);

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
                } else if (Math.abs(offer - firstBid) < 1) {
                    // Round 2: buyer matched our counter
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    reply.setContent("Accepted at counter price: " + offer);
                    myAgent.send(reply);
                    System.out.println("Accepted counter-offer from " + buyer.getLocalName());
                } else {
                    // Round 1: propose a counter-offer
                    double minAfterFirstBid = offer + 0.25 * (maxPrice - offer); // conservative bump
                    evAgent.setMinAfterFirstBid(minAfterFirstBid); // store for use in next round

                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.format(Locale.US, "%.2f", firstBid));
                    myAgent.send(reply);
                    System.out.println("Proposed counter: " + firstBid + ", will accept >= " + minAfterFirstBid);
                }
            }
            else if(msg.getPerformative()==ACLMessage.ACCEPT_PROPOSAL)
            {
                System.out.printf("accept proposal received\n");
            }

        } else {
            block();
        }
    }
}
