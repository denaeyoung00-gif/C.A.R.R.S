package carrs.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryProgramRepository implements ProgramRepository {
    private final List<Program> programs = new ArrayList<>();
    private int nextId = 1;

    @Override
    public Program save(Program program) {
        if (program.getId() == null) {
            program.setId(String.valueOf(nextId));
            nextId++;
            programs.add(program);
        } else {
            for (int i = 0; i < programs.size(); i++) {
                if (programs.get(i).getId().equals(program.getId())) {
                    programs.set(i, program);
                    return program;
                }
            }
            programs.add(program);
        }
        return program;
    }

    @Override
    public Optional<Program> findById(String id) {
        for (Program p : programs) {
            if (p.getId().equals(id)) return Optional.of(p);
        }
        return Optional.empty();
    }

    @Override
    public List<Program> findAll() {
        return programs;
    }

    @Override
    public boolean deleteById(String id) {
        return programs.removeIf(p -> p.getId().equals(id));
    }
}