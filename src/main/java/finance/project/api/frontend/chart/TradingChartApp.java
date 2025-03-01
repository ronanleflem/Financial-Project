package finance.project.api.frontend.chart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.Candle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static javafx.application.Application.launch;

public class TradingChartApp extends Application {

    private LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> series;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Trading Chart");

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Price");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Price Action");

        series = new XYChart.Series<>();
        chart.getData().add(series);

        VBox vbox = new VBox(chart);
        Scene scene = new Scene(vbox, 800, 600);
        stage.setScene(scene);
        stage.show();

        // Rafraîchir toutes les minutes
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::fetchData, 0, 1, TimeUnit.MINUTES);
    }

    private void fetchData() {
        try {
            URL url = new URL("http://localhost:8080/api/candles/EURUSD");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            ObjectMapper mapper = new ObjectMapper();
            List<Candle> candles = mapper.readValue(conn.getInputStream(),
                    new TypeReference<List<Candle>>() {});

            Platform.runLater(() -> updateChart(candles));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateChart(List<Candle> candles) {
        series.getData().clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Candle c : candles) {
            long timestamp = c.getDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            series.getData().add(new XYChart.Data<>(timestamp, c.getClose().doubleValue()));
        }
    }
}
