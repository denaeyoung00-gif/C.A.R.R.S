import carrs.app.MainFrame;
import carrs.app.*;
import java.time.LocalDate;
import javax.swing.*;

public class Main1 {
    public static void main(String[] args) {
        ProgramRepository repo = new InMemoryProgramRepository();
        ProgramManagementService service = new ProgramManagementService(repo);

        LocalDate today = LocalDate.now();
        service.createProgram("Java 101", today.minusDays(10), today.plusDays(20), 10);
        service.createProgram("Python 101", today.minusDays(5), today.plusDays(30), 20);

        SwingUtilities.invokeLater(() -> new MainFrame(repo, service).setVisible(true));
    }
}