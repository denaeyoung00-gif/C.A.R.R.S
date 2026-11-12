package carrs.model;

import java.util.HashMap;
import java.util.Map;
import com.mysql.cj.x.protobuf.MysqlxDatatypes;

public class Applicant {
    private final int id;
    private final String name;
    private final Map<String, String> details = new HashMap<>();
    public Applicant(int id, String name) { this.id = id; this.name = name; }
    public int getId() { return id; }
    public String getName() { return name; }
    public Applicant with(String field, String value) { details.put(field, value); return this; }
    public boolean hasDetail(String field) {
        String v = details.get(field);
        return v != null && !v.trim().isEmpty();
    }
}
