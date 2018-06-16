package ru.spbau.mit.kazakov;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.mit.kazakov.performance.PerformanceTestResult;
import ru.spbau.mit.kazakov.performance.PerformanceTester;
import ru.spbau.mit.kazakov.performance.ServerArchitecture;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;

public class GUI extends Application {
    private static final String NONBLOCKING_ARCHITECTURE = "Nonblocking";
    private static final String BLOCKING_ARCHITECTURE = "Blocking";
    private static final String SIMPLE_ARCHITECTURE = "Simple";
    private static final String ARRAY_SIZE = "Array size";
    private static final String NUMBER_OF_CLIENTS = "Number of clients";
    private static final String QUERY_DELAY = "Query delay";

    private static final UnaryOperator<TextFormatter.Change> INTEGER_FILTER = change -> {
        String input = change.getText();
        if (input.matches("[0-9]*")) {
            return change;
        }
        return null;
    };

    private Scene mainScene;

    @Override
    public void start(@NotNull Stage stage) {
        initializeMainScene();
        stage.setScene(mainScene);
        stage.setTitle("Nevermind");
        stage.show();
        stage.requestFocus();
    }

    private void initializeMainScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text title = new Text("Performance test");
        title.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(title, 0, 0, 2, 1);

        ComboBox<String> architectureComboBox = new ComboBox<>();
        architectureComboBox.getItems().addAll(SIMPLE_ARCHITECTURE, BLOCKING_ARCHITECTURE, NONBLOCKING_ARCHITECTURE);
        architectureComboBox.setValue(SIMPLE_ARCHITECTURE);
        addComboBoxField(1, "Architecture:", architectureComboBox, grid);

        ComboBox<String> parameterComboBox = new ComboBox<>();
        parameterComboBox.getItems().addAll(ARRAY_SIZE, NUMBER_OF_CLIENTS, QUERY_DELAY);
        parameterComboBox.setValue(ARRAY_SIZE);
        addComboBoxField(2, "Parameter:", parameterComboBox, grid);


        TextField stepField = addIntegerField(3, "Step:", grid);
        TextField maxValueField = addIntegerField(4, "Max value:", grid);
        TextField arraySizeField = addIntegerField(5, "Array size:", grid);
        TextField clientsNumberField = addIntegerField(6, "Number of clients:", grid);
        TextField queryDelayField = addIntegerField(7, "Query delay:", grid);
        TextField queriesNumberField = addIntegerField(8, "Number of queries:", grid);


        Label fileLabel = new Label();
        Button browseButton = new Button();
        browseButton.setText("Destination folder:");
        browseButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File file = directoryChooser.showDialog(null);
            if(file != null){
                fileLabel.setText(file.getPath());
            }
        });
        grid.add(browseButton, 0, 9);
        grid.add(fileLabel, 1, 9);

        Button startButton = new Button("Start");
        startButton.setOnAction(e -> {
            Integer step = getIntValue(stepField);
            Integer maxValue = getIntValue(maxValueField);
            Integer arraySize = getIntValue(arraySizeField);
            Integer clientsNumber = getIntValue(clientsNumberField);
            Integer queryDelay = getIntValue(queryDelayField);
            Integer queriesNumber = getIntValue(queriesNumberField);
            String directory = fileLabel.getText().trim();

            if (step == null || maxValue == null || arraySize == null || clientsNumber == null
                    || queryDelay == null || queriesNumber == null || directory.isEmpty()) {
                showAlertDialog("Fields can't be empty");
                return;
            }
            if (step <= 0 || maxValue <= 0 || arraySize <= 0 || clientsNumber <= 0
                    || queryDelay < 0 || queriesNumber <= 0) {
                showAlertDialog("Values must be positive");
                return;
            }

            ServerArchitecture architecture;
            String stringArchitecture = architectureComboBox.getValue();
            switch (stringArchitecture) {
                case NONBLOCKING_ARCHITECTURE:
                    architecture = ServerArchitecture.NONBLOCKING;
                    break;
                case BLOCKING_ARCHITECTURE:
                    architecture = ServerArchitecture.BLOCKING;
                    break;
                default:
                    architecture = ServerArchitecture.SIMPLE;
                    break;
            }

            PerformanceTester tester = new PerformanceTester(queriesNumber, queryDelay, clientsNumber, arraySize);
            PerformanceTestResult result;
            String parameter = parameterComboBox.getValue();

            try {
                switch (parameter) {
                    case ARRAY_SIZE:
                        if (arraySize > maxValue) {
                            showAlertDialog("Initial value should be less than max value");
                            return;
                        }
                        result = tester.testForArraySize(architecture, step, maxValue);
                        break;
                    case NUMBER_OF_CLIENTS:
                        if (clientsNumber > maxValue) {
                            showAlertDialog("Initial value should be less than max value");
                            return;
                        }
                        result = tester.testForClientsNumber(architecture, step, maxValue);
                        break;
                    default:
                        if (queryDelay > maxValue) {
                            showAlertDialog("Initial value should be less than max value");
                            return;
                        }
                        result = tester.testForQueriesDelay(architecture, step, maxValue);
                        break;
                }
                showChart(result);
                createResultFiles(result, directory);
            } catch (ConnectionException exception) {
                showAlertDialog("Connection error");
            } catch (IOException exception) {
                showAlertDialog("Unable to write to file");
            }
        });

        HBox connectHBox = new HBox(10);
        connectHBox.setAlignment(Pos.BOTTOM_RIGHT);
        connectHBox.getChildren().add(startButton);
        grid.add(connectHBox, 1, 10);

        mainScene = new Scene(grid, 400, 500);
    }

    private void createResultFiles(@NotNull PerformanceTestResult result, String directory) throws IOException {
        File description = new File(directory, "description");
        try (FileWriter writer = new FileWriter(description)) {
            writer.write("Architecture: " + result.getArchitecture() + "\n");
            writer.write("Parameter: " + result.getParameter() + "\n");
            writer.write("Step: " + result.getStep() + "\n");
            writer.write("Max value: " + result.getMaxValue() + "\n");
            writer.write("Array size: " + result.getArraySize() + "\n");
            writer.write("Number of clients: " + result.getClientsNumber() + "\n");
            writer.write("Query delay: " + result.getQueriesDelay() + "\n");
            writer.write("Number of queries: " + result.getQueriesNumber() + "\n");
        }

        File answerTime = new File(directory, "answer_time");
        printDots(answerTime, result.getAnswerTime(), result.getInitValue(), result.getStep());
        File sortTime = new File(directory, "sort_time");
        printDots(sortTime, result.getSortTime(), result.getInitValue(), result.getStep());
        File clientProcessingTime = new File(directory, "client_processing_time");
        printDots(clientProcessingTime, result.getClientProcessingTime(), result.getInitValue(), result.getStep());
    }

    private void printDots(@NotNull File file, @NotNull List<Double> time, int initValue, int step) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(time.size() + "\n");
            for(int i = 0; i < time.size(); i++) {
                writer.write(initValue + i * step + " " + time.get(i) + "\n");
            }
        }
    }

    @Nullable
    private Integer getIntValue(@NotNull TextField textField) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return null;
        } else {
            return Integer.parseInt(text);
        }
    }

    private void showChart(@NotNull PerformanceTestResult result) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(result.getParameter().toString());
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Time");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

        XYChart.Series answerTime = createSeries("Answer time", result.getAnswerTime(),
                result.getInitValue(), result.getStep());
        XYChart.Series sortTime = createSeries("Sort time", result.getSortTime(),
                result.getInitValue(), result.getStep());
        XYChart.Series clientProcessingTime = createSeries("Client processing time",
                result.getClientProcessingTime(), result.getInitValue(), result.getStep());
        lineChart.getData().addAll(answerTime, sortTime, clientProcessingTime);

        Scene chartScene = new Scene(lineChart, 800, 600);
        Stage chart = new Stage();
        chart.setScene(chartScene);
        chart.show();
    }

    @NotNull
    private XYChart.Series<Number, Number> createSeries(@NotNull String name, @NotNull List<Double> time,
                                                        int initValue, int step) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);

        for(int i = 0; i < time.size(); i++) {
            series.getData().add(new XYChart.Data<>(initValue + i * step, time.get(i)));
        }

        return series;
    }

    @NotNull
    private TextField addIntegerField(int row, @NotNull String text, @NotNull GridPane grid) {
        Label label = new Label(text);
        grid.add(label, 0, row);
        TextField integerField = new TextField();
        integerField.setTextFormatter(new TextFormatter<String>(INTEGER_FILTER));
        grid.add(integerField, 1, row);
        return integerField;
    }

    @NotNull
    private TextField addTextField(int row, @NotNull String text, @NotNull GridPane grid) {
        Label label = new Label(text);
        grid.add(label, 0, row);
        TextField integerField = new TextField();
        grid.add(integerField, 1, row);
        return integerField;
    }

    private void addComboBoxField(int row, @NotNull String text, @NotNull ComboBox<String> comboBox, @NotNull GridPane grid) {
        comboBox.setFocusTraversable(false);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        Label label = new Label(text);
        grid.add(label, 0, row);
        grid.add(comboBox, 1, row);
    }

    private void showAlertDialog(@NotNull String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
