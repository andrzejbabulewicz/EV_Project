package com.example.behaviours;

import jade.core.behaviours.OneShotBehaviour;
import com.example.agents.EVAgent;

public class EVRequestCharging extends OneShotBehaviour {

    private final EVAgent evAgent;
    public EVRequestCharging(EVAgent evAgent) { this.evAgent = evAgent; }

    public void action() {
        if (evAgent.isWantsToCharge())
        {
            // Send messages to Charging Stations

            evAgent.addBehaviour(new EVListenBuyingBehaviour(evAgent));
        }
    }

}
