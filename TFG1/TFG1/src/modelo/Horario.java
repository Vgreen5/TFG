package modelo;

import javafx.beans.property.*;

import java.time.LocalTime;

public class Horario {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty diaSemana = new SimpleStringProperty();
    private final ObjectProperty<LocalTime> horaInicio = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> horaFin = new SimpleObjectProperty<>();
    private final StringProperty grupo = new SimpleStringProperty();
    private final StringProperty aula = new SimpleStringProperty();
    private final BooleanProperty faltara = new SimpleBooleanProperty();
    
    public Horario(int id, String diaSemana, LocalTime horaInicio, LocalTime horaFin, 
                  String grupo, String aula, boolean faltara) {
        setId(id);
        setDiaSemana(diaSemana);
        setHoraInicio(horaInicio);
        setHoraFin(horaFin);
        setGrupo(grupo);
        setAula(aula);
        setFaltara(faltara);
    }
    
    
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    
    public String getDiaSemana() { return diaSemana.get(); }
    public void setDiaSemana(String diaSemana) { this.diaSemana.set(diaSemana); }
    
    public LocalTime getHoraInicio() { return horaInicio.get(); }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio.set(horaInicio); }
    
    public LocalTime getHoraFin() { return horaFin.get(); }
    public void setHoraFin(LocalTime horaFin) { this.horaFin.set(horaFin); }
    
    public String getGrupo() { return grupo.get(); }
    public void setGrupo(String grupo) { this.grupo.set(grupo); }
    
    public String getAula() { return aula.get(); }
    public void setAula(String aula) { this.aula.set(aula); }
    
    public boolean isFaltara() { return faltara.get(); }
    public void setFaltara(boolean faltara) { this.faltara.set(faltara); }
    
    // Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty diaSemanaProperty() { return diaSemana; }
    public ObjectProperty<LocalTime> horaInicioProperty() { return horaInicio; }
    public ObjectProperty<LocalTime> horaFinProperty() { return horaFin; }
    public StringProperty grupoProperty() { return grupo; }
    public StringProperty aulaProperty() { return aula; }
    public BooleanProperty faltaraProperty() { return faltara; }
}