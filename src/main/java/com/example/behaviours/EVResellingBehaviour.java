package com.example.behaviours;

import com.example.agents.ChargingStationAgent;
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
        String conversationId="propose_from_buyer";
//        MessageTemplate mt = MessageTemplate.and(
//                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
//                MessageTemplate.MatchConversationId(conversationId)
//        );
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

            else if((msg.getPerformative()==ACLMessage.PROPOSE && msg.getConversationId().equals(conversationId))|| msg.getPerformative()==ACLMessage.CFP)
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

                System.out.printf("[%s]----------------min price: %.2f, max price: %.2f, charge factor: %.2f\n",evAgent.getLocalName(), minPrice, maxPrice, chargeFactor);

                double firstBid = offer + (maxPrice - offer) * chargeFactor;

                System.out.printf("[%s] Received CFP from %s: %.2f, calculated first bid: %.2f\n",
                        myAgent.getLocalName(), buyer.getLocalName(), offer, firstBid);

                ACLMessage reply = msg.createReply();


                if (offer < minPrice)
                {
                    // Too low, reject immediately
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    reply.setContent("Offer below minimum: " + offer);
                    myAgent.send(reply);
                    System.out.println("Refused low offer from " + buyer.getLocalName() + ": " + offer);
                }
                else
                {
                    ACLMessage testmsg = msg.createReply();

                    double minAfterFirstBid = offer + 0.4 * Math.abs(maxPrice - offer); // conservative bump
                    evAgent.setMinAfterFirstBid(minAfterFirstBid); // store for use in next round

                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setConversationId("propose_from_seller");
                    reply.setContent(String.format(Locale.US, "%.2f", minAfterFirstBid));
                    myAgent.send(reply);
                    System.out.println("Proposed counter: " + firstBid);
                }
            }
            else if(msg.getPerformative()==ACLMessage.ACCEPT_PROPOSAL)
            {
                System.out.printf("accept proposal received\n");
                ACLMessage replyConfirm = msg.createReply();
                replyConfirm.setPerformative(ACLMessage.INFORM);
                replyConfirm.setConversationId("csInform");
                replyConfirm.setContent(String.format(Locale.US, "%s:%s", evAgent.getCurrentLocation().name(), evAgent.getCpId()));
                myAgent.send(replyConfirm);


                AID csInform = new AID(evAgent.getCurrentLocation().name(), AID.ISLOCALNAME);
                ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                reply.setConversationId("csInform");
                reply.setPerformative(ACLMessage.INFORM);
                reply.addReceiver(csInform);

                AID evBuyer = msg.getSender();

                reply.setContent(String.format(Locale.US, "%d:%s:%s",evAgent.getSlot(), evBuyer.getLocalName(),evAgent.getCpId()));
                myAgent.send(reply);


                // Receive confirmation from CS
                MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
                ACLMessage message = myAgent.blockingReceive(template, 2000);
                if (message != null) {
                    evAgent.removeBehaviour(this);
                    evAgent.setSlotToRequest(evAgent.getSlotToRequest() + 1);


                    evAgent.sortStations(evAgent.getCurrentLocation());
                    evAgent.addBehaviour(new EVRequestCharging(evAgent));
                }

            }

        } else {
            block();
        }
    }
}
