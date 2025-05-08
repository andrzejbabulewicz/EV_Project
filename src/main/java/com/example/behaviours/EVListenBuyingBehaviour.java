package com.example.behaviours;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.example.agents.EVAgent;

import java.util.ArrayList;
import java.util.List;

public class EVListenBuyingBehaviour extends CyclicBehaviour {

    private final EVAgent evAgent;
    private List<AID> evsInQueue = new ArrayList<>();
    private int evCount = 0;

    public EVListenBuyingBehaviour(EVAgent evAgent) {
        this.evAgent = evAgent;
    }

    public void action() {
        if (!evAgent.isNegotiating()) {
            if (!evAgent.isAskingCS()) {
                MessageTemplate conversationIDMatch = MessageTemplate.MatchConversationId("StationResponse");
                MessageTemplate agreeOrRefuse = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
                MessageTemplate messageTemplate = MessageTemplate.and(conversationIDMatch, agreeOrRefuse);
                ACLMessage message = myAgent.receive(messageTemplate);
                if (message != null) {
                    if (message.getPerformative() == ACLMessage.AGREE) {
                        // Handle accepted communication
                        evAgent.setAskingCS(true);
                    } else {
                        // Handle rejected communication
                    }
                }
            } else {
                MessageTemplate conversationIDMatch = MessageTemplate.MatchConversationId("StationResponse");
                MessageTemplate infoOrRefuse = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
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
        }
        else {
            if (!evAgent.isAskingEV()) {
                MessageTemplate conversationIDMatch = MessageTemplate.MatchConversationId("EVResponse");
                MessageTemplate agreeOrRefuse = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
                MessageTemplate messageTemplate = MessageTemplate.and(conversationIDMatch, agreeOrRefuse);
                ACLMessage message = myAgent.receive(messageTemplate);
                if (message != null) {
                    if (message.getPerformative() == ACLMessage.AGREE) {
                        // Handle accepted communication
                        evAgent.setAskingEV(true);
                    } else {
                        // Handle rejected communication
                    }
                }

            } else {
                MessageTemplate conversationIDMatch = MessageTemplate.MatchConversationId("EVResponse");
                MessageTemplate agreeOrPropose = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
                MessageTemplate agreeOrProposeOrCancel = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE), agreeOrPropose);
                MessageTemplate messageTemplate = MessageTemplate.and(conversationIDMatch, agreeOrProposeOrCancel);
                ACLMessage message = myAgent.receive(messageTemplate);
                if (message != null) {
                    if (message.getPerformative() == ACLMessage.PROPOSE) {
                        // Handle counteroffer
                    } else if (message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        // Handle accepting the offer
                        evAgent.setAskingEV(false);
                        evCount++;
                    } else {
                        // Handle canceling the offer
                        evAgent.setAskingEV(false);
                        evCount++;
                    }

                    if (evCount >= evsInQueue.size()) {

                    }
                }
            }
        }
    }
    private void startNegotiation() {
        // Send messages to EVs that wait in queues
        evAgent.setNegotiating(true);
    }
}
