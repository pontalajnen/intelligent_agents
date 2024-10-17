package template;

import logist.simulation.Vehicle;

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
}
