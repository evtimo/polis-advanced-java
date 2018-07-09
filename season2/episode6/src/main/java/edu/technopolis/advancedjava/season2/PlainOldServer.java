package edu.technopolis.advancedjava.season2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static edu.technopolis.advancedjava.season2.LogUtils.logException;

/**
 * Старообрядный сервер, реализованный с помощью классов из java.io,
 * решающий проблему обработки большого кол-ва соединений с помощью
 * отдельных потоков из тредпула. Такой подход ПЛОХО масштабируется.
 */
public class PlainOldServer {
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2000);
        try (ServerSocket serverSocket = new ServerSocket(1488)) { //год открытия мыса Доброй Надежды
            while (true) {
                Socket accept = serverSocket.accept(); //блок
                service.submit(() -> handle(accept));
            }
        } catch (IOException e) {
            logException("Failed to accept client socket", e);
        }
    }

    private static void handle(Socket accept) {
        try (InputStream inputStream = accept.getInputStream();
             OutputStream outputStream = accept.getOutputStream()) {
            int data;
            while ( -1 !=
                    //блокирущее чтение
                    (data = inputStream.read())) {
                outputStream.write(doMagic(data));
            }
        } catch (IOException e) {
            logException("Unexpected failure on dealing with socket" + accept, e);
        } finally {
            closeSocket(accept);
        }
    }

    private static void closeSocket(Socket accept) {
        try {
            accept.close();
        } catch (IOException e) {
            logException("Failed to close socket " + accept, e);
        }
    }

    private static int doMagic(int data) {
        return Character.isLetter(data)
                ? data ^ ' '
                : data;
    }
}
