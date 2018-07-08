package edu.technopolis;

import java.math.BigInteger;

public class MyBigInteger {

    private static int SIZE = 1; //Динамически расширяется при переполнении

    private long arr[] = new long[SIZE];

    MyBigInteger() {
        for (int i = 0; i < SIZE; i++) {
            arr[i] = 0;
        }
    }

    MyBigInteger(int x) {
        for (int i = 0; i < SIZE; i++) {
            arr[i] = 0;
        }

        int next = 0;
        while (x >= 1) {
            arr[next++] = x % Long.MAX_VALUE;
            x /= Long.MAX_VALUE;
        }
    }

    void assign(MyBigInteger x) {

        if (x.arr.length < this.arr.length) {
            long[] newArray = new long[this.arr.length];
            System.arraycopy(x.arr, 0, newArray, 0, SIZE - 1);
            x.arr = newArray;
        }
        for (int i = 0; i < SIZE; i++) {
            arr[i] = x.arr[i];
        }
    }

    //Вычисляет сумму двух чисел так, что каждый разряд одного LONG = один разряд суммы
    void summBinary(MyBigInteger y) {

        for (int i = 0; i < SIZE; i++) {
            this.arr[i] += y.arr[i];

            if (this.arr[i] < 0) {

                if (i == SIZE - 1)  //Если разрядов не хватает
                {
                    SIZE++;
                    long[] newArray = new long[SIZE];
                    long[] newArray2 = new long[SIZE];
                    System.arraycopy(this.arr, 0, newArray, 0, SIZE - 1);
                    this.arr = newArray;  //Расширяем массивы перед тем, как они переполнятся
                    System.arraycopy(y.arr, 0, newArray2, 0, SIZE - 1);
                    y.arr = newArray2;
                }

                //Система счисления по основанию = Long.MAX_VALUE
                this.arr[i + 1]++;
                this.arr[i] &= Long.MAX_VALUE;  //Операций побитового умножения, обнуляем разряд
                this.arr[i] += 1;
            }
        }
    }

    void printLongByLong() {
        System.out.println(arr[3] + " " + arr[2] + " " + arr[1] + " " + arr[0]);
    }

    public String toStringBinary() {
        StringBuilder str = new StringBuilder();
        for (long curr : arr) {
            str.append(Long.toBinaryString(curr));
        }
        return str.toString();
    }

    public String toString() {
        BigInteger l = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger pow = l;
        BigInteger res = BigInteger.valueOf(arr[0]);
        BigInteger tmp, item;  //item - один разряд, умноженный на основание системы счисления

        for (int i = 1; i < SIZE; i++) {
            tmp = BigInteger.valueOf(arr[i]);
            item = tmp.multiply(pow);
            res = res.add(item);
            pow = pow.multiply(l);
        }

        return res.toString();
    }

}
