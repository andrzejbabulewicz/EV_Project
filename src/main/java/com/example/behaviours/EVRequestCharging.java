package com.example.behaviours;

import com.example.domain.Map.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import com.example.agents.EVAgent;
import jade.lang.acl.MessageTemplate;

public class EVRequestCharging extends CyclicBehaviour {

    private final EVAgent evAgent;
    private int tryCounter = 0;

    public EVRequestCharging(EVAgent evAgent) { this.evAgent = evAgent; }

    public void action() {
        // Send messages to CS

        while (tryCounter < 2) {
            // send request for a slot
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            Station station = evAgent.getStationAtIndex(evAgent.getStationIndex());
            AID stationId = new AID(station.name(), AID.ISLOCALNAME);

            request.addReceiver(evAgent.getCurrentCommunicationAid());

            request.setContent("1");

            evAgent.send(request);
            System.out.println(evAgent.getLocalName() + " sent request to " + evAgent.getCurrentCommunication());

            // wait for a proposal or a list of AIDs
            MessageTemplate messageTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
            ACLMessage message = myAgent.blockingReceive(messageTemplate, 2000);
            String content = message.getContent();

            if (message != null) {
                if (message.getPerformative() == ACLMessage.PROPOSE) {
                    // Handle CS proposal
                    String[] parts = content.split(":");

                    if (Double.parseDouble(parts[1]) < evAgent.getTotalMoney()) {
                        // Accept the proposal
                        ACLMessage confirm = message.createReply();
                        confirm.setPerformative(ACLMessage.CONFIRM);

                        confirm.setContent("OK");

                        evAgent.send(confirm);

                        evAgent.setSlot(Integer.parseInt(parts[0]));
                        evAgent.setCpId(parts[2]);
                        evAgent.setChargingPrice(Double.parseDouble(parts[1]));

                        System.out.println(evAgent.getLocalName() + " accepts slot " + parts[0] + " at " +
                                parts[2] + ", " + evAgent.getCurrentCommunication() +
                                ", for $" + evAgent.getChargingPrice());

                        // Travel to the charging point
                        if (!evAgent.getCurrentLocation().equals(evAgent.getCurrentCommunication())) {
                            evAgent.travelToCp();
                        }

                        evAgent.removeBehaviour(EVRequestCharging.this);
                        evAgent.addBehaviour(new EVListenSellingBehaviour(evAgent));
                        return;
                    }
                    else {
                        // Price to high
                        if (!evAgent.askNextStation())
                        {
                            // Start negotiations
                            evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));
                            evAgent.removeBehaviour(new EVRequestCharging(evAgent));
                            return;
                        }
                        break;
                    }

                } else {
                    // Handle rejected communication
                    String[] parts = content.split(":");
                    String[] names = parts[1].split(",");

                    // Add ev's available for negotiations
                    for (String s : names) {
                        evAgent.getEvInQueue().add(new AID(s, AID.ISLOCALNAME));
                    }


                    if (!evAgent.askNextStation())
                    {
                        // Start negotiations
                        evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));
                        evAgent.removeBehaviour(new EVRequestCharging(evAgent));
                        evAgent.setEvInQueueIndex(0);
                        return;
                    }
                    break;
                }
            }
            else {
                tryCounter++;
            }

            // No response after multiple requests
            if (!evAgent.askNextStation())
            {
                // Start negotiations
                evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));
                evAgent.removeBehaviour(new EVRequestCharging(evAgent));
                evAgent.setEvInQueueIndex(0);
                return;
            }
        }
    }

}
