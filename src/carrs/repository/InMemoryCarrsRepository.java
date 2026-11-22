package carrs.repository;

import carrs.model.*;
import java.time.LocalDate;
import java.util.*;

/** Fake repository used by the JUnit tests, so tests run without MySQL. */
public class InMemoryCarrsRepository implements CarrsRepository {
    private final Map<Integer, Applicant> applicants = new LinkedHashMap<>();
    private final Map<Integer, Programme> programmes = new LinkedHashMap<>();
    private final Map<Integer, Enrolment> enrolments = new LinkedHashMap<>();
    private final Map<Integer, Payment> payments = new LinkedHashMap<>();
    private final List<PaymentChange> changes = new ArrayList<>();
    private int nextPaymentId = 1;
    private int nextChangeId = 1;
    public boolean failOnSave = false;   // lets a test simulate a database crash

    public void add(Applicant a) { applicants.put(a.getId(), a); }
    public void add(Programme p) { programmes.put(p.getId(), p); }
    public void add(Enrolment e) { enrolments.put(e.getId(), e); }

    @Override public Applicant findApplicant(int id) { return applicants.get(id); }
    @Override public List<Applicant> findAllApplicants() { return new ArrayList<>(applicants.values()); }
    @Override public Programme findProgramme(int id) { return programmes.get(id); }
    @Override public Enrolment findEnrolment(int id) {
        Enrolment e = enrolments.get(id);
        return e == null ? null : e.copy();
    }
    @Override public List<Enrolment> findEnrolmentsForApplicant(int applicantId) {
        List<Enrolment> out = new ArrayList<>();
        for (Enrolment e : enrolments.values())
            if (e.getApplicantId() == applicantId) out.add(e.copy());
        return out;
    }
    @Override public List<Programme> findActiveProgrammes() {
        List<Programme> out = new ArrayList<>();
        for (Programme p : programmes.values()) if (p.isActive()) out.add(p);
        return out;
    }
    @Override public int countLiveEnrolments(int programmeId) {
        int n = 0;
        for (Enrolment e : enrolments.values())
            if (e.getProgrammeId() == programmeId && e.isLive()) n++;
        return n;
    }

    // ---- payments
    @Override public void savePayment(Payment p) {
        if (failOnSave) throw new RuntimeException("Simulated database failure");
        p.setId(nextPaymentId++);
        payments.put(p.getId(), p.copy());
    }
    @Override public void updatePayment(Payment p, List<PaymentChange> newChanges) {
        if (failOnSave) throw new RuntimeException("Simulated database failure");
        payments.put(p.getId(), p.copy());
        for (PaymentChange c : newChanges) { c.setId(nextChangeId++); changes.add(c); }
    }
    @Override public Payment findPayment(int id) {
        Payment p = payments.get(id);
        return p == null ? null : p.copy();
    }
    @Override public List<Payment> findPaymentsByApplicant(int applicantId) {
        List<Payment> out = new ArrayList<>();
        for (Payment p : payments.values()) if (p.getApplicantId() == applicantId) out.add(p.copy());
        out.sort(Comparator.comparing(Payment::getScheduledDate).reversed());
        return out;
    }
    @Override public Payment findLivePayment(int applicantId, int programmeId, BenefitType type, LocalDate date) {
        for (Payment p : payments.values())
            if (p.getApplicantId() == applicantId && p.getProgrammeId() == programmeId
                    && p.getBenefitType() == type && p.getScheduledDate().equals(date)
                    && p.getStatus() != PaymentStatus.FAILED) return p.copy();
        return null;
    }
    @Override public List<Payment> findCompletedPaymentsBetween(LocalDate from, LocalDate to) {
        List<Payment> out = new ArrayList<>();
        for (Payment p : payments.values())
            if (p.getStatus() == PaymentStatus.COMPLETED && p.getDistributedDate() != null
                    && !p.getDistributedDate().isBefore(from) && !p.getDistributedDate().isAfter(to))
                out.add(p.copy());
        return out;
    }
    @Override public List<PaymentChange> findPaymentChanges(int paymentId) {
        List<PaymentChange> out = new ArrayList<>();
        for (PaymentChange c : changes) if (c.getPaymentId() == paymentId) out.add(c);
        return out;
    }
}
