package template;

//the list of imports
import java.util.*;

import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
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
	private TaskDistribution distribution;
	private Agent agent;
	private List<Vehicle> vehicles;
	private long timeout_setup;

	private AgentState ourAgent;
	private AgentState theirAgent;
	private long negativeBid;


	private double p;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.negativeBid = 3;

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
			 ourAgent.updateCandidate(previous);
			 System.out.println("Won the auction, Opponent bid " + opponentBid);
			 System.out.println("Current cost: " + ourAgent.getCost());
		}
		else {
			theirAgent.updateCandidate(previous);
			System.out.println("Lost the auction, Opponent bid " + opponentBid);
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		var bidHelper = new BidHelper(ourAgent, theirAgent, task);
		Long bid = bidHelper.bid(negativeBid);
		negativeBid = negativeBid != 0 ? negativeBid - 1 : negativeBid;
		return bid;
	}

	// Solve the optimization problem with the SLS algorithm
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return ourAgent.getPlan();

	}
}
