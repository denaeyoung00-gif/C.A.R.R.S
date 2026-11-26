package carrs.app;

import carrs.model.ProgrammeDetails;
import carrs.repository.CarrsRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * The rules for creating, editing and deleting programmes. The screens call this, and this talks to the
 * database through the repository. Every problem is reported as an IllegalArgumentException with a message
 * that can be shown to staff as it is.
 */
public class ProgramManagementService {
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_CAPACITY = 10000;

    private final CarrsRepository repo;

    public ProgramManagementService(CarrsRepository repo) { this.repo = repo; }

    public List<ProgrammeDetails> findAll() { return repo.findProgrammeDetails(); }

    public void create(String name, LocalDate start, LocalDate finish, int capacity) {
        name = validate(name, start, finish, capacity);
        try {
            repo.addProgramme(name, start, finish, capacity);
        } catch (IllegalStateException e) { throw new IllegalArgumentException(e.getMessage()); }
    }

    public void update(int id, String name, LocalDate start, LocalDate finish, int capacity) {
        name = validate(name, start, finish, capacity);
        int enrolled = repo.countLiveEnrolments(id);
        if (capacity < enrolled)
            throw new IllegalArgumentException("Capacity cannot be lower than the " + enrolled
                    + " people already enrolled.");
        try {
            repo.editProgramme(id, name, start, finish, capacity);
        } catch (IllegalStateException e) { throw new IllegalArgumentException(e.getMessage()); }
    }

    public void delete(int id) {
        try {
            repo.removeProgramme(id);
        } catch (IllegalStateException e) { throw new IllegalArgumentException(e.getMessage()); }
    }

    private String validate(String name, LocalDate start, LocalDate finish, int capacity) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Please enter the programme name.");
        name = name.trim();
        if (name.length() > MAX_NAME_LENGTH)
            throw new IllegalArgumentException("The name can be at most " + MAX_NAME_LENGTH + " characters.");
        if (start == null) throw new IllegalArgumentException("Please choose a start date.");
        if (finish == null) throw new IllegalArgumentException("Please choose a finish date.");
        if (finish.isBefore(start)) throw new IllegalArgumentException("The finish date cannot be before the start date.");
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be at least 1.");
        if (capacity > MAX_CAPACITY) throw new IllegalArgumentException("Capacity cannot be more than " + MAX_CAPACITY + ".");
        return name;
    }
}
