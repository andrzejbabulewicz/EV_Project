package com.example.behaviours;

import com.example.domain.Map;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import com.example.agents.EVAgent;
import com.example.domain.Map.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EVRoamBehaviour extends CyclicBehaviour {

    private final EVAgent evAgent;
    public EVRoamBehaviour(EVAgent evAgent) { this.evAgent = evAgent; }

    @Override
    public void action() {

        // Some randomness for the decision-making
        Random random = new Random();
        if (evAgent.getBatteryLevel() < evAgent.getMaxBatteryLevel() * 0.5) {

            evAgent.setCurrentCommunication(evAgent.getCurrentLocation());
            evAgent.setCurrentCommunicationAid(new AID(evAgent.getCurrentCommunication().name(), AID.ISLOCALNAME));

            evAgent.sortStations(evAgent.getCurrentLocation());
            evAgent.addBehaviour(new EVRequestCharging(evAgent));
            evAgent.removeBehaviour(this);
        }
        else {
            // Choose destination and roam randomly
            List<Road> possibleRoads = new ArrayList<>();

            for (Road r : Map.getRoads()) {
                if (r.from().name() == evAgent.getCurrentLocation().name())
                    possibleRoads.add(r);
                else if (r.to().name() == evAgent.getCurrentLocation().name())
                    possibleRoads.add(r);
            }
            Random rand = new Random();
            int index = rand.nextInt(possibleRoads.size());

            evAgent.travel(possibleRoads.get(index));
        }

    }
}
