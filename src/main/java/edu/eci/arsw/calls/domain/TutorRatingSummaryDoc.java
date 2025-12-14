package edu.eci.arsw.calls.domain;

/**
 * Documento que representa el resumen de calificaciones de un tutor.
 */
public class TutorRatingSummaryDoc {
    private String tutorId;
    private Long count;
    private Double avg;

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
    public Double getAvg() { return avg; }
    public void setAvg(Double avg) { this.avg = avg; }
}
