package com.example.agents;

import com.example.domain.Map.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import com.example.behaviours.CSListenBehaviour;

import java.util.List;

import com.example.domain.ChargingPoint;

public class ChargingStationAgent extends Agent {
    private AID stationId;
    private Station location;

    public int realTime =0;
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
            location = (Station)args[0];
            chargingPoints = (List<ChargingPoint>) args[1];
        } else {
            location = null;
            chargingPoints = null;
        }
        System.out.println(getLocalName() + " started at location: " + location);
        if (chargingPoints != null) {
            System.out.println(getLocalName() + " initialized with " + chargingPoints.size() + " charging point(s).");
        } else {
            System.out.println(getLocalName() + " has no charging points set.");
        }
        registerInDF();

        addBehaviour(new TickerBehaviour(this,10000) {
            @Override
            protected void onTick() {
                realTime++;
                System.out.println(getLocalName() + "realTime = " + realTime);
            }
        });

        addBehaviour(new CSListenBehaviour(this));
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
