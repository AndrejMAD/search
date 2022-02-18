package test;

public class TestTask {

    public static void main(String[] args) {
        Task task = new Task("1");

        task.fork();
        task.join();
    }
}
