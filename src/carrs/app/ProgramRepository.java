package carrs.app;

// package src.main.java; 

// import programme.Program;
import java.util.List;
// import java.util.Optional;

public interface ProgramRepository {
    Program save(Program program);
    // Optional<Program> findById(String id);
    List<Program> findAll();
    // boolean deleteById(String id);
}