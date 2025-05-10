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
    private int step = 0;

    public EVRequestCharging(EVAgent evAgent) { this.evAgent = evAgent; }

    public void action() {
        // Send messages to CS

        if (step == 0) {

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            Station station = evAgent.getStationAtIndex(evAgent.getStationIndex());
            AID stationId = new AID(station.name(), AID.ISLOCALNAME);

            request.addReceiver(evAgent.getCurrentCommunicationAid());
            request.setContent("Request for charging");
            evAgent.send(request);
            System.out.println(evAgent.getLocalName() + " sent request to " + evAgent.getCurrentCommunication());

            MessageTemplate conversationIDMatch = MessageTemplate.MatchConversationId("StationResponse");
            MessageTemplate agreeOrRefuse = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
            MessageTemplate messageTemplate = MessageTemplate.and(conversationIDMatch, agreeOrRefuse);
            ACLMessage message = myAgent.receive(messageTemplate);
            if (message != null) {
                if (message.getPerformative() == ACLMessage.AGREE) {
                    // Handle accepted communication
                    step++;


                } else {
                    // Handle rejected communication
                    if (evAgent.getStationIndex() != evAgent.getSortedStations().size() - 1) {
                        evAgent.setStationIndex(evAgent.getStationIndex() + 1);
                        // talk to next station
                    }
                    else {
                        //start negotiation
                        evAgent.setNegotiating(true);
                    }
                }
            }
        } else {
            MessageTemplate conversationIDMatch = MessageTemplate.MatchConversationId("StationResponse");
            MessageTemplate infoOrRefuse = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            MessageTemplate messageTemplate = MessageTemplate.and(conversationIDMatch, infoOrRefuse);
            ACLMessage message = myAgent.receive(messageTemplate);
            if (message != null) {
                if (message.getPerformative() == ACLMessage.INFORM) {
                    // Handle an offer from station
                    // If it is full, station sends a list of EVs waiting to charge
                    // If price is too high, request the list anyway, and: startNegotiation();
                } else {
                    // Handle refusal to give info
                    evAgent.setAskingCS(false);
                }
            }
        }
        evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));
    }

}
