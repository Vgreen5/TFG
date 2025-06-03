package modelo;

import java.time.LocalDate;
import java.time.LocalTime;

public class Guardia {
    private int id;
    private String docenteId;
    private String docenteNombreCompleto; 
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String aula;
    private boolean disponible;
 
    private String estado;

    public Guardia() {}

    public Guardia(int id, String docenteId, String docenteNombreCompleto, LocalDate fecha, LocalTime horaInicio,
                   LocalTime horaFin, String aula, boolean disponible, String estado) {
        this.id = id;
        this.docenteId = docenteId;
        this.docenteNombreCompleto = docenteNombreCompleto;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.aula = aula;
        this.disponible = disponible;
        this.estado = estado;
    }
 
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDocenteId() { return docenteId; }
    public void setDocenteId(String docenteId) { this.docenteId = docenteId; }

    public String getDocenteNombreCompleto() { return docenteNombreCompleto; }
    public void setDocenteNombreCompleto(String docenteNombreCompleto) { this.docenteNombreCompleto = docenteNombreCompleto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    // 🔹 Nuevo getter y setter
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Guardia{" +
                "id=" + id +
                ", docenteId='" + docenteId + '\'' +
                ", docenteNombreCompleto='" + docenteNombreCompleto + '\'' +
                ", fecha=" + fecha +
                ", horaInicio=" + horaInicio +
                ", horaFin=" + horaFin +
                ", aula='" + aula + '\'' +
                ", disponible=" + disponible +
                ", estado='" + estado + '\'' +
                '}';
    }
}
