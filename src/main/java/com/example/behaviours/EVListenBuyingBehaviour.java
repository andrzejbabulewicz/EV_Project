package com.example.behaviours;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.example.agents.EVAgent;

import java.util.ArrayList;
import java.util.List;

public class EVListenBuyingBehaviour extends CyclicBehaviour {
    /// NEGOTIATING BUYING SIDE
    private final EVAgent evAgent;
    private List<AID> evsInQueue = new ArrayList<>();
    private int evCount = 0;

    public EVListenBuyingBehaviour(EVAgent evAgent) {
        this.evAgent = evAgent;
    }

    public void action() {
        // Logic for negotiating from the buyer perspective
    }
}
