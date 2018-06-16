package ru.spbau.mit.kazakov.server;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import ru.spbau.mit.kazakov.utils.ArrayOuterClass;
import ru.spbau.mit.kazakov.utils.ArrayUtils;
import ru.spbau.mit.kazakov.utils.ChannelReader;
import ru.spbau.mit.kazakov.utils.ChannelWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class NonblockingServer {
    private static final int NUMBER_OF_THREADS = 4;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private final Map<SocketChannel, Queue<Buffer>> sortedArrayBuffer = new HashMap<>();
    private final ServerSocketChannel serverSocket;
    private final Selector writableChannels = Selector.open();
    private final Selector readableChannels = Selector.open();

    public NonblockingServer(int port) throws IOException {
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(true);
    }

    public void start() {
        new Thread(new ReadCycle()).start();
        new Thread(new WriteCycle()).start();

        while (true) {
            try {
                SocketChannel client = serverSocket.accept();
                client.configureBlocking(false);
                synchronized (readableChannels) {
                    client.register(readableChannels, SelectionKey.OP_READ, new ChannelReader());
                }
            } catch (IOException ignored) {
                //nothing to do
            }
        }
    }

    private class ReadCycle implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Set<SelectionKey> ready;
                    synchronized (readableChannels) {
                        int readyChannels = readableChannels.selectNow();
                        if (readyChannels == 0) {
                            continue;
                        }
                        ready = readableChannels.selectedKeys();
                    }

                    Iterator<SelectionKey> keyIterator = ready.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        ChannelReader reader = (ChannelReader) key.attachment();
                        reader.read((ByteChannel) key.channel());
                        while (reader.isDone()) {
                            StopWatch queryProcessTime = new StopWatch();
                            queryProcessTime.start();
                            threadPool.submit(new SortTask(key.channel(), reader.getData(), queryProcessTime));
                            reader.clear();
                            reader.read((ByteChannel) key.channel());
                        }
                        keyIterator.remove();
                    }
                } catch (IOException ignored) {
                    //nothing to do
                }
            }
        }
    }

    private class WriteCycle implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Set<SelectionKey> ready;
                    synchronized (writableChannels) {
                        int readyChannels = writableChannels.selectNow();
                        if (readyChannels == 0) {
                            continue;
                        }
                        ready = writableChannels.selectedKeys();
                    }

                    Iterator<SelectionKey> keyIterator = ready.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        ChannelWriter writer = (ChannelWriter) key.attachment();
                        SelectableChannel channel = key.channel();

                        synchronized (channel) {
                            writer.write((ByteChannel) channel);
                            if (writer.isDone()) {
                                synchronized (writableChannels) {
                                    //key.cancel(); may cause CancelledKeyException when register the channel next time
                                    key.interestOps(0);
                                }
                            }
                        }

                        keyIterator.remove();
                    }
                } catch (IOException ignored) {
                    //nothing to do
                }
            }
        }
    }

    private class SortTask implements Runnable {
        private final SelectableChannel client;
        private final byte[] data;
        private final StopWatch queryProcessTime;

        private SortTask(@NotNull SelectableChannel client, @NotNull byte[] data, @NotNull StopWatch queryProcessTime) {
            this.client = client;
            this.data = data;
            this.queryProcessTime = queryProcessTime;
        }

        @Override
        public void run() {
            try {
                ArrayOuterClass.Array protoArray = ArrayOuterClass.Array.parseFrom(data);
                int[] array = ArrayUtils.toIntArray(protoArray);

                StopWatch sortTime = new StopWatch();
                sortTime.start();
                ArrayUtils.sort(array);
                sortTime.stop();

                ArrayOuterClass.Array sorted = ArrayUtils.toProtoArray(array);
                byte[] serialized = sorted.toByteArray();

                synchronized (client) {
                    synchronized (writableChannels) {
                        SelectionKey key = client.keyFor(writableChannels);
                        ChannelWriter writer;

                        if (key == null) {
                            writer = new ChannelWriter();
                            try {
                                client.register(writableChannels, SelectionKey.OP_WRITE, writer);
                            } catch (ClosedChannelException ignored) {
                                //nothing to do
                            }
                        } else {
                            key.interestOps(SelectionKey.OP_WRITE);
                            writer = (ChannelWriter) key.attachment();
                        }

                        queryProcessTime.stop();
                        writer.addData(queryProcessTime.getTime(TimeUnit.MILLISECONDS));
                        writer.addData(sortTime.getTime(TimeUnit.MILLISECONDS));
                        writer.addData(serialized.length);
                        writer.addData(serialized);
                    }
                }
            } catch (InvalidProtocolBufferException ignored) {
                //nothing to do
            }
        }
    }


}
