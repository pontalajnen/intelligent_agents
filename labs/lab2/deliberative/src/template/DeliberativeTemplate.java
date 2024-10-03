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
		Plan plan = null;

		switch (algorithm) {
			case ASTAR:
				System.out.println("Running ASTAR...");
				plan = aStarPlan(vehicle, tasks);
				break;
			case BFS:
				System.out.println("Running BFS...");
				plan = bfsPlan(vehicle, tasks);
				break;
			default:
				System.out.println("Invalid Argument");
				break;
		}
		return plan;
	}

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks){
		Plan plan;
		int numberOfIterations = 0;

		var visited = new HashMap<Integer, Double>();

		Queue<State> queue = new LinkedList<>();

		var initialState = new State(
				null,
				vehicle.getCurrentCity(),
				0,
				TaskSet.noneOf(tasks),
				TaskSet.copyOf(tasks),
				TaskSet.noneOf(tasks),
				new ArrayList<>(),
				0
		);

		queue.add(initialState);

		State terminalState = null;

		while(!queue.isEmpty()){
			numberOfIterations++;
			var currentState = queue.poll();

			if(visited.containsKey(currentState.hashCode())){
				if (visited.get(currentState.hashCode()) > currentState.getTotalCost()){
					visited.put(currentState.hashCode(), currentState.getTotalCost());
				}
				else{
					continue;
				}
			}
			else{
				visited.put(currentState.hashCode(), currentState.getTotalCost());
			}

			if (terminalState != null && currentState.getTotalCost() > terminalState.getTotalCost()){
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

			if(currentState.getRemainingTasks().isEmpty() && currentState.getCarryingTasks().isEmpty()){
				if(terminalState == null || terminalState.getTotalCost() > currentState.getTotalCost()){
					terminalState = currentState;
				}
				continue;
			}

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
				if (potentialPickup.weightSum() <= vehicle.capacity() - currentState.getCarryingTasks().weightSum()){
					for (var neighbor: currentState.getLocation().neighbors()){
						var newerPlan = new ArrayList<>(newPlan);
						newerPlan.add(new Action.Move(neighbor));
						var newestState = new State(
								currentState,
								neighbor,
								currentState.getTotalCost() + currentState.getLocation().distanceTo(neighbor) * vehicle.costPerKm(),
								TaskSet.union(currentState.getCarryingTasks(), potentialPickup),
								TaskSet.intersectComplement(currentState.getRemainingTasks(), potentialPickup),
								currentState.getDeliveredTasks(),
								newerPlan,
								currentState.getTotalSteps() + 1);
						queue.add(newestState);
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
		System.out.println(plan);
		System.out.println(String.format("%s%d", "Number of iterations: ", numberOfIterations));
		return plan;
	}
	
	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		int numberOfIterations = 0;

		var visited = new HashMap<Integer, Double>();

		PriorityQueue<State> queue = new PriorityQueue<>(new StateComparator());

		var initialState = new State(
				null,
				vehicle.getCurrentCity(),
				0,
				TaskSet.noneOf(tasks),
				TaskSet.copyOf(tasks),
				TaskSet.noneOf(tasks),
				new ArrayList<>(),
				0
		);

		queue.add(initialState);

		State terminalState = null;

		while(!queue.isEmpty() && terminalState == null){
			numberOfIterations++;
			var currentState = queue.poll();

			if(visited.containsKey(currentState.hashCode())){
				if (visited.get(currentState.hashCode()) > currentState.getTotalCost()){
					visited.put(currentState.hashCode(), currentState.getTotalCost());
				}
				else{
					continue;
				}
			}
			else{
				visited.put(currentState.hashCode(), currentState.getTotalCost());
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

			if(currentState.getRemainingTasks().isEmpty() && currentState.getCarryingTasks().isEmpty()){
				terminalState = currentState;
				continue;
			}
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
				if (potentialPickup.weightSum() <= vehicle.capacity() - currentState.getCarryingTasks().weightSum()){
					for (var neighbor: currentState.getLocation().neighbors()){
						var newerPlan = new ArrayList<>(newPlan);
						newerPlan.add(new Action.Move(neighbor));
						var newestState = new State(
								currentState,
								neighbor,
								currentState.getTotalCost() + currentState.getLocation().distanceTo(neighbor) * vehicle.costPerKm(),
								TaskSet.union(currentState.getCarryingTasks(), potentialPickup),
								TaskSet.intersectComplement(currentState.getRemainingTasks(), potentialPickup),
								currentState.getDeliveredTasks(),
								newerPlan,
								currentState.getTotalSteps() + 1);

						queue.add(newestState);
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
		System.out.println(plan);
		System.out.println(String.format("%s%d", "Number of iterations: ", numberOfIterations));
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
