package template;

import logist.task.Task;
import logist.task.TaskSet;

public class PseudoTaskSet {
    private TaskSet taskSet;

    public PseudoTaskSet(){
        Task[] emptyTaskArray = {};
        this.taskSet = TaskSet.create(emptyTaskArray);
    }
    public PseudoTaskSet(TaskSet taskSet){
        this.taskSet = taskSet;
    }

    public void add(Task task){
        this.taskSet.add(task);
    }
    public TaskSet getTaskSet(){
        return this.taskSet;
    }
}
