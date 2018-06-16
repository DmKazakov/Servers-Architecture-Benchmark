package ru.spbau.mit.kazakov.server;

import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import ru.spbau.mit.kazakov.utils.ArrayOuterClass;
import ru.spbau.mit.kazakov.utils.ArrayUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class BlockingServer {
    private static final int NUMBER_OF_THREADS = 4;
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public BlockingServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            } catch (IOException ignored) {
                //nothing to do
            }
        }
    }

    /**
     * Handles client's requests.
     */
    private class ClientHandler implements Runnable {
        private Socket client;

        /**
         * Initializes client's socket.
         */
        public ClientHandler(@NotNull Socket client) {
            this.client = client;
        }

        /**
         * Request-Response cycle.
         */
        @Override
        public void run() {
            ExecutorService writer = Executors.newSingleThreadExecutor();
            try (DataInputStream in = new DataInputStream(client.getInputStream());
                 DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
                while (true) {
                    int size = in.readInt();
                    byte[] serializedArray = new byte[size];

                    int read = 0;
                    while (read != size) {
                        read += in.read(serializedArray, read, size - read);
                    }

                    StopWatch queryProcessTime = new StopWatch();
                    queryProcessTime.start();

                    ArrayOuterClass.Array array = ArrayOuterClass.Array.parseFrom(serializedArray);
                    threadPool.submit(new SortQueryHandler(array, writer, out, queryProcessTime));
                }
            } catch (Exception ignored) {
                try {
                    client.close();
                } catch (IOException exception) {
                    //nothing to do
                }
            }
        }
    }

    /**
     * Handles sort request.
     */
    private static class SortQueryHandler implements Runnable {
        private ArrayOuterClass.Array array;
        private ExecutorService writer;
        private DataOutputStream out;
        private StopWatch queryProcessTime;
        
        private SortQueryHandler(@NotNull ArrayOuterClass.Array array, @NotNull ExecutorService writer, 
                                 @NotNull DataOutputStream out, @NotNull StopWatch queryProcessTime) {
            this.array = array;
            this.writer = writer;
            this.out = out;
            this.queryProcessTime = queryProcessTime;
        }
        
        @Override
        public void run() {
            int[] intArray = ArrayUtils.toIntArray(array);
            StopWatch sortTime = new StopWatch();
            sortTime.start();
            ArrayUtils.sort(intArray);
            sortTime.stop();
            ArrayOuterClass.Array protoArray = ArrayUtils.toProtoArray(intArray);
            writer.submit(() -> {
                try {
                    byte[] serializedArray = protoArray.toByteArray();
                    queryProcessTime.stop();
                    out.writeLong(queryProcessTime.getTime(TimeUnit.MILLISECONDS));
                    out.writeLong(sortTime.getTime(TimeUnit.MILLISECONDS));
                    out.writeInt(serializedArray.length);
                    out.write(serializedArray);
                    out.flush();
                } catch (IOException ignored) {
                    //nothing to do
                }
            });
        }
    }
}
