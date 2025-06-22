package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class WriteToFiles
{
    public static void writeToFileCS(int noOfCp, int noOfCs, int noOfEv, int numberOfExtraRoads)
    {
        String filename = "simulation_results_CS.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {

            // Write header line with parameters
            writer.write("----------------------------------NEW SIMULATION----------------------------------:\n");
            writer.write("Simulation Parameters:\n");
            writer.write(String.format("Charging Points: %d\n", noOfCp));
            writer.write(String.format("Charging Stations: %d\n", noOfCs));
            writer.write(String.format("Electric Vehicles: %d\n", noOfEv));
            writer.write(String.format("Extra Roads: %d\n", numberOfExtraRoads));
            writer.write("\n");

            // Placeholder for column headers of simulation results
            writer.write("Timeslot,CS_no,total,accepted,rejected\n");

        } catch (IOException e) {
            System.err.println("Failed to write header to results file: " + e.getMessage());
        }
    }
    public static void writeToFileEV(int noOfCp, int noOfCs, int noOfEv, int numberOfExtraRoads)
    {
        String filename = "simulation_results_EV.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {

            // Write header line with parameters
            writer.write("----------------------------------NEW SIMULATION----------------------------------:\n");
            writer.write("Simulation Parameters:\n");
            writer.write(String.format("Charging Points: %d\n", noOfCp));
            writer.write(String.format("Charging Stations: %d\n", noOfCs));
            writer.write(String.format("Electric Vehicles: %d\n", noOfEv));
            writer.write(String.format("Extra Roads: %d\n", numberOfExtraRoads));
            writer.write("\n");

            // Placeholder for column headers of simulation results
            writer.write("EV_no,total_trials,negotiations,direct_purchases,negot_purchases,failed_purchases,total_frustration\n");

        } catch (IOException e) {
            System.err.println("Failed to write header to results file: " + e.getMessage());
        }
    }
}
