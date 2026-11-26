package carrs.app;
import carrs.repository.InMemoryCarrsRepository;
import carrs.repository.MySqlCarrsRepository;

public class Main {
    public static void main(String[] args) {
        MySqlCarrsRepository repo = new MySqlCarrsRepository();
        System.out.println("Connected. Active programmes: " + repo.findActiveProgrammes().size());
        System.out.println("Constituents: " + repo.findAllApplicants().size());
    }
}
