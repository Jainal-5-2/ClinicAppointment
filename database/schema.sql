CREATE DATABASE IF NOT EXISTS appointment_db CHARACTER SET utf8mb4;
USE appointment_db;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('ADMIN','DOCTOR','PATIENT') NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  phone VARCHAR(30),
  email VARCHAR(100),
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS doctors (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,
  specialization VARCHAR(100) NOT NULL,
  slot_minutes INT NOT NULL DEFAULT 30,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Weekly recurring working hours (day_of_week: 1=Monday ... 7=Sunday)
CREATE TABLE IF NOT EXISTS schedule_rules (
  id INT AUTO_INCREMENT PRIMARY KEY,
  doctor_id INT NOT NULL,
  day_of_week TINYINT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  CHECK (day_of_week BETWEEN 1 AND 7),
  CHECK (start_time < end_time),
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- Days off / exceptions to the weekly schedule
CREATE TABLE IF NOT EXISTS schedule_exceptions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  doctor_id INT NOT NULL,
  off_date DATE NOT NULL,
  reason VARCHAR(200),
  UNIQUE KEY uq_doctor_off (doctor_id, off_date),
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- active_slot is 1 while the appointment is not cancelled, NULL when cancelled.
-- The UNIQUE key below makes double-booking impossible at the database level.
CREATE TABLE IF NOT EXISTS appointments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  doctor_id INT NOT NULL,
  patient_id INT NOT NULL,
  appt_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status ENUM('BOOKED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'BOOKED',
  notes VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  active_slot TINYINT GENERATED ALWAYS AS (IF(status <> 'CANCELLED', 1, NULL)) STORED,
  UNIQUE KEY uq_doctor_slot (doctor_id, appt_date, start_time, active_slot),
  INDEX idx_patient (patient_id, appt_date),
  FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  FOREIGN KEY (patient_id) REFERENCES users(id)
);
