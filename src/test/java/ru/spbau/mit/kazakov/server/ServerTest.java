package ru.spbau.mit.kazakov.server;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.spbau.mit.kazakov.Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class ServerTest {
    private static final int BLOCKING_SERVER_PORT = 5555;
    private static final int NONBLOCKING_SERVER_PORT = 6666;
    private static final int SIMPLE_SERVER_PORT = 7777;
    private static final String HOST = "localhost";

    @BeforeClass
    public static void startServers() throws IOException, InterruptedException {
        BlockingServer blocking = new BlockingServer(BLOCKING_SERVER_PORT);
        NonblockingServer nonblocking = new NonblockingServer(NONBLOCKING_SERVER_PORT);
        SimpleServer simple = new SimpleServer(SIMPLE_SERVER_PORT);
        Thread t1 = new Thread(blocking::start);
        Thread t2 = new Thread(nonblocking::start);
        Thread t3 = new Thread(simple::start);
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
    }

    @Test
    public void testSimpleServerSingleClient() throws IOException {
        testSingleClient(SIMPLE_SERVER_PORT);
    }

    @Test
    public void testBlockingServerSingleClient() throws IOException {
        testSingleClient(BLOCKING_SERVER_PORT);
    }

    @Test
    public void testNonblockingServerSingleClient() throws IOException {
        testSingleClient(NONBLOCKING_SERVER_PORT);
    }

    @Test
    public void testSimpleServerMultipleClient() throws IOException, InterruptedException {
        testMultipleClient(SIMPLE_SERVER_PORT);
    }

    @Test
    public void testBlockingServerMultipleClient() throws IOException, InterruptedException {
        testMultipleClient(BLOCKING_SERVER_PORT);
    }

    @Test
    public void testNonblockingServerMultipleClient() throws IOException, InterruptedException {
        testMultipleClient(NONBLOCKING_SERVER_PORT);
    }

    private void testSingleClient(int port) throws IOException {
        Client client = new Client(HOST, port);
        assertArrayEquals(new int[]{-1, 3, 6}, client.sort(new int[]{3, -1, 6}));
        assertArrayEquals(new int[]{-1, 0, 41, 756, 3432}, client.sort(new int[]{41, 756, -1, 3432, 0}));
        assertArrayEquals(new int[]{1, 2}, client.sort(new int[]{1, 2}));
    }

    private void testMultipleClient(int port) throws IOException, InterruptedException {
        Runnable clientRequest = () -> {
            try {
                Client client = new Client(HOST, port);
                assertArrayEquals(new int[]{-232, -1, 0, 3, 6}, client.sort(new int[]{3, -1, 6, 0, -232}));
                assertArrayEquals(new int[]{-1, 0, 41, 756, 3432}, client.sort(new int[]{41, 756, -1, 3432, 0}));
                assertArrayEquals(new int[]{1, 2}, client.sort(new int[]{1, 2}));
            } catch (IOException ignored) {
                //nothing to do
            }

        };

        List<Thread> clients = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            clients.add(new Thread(clientRequest));
        }
        for(Thread clientThread : clients) {
            clientThread.start();
        }
        for(Thread clientThread : clients) {
            clientThread.join();
        }
    }
}