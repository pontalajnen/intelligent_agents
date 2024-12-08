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
import java.util.stream.Collectors;

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

	private double futureDiscount = 0.1;
	private double correctionFactor = 0.07;

	private int highestCapacity;

	private int totalTasksAuctioned = 0;

	private HashMap<City, Double> correction;

	private double aggro = 1;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
					  Agent agent) {

		this.topology = topology;
		this.taskDistribution = (DefaultTaskDistribution) distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();

		// Save time by calculating it here, maybe not much but
		for(var vehicle : vehicles){
			this.highestCapacity = Math.max(vehicle.capacity(), highestCapacity);
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

		System.out.println("Adding our agent");
		this.ourAgent = new AgentState(vehicles, timeout_plan, false, topology.cities(), new HashSet<>());
		var occupiedCities = new HashSet<City>();
		for (var vehicle : vehicles) {
			occupiedCities.add(vehicle.homeCity());
		}
		System.out.println("Adding their agent");
		this.theirAgent = new AgentState(vehicles, timeout_plan, true, topology.cities(), occupiedCities);

		System.out.println("Printing agents");
		System.out.println("Our agent: " + ourAgent.toString());
		System.out.println("Their agent: " + theirAgent.toString());

		correction = new HashMap<City, Double>(topology.cities().stream().map(c -> Map.entry(c, 0.0)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println("\n------- OUR AGENT AUCTION RESULT -------");
		totalTasksAuctioned++;

		long opponentBid = 0;
		long ourBid = 0;
		for (int i = 0; i < bids.length; i++) {
			if (i != agent.id()) {
				opponentBid = bids[i];
			}
		}

		double lowestDiff = Double.MAX_VALUE;
		City bestCity = null;


		if(totalTasksAuctioned == 1 && topology.cities().size() > 3){
			for(var city : topology.cities()){
				if (!ourAgent.getVehicles().stream().map(v -> v.getCurrentCity()).collect(Collectors.toList()).contains(city)){
					double distanceFromNewCity = city.distanceTo(previous.pickupCity) * theirAgent.getVehicles().get(0).getVehicle().costPerKm();
					double diff = Math.abs(opponentBid - distanceFromNewCity);
					if(diff < lowestDiff){
						lowestDiff = diff;
						bestCity = city;
					}
				}
			}
			System.out.println("New city for opponent: " + bestCity.name);
			theirAgent.updateCurrentLocation(bestCity);
		}

		if(opponentBid / theirAgent.getLowestBid() < 5){
			double diff = opponentBid - theirAgent.getLowestBid();
			correction.put(previous.pickupCity, diff);
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

		System.out.println("---------------------------------------\n");
	}

	@Override
	public Long askPrice(Task task) {
		System.out.println("\n------- OUR AGENT ASK PRICE -------");
		if (task.weight > highestCapacity) return Long.MAX_VALUE;

		double ourMarginalCost = ourAgent.calculateMarginalCost(task, 0.25);
		double theirMarginalCost = theirAgent.calculateMarginalCost(task, 0.25);

		double ourFutureSavings = BidHelper.computeFutureSavings(
				task, ourAgent, taskDistribution, 0.25
		);
		double theirFutureSavings = BidHelper.computeFutureSavings(
				task, theirAgent, taskDistribution, 0.25
		);

		double ourLowestBid = ourMarginalCost + futureDiscount * ourFutureSavings;
		double theirLowestBid = theirMarginalCost + futureDiscount * theirFutureSavings + 0 * correctionFactor * correction.get(task.pickupCity);

		System.out.println("\nBidding round " + (totalTasksAuctioned + 1) + ":");
		System.out.println("Our lowest bid: " + ourLowestBid);
		System.out.println("Their lowest bid: " + theirLowestBid);

		theirAgent.setLowestBid(Math.round(theirLowestBid));


		double earlyGameBid = 0;
		double lateGameBid = 0;

		double bid;

		if(ourLowestBid < theirLowestBid){
			long eps = Math.round((theirLowestBid - ourLowestBid) * 4 / 5);
			bid = Math.max(0, Math.round(ourLowestBid) + eps);
		}
		else {
			bid = theirLowestBid * 0.75 * aggro + ourLowestBid * (1 - aggro);
		}
		System.out.println("Our bid: " + bid);

		aggro -= 0.1;
		aggro = Math.max(aggro, 0);

		System.out.println("---------------------------------------\n");
		return Math.max(0, Math.round(Math.max(0.7 * ourMarginalCost, bid)));
	}


	// Solve the optimization problem with the SLS algorithm
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return ourAgent.getPlan();
	}
}