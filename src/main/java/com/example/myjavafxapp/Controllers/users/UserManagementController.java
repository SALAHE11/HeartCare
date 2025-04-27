package com.example.myjavafxapp.Controllers.users;

import com.example.myjavafxapp.Models.user.Users;
import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    // Role limits constants
    private static final int ADMIN_LIMIT = 1;
    private static final int MEDECIN_LIMIT = 4;
    private static final int PERSONNEL_LIMIT = 4;

    // Main view components
    @FXML private VBox usersListView;
    @FXML private TextField searchField;
    @FXML private TableView<Users> usersTable;
    @FXML private TableColumn<Users, String> cinColumn;
    @FXML private TableColumn<Users, String> nomColumn;
    @FXML private TableColumn<Users, String> prenomColumn;
    @FXML private TableColumn<Users, String> birthdateColumn;
    @FXML private TableColumn<Users, String> roleColumn;
    @FXML private TableColumn<Users, String> userNameColumn;
    @FXML private TableColumn<Users, String> emailColumn;
    @FXML private TableColumn<Users, Void> actionColumn;
    @FXML private Button addButton;

    // Form view components
    @FXML private ScrollPane userFormView;
    @FXML private Label formTitleLabel;
    @FXML private TextField cinField;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private DatePicker dateNaissanceField;
    @FXML private TextField telephoneField;
    @FXML private TextField adresseField;
    @FXML private TextField emailField;
    @FXML private Button saveButton;

    // Data model
    private ObservableList<Users> usersData = FXCollections.observableArrayList();
    private Users currentUser;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize role combo box
        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "Medecin", "Personnel"));

        // Set up date picker format
        setupDatePicker();

        // Set up table columns
        setupTableColumns();

        // Load users data
        loadUsersData();

        // Check if "Add" button should be enabled (if any role is available)
        updateAddButtonState();

        // Set up actions column with edit/delete buttons
        setupActionsColumn();

        // Set up search filter
        setupSearchFilter();

        // Set up role change listener
        roleComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // If changing to a different role, check availability
                if (!isEditMode || (isEditMode && !newValue.equals(currentUser.getROLE()))) {
                    checkRoleAvailability(newValue);
                }
            }
        });
    }

    /**
     * Update the state of the Add button based on role availability
     */
    private void updateAddButtonState() {
        // Check if any role is available
        boolean anyRoleAvailable = isRoleAvailable("Admin") ||
                isRoleAvailable("Medecin") ||
                isRoleAvailable("Personnel");

        // Enable add button only if at least one role is available
        addButton.setDisable(!anyRoleAvailable);

        // Update tooltip
        if (!anyRoleAvailable) {
            addButton.setTooltip(new Tooltip("Impossible d'ajouter plus d'utilisateurs, toutes les limites de rôles sont atteintes"));
        } else {
            addButton.setTooltip(new Tooltip("Ajouter un nouvel utilisateur"));
        }
    }

    /**
     * Check if a role is available and show a warning if not
     */
    private void checkRoleAvailability(String role) {
        if (!isRoleAvailable(role)) {
            String message = "Le rôle '" + role + "' a atteint sa limite maximale";
            switch (role) {
                case "Admin":
                    message += " (" + ADMIN_LIMIT + " administrateurs maximum)";
                    break;
                case "Medecin":
                    message += " (" + MEDECIN_LIMIT + " médecins maximum)";
                    break;
                case "Personnel":
                    message += " (" + PERSONNEL_LIMIT + " personnels maximum)";
                    break;
            }
            showAlert(Alert.AlertType.WARNING, "Limite de rôle atteinte", message);

            // Reset to previous value if in edit mode
            if (isEditMode && currentUser != null) {
                roleComboBox.setValue(currentUser.getROLE());
            } else {
                roleComboBox.setValue(null);
            }
        }
    }

    /**
     * Check if a role is available based on database counts
     */
    private boolean isRoleAvailable(String role) {
        int currentCount = countUsersWithRole(role);
        int limit;

        switch (role) {
            case "Admin":
                limit = ADMIN_LIMIT;
                break;
            case "Medecin":
                limit = MEDECIN_LIMIT;
                break;
            case "Personnel":
                limit = PERSONNEL_LIMIT;
                break;
            default:
                return true;
        }

        // If in edit mode and not changing role, the role is available
        if (isEditMode && currentUser != null && currentUser.getROLE().equals(role)) {
            return true;
        }

        return currentCount < limit;
    }

    /**
     * Count users with a specific role directly from the database
     */
    private int countUsersWithRole(String role) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        String query = "SELECT COUNT(*) FROM USERS WHERE ROLE = ?";

        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, role);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Set up date picker with proper format
     */
    private void setupDatePicker() {
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
    }

    /**
     * Set up table columns
     */
    private void setupTableColumns() {
        cinColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("FNAME"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("LNAME"));
        birthdateColumn.setCellValueFactory(new PropertyValueFactory<>("BIRTHDATE"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("ROLE"));
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("USERNAME"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("EMAIL"));
    }

    /**
     * Load users data from database
     */
    private void loadUsersData() {
        usersData.clear();
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        String usersQuery = "SELECT ID, FNAME, LNAME, BIRTHDATE, ROLE, USERNAME, EMAIL, TELEPHONE, ADRESSE FROM USERS";

        try {
            PreparedStatement pstm = conn.prepareStatement(usersQuery);
            ResultSet rs = pstm.executeQuery();

            while (rs.next()) {
                String ID = rs.getString("ID");
                String FNAME = rs.getString("FNAME");
                String LNAME = rs.getString("LNAME");
                Date BIRTHDATE = rs.getDate("BIRTHDATE");

                // Convert date to string
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = dateFormat.format(BIRTHDATE);

                String ROLE = rs.getString("ROLE");
                String USERNAME = rs.getString("USERNAME");
                String EMAIL = rs.getString("EMAIL");

                // Create user
                Users user = new Users(ID, FNAME, LNAME, dateString, ROLE, USERNAME, EMAIL);

                // Set additional properties
                try {
                    user.setTELEPHONE(rs.getInt("TELEPHONE"));
                    user.setADRESSE(rs.getString("ADRESSE"));
                } catch (SQLException e) {
                    // Ignore if columns don't exist
                }

                // Add to list
                usersData.add(user);
            }

            // Set table items
            usersTable.setItems(usersData);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de base de données",
                    "Impossible de charger les données des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }

        // Update add button state after loading data
        updateAddButtonState();
    }

    /**
     * Set up actions column with edit and delete buttons
     */
    private void setupActionsColumn() {
        actionColumn.setCellFactory(param -> new TableCell<Users, Void>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox buttonsContainer = new HBox(5);

            {
                // Configure edit button
                FontIcon editIcon = new FontIcon("fas-user-edit");
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                editIcon.setIconSize(16);
                editButton.setGraphic(editIcon);
                editButton.getStyleClass().addAll("action-button", "edit-button");
                editButton.setTooltip(new Tooltip("Modifier l'utilisateur"));

                editButton.setOnAction(event -> {
                    Users user = getTableView().getItems().get(getIndex());
                    showEditUserForm(user);
                });

                // Configure delete button
                FontIcon deleteIcon = new FontIcon("fas-trash-alt");
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                deleteIcon.setIconSize(16);
                deleteButton.setGraphic(deleteIcon);
                deleteButton.getStyleClass().addAll("action-button", "delete-button");
                deleteButton.setTooltip(new Tooltip("Supprimer l'utilisateur"));

                deleteButton.setOnAction(event -> {
                    Users user = getTableView().getItems().get(getIndex());
                    confirmAndDeleteUser(user);
                });

                // Add buttons to container
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

    /**
     * Set up search filter
     */
    private void setupSearchFilter() {
        FilteredList<Users> filteredData = new FilteredList<>(usersData, b -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty() || newValue.isBlank()) {
                    return true;
                }

                String searchKeyword = newValue.toLowerCase();

                return user.getID().toLowerCase().contains(searchKeyword) ||
                        user.getFNAME().toLowerCase().contains(searchKeyword) ||
                        user.getLNAME().toLowerCase().contains(searchKeyword) ||
                        user.getEMAIL().toLowerCase().contains(searchKeyword) ||
                        (user.getUSERNAME() != null && user.getUSERNAME().toLowerCase().contains(searchKeyword));
            });
        });

        SortedList<Users> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sortedData);
    }

    /**
     * Handle search button click
     */
    @FXML
    private void handleSearch() {
        // Search is already handled by the FilteredList, but we can add additional functionality here
        if (searchField.getText().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Recherche", "Veuillez entrer un terme de recherche");
        }
    }

    /**
     * Show add user form
     */
    @FXML
    private void showAddUserForm() {
        isEditMode = false;
        formTitleLabel.setText("Ajouter Un Utilisateur");

        // Clear all fields
        clearFields();

        // Enable CIN field for new users
        cinField.setEditable(true);

        // Show form, hide list
        usersListView.setVisible(false);
        userFormView.setVisible(true);

        // Reset save button
        saveButton.setDisable(false);
    }

    /**
     * Show edit user form
     */
    private void showEditUserForm(Users user) {
        isEditMode = true;
        currentUser = user;
        formTitleLabel.setText("Modifier l'Utilisateur");

        // Populate fields with user data
        populateFields(user);

        // Disable CIN field for editing (primary key)
        cinField.setEditable(false);

        // Show form, hide list
        usersListView.setVisible(false);
        userFormView.setVisible(true);

        // Reset save button
        saveButton.setDisable(false);
    }

    /**
     * Populate form fields with user data
     */
    private void populateFields(Users user) {
        cinField.setText(user.getID());
        nomField.setText(user.getFNAME());
        prenomField.setText(user.getLNAME());
        roleComboBox.setValue(user.getROLE());

        // Set date
        try {
            if (user.getBIRTHDATE() != null && !user.getBIRTHDATE().isEmpty()) {
                dateNaissanceField.setValue(LocalDate.parse(user.getBIRTHDATE()));
            }
        } catch (Exception e) {
            System.err.println("Error parsing birth date: " + e.getMessage());
        }

        // Set additional fields
        telephoneField.setText(user.getTELEPHONE() > 0 ? String.valueOf(user.getTELEPHONE()) : "");
        adresseField.setText(user.getADRESSE() != null ? user.getADRESSE() : "");
        emailField.setText(user.getEMAIL());
    }

    /**
     * Clear all form fields
     */
    private void clearFields() {
        cinField.clear();
        nomField.clear();
        prenomField.clear();
        roleComboBox.setValue(null);
        dateNaissanceField.setValue(null);
        telephoneField.clear();
        adresseField.clear();
        emailField.clear();
    }

    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        if (!validateFields()) {
            return;
        }

        // Check role availability again (final check)
        String selectedRole = roleComboBox.getValue();
        if (!isEditMode || (isEditMode && !selectedRole.equals(currentUser.getROLE()))) {
            if (!isRoleAvailable(selectedRole)) {
                showAlert(Alert.AlertType.ERROR, "Limite de rôle atteinte",
                        "Impossible de " + (isEditMode ? "modifier" : "créer") + " l'utilisateur. " +
                                "La limite pour le rôle '" + selectedRole + "' est atteinte.");
                return;
            }
        }

        if (isEditMode) {
            updateUser();
        } else {
            createUser();
        }
    }

    /**
     * Create new user
     */
    private void createUser() {
        try {
            Connection conn = DatabaseSingleton.getInstance().getConnection();

            // Check if user with this ID already exists
            String checkQuery = "SELECT COUNT(*) FROM USERS WHERE ID = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, cinField.getText());

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Un utilisateur avec ce CIN existe déjà.");
                return;
            }

            // If no existing user, proceed with insert
            String insertQuery = "INSERT INTO USERS (ID, FNAME, LNAME, ROLE, BIRTHDATE, TELEPHONE, ADRESSE, EMAIL) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(insertQuery);
            pstmt.setString(1, cinField.getText());
            pstmt.setString(2, nomField.getText());
            pstmt.setString(3, prenomField.getText());
            pstmt.setString(4, roleComboBox.getValue());
            pstmt.setDate(5, Date.valueOf(dateNaissanceField.getValue()));
            pstmt.setInt(6, Integer.parseInt(telephoneField.getText()));
            pstmt.setString(7, adresseField.getText());
            pstmt.setString(8, emailField.getText());

            int result = pstmt.executeUpdate();

            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur ajouté avec succès");
                // Return to list view and refresh data
                returnToListView();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout de l'utilisateur");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de format", "Le numéro de téléphone doit être un nombre");
        }
    }

    /**
     * Update existing user
     */
    private void updateUser() {
        try {
            Connection conn = DatabaseSingleton.getInstance().getConnection();
            String updateQuery = "UPDATE USERS SET FNAME=?, LNAME=?, ROLE=?, BIRTHDATE=?, " +
                    "TELEPHONE=?, ADRESSE=?, EMAIL=? WHERE ID=?";

            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setString(1, nomField.getText());
            pstmt.setString(2, prenomField.getText());
            pstmt.setString(3, roleComboBox.getValue());
            pstmt.setDate(4, Date.valueOf(dateNaissanceField.getValue()));
            pstmt.setInt(5, Integer.parseInt(telephoneField.getText()));
            pstmt.setString(6, adresseField.getText());
            pstmt.setString(7, emailField.getText());
            pstmt.setString(8, cinField.getText());

            int result = pstmt.executeUpdate();

            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur modifié avec succès");
                // Return to list view and refresh data
                returnToListView();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification de l'utilisateur");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de format", "Le numéro de téléphone doit être un nombre");
        }
    }

    /**
     * Confirm and delete user
     */
    private void confirmAndDeleteUser(Users user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'utilisateur");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet utilisateur ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteUser(user);
        }
    }

    /**
     * Delete user (hard delete instead of soft delete)
     */
    private void deleteUser(Users user) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        // Changed from soft delete to hard delete
        String deleteQuery = "DELETE FROM USERS WHERE ID = ?";

        try {
            // Check for dependencies first
            boolean hasDependencies = checkUserDependencies(user.getID());

            if (hasDependencies) {
                showAlert(Alert.AlertType.WARNING, "Suppression impossible",
                        "Cet utilisateur ne peut pas être supprimé car il a des enregistrements liés dans le système.");
                return;
            }

            // No dependencies, proceed with hard delete
            PreparedStatement pstm = conn.prepareStatement(deleteQuery);
            pstm.setString(1, user.getID());

            int result = pstm.executeUpdate();
            if (result > 0) {
                // Remove from the observable list
                usersData.remove(user);
                usersTable.refresh();

                // Update add button state as a role spot may have opened up
                updateAddButtonState();

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur supprimé avec succès");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression de l'utilisateur");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if user has dependencies in other tables
     */
    private boolean checkUserDependencies(String userId) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        boolean hasDependencies = false;

        try {
            // Check for appointments
            String appointmentQuery = "SELECT COUNT(*) FROM rendezvous WHERE MedecinID = ?";
            PreparedStatement appointmentStmt = conn.prepareStatement(appointmentQuery);
            appointmentStmt.setString(1, userId);

            ResultSet rs = appointmentStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return true;
            }

            // Add more dependency checks if needed...

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return hasDependencies;
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        // Check if there are unsaved changes
        if (hasUnsavedChanges()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Annuler les modifications");
            alert.setHeaderText("Vous avez des modifications non enregistrées");
            alert.setContentText("Êtes-vous sûr de vouloir annuler ces modifications ?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                returnToListView();
            }
        } else {
            returnToListView();
        }
    }

    /**
     * Check if there are unsaved changes in the form
     */
    private boolean hasUnsavedChanges() {
        if (!isEditMode) {
            // For new user, check if any field has been filled
            return !cinField.getText().isEmpty() ||
                    !nomField.getText().isEmpty() ||
                    !prenomField.getText().isEmpty() ||
                    roleComboBox.getValue() != null ||
                    dateNaissanceField.getValue() != null ||
                    !telephoneField.getText().isEmpty() ||
                    !adresseField.getText().isEmpty() ||
                    !emailField.getText().isEmpty();
        } else {
            // For edit mode, check if any field has been changed
            return !currentUser.getFNAME().equals(nomField.getText()) ||
                    !currentUser.getLNAME().equals(prenomField.getText()) ||
                    !currentUser.getROLE().equals(roleComboBox.getValue()) ||
                    !currentUser.getBIRTHDATE().equals(dateNaissanceField.getValue().toString()) ||
                    currentUser.getTELEPHONE() != Integer.parseInt(telephoneField.getText().isEmpty() ? "0" : telephoneField.getText()) ||
                    !Objects.equals(currentUser.getADRESSE(), adresseField.getText()) ||
                    !currentUser.getEMAIL().equals(emailField.getText());
        }
    }

    /**
     * Return to the list view
     */
    private void returnToListView() {
        // Reset current user
        currentUser = null;
        isEditMode = false;

        // Clear form
        clearFields();

        // Reload users data
        loadUsersData();

        // Show list view, hide form
        usersListView.setVisible(true);
        userFormView.setVisible(false);
    }

    /**
     * Return to main dashboard
     */
    @FXML
    private void returnToMain(ActionEvent event) {
        try {
            // Load the calendar view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/appointments/CalendarView.fxml"));
            Scene scene = new Scene(loader.load());

            // Get the current stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation",
                    "Impossible de retourner au tableau de bord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validate form fields
     */
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

        if (roleComboBox.getValue() == null || roleComboBox.getValue().isEmpty()) {
            errorMessage.append("Le rôle est obligatoire\n");
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
        } else if (!isValidEmail(emailField.getText())) {
            errorMessage.append("Format d'e-mail invalide\n");
        }

        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", errorMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    /**
     * Show an alert dialog
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}