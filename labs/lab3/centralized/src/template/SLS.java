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
        var bestPlan = oldPlan;
        int numberOfIterations = 10000;
        int unImprovedThresh = 0;
        do{
            var neighbours = chooseNeighbours(oldPlan);
            if (numberOfIterations % 500 == 0) {
                System.out.println("----------------------------");
                System.out.println(numberOfIterations);
                System.out.println("Current cost: " + calculatePlanCost(oldPlan));
                System.out.println("Best current cost: " + calculatePlanCost(bestPlan));
                System.out.println("Number of neighbours: " + neighbours.size());
            }
            if(!neighbours.isEmpty()){
                oldPlan = localChoice(oldPlan, neighbours);
            }
            if(calculatePlanCost(oldPlan) < calculatePlanCost(bestPlan)){
                bestPlan = oldPlan;
            }
            numberOfIterations--;

        }while(numberOfIterations > 0);
        System.out.println("----------------------------");
        System.out.println("Final plan cost: " + calculatePlanCost(oldPlan));
        System.out.println("Best plan cost: " + calculatePlanCost(bestPlan));

        return bestPlan;

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
        CentralizedPlan oldPlanCopy = null;
        try{
            oldPlanCopy = (CentralizedPlan) oldPlan.clone();
        }catch (CloneNotSupportedException e){
            System.out.println("Error!");
        }

        var planSet = new ArrayList<CentralizedPlan>();
        var random = new Random();
        int selectVehicleIndex = random.nextInt(vehicles.size());
        var oldPlanMap = oldPlanCopy.getNextState();


        for (Vehicle exchangeVehicle : vehicles){
            for (Vehicle currentVehicle : vehicles) {
                if(exchangeVehicle != currentVehicle){
                    CentralizedPlan newPlanCopy = null;
                    try{
                        newPlanCopy = (CentralizedPlan) oldPlan.clone();
                    }catch (CloneNotSupportedException e){
                        System.out.println("Error!");
                    }
                    var newPlan = changeVehicle(exchangeVehicle, currentVehicle, newPlanCopy);

                    if(!isConstraintViolated(newPlan)){
                        planSet.add(newPlan);
                    }
                }
            }

        }

        var exchangeVehicle = vehicles.get(selectVehicleIndex);

        if(oldPlanMap.get(exchangeVehicle) != null){
            int numberOfStates = oldPlanMap.get(exchangeVehicle).size();

            if(numberOfStates > 2){
                for(int i = 0; i < numberOfStates; ++i){
                    for(int j = 0; j < numberOfStates; ++j){
                        if (i != j){
                            CentralizedPlan newPlanCopy = null;
                            try{
                                newPlanCopy = (CentralizedPlan) oldPlan.clone();
                            }catch (CloneNotSupportedException e){
                                System.out.println("Error!");
                            }
                            var newPlan = changeOrderStates(newPlanCopy, exchangeVehicle, i, j);
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

        if((vehicleStates1 == null || vehicleStates1.isEmpty()) && vehicleStates2 != null && !vehicleStates2.isEmpty()){
            newPlan.moveTask(vehicle2, vehicle1, vehicleStates2.getFirst().getTask());
        }
        else if(vehicleStates1 != null  && (vehicleStates2 == null || vehicleStates2.isEmpty()) && !vehicleStates1.isEmpty()){
            newPlan.moveTask(vehicle1, vehicle2, vehicleStates1.getFirst().getTask());
        }
        else if(vehicleStates1 != null && vehicleStates2 != null && !vehicleStates2.isEmpty() && !vehicleStates1.isEmpty()){
            var random = new Random();
            var p = random.nextInt(3);

            switch (p){
                case 0:
                    newPlan.moveTask(vehicle1, vehicle2, vehicleStates1.getFirst().getTask());
                    break;
                case 1:
                    newPlan.moveTask(vehicle2, vehicle1, vehicleStates2.getFirst().getTask());
                    break;
                case 2:
                    var firstTaskVehicle1 = vehicleStates1.getFirst().getTask();
                    newPlan.moveTask(vehicle2, vehicle1, vehicleStates2.getFirst().getTask());
                    newPlan.moveTask(vehicle1, vehicle2, firstTaskVehicle1);
                    break;
                default:
                    System.out.println("WTF!!!!");
                    break;
            }
        }

        return new CentralizedPlan(newPlan.getNextState());
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
                        break;
                    }
                }
                if(temp_weight > entry.getKey().capacity()){
                    isViolated = true;
                    break;
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
        int p = 37;
        int r = random.nextInt(100);

        return r < p ? oldPlan : (r < 2 * p ? minCostPlan : plans.get(random.nextInt(plans.size())));
    }
}
