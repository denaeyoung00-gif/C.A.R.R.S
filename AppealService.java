import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class AppealService {

    private final Map<String, Appeal> appealStorage = new HashMap<>();
    private final AtomicLong docketSequence = new AtomicLong(1000);

    // Business rule constraints (SR-1, SR-3)
    private static final int STATUTORY_APPEAL_WINDOW_DAYS = 30;
    private static final int MIN_JUSTIFICATION_LENGTH = 50;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpeg", "jpg");
    private static final int MIN_DECISION_NOTE_LENGTH = 20;
    private static final int DENIAL_ARCHIVE_DAYS = 30;
    private boolean simulateEnrollmentSyncFailure = false;

    public void setSimulateEnrollmentSyncFailure(boolean simulateEnrollmentSyncFailure) {
        this.simulateEnrollmentSyncFailure = simulateEnrollmentSyncFailure;
    }

    // Central method to lodge appeal against a rejected/disqualified record
    public Appeal lodgeAppeal(
            ApplicationRecord application,
            String targetConstituencyCode,
            boolean isVerifiedResident,
            List<AppealGround> grounds,
            String justification,
            List<SupportingDocument> documents,
            String agentId) {

        // 1. Guard check: Application exists
        if (application == null) {
            throw new IllegalArgumentException("Record Error: Application does not exist.");
        }

        // 2. Guard check: Record already locked
        if (application.isLocked()) {
            throw new IllegalStateException("Record Locked: Application " + application.getApplicationId()
                    + " already has an active appeal pending review.");
        }

        // 3. SR-1 & AC-1: Verify status is "Rejected" or "Disqualified"
        String currentStatus = application.getStatus();
        if (!"Rejected".equalsIgnoreCase(currentStatus) && !"Disqualified".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException(
                    "Restriction Banner: Appeal can only be lodged for 'Rejected' or 'Disqualified' records. Current status: "
                            + currentStatus);
        }

        // 4. SR-1 & AC-1: Verify within 30-day statutory appeal window
        if (application.getDecisionDate() == null) {
            throw new IllegalStateException("Record Error: Application has no recorded decision date.");
        }
        long daysSinceDecision = Duration.between(application.getDecisionDate(), LocalDateTime.now()).toDays();
        if (daysSinceDecision > STATUTORY_APPEAL_WINDOW_DAYS) {
            throw new IllegalStateException("Deadline Expiry Error: The 30-day statutory appeal window has passed ("
                    + daysSinceDecision + " days since decision).");
        }

        // 5. SR-2 & AC-2: Validate resident status in constituency
        if (!isVerifiedResident) {
            throw new SecurityException(
                    "Constituency Boundary Error: Applicant is not an active, verified resident of constituency "
                            + targetConstituencyCode + ".");
        }

        // 6. SR-3 & AC-3: Grounds check
        if (grounds == null || grounds.isEmpty()) {
            throw new IllegalArgumentException("Validation Error: At least one appeal reason must be selected.");
        }

        // 7. SR-3 & AC-3: Justification character check (>= 50 chars)
        if (justification == null || justification.trim().length() < MIN_JUSTIFICATION_LENGTH) {
            throw new IllegalArgumentException("Validation Error: Justification statement must contain at least "
                    + MIN_JUSTIFICATION_LENGTH + " characters.");
        }

        // 8. SR-3 & AC-3: Supporting documents checks (>= 1 doc, allowed format, <=
        // 10MB)
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("Validation Error: At least one supporting proof document is required.");
        }
        for (SupportingDocument doc : documents) {
            if (!ALLOWED_EXTENSIONS.contains(doc.getExtension())) {
                throw new IllegalArgumentException("Unsupported File Format: " + doc.getFileName() + " (."
                        + doc.getExtension() + "). Allowed: PDF, PNG, JPEG.");
            }
            if (doc.getFileSizeBytes() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException(
                        "File Size Limit Exceeded: " + doc.getFileName() + " exceeds the 10 MB limit.");
            }
        }

        // 9. SR-4 & AC-4: Assign Constituency Appeal Docket Number (CADN)
        String cadn = "CADN-" + targetConstituencyCode.toUpperCase() + "-" + docketSequence.incrementAndGet();

        // 10. SR-4 & TS-8: Lock the original application from further standard edits
        application.setLocked(true);

        // 11. Create and store the Appeal Docket
        Appeal docket = new Appeal(
                cadn,
                application.getApplicationId(),
                application.getApplicantCitizenId(),
                targetConstituencyCode,
                grounds,
                justification,
                documents);
        appealStorage.put(cadn, docket);

        // 12. SR-4 / TS-7: Log to constituency audit trail
        logAuditTrail(agentId, cadn, application.getApplicationId(), "APPEAL_LODGED",
                "Status set to Appeal Pending Review. Source application locked.");

        return docket;
    }

    public Appeal openAppealForReview(String cadn, String reviewerId, boolean hasReviewerPermission) {
        if (!hasReviewerPermission) {
            throw new SecurityException(
                    "Access Denied: User '" + reviewerId + "' does not have appeal reviewer permissions.");
        }
        Appeal appeal = requireAppeal(cadn);
        if (appeal.getStatus() != AppealStatus.APPEAL_PENDING_REVIEW
                && appeal.getStatus() != AppealStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Review Error: Appeal " + cadn
                    + " is not in 'APPEAL_PENDING_REVIEW' status (current: " + appeal.getStatus() + ").");
        }
        appeal.recordReviewSession(reviewerId);
        logAuditTrail(reviewerId, cadn, appeal.getOriginalApplicationId(), "REVIEW_SESSION_OPENED",
                "Reviewer inspected grounds, justification, and documents.");
        return appeal;
    }

    public Appeal approveAppeal(
            String cadn,
            ApplicationRecord application,
            String reviewerId,
            boolean hasReviewerPermission,
            String decisionNote) {

        validateAdjudicationInputs(cadn, application, reviewerId, hasReviewerPermission, decisionNote);
        Appeal appeal = requireAppeal(cadn);

        application.setLocked(false);
        application.setStatus("Approved");
        application.setDecisionDate(LocalDateTime.now());

        if (simulateEnrollmentSyncFailure) {
            logAuditTrail(reviewerId, cadn, application.getApplicationId(), "ENROLLMENT_SYNC_FAILED",
                    "Enrollment Management handoff timed out; appeal remains retryable.");
            throw new IllegalStateException("Enrollment Sync Error: Timeout/sync failure routing appeal " + cadn
                    + " to Enrollment Management. Retry available.");
        }

        application.setEnrollmentTaskCreated(true);
        appeal.finalizeDecision(AppealStatus.APPEAL_APPROVED, reviewerId, decisionNote.trim(), null);

        logAuditTrail(
                reviewerId,
                cadn,
                application.getApplicationId(),
                "APPEAL_APPROVED",
                "Status updated to APPEAL_APPROVED; application unlocked, marked Approved, and routed to Enrollment Management. Note: "
                        + decisionNote.trim());
        return appeal;
    }

    public Appeal denyAppeal(
            String cadn,
            ApplicationRecord application,
            String reviewerId,
            boolean hasReviewerPermission,
            String decisionNote) {

        validateAdjudicationInputs(cadn, application, reviewerId, hasReviewerPermission, decisionNote);
        Appeal appeal = requireAppeal(cadn);

        LocalDateTime archiveUntil = LocalDateTime.now().plusDays(DENIAL_ARCHIVE_DAYS);
        application.setArchivedUntil(archiveUntil);
        application.setLocked(true);

        appeal.finalizeDecision(AppealStatus.APPEAL_DENIED, reviewerId, decisionNote.trim(), archiveUntil);

        logAuditTrail(
                reviewerId,
                cadn,
                application.getApplicationId(),
                "APPEAL_DENIED",
                "Status updated to APPEAL_DENIED; appeal finalized/locked and application archived until "
                        + archiveUntil + ". Reason: " + decisionNote.trim());
        return appeal;
    }

    private void validateAdjudicationInputs(
            String cadn,
            ApplicationRecord application,
            String reviewerId,
            boolean hasReviewerPermission,
            String decisionNote) {

        if (!hasReviewerPermission) {
            throw new SecurityException(
                    "Access Denied: User '" + reviewerId + "' is not authorized to adjudicate appeals.");
        }
        if (application == null) {
            throw new IllegalArgumentException("Record Error: Application does not exist.");
        }
        if (decisionNote == null || decisionNote.trim().length() < MIN_DECISION_NOTE_LENGTH) {
            throw new IllegalArgumentException("Validation Error: A decision justification of at least "
                    + MIN_DECISION_NOTE_LENGTH + " characters is required.");
        }
        Appeal appeal = requireAppeal(cadn);
        if (appeal.isFinalized()) {
            throw new IllegalStateException("Finalized Error: Appeal " + cadn + " is already finalized ("
                    + appeal.getStatus() + ") and cannot be altered or re-adjudicated.");
        }
    }

    private Appeal requireAppeal(String cadn) {
        Appeal appeal = appealStorage.get(cadn);
        if (appeal == null) {
            throw new IllegalArgumentException("Record Error: Appeal docket not found for CADN " + cadn);
        }
        return appeal;
    }

    // Helper for SR-4 audit requirement
    private void logAuditTrail(String agentId, String cadn, String appId, String action, String details) {
        System.out.println("[AUDIT LOG] " + LocalDateTime.now() + " | Agent: " + agentId
                + " | Docket: " + cadn + " | AppID: " + appId + " | Action: " + action + " | " + details);
    }

    public Appeal findAppealByCadn(String cadn) {
        return appealStorage.get(cadn);
    }

    public List<Appeal> getAllAppeals() {
        return new ArrayList<>(appealStorage.values());
    }
}