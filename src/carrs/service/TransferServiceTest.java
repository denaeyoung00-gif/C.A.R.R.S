package carrs.service;

import static org.junit.Assert.*;
import carrs.model.*;
import carrs.repository.InMemoryCarrsRepository;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;

public class TransferServiceTest {
    private InMemoryCarrsRepository repo;
    private TransferService service;
    private StaffMember manager, attendant;

    @Before public void setUp() {
        repo = new InMemoryCarrsRepository();
        service = new TransferService(repo);
        manager = new StaffMember(1, "Manager", Role.OFFICE_MANAGER);
        attendant = new StaffMember(2, "Attendant", Role.OFFICE_ATTENDANT);
        repo.add(new Applicant(1, "Jane").with("household_size", "4"));
        repo.add(new Applicant(2, "Tom"));
        repo.add(new Programme(10, "Poverty", ProgrammeStatus.ACTIVE));
        repo.add(new Programme(11, "Back to School", ProgrammeStatus.ACTIVE).requires("household_size"));
        repo.add(new Programme(12, "Upcoming One", ProgrammeStatus.UPCOMING));
        repo.add(enrol(100, 1, 10));
        repo.add(enrol(101, 2, 10));
    }

    private Enrolment enrol(int id, int applicant, int programme) {
        return new Enrolment(id, applicant, programme, EnrolmentStatus.ACTIVE, LocalDateTime.now(), null);
    }
    private void assertNothingChanged(int enrolmentId) {
        assertEquals(EnrolmentStatus.ACTIVE, repo.findEnrolment(enrolmentId).getStatus());
        assertTrue(repo.findAuditEntries().isEmpty());
    }

    @Test public void validTransferKeepsOldAndCreatesLinkedNew() throws Exception {
        Enrolment created = service.transfer(manager, 100, 11, "Needs school fee help");
        assertEquals(EnrolmentStatus.TRANSFERRED, repo.findEnrolment(100).getStatus());
        assertEquals(EnrolmentStatus.ACTIVE, created.getStatus());
        assertEquals(11, created.getProgrammeId());
        assertEquals(Integer.valueOf(100), created.getTransferredFromEnrolmentId());
    }
    @Test public void validTransferWritesOneCompleteAuditEntry() throws Exception {
        service.transfer(manager, 100, 11, "Needs school fee help");
        assertEquals(1, repo.findAuditEntries().size());
        AuditEntry a = repo.findAuditEntries().get(0);
        assertEquals(1, a.getStaffId());
        assertNotNull(a.getTimestamp());
        assertEquals(10, a.getSourceProgrammeId());
        assertEquals(11, a.getDestinationProgrammeId());
        assertEquals("Needs school fee help", a.getReason());
    }
    @Test public void reasonOfNineCharactersIsRejected() {
        try { service.transfer(manager, 100, 11, "123456789"); fail(); }
        catch (TransferException e) { assertNothingChanged(100); }
    }
    @Test public void reasonOfTenCharactersIsAccepted() throws Exception {
        service.transfer(manager, 100, 11, "1234567890");
        assertEquals(1, repo.findAuditEntries().size());
    }
    @Test public void blankReasonIsRejected() {
        try { service.transfer(manager, 100, 11, "   "); fail(); }
        catch (TransferException e) { assertNothingChanged(100); }
    }
    @Test public void blockedWhenAlreadyEnrolledInDestination() {
        repo.add(enrol(102, 1, 11));
        try { service.transfer(manager, 100, 11, "Valid reason here"); fail(); }
        catch (TransferException e) { assertNothingChanged(100); }
    }
    @Test public void blockedWhenDestinationInfoMissing() {
        try { service.transfer(manager, 101, 11, "Valid reason here"); fail(); }
        catch (TransferException e) {
            assertTrue(e.getMessage().contains("household_size"));
            assertNothingChanged(101);
        }
    }
    @Test public void blockedWhenDestinationNotActive() {
        try { service.transfer(manager, 100, 12, "Valid reason here"); fail(); }
        catch (TransferException e) { assertNothingChanged(100); }
    }
    @Test public void blockedWhenDestinationIsCurrentProgramme() {
        try { service.transfer(manager, 100, 10, "Valid reason here"); fail(); }
        catch (TransferException e) { assertNothingChanged(100); }
    }
    @Test public void eligibleListHasActiveProgrammesExceptCurrent() throws Exception {
        assertEquals(1, service.getEligibleDestinations(manager, 100).size());
        assertEquals(11, service.getEligibleDestinations(manager, 100).get(0).getId());
    }
    @Test public void attendantCannotTransfer() {
        try { service.transfer(attendant, 100, 11, "Valid reason here"); fail(); }
        catch (TransferException e) { assertNothingChanged(100); }
    }
    @Test public void databaseFailureLeavesNoPartialChange() {
        repo.failOnSave = true;
        try { service.transfer(manager, 100, 11, "Valid reason here"); fail(); }
        catch (TransferException e) { assertNothingChanged(100); }
    }
}