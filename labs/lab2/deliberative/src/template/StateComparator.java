package template;

import java.util.Comparator;

class StateComparator implements Comparator<State> {
    public int compare(State s1, State s2) {
        return Double.compare(s1.getTotalCost(), s2.getTotalCost());
    }
}