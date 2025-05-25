package modelo;

import java.time.LocalDate;
import java.time.LocalTime;

public class Absencia {
    private int id;
    private String docenteId; 
    private LocalDate fecha;
    private String tipoAbsencia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String observaciones;
    private boolean justificada;  

    public Absencia() {
        
    }

    public Absencia(int id, String docenteId, LocalDate fecha, String tipoAbsencia,
                    LocalTime horaInicio, LocalTime horaFin, String observaciones,
                    boolean justificada) {
        this.id = id;
        this.docenteId = docenteId;
        this.fecha = fecha;
        this.tipoAbsencia = tipoAbsencia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.observaciones = observaciones;
        this.justificada = justificada;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDocenteId() {
        return docenteId;
    }

    public void setDocenteId(String docenteId) {
        this.docenteId = docenteId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTipoAbsencia() {
        return tipoAbsencia;
    }

    public void setTipoAbsencia(String tipoAbsencia) {
        this.tipoAbsencia = tipoAbsencia;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean isJustificada() {
        return justificada;
    }

    public void setJustificada(boolean justificada) {
        this.justificada = justificada;
    }
}
