package test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

public class Task extends RecursiveAction {

    private static final Set<String> values = new HashSet<>();

    private String value;

    public Task(String value) {
        this.value = value;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(value);
        values.add(value);

        List<Task> taskList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Task task = new Task(value + i);
            task.fork();
            taskList.add(task);
        }

        for (Task task : taskList) {
            task.join();
        }
    }
}
