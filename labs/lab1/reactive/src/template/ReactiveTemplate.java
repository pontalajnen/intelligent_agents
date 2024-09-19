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

	private double[] V;
	private double[][][] Q;
	private Vehicle v;
	private double[] best_value;
	private City[] best_neighbor;


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


		// Computing Q-algorithm

		V = new double[topology.cities().size()]; // Best possible value for being in each city
		Q = new double [topology.cities().size()][topology.cities().size()][2]; //Q[city_a][city_b][action]
		v = agent.vehicles().get(0);

		// For convergence of Q-algorithm
		double diff = 0;
		double epsilon = 1e-5;

		do { // Continue until convergence
			// Compute Q-table each city pair city_1, city_2, two options
			// Reject [0] or accept [1] a task given the source and destination city

			for(City city1 : topology.cities()){
				for(City city2 : topology.cities()){

					// Update q-values for refusing a task
					if (city1.hasNeighbor(city2)) {

						var oldQ = Q[city1.id][city2.id][0];

						// Q-value based on cost and future reward
						double reward = -city1.distanceTo(city2) * v.costPerKm();
						double transition = 1 - td.probability(city1, city2);
						double value = V[city2.id];
						Q[city1.id][city2.id][0] =  reward + discount * transition * value;

						// Update value function
						V[city1.id] = Math.max(V[city1.id], Q[city1.id][city2.id][0]);

						// Update diff for convergence check
						diff = Math.max(diff, Math.abs(Q[city1.id][city2.id][0] - oldQ));
					}
					// Update q-values for accepting task
					if (td.weight(city1, city2) <= v.capacity() && city1.id != city2.id){
						var oldQ = Q[city1.id][city2.id][1];

						double reward = td.reward(city1, city2) - city1.distanceTo(city2) * v.costPerKm();
						double transition = td.probability(city1, city2);
						double value = V[city2.id];

						Q[city1.id][city2.id][1] =  reward + discount * transition * value;

						V[city1.id] = Math.max(V[city1.id], Q[city1.id][city2.id][1]);

						diff = Math.max(diff, Math.abs(Q[city1.id][city2.id][1] - oldQ));
					}

				}

			}
		} while (diff > epsilon);

		for (City city1 : topology.cities()){
			double best_qval = -1e10;
			City best_city = city1;

			for(City city2 : city1.neighbors()){
				if (best_qval < Q[city1.id][city2.id][0]){
					best_qval = Q[city1.id][city2.id][0];
					best_city = city2;
				}
			}

			best_value[city1.id] = best_qval;
			best_neighbor[city1.id] = best_city;
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = null;

		action = new Move(best_neighbor[vehicle.getCurrentCity().id]);

		if (availableTask != null && availableTask.weight <= vehicle.capacity()) {
			double pickup_value = Q[vehicle.getCurrentCity().id][availableTask.deliveryCity.id][1];

			if (pickup_value > best_value[vehicle.getCurrentCity().id]) {
				action = new Pickup(availableTask);
			}
		}

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;

		return action;
	}
}
