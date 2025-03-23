package com.example.myjavafxapp.Controllers.GestionUsers;
import com.example.myjavafxapp.Models.*;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.util.Callback;
import java.util.Optional;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class gestionUsers implements Initializable {

    @FXML
    private TableView<Users> usersTable;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TableColumn<Users, Integer> cincolumn;

    @FXML
    private TableColumn<Users, String> prenomColumn;

    @FXML
    private TableColumn<Users, String> nomColumn;

    @FXML
    private TableColumn<Users, String> birthdateColumn;

    @FXML
    private TableColumn<Users, String> roleColumn;

    @FXML
    private TableColumn<Users, String> userNameColumn;

    @FXML
    private TableColumn<Users, String> emailColumn;

    @FXML
    public TextField searchField;

    @FXML
    private TableColumn<Users, Void> actionColumn;

    private ObservableList<Users> UsersModelObservableList = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadUsersData();
        setupActionsColumn();
    }

    private void loadUsersData() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        String usersQuery = "SELECT ID, FNAME, LNAME, BIRTHDATE, ROLE, USERNAME, EMAIL FROM USERS WHERE deleted_at IS NULL";
        try {
            PreparedStatement pstm = conn.prepareStatement(usersQuery);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                String ID = rs.getString("ID");
                String FNAME = rs.getString("FNAME");
                String LNAME = rs.getString("LNAME");
                Date BIRTHDATE = rs.getDate("BIRTHDATE");
                // Convertir la date en chaîne de caractères
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = dateFormat.format(BIRTHDATE); // Définir dateString
                String ROLE = rs.getString("ROLE");
                String USERNAME = rs.getString("USERNAME");
                String EMAIL = rs.getString("EMAIL");

                // Ajouter un objet Users à la liste
                UsersModelObservableList.add(new Users(ID, FNAME, LNAME, dateString, ROLE, USERNAME, EMAIL));            }

            // Configurer les colonnes du TableView
            cincolumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
            birthdateColumn.setCellValueFactory(new PropertyValueFactory<>("BIRTHDATE"));

            prenomColumn.setCellValueFactory(new PropertyValueFactory<>("FNAME"));
            nomColumn.setCellValueFactory(new PropertyValueFactory<>("LNAME"));
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("ROLE"));
            userNameColumn.setCellValueFactory(new PropertyValueFactory<>("USERNAME"));
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("EMAIL"));

            // Lier la liste au TableView
            usersTable.setItems(UsersModelObservableList);

            // Configurer le filtre de recherche (si nécessaire)
            setupSearchFilter();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSearchFilter() {
        FilteredList<Users> filteredData=new FilteredList<>(UsersModelObservableList, b->true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(users -> {
                if(newValue.isEmpty() || newValue.isBlank() || newValue==null){
                    return true;
                }
                String searchKeyword = newValue.toLowerCase();
                if (users.getID().toLowerCase().indexOf(searchKeyword) > -1) {
                    return true; // Means we found a match in ID
                }
                else if(users.getEMAIL().toLowerCase().indexOf(searchKeyword)>-1) {
                    return true;
                }else {
                    return false;
                }
            });
        });
        SortedList<Users> sortedData = new SortedList<>(filteredData);

        // Bind sorted result with table view
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());

        // Apply filtered and sorted data to the table view
        usersTable.setItems(sortedData);
    }

    private void setupActionsColumn() {
        actionColumn.setCellFactory(param -> new TableCell<Users, Void>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox buttonsContainer = new HBox(5); // 5 est l'espacement

            {
                // Le bouton d'édition
                FontIcon editIcon = new FontIcon("fas-user-edit");
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                editIcon.setIconSize(16);
                editButton.setGraphic(editIcon);
                editButton.getStyleClass().addAll("action-button", "edit-button");
                editButton.setOnAction(event -> {
                    Users user = getTableView().getItems().get(getIndex());
                    try {
                        UsersDataHolder.getInstance().setCurrent(user);
                        SwitchScene.switchScene(event, "/com/example/myjavafxapp/ModifyUsers.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // Le bouton de suppression
                FontIcon deleteIcon = new FontIcon("fas-trash-alt"); // Icône plus appropriée pour la suppression
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                deleteIcon.setIconSize(16);
                deleteButton.setGraphic(deleteIcon);
                deleteButton.getStyleClass().addAll("action-button", "delete-button");
                deleteButton.setOnAction(event -> {
                    Users user = getTableView().getItems().get(getIndex());
                    // Afficher une confirmation avant de supprimer
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirmation de suppression");
                    alert.setHeaderText("Supprimer l'utilisateur");
                    alert.setContentText("Êtes-vous sûr de vouloir supprimer cet utilisateur ?");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        // Code pour supprimer l'utilisateur de votre base de données
                        deleteUser(user);
                        // Rafraîchir la table
                        UsersModelObservableList.remove(user);
                        getTableView().refresh();
                    }
                });

                // Ajouter les deux boutons au conteneur
                buttonsContainer.getChildren().addAll(editButton, deleteButton);
                buttonsContainer.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsContainer);
                }
            }
        });
}

    private void deleteUser(Users user) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        // Remplacer cette ligne:
        // String deleteQuery = "DELETE FROM USERS WHERE ID = ?";
        // Par celle-ci:
        String softDeleteQuery = "UPDATE USERS SET deleted_at = NOW() WHERE ID = ?";
        try {
            PreparedStatement pstm = conn.prepareStatement(softDeleteQuery);
            pstm.setString(1, user.getID());
            int result = pstm.executeUpdate();
            if (result > 0) {
                // Soft delete réussi dans la base de données
                // Maintenant, supprimez de la liste observable
                UsersModelObservableList.remove(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Gérer l'erreur (afficher un message, etc.)
        }
    }
    public void returnAction(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/Dashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void addPatientAction(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/AddUsers.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
