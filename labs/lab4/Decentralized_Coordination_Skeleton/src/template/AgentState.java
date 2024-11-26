package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.List;

public class AgentState {
    private int wonTasks;
    private EncodedCandidate currentEncodedCandidate;
    private EncodedCandidate potentialNextEncodedCandidate;
    private Candidate candidate;
    private PlanHelper planHelper;

    private double currentCost;
    private double potentialNextCost;

    public AgentState(List<Vehicle> vehicleList, long timeout) {
        this.wonTasks = 0;
        this.candidate = new Candidate(vehicleList);
        this.planHelper = new PlanHelper(vehicleList, timeout);

    }

    public double getCost(){
        return currentCost;
    }
    public int getWonTasks(){
        return wonTasks;
    }

    public void updateCandidate(Task task){
        wonTasks++;
        candidate.addTask(task);
        candidate = potentialNextEncodedCandidate.getCandidate(candidate);
        currentEncodedCandidate = new EncodedCandidate(potentialNextEncodedCandidate);
        currentCost = potentialNextCost;
    }

    public double calculateMarginalCost(Task task, double timeout_frac){
        calculateNextPotentialCandidate(task, timeout_frac);
        return potentialNextCost - currentCost;
    }

    public void calculateNextPotentialCandidate(Task task, double timeout_frac){
        var potentialNextCandidate = new Candidate(candidate);
        potentialNextCandidate.addTask(task);
        potentialNextCandidate = planHelper.computePlan(potentialNextCandidate, timeout_frac);
        potentialNextCandidate.updateCost();
        potentialNextCost = potentialNextCandidate.cost;
        potentialNextEncodedCandidate = new EncodedCandidate(potentialNextCandidate);
    }

    public List<Plan> getPlan(){
        if(currentEncodedCandidate == null){
            System.out.println("No encoding exists");
            return planHelper.planFromSolution(candidate);
        }
        System.out.println("Decoding candidate...");
        var finalCandidate = currentEncodedCandidate.getCandidate(candidate);
        System.out.println("Final cost: " + finalCandidate.cost);
        return planHelper.planFromSolution(finalCandidate);
    }





}
