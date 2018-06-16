package ru.spbau.mit.kazakov.performance;

import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import ru.spbau.mit.kazakov.Client;
import ru.spbau.mit.kazakov.ConnectionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class PerformanceTester {
    private int queriesNumber;
    private int queriesDelay;
    private int clientsNumber;
    private int arraySize;
    private AtomicInteger failedQueries = new AtomicInteger(0);

    private final List<Double> sortTime = new ArrayList<>();
    private final List<Double> answerTime = new ArrayList<>();
    private final List<Double> clientProcessingTime = new ArrayList<>();

    public PerformanceTester(int numberOfQueries, int queriesDelay, int numberOfClients, int arraySize) {
        setQueriesNumber(numberOfQueries);
        setQueriesDelay(queriesDelay);
        setClientsNumber(numberOfClients);
        setArraySize(arraySize);
    }

    @NotNull
    public PerformanceTestResult testForQueriesDelay(@NotNull ServerArchitecture architecture, int step,
                                                     int maxDelay) throws ConnectionException {
        if (maxDelay < queriesDelay) {
            throw new IllegalArgumentException();
        }
        clear();

        try {
            for (int delay = queriesDelay; delay <= maxDelay; delay += step) {
                runClients(architecture.getHost(), architecture.getPort(), clientsNumber, queriesNumber, delay, arraySize);
            }
        } catch (Exception exception) {
            throw new ConnectionException();
        }

        return buildTestResult(Parameter.QUERIES_DELAY, step, maxDelay, architecture);
    }

    @NotNull
    public PerformanceTestResult testForClientsNumber(@NotNull ServerArchitecture architecture, int step,
                                                        int maxNumberOfClients) throws ConnectionException {
        if (maxNumberOfClients < clientsNumber) {
            throw new IllegalArgumentException();
        }
        clear();

        try {
            for (int clients = clientsNumber; clients <= maxNumberOfClients; clients += step) {
                runClients(architecture.getHost(), architecture.getPort(), clients, queriesNumber, queriesDelay, arraySize);
            }
        } catch (Exception exception) {
            throw new ConnectionException();
        }

        return buildTestResult(Parameter.CLIENTS_NUMBER, step, maxNumberOfClients, architecture);
    }

    @NotNull
    public PerformanceTestResult testForArraySize(@NotNull ServerArchitecture architecture, int step,
                                                  int maxSize) throws ConnectionException {
        if (maxSize < arraySize) {
            throw new IllegalArgumentException();
        }
        clear();

        try {
            for (int size = arraySize; size <= maxSize; size += step) {
                runClients(architecture.getHost(), architecture.getPort(), clientsNumber, queriesNumber, queriesDelay, size);
            }
        } catch (Exception exception) {
            throw new ConnectionException();
        }

        return buildTestResult(Parameter.ARRAY_SIZE, step, maxSize, architecture);
    }

    public void setQueriesNumber(int queriesNumber) {
        if (queriesNumber <= 0) {
            throw new IllegalArgumentException();
        }
        this.queriesNumber = queriesNumber;
    }

    public void setQueriesDelay(int queriesDelay) {
        if (queriesDelay < 0) {
            throw new IllegalArgumentException();
        }
        this.queriesDelay = queriesDelay;
    }

    public void setClientsNumber(int clientsNumber) {
        if (clientsNumber <= 0) {
            throw new IllegalArgumentException();
        }
        this.clientsNumber = clientsNumber;
    }

    public void setArraySize(int arraySize) {
        if (arraySize <= 0) {
            throw new IllegalArgumentException();
        }
        this.arraySize = arraySize;
    }

    private void runClients(@NotNull String host, int port, int numberOfClients, int numberOfQueries,
                            int queriesDelay, int arraySize) throws IOException, InterruptedException {
        List<Double> sortTimes = new ArrayList<>();
        List<Double> answerTimes = new ArrayList<>();
        List<Double> clientProcessingTimes = new ArrayList<>();
        Consumer<Client> clientQueries = (Client client) -> {
            StopWatch answerTime = new StopWatch();
            answerTime.start();
            long sortTime = 0;
            long clientProcessingTime = 0;

            for (int i = 0; i < numberOfQueries; i++) {
                try {
                    client.sort(generateArray(arraySize));
                    sortTime += client.getSortTime();
                    clientProcessingTime += client.getClientProcessingTime();
                    Thread.sleep(queriesDelay);
                } catch (Exception exception) {
                    failedQueries.addAndGet(1);
                }
            }
            answerTime.stop();

            synchronized (sortTimes) {
                sortTimes.add(sortTime / (double) numberOfQueries);
            }
            synchronized (clientProcessingTimes) {
                clientProcessingTimes.add(clientProcessingTime / (double) numberOfQueries);
            }
            synchronized (answerTimes) {
                answerTimes.add(answerTime.getTime(TimeUnit.MILLISECONDS) / (double) numberOfQueries);
            }
        };

        Thread[] clientsThreads = new Thread[numberOfClients];
        for (int i = 0; i < numberOfClients; i++) {
            Client client = new Client(host, port);
            clientsThreads[i] = new Thread(() -> clientQueries.accept(client));
        }
        for (int i = 0; i < numberOfClients; i++) {
            clientsThreads[i].start();
        }
        for (int i = 0; i < numberOfClients; i++) {
            clientsThreads[i].join();
        }

        sortTime.add(sortTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        answerTime.add(answerTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        clientProcessingTime.add(clientProcessingTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    @NotNull
    private PerformanceTestResult buildTestResult(@NotNull Parameter parameter, int step,
                                                  int maxValue, @NotNull ServerArchitecture architecture) {
        return PerformanceTestResult.builder()
                .architecture(architecture)
                .parameter(parameter)
                .step(step)
                .maxValue(maxValue)
                .queriesNumber(queriesNumber)
                .queriesDelay(queriesDelay)
                .clientsNumber(clientsNumber)
                .arraySize(arraySize)
                .answerTime(answerTime)
                .clientProcessingTime(clientProcessingTime)
                .sortTime(sortTime)
                .failedQueries(failedQueries.get())
                .build();
    }

    private void clear() {
        failedQueries.set(0);
        sortTime.clear();
        answerTime.clear();
        clientProcessingTime.clear();
    }

    @NotNull
    private static int[] generateArray(int size) {
        int[] array = new int[size];

        for (int i = 0; i < size; i++) {
            array[i] = ThreadLocalRandom.current().nextInt();
        }

        return array;
    }
}
