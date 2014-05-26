package com.easytimelog.timekeeper.util;

import android.util.Log;

import java.util.LinkedList;

// Why doesn't Android have a serial TaskQueue in the API?
// Note: This is a customized TaskQueue that will not stop until the queue is empty. Once empty, it
//       will not restart until start() is called again.
public class TaskQueue {
    private Thread   mThread;
    private Runnable mQueueRunner;
    private boolean  mRunning;

    private LinkedList<Runnable> mTasks;
    private LinkedList<Runnable> mAfterTasks;

    private class QueueRunner implements Runnable { public void run() { processQueue(); } }

    public TaskQueue() {
        mTasks = new LinkedList<Runnable>();
        mAfterTasks = new LinkedList<Runnable>();
        mQueueRunner = new QueueRunner();
    }

    public void start() {
        if(!mRunning) {
            mThread = new Thread(mQueueRunner);
            mThread.setDaemon(false);
            mRunning = true;
            mThread.start();
        }
    }

    public void stop() {
        mRunning = false;
        if(mTasks.isEmpty()) {
            addTask(new Runnable() { public void run() { /* do nothing */ } });
        }
    }

    public boolean isRunning() {
        return mRunning;
    }
    public void addTask(Runnable task) {
        synchronized (mTasks) {
            mTasks.addLast(task);
            mTasks.notify();
        }
    }

    private Runnable getNextTask() {
        synchronized(mTasks) {
            if(mTasks.isEmpty()) {
                try {
                    mTasks.wait();
                }catch(InterruptedException e) {
                    stop();
                }
            }
            return mTasks.removeFirst();
        }
    }

    private void processQueue() {
        while(mRunning || !mTasks.isEmpty()) {
            Runnable task = getNextTask();
            try {
                task.run();
                Thread.yield();
            }catch(Throwable t) {
                // Task Threw Exception
                Log.e("TaskQueue", "Task Threw Exception: ", t);
            }
        }
        processAfterTasksQueue();
    }

    public void addAfterProcessQueueTask(Runnable task) {
        mAfterTasks.add(task);
    }

    private void processAfterTasksQueue() {
        if(!mAfterTasks.isEmpty()) {
            for(Runnable task:mAfterTasks) {
                task.run();
            }
        }
    }
}
