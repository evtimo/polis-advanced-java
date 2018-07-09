package edu.technopolis.advancedjava;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class Fork extends ReentrantLock {

}

public class Philosopher implements Runnable {

    private final Fork fork1, fork2;
    private final AtomicInteger eaten;
    private final AtomicInteger rejected;

    public Philosopher(Fork f1, Fork f2, AtomicInteger eaten, AtomicInteger rejected) {
        fork1 = f1;
        fork2 = f2;
        this.eaten = eaten;
        this.rejected = rejected;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!fork1.tryLock()) {
                    rejected.incrementAndGet();
                    continue;
                }
                try {
                    if (!fork2.tryLock()) {
                        rejected.incrementAndGet();
                        continue;
                    }
                    try {
                        eat();
                    } finally {
                        fork2.unlock();
                    }
                } finally {
                    fork1.unlock();
                }
                think();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    private void think() throws InterruptedException {
        Thread.sleep(1000);
    }

    private void eat() throws InterruptedException {
        eaten.incrementAndGet();
        Thread.sleep(1000);
    }
}