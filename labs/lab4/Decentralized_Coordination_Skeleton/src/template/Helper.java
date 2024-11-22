package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.List;

public class Helper {
    public static boolean CanCarryTask(List<Vehicle> vehicles, Task task){
        for(var vehicle: vehicles){
            if (vehicle.capacity() >= task.weight){
                return true;
            }
        }
        return false;
    }
    public static double CalculateCostOfPlans(List<Plan> plans, List<Vehicle> vehicles){
        double cost = 0;
        for(int i = 0; i < plans.size(); i++){
            var plan = plans.get(i);
            var vehicle = vehicles.get(i);
            cost += plan.totalDistance() * vehicle.costPerKm();
        }
        return cost;
    }
}
