package ru.spbau.mit.kazakov.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.LinkedList;
import java.util.Queue;

public class ChannelWriter {
    private final Queue<ByteBuffer> dataQueue = new LinkedList<>();

    public void addData(@NotNull byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        this.dataQueue.add(buffer);
    }

    public void addData(int num) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(num);
        buffer.flip();
        this.dataQueue.add(buffer);
    }

    public void addData(long num) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(num);
        buffer.flip();
        this.dataQueue.add(buffer);
    }

    public void write(@NotNull ByteChannel channel) throws IOException {
        while (!dataQueue.isEmpty()) {
            ByteBuffer currentBuffer = dataQueue.peek();
            channel.write(currentBuffer);
            if (currentBuffer.position() != currentBuffer.capacity()) {
                break;
            } else {
                dataQueue.remove();
            }
        }
    }

    public boolean isDone() {
        return dataQueue.isEmpty();
    }
}
