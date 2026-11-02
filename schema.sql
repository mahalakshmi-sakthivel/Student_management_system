-- ==========================================================
-- Student Management System - Database Schema & Sample Data
-- Database: MySQL 8.0+
-- ==========================================================

CREATE DATABASE IF NOT EXISTS student_db;
USE student_db;

-- 1. Departments Table
CREATE TABLE IF NOT EXISTS departments (
    department_id   INT PRIMARY KEY AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Courses Table
CREATE TABLE IF NOT EXISTS courses (
    course_id     INT PRIMARY KEY AUTO_INCREMENT,
    course_name   VARCHAR(100) NOT NULL,
    department_id INT,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL
);

-- 3. Students Table
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

-- 4. Admin Users Table
CREATE TABLE IF NOT EXISTS admin_users (
    admin_id      INT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

-- ==========================================================
-- Sample Data Insertion
-- ==========================================================

-- Default Departments
INSERT IGNORE INTO departments (department_id, department_name) VALUES
(1, 'Computer Science'),
(2, 'Information Technology'),
(3, 'Electronics and Communication'),
(4, 'Mechanical Engineering'),
(5, 'Civil Engineering');

-- Default Courses
INSERT IGNORE INTO courses (course_id, course_name, department_id) VALUES
(1, 'B.E. Computer Science & Engineering', 1),
(2, 'B.Tech Information Technology', 2),
(3, 'B.E. Electronics & Communication', 3),
(4, 'B.E. Mechanical Engineering', 4),
(5, 'B.E. Civil Engineering', 5);

-- Default Admin User (username: admin, password: admin123)
-- SHA-256 for 'admin123' is 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
INSERT IGNORE INTO admin_users (admin_id, username, password_hash) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');

-- 10+ Initial Sample Students
INSERT IGNORE INTO students (student_id, student_name, date_of_birth, gender, email, phone, department_id) VALUES
(1001, 'Aarav Sharma',     '2003-05-14', 'Male',   'aarav.sharma@example.com',     '9876543210', 1),
(1002, 'Diya Patel',       '2003-08-22', 'Female', 'diya.patel@example.com',       '9876543211', 2),
(1003, 'Rohan Verma',      '2002-11-05', 'Male',   'rohan.verma@example.com',      '9876543212', 3),
(1004, 'Ananya Iyer',      '2003-02-18', 'Female', 'ananya.iyer@example.com',      '9876543213', 1),
(1005, 'Karthik Raja',     '2002-09-30', 'Male',   'karthik.raja@example.com',     '9876543214', 4),
(1006, 'Pooja Reddy',      '2003-07-12', 'Female', 'pooja.reddy@example.com',      '9876543215', 2),
(1007, 'Siddharth Menon',  '2002-04-25', 'Male',   'siddharth.m@example.com',      '9876543216', 1),
(1008, 'Sneha Mukherjee',  '2003-12-03', 'Female', 'sneha.m@example.com',          '9876543217', 3),
(1009, 'Varun Nair',       '2002-06-19', 'Male',   'varun.nair@example.com',       '9876543218', 5),
(1010, 'Meera Nambiar',    '2003-10-08', 'Female', 'meera.nambiar@example.com',    '9876543219', 1);
