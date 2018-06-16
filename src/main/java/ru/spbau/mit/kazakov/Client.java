package ru.spbau.mit.kazakov;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.spbau.mit.kazakov.utils.ArrayOuterClass;
import ru.spbau.mit.kazakov.utils.ArrayUtils;

import java.io.*;
import java.net.Socket;

public class Client {
    private final DataInputStream in;
    private final DataOutputStream out;
    @Getter
    private long sortTime = 0;
    @Getter
    private long clientProcessingTime = 0;

    public Client(@NotNull String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public int[] sort(@NotNull int[] array) throws IOException {
        ArrayOuterClass.Array protoArray = ArrayUtils.toProtoArray(array);
        byte[] serialized = protoArray.toByteArray();

        out.writeInt(serialized.length);
        out.write(serialized);
        out.flush();

        clientProcessingTime = in.readLong();
        sortTime = in.readLong();
        int size = in.readInt();

        byte[] serializedArray = new byte[size];
        int read = 0;
        while (read != size) {
            read += in.read(serializedArray, read, size - read);
        }

        return ArrayUtils.toIntArray(ArrayOuterClass.Array.parseFrom(serializedArray));
    }
}
