import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   CONSTITUENCY APPEAL & DISPUTE MODULE (FTR-GOV-007) TEST SUITE  ");
        System.out.println("==================================================================\n");

        AppealService service = new AppealService();

        // Reusable mock data
        SupportingDocument validPdf = new SupportingDocument("tax_records_2025.pdf", "pdf", 2 * 1024 * 1024);
        SupportingDocument invalidZip = new SupportingDocument("evidence.zip", "zip", 1024);
        String validJustification = "The assessment officer relied upon outdated household income data from previous fiscal year.";

        // --- TS-1 & TS-5 & TS-7: Valid Appeal Submission ---
        System.out.println(">>> RUNNING TS-1, TS-5, TS-7: Valid Appeal Lodging");
        ApplicationRecord app1 = new ApplicationRecord("APP-001", "CIT-1001", "PROG-WELFARE", "Rejected", LocalDateTime.now().minusDays(14));
        try {
            Appeal appeal = service.lodgeAppeal(
                app1, "CENTRAL", true,
                List.of(AppealGround.INCORRECT_INCOME_ASSESSMENT),
                validJustification,
                List.of(validPdf),
                "AGENT-007"
            );
            System.out.println("STATUS: [PASS] Docket successfully created.");
            System.out.println(appeal.generateReceipt());
        } catch (Exception e) {
            System.out.println("STATUS: [FAIL] " + e.getMessage());
        }

        // --- TS-8: Verify underlying application is locked ---
        System.out.println("\n>>> RUNNING TS-8: Original Record Locked");
        if (app1.isLocked()) {
            System.out.println("STATUS: [PASS] Application APP-001 is locked against concurrent edits.");
        } else {
            System.out.println("STATUS: [FAIL] Application APP-001 was not locked!");
        }

        // --- TS-2: Block submission past 30 days ---
        System.out.println("\n>>> RUNNING TS-2 (Part A): Expired Appeal Window (> 30 Days)");
        ApplicationRecord expiredApp = new ApplicationRecord("APP-002", "CIT-1002", "PROG-GRANT", "Rejected", LocalDateTime.now().minusDays(32));
        assertThrows(() -> service.lodgeAppeal(expiredApp, "CENTRAL", true, List.of(AppealGround.OVERLOOKED_DOCUMENTATION), validJustification, List.of(validPdf), "AGENT-007"), "TS-2 Expired Deadline");

        // --- TS-2: Block approved application ---
        System.out.println("\n>>> RUNNING TS-2 (Part B): Attempt Appeal on 'Approved' Status");
        ApplicationRecord approvedApp = new ApplicationRecord("APP-003", "CIT-1003", "PROG-GRANT", "Approved", LocalDateTime.now().minusDays(5));
        assertThrows(() -> service.lodgeAppeal(approvedApp, "CENTRAL", true, List.of(AppealGround.OVERLOOKED_DOCUMENTATION), validJustification, List.of(validPdf), "AGENT-007"), "TS-2 Approved Status");

        // --- TS-4: Block non-resident applicant ---
        System.out.println("\n>>> RUNNING TS-4: Adjacent Constituency / Non-Resident");
        ApplicationRecord nonResidentApp = new ApplicationRecord("APP-004", "CIT-1004", "PROG-SEASONAL", "Disqualified", LocalDateTime.now().minusDays(5));
        assertThrows(() -> service.lodgeAppeal(nonResidentApp, "CENTRAL", false, List.of(AppealGround.ADMINISTRATIVE_ERROR), validJustification, List.of(validPdf), "AGENT-007"), "TS-4 Non-Resident");

        // --- TS-6: Narrative under 50 characters ---
        System.out.println("\n>>> RUNNING TS-6 (Part A): Justification Narrative Under 50 Characters");
        ApplicationRecord app6a = new ApplicationRecord("APP-005", "CIT-1005", "PROG-WELFARE", "Rejected", LocalDateTime.now().minusDays(5));
        assertThrows(() -> service.lodgeAppeal(app6a, "CENTRAL", true, List.of(AppealGround.ADMINISTRATIVE_ERROR), "Too short", List.of(validPdf), "AGENT-007"), "TS-6 Justification Length");

        // --- TS-6: Unsupported file extension (.zip) ---
        System.out.println("\n>>> RUNNING TS-6 (Part B): Unsupported File Extension (.zip)");
        ApplicationRecord app6b = new ApplicationRecord("APP-006", "CIT-1006", "PROG-WELFARE", "Rejected", LocalDateTime.now().minusDays(5));
        assertThrows(() -> service.lodgeAppeal(app6b, "CENTRAL", true, List.of(AppealGround.ADMINISTRATIVE_ERROR), validJustification, List.of(invalidZip), "AGENT-007"), "TS-6 File Extension");

        System.out.println("\n==================================================================");
        System.out.println("                 ALL TEST SCENARIOS EXECUTED                      ");
        System.out.println("==================================================================");
    }

    private static void assertThrows(Runnable runnable, String testName) {
        try {
            runnable.run();
            System.out.println("STATUS: [FAIL] " + testName + " - Exception was not thrown.");
        } catch (Exception e) {
            System.out.println("STATUS: [PASS] " + testName + " -> " + e.getMessage());
        }
    }
}