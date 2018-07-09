package edu.technopolis.advancedjava.season2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import jdk.internal.jline.internal.Nullable;

/**
 * Пример неудачной реализации сервера на nio.
 * Несмотря на то, что один поток обрабатывает все запросы, данное решение ресурсоёмкое (100% CPU, большая часть sys
 * calls) и немасштабируемое (чем больше соединений, тем дольше задержка на обработку данных от конкретного соединения).
 * <br/>
 * Не стоит брать пример с данного сервера
 */
public class NonBlockingPollingServer_DontDoInThisWay {
    public static void main(String[] args) {
        HashMap<SocketChannel, ByteBuffer> map = new HashMap<>();
        try (ServerSocketChannel open = openAndBind()) {
            while (true) {
                @Nullable
                SocketChannel accept = open.accept(); //не блокируется, почти всегда null
                if (accept != null) {
                    accept.configureBlocking(false);
                    map.put(accept, ByteBuffer.allocateDirect(1024));
                }
                map.forEach((sc, byteBuffer) -> {
                    try {
                        int read = sc.read(byteBuffer);
                        if (read == -1) {
                            closeChannel(sc);
                        } else if (read > 0) {
                            byteBuffer.flip();
                            doMagic(byteBuffer);
                            while (byteBuffer.hasRemaining()) {
                                sc.write(byteBuffer);
                            }
                            byteBuffer.compact();
                        }
                    } catch (IOException e) {
                        LogUtils.logException("Error on processing connection from " + sc, e);
                        closeChannel(sc);
                    }
                });
                map.keySet().removeIf(sc -> !sc.isOpen());
            }
        } catch (IOException e) {
            LogUtils.logException("Unexpected error on handling incoming connection", e);
        }

    }

    private static ServerSocketChannel openAndBind() throws IOException {
        ServerSocketChannel open = ServerSocketChannel.open();
        open.bind(new InetSocketAddress(10101));
        open.configureBlocking(false);
        return open;
    }

    private static void closeChannel(SocketChannel sc) {
        try {
            sc.close();
        } catch (IOException ioe) {
            LogUtils.logException("Failure on closing channel " + sc, ioe);
        }
    }

    private static int doMagic(int data) {
        return Character.isLetter(data) ? data ^ ' ' : data;
    }

    private static void doMagic(ByteBuffer data) {
        for (int i = 0; i < data.limit(); i++) {
            data.put(i, (byte) doMagic(data.get(i)));
        }
    }
}
