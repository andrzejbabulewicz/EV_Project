package com.example.behaviours;

import com.example.domain.Map.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import com.example.agents.EVAgent;
import jade.lang.acl.MessageTemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class EVRequestCharging extends CyclicBehaviour {

    private final EVAgent evAgent;
    private int tryCounter = 0;
    private double currentPrice = 0;

    public EVRequestCharging(EVAgent evAgent) { this.evAgent = evAgent; }

    public void action() {
        // Send messages to CS

        while (tryCounter < 2) {
            // send request for a slot
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);

            request.addReceiver(evAgent.getCurrentCommunicationAid());
            request.setContent(String.format("%d", evAgent.getSlotToRequest()));

            evAgent.noOfTrials++;
            evAgent.send(request);
            System.out.println("[" + evAgent.getLocalName() + "]" + " sent request to " + evAgent.getCurrentCommunication());

            // wait for a proposal or a list of AIDs
            MessageTemplate messageTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
            ACLMessage message = myAgent.blockingReceive(messageTemplate, 2000);

            if (message != null) {
                String content = message.getContent();
                if (message.getConversationId().equals("cs_reply"))
                {
                    if (message.getPerformative() == ACLMessage.PROPOSE) {
                        // Handle CS proposal
                        String[] parts = content.split(":");

                        // Termination signal from CS
                        if (Integer.parseInt(parts[0]) == 0) {
                            evAgent.terminate();
                        }

                        if (Double.parseDouble(parts[1]) < evAgent.getTotalMoney()) {
                            // Accept the proposal
                            ACLMessage confirm = message.createReply();
                            confirm.setPerformative(ACLMessage.CONFIRM);

                            confirm.setContent("OK");

                            evAgent.send(confirm);

                            evAgent.setSlot(Integer.parseInt(parts[0]));
                            evAgent.setCpId(parts[2]);
                            evAgent.setChargingPrice(Double.parseDouble(parts[1]));


                            System.out.println("[" + evAgent.getLocalName() + "]" + " accepts slot " + parts[0] + " at " +
                                    parts[2] + ", " + evAgent.getCurrentCommunication() +
                                    ", for $" + evAgent.getChargingPrice());

                            if(evAgent.didIncreaseSlot == true)
                            {
                                evAgent.noOfPostponedPurchases++;
                                evAgent.setDidIncreaseSlot(false);
                            }
                            else
                            {
                                evAgent.noOfDirectPurchases++;
                            }
                            // Travel to the charging point
                            if (!evAgent.getCurrentLocation().equals(evAgent.getCurrentCommunication())) {
                                evAgent.travelToCp(evAgent.getCurrentCommunication());
                            }


                            if(evAgent.getSlotToRequest() > 1)
                            {
                                evAgent.setSlotToRequest(1);
                            }

                            evAgent.removeBehaviour(this);
                            evAgent.addBehaviour(new EVResellingBehaviour(evAgent));
                            return;
                        }
                        else {
                            // Price too high FOR NOW IT IS USELESS

                            if (currentPrice != 0)
                                evAgent.sumNextPrice(currentPrice);

                            if (!evAgent.askNextStation())
                            {
                                evAgent.calculateMeanPrice();

                                // Start negotiations
                                evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));

                                evAgent.removeBehaviour(this);
                            }
                        }

                    } else {
                        // Handle rejected communication
                        String[] parts = content.split(":");

                        if (parts.length != 3)
                            System.out.println("INCORRECT MESSAGE FORMAT");

                        if (Integer.parseInt(parts[0]) == -1) {
                            evAgent.setTooLateForNegotiation(true);
                        }
                        else {
                            String[] names = parts[2].split(",");

                            // Add ev's available for negotiations
                            for (String s : names) {
                                evAgent.getEvInQueue().add(new AID(s, AID.ISLOCALNAME));
                            }
                            evAgent.setSlot(Integer.parseInt(parts[0]));
                        }

                        currentPrice = Double.parseDouble(parts[1]);

                        if (currentPrice != 0)
                            evAgent.sumNextPrice(currentPrice);
                        //System.out.println("-------- we are at" + evAgent.getStationIndex());
                        if (!evAgent.askNextStation())
                        {

                            evAgent.calculateMeanPrice();

                            if (evAgent.isTooLateForNegotiation()) {
                                // Too late for negotiations, find a spot at later time
                                evAgent.setSlotToRequest(evAgent.getSlotToRequest() + 1);
                                evAgent.noOfNegotiationsFailed++;
                            }
                            else {
                                // Start negotiations
                                evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));
                                evAgent.removeBehaviour(this);
                            }
                        }

                    }
                }
                break;
            }
            else {
                tryCounter++;
            }
        }

        // No response after multiple requests
        if (!evAgent.askNextStation())
        {
            evAgent.calculateMeanPrice();

            if (evAgent.isTooLateForNegotiation()) {
                // Too late for negotiations, find a spot at later time
                evAgent.setSlotToRequest(evAgent.getSlotToRequest() + 1);
                evAgent.setDidIncreaseSlot(true);
            }
            else {
                // Start negotiations
                evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));
                evAgent.removeBehaviour(this);
            }
        }
    }

}
