package com.example.myjavafxapp.Controllers.patients;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import com.example.myjavafxapp.Models.patient.Patient;
import com.example.myjavafxapp.Models.patient.PatientDataHolder;
import com.example.myjavafxapp.Models.util.SwitchScene;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class GestionDossierController implements Initializable {

    @FXML
    public TableView<Patient> patientTable;

    @FXML
    public TableColumn<Patient, String> cinColumn;

    @FXML
    public TableColumn<Patient, String> nameColumn;

    @FXML
    public TableColumn<Patient, String> surnameColumn;

    @FXML
    public TableColumn<Patient, String> birthdateColumn;

    @FXML
    public TableColumn<Patient, String> sexeColumn;

    @FXML
    public TableColumn<Patient, String> adresseColumn;

    @FXML
    public TableColumn<Patient, Integer> phoneColumn;

    @FXML
    public TableColumn<Patient, String> emailColumn;

    @FXML
    public TableColumn<Patient, Void> actionsColumn;

    @FXML
    public TextField searchField;

    @FXML
    public Button addButton;

    private ObservableList<Patient> PatientModelObservableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadPatientData();
        setupActionsColumn();
    }

    private void loadPatientData() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        String patientQuery = "SELECT ID, FNAME, LNAME, BIRTHDATE, SEXE, ADRESSE, TELEPHONE, EMAIL FROM patient;";
        try {
            PreparedStatement pstm = conn.prepareStatement(patientQuery);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                String ID = rs.getString("ID");
                String FNAME = rs.getString("FNAME");
                String LNAME = rs.getString("LNAME");

                Date BIRTHDATE = rs.getDate("BIRTHDATE");
                // Convert the date to a String
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                // Define your desired format

                String dateString = dateFormat.format(BIRTHDATE);
                String SEXE = rs.getString("SEXE");
                String ADRESSE = rs.getString("ADRESSE");
                Integer TELEPHONE = rs.getInt("TELEPHONE");
                String EMAIL = rs.getString("EMAIL");

                // Populate the observableList
                PatientModelObservableList.add(new Patient(ID, FNAME, LNAME, dateString, SEXE, ADRESSE, TELEPHONE, EMAIL));
            }

            // PropertyValueFactory corresponds to the new Patient Model fields
            cinColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("FNAME"));
            surnameColumn.setCellValueFactory(new PropertyValueFactory<>("LNAME"));
            birthdateColumn.setCellValueFactory(new PropertyValueFactory<>("BIRTHDATE"));
            sexeColumn.setCellValueFactory(new PropertyValueFactory<>("SEXE"));
            adresseColumn.setCellValueFactory(new PropertyValueFactory<>("ADRESSE"));
            phoneColumn.setCellValueFactory(new PropertyValueFactory<>("TELEPHONE"));
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("EMAIL"));

            patientTable.setItems(PatientModelObservableList);

            setupSearchFilter();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearchFilter() {
        // Initial filtered list
        FilteredList<Patient> filteredData = new FilteredList<>(PatientModelObservableList, b -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(patient -> {
                // If no search value then display all records or whatever records it currently has
                if (newValue.isEmpty() || newValue.isBlank() || newValue == null) {
                    return true;
                }

                String searchKeyword = newValue.toLowerCase();
                if (patient.getID().toLowerCase().indexOf(searchKeyword) > -1) {
                    return true; // Means we found a match in ID
                } else if (patient.getEMAIL().toLowerCase().indexOf(searchKeyword) > -1) {
                    return true; // Means we found a match in EMAIL
                } else {
                    return false; // No match found
                }
            });
        });

        SortedList<Patient> sortedData = new SortedList<>(filteredData);

        // Bind sorted result with table view
        sortedData.comparatorProperty().bind(patientTable.comparatorProperty());

        // Apply filtered and sorted data to the table view
        patientTable.setItems(sortedData);
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button folderButton = new Button();

            {
                // Configure edit button
                FontIcon editIcon = new FontIcon("fas-user-edit");
                editIcon.setIconSize(16);
                editButton.setGraphic(editIcon);
                editButton.getStyleClass().addAll("action-button", "edit-button");
                editButton.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    try {
                        // Store the selected patient in a shared location or pass as parameter
                        PatientDataHolder.getInstance().setCurrentPatient(patient);
                        SwitchScene.switchScene(event, "/com/example/myjavafxapp/patients/editerPatient.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // Configure folder button
                FontIcon folderIcon = new FontIcon("fas-folder");
                folderIcon.setIconSize(16);
                folderButton.setGraphic(folderIcon);
                folderButton.getStyleClass().addAll("action-button", "folder-button");
                folderButton.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    try {
                        // Store the selected patient in a shared location or pass as parameter
                        PatientDataHolder.getInstance().setCurrentPatient(patient);
                        SwitchScene.switchScene(event, "/com/example/myjavafxapp/patients/dossierPatient.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.getChildren().addAll(editButton, folderButton);
                    setGraphic(hbox);
                }
            }
        });
    }

    public void returnAction(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/appointments/CalendarView.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void addPatientAction(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/patients/ajouterPatient.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}