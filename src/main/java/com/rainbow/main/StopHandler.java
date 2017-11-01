package com.rainbow.main;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Created by xuming on 2017/9/13.
 */
public class StopHandler implements SignalHandler {

    private Thread thread;
    private boolean isDone = false;

    public StopHandler(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void handle(Signal signal) {
        if (!isDone) {
            thread.interrupt();
            isDone = true;
        }
    }
}
