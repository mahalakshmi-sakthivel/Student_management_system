# 🎓 Student Management System
### Java Swing + JDBC + MySQL | Wine Red & Black Theme

A desktop database application designed to manage student records digitally, replacing paper-based registries with a secure, high-performance, and visually elegant system.

---

## 📸 Key Features & Architecture
- **Theme:** Wine Red (`#722F37`), Deep Burgundy (`#4A0E1A`), Jet Black (`#0D0D0D`), Charcoal (`#1A1A1A`), and Rose Gold (`#C9A27E`) accent.
- **Role-based Authentication:** Secure admin sign-in with SHA-256 hashed password verification.
- **Complete CRUD Operations:**
  - **Add Student:** 2-column validated form for student ID, name, date of birth, gender, department, email, and phone.
  - **View Students:** Interactive JTable with alternate row styling, live search filter, and 1-click **Export to CSV**.
  - **Update Student:** Quick student selector on the left, locked primary key, and auto-populated edit form.
  - **Search & Delete:** Multi-attribute search (by ID, Name, Department, Email) with record preview card and deletion confirmation prompt.
  - **Analytics Overview:** Real-time statistics for total students, total departments, and gender distribution.
- **SQL Injection Prevention:** Built entirely using `PreparedStatement`.
- **Database Self-Healing:** Built-in connection tester and 1-click database & table schema initialization.

---

## 🛠️ Project Structure

```
d:/DBMS_exp13/
├── lib/
│   └── mysql-connector-j-8.3.0.jar      # Official MySQL JDBC Driver
├── src/
│   ├── db/
│   │   └── DBConnection.java            # Configurable connection & auto-schema init
│   ├── model/
│   │   ├── Student.java                 # Student entity model
│   │   ├── Department.java              # Department entity model
│   │   └── AdminUser.java               # Administrator entity model
│   ├── dao/
│   │   ├── StudentDAO.java              # Full CRUD queries via PreparedStatement
│   │   ├── DepartmentDAO.java           # Department loader for dropdowns
│   │   └── AdminDAO.java                # Authentication & password hash queries
│   ├── ui/
│   │   ├── Theme.java                   # Palette, fonts, custom renderers & components
│   │   ├── LoginFrame.java              # Admin login with password toggle & DB setup
│   │   ├── DashboardFrame.java          # Sidebar navigation & CardLayout center
│   │   ├── OverviewPanel.java           # Dashboard metrics & system status
│   │   ├── AddStudentPanel.java         # Student registration form
│   │   ├── ViewStudentPanel.java        # Student directory with live filter & CSV export
│   │   ├── UpdateStudentPanel.java      # Split selector and update form
│   │   ├── SearchDeletePanel.java       # Search bar, results & delete confirmation
│   │   └── DatabaseConfigDialog.java    # MySQL connection & schema configuration dialog
│   ├── util/
│   │   ├── Validator.java               # Input validation for all student fields
│   │   └── SecurityUtil.java            # SHA-256 cryptographic hashing
│   ├── Main.java                        # Application entry point
│   └── TestRunner.java                  # Automated validation & unit test suite
├── bin/                                 # Compiled .class binaries
├── schema.sql                           # MySQL database DDL and sample records
├── db.properties                        # Editable database connection settings
├── manifest.mf                          # Manifest for executable JAR
├── StudentManagementSystem.jar          # Double-clickable runnable application JAR
├── build.bat                            # Windows 1-click compilation script
├── run.bat                              # Windows 1-click run script
├── PROJECT_REPORT.md                    # Full academic project documentation & viva guide
└── README.md
```

---

## ⚙️ Prerequisites & Setup

### 1. Prerequisites
- **Java JDK:** JDK 17 or later (OpenJDK 25 installed)
- **MySQL Server:** Version 8.0+

### 2. Database Setup
1. Start your local MySQL service (via MySQL Workbench, XAMPP, or Services).
2. Open `db.properties` and verify your MySQL password (default is `root`):
   ```properties
   db.host=localhost
   db.port=3306
   db.name=student_db
   db.user=root
   db.password=root
   ```
3. Import `schema.sql` into MySQL Workbench or let the application initialize it automatically by clicking **"⚙ DB Settings" -> "Initialize DB & Tables"** on the Login screen!

---

## 🚀 How to Run

### Method 1: Using the Executable JAR
Simply double-click `StudentManagementSystem.jar` or execute:
```cmd
java -jar StudentManagementSystem.jar
```

### Method 2: Using the Batch Scripts
- Double-click `build.bat` to recompile the project.
- Double-click `run.bat` to launch the application.

### Method 3: Command Line
```cmd
# Compile
javac -d bin -cp "lib/*;src" src/db/*.java src/model/*.java src/dao/*.java src/util/*.java src/ui/*.java src/Main.java

# Run
java -cp "bin;lib/*" Main
```

---

## 🔑 Default Administrator Credentials
- **Username:** `admin`
- **Password:** `admin123`
*(Password is stored as a SHA-256 hash in MySQL: `240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9`)*

---

## 🧪 Validation & Test Checklist
To run the automated verification suite:
```cmd
java -cp "bin;lib/*" TestRunner
```
Results:
- **Test 1:** Password hashing matches MySQL `SHA2('admin123', 256)` [PASSED]
- **Test 2:** Valid student inputs pass all validation rules [PASSED]
- **Test 3:** Non-numeric Student ID rejected correctly [PASSED]
- **Test 4:** Future Date of Birth rejected correctly [PASSED]
- **Test 5:** Malformed email rejected correctly [PASSED]
- **Test 6:** Invalid phone number rejected correctly [PASSED]
- **Test 7:** Missing department rejected correctly [PASSED]
- **Test 8:** MySQL Connector/J driver loaded successfully [PASSED]

