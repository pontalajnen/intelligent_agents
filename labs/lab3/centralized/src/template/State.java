package template;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.Objects;

public class State {
    private boolean pickup;
    private Task task;

    public State(Task task, boolean pickup) {
        this.pickup = pickup;
        this.task = task;
    }

    public boolean isPickup() {
        return pickup;
    }

    public Task getTask() {
        return task;
    }



    @Override
    public int hashCode() {
        return Objects.hash(
                pickup,
                task
        );
    }
    @Override
    public String toString() {
        return "State [isPickup=" + pickup + ", currentTask=" + task + "]";
    }
}
