package carrs.app;

import java.time.LocalDate;
import java.util.List;

public class ProgramManagementService {
    private final ProgramRepository repository;

    public ProgramManagementService(ProgramRepository repository) {
        this.repository = repository;
    }

    public Program createProgram(String name, LocalDate startDate,
                                 LocalDate finishDate, int capacity) {
        validate(name, startDate, finishDate, capacity);
        Program program = new Program(name, startDate, finishDate, capacity);
        return repository.save(program);
    }

    public Program updateProgram(String id, String name, LocalDate startDate,
                                 LocalDate finishDate, int capacity) {
        Program existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No programme with id " + id));
        validate(name, startDate, finishDate, capacity);
        Program updated = new Program(name, startDate, finishDate, capacity);
        updated.setId(existing.getId());
        return repository.save(updated);
    }

    public void deleteProgram(String id) {
        if (!repository.deleteById(id)) {
            throw new IllegalArgumentException("No programme with id " + id);
        }
    }

    public Program findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No programme with id " + id));
    }

    public List<Program> findAll() {
        return repository.findAll();
    }

    /** Placeholder — real count comes from enrolment records once wired to MySQL. */
    public int getEnrolledCount(String id) {
        return 0;
    }

    private void validate(String name, LocalDate startDate,
                          LocalDate finishDate, int capacity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (finishDate == null) {
            throw new IllegalArgumentException("Finish date is required");
        }
        if (finishDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Finish date cannot be before start date");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
    }
}