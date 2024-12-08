package template;

import logist.Measures;
import logist.simulation.Vehicle;
import logist.simulation.VehicleController;
import logist.simulation.VehicleImpl;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.awt.*;

public class OurVehicle implements Vehicle {
    private final String name;
    private final int id;
    private final int capacity;
    private final int costPerKm;
    private final City homeCity;
    private long totalReward;
    private long totalDistance;
    private TaskSet currentTasks;
    private City nextCity;
    private long speed;
    private Color color;


    public OurVehicle(Vehicle vehicle) {
        this.name = vehicle.name();
        this.id = vehicle.id();
        this.capacity = vehicle.capacity();
        this.costPerKm = vehicle.costPerKm();
        this.homeCity = vehicle.homeCity();
        this.totalReward = vehicle.getReward();
        this.totalDistance = vehicle.getDistanceUnits();
        this.currentTasks = vehicle.getCurrentTasks();
        this.nextCity = vehicle.getCurrentCity();
        this.speed = (long) vehicle.speed();
        this.color = vehicle.color();

    }

    public int capacity() {
        return capacity;
    }

    public int id() {
        return id;
    }

    public TaskSet getCurrentTasks() {
        return currentTasks.clone();
    }

    public Topology.City getCurrentCity() {
        return nextCity;
    }

    public Topology.City homeCity() {
        return homeCity;
    }

    public String name() {
        return name;
    }

    public double speed() {
        return Measures.unitsToKM(speed);
    }

    public long getReward() {
        return totalReward;
    }

    public long getDistanceUnits() {
        return totalDistance;
    }

    public double getDistance() {
        return Measures.unitsToKM(totalDistance);
    }

    public int costPerKm() {
        return costPerKm;
    }

    public Color color() {
        return color;
    }
}
