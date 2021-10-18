package edu.ucsb.ece150.maskme;

import java.util.concurrent.Executor;

// A simple class to execute tasks on a new thread
// Source: https://developer.android.com/reference/java/util/concurrent/Executor
public class ThreadPerTaskExecutor implements Executor {
    public void execute(Runnable r) {
        new Thread(r).start();
    }
}
