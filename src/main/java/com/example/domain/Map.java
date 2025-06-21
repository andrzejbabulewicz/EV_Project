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

    public static void createRandomGraph(int additionalEdges) {
        // Clear any existing roads to start fresh, but keep the stations.
        roads.clear();

        int noOfStations = stations.size();
        if (noOfStations < 2) {
            // Not enough stations to form a graph
            return;
        }

        Random random = new Random();

        // 1. Create a spanning tree to ensure the graph is connected.
        // We use a copy of the stations list and shuffle it to randomize the tree structure.
        List<Station> shuffledStations = new ArrayList<>(stations);
        Collections.shuffle(shuffledStations);

        for (int i = 1; i < shuffledStations.size(); i++) {
            Station currentStation = shuffledStations.get(i);
            // Connect to a random, already-connected station (from index 0 to i-1)
            Station targetStation = shuffledStations.get(random.nextInt(i));
            int distance = 10 + random.nextInt(91); // Random distance between 10 and 100
            roads.add(new Road(currentStation, targetStation, distance));
        }

        // 2. Add a few extra edges to make the graph more complex.
        int addedEdges = 0;
        int maxAttempts = additionalEdges * 5; // To prevent infinite loops
        int attempts = 0;
        while (addedEdges < additionalEdges && attempts < maxAttempts) {
            // Pick two distinct random stations
            Station from = stations.get(random.nextInt(noOfStations));
            Station to = stations.get(random.nextInt(noOfStations));

            // Ensure they are not the same station and a road doesn't already exist
            if (!from.equals(to) && !roadExists(from, to)) {
                int distance = 10 + random.nextInt(91);
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
