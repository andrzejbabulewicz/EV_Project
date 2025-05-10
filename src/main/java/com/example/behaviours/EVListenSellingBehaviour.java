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

    }
}
