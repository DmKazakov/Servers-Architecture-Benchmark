package ru.spbau.mit.kazakov.performance;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PerformanceTestResult {
    private ServerArchitecture architecture;
    private Parameter parameter;
    private int maxValue;
    private int queriesNumber;
    private int queriesDelay;
    private int clientsNumber;
    private int arraySize;
    private int step;
    @Singular("answerTime")
    private List<Double> answerTime;
    @Singular("sortTime")
    private List<Double> sortTime;
    @Singular("clientProcessingTime")
    private List<Double> clientProcessingTime;
    private int failedQueries;

    public int getInitValue() {
        if (parameter.equals(Parameter.ARRAY_SIZE)) {
            return arraySize;
        } else if (parameter.equals(Parameter.CLIENTS_NUMBER)) {
            return clientsNumber;
        } else {
            return queriesDelay;
        }
    }
}
