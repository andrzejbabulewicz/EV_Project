package com.example;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import com.example.agents.EVAgent;
import com.example.agents.ChargingStationAgent;
import com.example.domain.ChargingPoint;
import com.example.domain.Map;
import com.example.domain.chargerTypes;
import com.example.domain.Map.Station;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

public class Main
{
    public static void main(String[] args)
    {

        Runtime rt = Runtime.instance();

        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");

        ContainerController container = rt.createMainContainer(p);



        List<Station> stations = Map.getStations();

        if (stations == null)
        {
            System.out.println("No stations found");
            exit(1);
        }

        try
        {
            //-----------Initialize Charging Points-------------
            ChargingPoint cp1 = new ChargingPoint("cp1", false);
            ChargingPoint cp2 = new ChargingPoint("cp2", false);
            ChargingPoint cp3 = new ChargingPoint("cp3", false);
            ChargingPoint cp4 = new ChargingPoint("cp4", false);
            ChargingPoint cp5 = new ChargingPoint("cp5", false);
            ChargingPoint cp6 = new ChargingPoint("cp6", false);

            //------------Initialize Charging Stations---------------
            AgentController cs1 = container.createNewAgent("cs1", "com.example.agents.ChargingStationAgent",
                    new Object[]{
                            stations.get(0), // Location
                            new ArrayList<ChargingPoint>(){{    // List of Charging Points in Station
                                add(cp1);
                                add(cp2);
                                add(cp3);
                            }},
                    });
            AgentController cs2 = container.createNewAgent("cs2", "com.example.agents.ChargingStationAgent",
                    new Object[]{
                            stations.get(1), // Location
                            new ArrayList<ChargingPoint>(){{    // List of Charging Points in Station
                                add(cp4);

                            }},
                    });
            AgentController cs3 = container.createNewAgent("cs3", "com.example.agents.ChargingStationAgent",
                    new Object[]{
                            stations.get(2), // Location
                            new ArrayList<ChargingPoint>(){{    // List of Charging Points in Station
                                add(cp5);
                                add(cp6);
                            }},
                    });

            //------------Initialize Electric Vehicles---------------
            AgentController ev1 = container.createNewAgent("ev1", "com.example.agents.EVAgent",
                    new Object[]{
                            chargerTypes.CCS2, // type
                            2.0,
                            15.0,     // batteryLevel
                            100.0,    // maxBatteryLevel
                            stations.get(0), // currentLocation
                            500.0 // totalMoney
                    });
            AgentController ev2 = container.createNewAgent("ev2", "com.example.agents.EVAgent",
                    new Object[]{
                            chargerTypes.Type2, // type
                            3.5,
                            15.0,     // batteryLevel
                            100.0,    // maxBatteryLevel
                            stations.get(1), // currentLocation
                            500.0 // totalMoney

                    });
            cs1.start();
            cs2.start();
            cs3.start();

            ev1.start();
            ev2.start();

            System.out.println("CS, CP, and EV agents have been started.");
        }
        catch (StaleProxyException e)
        {
            System.out.println("Stale proxy exception");
            exit(1);
        }

    }
}
