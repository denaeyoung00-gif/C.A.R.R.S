// Programme.java
package carrs.model;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class Programme {
    private final int id;
    private final String name;
    private final ProgrammeStatus status;   // stored status — used only by legacy constructors
    private final int capacity;
    private final LocalDate startDate;      // may be null for legacy rows
    private final LocalDate finishDate;     // may be null for legacy rows
    private final Set<String> requiredFields = new HashSet<>();
    private boolean providesBenefits = true;

    /** Legacy constructor — no dates. statusOn() falls back to the stored status. */
    public Programme(int id, String name, ProgrammeStatus status, int capacity) {
        this(id, name, status, capacity, null, null);
    }

    /** Legacy constructor — no dates, default capacity. */
    public Programme(int id, String name, ProgrammeStatus status) {
        this(id, name, status, 25, null, null);
    }

    /** Full constructor with dates. Status is derived via statusOn(today). */
    public Programme(int id, String name, LocalDate startDate, LocalDate finishDate, int capacity) {
        this(id, name, null, capacity, startDate, finishDate);
    }

    private Programme(int id, String name, ProgrammeStatus status, int capacity,
                      LocalDate startDate, LocalDate finishDate) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.capacity = capacity;
        this.startDate = startDate;
        this.finishDate = finishDate;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }

    /** Stored status — legacy. Prefer statusOn(today). */
    public ProgrammeStatus getStatus() { return status; }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getFinishDate() { return finishDate; }

    public Set<String> getRequiredFields() { return requiredFields; }
    public Programme requires(String field) { requiredFields.add(field); return this; }
    public boolean providesBenefits() { return providesBenefits; }
    public Programme paysBenefits(boolean value) { this.providesBenefits = value; return this; }

    /** Legacy check — reads the stored status only. Kept so existing code compiles. */
    public boolean isActive() {
        return status == ProgrammeStatus.ACTIVE;
    }

    /**
     * Derived status from the current date (SR-2).
     *   - before start      -> UPCOMING
     *   - after finish      -> COMPLETED
     *   - on/after start and on/before finish -> ACTIVE
     * Falls back to the stored status if dates are missing (legacy rows).
     */
    public ProgrammeStatus statusOn(LocalDate today) {
        if (startDate == null || finishDate == null) return status;
        if (today.isBefore(startDate))  return ProgrammeStatus.UPCOMING;
        if (today.isAfter(finishDate))  return ProgrammeStatus.COMPLETED;
        return ProgrammeStatus.ACTIVE;
    }
}