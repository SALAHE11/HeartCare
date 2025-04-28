package com.example.myjavafxapp.Components;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.time.LocalTime;

public class TimePicker extends HBox {
    private ComboBox<Integer> hourComboBox;
    private ComboBox<Integer> minuteComboBox;
    private LocalTime time;

    public TimePicker() {
        this(LocalTime.of(23, 0)); // Default to 11:00 PM
    }

    public TimePicker(LocalTime initialTime) {
        super(5); // Spacing

        this.time = initialTime;

        // Hour ComboBox (0-23)
        hourComboBox = new ComboBox<>();
        for (int i = 0; i < 24; i++) {
            hourComboBox.getItems().add(i);
        }
        hourComboBox.setValue(initialTime.getHour());

        // Format to show leading zero
        hourComboBox.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer hour) {
                return hour != null ? String.format("%02d", hour) : "";
            }

            @Override
            public Integer fromString(String string) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        });

        // Minute ComboBox (0, 15, 30, 45)
        minuteComboBox = new ComboBox<>();
        minuteComboBox.getItems().addAll(0, 15, 30, 45);

        // Find closest minute
        int minute = initialTime.getMinute();
        int closestMinute = 0;
        int minDiff = Integer.MAX_VALUE;

        for (int m : minuteComboBox.getItems()) {
            int diff = Math.abs(minute - m);
            if (diff < minDiff) {
                minDiff = diff;
                closestMinute = m;
            }
        }

        minuteComboBox.setValue(closestMinute);

        // Format to show leading zero
        minuteComboBox.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer minute) {
                return minute != null ? String.format("%02d", minute) : "";
            }

            @Override
            public Integer fromString(String string) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        });

        // Add listeners to update time
        hourComboBox.setOnAction(e -> updateTime());
        minuteComboBox.setOnAction(e -> updateTime());

        // Add components to HBox
        getChildren().addAll(
                hourComboBox,
                new Label(":"),
                minuteComboBox
        );
    }

    private void updateTime() {
        int hour = hourComboBox.getValue();
        int minute = minuteComboBox.getValue();
        time = LocalTime.of(hour, minute);
    }

    public LocalTime getValue() {
        return time;
    }

    public void setValue(LocalTime time) {
        this.time = time;
        hourComboBox.setValue(time.getHour());

        // Find closest minute
        int minute = time.getMinute();
        int closestMinute = 0;
        int minDiff = Integer.MAX_VALUE;

        for (int m : minuteComboBox.getItems()) {
            int diff = Math.abs(minute - m);
            if (diff < minDiff) {
                minDiff = diff;
                closestMinute = m;
            }
        }

        minuteComboBox.setValue(closestMinute);
    }
}