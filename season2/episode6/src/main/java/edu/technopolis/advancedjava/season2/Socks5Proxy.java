package edu.technopolis.advancedjava.season2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class Socks5Proxy {

    //localhost:8000

    class SocksClient {
        SocketChannel client, remote;
        boolean connected;
        long lastData = 0;

        SocksClient(SocketChannel c) throws IOException {
            client = c;
            client.configureBlocking(false);
            lastData = System.currentTimeMillis();
        }

        public void newRemoteData(Selector selector, SelectionKey sk, SocketChannel sch) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(1024);
            if (sch.read(buf) == -1)
                throw new IOException("Disconnected");
            lastData = System.currentTimeMillis();
            buf.flip();
            client.write(buf);
        }

        public void newClientData(Selector selector, SelectionKey sk) throws IOException {
            if (!connected) {
                ByteBuffer inbuf = ByteBuffer.allocate(512);
                if (client.read(inbuf) < 1)
                    return;
                inbuf.flip();
                int ver = inbuf.get();
                if (ver != 5) {
                    throw new IOException("Incorrect version" + ver);
                }
                int cmd = inbuf.get();
                if (cmd != 1) {
                    throw new IOException("Incorrect version");
                }

                final int port = inbuf.getShort();

                final byte ip[] = new byte[4];
                inbuf.get(ip);

                InetAddress remoteAddr = InetAddress.getByAddress(ip);

                while ((inbuf.get()) != 0) ;

                if (ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] != 0) {
                    String host = "";
                    byte b;
                    while ((b = inbuf.get()) != 0) {
                        host += b;
                    }
                    remoteAddr = InetAddress.getByName(host);
                    System.out.println(host + remoteAddr);
                }

                remote = SocketChannel.open(new InetSocketAddress(remoteAddr, port));

                ByteBuffer out = ByteBuffer.allocate(20);
                out.put((byte) 0);
                out.put((byte) (remote.isConnected() ? 0x5a : 0x5b));
                out.putShort((short) port);
                out.put(remoteAddr.getAddress());
                out.flip();
                client.write(out);

                if (!remote.isConnected())
                    throw new IOException("Connect failed");

                remote.configureBlocking(false);
                remote.register(selector, SelectionKey.OP_READ);

                connected = true;
            } else {
                newRemoteData(selector, sk, client);
            }
        }
    }

    static ArrayList<SocksClient> clients = new ArrayList<SocksClient>();

    public SocksClient addClient(SocketChannel s) {
        SocksClient cl;
        try {
            cl = new SocksClient(s);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        clients.add(cl);
        return cl;
    }

    public Socks5Proxy() throws IOException {
        ServerSocketChannel socks = ServerSocketChannel.open();
        socks.socket().bind(new InetSocketAddress(8000));
        socks.configureBlocking(false);
        Selector select = Selector.open();
        socks.register(select, SelectionKey.OP_ACCEPT);

        int lastClients = clients.size();
        while (true) {
            select.select(1000);

            Set keys = select.selectedKeys();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey k = (SelectionKey) iterator.next();

                if (!k.isValid())
                    continue;

                if (k.isAcceptable() && k.channel() == socks) {

                    SocketChannel csock = socks.accept();
                    if (csock == null)
                        continue;
                    addClient(csock);
                    csock.register(select, SelectionKey.OP_READ);
                } else if (k.isReadable()) {

                    for (int i = 0; i < clients.size(); i++) {
                        SocksClient cl = clients.get(i);
                        try {
                            if (k.channel() == cl.client)
                                cl.newClientData(select, k);
                            else if (k.channel() == cl.remote) {
                                cl.newRemoteData(select, k, cl.remote);
                            }
                        } catch (IOException e) {
                            cl.client.close();
                            if (cl.remote != null)
                                cl.remote.close();
                            k.cancel();
                            clients.remove(cl);
                        }
                    }
                }
            }

            for (int i = 0; i < clients.size(); i++) {
                SocksClient cl = clients.get(i);
                if ((System.currentTimeMillis() - cl.lastData) > 30000L) {
                    cl.client.close();
                    if (cl.remote != null)
                        cl.remote.close();
                    clients.remove(cl);
                }
            }
            if (clients.size() != lastClients) {
                System.out.println(clients.size());
                lastClients = clients.size();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Socks5Proxy();
    }
}