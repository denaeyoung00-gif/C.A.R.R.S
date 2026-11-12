package carrs.repository;
import carrs.model.*;
import java.util.List;

    public interface CarrsRepository {
        Applicant findApplicant(int applicantId);
        Programme findProgramme(int programmeId);
        Enrolment findEnrolment(int enrolmentId);
        List<Enrolment> findEnrolmentsForApplicant(int applicantId);
        List<Programme> findActiveProgrammes();
        int countLiveEnrolments(int programmeId);
        List<AuditEntry> findAuditEntries();
        /** Atomic: mark source Transferred, save the new enrolment, save the audit entry. */
        void saveTransfer(Enrolment source, Enrolment created, AuditEntry audit);
    }

