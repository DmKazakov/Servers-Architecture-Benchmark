package ru.spbau.mit.kazakov;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChartBuilder extends Application {
    private static final String FIRST = "/home/dmitry/javares/s/cliets/client_processing_time";
    private static final String SECOND = "/home/dmitry/javares/bl/cliets/client_processing_time";
    private static final String THIRD = "/home/dmitry/javares/nonbl/cliets/client_processing_time";
    private Scene chartScene;

    @Override
    public void start(@NotNull Stage stage) throws FileNotFoundException {
        initializeChartScene();
        stage.setTitle("Nevermind");
        stage.setScene(chartScene);
        stage.show();
        stage.requestFocus();
    }

    private void initializeChartScene() throws FileNotFoundException {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Delay");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Time");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

        List<Pair<Integer, Double>> fDots = readDots(FIRST);
        List<Pair<Integer, Double>> sDots = readDots(SECOND);
        List<Pair<Integer, Double>> tDots = readDots(THIRD);

        XYChart.Series fSeries = createSeries("Simple", fDots);
        XYChart.Series sSeries = createSeries("Blocking", sDots);
        XYChart.Series tSeries = createSeries("Nonblocking", tDots);
        lineChart.getData().addAll(fSeries, sSeries, tSeries);

        chartScene = new Scene(lineChart, 800, 600);
        //saveAsPng(chartScene, "/home/dmitry/javares/pics/" + "clients-sort_time");
    }

    private List<Pair<Integer, Double>> readDots(@NotNull String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        int size = scanner.nextInt();
        List<Pair<Integer, Double>> dots = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int x = scanner.nextInt();
            double y = scanner.nextDouble();
            dots.add(new Pair<>(x, y));
        }

        return dots;
    }

    @NotNull
    private XYChart.Series<Number, Number> createSeries(@NotNull String name, @NotNull List<Pair<Integer, Double>> dots) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);

        for(int i = 0; i < dots.size(); i++) {
            series.getData().add(new XYChart.Data<>(dots.get(i).getKey(), dots.get(i).getValue()));
        }

        return series;
    }

    public void saveAsPng(@NotNull Scene scene, @NotNull String path) {
        WritableImage image = scene.snapshot(null);
        File file = new File(path);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
