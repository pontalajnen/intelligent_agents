package template;

//the list of imports
import java.awt.desktop.SystemSleepEvent;
import java.util.*;

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
	private double currentCost;
	private double potentialNewCost;
	private Candidate currentCandidate;
	private int wonTasks;
	private int totalTasksAuctioned;
	private EncodedCandidate currentEncodedCandidate;
	private EncodedCandidate potentialNextEncodedCandidate;


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
		this.currentCandidate = new Candidate(vehicles);
		this.totalTasksAuctioned = 0;
		this.wonTasks = 0;


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

		// Find opponent bid
		long opponentBid = 0;
		for (int i = 0; i < bids.length; i++) {
			if (i != agent.id()) {
				opponentBid = bids[i];
			}
		}

		if (winner == agent.id()){
			 currentCandidate.addTask(previous);
			 currentCandidate = potentialNextEncodedCandidate.getCandidate(currentCandidate);
			 currentEncodedCandidate = potentialNextEncodedCandidate;
			 currentCost = potentialNewCost;
			 System.out.println("Won the auction, Opponent bid " + opponentBid);
		}
		else {
			System.out.println("Lost the auction, Opponent bid " + opponentBid);
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		if (!Helper.CanCarryTask(vehicles, task)){
			return Long.MAX_VALUE;
		}
		var potentialNextCandidate = new Candidate(currentCandidate);
		potentialNextCandidate.addTask(task);
		potentialNextCandidate = internalPlan(vehicles, potentialNextCandidate);
		potentialNewCost = potentialNextCandidate.cost;

		var marginalCost = potentialNewCost - currentCost;

		System.out.println("\nCurrent cost: " + currentCandidate.cost);
		System.out.println("Potential new cost: " + potentialNewCost);
		System.out.println("Marginal cost: " + marginalCost + "\n");

		potentialNextEncodedCandidate = new EncodedCandidate(potentialNextCandidate);

		return Math.max(0, Math.round(marginalCost));
	}

	public Candidate internalPlan (List<Vehicle> vehicles, Candidate candidate) {
		long time_start = System.currentTimeMillis();

		// Begin SLS Algorithm

		// create initial solution
		Candidate A = new Candidate(candidate);

		boolean noTasks = true;

		for (var taskList : A.taskLists) {
			if (!taskList.isEmpty()) {
				noTasks = false;
				break;
			}
		}

		if (noTasks)
			return A;

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

		return A;
	}

	// Solve the optimization problem with the SLS algorithm
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		// We won 0 tasks
		if (currentEncodedCandidate == null){
			System.out.println("No encoding exists");
			return PlanFromSolution(internalPlan(vehicles, currentCandidate));
		}

		System.out.println("Decoding candidate...");
		var candidate = currentEncodedCandidate.getCandidate(currentCandidate);
		System.out.println("Constructing solution...");
		return PlanFromSolution(candidate);
	}



	// Local choice to choose the next solution from the neighbours and the current solution
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
