package edu.technopolis.advancedjava.season2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.istack.internal.NotNull;

/**
 * Сервер, построенный на API java.nio.* . Работает единственный поток,
 * обрабатывающий события, полученные из селектора.
 * Нельзя блокировать или нагружать долгоиграющими действиями данный поток, потому что это
 * замедлит обработку соединений.
 */
public class NewServer {
    public static void main(String[] args) {
        Map<SocketChannel, ByteBuffer> map = new HashMap<>();
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()){
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(10001));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select(); //блокирующий вызов
                @NotNull
                Set<SelectionKey> keys = selector.selectedKeys();
                if (keys.isEmpty()) {
                    continue;
                }
                //при обработке ключей из множества selected, необходимо обязательно очищать множество.
                //иначе те же ключи могут быть обработаны снова
                keys.removeIf(key -> {
                    if (!key.isValid()) {
                        return true;
                    }
                    if (key.isAcceptable()) {
                        return accept(map, key);
                    }
                    if (key.isReadable()) {
                        //Внимание!!!
                        //Важно, чтобы при обработке не было долгоиграющих (например, блокирующих операций),
                        //поскольку текущий поток занимается диспетчеризацией каналов и должен быть всегда доступен
                        return read(map, key);
                    }
                    if (key.isWritable()) {
                        //Внимание!!!
                        //Важно, чтобы при обработке не было долгоиграющих (например, блокирующих операций),
                        //поскольку текущий поток занимается диспетчеризацией каналов и должен быть всегда доступен
                        return write(map, key);
                    }
                    return true;
                });
                //удаление закрытых каналов из списка обрабатываемых
                map.keySet().removeIf(channel -> !channel.isOpen());
            }

        } catch (IOException e) {
            LogUtils.logException("Unexpected error on processing incoming connection", e);
        }
    }

    private static boolean write(Map<SocketChannel, ByteBuffer> map, SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = map.get(channel);
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.compact();
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            closeChannel(channel);
            e.printStackTrace();
        }
        return true;
    }

    private static boolean read(Map<SocketChannel, ByteBuffer> map, SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        if (handleSocketChannel(channel, map.get(channel))) {
            key.interestOps(SelectionKey.OP_WRITE);
        }
        return true;
    }

    private static boolean accept(Map<SocketChannel, ByteBuffer> map, SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = null;
        try {
            channel = serverChannel.accept(); //non-blocking call
            channel.configureBlocking(false);
            channel.register(key.selector(), SelectionKey.OP_READ);
            map.put(channel, ByteBuffer.allocateDirect(80));
        } catch (IOException e) {
            LogUtils.logException("Failed to process channel " + channel, e);
            if (channel != null) {
                closeChannel(channel);
            }
        }
        return true;
    }

    private static boolean handleSocketChannel(SocketChannel channel, ByteBuffer bb) {
        try {
            int bytesRead = channel.read(bb);
            if (bytesRead == 0) {
                return false;
            }
            if (bytesRead == -1) {
                closeChannel(channel);
                return false;
            }
            bb.flip();
            doMagic(bb);
            return true;
        } catch (IOException e) {
            LogUtils.logException("Failed to read data from channel " + channel, e);
            closeChannel(channel);
            return false;
        }
    }

    private static void doMagic(ByteBuffer bb) {
        for (int index = bb.position(); index < bb.limit(); index++) {
            bb.put(index, (byte) doMagic(bb.get(index)));
        }
    }

    private static void closeChannel(SocketChannel accept) {
        try {
            accept.close();
        } catch (IOException e) {
            System.err.println("Failed to close channel " + accept);
        }
    }

    private static int doMagic(int data) {
        return Character.isLetter(data)
                ? data ^ ' '
                : data;
    }
}
