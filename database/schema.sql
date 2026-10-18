CREATE DATABASE IF NOT EXISTS appointment_db CHARACTER SET utf8mb4;
USE appointment_db;

-- =====================================================================
-- USERS
-- =====================================================================
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

-- =====================================================================
-- PATIENTS (1:1 profile extension of users with role PATIENT)
-- =====================================================================
CREATE TABLE IF NOT EXISTS patients (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,

  -- Identity
  medical_record_number VARCHAR(50) UNIQUE,
  date_of_birth DATE,
  gender ENUM('MALE','FEMALE','OTHER','UNKNOWN') DEFAULT 'UNKNOWN',
  blood_group ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-','UNKNOWN') DEFAULT 'UNKNOWN',

  -- Contact
  address VARCHAR(255),
  city VARCHAR(100),
  postal_code VARCHAR(20),
  country VARCHAR(100),
  emergency_contact_name VARCHAR(100),
  emergency_contact_phone VARCHAR(30),
  emergency_contact_relation VARCHAR(50),

  -- Clinical safety
  allergies VARCHAR(500),
  chronic_conditions VARCHAR(500),
  current_medications VARCHAR(500),

  -- Insurance / billing
  insurance_provider VARCHAR(150),
  insurance_policy_number VARCHAR(100),
  insurance_expiry DATE,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================================
-- DOCTORS
-- =====================================================================
CREATE TABLE IF NOT EXISTS doctors (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE,

  -- Professional identity
  specialization VARCHAR(100) NOT NULL,
  license_number VARCHAR(50) NOT NULL UNIQUE,
  license_expiry DATE,
  dea_number VARCHAR(50),
  npi_number VARCHAR(20),

  -- Credentials
  board_certifications VARCHAR(255),
  medical_school VARCHAR(200),
  graduation_year INT,
  years_of_experience INT,

  -- Practice / directory info
  clinic_name VARCHAR(150),
  clinic_address VARCHAR(255),
  clinic_phone VARCHAR(30),
  languages VARCHAR(200),
  accepting_new_patients TINYINT(1) NOT NULL DEFAULT 1,

  -- Doctor's personal address (separate from clinic address)
  address VARCHAR(255),
  city VARCHAR(100),
  postal_code VARCHAR(20),
  country VARCHAR(100),

  -- Scheduling
  slot_minutes INT NOT NULL DEFAULT 30,

  -- Profile
  bio TEXT,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =====================================================================
-- SCHEDULE
-- =====================================================================
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

CREATE TABLE IF NOT EXISTS schedule_exceptions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  doctor_id INT NOT NULL,
  off_date DATE NOT NULL,
  reason VARCHAR(200),
  UNIQUE KEY uq_doctor_off (doctor_id, off_date),
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- =====================================================================
-- APPOINTMENTS
-- =====================================================================
-- Status flow:
--   PENDING  -> APPROVED  -> COMPLETED
--   PENDING  -> CANCELLED
--   APPROVED -> CANCELLED
--
-- active_slot = 1 for PENDING and APPROVED (slot held),
--               NULL for CANCELLED and COMPLETED (slot free).
CREATE TABLE IF NOT EXISTS appointments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  doctor_id INT NOT NULL,
  patient_id INT NOT NULL,
  appt_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status ENUM('PENDING','APPROVED','CANCELLED','COMPLETED')
         NOT NULL DEFAULT 'PENDING',
  notes VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  active_slot TINYINT
    GENERATED ALWAYS AS (IF(status IN ('PENDING','APPROVED'), 1, NULL)) STORED,
  UNIQUE KEY uq_doctor_slot (doctor_id, appt_date, start_time, active_slot),
  INDEX idx_patient (patient_id, appt_date),
  INDEX idx_status (status),
  FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  FOREIGN KEY (patient_id) REFERENCES users(id)
);

USE appointment_db;

-- =====================================================================
-- USERS
-- All seeded accounts use password: password123
-- Hash format matches com.clinic.service.Passwords (PBKDF2WithHmacSHA256).
-- =====================================================================
INSERT INTO users (username, password_hash, role, full_name, phone, email) VALUES
  ('admin',    'JN9YmcHcKE0oKJNPbPFxxA==:AgQ+1anuQ7VGV05pfeEpKUZ16TQ+VC84n1mQ7VXgD7Q=', 'ADMIN',   'Maria Santos',  '+1-555-0100', 'admin@clinic.test'),
  ('dr.smith', 'w+2GccmMWfwE8hLb6siS+A==:jvwXjAlVAWeHwQtspcquHbr65BLOCT4Udf5XAqrtchE=', 'DOCTOR',  'John Smith',    '+1-555-0101', 'j.smith@clinic.test'),
  ('dr.chen',  'w+2GccmMWfwE8hLb6siS+A==:jvwXjAlVAWeHwQtspcquHbr65BLOCT4Udf5XAqrtchE=', 'DOCTOR',  'Wei Chen',      '+1-555-0102', 'w.chen@clinic.test'),
  ('dr.patel', 'w+2GccmMWfwE8hLb6siS+A==:jvwXjAlVAWeHwQtspcquHbr65BLOCT4Udf5XAqrtchE=', 'DOCTOR',  'Priya Patel',   '+1-555-0103', 'p.patel@clinic.test'),
  ('alice',    'w+2GccmMWfwE8hLb6siS+A==:jvwXjAlVAWeHwQtspcquHbr65BLOCT4Udf5XAqrtchE=', 'PATIENT', 'Alice Johnson', '+1-555-0201', 'alice@example.test'),
  ('bob',      'w+2GccmMWfwE8hLb6siS+A==:jvwXjAlVAWeHwQtspcquHbr65BLOCT4Udf5XAqrtchE=', 'PATIENT', 'Bob Martinez',  '+1-555-0202', 'bob@example.test'),
  ('carol',    'w+2GccmMWfwE8hLb6siS+A==:jvwXjAlVAWeHwQtspcquHbr65BLOCT4Udf5XAqrtchE=', 'PATIENT', 'Carol Nguyen',  '+1-555-0203', 'carol@example.test'),
  ('daniel',   'w+2GccmMWfwE8hLb6siS+A==:jvwXjAlVAWeHwQtspcquHbr65BLOCT4Udf5XAqrtchE=', 'PATIENT', 'Daniel Okafor', '+1-555-0204', 'daniel@example.test');

-- =====================================================================
-- DOCTORS
-- =====================================================================
INSERT INTO doctors (user_id, specialization, license_number, license_expiry, dea_number, npi_number,
                     board_certifications, medical_school, graduation_year, years_of_experience,
                     clinic_name, clinic_address, clinic_phone, languages, accepting_new_patients,
                     slot_minutes, bio) VALUES
  ((SELECT id FROM users WHERE username='dr.smith'),
   'Cardiology', 'MD-12345', '2027-06-30', 'BS1234567', '1234567890',
   'ABIM Cardiovascular Disease', 'Johns Hopkins School of Medicine', 2005, 18,
   'Downtown Heart Clinic', '100 Main St, Springfield', '+1-555-0301',
   'English, Spanish', 1, 30,
   'Board-certified cardiologist with 18 years of experience in preventive cardiology and heart failure management.'),

  ((SELECT id FROM users WHERE username='dr.chen'),
   'Pediatrics', 'MD-23456', '2026-12-31', NULL, '2345678901',
   'ABP General Pediatrics', 'Stanford University School of Medicine', 2010, 13,
   'Sunrise Pediatrics', '250 Oak Ave, Springfield', '+1-555-0302',
   'English, Mandarin', 1, 20,
   'Pediatrician focused on newborn care, childhood asthma, and developmental screening.'),

  ((SELECT id FROM users WHERE username='dr.patel'),
   'Dermatology', 'MD-34567', '2028-03-15', NULL, '3456789012',
   'ABD Dermatology', 'University of Michigan Medical School', 2012, 11,
   'Clear Skin Dermatology', '75 Birch Rd, Springfield', '+1-555-0303',
   'English, Hindi, Gujarati', 1, 45,
   'Dermatologist specializing in acne, eczema, and skin cancer screening.');

-- =====================================================================
-- PATIENTS
-- =====================================================================
INSERT INTO patients (user_id, medical_record_number, date_of_birth, gender, blood_group,
                      address, city, postal_code, country,
                      emergency_contact_name, emergency_contact_phone, emergency_contact_relation,
                      allergies, chronic_conditions, current_medications,
                      insurance_provider, insurance_policy_number, insurance_expiry) VALUES
  ((SELECT id FROM users WHERE username='alice'),
   'MRN-0001', '1988-04-12', 'FEMALE', 'A+',
   '12 Elm St', 'Springfield', '62701', 'USA',
   'Mark Johnson', '+1-555-0401', 'Spouse',
   'Penicillin', 'Hypertension', 'Lisinopril 10mg daily',
   'BlueCross BlueShield', 'BCB-88421', '2026-12-31'),

  ((SELECT id FROM users WHERE username='bob'),
   'MRN-0002', '1975-11-30', 'MALE', 'O-',
   '48 Pine Ave', 'Springfield', '62702', 'USA',
   'Susan Martinez', '+1-555-0402', 'Sister',
   'None known', 'Type 2 Diabetes', 'Metformin 500mg twice daily',
   'Aetna', 'AET-55107', '2026-09-30'),

  ((SELECT id FROM users WHERE username='carol'),
   'MRN-0003', '1995-07-22', 'FEMALE', 'B+',
   '9 Cedar Ln', 'Springfield', '62703', 'USA',
   'Linh Nguyen', '+1-555-0403', 'Mother',
   'Sulfa drugs, shellfish', 'Asthma', 'Albuterol inhaler as needed',
   'UnitedHealthcare', 'UHC-30128', '2027-03-31'),

  ((SELECT id FROM users WHERE username='daniel'),
   'MRN-0004', '1962-02-08', 'MALE', 'AB+',
   '33 Maple Dr', 'Springfield', '62704', 'USA',
   'Grace Okafor', '+1-555-0404', 'Spouse',
   'Aspirin', 'Coronary artery disease; high cholesterol', 'Atorvastatin 20mg nightly; Aspirin 81mg daily',
   'Medicare', 'MC-99876', '2026-06-30');

-- =====================================================================
-- SCHEDULE RULES (day_of_week: 1=Mon ... 7=Sun)
-- =====================================================================
INSERT INTO schedule_rules (doctor_id, day_of_week, start_time, end_time)
SELECT d.id, 1, '09:00:00', '12:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.smith'
UNION ALL SELECT d.id, 3, '09:00:00', '12:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.smith'
UNION ALL SELECT d.id, 5, '09:00:00', '12:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.smith'
UNION ALL SELECT d.id, 2, '13:00:00', '17:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.smith'
UNION ALL SELECT d.id, 4, '13:00:00', '17:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.smith';

INSERT INTO schedule_rules (doctor_id, day_of_week, start_time, end_time)
SELECT d.id, 1, '08:00:00', '16:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.chen'
UNION ALL SELECT d.id, 2, '08:00:00', '16:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.chen'
UNION ALL SELECT d.id, 3, '08:00:00', '16:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.chen'
UNION ALL SELECT d.id, 4, '08:00:00', '16:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.chen'
UNION ALL SELECT d.id, 5, '08:00:00', '12:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.chen';

INSERT INTO schedule_rules (doctor_id, day_of_week, start_time, end_time)
SELECT d.id, 2, '10:00:00', '18:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.patel'
UNION ALL SELECT d.id, 4, '10:00:00', '18:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.patel'
UNION ALL SELECT d.id, 6, '10:00:00', '14:00:00' FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.patel';

-- =====================================================================
-- DAY OFF (one example)
-- =====================================================================
INSERT INTO schedule_exceptions (doctor_id, off_date, reason)
SELECT d.id, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'Attending cardiology conference'
FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.smith';

-- =====================================================================
-- APPOINTMENTS
-- Mix of past/future and every status, so the UI has something to show
-- on day one. Times avoid collisions per doctor+date.
-- =====================================================================
INSERT INTO appointments (doctor_id, patient_id, appt_date, start_time, end_time, status, notes)
-- Past: completed visits
SELECT d.id, p.id, DATE_SUB(CURDATE(), INTERVAL 14 DAY), '09:00:00', '09:30:00', 'COMPLETED', 'Annual checkup'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.smith' AND p.username = 'alice'

UNION ALL
SELECT d.id, p.id, DATE_SUB(CURDATE(), INTERVAL 10 DAY), '09:30:00', '10:00:00', 'COMPLETED', 'Follow-up on BP medication'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.smith' AND p.username = 'alice'

UNION ALL
SELECT d.id, p.id, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:00:00', '08:20:00', 'COMPLETED', 'Child wellness visit'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.chen' AND p.username = 'carol'

UNION ALL
SELECT d.id, p.id, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:20:00', '08:40:00', 'COMPLETED', 'Asthma review'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.chen' AND p.username = 'carol'

-- Past: cancelled (should not block slots)
UNION ALL
SELECT d.id, p.id, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '13:00:00', '13:30:00', 'CANCELLED', 'Patient rescheduled'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.smith' AND p.username = 'bob'

-- Future: pending requests awaiting doctor approval
UNION ALL
SELECT d.id, p.id, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00:00', '09:30:00', 'PENDING', 'Chest tightness when exercising'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.smith' AND p.username = 'daniel'

UNION ALL
SELECT d.id, p.id, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:00:00', '10:45:00', 'PENDING', 'Recurring rash on arms'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.patel' AND p.username = 'alice'

UNION ALL
SELECT d.id, p.id, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '08:40:00', '09:00:00', 'PENDING', 'Toddler fever and cough'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.chen' AND p.username = 'bob'

-- Future: approved bookings
UNION ALL
SELECT d.id, p.id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:00:00', '13:30:00', 'APPROVED', 'Diabetes follow-up'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.smith' AND p.username = 'bob'

UNION ALL
SELECT d.id, p.id, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:45:00', '11:30:00', 'APPROVED', 'Mole check'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.patel' AND p.username = 'carol'

UNION ALL
SELECT d.id, p.id, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '08:00:00', '08:20:00', 'APPROVED', 'Vaccination'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.chen' AND p.username = 'daniel'

-- Future: cancelled (should not block slots)
UNION ALL
SELECT d.id, p.id, DATE_ADD(CURDATE(), INTERVAL 6 DAY), '10:00:00', '10:45:00', 'CANCELLED', 'Patient no longer needs visit'
FROM doctors d JOIN users u ON u.id = d.user_id,
     users p WHERE u.username = 'dr.patel' AND p.username = 'bob';
