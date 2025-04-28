package com.example.myjavafxapp.Controllers.backup;

import com.example.myjavafxapp.Models.backup.BackupHistory;
import com.example.myjavafxapp.Models.backup.BackupManager;
import com.example.myjavafxapp.Models.backup.BackupSchedule;
import com.example.myjavafxapp.Models.user.UserSession;
import com.example.myjavafxapp.Models.util.SwitchScene;
import com.example.myjavafxapp.Components.TimePicker;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

public class BackupController implements Initializable {

    // History tab
    @FXML private TableView<BackupHistory> historyTableView;
    @FXML private TableColumn<BackupHistory, Integer> idColumn;
    @FXML private TableColumn<BackupHistory, String> dateTimeColumn;
    @FXML private TableColumn<BackupHistory, String> typeColumn;
    @FXML private TableColumn<BackupHistory, String> formatColumn;
    @FXML private TableColumn<BackupHistory, String> sizeColumn;
    @FXML private TableColumn<BackupHistory, String> statusColumn;
    @FXML private TableColumn<BackupHistory, String> userColumn;
    @FXML private TableColumn<BackupHistory, String> pathColumn;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Button restoreButton;
    @FXML private Button deleteButton;
    @FXML private Button detailsButton;
    @FXML private Button openFolderButton;

    // Schedule tab
    @FXML private CheckBox enableScheduleCheckBox;
    @FXML private TimePicker backupTimePicker;
    @FXML private ComboBox<String> backupFormatComboBox;
    @FXML private TextField backupLocationTextField;
    @FXML private CheckBox backupOnExitCheckBox;
    @FXML private CheckBox backupOnStartupCheckBox;
    @FXML private Spinner<Integer> retentionDaysSpinner;

    // Manual backup tab
    @FXML private ComboBox<String> manualBackupFormatComboBox;
    @FXML private TextField manualBackupLocationTextField;
    @FXML private TextField manualBackupDescriptionTextField;
    @FXML private ProgressIndicator backupProgressIndicator;
    @FXML private Button createBackupButton;

    // Restore tab
    @FXML private TextField restoreFileTextField;
    @FXML private ComboBox<String> restoreTypeComboBox;
    @FXML private CheckBox confirmRestoreCheckBox;
    @FXML private ProgressIndicator restoreProgressIndicator;
    @FXML private Button restoreSystemButton;

    // Status
    @FXML private Label statusLabel;

    // Data and managers
    private BackupManager backupManager;
    private BackupSchedule backupSchedule;
    private ObservableList<BackupHistory> backupHistory;
    private FilteredList<BackupHistory> filteredBackupHistory;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        backupManager = BackupManager.getInstance();
        backupSchedule = backupManager.getBackupSchedule();

        // Initialize the filter
        statusFilterComboBox.getSelectionModel().selectFirst();

        // Setup columns
        setupHistoryTable();

        // Load backup history
        refreshHistory();

        // Initialize schedule tab
        initializeScheduleTab();

        // Initialize manual backup tab
        initializeManualBackupTab();

        // Initialize restore tab
        initializeRestoreTab();

        // Set status
        statusLabel.setText("Prêt");
    }

    /**
     * Setup the history table columns
     */
    private void setupHistoryTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("backupID"));
        dateTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedDateTime()));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("backupType"));
        formatColumn.setCellValueFactory(new PropertyValueFactory<>("backupFormat"));
        sizeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedSize()));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("backupStatus"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("backupPath"));

        // Handle row selection
        historyTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            restoreButton.setDisable(!hasSelection || !"Success".equals(newSelection.getBackupStatus()));
            deleteButton.setDisable(!hasSelection);
            detailsButton.setDisable(!hasSelection);
        });
    }

    /**
     * Initialize the schedule tab
     */
    private void initializeScheduleTab() {
        // Set values from the schedule
        enableScheduleCheckBox.setSelected(backupSchedule.isEnabled());
        backupTimePicker.setValue(backupSchedule.getBackupTime());
        backupFormatComboBox.setValue(backupSchedule.getBackupFormat());
        backupLocationTextField.setText(backupSchedule.getBackupLocation());
        backupOnExitCheckBox.setSelected(backupSchedule.isBackupOnExit());
        backupOnStartupCheckBox.setSelected(backupSchedule.isBackupOnStartup());

        // Setup retention spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, backupSchedule.getRetentionDays());
        retentionDaysSpinner.setValueFactory(valueFactory);
    }

    /**
     * Initialize the manual backup tab
     */
    private void initializeManualBackupTab() {
        manualBackupFormatComboBox.setValue("SQL");
        manualBackupLocationTextField.setText(backupSchedule.getBackupLocation());
    }

    /**
     * Initialize the restore tab
     */
    private void initializeRestoreTab() {
        restoreTypeComboBox.setValue("SQL");

        // Link the confirm checkbox to the restore button
        confirmRestoreCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            restoreSystemButton.setDisable(!newVal || restoreFileTextField.getText().isEmpty());
        });

        // Link the file field to the restore button
        restoreFileTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            restoreSystemButton.setDisable(!confirmRestoreCheckBox.isSelected() || newVal.isEmpty());
        });
    }

    /**
     * Refresh the backup history
     */
    @FXML
    public void refreshHistory() {
        backupHistory = backupManager.getBackupHistory();

        // Apply filter
        filteredBackupHistory = new FilteredList<>(backupHistory);
        String filterStatus = statusFilterComboBox.getValue();

        if (!"Tous".equals(filterStatus)) {
            filteredBackupHistory.setPredicate(history -> filterStatus.equals(history.getBackupStatus()));
        }

        historyTableView.setItems(filteredBackupHistory);

        // Clear selection
        historyTableView.getSelectionModel().clearSelection();
    }

    /**
     * Filter the history by status
     */
    @FXML
    public void filterHistory() {
        if (filteredBackupHistory == null) return;

        String filterStatus = statusFilterComboBox.getValue();

        if ("Tous".equals(filterStatus)) {
            filteredBackupHistory.setPredicate(null);
        } else {
            filteredBackupHistory.setPredicate(history -> filterStatus.equals(history.getBackupStatus()));
        }
    }

    /**
     * Show details of the selected backup
     */
    @FXML
    public void showBackupDetails() {
        BackupHistory selectedBackup = historyTableView.getSelectionModel().getSelectedItem();
        if (selectedBackup == null) return;

        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Détails de la sauvegarde");
        details.setHeaderText("Sauvegarde #" + selectedBackup.getBackupID());

        // Create content
        TextArea textArea = new TextArea(
                "ID: " + selectedBackup.getBackupID() + "\n" +
                        "Date et heure: " + selectedBackup.getFormattedDateTime() + "\n" +
                        "Type: " + selectedBackup.getBackupType() + "\n" +
                        "Format: " + selectedBackup.getBackupFormat() + "\n" +
                        "Taille: " + selectedBackup.getFormattedSize() + "\n" +
                        "Statut: " + selectedBackup.getBackupStatus() + "\n" +
                        "Créé par: " + selectedBackup.getCreatedBy() + "\n" +
                        "Chemin: " + selectedBackup.getBackupPath() + "\n\n" +
                        (selectedBackup.getBackupDescription() != null ?
                                "Description: " + selectedBackup.getBackupDescription() : "")
        );
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(500);

        details.getDialogPane().setContent(textArea);
        details.showAndWait();
    }

    /**
     * Delete the selected backup
     */
    @FXML
    public void deleteBackup() {
        BackupHistory selectedBackup = historyTableView.getSelectionModel().getSelectedItem();
        if (selectedBackup == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la sauvegarde");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer cette sauvegarde?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete the file
                File backupFile = new File(selectedBackup.getBackupPath());
                if (backupFile.exists()) {
                    backupFile.delete();
                }

                // Update the record
                selectedBackup.setBackupStatus("Deleted");
                selectedBackup.setBackupDescription("Supprimé manuellement par " + UserSession.getInstance().getUsername());

                if (backupManager.saveBackupHistory(selectedBackup)) {
                    refreshHistory();
                    statusLabel.setText("Sauvegarde supprimée avec succès");
                } else {
                    showErrorAlert("Erreur", "Impossible de mettre à jour l'historique des sauvegardes");
                }

            } catch (Exception e) {
                showErrorAlert("Erreur", "Erreur lors de la suppression de la sauvegarde: " + e.getMessage());
            }
        }
    }

    /**
     * Open the backup folder
     */
    @FXML
    public void openBackupFolder() {
        try {
            String path = backupSchedule.getBackupLocation();
            File folder = new File(path);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            Desktop.getDesktop().open(folder);

        } catch (Exception e) {
            showErrorAlert("Erreur", "Impossible d'ouvrir le dossier de sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Browse for a backup location directory
     */
    @FXML
    public void browseBackupLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisir le dossier de sauvegarde");

        File initialDirectory = new File(backupLocationTextField.getText());
        if (initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        File selectedDirectory = directoryChooser.showDialog(backupLocationTextField.getScene().getWindow());
        if (selectedDirectory != null) {
            backupLocationTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Save the schedule settings
     */
    @FXML
    public void saveScheduleSettings() {
        // Update the schedule from UI
        backupSchedule.setEnabled(enableScheduleCheckBox.isSelected());
        backupSchedule.setBackupTime(backupTimePicker.getValue());
        backupSchedule.setBackupFormat(backupFormatComboBox.getValue());
        backupSchedule.setBackupLocation(backupLocationTextField.getText());
        backupSchedule.setBackupOnExit(backupOnExitCheckBox.isSelected());
        backupSchedule.setBackupOnStartup(backupOnStartupCheckBox.isSelected());
        backupSchedule.setRetentionDays(retentionDaysSpinner.getValue());

        // Save the schedule
        if (backupManager.saveBackupSchedule(backupSchedule)) {
            statusLabel.setText("Paramètres de sauvegarde enregistrés");
        } else {
            showErrorAlert("Erreur", "Impossible d'enregistrer les paramètres de sauvegarde");
        }
    }

    /**
     * Reset the schedule settings to the last saved values
     */
    @FXML
    public void resetScheduleSettings() {
        backupSchedule = backupManager.getBackupSchedule();
        initializeScheduleTab();
    }

    /**
     * Browse for a manual backup location
     */
    @FXML
    public void browseManualBackupLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisir le dossier de sauvegarde");

        File initialDirectory = new File(manualBackupLocationTextField.getText());
        if (initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        File selectedDirectory = directoryChooser.showDialog(manualBackupLocationTextField.getScene().getWindow());
        if (selectedDirectory != null) {
            manualBackupLocationTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Create a manual backup
     */
    @FXML
    public void createManualBackup() {
        String format = manualBackupFormatComboBox.getValue();
        String location = manualBackupLocationTextField.getText();
        String description = manualBackupDescriptionTextField.getText();

        // Check if the location exists
        File locationDir = new File(location);
        if (!locationDir.exists()) {
            if (!locationDir.mkdirs()) {
                showErrorAlert("Erreur", "Impossible de créer le dossier de sauvegarde");
                return;
            }
        }

        // Temporarily update the backup schedule location
        String originalLocation = backupSchedule.getBackupLocation();
        backupSchedule.setBackupLocation(location);
        backupManager.saveBackupSchedule(backupSchedule);

        // Disable UI during backup
        setManualBackupControlsDisabled(true);
        backupProgressIndicator.setVisible(true);
        statusLabel.setText("Sauvegarde en cours...");

        // Create the backup in a separate thread
        Task<BackupHistory> backupTask = backupManager.startBackupTask(
                BackupManager.BACKUP_TYPE_MANUAL, format);

        backupTask.setOnSucceeded(event -> {
            BackupHistory result = backupTask.getValue();

            // Update description if provided
            if (description != null && !description.isEmpty()) {
                result.setBackupDescription(description);
                backupManager.saveBackupHistory(result);
            }

            // Update UI
            backupProgressIndicator.setVisible(false);
            setManualBackupControlsDisabled(false);

            // Refresh history
            refreshHistory();

            // Restore original location
            backupSchedule.setBackupLocation(originalLocation);
            backupManager.saveBackupSchedule(backupSchedule);

            // Show success message
            statusLabel.setText("Sauvegarde créée avec succès");

            // Clear description field
            manualBackupDescriptionTextField.clear();
        });

        backupTask.setOnFailed(event -> {
            Throwable exception = backupTask.getException();
            showErrorAlert("Erreur de sauvegarde",
                    "Une erreur s'est produite lors de la sauvegarde: " +
                            (exception != null ? exception.getMessage() : "Erreur inconnue"));

            // Update UI
            backupProgressIndicator.setVisible(false);
            setManualBackupControlsDisabled(false);

            // Restore original location
            backupSchedule.setBackupLocation(originalLocation);
            backupManager.saveBackupSchedule(backupSchedule);

            statusLabel.setText("Échec de la sauvegarde");
        });

        new Thread(backupTask).start();
    }

    /**
     * Enable or disable manual backup controls
     */
    private void setManualBackupControlsDisabled(boolean disabled) {
        manualBackupFormatComboBox.setDisable(disabled);
        manualBackupLocationTextField.setDisable(disabled);
        manualBackupDescriptionTextField.setDisable(disabled);
        createBackupButton.setDisable(disabled);
    }

    /**
     * Browse for a restore file
     */
    @FXML
    public void browseRestoreFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir le fichier de restauration");

        // Set file filters based on restore type
        FileChooser.ExtensionFilter sqlFilter = new FileChooser.ExtensionFilter("Fichiers SQL", "*.sql");
        FileChooser.ExtensionFilter zipFilter = new FileChooser.ExtensionFilter("Fichiers ZIP", "*.zip");

        if ("SQL".equals(restoreTypeComboBox.getValue())) {
            fileChooser.getExtensionFilters().addAll(sqlFilter, zipFilter);
        } else {
            fileChooser.getExtensionFilters().addAll(zipFilter, sqlFilter);
        }

        // Set initial directory to backup location
        File initialDirectory = new File(backupSchedule.getBackupLocation());
        if (initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        File selectedFile = fileChooser.showOpenDialog(restoreFileTextField.getScene().getWindow());
        if (selectedFile != null) {
            restoreFileTextField.setText(selectedFile.getAbsolutePath());

            // Update restore type based on file extension
            if (selectedFile.getName().toLowerCase().endsWith(".sql")) {
                restoreTypeComboBox.setValue("SQL");
            } else if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
                restoreTypeComboBox.setValue("CSV");
            }
        }
    }

    /**
     * Restore the system from the selected backup
     */
    @FXML
    public void restoreSystem() {
        String restoreFile = restoreFileTextField.getText();
        String restoreType = restoreTypeComboBox.getValue();

        if (restoreFile.isEmpty()) {
            showErrorAlert("Erreur", "Veuillez sélectionner un fichier de restauration");
            return;
        }

        File file = new File(restoreFile);
        if (!file.exists()) {
            showErrorAlert("Erreur", "Le fichier de restauration n'existe pas");
            return;
        }

        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Confirmer la restauration");
        confirm.setHeaderText("Êtes-vous absolument sûr de vouloir restaurer le système?");
        confirm.setContentText("Toutes les données actuelles seront remplacées par celles de la sauvegarde. " +
                "Cette action est irréversible!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Disable UI during restore
            setRestoreControlsDisabled(true);
            restoreProgressIndicator.setVisible(true);
            statusLabel.setText("Restauration en cours...");

            // Perform restore in a separate thread
            Task<Boolean> restoreTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return performRestore(file, restoreType);
                }
            };

            restoreTask.setOnSucceeded(event -> {
                Boolean success = restoreTask.getValue();

                // Update UI
                restoreProgressIndicator.setVisible(false);
                setRestoreControlsDisabled(false);

                if (success) {
                    statusLabel.setText("Restauration terminée avec succès");

                    // Show success dialog
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Restauration réussie");
                    successAlert.setHeaderText("La restauration a été effectuée avec succès");
                    successAlert.setContentText("Le système a été restauré à partir de la sauvegarde. " +
                            "Il est recommandé de redémarrer l'application.");
                    successAlert.showAndWait();
                } else {
                    statusLabel.setText("Échec de la restauration");
                }
            });

            restoreTask.setOnFailed(event -> {
                Throwable exception = restoreTask.getException();
                showErrorAlert("Erreur de restauration",
                        "Une erreur s'est produite lors de la restauration: " +
                                (exception != null ? exception.getMessage() : "Erreur inconnue"));

                // Update UI
                restoreProgressIndicator.setVisible(false);
                setRestoreControlsDisabled(false);
                statusLabel.setText("Échec de la restauration");
            });

            new Thread(restoreTask).start();
        }
    }

    /**
     * Perform the actual restore operation
     */
    private boolean performRestore(File restoreFile, String restoreType) {
        try {
            ProcessBuilder pb = null;

            if ("SQL".equals(restoreType)) {
                // For SQL files, use mysql command line
                String url = "jdbc:mysql://localhost:3307/heartcare";
                String username = "root";
                String password = "Zoro*2222";

                // Extract database parameters from URL
                String[] parts = url.split("/");
                String database = parts[parts.length - 1];
                String host = url.split("://")[1].split(":")[0];
                String port = url.split(":")[2].split("/")[0];

                pb = new ProcessBuilder(
                        "mysql",
                        "--host=" + host,
                        "--port=" + port,
                        "--user=" + username,
                        "--password=" + password,
                        database
                );

                // Redirect input from the SQL file
                pb.redirectInput(restoreFile);

            } else {
                // For CSV files, we need a custom approach
                // Unzip the file first if it's a ZIP
                if (restoreFile.getName().toLowerCase().endsWith(".zip")) {
                    // TODO: Implement CSV restoration
                    // This would involve:
                    // 1. Unzipping the file
                    // 2. Reading each CSV file
                    // 3. Truncating the corresponding table
                    // 4. Inserting the data from the CSV
                    throw new UnsupportedOperationException("La restauration CSV n'est pas encore implémentée");
                }
            }

            Process process = pb.start();
            int exitCode = process.waitFor();

            return exitCode == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Enable or disable restore controls
     */
    private void setRestoreControlsDisabled(boolean disabled) {
        restoreFileTextField.setDisable(disabled);
        restoreTypeComboBox.setDisable(disabled);
        confirmRestoreCheckBox.setDisable(disabled);
        restoreSystemButton.setDisable(disabled);
    }

    /**
     * Restore from a selected backup in the history table
     */
    @FXML
    public void restoreBackup() {
        BackupHistory selectedBackup = historyTableView.getSelectionModel().getSelectedItem();
        if (selectedBackup == null) return;

        // Set the restore file and type
        restoreFileTextField.setText(selectedBackup.getBackupPath());
        restoreTypeComboBox.setValue(selectedBackup.getBackupFormat());

        // Switch to the restore tab
        TabPane tabPane = (TabPane) restoreFileTextField.getScene().lookup(".tab-pane");
        tabPane.getSelectionModel().select(3); // Index 3 is the restore tab
    }

    /**
     * Display an error alert
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Close the window
     */
    @FXML
    public void close() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Return to the calendar view
     */
    @FXML
    public void returnToCalendar(ActionEvent event) {
        try {
            // Ensure any pending changes are saved
            if (backupSchedule.isEnabled() != enableScheduleCheckBox.isSelected() ||
                    !backupSchedule.getBackupTime().equals(backupTimePicker.getValue()) ||
                    !backupSchedule.getBackupFormat().equals(backupFormatComboBox.getValue()) ||
                    !backupSchedule.getBackupLocation().equals(backupLocationTextField.getText()) ||
                    backupSchedule.isBackupOnExit() != backupOnExitCheckBox.isSelected() ||
                    backupSchedule.isBackupOnStartup() != backupOnStartupCheckBox.isSelected() ||
                    backupSchedule.getRetentionDays() != retentionDaysSpinner.getValue()) {

                // Prompt user to save changes
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Changements non sauvegardés");
                confirm.setHeaderText("Vous avez des modifications non sauvegardées");
                confirm.setContentText("Voulez-vous sauvegarder les changements avant de quitter?");

                ButtonType saveButton = new ButtonType("Sauvegarder");
                ButtonType discardButton = new ButtonType("Ne pas sauvegarder");
                ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

                confirm.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == saveButton) {
                        saveScheduleSettings();
                    } else if (result.get() == cancelButton) {
                        return; // Don't navigate away
                    }
                }
            }

            // Navigate to the calendar view using the provided event
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/Views/AppointmentCalendarView.fxml");

        } catch (IOException e) {
            showErrorAlert("Erreur", "Impossible de revenir au calendrier: " + e.getMessage());
        }
    }
}