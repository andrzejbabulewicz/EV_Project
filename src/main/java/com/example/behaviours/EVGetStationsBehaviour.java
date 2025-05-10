package com.example.behaviours;

import jade.core.AID;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import com.example.agents.EVAgent;
import org.agents.exceptions.InvalidServiceSpecification;

public class EVGetStationsBehaviour extends WakerBehaviour {

    private final EVAgent evAgent;
    public EVGetStationsBehaviour(EVAgent evAgent) {
        super(evAgent, 2000);
        this.evAgent = evAgent;
    }
    @Override
    public void onWake() {
        System.out.printf("[%s] Searching for available Charging Stations...%n", evAgent.getLocalName());

        // Create a service description to search for ChargingStation agents
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("ChargingStation");

        try {
            DFAgentDescription template = new DFAgentDescription();
            template.addServices(serviceDescription);
            DFAgentDescription[] availableServices = DFService.search(evAgent, template);

            // Clear current list (if any) and add found stations.
            evAgent.getStations().clear();
            for (DFAgentDescription station : availableServices) {
                AID csAID = station.getName();
                System.out.printf("[%s] Found Charging Station: %s%n", evAgent.getLocalName(), csAID.getLocalName());
                evAgent.getStations().add(csAID);
            }
        } catch (FIPAException e) {
            throw new InvalidServiceSpecification(e);
        }

        evAgent.addBehaviour(new EVRoamBehaviour(evAgent));
    }
}
