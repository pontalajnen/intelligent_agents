package template;

/* import table */
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.VehicleImpl;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			Queue<State> queue = new LinkedList<>();

			var initialState = new State(
					null,
					vehicle.getCurrentCity(),
					0,
					TaskSet.noneOf(tasks),
					TaskSet.copyOf(tasks),
					TaskSet.noneOf(tasks),
					new ArrayList<>()
			);

			queue.add(initialState);

			State terminalState = null;

			while(!queue.isEmpty() || terminalState != null){

				var currentState = queue.poll();

				if (terminalState != null && currentState.getTotalCost() > terminalState.getTotalCost()){
					continue;
				}
				if(currentState.getRemainingTasks().isEmpty() && currentState.getCarryingTasks().isEmpty()){
					if(terminalState == null || terminalState.getTotalCost() > currentState.getTotalCost()){
						terminalState = currentState;
					}
					continue;
				}
				TaskSet deliverableTasks = TaskSet.copyOf(currentState.getCarryingTasks());

				List<Action> currentPlan = currentState.getActions();

				for(var task:currentState.getCarryingTasks()){
					if (task.deliveryCity != currentState.getLocation()){
						deliverableTasks.remove(task);
					}
				}
				for (var task:deliverableTasks){
					currentPlan.add(new Action.Delivery(task));
				}
				currentState.setCarryingTasks(TaskSet.intersectComplement(currentState.getCarryingTasks(), deliverableTasks));
				currentState.setDeliveredTasks(TaskSet.union(currentState.getDeliveredTasks(), deliverableTasks));

				TaskSet cityTasks = TaskSet.noneOf(currentState.getRemainingTasks());

				for(var task:currentState.getRemainingTasks()){
					if (task.pickupCity == currentState.getLocation()){
						cityTasks.add(task);
					}
				}
				var permutationCounter = Math.pow(2, cityTasks.size());

				for(var i = 0; i < permutationCounter; i++){
					var newPlan = new ArrayList<Action>(currentPlan);
					var permutation = Integer.toBinaryString(i);
					while (permutation.length() < cityTasks.size()){
						permutation = "0" + permutation;
					}

					TaskSet potentialPickup = TaskSet.noneOf(cityTasks);
					int j = 0;
					for(var task:cityTasks){
						if (permutation.charAt(j) == '1'){
							newPlan.add(new Action.Pickup(task));
							potentialPickup.add(task);
						}
						j++;
					}
					if (potentialPickup.weightSum() < vehicle.capacity() - currentState.getCarryingTasks().weightSum()){
						for (var neighbor: currentState.getLocation().neighbors()){
							var newerPlan = new ArrayList<>(newPlan);
							newerPlan.add(new Action.Move(neighbor));
							queue.add(new State(
									currentState,
									neighbor,
									currentState.getTotalCost() + currentState.getLocation().distanceUnitsTo(neighbor) * vehicle.costPerKm(),
									TaskSet.union(currentState.getCarryingTasks(), potentialPickup),
									TaskSet.intersectComplement(currentState.getRemainingTasks(), potentialPickup),
									currentState.getDeliveredTasks(),
									newerPlan));
						}
					}
				}
			}
			//Construct plan
			plan = new Plan(vehicle.getCurrentCity());
			for(var action: terminalState.getActions()) {
				plan.append(action);
			}
			System.out.println("Plan Competed!");
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
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

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
