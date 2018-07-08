package edu.technopolis;

import java.util.InputMismatchException;

public class MyFibonacciAlgorithm implements FibonacciAlgorithm {
    @Override
    public String evaluate(int index) {
        if (index < 0) throw new InputMismatchException("Index can't be negative!");
        if (index == 0) return "0";
        if (index < 3) return "1";
        MyBigInteger x = new MyBigInteger(1);
        MyBigInteger y = new MyBigInteger();
        MyBigInteger tmp = new MyBigInteger();
        for (int i = 0; i < index - 1; i++) {
            tmp.assign(x);
            x.summBinary(y);
            y.assign(tmp);
        }
        return x.toString();
    }
}
