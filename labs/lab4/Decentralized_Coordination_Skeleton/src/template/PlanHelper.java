package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

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

        Candidate A = new Candidate(candidate);

        boolean noTasks = true;

        for (var taskList : A.taskLists) {
            if (!taskList.isEmpty()) {
                noTasks = false;
                break;
            }
        }

        if (noTasks) return A;

        boolean timeout_reached = false;

        while(!timeout_reached)	{

            Candidate A_old = A;

            List<Candidate> N = A_old.ChooseNeighbours(random);

            A = LocalChoice(N, A_old);

            if( System.currentTimeMillis() - time_start > timeout_frac * timeout) {
                timeout_reached = true;
            }
        }

        return A;
    }
    public Candidate LocalChoice(List<Candidate> N, Candidate A) {
        if (random.nextFloat() < p) {	// Return A with probability p
            return A;
        }
        else {	// Return the best neighbour with probability 1-p

            int best_cost_index = 0; // index of the neighbour with best cost until now
            double best_cost = N.get(best_cost_index).cost; // cost of the neighbour with best cost until now

            for (int n_ind = 1; n_ind < N.size(); n_ind++ ) {
                // check if current alternative has lower cost than the current best
                if( N.get(n_ind).cost < best_cost )	{
                    // if so, update the best solution
                    best_cost_index = n_ind;
                    best_cost = N.get(best_cost_index).cost;
                }
            }

            // return the best solution
            return N.get(best_cost_index);
        }
    }

    // Build the plan for logist platform from the candidate solution
    public List<Plan> planFromSolution(Candidate A) {


        List<Plan> plan_list = new ArrayList<>();	// create empty list of plans

        // Build plan for each vehicle
        for (int vehicle_ind = 0; vehicle_ind < A.vehicles.size(); vehicle_ind++) {

            Vehicle v = A.vehicles.get(vehicle_ind);

            // get constructed plan of the vehicle
            List<PD_Action> plan = A.plans.get(vehicle_ind);

            // follow vehicle cities to construct plan
            Topology.City current_city = v.getCurrentCity();
            Plan v_plan = new Plan(current_city);

            // Append required primitive actions for each pickup/delivery action
            for (PD_Action act : plan) {

                Topology.City next_city;
                if(act.is_pickup) {
                    next_city = act.task.pickupCity;
                }
                else {
                    next_city = act.task.deliveryCity;
                }

                // Append move actions
                for(Topology.City move_city : current_city.pathTo(next_city)) {
                    v_plan.appendMove(move_city);
                }
                // Append pickup-delivery actions
                if (act.is_pickup) {
                    v_plan.appendPickup(act.task);
                } else {
                    v_plan.appendDelivery(act.task);
                }
                current_city = next_city;
            }

            // add plan to the list of plans
            plan_list.add(v_plan);
        }
        return plan_list;
    }
}
