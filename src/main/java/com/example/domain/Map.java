package com.example.domain;


import lombok.Getter;

import java.util.*;

public final class Map {
    @Getter
    private static final List<Station> stations;
    @Getter
    private static final List<Road> roads;
    static {
        stations = List.of(
                new Station("CS1"),
                new Station("CS2"),
                new Station("CS3")
        );

        roads = List.of(
                new Road(stations.get(0), stations.get(1), 5),
                new Road(stations.get(1), stations.get(2), 8),
                new Road(stations.get(2), stations.get(0), 3)
        );
    }

    public record Station(String name) {}
    public record Road(Station from, Station to, int distance) {}


}
