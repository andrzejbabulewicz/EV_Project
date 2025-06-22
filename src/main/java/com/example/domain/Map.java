package com.example.domain;


import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class Map {
    @Getter
    @Setter
    private static List<Station> stations;
    @Getter
    @Setter
    private static List<Road> roads;

    static {
        stations = new ArrayList<>();
        roads = new ArrayList<>();
    }

    public static Station getStationByName(String name) {
        for (Station station : stations) {
            if (station.name().equals(name)) {
                return station;
            }
        }
        return null;
    }

    // Create roads 
    public static void createRandomGraph(int additionalEdges) {
        roads.clear();

        int noOfStations = stations.size();
        Random random = new Random();

        List<Station> shuffledStations = new ArrayList<>(stations);
        Collections.shuffle(shuffledStations);

        for (int i = 1; i < shuffledStations.size(); i++) {
            Station currentStation = shuffledStations.get(i);
            Station targetStation = shuffledStations.get(random.nextInt(i));
            int distance = 6 + (int)(5 * random.nextDouble()); // Random distance between 10 and 100
            roads.add(new Road(currentStation, targetStation, distance));
        }

        int addedEdges = 0;
        int maxAttempts = additionalEdges * 5;
        int attempts = 0;
        while (addedEdges < additionalEdges && attempts < maxAttempts) {
            Station from = stations.get(random.nextInt(noOfStations));
            Station to = stations.get(random.nextInt(noOfStations));

            if (!from.equals(to) && !roadExists(from, to)) {
                int distance = 6 + random.nextInt(5);
                roads.add(new Road(from, to, distance));
                addedEdges++;
            }
            attempts++;
        }
    }

    private static boolean roadExists(Station from, Station to) {
        for (Road road : roads) {
            if ((road.from().equals(from) && road.to().equals(to)) ||
                    (road.from().equals(to) && road.to().equals(from))) {
                return true;
            }
        }
        return false;
    }

    public record Station(String name) {}
    public record Road(Station from, Station to, int distance) {}


}
