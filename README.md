# HeartCare - Medical Clinic Management System

HeartCare is a comprehensive medical clinic management system designed to streamline patient care, appointment scheduling, medical record management, and administrative tasks. Built with JavaFX and following the MVC architecture, it provides an intuitive interface for healthcare professionals to manage their daily operations efficiently.

## 🏥 Features

### Patient Management
- Register new patients with comprehensive demographic information
- Search and filter patient records
- Edit and update patient information
- View patient history and appointments

### Appointment Scheduling
- Interactive calendar interface for appointment scheduling
- Multiple views for doctors and time slots
- Color-coded appointment status tracking (scheduled, checked-in, in progress, completed, missed, canceled)
- Quick patient check-in process

### Medical Records
- Digital patient medical records
- Track medical history, allergies, medications, and chronic conditions
- Record blood type, insurance information, and previous surgeries
- Secure access controls based on user role

### Payment Processing
- Record and track payments for appointments
- Multiple payment method support (cash, credit card, insurance)
- Generate and print invoices
- Payment history tracking

### User Management
- Role-based access control (Admin, Doctor, Staff)
- Secure authentication with password hashing
- User profiles and contact information
- Permission-based interface elements

### Analytics & Reporting
- Daily appointment summaries
- Patient demographics analysis
- Revenue tracking and financial statistics
- Custom date range filtering for reports
- Exportable PDF reports

## 💻 Technical Details

- **Language**: Java
- **UI Framework**: JavaFX
- **Architecture**: Model-View-Controller (MVC)
- **Database**: MySQL
- **Authentication**: BCrypt password hashing
- **Icons**: Ikonli icons
- **PDF Generation**: Apache PDFBox

## 🚀 Getting Started

### Prerequisites
- JDK 11 or later
- MySQL 5.7 or later
- Maven (for dependency management)

### Database Setup
1. Create a MySQL database named `heartcare`
2. Adjust the database connection settings in `DatabaseSingleton.java`:
   ```java
   private static final String URL = "jdbc:mysql://localhost:3307/heartcare";
   private static final String USER = "root";
   private static final String PASSWORD = "your_password";
   ```
3. Execute the following SQL statements to create the required tables:

```sql
-- Patient table
CREATE TABLE patient ( 
  ID varchar(8) PRIMARY KEY, 
  FNAME varchar(50), 
  LNAME varchar(50), 
  BIRTHDATE date, 
  SEXE enum('male','female'), 
  ADRESSE varchar(255), 
  TELEPHONE int, 
  EMAIL varchar(255), 
  CREATED_AT timestamp DEFAULT CURRENT_TIMESTAMP 
);

-- Medical record table 
CREATE TABLE dossierpatient ( 
  DossierID int AUTO_INCREMENT PRIMARY KEY, 
  PatientID varchar(8), 
  BloodType enum('A-','A+','B-','B+','AB-','AB+','O-','O+'), 
  Allergies text, 
  MedicalHistory text, 
  CurrentMedications text, 
  DateCreated timestamp DEFAULT CURRENT_TIMESTAMP, 
  LastUpdated timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, 
  BloodPressure varchar(20), 
  ChronicConditions text, 
  PreviousSurgeries text, 
  FamilyMedicalHistory text, 
  InsuranceProvider varchar(100), 
  InsurancePolicyNumber varchar(50), 
  FOREIGN KEY (PatientID) REFERENCES patient(ID) 
);

-- Users table 
CREATE TABLE users ( 
  ID varchar(8) PRIMARY KEY, 
  USERNAME varchar(50) UNIQUE, 
  PASSWORD varchar(255), 
  FNAME varchar(50), 
  LNAME varchar(50), 
  EMAIL varchar(50), 
  TELEPHONE int, 
  ADRESSE varchar(255), 
  BIRTHDATE date, 
  ROLE enum('medecin','admin','personnel') 
);

-- Appointments table 
CREATE TABLE rendezvous ( 
  RendezVousID int AUTO_INCREMENT PRIMARY KEY, 
  PatientID varchar(8), 
  MedecinID varchar(8), 
  AppointmentDateTime datetime, 
  ReasonForVisit text, 
  Status enum('Scheduled','CheckedIn','InProgress','Completed','Missed','Rescheduled','Patient_Cancelled','Clinic_Cancelled') DEFAULT 'Scheduled', 
  DateCreated timestamp DEFAULT CURRENT_TIMESTAMP, 
  LastUpdated timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, 
  StatusReason text, 
  NoShowFlag tinyint(1) DEFAULT 0, 
  RescheduledToID int NULL, 
  CancellationTime datetime NULL, 
  Priority enum('Normal','Urgent') DEFAULT 'Normal', 
  FOREIGN KEY (PatientID) REFERENCES patient(ID), 
  FOREIGN KEY (MedecinID) REFERENCES users(ID), 
  FOREIGN KEY (RescheduledToID) REFERENCES rendezvous(RendezVousID) 
);

-- Appointment history table 
CREATE TABLE rendezvous_history ( 
  HistoryID int AUTO_INCREMENT PRIMARY KEY, 
  RendezVousID int, 
  PreviousStatus enum('Scheduled','CheckedIn','InProgress','Completed','Missed','Rescheduled','Patient_Cancelled','Clinic_Cancelled'), 
  NewStatus enum('Scheduled','CheckedIn','InProgress','Completed','Missed','Rescheduled','Patient_Cancelled','Clinic_Cancelled'), 
  StatusReason text, 
  ChangedBy varchar(50), 
  ChangedAt datetime DEFAULT CURRENT_TIMESTAMP, 
  FOREIGN KEY (RendezVousID) REFERENCES rendezvous(RendezVousID) 
);

-- Payments table 
CREATE TABLE paiment ( 
  PaimentID int AUTO_INCREMENT PRIMARY KEY, 
  PatientID varchar(8), 
  RendezVousID int, 
  Amount decimal(10,2), 
  PaymentMethod enum('Cash',' Credit Card','Insurance'), 
  PaimentDate datetime DEFAULT CURRENT_TIMESTAMP, 
  FOREIGN KEY (PatientID) REFERENCES patient(ID), 
  FOREIGN KEY (RendezVousID) REFERENCES rendezvous(RendezVousID) 
);
```

### Running the Application
1. Clone the repository
2. Configure the database connection
3. Build the project using Maven: `mvn clean package`
4. Run the application: `java -jar target/HeartCare.jar`

## 📋 Project Structure

- `src/main/java/com/example/myjavafxapp/Models/` - Data models and business logic
- `src/main/java/com/example/myjavafxapp/Controllers/` - UI controllers
- `src/main/resources/com/example/myjavafxapp/` - FXML files and resources
- `src/main/resources/Icons/` - Application icons

## 🔐 Security Features
- Password hashing using jBCrypt
- Session management
- Role-based access control

## 📊 Screenshots
## Connexion au système
   L'écran d'accueil de HeartCare propose une interface de connexion sécurisée. Les utilisateurs peuvent saisir leurs identifiants, se connecter, s'inscrire ou récupérer un mot de passe oublié. Le design sobre met en valeur le nom du système "HeartCare" et sa qualité.
   ![Interface de connexion](./screenshots/login.png)

   ## Inscription des utilisateurs (CIN)
   Cette interface permet aux nouveaux utilisateurs de commencer leur inscription en saisissant leur numéro de CIN (Carte d'Identité Nationale). Les boutons "Soumettre" et "Annuler" permettent respectivement de valider ou d'abandonner le processus.
   ![Interface de connexion](./screenshots/inscrire2.png)

   ## Inscription des utilisateurs (Complète)
   Après validation du CIN, l'utilisateur accède à cette interface pour compléter son inscription. Il doit créer un nom d'utilisateur, un mot de passe et le confirmer. Un message de bienvenue personnalisé s'affiche (ici pour "mehdi darnakh").
   ![Interface de connexion](./screenshots/inscrire3.png)

   ## Récupération de mot de passe (Étape 1)
   Pour les utilisateurs ayant oublié leur mot de passe, cette interface permet de commencer le processus de récupération en saisissant le nom d'utilisateur et la date de naissance associée au compte.
   ![Interface de connexion](./screenshots/mdpo1.png)

   ## Récupération de mot de passe (Étape 2)
   Après vérification des informations, l'utilisateur (ici "Jane Smith") peut saisir un nouveau mot de passe et le confirmer. L'interface est sécurisée et intuitive pour ce processus critique.
   ![Interface de connexion](./screenshots/mdpo2.png)

## Liste des Utilisateurs
L'interface Gestion des utilisateurs affiche un tableau complet des utilisateurs enregistrés, incluant leurs CIN, noms, rôles (admin, personnel ou médecin), et coordonnées. Une barre de recherche permet un filtrage rapide. Le système applique des restrictions strictes sur le nombre d'utilisateurs par rôle : 1 administrateur max, 4 membres du personnel max, et 4 médecins max, assurant ainsi une gestion sécurisée des accès. La colonne "Action" permet la modification ou suppression des comptes.
![Interface de connexion](./screenshots/users1.png)

## Ajout d'un Utilisateur
Le formulaire Ajouter Un Utilisateur permet a l'administrateur de créer de nouveaux comptes en saisissant les informations essentielles (CIN, nom, rôle, date de naissance, etc.). Le système vérifie automatiquement les quotas par rôle avant validation. Cette interface garantit que seuls les utilisateurs autorisés sont ajoutés, en respectant les limites définies pour chaque type de rôle.![Interface de connexion](./screenshots/users2.png)
## 👥 Contributors

*Salaheddine Moujahid & Rihab Rochdi*
