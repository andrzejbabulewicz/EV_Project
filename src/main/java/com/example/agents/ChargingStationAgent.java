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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.example.domain.ChargingPoint;
import jade.lang.acl.ACLMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class ChargingStationAgent extends Agent {
    private AID stationId;
    private Station location;

    @Getter
    public int realTime =0;
    public LocalTime nextSlotStartTime;
    public List<ChargingPoint> chargingPoints;



    @Getter public double basePrice = 10.00;


    @Getter @Setter
    public int noAccepted=0;
    @Getter @Setter
    public int noRejected=0;
    @Getter @Setter
    public int noTotal=0;



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

        nextSlotStartTime = LocalTime.now().plusSeconds(20);

        addBehaviour(new TickerBehaviour(this,20000) {
            @Override
            protected void onTick() {
                int noA = getNoAccepted();
                int noR = getNoRejected();
                int noT = getNoTotal();
                noAccepted = 0;
                noRejected = 0;
                noTotal = 0;

                realTime++;
                //send to all evs INFORM
                nextSlotStartTime = LocalTime.now().plusSeconds(20);

                //List<AID> occupants = new ArrayList<>();
                for (ChargingPoint cp : chargingPoints) {
                    AID ev = cp.getChargingSlots(realTime);
                    if(ev != null)
                    {
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(ev);
                        msg.setContent(String.format("%d", realTime)); //you can start charging now -> changed to slot no.
                        send(msg);
                        System.out.println(ev.getLocalName() + ": starts charging at " + cp.getCpId());
                    }
                }
                System.out.println(getLocalName() + "realTime = " + realTime);

                if(realTime > 0 && realTime <=5)
                {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("simulation_results_CS.csv", true))) {

                        // Write header line with parameters
                        writer.write(String.format("%d,%s,%d,%d,%d\n",realTime,getLocalName(),noT,noA,noR));

                    } catch (IOException e) {
                        System.err.println("Failed to write header to results file: " + e.getMessage());
                    }
                }
            }

        });



        addBehaviour(new CSListenBehaviour(this));
    }

    public double calculateFactoredPrice(int slot)
    {
        int totalPoints = chargingPoints.size();
        int occupiedPoints = 0;
        for(ChargingPoint cp : chargingPoints)
        {
            if(cp.chargingQueue[slot]!=null)
            {
                occupiedPoints++;
            }
        }

        double occupancyFactor = 1+((double)occupiedPoints/totalPoints);
        double urgencyFactor = (slot-realTime <= 1) ? 1.2 : 1.0;

        return basePrice * occupancyFactor * urgencyFactor;
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
