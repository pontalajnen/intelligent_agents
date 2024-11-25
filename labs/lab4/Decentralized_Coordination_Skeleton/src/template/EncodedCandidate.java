package template;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.*;

public class EncodedCandidate {
    private List<Vehicle> vehicles;
    private List<List<EncodedAction>> plans;
    private List<List<Integer>> taskLists;
    private Double cost;

    public EncodedCandidate(Candidate candidate) {
        this.vehicles = candidate.vehicles;
        this.plans = new ArrayList<>(vehicles.size());
        this.taskLists = new ArrayList<>(vehicles.size());
        this.cost = candidate.cost;

        for (var plan : candidate.plans){
            List<EncodedAction> vehiclePlan = new ArrayList<>();
            for (var pdAction: plan){
                var encodedAction = new EncodedAction(pdAction);
                vehiclePlan.add(encodedAction);
            }
            this.plans.add(vehiclePlan);
        }

        for (var taskList: candidate.taskLists){
            List<Integer> vehicleTasks = new ArrayList<>();
            for (var task: taskList){
                vehicleTasks.add(task.id);
            }
            this.taskLists.add(vehicleTasks);
        }
    }

    public Candidate getCandidate(Candidate candidate){
        HashMap<Integer, Task> taskLookup = new HashMap<>();

        for (var taskList: candidate.taskLists){
           for (var task : taskList) {
               taskLookup.put(task.id, task);
           }
        }

        List<List<Task>> finalTaskLists = new ArrayList<>();
        List<List<PD_Action>> finalPlanLists = new ArrayList<>();

        for (var i = 0; i < vehicles.size(); i++){
            var vehicleTaskList = new ArrayList<Task>();
            var vehiclePlan = new ArrayList<PD_Action>();

            if(i < this.taskLists.size()){
                for (var taskId : this.taskLists.get(i)){
                    vehicleTaskList.add(taskLookup.get(taskId));
                }
            }

            if(i < this.plans.size()){
                for (var encodedPlan : this.plans.get(i)){
                    var pd_action = new PD_Action(
                            encodedPlan.getIsPickup(),
                            taskLookup.get(encodedPlan.getTaskId())
                    );
                    vehiclePlan.add(pd_action);
                }

            }
        }
        return new Candidate(
                this.vehicles,
                finalPlanLists,
                finalTaskLists,
                this.cost
        );
    }

}
