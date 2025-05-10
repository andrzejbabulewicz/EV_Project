package com.example.behaviours;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.example.agents.EVAgent;

import static java.lang.Thread.sleep;

public class EVListenSellingBehaviour extends CyclicBehaviour {

    /// NEGOTIATION SELLING SIDE AND WAITING TO CHARGE
    private final EVAgent evAgent;

    public EVListenSellingBehaviour(EVAgent evAgent) { this.evAgent = evAgent; }
    @Override
    public void action() {
        // Logic for waiting in queue and listening for negotiation proposals
        MessageTemplate messageTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage message = myAgent.receive(messageTemplate);

        System.out.println(evAgent.getLocalName() + " waiting for charging");
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (message != null) {
            if (message.getPerformative() == ACLMessage.INFORM) {
                // Messages that the charging can start
                evAgent.charge(20);
                System.out.println(evAgent.getLocalName() + " charged at " + evAgent.getCurrentLocation());
                evAgent.addBehaviour(new EVRoamBehaviour(evAgent));
                evAgent.removeBehaviour(EVListenSellingBehaviour.this);
            }
            else if (message.getPerformative() == ACLMessage.REQUEST) {
                beginNegotiation(message.getSender());
            }
        }
    }

    private void beginNegotiation(AID agent) {

    }
}
