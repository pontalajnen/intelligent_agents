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
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof State)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        State c = (State) o;

        if(c.getTask().id != this.getTask().id){
            return false;
        }
        if(c.isPickup() != this.isPickup()){
            return false;
        }
        return true;

    }

    @Override
    public int hashCode() {
        return Objects.hash(
                pickup,
                task.id
        );
    }
    @Override
    public String toString() {
        return "State [isPickup=" + pickup + ", currentTask=" + task + "]";
    }
}
