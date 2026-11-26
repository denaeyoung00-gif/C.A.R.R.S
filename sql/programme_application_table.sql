-- =====================================================================
-- C.A.R.R.S - Program Administration & Approval Tables
-- Author: Justin Bent (FTR-GOV-004)
-- =====================================================================

USE carrs;

-- 1. Add the Personal Information columns to the APPLICANT table
ALTER TABLE applicant
    ADD COLUMN id_type ENUM('TRN', 'NATIONAL_ID', 'DRIVERS_LICENCE') NULL,
  ADD COLUMN id_number VARCHAR(15) NULL,
  ADD COLUMN date_of_birth DATE NULL,
  ADD COLUMN address VARCHAR(255) NULL,
  ADD COLUMN employment_status ENUM('EMPLOYED', 'UNEMPLOYED', 'SELF_EMPLOYED') NULL,
  ADD COLUMN monthly_income DECIMAL(12,2) NULL,
  ADD COLUMN dependents INT NULL;

-- 2. Create your own separate APPLICATION table
CREATE TABLE IF NOT EXISTS programme_application (
                                                     application_id       INT PRIMARY KEY AUTO_INCREMENT,
                                                     reference_no         VARCHAR(20) NOT NULL UNIQUE,
    applicant_id         INT NOT NULL,
    programme_id         INT NOT NULL,
    assistance_requested VARCHAR(150) NOT NULL,
    reason_for_applying  TEXT NOT NULL,
    status               ENUM('COMPLETED', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'COMPLETED',
    rejection_reason     VARCHAR(500) NULL,
    decided_by           INT NULL,
    decided_at           DATETIME NULL,
    created_by           INT NOT NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    supporting_doc_path  VARCHAR(500) NULL,
    FOREIGN KEY (applicant_id) REFERENCES applicant(applicant_id),
    FOREIGN KEY (programme_id) REFERENCES programme(programme_id),
    FOREIGN KEY (created_by) REFERENCES staff_member(staff_id)
    );

-- 3. Create your own AUDIT table
CREATE TABLE IF NOT EXISTS programme_application_audit (
                                                           audit_id       INT PRIMARY KEY AUTO_INCREMENT,
                                                           application_id INT NULL,
                                                           programme_id   INT NULL,
                                                           action         VARCHAR(40)  NOT NULL,
    details        VARCHAR(500) NOT NULL,
    performed_by   INT NOT NULL,
    performed_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES programme_application(application_id),
    FOREIGN KEY (programme_id) REFERENCES programme(programme_id),
    FOREIGN KEY (performed_by) REFERENCES staff_member(staff_id)
    );