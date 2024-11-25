package template;

public class EncodedAction {
    private int taskId;
    private boolean isPickup;

    public EncodedAction(PD_Action pdAction) {
        this.taskId = pdAction.task.id;
        this.isPickup = pdAction.is_pickup;
    }
    public int getTaskId() {
        return taskId;
    }

    public boolean getIsPickup() {
        return isPickup;
    }
}
