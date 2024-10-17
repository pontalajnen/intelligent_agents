package template;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.HashMap;
import java.util.LinkedList;

public class CentralizedPlan {
    private HashMap<Vehicle, LinkedList<State>> nextState;

    public HashMap<Vehicle, LinkedList<State>> getNextState(){
        return this.nextState;
    }

    public CentralizedPlan(HashMap<Vehicle, LinkedList<State>> nextState) {
        this.nextState = nextState;
    }

    public void setNextState(HashMap<Vehicle, LinkedList<State>> nextState){
        this.nextState = nextState;
    }
    public void moveTask(Vehicle giver, Vehicle receiver, Task task){
        var giverStateList = nextState.get(giver);

        giverStateList.remove(new State(task, true));
        giverStateList.remove(new State(task, false));
        nextState.put(giver, giverStateList);

        var receiverStateList = nextState.get(receiver);
        if (receiverStateList == null){
            receiverStateList = new LinkedList<State>();
        }

        receiverStateList.addFirst(new State(task, true));
        receiverStateList.addLast(new State(task, false));

        nextState.put(receiver, receiverStateList);

    }
}
