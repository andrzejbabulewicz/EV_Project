package com.example.behaviours;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import com.example.agents.EVAgent;

public class EVRoamBehaviour extends CyclicBehaviour {

    private final EVAgent evAgent;
    public EVRoamBehaviour(EVAgent evAgent) { this.evAgent = evAgent; }

    @Override
    public void action() {

        if (evAgent.getBatteryLevel() < evAgent.getMaxBatteryLevel()) {

            evAgent.setWantsToCharge(true);
            evAgent.setCurrentCommunication(evAgent.getCurrentLocation());
            evAgent.setCurrentCommunicationAid(new AID(evAgent.getCurrentCommunication().name(), AID.ISLOCALNAME));

            evAgent.addBehaviour(new EVRequestCharging(evAgent));
            evAgent.addBehaviour(new EVRoamBehaviour(evAgent));
        }
        else {
            // Roam or wait behaviour
        }

    }
}
