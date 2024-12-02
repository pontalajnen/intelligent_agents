package template;

//the list of imports
import java.util.*;

import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.DefaultTaskDistribution;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.LogistSettings;
import java.io.File;
import logist.config.Parsers;

@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private DefaultTaskDistribution taskDistribution;
	private Agent agent;
	private List<Vehicle> vehicles;
	private long timeout_setup;

	private AgentState ourAgent;
	private AgentState theirAgent;
	private Map<City, Long> deliveryCities;
	private Map<City, Long> pickUpCities;

	private double p;

	private double futureDiscount = 0.1;
	private double cityAproximation = 0.1;

	private int highestCapacity;
	private int correctGuesses = 0;
	private int wrongGuesses = 0;
	private long cumulativeLoss = 0;
	private long cumulativeGain = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.taskDistribution = (DefaultTaskDistribution) distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.deliveryCities = new HashMap<>();
		this.pickUpCities = new HashMap<>();

		// Save time by calculating it here, maybe not much but
		for(var vehicle : vehicles){
			this.highestCapacity = Math.max(vehicle.capacity(), highestCapacity);
		}

		// Initiates a map with all the cities
		for (City city : topology.cities()) {
			this.deliveryCities.put(city, (long) 0);
			this.pickUpCities.put(city, (long) 0);
		}

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
		var timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN) - 200;	// We add a little safety margin

		this.ourAgent = new AgentState(vehicles, timeout_plan);
		this.theirAgent = new AgentState(vehicles, timeout_plan);
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		// We won the auction

		// Find opponent bid
		long opponentBid = 0;
		long ourBid = 0;
		for (int i = 0; i < bids.length; i++) {
			if (i != agent.id()) {
				opponentBid = bids[i];
			}
		}

		// Maybe it should be pickup, or a combination of them both
		long bidDifference = opponentBid - theirAgent.getLowestBid();

		boolean overEstimate = bidDifference < 0;
		long correction = deliveryCities.get(previous.deliveryCity) + pickUpCities.get(previous.pickupCity);
		// Could be zero in some edge cases but realistically won't happen
		boolean deliveryPredict = deliveryCities.get(previous.deliveryCity) != 0;
		boolean pickUpPredict = pickUpCities.get(previous.pickupCity) != 0;
		boolean rightCorrection = correction < 0;
		if (pickUpPredict || deliveryPredict) {
			if (overEstimate == rightCorrection) {
				correctGuesses++;
				cumulativeGain += Math.abs(correction);
				System.out.println("Correctly predicted: " + bidDifference);
				System.out.println("Correct guesses: " + correctGuesses);
				System.out.println("Cumulative gain: " + cumulativeGain);
			} else {
				wrongGuesses++;
				cumulativeLoss += Math.abs(correction);
				System.out.println("Wrongly predicted: " + bidDifference);
				System.out.println("Wrong guesses: " + wrongGuesses);
				System.out.println("Cumulative loss: " + cumulativeLoss);
			}
		} else {
			System.out.println("No data on opponent!");
		}

		// Protect against adversarial bids, should maybe be lower
		opponentBid = Math.min(opponentBid, 5000);
		deliveryCities.put(previous.deliveryCity, bidDifference);
		pickUpCities.put(previous.pickupCity, bidDifference);


		if (winner == agent.id()){
			 ourAgent.updateCandidate(previous, bids[agent.id()]);
			 System.out.println("Won the auction, Opponent bid " + opponentBid);
			 System.out.println("Current cost: " + ourAgent.getCost());
		}
		else {
			theirAgent.updateCandidate(previous, bids[agent.id()]);
			System.out.println("Lost the auction, Opponent bid " + opponentBid);
		}
		System.out.println("Our profit: " + ourAgent.getProfit());
		System.out.println("Their profit: " + theirAgent.getProfit());

	}

	
	@Override
	public Long askPrice(Task task) {
		if (task.weight > highestCapacity) return Long.MAX_VALUE;

		double ourMarginalCost = ourAgent.calculateMarginalCost(task, 0.25);
		double theirMarginalCost = theirAgent.calculateMarginalCost(task, 0.25);
		int totalTasksAuctioned = ourAgent.getWonTasks() + theirAgent.getWonTasks();

		double ourFutureSavings = BidHelper.computeFutureSavings(
				task, ourAgent, taskDistribution, 0.25
		);
		double theirFutureSavings = BidHelper.computeFutureSavings(
				task, theirAgent, taskDistribution, 0.25
		);

		double ourLowestBid = ourMarginalCost + futureDiscount * ourFutureSavings;
		double theirLowestBid = theirMarginalCost + futureDiscount * theirFutureSavings;
		// Add up the supposed precictions from earlier deliveries
		double learnedDiscount = (deliveryCities.get(task.deliveryCity) + pickUpCities.get(task.pickupCity));
		double theirLowestBidTemp = theirLowestBid + cityAproximation * learnedDiscount;
		theirLowestBid = theirLowestBidTemp;

		System.out.println("\nBidding round " + (totalTasksAuctioned + 1) + ":");
		System.out.println("Our lowest bid: " + ourLowestBid);
		System.out.println("Their lowest bid: " + theirLowestBid);
		System.out.println(
				"Their lowest bid with learning: " + theirLowestBidTemp
		);
		theirAgent.setLowestBid(Math.round(theirLowestBid));

		double bid;
		if(ourLowestBid < theirLowestBid){
			long eps = Math.round(((theirLowestBid - ourLowestBid) / 5) * 4);
			bid = Math.max(0, Math.round(ourLowestBid) + eps);
		}
		else if(ourLowestBid > theirLowestBid){
			if (ourAgent.getWonTasks() < 3){
				bid = Math.max(ourLowestBid - 300, theirLowestBid - 100);
			}
			else{
				bid = ourLowestBid;
			}
		}
		else{
			bid = ourLowestBid;
		}
		System.out.println("Our bid: " + bid);

		return Math.max(0, Math.round(bid));
	}


	// Solve the optimization problem with the SLS algorithm
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return ourAgent.getPlan();
	}
}
