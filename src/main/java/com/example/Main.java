package com.example;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import com.example.domain.ChargingPoint;
import com.example.domain.Map;
import com.example.domain.chargerTypes;
import com.example.domain.Map.Station;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.System.exit;

public class Main
{
    public static void main(String[] args)
    {
        // SIMULATION PARAMETERS
        int noOfCp = 11;
        int noOfCs = 7;
        int noOfEv = 10;
        int numberOfExtraRoads = 2;

        List<AgentController> csAgents = new ArrayList<>();
        List<AgentController> evAgents = new ArrayList<>();

        Runtime rt = Runtime.instance();

        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");

        ContainerController container = rt.createMainContainer(p);

        //-----------Initialize Map Structure-------------
        for (int i = 0; i < noOfCs; i++) {
            Map.getStations().add(new Station(String.format("CS%d", i + 1)));
        }
        Map.createRandomGraph(numberOfExtraRoads);

        List<Station> stations = Map.getStations();

        if (stations == null)
        {
            System.out.println("No stations found");
            exit(1);
        }

        System.out.println("Map generated.");

        try
        {
            //-----------Initialize Charging Points-------------
            ChargingPoint[] cpArray = new ChargingPoint[noOfCp];
            for (int i = 0; i < noOfCp; i++){
                String name = String.format("cp%d", i + 1);
                cpArray[i] = new ChargingPoint("cp1", false);
            }

            //------------Initialize Charging Stations---------------
            int index = 0;
            int cpPerCS = noOfCp / noOfCs;
            int remainder = noOfCp % noOfCs;

            for (int i = 0; i < noOfCs; i++){
                List<ChargingPoint> cpList = new ArrayList<>();
                int thisCp = cpPerCS + (i < remainder ? 1 : 0);

                for (int j = 0; j < thisCp; j++){
                    cpList.add(cpArray[index++]);
                }

                String name = String.format("cs%d", i + 1);

                csAgents.add(container.createNewAgent(
                        name,
                        "com.example.agents.ChargingStationAgent",
                        new Object[]{
                                stations.get(i), // Location
                                cpList      // The fairly distributed list of CPs
                        }
                ));
            }


            Random r = new Random();
            //------------Initialize Electric Vehicles---------------
            for (int i = 0; i < noOfEv; i++) {
                double batteryLevel = 15 + 25 * r.nextDouble();
                String evName = String.format("ev%d", i + 1);
                evAgents.add(container.createNewAgent(evName, "com.example.agents.EVAgent",
                        new Object[]{
                                chargerTypes.CCS2, // type
                                2.0,        // batteryPerKm
                                batteryLevel,     // batteryLevel
                                100.0,    // maxBatteryLevel
                                stations.get((int)(r.nextDouble()*noOfCs)), // currentLocation
                                500.0, // totalMoney
                                r.nextDouble() // chargingUrgency
                        }));
            }

            for (int i = 0; i < noOfCs; i++)
                csAgents.get(i).start();
            for (int i = 0; i < noOfEv; i++)
                evAgents.get(i).start();

            System.out.println("CS, CP, and EV agents have been started.");
        }
        catch (StaleProxyException e)
        {
            System.out.println("Stale proxy exception");
            exit(1);
        }

    }
}
