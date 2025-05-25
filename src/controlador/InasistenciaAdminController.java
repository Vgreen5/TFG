package controlador;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
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

public class InasistenciaAdminController implements Initializable {

    @FXML private TextField docenteIdField;
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
            "Enfermedad", "Asuntos personales", "Formación", "Otros"
        );
        tipoInasistenciaCombo.getSelectionModel().selectFirst();
    }

    private void configurarTabla() {
        horaInicioCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHoraInicio().format(timeFormatter)));
        horaFinCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHoraFin().format(timeFormatter)));
        grupoCol.setCellValueFactory(cell -> cell.getValue().grupoProperty());
        aulaCol.setCellValueFactory(cell -> cell.getValue().aulaProperty());
        faltaraCol.setCellValueFactory(cell -> cell.getValue().faltaraProperty());
        faltaraCol.setCellFactory(CheckBoxTableCell.forTableColumn(faltaraCol));
        faltaraCol.setEditable(true);

        horarioTable.setEditable(true);
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

    private void marcarTodoElDia(boolean faltara) {
        horarios.forEach(horario -> horario.setFaltara(faltara));
        horarioTable.refresh();
    }

    private Usuario obtenerDocentePorId(String id) {
        String query = "SELECT nom, cognom1, cognom2, tipo_doc, document, sexe, data_ingres, hores_lloc, hores_dedicades, data_naix, ensenyament, organisme FROM docent WHERE document = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Usuario(
                    rs.getString("document"),
                    rs.getString("nom"),
                    rs.getString("cognom1"),
                    rs.getString("cognom2"),
                    rs.getString("tipo_doc"),
                    rs.getString("sexe"),
                    rs.getString("data_ingres"),
                    rs.getString("hores_lloc"),
                    rs.getString("hores_dedicades"),
                    rs.getString("data_naix"),
                    rs.getInt("ensenyament") == 1,
                    rs.getInt("organisme") == 1,
                    null,   
                    false  
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void cargarHorarioParaFecha(LocalDate fecha) {
        int diaNumero = fecha.getDayOfWeek().getValue();
        String diaSemana = switch (diaNumero) {
            case 1 -> "L";
            case 2 -> "M";
            case 3 -> "X";
            case 4 -> "J";
            case 5 -> "V";
            default -> "";
        };

        if (diaSemana.isEmpty()) {
            horarios.clear();
            mostrarMensaje("No hay clases en fin de semana", false);
            return;
        }

        String query = "SELECT id, hora_desde, hora_fins, grup, aula FROM horari_grup WHERE docent = ? AND dia_setmana = ? ORDER BY hora_desde";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, docenteActual.getDocument());
            stmt.setString(2, diaSemana);

            ResultSet rs = stmt.executeQuery();
            horarios.clear();
            while (rs.next()) {
                horarios.add(new Horario(
                    rs.getInt("id"),
                    diaSemana,
                    LocalTime.parse(rs.getString("hora_desde")),
                    LocalTime.parse(rs.getString("hora_fins")),
                    rs.getString("grup"),
                    rs.getString("aula"),
                    false
                ));
            }

            mostrarMensaje(horarios.isEmpty() ?
                "No tiene clases programadas para este día" :
                "Seleccione las horas que faltará", false);

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarMensaje("Error al cargar horario: " + e.getMessage(), true);
        }
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
        String docenteId = docenteIdField.getText();
        if (docenteId == null || docenteId.isBlank()) {
            mostrarMensaje("Debe ingresar el ID del docente", true);
            return false;
        }

        docenteActual = obtenerDocentePorId(docenteId);
        if (docenteActual == null) {
            mostrarMensaje("Docente no encontrado", true);
            return false;
        }

        if (fechaPicker.getValue() == null) {
            mostrarMensaje("Seleccione una fecha", true);
            return false;
        }

        if (!ausenciaCompletaCheck.isSelected() && horarios.stream().noneMatch(Horario::isFaltara)) {
            mostrarMensaje("Seleccione al menos una hora de inasistencia", true);
            return false;
        }

        return true;
    }

    private boolean guardarInasistencia(LocalDate fecha, String tipo, boolean completa, String observaciones) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);
            for (Horario horario : horarios) {
                if (completa || horario.isFaltara()) {
                    guardarInasistenciaYGuardia(conn, horario, fecha, tipo, observaciones);
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void guardarInasistenciaYGuardia(Connection conn, Horario horario, LocalDate fecha,
            String tipo, String observaciones) throws SQLException {
    	
String insertInasistencia = "INSERT INTO absencia (docente_id, fecha, tipo_absencia, hora_inici, hora_fi, observaciones, justificada) VALUES (?, ?, ?, ?, ?, ?, 0)";
try (PreparedStatement stmt = conn.prepareStatement(insertInasistencia)) {
stmt.setString(1, docenteActual.getDocument());
stmt.setDate(2, Date.valueOf(fecha));
stmt.setString(3, tipo);
if (horario.getHoraInicio() != null)
stmt.setTime(4, Time.valueOf(horario.getHoraInicio()));
else
stmt.setNull(4, Types.TIME);
if (horario.getHoraFin() != null)
stmt.setTime(5, Time.valueOf(horario.getHoraFin()));
else
stmt.setNull(5, Types.TIME);
stmt.setString(6, observaciones);
stmt.executeUpdate();
}

// Insertar en guardia
String insertGuardia = "INSERT INTO guardia (docente_id, fecha, hora_inici, hora_fi, aula, disponible) VALUES (?, ?, ?, ?, ?, 1)";
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
        docenteIdField.clear();
        fechaPicker.setValue(null);
        ausenciaCompletaCheck.setSelected(false);
        observacionesArea.clear();
        tipoInasistenciaCombo.getSelectionModel().selectFirst();
        horarios.clear();
    }

    private void cerrarVentanaConRetraso() {
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> ((Stage) mensajeLabel.getScene().getWindow()).close());
        delay.play();
    }
    @FXML
    private void cargarDocenteYHorario() {
        String id = docenteIdField.getText();
        if (id == null || id.isBlank()) {
            mostrarMensaje("Ingrese el ID del docente", true);
            horarios.clear();
            return;
        }

        Usuario docente = obtenerDocentePorId(id);
        if (docente == null) {
            mostrarMensaje("Docente no encontrado", true);
            horarios.clear();
            return;
        }

        docenteActual = docente;
        mostrarMensaje("Docente cargado: " + docente.getNom(), false);

        if (fechaPicker.getValue() != null) {
            cargarHorarioParaFecha(fechaPicker.getValue());
        } else {
            horarios.clear();
            mostrarMensaje("Seleccione una fecha para cargar el horario", false);
        }
    }

    @FXML
    private void cancelar() {
        ((Stage) mensajeLabel.getScene().getWindow()).close();
    }
}
