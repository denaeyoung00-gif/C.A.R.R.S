package carrs.service;

import carrs.model.*;
import carrs.repository.CarrsRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** The rules for Payment and Benefit Management. Talks to data only through CarrsRepository. */
public class PaymentService {
    public static final int MIN_REASON_LENGTH = 10;
    public static final long MAX_SCAN_BYTES = 10L * 1024 * 1024;          // 10 MB, as in the SRS
    public static final Set<String> SCAN_TYPES = Set.of("pdf", "png", "jpg", "jpeg");

    private final CarrsRepository repo;
    private final Clock clock;

    public PaymentService(CarrsRepository repo) { this(repo, Clock.systemDefaultZone()); }
    /** The clock can be replaced so tests can pretend it is a different day. */
    public PaymentService(CarrsRepository repo, Clock clock) { this.repo = repo; this.clock = clock; }

    private LocalDate today() { return LocalDate.now(clock); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }

    // ---------------------------------------------------------------- viewing / searching
    /** Programmes this constituent can be paid from: approved (active/completed enrolment) AND the programme pays benefits. */
    public List<Programme> getEligibleProgrammes(int applicantId) {
        List<Programme> out = new ArrayList<>();
        for (Enrolment e : repo.findEnrolmentsForApplicant(applicantId)) {
            Programme p = repo.findProgramme(e.getProgrammeId());
            if (p != null && p.providesBenefits() && p.isActive()
                    && (e.getStatus() == EnrolmentStatus.ACTIVE || e.getStatus() == EnrolmentStatus.COMPLETED)
                    && out.stream().noneMatch(x -> x.getId() == p.getId()))
                out.add(p);
        }
        return out;
    }

    /** Search the constituents by name or ID number. Blank text returns nobody: staff must type something. */
    public List<Applicant> searchApplicants(StaffMember staff, String text) throws PaymentException {
        requireCanView(staff);
        String q = text == null ? "" : text.trim().toLowerCase();
        List<Applicant> out = new ArrayList<>();
        for (Applicant a : repo.findAllApplicants())
            if (!q.isEmpty() && (a.getName().toLowerCase().contains(q) || String.valueOf(a.getId()).equals(q))) out.add(a);
        return out;
    }

    /** Payment history for one constituent, newest first. Only authorised staff may view it. */
    public List<Payment> getPaymentHistory(StaffMember staff, int applicantId) throws PaymentException {
        requireCanView(staff);
        return repo.findPaymentsByApplicant(applicantId);
    }
    /** Same list without the permission check (used by the service's own tests and reports). */
    public List<Payment> getPaymentHistory(int applicantId) { return repo.findPaymentsByApplicant(applicantId); }

    public List<PaymentChange> getChanges(int paymentId) { return repo.findPaymentChanges(paymentId); }

    /** Audit trail: who did what to every payment of this constituent, and when. */
    public List<PaymentAudit> getAuditTrail(StaffMember staff, int applicantId) throws PaymentException {
        requireCanView(staff);
        return repo.findAuditForApplicant(applicantId);
    }

    // ---------------------------------------------------------------- recording
    /** Record a new payment. It starts as PENDING. */
    public Payment recordPayment(StaffMember staff, int applicantId, int programmeId,
                                 BenefitType type, BigDecimal amount, LocalDate scheduledDate,
                                 String payeeName) throws PaymentException {
        requireCanRecord(staff);
        Applicant applicant = repo.findApplicant(applicantId);
        if (applicant == null) throw new PaymentException("That constituent was not found.");
        Programme programme = repo.findProgramme(programmeId);
        if (programme == null) throw new PaymentException("That programme was not found.");
        if (!programme.providesBenefits())
            throw new PaymentException("That programme does not provide financial benefits, so no payment can be recorded.");
        if (type == null) throw new PaymentException("Choose a benefit type.");
        if (amount == null || amount.signum() <= 0) throw new PaymentException("The amount must be more than zero.");
        if (scheduledDate == null) throw new PaymentException("Enter a valid scheduled payment date.");
        if (!isApprovedFor(applicantId, programmeId))
            throw new PaymentException("This constituent is not approved and actively enrolled in that programme, so no payment can be recorded.");
        if (repo.findLivePayment(applicantId, programmeId, type, scheduledDate) != null)
            throw new PaymentException("A payment for this constituent, programme, benefit and date already exists.");

        String payee = (payeeName == null || payeeName.isBlank()) ? applicant.getName() : payeeName.trim();
        Payment p = new Payment(0, applicantId, programmeId, type, PaymentMethod.CHEQUE, amount.setScale(2, java.math.RoundingMode.HALF_UP),
                scheduledDate, null, PaymentStatus.PENDING, null, payee,
                false, null, null, staff.getId(), now(), null);
        PaymentAudit audit = audit(null, "RECORDED", "Recorded cheque payment of $" + p.getAmount().toPlainString()
                + " (" + type + ") scheduled for " + scheduledDate + " to " + payee, staff);
        save(() -> repo.savePayment(p, audit));
        return p;
    }

    // ---------------------------------------------------------------- status
    /** Move a payment along: Pending -> Processed -> Completed, or to Failed (a reason is required). */
    public Payment updateStatus(StaffMember staff, int paymentId, PaymentStatus next, String note)
            throws PaymentException {
        requireCanRecord(staff);
        Payment p = requirePayment(paymentId);
        if (next == null) throw new PaymentException("Choose a new status.");
        if (!p.getStatus().canTransitionTo(next))
            throw new PaymentException("A " + p.getStatus() + " payment cannot be changed to " + next + ".");
        if (next == PaymentStatus.FAILED && (note == null || note.isBlank()))
            throw new PaymentException("Enter the reason the payment failed.");
        if ((next == PaymentStatus.PROCESSED || next == PaymentStatus.COMPLETED) && p.getScheduledDate().isAfter(today()))
            throw new PaymentException("This payment is scheduled for " + p.getScheduledDate()
                    + ". It stays Pending until that date.");
        if (next == PaymentStatus.COMPLETED && !p.hasChequeScan())
            throw new PaymentException("Attach the scanned copy of the cheque before marking this payment Completed.");

        LocalDateTime now = now();
        List<PaymentChange> changes = new ArrayList<>();
        String why = (note == null || note.isBlank()) ? "Status update" : note.trim();
        changes.add(change(p, "status", p.getStatus().name(), next.name(), why, staff, now));
        PaymentStatus old = p.getStatus();
        p.setStatus(next);
        if (next == PaymentStatus.COMPLETED) {
            p.setDistributedDate(today());
            changes.add(change(p, "distributedDate", null, p.getDistributedDate().toString(), why, staff, now));
        }
        if (next == PaymentStatus.FAILED) p.setFailureReason(note.trim());
        PaymentAudit audit = audit(p.getId(), "STATUS_CHANGED", "Status " + old + " -> " + next + ". " + why, staff);
        save(() -> repo.updatePayment(p, changes, audit));
        return p;
    }

    // ---------------------------------------------------------------- correcting
    /** Correct a payment. The old values are kept as change records. A reason is required. */
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

        LocalDateTime now = now();
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
        StringBuilder fields = new StringBuilder();
        for (PaymentChange c : changes) fields.append(fields.length() == 0 ? "" : ", ").append(c.getFieldName());
        PaymentAudit audit = audit(p.getId(), "CORRECTED", "Corrected " + fields + ". Reason: " + why, staff);
        p.setBenefitType(type); p.setAmount(amount); p.setScheduledDate(date); p.setPayeeName(payee);
        save(() -> repo.updatePayment(p, changes, audit));
        return p;
    }

    // ---------------------------------------------------------------- verifying
    /** Confirm a completed payment reached the constituent. Not by the person who recorded it. */
    public Payment verifyPayment(StaffMember staff, int paymentId) throws PaymentException {
        if (staff == null || !staff.canVerifyPayments())
            throw new PaymentException("Only the Office Manager or the MP can verify a payment.");
        Payment p = requirePayment(paymentId);
        if (p.getStatus() != PaymentStatus.COMPLETED)
            throw new PaymentException("Only a Completed payment can be verified.");
        if (p.isVerified()) throw new PaymentException("This payment is already verified.");
        if (p.getRecordedBy() == staff.getId())
            throw new PaymentException("The person who recorded a payment cannot verify it.");

        LocalDateTime now = now();
        List<PaymentChange> changes = List.of(change(p, "verified", "false", "true", "Verified", staff, now));
        p.markVerified(staff.getId(), now);
        PaymentAudit audit = audit(p.getId(), "VERIFIED", "Payment verified", staff);
        save(() -> repo.updatePayment(p, changes, audit));
        return p;
    }

    // ---------------------------------------------------------------- cheques (use case step 4)
    /** Make the cheque for a payment, ready to print. Only on or after the scheduled date, and only for cheque payments. */
    public String printCheque(StaffMember staff, int paymentId) throws PaymentException {
        requireCanRecord(staff);
        Payment p = requirePayment(paymentId);
        if (p.getStatus() == PaymentStatus.FAILED || p.getStatus() == PaymentStatus.COMPLETED)
            throw new PaymentException("A " + p.getStatus() + " payment cannot have a cheque printed.");
        if (p.getScheduledDate().isAfter(today()))
            throw new PaymentException("The cheque can only be printed on or after the scheduled date (" + p.getScheduledDate() + ").");
        Applicant a = repo.findApplicant(p.getApplicantId());
        Programme pr = repo.findProgramme(p.getProgrammeId());
        String text = "OFFICE OF THE MEMBER OF PARLIAMENT - DR ANGELA BROWN-BURKE\n"
                + "----------------------------------------------------------\n"
                + "CHEQUE (payment ref " + p.getId() + ")\n\n"
                + "Date:       " + today() + "\n"
                + "Pay to:     " + p.getPayeeName() + "\n"
                + "Amount:     $" + p.getAmount().toPlainString() + "\n"
                + "Benefit:    " + p.getBenefitType() + "\n"
                + "Programme:  " + (pr == null ? "" : pr.getName()) + "\n"
                + "Constituent: " + (a == null ? "" : a.getName()) + "\n\n"
                + "Authorised by: ______________________\n";
        saveAuditLine(audit(p.getId(), "CHEQUE_PRINTED", "Cheque printed for $" + p.getAmount().toPlainString()
                + " payable to " + p.getPayeeName(), staff));
        return text;
    }

    /**
     * Attach the scanned copy of the cheque to the payment. PDF, PNG or JPEG, up to 10 MB.
     * The screen supplies the file path and size; the file itself stays in the office's shared folder.
     */
    public Payment attachChequeScan(StaffMember staff, int paymentId, String filePath, long sizeBytes)
            throws PaymentException {
        requireCanRecord(staff);
        Payment p = requirePayment(paymentId);
        if (p.getStatus() == PaymentStatus.FAILED)
            throw new PaymentException("A failed payment cannot have a scanned cheque.");
        if (filePath == null || filePath.isBlank()) throw new PaymentException("Choose the scanned cheque file.");
        int dot = filePath.lastIndexOf('.');
        String ext = dot < 0 ? "" : filePath.substring(dot + 1).toLowerCase();
        if (!SCAN_TYPES.contains(ext))
            throw new PaymentException("The scan must be a PDF, PNG or JPEG file.");
        if (sizeBytes <= 0) throw new PaymentException("The scan file is empty.");
        if (sizeBytes > MAX_SCAN_BYTES) throw new PaymentException("The scan is larger than 10 MB.");

        LocalDateTime now = now();
        List<PaymentChange> changes = List.of(change(p, "chequeScan", p.getChequeScanPath(), filePath, "Scanned cheque attached", staff, now));
        p.setChequeScanPath(filePath);
        PaymentAudit audit = audit(p.getId(), "CHEQUE_SCAN_ATTACHED", "Scanned cheque attached: " + filePath, staff);
        save(() -> repo.updatePayment(p, changes, audit));
        return p;
    }

    // ---------------------------------------------------------------- summary
    /** Total assistance distributed (Completed payments) between two dates, inclusive. */
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

    // ---------------------------------------------------------------- helpers
    private void requireCanRecord(StaffMember staff) throws PaymentException {
        if (staff == null || !staff.canRecordPayments())
            throw new PaymentException("You are not authorised to record or change payments.");
    }
    private void requireCanView(StaffMember staff) throws PaymentException {
        if (staff == null || !staff.canViewPayments())
            throw new PaymentException("You are not authorised to view or manage payments.");
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
    private PaymentAudit audit(Integer paymentId, String action, String details, StaffMember staff) {
        return new PaymentAudit(0, paymentId, action, details, staff.getId(), now());
    }

    private void save(Runnable action) throws PaymentException {
        try { action.run(); }
        catch (RuntimeException e) { throw new PaymentException("The payment could not be saved. No changes were made."); }
    }
    private void saveAuditLine(PaymentAudit a) throws PaymentException { save(() -> repo.saveAudit(a)); }
}
