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

	private double p;

	private double futureDiscount = 0.15;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.taskDistribution = (DefaultTaskDistribution) distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();

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
		for (int i = 0; i < bids.length; i++) {
			if (i != agent.id()) {
				opponentBid = bids[i];
			}
		}

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
		int highestCapacity = 0;
		for(var vehicle : vehicles){
			if (vehicle.capacity() > highestCapacity){
				highestCapacity = vehicle.capacity();
			}
		}
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
		double theirLowestBid = theirMarginalCost+ futureDiscount * theirFutureSavings;

		System.out.println("\nBidding round " + (totalTasksAuctioned + 1) + ":");
		System.out.println("Our lowest bid: " + ourLowestBid);
		System.out.println("Their lowest bid: " + theirLowestBid);

		double bid = 0;

		if(ourLowestBid < theirLowestBid){
			long eps = Math.round(((theirLowestBid - ourLowestBid) / 5) * 4);
			bid = Math.max(0, Math.round(ourLowestBid) + eps);
		}
		else if(ourLowestBid > theirLowestBid){
			if (totalTasksAuctioned < 3){
				bid = Math.max(ourLowestBid - 500, theirLowestBid - 100);
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
