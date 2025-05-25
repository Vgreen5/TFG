package controlador;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import modelo.Horario;
import modelo.Usuario;
import databases.DatabaseConnector;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class InasistenciaController implements Initializable {

    @FXML private DatePicker fechaPicker;
    @FXML private ComboBox<String> tipoInasistenciaCombo;
    @FXML private CheckBox ausenciaCompletaCheck;
    @FXML private TableView<Horario> horarioTable;
    @FXML private TableColumn<Horario, String> horaInicioCol;
    @FXML private TableColumn<Horario, String> horaFinCol;
    @FXML private TableColumn<Horario, String> grupoCol;
    @FXML private TableColumn<Horario, String> aulaCol;
    @FXML private TableColumn<Horario, Boolean> faltaraCol;
    @FXML private VBox horarioContainer;
    @FXML private TextArea observacionesArea;
    @FXML private Label mensajeLabel;

    private Usuario docenteActual;
    private ObservableList<Horario> horarios = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarCombobox();
        configurarTabla();
        configurarListeners();
    }

    private void configurarCombobox() {
        tipoInasistenciaCombo.getItems().addAll(
            "Enfermedad", 
            "Asuntos personales", 
            "Formación", 
            "Otros"
        );
        tipoInasistenciaCombo.getSelectionModel().selectFirst();
    }

    private void configurarTabla() {
        horaInicioCol.setCellValueFactory(cellData -> {
            LocalTime hora = cellData.getValue().getHoraInicio();
            return new SimpleStringProperty(hora != null ? hora.format(timeFormatter) : "");
        });
        
        horaFinCol.setCellValueFactory(cellData -> {
            LocalTime hora = cellData.getValue().getHoraFin();
            return new SimpleStringProperty(hora != null ? hora.format(timeFormatter) : "");
        });
        
        grupoCol.setCellValueFactory(cellData -> cellData.getValue().grupoProperty());
        aulaCol.setCellValueFactory(cellData -> cellData.getValue().aulaProperty());
        
        faltaraCol.setCellValueFactory(cellData -> cellData.getValue().faltaraProperty());
        faltaraCol.setCellFactory(CheckBoxTableCell.forTableColumn(faltaraCol));
        faltaraCol.setEditable(true);
        
        horarioTable.setEditable(true);
        horaInicioCol.setStyle("-fx-alignment: CENTER;");
        horaFinCol.setStyle("-fx-alignment: CENTER;");
        grupoCol.setStyle("-fx-alignment: CENTER;");
        aulaCol.setStyle("-fx-alignment: CENTER;");
        faltaraCol.setStyle("-fx-alignment: CENTER;");
        
        horarioTable.setItems(horarios);
    }

    private void configurarListeners() {
        ausenciaCompletaCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            horarioContainer.setDisable(newVal);
            marcarTodoElDia(newVal);
        });
        
        fechaPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && docenteActual != null) {
                cargarHorarioParaFecha(newVal);
            }
        });
    }

    public void setDocenteActual(Usuario docente) {
        this.docenteActual = docente;
        fechaPicker.setValue(LocalDate.now());
    }

    private void cargarHorarioParaFecha(LocalDate fecha) {
        int diaNumero = fecha.getDayOfWeek().getValue();

        String diaSemana;
        switch(diaNumero) {
            case 1: diaSemana = "L"; break;
            case 2: diaSemana = "M"; break;
            case 3: diaSemana = "X"; break;
            case 4: diaSemana = "J"; break;
            case 5: diaSemana = "V"; break;
            default:
                diaSemana = "";
        }

        if (diaSemana.isEmpty()) {
            horarios.clear();
            mostrarMensaje("No hay clases en fin de semana", false);
            return;
        }

        String query = "SELECT id, hora_desde, hora_fins, grup, aula " +
                       "FROM horari_grup " +
                       "WHERE docent = ? AND dia_setmana = ? " +
                       "ORDER BY hora_desde";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, docenteActual.getDocument());
            stmt.setString(2, diaSemana);

            ResultSet rs = stmt.executeQuery();
            horarios.clear();

            while (rs.next()) {
                int id = rs.getInt("id");
                LocalTime horaInicio = LocalTime.parse(rs.getString("hora_desde"));
                LocalTime horaFin = LocalTime.parse(rs.getString("hora_fins"));
                String grupo = rs.getString("grup");
                String aula = rs.getString("aula");

                horarios.add(new Horario(id, diaSemana, horaInicio, horaFin, grupo, aula, false));
            }

            if (horarios.isEmpty()) {
                mostrarMensaje("No tiene clases programadas para este día", false);
            } else {
                mostrarMensaje("Seleccione las horas que faltará", false);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarMensaje("Error al cargar horario: " + e.getMessage(), true);
        }
    }

    private void marcarTodoElDia(boolean faltara) {
        horarios.forEach(horario -> horario.setFaltara(faltara));
        horarioTable.refresh();
    }

    @FXML
    private void reportarInasistencia() {
        if (!validarCampos()) return;

        LocalDate fecha = fechaPicker.getValue();
        String tipo = tipoInasistenciaCombo.getValue();
        boolean completa = ausenciaCompletaCheck.isSelected();
        String observaciones = observacionesArea.getText();

        if (guardarInasistencia(fecha, tipo, completa, observaciones)) {
            mostrarMensaje("Inasistencia reportada correctamente", false);
            limpiarFormulario();
            cerrarVentanaConRetraso();
        } else {
            mostrarMensaje("Error al reportar inasistencia", true);
        }
    }

    private boolean validarCampos() {
        if (fechaPicker.getValue() == null) {
            mostrarMensaje("Seleccione una fecha", true);
            return false;
        }
        if (tipoInasistenciaCombo.getValue() == null) {
            mostrarMensaje("Seleccione un tipo de inasistencia", true);
            return false;
        }
        if (!ausenciaCompletaCheck.isSelected() && horarios.stream().noneMatch(Horario::isFaltara)) {
            mostrarMensaje("Seleccione al menos una hora de inasistencia", true);
            return false;
        }
        return true;
    }

    private boolean guardarInasistencia(LocalDate fecha, String tipo, boolean completa, String observaciones) {
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            if (completa) {
                for (Horario horario : horarios) {
                    guardarInasistenciaYGuardia(conn, horario, fecha, tipo, observaciones);
                }
            } else {
                for (Horario horario : horarios) {
                    if (horario.isFaltara()) {
                        guardarInasistenciaYGuardia(conn, horario, fecha, tipo, observaciones);
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void guardarInasistenciaYGuardia(Connection conn, Horario horario, LocalDate fecha, 
                                             String tipo, String observaciones) throws SQLException {
        String insertInasistencia = "INSERT INTO absencia " +
                                    "(docente_id, fecha, tipo_absencia, hora_inici, hora_fi, observaciones, justificada) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, false)";
        try (PreparedStatement stmt = conn.prepareStatement(insertInasistencia)) {
            stmt.setString(1, docenteActual.getDocument());
            stmt.setDate(2, Date.valueOf(fecha));
            stmt.setString(3, tipo);
            stmt.setTime(4, Time.valueOf(horario.getHoraInicio()));
            stmt.setTime(5, Time.valueOf(horario.getHoraFin()));
            stmt.setString(6, observaciones);
            stmt.executeUpdate();
        }

        String insertGuardia = "INSERT INTO guardia " +
                               "(docente_id, fecha, hora_inici, hora_fi, aula, disponible) " +
                               "VALUES (?, ?, ?, ?, ?, true)";
        try (PreparedStatement stmt = conn.prepareStatement(insertGuardia)) {
            stmt.setString(1, docenteActual.getDocument());
            stmt.setDate(2, Date.valueOf(fecha));
            stmt.setTime(3, Time.valueOf(horario.getHoraInicio()));
            stmt.setTime(4, Time.valueOf(horario.getHoraFin()));
            stmt.setString(5, horario.getAula());
            stmt.executeUpdate();
        }
    }

    private void mostrarMensaje(String mensaje, boolean esError) {
        mensajeLabel.setText(mensaje);
        mensajeLabel.setStyle(esError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    private void limpiarFormulario() {
        ausenciaCompletaCheck.setSelected(false);
        observacionesArea.clear();
        tipoInasistenciaCombo.getSelectionModel().selectFirst();
        horarios.clear();
    }

    private void cerrarVentanaConRetraso() {
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> {
            Stage stage = (Stage) mensajeLabel.getScene().getWindow();
            stage.close();
        });
        delay.play();
    }

    @FXML
    private void cancelar() {
        Stage stage = (Stage) mensajeLabel.getScene().getWindow();
        stage.close();
    }
}
