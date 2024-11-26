package template;

import logist.task.Task;

public class BidHelper {

    private AgentState ourAgent;
    private AgentState theirAgent;
    private Task task;

    public BidHelper(AgentState ourAgent, AgentState theirAgent, Task task) {
        this.ourAgent = ourAgent;
        this.theirAgent = theirAgent;
        this.task = task;
    }

    public long bid() {
        double ourMarginalCost = ourAgent.calculateMarginalCost(task, 0.4);
        double theirMarginalCost = theirAgent.calculateMarginalCost(task, 0.4);
        int totalTasksAuctioned = getTotalTasksAuctioned(ourAgent, theirAgent);

        System.out.println("\nBidding round " + totalTasksAuctioned + ":");
        System.out.println("Our agent cost: " + ourMarginalCost);;
        System.out.println("Their agent cost: " + theirMarginalCost + "\n");

        return Math.max(0, Math.round(ourMarginalCost));
    }

    private int getTotalTasksAuctioned(AgentState ourAgent, AgentState theirAgent) {
        return ourAgent.getWonTasks() + theirAgent.getWonTasks();
    }
}
