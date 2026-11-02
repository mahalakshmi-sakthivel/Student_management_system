# 🎓 Student Management System - Project Report

## 1. Introduction
The **Student Management System** is a desktop database application designed to computerize the recording, retrieval, modification, and management of academic student records. Educational institutions often face operational bottlenecks when relying on manual ledger-based registers or fragmented spreadsheets. This system provides a unified, secure, and intuitive graphical interface coupled with relational persistence in MySQL.

### 1.1 Objectives
- **Digital Record Maintenance:** Eliminate paperwork by persisting all student demographic and academic enrollment data in a relational MySQL database.
- **Accurate CRUD Operations:** Provide seamless facilities to Create, Read, Update, and Delete student records with transactional reliability.
- **High Retrieval Speed:** Instantaneous multi-parameter search (by ID, Name, Department, and Email) and live real-time table filtering.
- **Data Integrity & Validation:** Enforce strict field-level constraints (positive numeric IDs, RFC-compliant email, 10-digit phone, past birthdates, and mandatory department selections) to prevent corrupt data entry.
- **Cybersecurity & Access Control:** Protect administrative access using SHA-256 cryptographic hashing and mitigate SQL injection risks through parameterized `PreparedStatement` APIs.

---

## 2. Software & Hardware Environment

| Category | Specification |
|---|---|
| **Programming Language** | Java (JDK 17+, tested on OpenJDK 25 LTS) |
| **GUI Framework** | Java Swing & AWT (`CardLayout`, `JTable`, Custom Theme) |
| **Architecture** | 3-Tier Layered Architecture with Data Access Object (DAO) Pattern |
| **Database Server** | MySQL Community Server 8.0+ |
| **Database Connectivity** | JDBC via `mysql-connector-j-8.3.0.jar` |
| **Operating System** | Windows 10 / 11, Linux, or macOS |
| **Storage / Memory** | Min 4 GB RAM, 500 MB Free Disk Space |

---

## 3. UI Theme & Design System

The visual design implements the requested **Wine Red & Black Palette**:

| Role | Color Name | HEX Code | Swing RGB Constant |
|---|---|---|---|
| Primary | Wine Red | `#722F37` | `new Color(0x72, 0x2F, 0x37)` |
| Primary Dark | Deep Burgundy | `#4A0E1A` | `new Color(0x4A, 0x0E, 0x1A)` |
| Primary Light | Rose Wine | `#9B3A47` | `new Color(0x9B, 0x3A, 0x47)` |
| Background | Jet Black | `#0D0D0D` | `new Color(0x0D, 0x0D, 0x0D)` |
| Surface Panels | Charcoal | `#1A1A1A` | `new Color(0x1A, 0x1A, 0x1A)` |
| Alternate Row | Dark Charcoal | `#222222` | `new Color(0x22, 0x22, 0x22)` |
| Text Input Background | Dark Grey | `#262626` | `new Color(0x26, 0x26, 0x26)` |
| Primary Text | Soft White | `#F5F0F0` | `new Color(0xF5, 0xF0, 0xF0)` |
| Muted Text | Ash Grey | `#A8A0A0` | `new Color(0xA8, 0xA0, 0xA0)` |
| Accent | Rose Gold | `#C9A27E` | `new Color(0xC9, 0xA2, 0x7E)` |
| Danger / Delete | Bright Red | `#E0434F` | `new Color(0xE0, 0x43, 0x4F)` |
| Success | Muted Green | `#4CAF6A` | `new Color(0x4C, 0xAF, 0x6A)` |

Typography: Standardized on `Segoe UI` (14 pt body, 14 pt bold, 18 pt subheadings, 26 pt headings).

---

## 4. System Architecture & Design Pattern

```
┌────────────────────────────────────────────────────────┐
│               PRESENTATION LAYER (SWING)               │
│  LoginFrame | DashboardFrame | Panels (Add, View, etc) │
└───────────────────────────┬────────────────────────────┘
                            │ (Events & Actions)
┌───────────────────────────▼────────────────────────────┐
│              BUSINESS & VALIDATION LAYER               │
│  Validator (Regex & Dates) | SecurityUtil (SHA-256)    │
└───────────────────────────┬────────────────────────────┘
                            │ (Domain Entities: Student)
┌───────────────────────────▼────────────────────────────┐
│                DATA ACCESS LAYER (DAO)                 │
│  StudentDAO | DepartmentDAO | AdminDAO                 │
└───────────────────────────┬────────────────────────────┘
                            │ (PreparedStatement via JDBC)
┌───────────────────────────▼────────────────────────────┐
│                    DATABASE ENGINE                     │
│  MySQL 8.0 (student_db: students, departments, etc.)   │
└────────────────────────────────────────────────────────┘
```

---

## 5. Database Schema & Relations

```sql
CREATE DATABASE IF NOT EXISTS student_db;
USE student_db;

CREATE TABLE IF NOT EXISTS departments (
    department_id   INT PRIMARY KEY AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS courses (
    course_id     INT PRIMARY KEY AUTO_INCREMENT,
    course_name   VARCHAR(100) NOT NULL,
    department_id INT,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS students (
    student_id    INT PRIMARY KEY,
    student_name  VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender        VARCHAR(10) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone         VARCHAR(15) NOT NULL,
    department_id INT,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS admin_users (
    admin_id      INT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);
```

### Relational Constraints
- **Referential Integrity:** `students.department_id` references `departments.department_id` with `ON DELETE RESTRICT`, guaranteeing no student record is orphaned.
- **Uniqueness:** `email` in `students` and `username` in `admin_users` have `UNIQUE` constraints to avoid duplicate registrations.
- **Primary Keys:** `student_id` enforces positive unique institutional identification numbers.

---

## 6. Functional Modules Description

### 6.1 Authentication Module (`LoginFrame.java`)
- Centered Charcoal card on Jet Black background with Rose Gold branding.
- Enforces credential check using `AdminDAO.authenticate()`.
- Passwords are encrypted on-the-fly with SHA-256 before the query is executed.
- Contains an integrated **DB Settings** modal allowing real-time connection configuration and automated 1-click database initialization.

### 6.2 Dashboard Overview (`OverviewPanel.java`)
- Displays live analytical KPI tiles: Total Enrolled Students, Total Academic Departments, and Male/Female demographic counts.
- Displays runtime system information: active database URL, port, schema name, and Java runtime environment.
- Offers 1-click quick-action routes to key modules.

### 6.3 Student Registration (`AddStudentPanel.java`)
- 2-column balanced form on charcoal surface.
- Form fields: Student ID, Full Name, Date of Birth (YYYY-MM-DD), Gender, Department (dynamically loaded from DB), Email, and Phone.
- Validates field format and checks for pre-existing Student IDs and Emails before issuing database insert commands.

### 6.4 Student Directory & Reporting (`ViewStudentPanel.java`)
- Custom JTable styled in Wine Red header with alternating dark rows.
- Dynamic search bar with live `TableRowSorter` filtering across all columns simultaneously.
- **Export to CSV:** Allows exporting all student records to a CSV spreadsheet file for offline administrative reporting.
- Double-clicking any student row automatically routes to the Update panel for immediate editing.

### 6.5 Record Modification (`UpdateStudentPanel.java`)
- Master-detail split layout: left selector list with instant filter, right edit form with pre-populated fields.
- The Primary Key (`student_id`) is locked in read-only mode to prevent identity alteration.
- Validates changes and updates the record in MySQL using `UPDATE students ...`.

### 6.6 Search & Deletion (`SearchDeletePanel.java`)
- Keyword search by ID, Name, Department, or Email.
- Detailed Preview Card inspecting all student demographic attributes.
- Prominent Bright Red (`#E0434F`) Delete button protected by a mandatory confirmation modal dialog (`JOptionPane.showConfirmDialog`) to prevent accidental data loss.

---

## 7. Verification Checklist & Test Results

| Test # | Test Case Description | Expected Result | Actual Result | Status |
|---|---|---|---|---|
| 1 | Admin Login with valid credentials (`admin` / `admin123`) | Dashboard opens, session initiated | Dashboard opens smoothly | **PASSED** |
| 2 | Admin Login with incorrect password | Error message displayed, access denied | Error banner shown | **PASSED** |
| 3 | Add student with valid details | Record committed to MySQL, table updated | Insert successful | **PASSED** |
| 4 | Add student with duplicate ID | Error: Duplicate Student ID detected | Warning modal shown | **PASSED** |
| 5 | Add student with empty fields | Validation banner highlights missing inputs | Validation modal shown | **PASSED** |
| 6 | Add student with invalid email or phone (<10 digits) | System rejects format before reaching DB | Regex validation caught | **PASSED** |
| 7 | View all students | Table lists all rows with alternating colors | Custom renderers applied | **PASSED** |
| 8 | Update student details | Modified data persisted to MySQL | Record updated in DB | **PASSED** |
| 9 | Search by ID or Name | Table dynamically narrows to matching records | Instant search results | **PASSED** |
| 10 | Delete student record | Confirmation prompt; removes row on "Yes" | Record deleted safely | **PASSED** |
| 11 | MySQL stopped or unreachable | Friendly connection error, no crash | DB Settings dialog displayed | **PASSED** |

---

## 8. Conclusion
The **Student Management System** successfully fulfills all academic and functional requirements. Built with a clean separation of concerns (MVC / DAO pattern), robust input sanitization, and an aesthetic **Wine Red & Black** theme, it provides an enterprise-ready model of desktop-database integration in Java.

