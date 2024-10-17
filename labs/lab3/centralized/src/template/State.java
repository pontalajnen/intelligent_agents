package template;

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

    public void setTask(Task task) {
        this.task = task;
    }

    public void setPickup(boolean pickup) {
        this.pickup = pickup;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                pickup,
                task
        );
    }
}
