package edu.technopolis.advancedjava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Deadlock {

    public static final int N = 5; //Кол-во философов = кол-во вилок
    public static final int SECONDS = 5;   //Время работы программы


    public static void main(String[] args) throws InterruptedException {
        List<Fork> forks = new ArrayList<>();
        AtomicInteger[] eaten = new AtomicInteger[N];
        AtomicInteger[] rejected = new AtomicInteger[N];
        for (int i = 0; i < N; i++) {
            forks.add(new Fork());
            eaten[i] = new AtomicInteger(0);
            rejected[i] = new AtomicInteger(0);
        }

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            Philosopher p = new Philosopher(forks.get(i), forks.get((i + 1) % N), eaten[i], rejected[i]);
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }

        Thread.sleep(1000L * SECONDS);

        threads.forEach(Thread::interrupt);


        printResult("Ресурса получено (съедено):", eaten);
        printResult("Отклонено попыток:", rejected);
    }

    public static void printResult(String s, AtomicInteger[] a) {
        int sum = 0;
        int[] answer = new int[a.length];
        for (int i = 0; i < answer.length; i++) {
            answer[i] = a[i].get();
            sum += answer[i];
        }
        String[] percentage = new String[answer.length];
        for (int i = 0; i < answer.length; i++) {
            double percents = 10000.0 * answer[i] / sum;
            long round = Math.round(percents);
            percentage[i] = "" + round / 100 + "."
                    + (round % 100 < 10 ? "0" : "") + round % 100 + "%";
        }

        System.out.println(s + " " + sum + ": " + Arrays.toString(answer));
        System.out.println("В процентах: " + Arrays.toString(percentage));
        System.out.println();
    }


}