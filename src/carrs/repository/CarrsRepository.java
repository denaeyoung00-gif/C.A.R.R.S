package carrs.repository;

import carrs.model.*;
import java.time.LocalDate;
import java.util.List;

public interface CarrsRepository {
    // ---- shared (everyone uses these)
    Applicant findApplicant(int applicantId);          // null if none
    List<Applicant> findAllApplicants();
    Programme findProgramme(int programmeId);          // null if none
    Enrolment findEnrolment(int enrolmentId);          // null if none
    List<Enrolment> findEnrolmentsForApplicant(int applicantId);
    List<Programme> findActiveProgrammes();
    int countLiveEnrolments(int programmeId);

    // ---- Payment & Benefit Management
    void savePayment(Payment payment);                 // sets the new id on 'payment'
    /** ATOMIC: save the changed payment and its change records together, or neither. */
    void updatePayment(Payment payment, List<PaymentChange> changes);
    Payment findPayment(int paymentId);                // null if none
    List<Payment> findPaymentsByApplicant(int applicantId);
    /** A payment for the same applicant, programme, benefit and date that is not Failed. */
    Payment findLivePayment(int applicantId, int programmeId, BenefitType type, LocalDate scheduledDate);
    List<Payment> findCompletedPaymentsBetween(LocalDate from, LocalDate to);
    List<PaymentChange> findPaymentChanges(int paymentId);
}
