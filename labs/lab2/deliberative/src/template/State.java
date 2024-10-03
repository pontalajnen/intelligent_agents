package template;

import logist.plan.Action;
import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class State {
    private State parentState;
    private Topology.City location;
    private double totalCost;
    private TaskSet carryingTasks;
    private TaskSet remainingTasks;
    private TaskSet deliveredTasks;
    private List<Action> actions;
    private int totalSteps;

    public State(State parentState, Topology.City location, double totalCost, TaskSet carryingTasks, TaskSet remainingTasks, TaskSet deliveredTasks, List<Action> actions, int totalSteps) {
        this.parentState = parentState;
        this.location = location;
        this.totalCost = totalCost;
        this.carryingTasks = carryingTasks;
        this.remainingTasks = remainingTasks;
        this.deliveredTasks = deliveredTasks;
        this.actions = actions;
        this.totalSteps = totalSteps;

    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, carryingTasks, remainingTasks, deliveredTasks);
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public State getParentState() {
        return parentState;
    }

    public TaskSet getDeliveredTasks() {
        return deliveredTasks;
    }

    public void setDeliveredTasks(TaskSet deliveredTasks) {
        this.deliveredTasks = deliveredTasks;
    }

    public void setParentState(State parentState) {
        this.parentState = parentState;
    }

    public Topology.City getLocation() {
        return location;
    }

    public void setLocation(Topology.City location) {
        this.location = location;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public TaskSet getCarryingTasks() {
        return carryingTasks;
    }

    public void setCarryingTasks(TaskSet carryingTasks) {
        this.carryingTasks = carryingTasks;
    }

    public TaskSet getRemainingTasks() {
        return remainingTasks;
    }

    public void setRemainingTasks(TaskSet remainingTasks) {
        this.remainingTasks = remainingTasks;
    }
}
