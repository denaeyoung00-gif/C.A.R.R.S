package carrs.app;

import java.time.LocalDate;

public class ProgramManagementService {
    private final ProgramRepository repository;

    public ProgramManagementService(ProgramRepository repository) {
        this.repository = repository;
    }

    public Program createProgram(String name, LocalDate startDate,
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
        Program program = new Program(name, startDate, finishDate, capacity);
        return repository.save(program);
    }
}