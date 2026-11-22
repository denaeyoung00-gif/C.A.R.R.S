package carrs.service;

import static org.junit.Assert.*;

import carrs.model.*;
import carrs.repository.InMemoryCarrsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class PaymentServiceTest {
    InMemoryCarrsRepository repo;
    PaymentService service;
    StaffMember manager, staff, attendant, mp;
    LocalDate date = LocalDate.of(2026, 11, 10);

    @Before public void setUp() {
        repo = new InMemoryCarrsRepository();
        service = new PaymentService(repo);
        manager   = new StaffMember(1, "Alexia James", Role.OFFICE_MANAGER);
        staff     = new StaffMember(2, "Office Staff", Role.OFFICE_STAFF);
        attendant = new StaffMember(3, "Sam Intern", Role.OFFICE_ATTENDANT);
        mp        = new StaffMember(4, "Dr Brown-Burke", Role.MEMBER_OF_PARLIAMENT);
        repo.add(new Applicant(1, "Jane Brown"));
        repo.add(new Applicant(2, "Tom Clarke"));        // not enrolled anywhere
        repo.add(new Programme(1, "Tertiary Scholarship", ProgrammeStatus.ACTIVE));
        repo.add(new Programme(2, "Emergency Assistance", ProgrammeStatus.ACTIVE));
        repo.add(new Enrolment(100, 1, 1, EnrolmentStatus.ACTIVE, LocalDateTime.now(), null));
    }

    private Payment record() throws PaymentException {
        return service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT,
                new BigDecimal("5000"), date, null);
    }
    private Payment completed() throws PaymentException {
        Payment p = record();
        service.updateStatus(staff, p.getId(), PaymentStatus.PROCESSED, null);
        return service.updateStatus(staff, p.getId(), PaymentStatus.COMPLETED, null);
    }
    private void assertBlocked(PaymentException e, String text) {
        assertTrue("message was: " + e.getMessage(), e.getMessage().contains(text));
    }

    // ---- recording (AC-1)
    @Test public void recordsAPaymentAsPending() throws Exception {
        Payment p = record();
        assertTrue(p.getId() > 0);
        assertEquals(PaymentStatus.PENDING, p.getStatus());
        assertEquals(new BigDecimal("5000.00"), p.getAmount());
        assertEquals("Jane Brown", p.getPayeeName());          // defaults to the constituent
        assertEquals(1, service.getPaymentHistory(1).size());
    }
    @Test public void payeeCanBeAnInstitution() throws Exception {
        Payment p = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT,
                new BigDecimal("80000"), date, "University of the West Indies");
        assertEquals("University of the West Indies", p.getPayeeName());
    }
    @Test public void attendantCannotRecord() {
        try { service.recordPayment(attendant, 1, 1, BenefitType.TUITION_SUPPORT, BigDecimal.TEN, date, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not authorised"); }
        assertTrue(service.getPaymentHistory(1).isEmpty());
    }
    @Test public void amountMustBeAboveZero() {
        for (BigDecimal bad : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-5"), null}) {
            try { service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT, bad, date, null); fail(); }
            catch (PaymentException e) { assertBlocked(e, "more than zero"); }
        }
    }
    @Test public void benefitTypeAndDateAreRequired() {
        try { service.recordPayment(staff, 1, 1, null, BigDecimal.TEN, date, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "benefit type"); }
        try { service.recordPayment(staff, 1, 1, BenefitType.OTHER, BigDecimal.TEN, null, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "date"); }
    }
    @Test public void notApprovedConstituentIsRejected() {
        try { service.recordPayment(staff, 2, 1, BenefitType.TUITION_SUPPORT, BigDecimal.TEN, date, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not approved"); }
        try { service.recordPayment(staff, 1, 2, BenefitType.EMERGENCY_GRANT, BigDecimal.TEN, date, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not approved"); }   // enrolled in a different programme
    }
    @Test public void duplicatePaymentIsBlocked() throws Exception {
        record();
        try { record(); fail(); } catch (PaymentException e) { assertBlocked(e, "already exists"); }
        assertEquals(1, service.getPaymentHistory(1).size());
    }
    @Test public void aFailedPaymentDoesNotBlockANewOne() throws Exception {
        Payment p = record();
        service.updateStatus(staff, p.getId(), PaymentStatus.FAILED, "Bank rejected the transfer");
        record();   // allowed
        assertEquals(2, service.getPaymentHistory(1).size());
    }

    // ---- status (AC-2 / AC-5)
    @Test public void statusMovesInOrderAndSetsDistributedDate() throws Exception {
        Payment p = completed();
        assertEquals(PaymentStatus.COMPLETED, p.getStatus());
        assertEquals(LocalDate.now(), p.getDistributedDate());
        assertTrue(p.isDistributed());
    }
    @Test public void cannotSkipAStatus() throws Exception {
        Payment p = record();
        try { service.updateStatus(staff, p.getId(), PaymentStatus.COMPLETED, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "cannot be changed"); }
        assertEquals(PaymentStatus.PENDING, repo.findPayment(p.getId()).getStatus());
    }
    @Test public void completedAndFailedAreFinal() throws Exception {
        Payment p = completed();
        try { service.updateStatus(staff, p.getId(), PaymentStatus.FAILED, "x"); fail(); }
        catch (PaymentException e) { assertBlocked(e, "cannot be changed"); }
    }
    @Test public void failedNeedsAReasonAndStoresIt() throws Exception {
        Payment p = record();
        try { service.updateStatus(staff, p.getId(), PaymentStatus.FAILED, "  "); fail(); }
        catch (PaymentException e) { assertBlocked(e, "reason"); }
        Payment failed = service.updateStatus(staff, p.getId(), PaymentStatus.FAILED, "Bank rejected the transfer");
        assertEquals("Bank rejected the transfer", failed.getFailureReason());
    }
    @Test public void everyStatusChangeIsRecorded() throws Exception {
        Payment p = completed();
        List<PaymentChange> log = service.getChanges(p.getId());
        assertEquals("status", log.get(0).getFieldName());
        assertEquals("PENDING", log.get(0).getOldValue());
        assertEquals("PROCESSED", log.get(0).getNewValue());
        assertEquals(3, log.size());   // status, status, distributedDate
    }

    // ---- verification (AC-6)
    @Test public void onlyACompletedPaymentCanBeVerified() throws Exception {
        Payment p = record();
        try { service.verifyPayment(manager, p.getId()); fail(); }
        catch (PaymentException e) { assertBlocked(e, "Completed"); }
    }
    @Test public void officeStaffCannotVerify() throws Exception {
        Payment p = completed();
        try { service.verifyPayment(staff, p.getId()); fail(); }
        catch (PaymentException e) { assertBlocked(e, "Office Manager or the MP"); }
    }
    @Test public void theRecorderCannotVerifyTheirOwnPayment() throws Exception {
        Payment p = service.recordPayment(manager, 1, 1, BenefitType.TUITION_SUPPORT, BigDecimal.TEN, date, null);
        service.updateStatus(manager, p.getId(), PaymentStatus.PROCESSED, null);
        service.updateStatus(manager, p.getId(), PaymentStatus.COMPLETED, null);
        try { service.verifyPayment(manager, p.getId()); fail(); }
        catch (PaymentException e) { assertBlocked(e, "cannot verify it"); }
    }
    @Test public void managerCanVerifyAnotherPersonsPayment() throws Exception {
        Payment p = completed();                       // recorded by staff
        Payment v = service.verifyPayment(manager, p.getId());
        assertTrue(v.isVerified());
        assertEquals(Integer.valueOf(1), v.getVerifiedBy());
        assertNotNull(v.getVerifiedAt());
        try { service.verifyPayment(mp, p.getId()); fail(); }
        catch (PaymentException e) { assertBlocked(e, "already verified"); }
    }

    // ---- corrections (AC-7)
    @Test public void correctionKeepsTheOldValue() throws Exception {
        Payment p = record();
        service.correctPayment(staff, p.getId(), null, new BigDecimal("4500"), null, null, "Amount keyed in wrongly");
        assertEquals(new BigDecimal("4500.00"), repo.findPayment(p.getId()).getAmount());
        PaymentChange c = service.getChanges(p.getId()).get(0);
        assertEquals("amount", c.getFieldName());
        assertEquals("5000.00", c.getOldValue());
        assertEquals("4500.00", c.getNewValue());
        assertEquals("Amount keyed in wrongly", c.getReason());
    }
    @Test public void correctionNeedsAReasonOfTenCharacters() throws Exception {
        Payment p = record();
        try { service.correctPayment(staff, p.getId(), null, new BigDecimal("1"), null, null, "123456789"); fail(); }
        catch (PaymentException e) { assertBlocked(e, "at least 10"); }
        service.correctPayment(staff, p.getId(), null, new BigDecimal("1"), null, null, "1234567890");  // exactly 10: ok
    }
    @Test public void correctingAVerifiedAmountClearsVerification() throws Exception {
        Payment p = completed();
        service.verifyPayment(manager, p.getId());
        Payment fixed = service.correctPayment(staff, p.getId(), null, new BigDecimal("4000"), null, null, "Adjusted after review");
        assertFalse(fixed.isVerified());
        assertNull(fixed.getVerifiedBy());
    }
    @Test public void correctingOnlyThePayeeKeepsVerification() throws Exception {
        Payment p = completed();
        service.verifyPayment(manager, p.getId());
        Payment fixed = service.correctPayment(staff, p.getId(), null, null, null, "UTech", "Wrong institution named");
        assertTrue(fixed.isVerified());
    }
    @Test public void aCorrectionThatChangesNothingIsRejected() throws Exception {
        Payment p = record();
        try { service.correctPayment(staff, p.getId(), null, null, null, null, "No real change here"); fail(); }
        catch (PaymentException e) { assertBlocked(e, "Nothing was changed"); }
    }
    @Test public void correctionCannotCreateADuplicate() throws Exception {
        record();
        Payment other = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT,
                BigDecimal.TEN, date.plusDays(5), null);
        try { service.correctPayment(staff, other.getId(), null, null, date, null, "Moving it back a week"); fail(); }
        catch (PaymentException e) { assertBlocked(e, "already exists"); }
    }
    @Test public void aFailedPaymentCannotBeCorrected() throws Exception {
        Payment p = record();
        service.updateStatus(staff, p.getId(), PaymentStatus.FAILED, "Bank rejected the transfer");
        try { service.correctPayment(staff, p.getId(), null, new BigDecimal("1"), null, null, "Trying to fix it"); fail(); }
        catch (PaymentException e) { assertBlocked(e, "failed payment"); }
    }

    // ---- a database failure changes nothing
    @Test public void aFailedSaveChangesNothing() throws Exception {
        Payment p = record();
        repo.failOnSave = true;
        try { service.updateStatus(staff, p.getId(), PaymentStatus.PROCESSED, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "No changes were made"); }
        repo.failOnSave = false;
        assertEquals(PaymentStatus.PENDING, repo.findPayment(p.getId()).getStatus());
        assertTrue(service.getChanges(p.getId()).isEmpty());
    }

    // ---- summary (AC-4 / payment records)
    @Test public void summaryTotalsOnlyCompletedPaymentsInThePeriod() throws Exception {
        Payment a = completed();                                         // 5000 completed today
        Payment b = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT,
                new BigDecimal("2000"), date.plusDays(1), null);          // stays Pending: not counted
        PaymentSummary s = service.getSummary(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        assertEquals(new BigDecimal("5000.00"), s.getTotalDistributed());
        assertEquals(1, s.getPaymentCount());
        assertEquals(new BigDecimal("5000.00"), s.getTotalByBenefitType().get(BenefitType.TUITION_SUPPORT));
        PaymentSummary none = service.getSummary(LocalDate.now().plusDays(2), LocalDate.now().plusDays(9));
        assertEquals(0, none.getPaymentCount());
    }
    @Test public void summaryRejectsBackwardsDates() {
        try { service.getSummary(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)); fail(); }
        catch (PaymentException e) { assertBlocked(e, "cannot be before"); }
    }
}
