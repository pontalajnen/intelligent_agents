package template;

import java.util.*;

import logist.Measures;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

// The Candidate class holds a conceptual candidate solution and methods associated with it.
public class Candidate {

	public Double cost; // cost of the plan
	public List<Vehicle> vehicles; // list of vehicles
	public List<List<PD_Action>> plans; // lists of plans for each vehicle
	public List<List<Task>> taskLists; // lists of tasks for each vehicle

	public Candidate(List<Vehicle> vehicles, List<List<PD_Action>> plans, List<List<Task>> taskLists, Double cost) {
		this.vehicles = vehicles;
		this.plans = plans;
		this.taskLists = taskLists;
		this.cost = cost;
	}

	public Candidate(List<Vehicle> vehicles) {
		// Initialise plans and tasks variables
		List<List<PD_Action>> plans = new ArrayList<>();
		List<List<Task>> taskLists = new ArrayList<>();
		// initialize plans and task list
		for (int i = 0; i < vehicles.size(); i++) {
			plans.add(new ArrayList<>());
			taskLists.add(new ArrayList<>());
		}

		this.vehicles = vehicles;
		this.plans = plans;
		this.taskLists = taskLists;
		this.cost = 0.0;
	}

	public Candidate(Candidate solution) {
		this.vehicles = solution.vehicles;
		this.cost = solution.cost;

		this.plans = new ArrayList<>();
		for (List<PD_Action> sublist : solution.plans) {
			plans.add(new ArrayList<>(sublist));
		}

		this.taskLists = new ArrayList<>();
		for (List<Task> sublist : solution.taskLists) {
			taskLists.add(new ArrayList<>(sublist));
		}
	}

	// MAIN OPERATIONS: Choose neighbours, select initial solution

	// Function that generates neighbours
	public List<Candidate> ChooseNeighbours(Random random) {
		List<Candidate> neighs = new ArrayList<>(); // List to hold generated neighbours
		int num_vehicles = vehicles.size();

		// 1 - GENERATE NEIGHBOURS BY CHANGING VEHICLES OF TASKS
		for (int vid_i = 0; vid_i < num_vehicles; vid_i++) {
			List<Task> vehicle_tasks = taskLists.get(vid_i); // Get tasks of the vehicle
			if (vehicle_tasks.size() == 0) {
				continue;
			}

			int task_id = 0;
			double task_weight = vehicle_tasks.get(task_id).weight; // Get task weight
			int vid_j = random.nextInt(num_vehicles);
			while (vid_i == vid_j || vehicles.get(vid_j).capacity() < task_weight) {
				vid_j = random.nextInt(num_vehicles);
			}

			neighs.add(ChangingVehicle(random, task_id, vid_i, vid_j));
		}

		// 2 - GENERATE NEIGHBOURS BY CHANGING TASK ORDERS
		for (int vid_i = 0; vid_i < num_vehicles; vid_i++) {
			List<Task> vehicle_tasks = taskLists.get(vid_i); // Get tasks of the vehicle
			if (vehicle_tasks.size() < 2) {
				continue;
			}

			int task_id = random.nextInt(vehicle_tasks.size());
			neighs.add(ChangingTaskOrder(random, task_id, vid_i));
		}

		return neighs;
	}

	// Create initial candidate solution: All tasks assigned to the largest vehicle
	public static Candidate SelectInitialSolution(Random random, List<Vehicle> vehicles, List<Task> tasks) {
		int num_vehicles = vehicles.size();
		List<List<PD_Action>> plans = new ArrayList<>();
		List<List<Task>> taskLists = new ArrayList<>();
		List<Task> allTasks = new ArrayList<>(tasks);

		for (int i = 0; i < num_vehicles; i++) {
			plans.add(new ArrayList<>());
			taskLists.add(new ArrayList<>());
		}

		double[] vehicle_capacities = new double[num_vehicles];
		int largest_vehicle = MaxIndex(vehicle_capacities);

		for (Task t : allTasks) {
			List<PD_Action> plan = plans.get(largest_vehicle);
			List<Task> tasks_vehicle = taskLists.get(largest_vehicle);
			plan.add(new PD_Action(true, t));
			plan.add(new PD_Action(false, t));
			tasks_vehicle.add(t);
		}

		double initial_cost = 0.0;
		for (int i = 0; i < vehicles.size(); i++) {
			initial_cost += ComputeCost(vehicles.get(i), plans.get(i));
		}

		return new Candidate(vehicles, plans, taskLists, initial_cost);
	}

	// HELPER FUNCTIONS

	// Helper function for calculating the maximum of an array
	public static int MaxIndex(double[] array) {
		int max_ind = 0;
		for (int index = 0; index < array.length; index++) {
			if (array[index] > array[max_ind]) {
				max_ind = index;
			}
		}
		return max_ind;
	}

	// Function to check weight constraint for a single plan
	public static boolean SatisfiesWeightConstraints(List<PD_Action> plan, int vehicle_capacity) {
		for (PD_Action act : plan) {
			if (act.is_pickup) {
				vehicle_capacity -= act.task.weight;
			} else {
				vehicle_capacity += act.task.weight;
			}
			if (vehicle_capacity < 0) {
				return false;
			}
		}
		return true;
	}

	// Function to compute the cost of individual vehicles
	private static double ComputeCost(Vehicle v, List<PD_Action> plan) {
		double cost = 0.0;
		City current_city = v.getCurrentCity();
		for (PD_Action act : plan) {
			if (act.is_pickup) {
				cost += Measures.unitsToKM(current_city.distanceUnitsTo(act.task.pickupCity)) * v.costPerKm();
				current_city = act.task.pickupCity;
			} else {
				cost += Measures.unitsToKM(current_city.distanceUnitsTo(act.task.deliveryCity)) * v.costPerKm();
				current_city = act.task.deliveryCity;
			}
		}
		return cost;
	}

	// VEHICLE AND TASK ORDER CHANGE OPERATORS

	// Function to change the vehicle of a given task
	public Candidate ChangingVehicle(Random random, int task_id, int vid_i, int vid_j) {
		Vehicle v_i = vehicles.get(vid_i);
		Vehicle v_j = vehicles.get(vid_j);

		List<Task> i_tasks_old = taskLists.get(vid_i);
		List<Task> j_tasks_old = taskLists.get(vid_j);
		Task t = i_tasks_old.get(task_id);

		List<Task> i_tasks_new = new ArrayList<>(i_tasks_old);
		i_tasks_new.remove(task_id);

		List<Task> j_tasks_new = new ArrayList<>(j_tasks_old);
		j_tasks_new.add(t);

		List<List<Task>> updated_taskLists = new ArrayList<>(taskLists);
		updated_taskLists.set(vid_i, i_tasks_new);
		updated_taskLists.set(vid_j, j_tasks_new);

		List<PD_Action> i_plan_old = plans.get(vid_i);
		List<PD_Action> j_plan_old = plans.get(vid_j);

		List<PD_Action> i_plan_new = new ArrayList<>(i_plan_old);
		for (int act_ind = 0; act_ind < i_plan_new.size(); ) {
			PD_Action act = i_plan_new.get(act_ind);
			if (act.task == t) {
				i_plan_new.remove(act_ind);
			} else {
				act_ind++;
			}
		}

		List<PD_Action> j_plan_new = new ArrayList<>(j_plan_old);
		j_plan_new.add(0, new PD_Action(false, t));
		j_plan_new.add(0, new PD_Action(true, t));

		List<List<PD_Action>> updated_plans = new ArrayList<>(plans);
		updated_plans.set(vid_i, i_plan_new);
		updated_plans.set(vid_j, j_plan_new);

		double i_cost_old = ComputeCost(v_i, i_plan_old);
		double j_cost_old = ComputeCost(v_j, j_plan_old);
		double i_cost_new = ComputeCost(v_i, i_plan_new);
		double j_cost_new = ComputeCost(v_j, j_plan_new);
		double updated_cost = this.cost - i_cost_old + i_cost_new - j_cost_old + j_cost_new;

		return new Candidate(vehicles, updated_plans, updated_taskLists, updated_cost);
	}

	// Randomly change the place of pickup and delivery actions of one of the tasks in a given vehicle, considering the constraints
	public Candidate ChangingTaskOrder(Random random, int task_id, int vid_i) {
		Vehicle v_i = vehicles.get(vid_i);
		List<Task> vehicle_tasks = taskLists.get(vid_i);
		Task t = vehicle_tasks.get(task_id);

		List<PD_Action> i_plan_old = plans.get(vid_i);
		List<PD_Action> i_plan_new = new ArrayList<>(i_plan_old);
		for (int act_ind = 0; act_ind < i_plan_new.size(); ) {
			PD_Action act = i_plan_new.get(act_ind);
			if (act.task == t) {
				i_plan_new.remove(act_ind);
			} else {
				act_ind++;
			}
		}

		int vehicle_capacity = v_i.capacity();
		int pickup_location = 0;
		List<PD_Action> candidate_plan_pickup = new ArrayList<>(i_plan_new);
		boolean done = false;
		while (!done) {
			pickup_location = random.nextInt(i_plan_new.size());
			candidate_plan_pickup = new ArrayList<>(i_plan_new);
			candidate_plan_pickup.add(pickup_location, new PD_Action(true, t));
			if (SatisfiesWeightConstraints(candidate_plan_pickup, vehicle_capacity)) {
				done = true;
			}
		}

		List<PD_Action> candidate_plan_delivery = new ArrayList<>(candidate_plan_pickup);
		done = false;
		while (!done) {
			int delivery_location_offset = random.nextInt(i_plan_new.size() - pickup_location);
			int delivery_location = pickup_location + 1 + delivery_location_offset;
			candidate_plan_delivery = new ArrayList<>(candidate_plan_pickup);
			candidate_plan_delivery.add(delivery_location, new PD_Action(false, t));
			if (SatisfiesWeightConstraints(candidate_plan_delivery, vehicle_capacity)) {
				done = true;
			}
		}

		i_plan_new = new ArrayList<>(candidate_plan_delivery);
		List<List<PD_Action>> updated_plans = new ArrayList<>(plans);
		updated_plans.set(vid_i, i_plan_new);

		double i_cost_old = ComputeCost(v_i, i_plan_old);
		double i_cost_new = ComputeCost(v_i, i_plan_new);
		double updated_cost = this.cost - i_cost_old + i_cost_new;

		return new Candidate(vehicles, updated_plans, taskLists, updated_cost);
	}

	public void addTask(Task t) {
		double[] vehicle_capacities = new double[vehicles.size()];
		int largest_vehicle = MaxIndex(vehicle_capacities);

		this.plans.get(largest_vehicle).add(new PD_Action(true, t));
		this.plans.get(largest_vehicle).add(new PD_Action(false, t));
		this.taskLists.get(largest_vehicle).add(t);

		double new_cost = 0.0;
		for (int i = 0; i < vehicles.size(); i++) {
			new_cost += ComputeCost(vehicles.get(i), plans.get(i));
		}

		this.cost = new_cost;
	}
	public void updateCost(){
		Double newCost = 0.0;
		for(var i = 0; i < vehicles.size(); i++){
			newCost += ComputeCost(vehicles.get(i), plans.get(i));
		}
		this.cost = newCost;
	}
}