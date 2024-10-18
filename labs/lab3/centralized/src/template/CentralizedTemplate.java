package template;

//the list of imports

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;
import java.util.*;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        System.out.println("Before SLS");
        var sls = new SLS(vehicles, tasks);

        var plan = sls.createPlan();
        System.out.println("After SLS");
        //printPlan(plan);

        var plans = new ArrayList<Plan>();


        var planMap = plan.getNextState();

        for(var vehicle : vehicles){
            if(planMap.containsKey(vehicle)){
                var vehiclePlan = buildPlan(vehicle, planMap.get(vehicle));
                plans.add(vehiclePlan);
            }
            else{
               plans.add(Plan.EMPTY);
            }
        }

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        System.out.println(plan);

        return plans;
    }

    private Plan buildPlan(Vehicle vehicle, LinkedList<State> stateList){
        var currentCity = vehicle.homeCity();
        var plan = new Plan(currentCity);

        for(var state : stateList){
            if (state.isPickup()){
                for(var city : currentCity.pathTo(state.getTask().pickupCity)){
                    plan.appendMove(city);
                }
                currentCity = state.getTask().pickupCity;
                plan.appendPickup(state.getTask());
            }
            else{
                for(var city : currentCity.pathTo(state.getTask().deliveryCity)){
                    plan.appendMove(city);
                }
                currentCity = state.getTask().deliveryCity;
                plan.appendDelivery(state.getTask());

            }
        }
        return plan;
    }


    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
    public void printPlan(CentralizedPlan plan) {
        HashMap<Vehicle, LinkedList<State>> vehicleToState = plan.getNextState();
        for (Map.Entry<Vehicle, LinkedList<State>> entry : vehicleToState.entrySet()) {
            System.out.println(entry.getKey().name() + " " + entry.getValue());
        }
    }
}
