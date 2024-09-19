package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		double[] V = new double[topology.cities().size()];
		double[][][] Q = new double [topology.cities().size()][topology.cities().size()][2];
		Vehicle v = agent.vehicles().get(0);
		double diff = 1e10;
		double epsilon = 1e-5;

		while (diff > epsilon){
			for(City city1 : topology.cities()){
				for(City city2 : topology.cities()){
					if (city1.hasNeighbor(city2)) {
						var oldQ = Q[city1.id][city2.id][0];
						Q[city1.id][city2.id][0] = -city1.distanceTo(city2) * v.costPerKm() +
								discount * (1 - td.probability(city1, city2)) * V[city2.id];
						V[city1.id] = Math.max(V[city1.id], Q[city1.id][city2.id][0]);
						diff = Math.max(diff, Math.abs(Q[city1.id][city2.id][0] - oldQ));

					}

				}

			}
		}




	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;

		return action;
	}
}
