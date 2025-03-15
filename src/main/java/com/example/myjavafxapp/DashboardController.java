package com.example.myjavafxapp;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
    @FXML
    private BarChart<String, Double> chart;
    @FXML
    private Circle circle;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        circle.setFill(new ImagePattern(new Image("file:///C:/Users/Dell%20Vostro%2015%203000/Videos/Captures/ppp.png")));
        XYChart.Series<String, Double> series_01= new XYChart.Series();
        series_01.setName("Year 2025");
        series_01.getData().add(new XYChart.Data("January",500));
        series_01.getData().add(new XYChart.Data("march",250));
        series_01.getData().add(new XYChart.Data("April",100));
        series_01.getData().add(new XYChart.Data("July",440));
        series_01.getData().add(new XYChart.Data("december",70));

        chart.getData().add(series_01);
    }
}
