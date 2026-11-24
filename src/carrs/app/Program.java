package carrs.app;



// package programme.domain;

// import java.time.LocalDate;

// // import java.time.LocalDate;



// public class Program {
//     private String programName;
//     private LocalDate startDate;
//     private LocalDate finishDate;
//     private int capacity;

//     public Program(String programName, LocalDate startDate,
//         LocalDate finishDate, int capacity
//     ) {

//         this.programName = programName;
//         this.startDate = startDate;
//         this.finishDate = finishDate;
//         this.capacity = capacity;

//     }




//     public String getProgramName() {
//         return programName;

//     }

//     public LocalDate getStartDate() {
//         return startDate;

//     }


//     public LocalDate getFinishDate() {
//         return finishDate;

//     }

//     public int getCapacity() {
//         return capacity;

//     }




//     public boolean hasValidDates() {
//         return !finishDate.isBefore(startDate);
//     }


//     // public ProgramStatus computeStatus(LocalDate today) {
//     //     if (today.isBefore(startDate)) return ProgramStatus.UPCOMNING;
//     //     if (today.isAfter(finishDate)) return ProgramStatus.INACTIVE;
//     //     return ProgramStatus.ACTIVE;
//     // }
// }


// package programme;

import java.time.LocalDate;

public class Program {
    private String id;
    private String programName;
    private LocalDate startDate;
    private LocalDate finishDate;
    private int capacity;

    public Program(String programName, LocalDate startDate, LocalDate finishDate, int capacity) {
        this.programName = programName;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.capacity = capacity;
    }

    public String getId()            { return id; }
    public String getProgramName()          { return programName; }
    public LocalDate getStartDate()  { return startDate; }
    public LocalDate getFinishDate() { return finishDate; }
    public int getCapacity()         { return capacity; }

    public void setId(String id) {
        this.id = id;
    }

    public ProgramStatus computeStatus(LocalDate today) {
        if (today.isBefore(startDate))  return ProgramStatus.UPCOMING;
        if (today.isAfter(finishDate))  return ProgramStatus.INACTIVE;
        return ProgramStatus.ACTIVE;
    }
}