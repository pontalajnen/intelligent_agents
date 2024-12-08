package template;

import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.simulation.VehicleImpl;
import logist.task.DefaultTaskDistribution;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AgentState {
    private int wonTasks;
    private EncodedCandidate currentEncodedCandidate;
    private EncodedCandidate potentialNextEncodedCandidate;
    private Candidate candidate;
    private PlanHelper planHelper;

    private double currentCost;
    private double potentialNextCost;

    private long totalBid;
    private long lowestBid;



    public AgentState(
            List<Vehicle> vehicleList,
            long timeout,
            boolean randomizeVehicles,
            List<Topology.City> cities,
            HashSet<Topology.City> occupiedCities) {
        var random = new Random();
        var randomizedVehicles = new ArrayList<Vehicle2>();
        if (cities.size() < 4){
            randomizeVehicles = false;
        }

        if (randomizeVehicles){
            for(var vehicle: vehicleList){
                var city = cities.get(random.nextInt(cities.size()));
                while (occupiedCities.contains(city)){
                    city = cities.get(random.nextInt(cities.size()));
                }
                var newVehicle = new Vehicle2(
                        city,
                        vehicle
                );
                randomizedVehicles.add(newVehicle);
                occupiedCities.add(city);
            }
        }
        var vehicleList2 = new ArrayList<Vehicle2>();
        for(int i = 0; i < vehicleList.size(); ++i){
            vehicleList2.add(
                    new Vehicle2(vehicleList.get(i).homeCity(), vehicleList.get(i))
            );
        }
        this.wonTasks = 0;
        this.candidate = new Candidate(randomizeVehicles ? randomizedVehicles : vehicleList2);
        this.planHelper = new PlanHelper(randomizeVehicles ? randomizedVehicles : vehicleList2, timeout);
        this.totalBid = 0L;
        this.lowestBid = 0;
    }

    public AgentState(AgentState agentState) {
        this.wonTasks = agentState.getWonTasks();
        this.currentEncodedCandidate = agentState.getCurrentEncodedCandidate();
        this.potentialNextEncodedCandidate = agentState.getPotentialNextEncodedCandidate();
        this.candidate = agentState.getCandidate();
        this.planHelper = agentState.getPlanHelper();
        this.currentCost = agentState.getCurrentCost();
        this.potentialNextCost = agentState.getPotentialNextCost();
        this.totalBid = agentState.getTotalBid();
        this.lowestBid = agentState.getLowestBid();
    }

    public long getLowestBid() {
        return lowestBid;
    }

    public long getProfit(){
        return getTotalBid() - Math.round(candidate.cost);
    }

    public double getCost(){
        return currentCost;
    }

    public long getTotalBid(){
        return totalBid;
    }

    public double getPotentialNextCost() {
        return potentialNextCost;
    }

    public double getCurrentCost() {
        return currentCost;
    }

    public PlanHelper getPlanHelper() {
        return planHelper;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public EncodedCandidate getPotentialNextEncodedCandidate() {
        return potentialNextEncodedCandidate;
    }

    public EncodedCandidate getCurrentEncodedCandidate() {
        return currentEncodedCandidate;
    }

    public int getWonTasks(){
        return wonTasks;
    }

    public void setLowestBid(long bid) {
        lowestBid = bid;
    }

    public void updateCandidate(Task task, Long bid){
        wonTasks++;
        candidate.addTask(task);
        candidate = potentialNextEncodedCandidate.getCandidate(candidate);
        currentEncodedCandidate = new EncodedCandidate(potentialNextEncodedCandidate);
        currentCost = potentialNextCost;
        totalBid += bid;
    }

    public double computeMarginalCostMultipleTasks(Task currentTask, List<Task> tasks, double timeout_frac){
        var futureCandidate = new Candidate(candidate);
        for(var task : tasks){
            futureCandidate.addTask(task);
        }
        futureCandidate = planHelper.computePlan(futureCandidate, timeout_frac * 3 / 4);
        futureCandidate.updateCost();
        double loseCost = futureCandidate.cost;
        futureCandidate.addTask(currentTask);
        futureCandidate = planHelper.computePlan(futureCandidate, timeout_frac * 1 / 4);
        futureCandidate.updateCost();

        return futureCandidate.cost - loseCost; // This will be negative
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

    @Override
    public String toString(){
        return "Cities " + candidate.vehicles.stream().map(v -> v.getCurrentCity().name).collect(Collectors.joining(", "));
    }





}
