package ru.spbau.mit.kazakov.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public class ChannelReader {
    private final static int INT_SIZE = 4;
    private final ByteBuffer size = ByteBuffer.allocate(INT_SIZE);
    private ByteBuffer data = null;

    /**
     * Reads part of packet.
     *
     * @return true if array data was read and false otherwise
     */
    public boolean read(@NotNull ByteChannel channel) throws IOException {
        if (data == null) {
            channel.read(size);
            if (size.position() == INT_SIZE) {
                data = ByteBuffer.allocate(size.getInt(0));
                size.clear();
            } else {
                return false;
            }
        }

        return channel.read(data) != 0;
    }

    public boolean isDone() {
        return data != null && data.position() == data.capacity();
    }

    public void clear() {
        size.clear();
        data = null;
    }

    @NotNull
    public byte[] getData() {
        return data.array();
    }
}
