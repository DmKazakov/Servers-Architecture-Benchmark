package ru.spbau.mit.kazakov.server;

import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import ru.spbau.mit.kazakov.utils.ArrayOuterClass;
import ru.spbau.mit.kazakov.utils.ArrayUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;


public class SimpleServer {
    private final ServerSocket serverSocket;

    public SimpleServer(int port) throws IOException {
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
                    int[] intArray = ArrayUtils.toIntArray(array);
                    StopWatch sortTime = new StopWatch();
                    sortTime.start();
                    ArrayUtils.sort(intArray);
                    sortTime.stop();
                    ArrayOuterClass.Array protoArray = ArrayUtils.toProtoArray(intArray);

                    byte[] serializedSortedArray = protoArray.toByteArray();
                    queryProcessTime.stop();
                    out.writeLong(queryProcessTime.getTime(TimeUnit.MILLISECONDS));
                    out.writeLong(sortTime.getTime(TimeUnit.MILLISECONDS));
                    out.writeInt(serializedSortedArray.length);
                    out.write(serializedSortedArray);
                    out.flush();
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
}
