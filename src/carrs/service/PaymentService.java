package carrs.service;

import carrs.model.*;
import carrs.repository.CarrsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** The rules for Payment and Benefit Management. Talks to data only through CarrsRepository. */
public class PaymentService {
    public static final int MIN_REASON_LENGTH = 10;
    private final CarrsRepository repo;

    public PaymentService(CarrsRepository repo) { this.repo = repo; }

    /** SR-1: record a new payment. It starts as PENDING. */
    public Payment recordPayment(StaffMember staff, int applicantId, int programmeId,
                                 BenefitType type, BigDecimal amount, LocalDate scheduledDate,
                                 String payeeName) throws PaymentException {
        requireCanRecord(staff);
        Applicant applicant = repo.findApplicant(applicantId);
        if (applicant == null) throw new PaymentException("That constituent was not found.");
        if (repo.findProgramme(programmeId) == null) throw new PaymentException("That programme was not found.");
        if (type == null) throw new PaymentException("Choose a benefit type.");
        if (amount == null || amount.signum() <= 0) throw new PaymentException("The amount must be more than zero.");
        if (scheduledDate == null) throw new PaymentException("Enter the scheduled payment date.");
        if (!isApprovedFor(applicantId, programmeId))
            throw new PaymentException("This constituent is not approved for that programme, so no payment can be recorded.");
        if (repo.findLivePayment(applicantId, programmeId, type, scheduledDate) != null)
            throw new PaymentException("A payment for this constituent, programme, benefit and date already exists.");

        String payee = (payeeName == null || payeeName.isBlank()) ? applicant.getName() : payeeName.trim();
        Payment p = new Payment(0, applicantId, programmeId, type, amount.setScale(2, java.math.RoundingMode.HALF_UP),
                scheduledDate, null, PaymentStatus.PENDING, null, payee,
                false, null, null, staff.getId(), LocalDateTime.now());
        save(() -> repo.savePayment(p));
        return p;
    }

    /** SR-5: move a payment along: Pending -> Processed -> Completed, or to Failed (a reason is required). */
    public Payment updateStatus(StaffMember staff, int paymentId, PaymentStatus next, String note)
            throws PaymentException {
        requireCanRecord(staff);
        Payment p = requirePayment(paymentId);
        if (!p.getStatus().canTransitionTo(next))
            throw new PaymentException("A " + p.getStatus() + " payment cannot be changed to " + next + ".");
        if (next == PaymentStatus.FAILED && (note == null || note.isBlank()))
            throw new PaymentException("Enter the reason the payment failed.");

        LocalDateTime now = LocalDateTime.now();
        List<PaymentChange> changes = new ArrayList<>();
        String why = (note == null || note.isBlank()) ? "Status update" : note.trim();
        changes.add(change(p, "status", p.getStatus().name(), next.name(), why, staff, now));
        p.setStatus(next);
        if (next == PaymentStatus.COMPLETED) {
            p.setDistributedDate(LocalDate.now());
            changes.add(change(p, "distributedDate", null, p.getDistributedDate().toString(), why, staff, now));
        }
        if (next == PaymentStatus.FAILED) p.setFailureReason(note.trim());
        save(() -> repo.updatePayment(p, changes));
        return p;
    }

    /** SR-8: correct a payment. The old values are kept as change records. A reason is required. */
    public Payment correctPayment(StaffMember staff, int paymentId, BenefitType newType, BigDecimal newAmount,
                                  LocalDate newDate, String newPayee, String reason) throws PaymentException {
        requireCanRecord(staff);
        Payment p = requirePayment(paymentId);
        if (p.getStatus() == PaymentStatus.FAILED)
            throw new PaymentException("A failed payment cannot be corrected. Record a new payment instead.");
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH)
            throw new PaymentException("Enter a reason of at least " + MIN_REASON_LENGTH + " characters.");
        if (newAmount != null && newAmount.signum() <= 0) throw new PaymentException("The amount must be more than zero.");

        BenefitType type = newType != null ? newType : p.getBenefitType();
        BigDecimal amount = newAmount != null ? newAmount.setScale(2, java.math.RoundingMode.HALF_UP) : p.getAmount();
        LocalDate date = newDate != null ? newDate : p.getScheduledDate();
        String payee = (newPayee != null && !newPayee.isBlank()) ? newPayee.trim() : p.getPayeeName();

        LocalDateTime now = LocalDateTime.now();
        String why = reason.trim();
        List<PaymentChange> changes = new ArrayList<>();
        boolean money = false;
        if (type != p.getBenefitType()) { changes.add(change(p, "benefitType", p.getBenefitType().name(), type.name(), why, staff, now)); money = true; }
        if (amount.compareTo(p.getAmount()) != 0) { changes.add(change(p, "amount", p.getAmount().toPlainString(), amount.toPlainString(), why, staff, now)); money = true; }
        if (!date.equals(p.getScheduledDate())) { changes.add(change(p, "scheduledDate", p.getScheduledDate().toString(), date.toString(), why, staff, now)); money = true; }
        if (!Objects.equals(payee, p.getPayeeName())) changes.add(change(p, "payeeName", p.getPayeeName(), payee, why, staff, now));
        if (changes.isEmpty()) throw new PaymentException("Nothing was changed.");

        if (type != p.getBenefitType() || !date.equals(p.getScheduledDate())) {
            Payment clash = repo.findLivePayment(p.getApplicantId(), p.getProgrammeId(), type, date);
            if (clash != null && clash.getId() != p.getId())
                throw new PaymentException("A payment for this constituent, programme, benefit and date already exists.");
        }
        if (money && p.isVerified()) {   // changed money details must be checked again
            changes.add(change(p, "verified", "true", "false", "Changed after verification", staff, now));
            p.clearVerification();
        }
        p.setBenefitType(type); p.setAmount(amount); p.setScheduledDate(date); p.setPayeeName(payee);
        save(() -> repo.updatePayment(p, changes));
        return p;
    }

    /** SR-6: confirm a completed payment reached the constituent. Not by the person who recorded it. */
    public Payment verifyPayment(StaffMember staff, int paymentId) throws PaymentException {
        if (staff == null || !staff.canVerifyPayments())
            throw new PaymentException("Only the Office Manager or the MP can verify a payment.");
        Payment p = requirePayment(paymentId);
        if (p.getStatus() != PaymentStatus.COMPLETED)
            throw new PaymentException("Only a Completed payment can be verified.");
        if (p.isVerified()) throw new PaymentException("This payment is already verified.");
        if (p.getRecordedBy() == staff.getId())
            throw new PaymentException("The person who recorded a payment cannot verify it.");

        LocalDateTime now = LocalDateTime.now();
        List<PaymentChange> changes = List.of(change(p, "verified", "false", "true", "Verified", staff, now));
        p.markVerified(staff.getId(), now);
        save(() -> repo.updatePayment(p, changes));
        return p;
    }

    /** SR-4: every payment for one constituent, newest first. */
    public List<Payment> getPaymentHistory(int applicantId) { return repo.findPaymentsByApplicant(applicantId); }

    public List<PaymentChange> getChanges(int paymentId) { return repo.findPaymentChanges(paymentId); }

    /** SR-7: total assistance distributed (Completed payments) between two dates, inclusive. */
    public PaymentSummary getSummary(LocalDate from, LocalDate to) throws PaymentException {
        if (from == null || to == null) throw new PaymentException("Enter both dates.");
        if (to.isBefore(from)) throw new PaymentException("The end date cannot be before the start date.");
        BigDecimal total = BigDecimal.ZERO;
        Map<BenefitType, BigDecimal> byType = new EnumMap<>(BenefitType.class);
        List<Payment> done = repo.findCompletedPaymentsBetween(from, to);
        for (Payment p : done) {
            total = total.add(p.getAmount());
            byType.merge(p.getBenefitType(), p.getAmount(), BigDecimal::add);
        }
        return new PaymentSummary(from, to, total, done.size(), byType);
    }

    // ---------- helpers ----------
    private void requireCanRecord(StaffMember staff) throws PaymentException {
        if (staff == null || !staff.canRecordPayments())
            throw new PaymentException("You are not authorised to record or change payments.");
    }

    private Payment requirePayment(int id) throws PaymentException {
        Payment p = repo.findPayment(id);
        if (p == null) throw new PaymentException("That payment was not found.");
        return p;
    }

    /** Until the Administration & Approval table exists: approved = has an Active or Completed enrolment. */
    private boolean isApprovedFor(int applicantId, int programmeId) {
        for (Enrolment e : repo.findEnrolmentsForApplicant(applicantId))
            if (e.getProgrammeId() == programmeId
                    && (e.getStatus() == EnrolmentStatus.ACTIVE || e.getStatus() == EnrolmentStatus.COMPLETED))
                return true;
        return false;
    }

    private PaymentChange change(Payment p, String field, String oldV, String newV, String why,
                                 StaffMember staff, LocalDateTime when) {
        return new PaymentChange(0, p.getId(), field, oldV, newV, why, staff.getId(), when);
    }

    private void save(Runnable action) throws PaymentException {
        try { action.run(); }
        catch (RuntimeException e) { throw new PaymentException("The payment could not be saved. No changes were made."); }
    }
}
