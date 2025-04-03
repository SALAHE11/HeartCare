package com.example.myjavafxapp.Controllers.GestionUsers;

import com.example.myjavafxapp.Models.DatabaseSingleton;
import com.example.myjavafxapp.Models.Users;
import com.example.myjavafxapp.Models.UsersDataHolder;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class editerUsersController implements Initializable {

    @FXML
    private TextField adresseField;

    @FXML
    private Button cancelButton;

    @FXML
    private TextField cinField;

    @FXML
    private DatePicker dateNaissanceField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Button saveButton;

    @FXML
    private TextField telephoneField;

    @FXML
    private Users currentUser ;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "Medecin", "Personnel"));

        dateNaissanceField.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });

        loadUsersData();
    }

    private void loadUsersData() {
        currentUser=UsersDataHolder.getInstance().getCurrentUsers();
        if (currentUser != null) {
            cinField.setText(currentUser.getID());
            // Disable CIN field since it's the primary key
            cinField.setEditable(false);

            cinField.setText(currentUser.getID());
            nomField.setText(currentUser.getFNAME());
            prenomField.setText(currentUser.getLNAME());
            dateNaissanceField.setValue(LocalDate.parse(currentUser.getBIRTHDATE()));
            roleComboBox.setValue(currentUser.getROLE());
            adresseField.setText(currentUser.getADRESSE());
            telephoneField.setText(String.valueOf(currentUser.getTELEPHONE()));
            emailField.setText(currentUser.getEMAIL());
        }
    }


    @FXML
    void updateUsers(ActionEvent event) {
        validateFields();
        {
            try{
                Connection conn= DatabaseSingleton.getInstance().getConnection();
                String updateQuery="UPDATE USERS SET ID=?, FNAME=?, LNAME=?, ROLE=?, BIRTHDATE=?, TELEPHONE=?, ADRESSE=?, EMAIL=? WHERE ID=?";
                PreparedStatement pstmt=conn.prepareStatement(updateQuery);

                pstmt.setString(1, cinField.getText());
                pstmt.setString(2, nomField.getText());
                pstmt.setString(3, prenomField.getText());
                pstmt.setString(4, roleComboBox.getValue());
                pstmt.setDate(5, Date.valueOf(dateNaissanceField.getValue()));
                pstmt.setInt(6, Integer.parseInt(telephoneField.getText()));
                pstmt.setString(7, adresseField.getText());
                pstmt.setString(8, emailField.getText());
                pstmt.setString(9, cinField.getText());

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    updateUsersDataHolder();

                    showAlert(Alert.AlertType.INFORMATION, "Succès", "User modifié avec succès");
                    closeWindow(event);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification du l'utilisateur");
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de format", "Le numéro de téléphone doit être un nombre");
            }
        }
    }

    public void cancelAction(ActionEvent event) {
        try {
            // Charger le fichier FXML du tableau de bord
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/gestionUtilisateurs.fxml"));
            Parent root = loader.load();

            // Obtenir la fenêtre actuelle
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Définir la nouvelle scène
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Vous pouvez afficher une alerte en cas d'erreur
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le tableau de bord.");
        }
    }


    private void closeWindow(ActionEvent event) {
        // Get the source of the event
        Node source = (Node) event.getSource();
        // Get the stage (window) that contains the source
        Stage stage = (Stage) source.getScene().getWindow();
        // Close the window
        stage.close();
    }

    public void returnAction(ActionEvent event) {
        closeWindow(event);
    }


    private void updateUsersDataHolder() {
        if(currentUser !=null){
            currentUser.setID(cinField.getText());
            currentUser.setFNAME(nomField.getText());
            currentUser.setLNAME(prenomField.getText());
            currentUser.setROLE(roleComboBox.getValue());
            currentUser.setBIRTHDATE(dateNaissanceField.getValue().toString());
            currentUser.setTELEPHONE(Integer.parseInt(telephoneField.getText()));
            currentUser.setADRESSE(adresseField.getText());
            currentUser.setEMAIL(emailField.getText());

            UsersDataHolder.getInstance().setCurrentUser(currentUser);

        }
    }

    private boolean validateFields() {
        StringBuilder errorMessage = new StringBuilder();

        if (cinField.getText().isEmpty()) {
            errorMessage.append("Le CIN est obligatoire\n");
        }
        if (nomField.getText().isEmpty()) {
            errorMessage.append("Le nom est obligatoire\n");
        }
        if (prenomField.getText().isEmpty()) {
            errorMessage.append("Le prénom est obligatoire\n");
        }
        if (roleComboBox.getValue().isEmpty()) {
            errorMessage.append("Le role est obligatoire\n");
        }
        if (dateNaissanceField.getValue() == null) {
            errorMessage.append("La date de naissance est obligatoire\n");
        }
        if (telephoneField.getText().isEmpty()) {
            errorMessage.append("Le téléphone est obligatoire\n");
        } else {
            try {
                Integer.parseInt(telephoneField.getText());
            } catch (NumberFormatException e) {
                errorMessage.append("Le téléphone doit être un nombre\n");
            }
        }
        if (adresseField.getText().isEmpty()) {
            errorMessage.append("L'adresse est obligatoire\n");
        }
        if (emailField.getText().isEmpty()) {
            errorMessage.append("L'E-mail est obligatoire\n");
        }
        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", errorMessage.toString());
            return false;
        }
        return true;
    }
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


}
