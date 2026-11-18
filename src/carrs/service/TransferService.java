package carrs.service;

import carrs.model.*;
import carrs.repository.CarrsRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransferService {
    public static final int MIN_REASON_LENGTH = 10;
    private final CarrsRepository repo;

    public TransferService(CarrsRepository repo) { this.repo = repo; }

    public List<Programme> getEligibleDestinations(StaffMember staff, int enrolmentId)
            throws TransferException {
        requireAuthorised(staff);
        Enrolment source = requireTransferableEnrolment(enrolmentId);
        List<Programme> out = new ArrayList<>();
        for (Programme p : repo.findActiveProgrammes())
            if (p.getId() != source.getProgrammeId()) out.add(p);
        return out;
    }

    public Enrolment transfer(StaffMember staff, int enrolmentId, int destinationProgrammeId,
                              String reason) throws TransferException {
        requireAuthorised(staff);
        Enrolment source = requireTransferableEnrolment(enrolmentId);

        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() < MIN_REASON_LENGTH)
            throw new TransferException("A reason of at least " + MIN_REASON_LENGTH
                    + " characters is required for a transfer.");

        Programme destination = repo.findProgramme(destinationProgrammeId);
        if (destination == null)
            throw new TransferException("The destination programme does not exist.");
        if (destination.getId() == source.getProgrammeId())
            throw new TransferException("The applicant is already in that programme.");
        if (!destination.isActive())
            throw new TransferException("The destination programme is not active.");

        for (Enrolment e : repo.findEnrolmentsForApplicant(source.getApplicantId()))
            if (e.getProgrammeId() == destination.getId() && e.isLive())
                throw new TransferException("The applicant is already enrolled in "
                        + destination.getName() + ".");

        if (repo.countLiveEnrolments(destination.getId()) >= destination.getCapacity())
            throw new TransferException(destination.getName() + " is full.");

        Applicant applicant = repo.findApplicant(source.getApplicantId());
        if (applicant == null)
            throw new TransferException("The applicant record could not be found.");
        List<String> missing = new ArrayList<>();
        for (String field : destination.getRequiredFields())
            if (!applicant.hasDetail(field)) missing.add(field);
        if (!missing.isEmpty())
            throw new TransferException("Cannot transfer. Missing information required by "
                    + destination.getName() + ": " + String.join(", ", missing));

        // All checks passed: now (and only now) save
        LocalDateTime now = LocalDateTime.now();
        Enrolment created = new Enrolment(0, applicant.getId(), destination.getId(),
                EnrolmentStatus.ACTIVE, now, source.getId());
        AuditEntry audit = new AuditEntry(staff.getId(), now, source.getProgrammeId(),
                destination.getId(), cleanReason, applicant.getId());
        try {
            repo.saveTransfer(source, created, audit);
        } catch (RuntimeException ex) {
            throw new TransferException("The transfer could not be saved. No changes were made.", ex);
        }
        return created;
    }

    private void requireAuthorised(StaffMember staff) throws TransferException {
        if (staff == null || !staff.getRole().canTransfer())
            throw new TransferException("You are not authorised to transfer applicants.");
    }

    private Enrolment requireTransferableEnrolment(int enrolmentId) throws TransferException {
        Enrolment e = repo.findEnrolment(enrolmentId);
        if (e == null) throw new TransferException("Enrolment not found.");
        if (e.getStatus() != EnrolmentStatus.ACTIVE && e.getStatus() != EnrolmentStatus.ON_HOLD)
            throw new TransferException("Only a current enrolment can be transferred.");
        return e;
    }
}