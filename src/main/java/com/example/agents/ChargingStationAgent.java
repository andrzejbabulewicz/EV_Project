package com.example.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.List;
import com.example.domain.ChargingPoint;

public class ChargingStationAgent extends Agent {
    private AID stationId;
    private String location;
    private List<ChargingPoint> chargingPoints;

    protected void setup() {
        stationId = getAID();

        try
        {
            System.out.println(getLocalName() + ": you have 20s for setting up sniffer");
            Thread.sleep(20000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        // Expected parameters: [location (String), chargingPoints (List<ChargingPoint>)]
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            //location = (String) args[0];
            chargingPoints = (List<ChargingPoint>) args[1];
        } else {
            location = "Undefined";
            chargingPoints = null;
        }
        System.out.println(getLocalName() + " started at location: " + location);
        if (chargingPoints != null) {
            System.out.println(getLocalName() + " initialized with " + chargingPoints.size() + " charging point(s).");
        } else {
            System.out.println(getLocalName() + " has no charging points set.");
        }
        registerInDF();

        addBehaviour(new EVRequestBehaviour());
    }
    private void registerInDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ChargingStation");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " successfully registered in DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }


    protected void takeDown() {
        System.out.println(getLocalName() + " terminating.");
    }

    protected void bookSlot()
    {
        /*
         * booking a given spot
         */
    }

    protected void removeBooking()
    {
        /*
         * removing a given booking without returning the money
         */
    }

    protected void processPayment()
    {
        /*
         * payment logic
         */
    }


    private class EVRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    System.out.println(getLocalName() + " received EV request: " + msg.getContent());
                    ACLMessage reply = msg.createReply();

                    //a dummy answer for now
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Request received. Processing...");
                    send(reply);
                    System.out.println(getLocalName() + " sent reply.");
                }

                /*
                * it should check the availability and return positive information
                    with a suggested price to the EV in case of a free space
                * in case of no free spaces, it sends the AIDs of EVs that match the time slot requirements
                * for now, there is no price negotiation between EV and CS,
                    left for future consideration
                */

            } else {
                block();
            }
        }
    }
}
