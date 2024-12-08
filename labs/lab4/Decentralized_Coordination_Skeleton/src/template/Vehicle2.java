package template;

import logist.simulation.Vehicle;
import logist.topology.Topology;

public class Vehicle2 {
    private Topology.City currentCity;
    private Vehicle vehicle;

    public Vehicle2(Topology.City currentCity, Vehicle vehicle) {
        this.currentCity = currentCity;
        this.vehicle = vehicle;
    }

    public Topology.City getCurrentCity() {
        return currentCity;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setCurrentCity(Topology.City city) {
        this.currentCity = city;
    }
}