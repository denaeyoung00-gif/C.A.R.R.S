// Programme.java
package carrs.model;
import java.util.HashSet;
import java.util.Set;

public class Programme {
    private final int id; private final String name;
    private final ProgrammeStatus status; private final int capacity;
    private final Set<String> requiredFields = new HashSet<>();
    public Programme(int id, String name, ProgrammeStatus status, int capacity) {
        this.id = id; this.name = name; this.status = status; this.capacity = capacity;
    }
    public Programme(int id, String name, ProgrammeStatus status) { this(id, name, status, 25); }
    public int getId() { return id; }
    public String getName() { return name; }
    public ProgrammeStatus getStatus() { return status; }
    public int getCapacity() { return capacity; }
    public Set<String> getRequiredFields() { return requiredFields; }
    public Programme requires(String field) { requiredFields.add(field); return this; }
    public boolean isActive() { return status == ProgrammeStatus.ACTIVE; }
}