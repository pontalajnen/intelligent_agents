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
	private double cityApproximation = 0.1;

	private int highestCapacity;
	private int correctGuesses = 0;
	private int wrongGuesses = 0;
	private long cumulativeLoss = 0;
	private long cumulativeGain = 0;
	private double risk = 1;

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

		this.ourAgent = new AgentState(vehicles, timeout_plan, false, topology.cities());
		this.theirAgent = new AgentState(vehicles, timeout_plan, true, topology.cities());
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
		System.out.println("-------Result information------");

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
		opponentBid = Math.min(opponentBid, 2000);
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
		System.out.println("---------------------------------");

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

		double ourLowestBid = ourMarginalCost + (1 / (1 + ourAgent.getWonTasks())) * 0.2 * ourFutureSavings;
		double theirLowestBid = theirMarginalCost + (1 / (1 + theirAgent.getWonTasks())) * 0.2 * theirFutureSavings;
		// Add up the supposed precictions from earlier deliveries
		double learnedDiscount = (deliveryCities.get(task.deliveryCity) + pickUpCities.get(task.pickupCity));
		double theirLowestBidTemp = theirLowestBid + cityApproximation * learnedDiscount;
		theirLowestBid = theirLowestBidTemp;

		System.out.println("------Our askprice calc-----");
		System.out.println("\nBidding round " + (totalTasksAuctioned + 1) + ":");
		System.out.println("Our lowest bid: " + ourLowestBid);
		System.out.println("Their lowest bid: " + theirLowestBid);
		System.out.println(
				"Their lowest bid with learning: " + theirLowestBidTemp
		);
		theirAgent.setLowestBid(Math.round(theirLowestBid));

		long profitDifference = ourAgent.getProfit() - theirAgent.getProfit();
		boolean ahead = profitDifference > 0;
		boolean moreTasksWon = ourAgent.getWonTasks() > theirAgent.getWonTasks();
		boolean closeGame = ourAgent.getProfit() / ourAgent.getProfit() > 0.9 || ourAgent.getProfit() / theirAgent.getProfit() < 1.1;

		double earlyBid;
		double lateBid;

		double delta = 1 / (totalTasksAuctioned + 1);

		// Early game strategy
		if (totalTasksAuctioned == 0) {
			earlyBid = 0.8 * ourLowestBid;
			lateBid = 0.0;
		}
		else {
			if (ourLowestBid < theirLowestBid){
				earlyBid = ourLowestBid + (theirLowestBid - ourLowestBid) * 1 / 4;
				if (ourAgent.getWonTasks() / totalTasksAuctioned > 0.7){
					lateBid = ourMarginalCost + (theirMarginalCost - ourMarginalCost) * 4 / 5;
				}
				else{
					lateBid = ourMarginalCost + (theirMarginalCost - ourMarginalCost) * 2 / 5;
				}
			}
			else{
				earlyBid = ourLowestBid;
				lateBid = ((1 + ourAgent.getWonTasks()) / (1 + totalTasksAuctioned)) * ourMarginalCost;
			}
		}
		// if (ahead){
		// 	if (earlyGame){
		// 		System.out.println("MOOD: Agent is feeling optimistic");
		// 		if (ourLowestBid > theirLowestBid){
		// 			bid = 0.7 * Math.max(0.8 * ourLowestBid, 0.75 * theirLowestBid);
		// 		}
		// 		else {
		// 			bid = ourLowestBid;
		// 		}
		// 	}
		// 	else{
		// 		System.out.println("MOOD: Agent is feeling ecstatic!");
		// 		// Goal: Keep the lead and low risk appetite
		// 		if (ourLowestBid > theirLowestBid){
		// 			bid = Math.max(ourLowestBid - profitDifference * 0.1, 1/margin * ourLowestBid);
		// 		}
		// 		else {
		// 			bid = ourLowestBid + (theirLowestBid - ourLowestBid) * 1 / 2;
		// 		}
		// 	}

		// }
		// else {
		// 	if (earlyGame){
		// 		System.out.println("MOOD: Agent is locking in!");
		// 		if (ourLowestBid > theirLowestBid){
		// 			if (moreTasksWon){
		// 				bid = ourLowestBid;
		// 			}
		// 			else{

		// 			}
		// 			bid = 0.5 * Math.max(0.8 * ourLowestBid, 0.75 * theirLowestBid);
		// 		}
		// 		else {
		// 			bid = 0.7 * Math.max(0.8 * ourLowestBid, 0.75 * theirLowestBid);
		// 		}
		// 	}
		// 	else{
		// 		System.out.println("MOOD: Agent is depressed!");
		// 		if (ourLowestBid > theirLowestBid){
		// 			if (margin > 0.9){
		// 				bid = ourLowestBid;
		// 			}
		// 			else {
		// 				bid = ourLowestBid * 1.1;
		// 			}
		// 		}
		// 		else {
		// 			if (margin > 0.9){
		// 				bid = ourLowestBid + (theirLowestBid - ourLowestBid) * 4 / 6;
		// 			}
		// 			else{
		// 				bid = ourLowestBid + (theirLowestBid - ourLowestBid) * 5 / 6;
		// 			}

		// 		}
		// 	}
		// }

//		double bid = Math.max(0, Math.max(0.8 * ourMarginalCost, 0.75 * theirMarginalCost));
//		if (ourAgent.getWonTasks() < 4) {
//			bid *= 0.5;
//		}
//		if(ourLowestBid < theirLowestBid){
//			long eps = Math.round(((theirLowestBid - ourLowestBid) / 6) * 5);
//			bid = Math.max(0, Math.round(ourLowestBid) + eps);
//		}
//		else if(ourLowestBid > theirLowestBid){
//			if (ourAgent.getWonTasks() < 3){
//				bid = Math.max(0, 0.80 * theirLowestBid);
//			}
//			else{
//				if (profitDifference > 0) {
//					bid = Math.max(0, 0.95 * theirLowestBid);
//
//				} else {
//					bid = Math.max(0, 0.80 * theirLowestBid);
//
//				}
//			}
//		}
//		else{
//			bid = ourLowestBid - 100;
//		}

		double bid = delta * earlyBid + (1 - delta) * lateBid;
		System.out.println("Our bid: " + bid);
		System.out.println("----------------------------");

		return (long) Math.max(0, bid);
	}


	// Solve the optimization problem with the SLS algorithm
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return ourAgent.getPlan();
	}
}
