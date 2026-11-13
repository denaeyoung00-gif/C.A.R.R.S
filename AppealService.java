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
            throw new IllegalStateException("Record Locked: Application " + application.getApplicationId() + " already has an active appeal pending review.");
        }

        // 3. SR-1 & AC-1: Verify status is "Rejected" or "Disqualified"
        String currentStatus = application.getStatus();
        if (!"Rejected".equalsIgnoreCase(currentStatus) && !"Disqualified".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Restriction Banner: Appeal can only be lodged for 'Rejected' or 'Disqualified' records. Current status: " + currentStatus);
        }

        // 4. SR-1 & AC-1: Verify within 30-day statutory appeal window
        if (application.getDecisionDate() == null) {
            throw new IllegalStateException("Record Error: Application has no recorded decision date.");
        }
        long daysSinceDecision = Duration.between(application.getDecisionDate(), LocalDateTime.now()).toDays();
        if (daysSinceDecision > STATUTORY_APPEAL_WINDOW_DAYS) {
            throw new IllegalStateException("Deadline Expiry Error: The 30-day statutory appeal window has passed (" + daysSinceDecision + " days since decision).");
        }

        // 5. SR-2 & AC-2: Validate resident status in constituency
        if (!isVerifiedResident) {
            throw new SecurityException("Constituency Boundary Error: Applicant is not an active, verified resident of constituency " + targetConstituencyCode + ".");
        }

        // 6. SR-3 & AC-3: Grounds check
        if (grounds == null || grounds.isEmpty()) {
            throw new IllegalArgumentException("Validation Error: At least one appeal reason must be selected.");
        }

        // 7. SR-3 & AC-3: Justification character check (>= 50 chars)
        if (justification == null || justification.trim().length() < MIN_JUSTIFICATION_LENGTH) {
            throw new IllegalArgumentException("Validation Error: Justification statement must contain at least " + MIN_JUSTIFICATION_LENGTH + " characters.");
        }

        // 8. SR-3 & AC-3: Supporting documents checks (>= 1 doc, allowed format, <= 10MB)
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("Validation Error: At least one supporting proof document is required.");
        }
        for (SupportingDocument doc : documents) {
            if (!ALLOWED_EXTENSIONS.contains(doc.getExtension())) {
                throw new IllegalArgumentException("Unsupported File Format: " + doc.getFileName() + " (." + doc.getExtension() + "). Allowed: PDF, PNG, JPEG.");
            }
            if (doc.getFileSizeBytes() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("File Size Limit Exceeded: " + doc.getFileName() + " exceeds the 10 MB limit.");
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
            documents
        );
        appealStorage.put(cadn, docket);

        // 12. SR-4 / TS-7: Log to constituency audit trail
        logAuditTrail(agentId, cadn, application.getApplicationId(), "APPEAL_LODGED", "Status set to Appeal Pending Review. Source application locked.");

        return docket;
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