package ru.spbau.mit.kazakov.performance;

import org.jetbrains.annotations.NotNull;

public enum  ServerArchitecture {
    BLOCKING {
        @Override
        @NotNull
        String getHost() {
            return "192.168.210.74";
        }

        @Override
        int getPort() {
            return 5555;
        }
    },
    NONBLOCKING {
        @Override
        @NotNull
        String getHost() {
            return "192.168.210.74";
        }

        @Override
        int getPort() {
            return 6666;
        }
    },
    SIMPLE {
        @Override
        @NotNull
        String getHost() {
            return "192.168.210.74";
        }

        @Override
        int getPort() {
            return 7777;
        }
    };

    @NotNull
    abstract String getHost();

    abstract int getPort();
}
