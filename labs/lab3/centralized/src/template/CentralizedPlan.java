package template;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class CentralizedPlan implements  Cloneable {

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
        nextState.remove(giver);
        nextState.put(giver, giverStateList);

        var receiverStateList = nextState.get(receiver);
        if (receiverStateList == null){
            receiverStateList = new LinkedList<State>();
        }
        else {
            nextState.remove(receiver);
        }

        receiverStateList.addFirst(new State(task, false));
        receiverStateList.addFirst(new State(task, true));

        nextState.put(receiver, receiverStateList);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        CentralizedPlan o = null;
        try {
            o = (CentralizedPlan) super.clone();
        } catch (CloneNotSupportedException e) {
            System.out.println("Error");
        }

        o.nextState = new HashMap<Vehicle, LinkedList<State>>();
        for (Iterator<Vehicle> keyIt = nextState.keySet().iterator(); keyIt.hasNext();) {
            Vehicle key = keyIt.next();
            o.nextState.put(key, (LinkedList<State>) nextState.get(key).clone());
        }

        return o;
    }
}
