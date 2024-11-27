package template;

import logist.task.DefaultTaskDistribution;
import logist.task.Task;

import java.util.ArrayList;

public class BidHelper {

    public static double computeFutureSavings(
            Task currentTask,
            AgentState agent,
            DefaultTaskDistribution taskDistribution,
            double timeout_frac
    ) {
        int taskHorizon = 3;
        int samples = 3;
        double futureMarginalCost = 0;

        for (int i = 0; i < samples; i++){
            var futureTasks = new ArrayList<Task>();
            for(int j = 0; j < taskHorizon; j++){
                var futureTask = taskDistribution.createTask();
                futureTasks.add(futureTask);
            }
            futureMarginalCost += agent.computeMarginalCostMultipleTasks(currentTask, futureTasks, timeout_frac / samples) / samples;
        }
        return futureMarginalCost;
    }
}
