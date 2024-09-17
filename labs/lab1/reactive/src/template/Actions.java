package template;

import logist.topology.Topology.City;

public class Actions {
    private City startCity;
    private City endCity;
    private boolean pickUp;

    public City getStartCity() {
       return startCity;
    }

    public City getEndCity() {
        return endCity;
    }

    public boolean isPickUp() {
        return pickUp;
    }

    public void setStartCity(City startCity) {
        this.startCity = startCity;
    }

    public void setEndCity(City endCity) {
        this.endCity = endCity;
    }

    public void setPickUp(boolean pickUp) {
        this.pickUp = pickUp;
    }
}
