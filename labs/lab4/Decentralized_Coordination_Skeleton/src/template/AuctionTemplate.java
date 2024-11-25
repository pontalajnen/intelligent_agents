package template;

//the list of imports
import java.io.Console;
import java.util.*;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.LogistSettings;
import java.io.File;
import logist.config.Parsers;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private List<Vehicle> vehicles;
	private long timeout_setup;
	private long timeout_plan;
	private List<Task> wonTasks;
	private double currentCost;
	private double potentialNewCost;
	private Candidate currentPlan;
	private Candidate potentialNewPlan;

	private double p;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {


		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.p = 0.2;
		this.currentCost = 0;
		this.potentialNewCost = 0;
		this.wonTasks = new ArrayList<>();
		this.currentPlan = new Candidate(vehicles);

		long seed = -9019554669489983951L;
		this.random = new Random(seed);

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);

		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN) - 200;	// We add a little safety margin
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		// We won the auction
		if (winner == agent.id()){
			 wonTasks.add(previous);
			 currentCost = potentialNewCost;
			 currentPlan = potentialNewPlan;
			 System.out.println("Our agent won the auction");
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		if (!Helper.CanCarryTask(vehicles, task)){
			return Long.MAX_VALUE;
		}

		var tempTasks = new ArrayList<>(wonTasks);
		tempTasks.add(task);
		potentialNewPlan = new Candidate(vehicles, currentPlan.plans, currentPlan.taskLists, currentPlan.cost);

		Vehicle highestCapacityVehicle = currentPlan.vehicles.get(0);
		int index = 0;
		for (Vehicle vehicle : currentPlan.vehicles) {
			if (vehicle.capacity() > highestCapacityVehicle.capacity()) {
				highestCapacityVehicle = vehicle;
				index = currentPlan.vehicles.indexOf(vehicle);
			}
		}

		potentialNewPlan.plans.get(index).add(new PD_Action(true, task));
		potentialNewPlan.plans.get(index).add(new PD_Action(false, task));

		potentialNewPlan.taskLists.get(index).add(task);

		var plan = internalPlan(vehicles, tempTasks, potentialNewPlan);
		var cost = Helper.CalculateCostOfPlans(plan, vehicles);
		var marginalCost = cost - currentCost;
		potentialNewCost = cost;
		System.out.println("Our agent marginal cost: " + marginalCost);
		return Math.round(marginalCost);
	}

	public List<Plan> internalPlan (List<Vehicle> vehicles, List<Task> tasks, Candidate currentPlan) {
		System.out.println("Building internal plan...");

		long time_start = System.currentTimeMillis();

		// Begin SLS Algorithm


		// create initial solution
		Candidate A = Candidate.SelectInitialSolution(random, vehicles, tasks);

		// Optimization loop - repeat until timeout
		boolean timeout_reached = false;

		while(!timeout_reached)	{

			// record old solution
			Candidate A_old = A;

			// generate neighbours
			List<Candidate> N = A_old.ChooseNeighbours(random);

			// Get the soluti.getTaskSet()on for the next iteration
			A = LocalChoice(N, A_old);

			// Check timeout condition
			if( System.currentTimeMillis() - time_start > timeout_plan ) {
				timeout_reached = true;
			}
		}

		// End SLS Algorithm

		// Build plans for vehicles from the found solution
		List<Plan> plan = PlanFromSolution(A);

		// Informative outputs
		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		double cost_plan  = A.cost;

		System.out.println("The plan was generated in " + duration + " ms with a cost of " + A.cost);

		return plan;
	}

	// Solve the optimization problem with the SLS algorithm
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		System.out.println("Building plan...");

		long time_start = System.currentTimeMillis();

		// Begin SLS Algorithm

		var taskList = new ArrayList<>(tasks);

		// create initial solution
		Candidate A = Candidate.SelectInitialSolution(random, vehicles, taskList);

		// Optimization loop - repeat until timeout
		boolean timeout_reached = false;

		while(!timeout_reached)	{

			// record old solution
			Candidate A_old = A;

			// generate neighbours
			List<Candidate> N = A_old.ChooseNeighbours(random);

			// Get the soluti.getTaskSet()on for the next iteration
			A = LocalChoice(N, A_old);

			// Check timeout condition
			if( System.currentTimeMillis() - time_start > timeout_plan ) {
				timeout_reached = true;
			}
		}

		// End SLS Algorithm

		// Build plans for vehicles from the found solution
		List<Plan> plan = PlanFromSolution(A);

		// Informative outputs
		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		double cost_plan  = A.cost;

		System.out.println("The plan was generated in " + duration + " ms with a cost of " + A.cost);

		return plan;
	}


	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	// Local choice to choose the next solution from the neighbours and the current solution
	public Candidate LocalChoice(List<Candidate> N, Candidate A) {
		if (random.nextFloat() < p) {	// Return A with probability p
			return A;
		}
		else {	// Return the best neightbour with probability 1-p

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
	public List<Plan> PlanFromSolution(Candidate A) {

		// System.out.println("Constructing plan from solution...");

		List<Plan> plan_list = new ArrayList<>();	// create empty list of plans

		// Build plan for each vehicle
		for (int vehicle_ind = 0; vehicle_ind < A.vehicles.size(); vehicle_ind++) {

			Vehicle v = A.vehicles.get(vehicle_ind);

			// get constructed plan of the vehicle
			List<PD_Action> plan = A.plans.get(vehicle_ind);

			// follow vehicle cities to construct plan
			City current_city = v.getCurrentCity();
			Plan v_plan = new Plan(current_city);

			// Append required primitive actions for each pickup/delivery action
			for (PD_Action act : plan) {

				City next_city;
				if(act.is_pickup) {
					next_city = act.task.pickupCity;
				}
				else {
					next_city = act.task.deliveryCity;
				}

				// Append move actions
				for(City move_city : current_city.pathTo(next_city)) {
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
