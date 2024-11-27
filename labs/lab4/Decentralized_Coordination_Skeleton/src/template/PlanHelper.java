package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlanHelper {
    private List<Vehicle> vehicles;
    private Random random;
    private long timeout;
    private double p;

    public PlanHelper(List<Vehicle> vehicles, long timeout){
        this.vehicles = vehicles;

        long seed = -9019554669489983951L;
        this.random = new Random(seed);

        this.timeout = timeout;
        this.p = 0.1;
    }


    public boolean canCarryTask(Task task){
        for(var vehicle: vehicles){
            if (vehicle.capacity() >= task.weight){
                return true;
            }
        }
        return false;
    }


    public Candidate computePlan (Candidate candidate, double timeout_frac) {
        long time_start = System.currentTimeMillis();

        Candidate newCandidate = new Candidate(candidate);

        boolean noTasks = true;

        for (var taskList : newCandidate.taskLists) {
            if (!taskList.isEmpty()) {
                noTasks = false;
                break;
            }
        }

        if (noTasks) return newCandidate;


        while(System.currentTimeMillis() - time_start < timeout_frac * timeout)	{
            Candidate oldCandidate = newCandidate;
            List<Candidate> candidateList = oldCandidate.ChooseNeighbours(random);
            newCandidate = LocalChoice(candidateList, oldCandidate);
        }

        return newCandidate;
    }


    public Candidate LocalChoice(List<Candidate> candidateList, Candidate candidate) {
        if (random.nextFloat() < p) {
            return candidate;
        } else {
            int bestCostIndex = 0;
            double bestCost = candidateList.get(bestCostIndex).cost;

            for (int candidateIndex = 1; candidateIndex < candidateList.size(); candidateIndex++) {
                if(candidateList.get(candidateIndex).cost < bestCost) {
                    bestCostIndex = candidateIndex;
                    bestCost = candidateList.get(bestCostIndex).cost;
                }
            }

            return candidateList.get(bestCostIndex);
        }
    }


    public List<Plan> planFromSolution(Candidate candidate) {
        List<Plan> planList = new ArrayList<>();

        for (int vehicleIndex = 0; vehicleIndex < candidate.vehicles.size(); vehicleIndex++) {

            Vehicle vehicle = candidate.vehicles.get(vehicleIndex);

            List<PD_Action> plan = candidate.plans.get(vehicleIndex);

            City currentCity = vehicle.getCurrentCity();
            Plan vechiclePlan = new Plan(currentCity);

            for (PD_Action act : plan) {

                Task task = act.task;
                City nextCity = act.is_pickup ? task.pickupCity : task.deliveryCity;

                for(City move_city : currentCity.pathTo(nextCity)) {
                    vechiclePlan.appendMove(move_city);
                }

                if (act.is_pickup) {
                    vechiclePlan.appendPickup(task);
                } else {
                    vechiclePlan.appendDelivery(task);
                }
                currentCity = nextCity;
            }
            planList.add(vechiclePlan);
        }
        return planList;
    }
}
