package com.example.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import com.example.behaviours.EVRequestBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.example.domain.ChargingPoint;
import lombok.Getter;

public class ChargingStationAgent extends Agent {
    private AID stationId;
    private String location;


    public List<ChargingPoint> chargingPoints;



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

        addBehaviour(new EVRequestBehaviour(this));
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




}
