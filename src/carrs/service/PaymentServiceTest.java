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
    LocalDate date = LocalDate.now().minusDays(3);   // a past date, so the payment is due

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
    /** The whole cheque flow: print, attach the scan, Processed, Completed. */
    private Payment complete(StaffMember who, Payment p) throws PaymentException {
        service.printCheque(who, p.getId());
        service.attachChequeScan(who, p.getId(), "scans/cheque" + p.getId() + ".pdf", 150_000);
        service.updateStatus(who, p.getId(), PaymentStatus.PROCESSED, null);
        return service.updateStatus(who, p.getId(), PaymentStatus.COMPLETED, null);
    }
    private Payment completed() throws PaymentException { return complete(staff, record()); }
    private Payment recordCheque() throws PaymentException {
        return service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT, new BigDecimal("5000"), date, null);
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
        Payment p = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT, new BigDecimal("80000"), date, "University of the West Indies");
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
        assertEquals("chequeScan", log.get(0).getFieldName());
        assertEquals("status", log.get(1).getFieldName());
        assertEquals("PENDING", log.get(1).getOldValue());
        assertEquals("PROCESSED", log.get(1).getNewValue());
        assertEquals(4, log.size());   // chequeScan, status, status, distributedDate
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
        complete(manager, p);
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
        Payment other = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT, BigDecimal.TEN, date.plusDays(5), null);
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
        Payment b = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT, new BigDecimal("2000"), date.plusDays(1), null);          // stays Pending: not counted
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

    // ---- payment method: cheque is the only method
    @Test public void everyPaymentIsAChequePayment() throws Exception {
        assertEquals(PaymentMethod.CHEQUE, record().getPaymentMethod());
    }
    @Test public void programmeThatPaysNothingBlocksPayments() throws Exception {
        repo.add(new Programme(3, "Training Workshop", ProgrammeStatus.ACTIVE).paysBenefits(false));
        repo.add(new Enrolment(101, 1, 3, EnrolmentStatus.ACTIVE, LocalDateTime.now(), null));
        try { service.recordPayment(staff, 1, 3, BenefitType.OTHER, BigDecimal.TEN, date, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "does not provide financial benefits"); }
    }
    @Test public void withdrawnEnrolmentCannotBePaid() throws Exception {
        repo.add(new Applicant(3, "Mary Smith"));
        repo.add(new Enrolment(102, 3, 1, EnrolmentStatus.WITHDRAWN, LocalDateTime.now(), null));
        try { service.recordPayment(staff, 3, 1, BenefitType.TUITION_SUPPORT, BigDecimal.TEN, date, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not approved"); }
    }

    // ---- eligibility and search (use case step 1)
    @Test public void onlyEnrolledConstituentsAreEligible() {
        assertEquals(1, service.getEligibleProgrammes(1).size());
        assertEquals("Tertiary Scholarship", service.getEligibleProgrammes(1).get(0).getName());
        assertTrue(service.getEligibleProgrammes(2).isEmpty());       // Tom is not enrolled
    }
    @Test public void programmesThatPayNothingAreNotEligible() {
        repo.add(new Programme(3, "Training Workshop", ProgrammeStatus.ACTIVE).paysBenefits(false));
        repo.add(new Enrolment(101, 2, 3, EnrolmentStatus.ACTIVE, LocalDateTime.now(), null));
        assertTrue(service.getEligibleProgrammes(2).isEmpty());
    }
    @Test public void searchFindsByNameOrId() throws Exception {
        assertEquals(1, service.searchApplicants(staff, "jane").size());
        assertEquals(1, service.searchApplicants(staff, "2").size());
        assertEquals(0, service.searchApplicants(staff, "zzz").size());
        assertEquals(0, service.searchApplicants(staff, "  ").size());   // must type something
    }
    @Test public void attendantCannotViewOrSearch() throws Exception {
        try { service.getPaymentHistory(attendant, 1); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not authorised"); }
        try { service.searchApplicants(attendant, ""); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not authorised"); }
        try { service.getAuditTrail(attendant, 1); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not authorised"); }
        assertEquals(0, service.getPaymentHistory(staff, 1).size());   // staff may
    }

    // ---- scheduling: stays Pending until the date
    @Test public void futurePaymentStaysPendingUntilItsDate() throws Exception {
        Payment p = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT, BigDecimal.TEN, LocalDate.now().plusDays(5), null);
        assertFalse(p.isDue(LocalDate.now()));
        assertTrue(p.isDue(LocalDate.now().plusDays(5)));
        for (PaymentStatus next : new PaymentStatus[]{PaymentStatus.PROCESSED}) {
            try { service.updateStatus(staff, p.getId(), next, null); fail(); }
            catch (PaymentException e) { assertBlocked(e, "stays Pending"); }
        }
        assertEquals(PaymentStatus.PENDING, repo.findPayment(p.getId()).getStatus());
        service.updateStatus(staff, p.getId(), PaymentStatus.FAILED, "Programme paused");   // failing early is fine
    }
    @Test public void pastDatedPaymentIsDue() throws Exception {
        assertTrue(record().isDue(LocalDate.now()));
    }

    // ---- cheques (use case: print the cheque, upload the scan, then update the status)
    @Test public void chequeCannotBeCompletedWithoutTheScan() throws Exception {
        Payment p = recordCheque();
        service.updateStatus(staff, p.getId(), PaymentStatus.PROCESSED, null);
        try { service.updateStatus(staff, p.getId(), PaymentStatus.COMPLETED, null); fail(); }
        catch (PaymentException e) { assertBlocked(e, "scanned copy of the cheque"); }
        service.attachChequeScan(staff, p.getId(), "C:/scans/cheque1.pdf", 200_000);
        assertEquals(PaymentStatus.COMPLETED, service.updateStatus(staff, p.getId(), PaymentStatus.COMPLETED, null).getStatus());
    }
    @Test public void scanMustBeAllowedTypeAndSize() throws Exception {
        Payment p = recordCheque();
        try { service.attachChequeScan(staff, p.getId(), "cheque.zip", 1000); fail(); }
        catch (PaymentException e) { assertBlocked(e, "PDF, PNG or JPEG"); }
        try { service.attachChequeScan(staff, p.getId(), "cheque.xlsx", 1000); fail(); }
        catch (PaymentException e) { assertBlocked(e, "PDF, PNG or JPEG"); }
        try { service.attachChequeScan(staff, p.getId(), "cheque.pdf", 11L * 1024 * 1024); fail(); }
        catch (PaymentException e) { assertBlocked(e, "larger than 10 MB"); }
        try { service.attachChequeScan(staff, p.getId(), "", 1000); fail(); }
        catch (PaymentException e) { assertBlocked(e, "Choose the scanned cheque"); }
        assertFalse(repo.findPayment(p.getId()).hasChequeScan());
        service.attachChequeScan(staff, p.getId(), "cheque.JPG", 9L * 1024 * 1024);
        assertTrue(repo.findPayment(p.getId()).hasChequeScan());
    }
    @Test public void chequeTextHasThePayeeAndAmount() throws Exception {
        Payment p = recordCheque();
        String text = service.printCheque(staff, p.getId());
        assertTrue(text.contains("Jane Brown"));
        assertTrue(text.contains("$5000.00"));
    }

    @Test public void chequeOnlyOnOrAfterTheScheduledDateAndOnlyByAuthorisedStaff() throws Exception {
        Payment later = service.recordPayment(staff, 1, 1, BenefitType.TUITION_SUPPORT,
                BigDecimal.TEN, LocalDate.now().plusDays(4), null);
        try { service.printCheque(staff, later.getId()); fail(); }
        catch (PaymentException e) { assertBlocked(e, "on or after the scheduled date"); }
        try { service.printCheque(attendant, later.getId()); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not authorised"); }
        try { service.attachChequeScan(attendant, later.getId(), "x.pdf", 10); fail(); }
        catch (PaymentException e) { assertBlocked(e, "not authorised"); }
    }
    @Test public void aCompletedPaymentCannotHaveAChequePrintedAgain() throws Exception {
        Payment p = completed();
        try { service.printCheque(staff, p.getId()); fail(); }
        catch (PaymentException e) { assertBlocked(e, "cannot have a cheque printed"); }
    }

    // ---- audit trail (every payment-related activity: who, when, what)
    @Test public void everyActivityIsInTheAuditTrail() throws Exception {
        Payment p = recordCheque();
        service.printCheque(staff, p.getId());
        service.attachChequeScan(staff, p.getId(), "cheque.png", 5000);
        service.updateStatus(staff, p.getId(), PaymentStatus.PROCESSED, null);
        service.updateStatus(staff, p.getId(), PaymentStatus.COMPLETED, null);
        service.verifyPayment(manager, p.getId());
        service.correctPayment(staff, p.getId(), null, null, null, "UTech", "Wrong payee named");
        List<PaymentAudit> trail = service.getAuditTrail(staff, 1);
        List<String> actions = new java.util.ArrayList<>();
        for (PaymentAudit a : trail) actions.add(a.getAction());
        assertEquals(List.of("RECORDED", "CHEQUE_PRINTED", "CHEQUE_SCAN_ATTACHED", "STATUS_CHANGED",
                "STATUS_CHANGED", "VERIFIED", "CORRECTED"), actions);
        assertEquals(2, trail.get(0).getPerformedBy());       // who recorded it
        assertEquals(1, trail.get(5).getPerformedBy());       // the manager verified
        assertNotNull(trail.get(0).getPerformedAt());
        assertTrue(trail.get(0).getDetails().contains("5000.00"));
    }
    @Test public void aFailedSaveLeavesNoAuditLine() throws Exception {
        repo.failOnSave = true;
        try { record(); fail(); } catch (PaymentException e) { assertBlocked(e, "No changes were made"); }
        repo.failOnSave = false;
        assertTrue(service.getAuditTrail(staff, 1).isEmpty());
        assertTrue(service.getPaymentHistory(1).isEmpty());
    }
}
