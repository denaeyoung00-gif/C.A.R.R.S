import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        InMemoryProgramRepository repo = new InMemoryProgramRepository();
        ProgramManagementServices service = new ProgramManagementServices(repo);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Programme Management: Create ===");

        while (true) {
            System.out.print("\nProgramme name (or 'quit'): ");
            String name = scanner.nextLine().trim();
            if (name.equalsIgnoreCase("quit")) {
                break;
            }

            LocalDate startDate = askDate(scanner, "Start date (YYYY-MM-DD): ");
            if (startDate == null) continue;

            LocalDate finishDate = askDate(scanner, "Finish date (YYYY-MM-DD): ");
            if (finishDate == null) continue;

            System.out.print("Capacity: ");
            String capacityInput = scanner.nextLine().trim();
            int capacity;
            try {
                capacity = Integer.parseInt(capacityInput);
            } catch (NumberFormatException e) {
                System.out.println("Error: capacity must be a whole number.");
                continue;
            }

            try {
                Program p = service.createProgram(name, startDate, finishDate, capacity);
                System.out.println("Created programme #" + p.getId()
                        + " (" + p.getProgramName() + "), status now: "
                        + p.computeStatus(LocalDate.now()));
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("\n--- All programmes ---");
        for (Program p : repo.findAll()) {
            System.out.println("#" + p.getId() + " | " + p.getProgramName()
                    + " | " + p.getStartDate() + " → " + p.getFinishDate()
                    + " | capacity " + p.getCapacity()
                    + " | " + p.computeStatus(LocalDate.now()));
        }

        scanner.close();
    }

    private static LocalDate askDate(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println("Error: date must be YYYY-MM-DD (e.g. 2026-01-15).");
            return null;
        }
    }
}