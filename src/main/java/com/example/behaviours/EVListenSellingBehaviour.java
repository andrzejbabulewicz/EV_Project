package com.example.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.example.agents.EVAgent;

public class EVListenSellingBehaviour extends CyclicBehaviour {

    private final EVAgent evAgent;
    public EVListenSellingBehaviour(EVAgent evAgent) { this.evAgent = evAgent; }
    @Override
    public void action() {
        if (evAgent.isWaiting()) {
            if (!evAgent.isAnsweringEV()) {
                MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchConversationId("NegotiationRequest"));
                ACLMessage message = myAgent.receive(messageTemplate);
                if (message != null) {
                    // Accept or refuse a request for negotiations
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
                    } else {
                        // Handle canceling the offer
                    }
                }
            }

            MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("ChargingPointReady"));
            ACLMessage message = myAgent.receive(messageTemplate);
            if (message != null) {
                // Start charging and stop listening for negotiation offers
            }
        }
    }
}
