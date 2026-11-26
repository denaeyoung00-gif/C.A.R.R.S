package carrs.repository;

import carrs.model.*;
import java.time.LocalDate;
import java.util.List;

public interface CarrsRepository {
    // ---- shared (everyone uses these)
    Applicant findApplicant(int applicantId);          // null if none
    List<Applicant> findAllApplicants();
    List<StaffMember> findAllStaff();                  // the people who can log in
    Programme findProgramme(int programmeId);          // null if none
    Enrolment findEnrolment(int enrolmentId);          // null if none
    List<Enrolment> findEnrolmentsForApplicant(int applicantId);
    List<Programme> findActiveProgrammes();
    int countLiveEnrolments(int programmeId);

    // ---- Payment & Benefit Management
    /** ATOMIC: save the new payment and its audit line together, or neither. Sets the new id on 'payment'. */
    void savePayment(Payment payment, PaymentAudit audit);
    /** ATOMIC: save the changed payment, its change records and its audit line together, or none. */
    void updatePayment(Payment payment, List<PaymentChange> changes, PaymentAudit audit);
    /** An audit line for an action that changes no payment data (e.g. a cheque was printed). */
    void saveAudit(PaymentAudit audit);
    /** Audit trail for every payment of one constituent, oldest first. */
    List<PaymentAudit> findAuditForApplicant(int applicantId);
    Payment findPayment(int paymentId);                // null if none
    List<Payment> findPaymentsByApplicant(int applicantId);
    /** A payment for the same applicant, programme, benefit and date that is not Failed. */
    Payment findLivePayment(int applicantId, int programmeId, BenefitType type, LocalDate scheduledDate);
    List<Payment> findCompletedPaymentsBetween(LocalDate from, LocalDate to);
    List<PaymentChange> findPaymentChanges(int paymentId);

<<<<<<< HEAD
    // ============ Appeals & Disputes ============
    List<Application> findApplications();

    void saveAppeal(AppealRecord a);

    void saveAppealDecision(AppealRecord a, Application app);

    List<AppealRecord> findAppeals();


//     // ---- Programme Management
//    /** Saves a new programme and returns it with the id the database assigned. */
//    Programme saveProgramme(Programme programme);
//    /** Updates an existing programme in place. */
//    void updateProgramme(Programme programme);
//    /** Deletes a programme by id. Returns true if a row was removed. */
//    boolean deleteProgramme(int programmeId);
//    /** All programmes, ordered by id. */
//    List<Programme> findAllProgrammes();
=======
    // ---- Appeals & Disputes
    /** Every application that has a decision, with the applicant and programme names filled in. */
    List<Application> findApplications();
    /** ATOMIC: save the new appeal (with its documents) and lock its application, or neither. */
    void saveAppeal(AppealRecord appeal);
    /** ATOMIC: save the appeal's decision and the application changes it caused, or neither. */
    void saveAppealDecision(AppealRecord appeal, Application application);
    /** Every appeal lodged so far, oldest first, with its documents. */
    List<AppealRecord> findAppeals();
>>>>>>> 942262f303535ddd0cbba18e1efc755dec41a203
}
