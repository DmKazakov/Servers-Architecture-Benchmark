package ru.spbau.mit.kazakov.performance;

import org.jetbrains.annotations.NotNull;

public enum Parameter {
    CLIENTS_NUMBER {
        @Override
        @NotNull
        public String toString() {
            return "Number of clients";
        }
    },
    QUERIES_DELAY {
        @Override
        @NotNull
        public String toString() {
            return "Queries delay";
        }
    },
    ARRAY_SIZE {
        @Override
        @NotNull
        public String toString() {
            return "Array size";
        }
    }
}
