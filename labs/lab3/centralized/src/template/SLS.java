package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.*;


public class SLS {
    private List<Vehicle> vehicles;
    private TaskSet tasks;

    public SLS(List<Vehicle> vehicles, TaskSet tasks) {
        this.vehicles = vehicles;
        this.tasks = tasks;
    }

    public CentralizedPlan createPlan(){
        var oldPlan = selectInitialSolution();
        System.out.println("Initial plan done");
        int numberOfIterations = 10000;
        int unImprovedThresh = 0;
        do{
            var neighbours = chooseNeighbours(oldPlan);
            if(neighbours.size() > 0){
                var newPlan = localChoice(oldPlan, neighbours);
                oldPlan = newPlan;
            }
            numberOfIterations--;

        }while(numberOfIterations > 0);

       return oldPlan;
    }

    private CentralizedPlan selectInitialSolution(){
        Vehicle largestVehicle = vehicles.get(0);

        for (var vehicle : vehicles){
            if(vehicle.capacity() > largestVehicle.capacity()){
                largestVehicle = vehicle;
            }
        }

        var taskList = new LinkedList<State>();
        boolean solutionExists = true;

        for (Task task : tasks){
            if (task.weight > largestVehicle.capacity()){
                solutionExists = false;
            }
            taskList.add(new State(task, true));
            taskList.add(new State(task, false));

        }

        var plan = new HashMap<Vehicle, LinkedList<State>>();
        plan.put(largestVehicle, taskList);


        if (solutionExists){
            return new CentralizedPlan(plan);
        }
        return null;
    }

    private ArrayList<CentralizedPlan> chooseNeighbours (CentralizedPlan oldPlan){
        var planSet = new ArrayList<CentralizedPlan>();
        var random = new Random();
        int selectVehicleIndex = random.nextInt(vehicles.size());
        var exchangeVehicle = vehicles.get(selectVehicleIndex);
        var oldPlanMap = oldPlan.getNextState();

        for (Vehicle currentVehicle : vehicles) {
            if(exchangeVehicle != currentVehicle){
                var newPlan = changeVehicle(exchangeVehicle, currentVehicle, oldPlan);
                if(!isConstraintViolated(newPlan)){
                    planSet.add(newPlan);
                }
            }

        }

        if(oldPlanMap.get(exchangeVehicle) != null){
            int numberOfStates = oldPlanMap.get(exchangeVehicle).size();

            if(numberOfStates >= 2){
                for(int i = 0; i < numberOfStates; ++i){
                    for(int j = 0; j < numberOfStates; ++j){
                        if (i != j){
                            var newPlan = changeOrderStates(oldPlan, exchangeVehicle, i, j);
                            if(!isConstraintViolated(newPlan)){
                                planSet.add(newPlan);
                            }
                        }

                    }
                }
            }
        }



        return planSet;
    }

    private CentralizedPlan changeVehicle(Vehicle vehicle1, Vehicle vehicle2, CentralizedPlan plan){
        var newPlan = new CentralizedPlan(plan.getNextState());

        var vehicleStates1 = newPlan.getNextState().get(vehicle1);
        var vehicleStates2 = newPlan.getNextState().get(vehicle2);

        if(vehicleStates1 == null && vehicleStates2 != null){
            vehicleStates1 = new LinkedList<State>();
            vehicleStates2.removeFirst();
        }
        if(vehicleStates1 != null && vehicleStates2 == null){
            vehicleStates2 = new LinkedList<State>();
            vehicleStates2.add(vehicleStates1.getFirst());
            vehicleStates1.removeFirst();
        }
        if(vehicleStates1 != null && vehicleStates2 != null){
            var startStateVehicle1 = vehicleStates1.getFirst();
            vehicleStates1.removeFirst();
            vehicleStates1.addFirst(vehicleStates2.getFirst());
            vehicleStates2.removeFirst();
            vehicleStates2.addFirst(startStateVehicle1);
        }

        var newPlanMap = newPlan.getNextState();
        newPlanMap.put(vehicle1, vehicleStates1);
        newPlanMap.put(vehicle2, vehicleStates2);

        return new CentralizedPlan(newPlanMap);
    }

    private CentralizedPlan changeOrderStates(CentralizedPlan plan, Vehicle vehicle, int stateIndex1, int stateIndex2){
        var stateList =  plan.getNextState().get(vehicle);
        var state1 = stateList.get(stateIndex1);
        var state2 = stateList.get(stateIndex2);
        stateList.set(stateIndex1, state2);
        stateList.set(stateIndex2, state1);

        var newPlan = new CentralizedPlan(plan.getNextState());
        var newPlanMap = newPlan.getNextState();
        newPlanMap.put(vehicle, stateList);
        newPlan.setNextState(newPlanMap);

        return newPlan;
    }

    private boolean isConstraintViolated(CentralizedPlan plan){
        boolean isViolated = false;

        var planMap = plan.getNextState();

        for(var entry : planMap.entrySet()){
            int temp_weight = 0;

            var currentTasks = new HashSet<Task>();

            for (var state : entry.getValue()){
                if (state.isPickup()){
                    currentTasks.add(state.getTask());
                    temp_weight += state.getTask().weight;
                }else{
                    if(currentTasks.contains(state.getTask())){
                        temp_weight -= state.getTask().weight;
                        currentTasks.remove(state.getTask());
                    }
                    else{
                        isViolated = true;
                    }
                }
                if(temp_weight > entry.getKey().capacity()){
                    isViolated = true;
                }
            }
        }

        return isViolated;

    }
    public int calculatePlanCost(CentralizedPlan plan) {
        int cost = 0;
        var planMap = plan.getNextState();
        for (var entry : planMap.entrySet()) {
            var v1 = entry.getKey();
            var stateList = entry.getValue();
            if (stateList != null && stateList.size() > 0) {
                Task startTask = stateList.get(0).getTask();
                cost += v1.homeCity().distanceTo(startTask.pickupCity) * v1.costPerKm();
                for (int i = 0; i < stateList.size() - 1; i++) {
                    State preState = stateList.get(i);
                    State postState = stateList.get(i + 1);
                    Task preTask = preState.getTask();
                    Task postTask = postState.getTask();
                    City preCity = preState.isPickup() ? preTask.pickupCity : preTask.deliveryCity;
                    City postCity = postState.isPickup() ? postTask.pickupCity : postTask.deliveryCity;
                    cost += preCity.distanceTo(postCity) * v1.costPerKm();
                }
            }
        }

        return cost;
    }

    private CentralizedPlan localChoice(CentralizedPlan oldPlan, ArrayList<CentralizedPlan> plans){
        CentralizedPlan minCostPlan = plans.get(0);

        for(var plan : plans){
            if(calculatePlanCost(plan) < calculatePlanCost(minCostPlan)){
                minCostPlan = plan;
            }
        }

        var random = new Random();
        int p = 35;
        int r = random.nextInt(100);

        return r < p ? oldPlan : (r < 2 * p ? minCostPlan : plans.get(random.nextInt(plans.size())));
    }
}
