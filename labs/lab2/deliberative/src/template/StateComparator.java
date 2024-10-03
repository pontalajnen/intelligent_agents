package template;

import java.util.Comparator;

class StateComparator implements Comparator<State> {
    public int compare(State s1, State s2) {
        double dist1 = 999999999.9;
        double dist2 = 999999999.9;
        for (var task:s1.getRemainingTasks()){
            dist1 = Math.min(dist1, s1.getLocation().distanceTo(task.pickupCity) + task.pathLength());
        }
        for (var task:s1.getCarryingTasks()){
            dist1 = Math.min(dist1, s1.getLocation().distanceTo(task.deliveryCity) - task.pathLength());
        }
        for (var task:s2.getRemainingTasks()){
            dist2 = Math.min(dist2, s2.getLocation().distanceTo(task.pickupCity) + task.pathLength());
        }
        for (var task:s2.getCarryingTasks()){
            dist2 = Math.min(dist2, s2.getLocation().distanceTo(task.deliveryCity) - task.pathLength());
        }
        if(s1.getCarryingTasks().size() == 0 && s1.getRemainingTasks().size() == 0){
            dist1 = 0;
        }
        if(s2.getCarryingTasks().size() == 0 && s2.getRemainingTasks().size() == 0){
            dist2 = 0;
        }
        double temp1 = s1.getTotalCost() + dist1 * s1.getCostPerKilometer();
        double temp2 = s2.getTotalCost() + dist2 * s2.getCostPerKilometer();

        return Double.compare(temp1, temp2);
    }
}