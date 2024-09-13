package template;

import logist.topology.Topology.City;

public class State {
    private City origin;
    private City destination;
    private boolean load;

    public City getOrigin() {
        return origin;
    }

    public City getDestination() {
        return destination;
    }

    public boolean isLoad() {
        return load;
    }

    public void setOrigin(City origin) {
        this.origin = origin;
    }

    public void setDestination(City destination) {
        this.destination = destination;
    }

    public void setLoad(boolean load) {
        this.load = load;
    }
}
