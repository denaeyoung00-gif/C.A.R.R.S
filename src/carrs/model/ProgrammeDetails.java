package carrs.model;

import java.time.LocalDate;

/** A programme as the Programmes screen shows it: dates, capacity and how many places are filled. */
public class ProgrammeDetails {
    private final int id;
    private final String name;
    private final LocalDate startDate, finishDate;   // null for programmes created before dates existed
    private final int capacity, enrolled;
    private final ProgrammeStatus status;

    public ProgrammeDetails(int id, String name, LocalDate startDate, LocalDate finishDate,
                            int capacity, int enrolled, ProgrammeStatus status) {
        this.id = id; this.name = name; this.startDate = startDate; this.finishDate = finishDate;
        this.capacity = capacity; this.enrolled = enrolled; this.status = status;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getFinishDate() { return finishDate; }
    public int getCapacity() { return capacity; }
    public int getEnrolled() { return enrolled; }
    public int getRemaining() { return Math.max(0, capacity - enrolled); }
    public ProgrammeStatus getStatus() { return status; }
    public boolean hasDates() { return startDate != null && finishDate != null; }

    /** Upcoming before the start date, Completed after the finish date, otherwise Active. */
    public static ProgrammeStatus statusFor(LocalDate start, LocalDate finish, LocalDate today) {
        if (today.isBefore(start)) return ProgrammeStatus.UPCOMING;
        if (today.isAfter(finish)) return ProgrammeStatus.COMPLETED;
        return ProgrammeStatus.ACTIVE;
    }
}
