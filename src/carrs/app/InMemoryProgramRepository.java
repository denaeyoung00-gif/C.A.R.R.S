package carrs.app;

// package src.main.java;


// import programme.Program;
import java.util.ArrayList;
import java.util.List;

public class InMemoryProgramRepository implements ProgramRepository {
    private final List<Program> programs = new ArrayList<>();
    private int nextId = 1;

    @Override
    public Program save(Program program) {
        if (program.getId() == null) {
            program.setId(String.valueOf(nextId));
            nextId++;
        }
        programs.add(program);
        return program;
    }

    @Override
    public List<Program> findAll() {
        return programs;
    }
}