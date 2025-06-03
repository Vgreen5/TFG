package controlador;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import modelo.Usuario;
import databases.DatabaseConnector;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class JornadaController {

    @FXML private Label estadoActualLabel;
    @FXML private Label horaActualLabel;
    @FXML private Label inicioRegistradoLabel;
    @FXML private Label finRegistradoLabel;
    @FXML private Label fechaActualLabel;
    @FXML private VBox mensajeContainer;
    @FXML private Label mensajeLabel;
    @FXML private Button btnRegistrarInicio;
    @FXML private Button btnRegistrarFin;
    @FXML private Label estadoJornadaLabel;
    @FXML private Label ultimaAccionLabel;

    private Usuario docenteActual;
    private boolean jornadaActiva = false;

    private static final LocalTime HORA_CIERRE = LocalTime.of(21, 20);
    private static final LocalTime HORA_AVISO = HORA_CIERRE.minusMinutes(5);
    private final DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarReloj();
        verificarHoraParaAviso();
    }

    public void setDocenteActual(Usuario docente) {
        this.docenteActual = docente;
        verificarEstadoJornada();
    }

    private void configurarReloj() {
        actualizarHoraActual();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> actualizarHoraActual()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        horaActualLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) timeline.stop();
        });
    }

    private void actualizarHoraActual() {
        LocalDateTime ahora = LocalDateTime.now();
        horaActualLabel.setText(ahora.format(horaFormatter));
        fechaActualLabel.setText(ahora.format(fechaFormatter));
    }

    private void verificarHoraParaAviso() {
        LocalTime ahora = LocalTime.now();
        if (ahora.isAfter(HORA_AVISO) && ahora.isBefore(HORA_CIERRE)) {
            mostrarAvisoCierreProximo();
        } else if (ahora.isAfter(HORA_CIERRE)) {
            registrarSalidaAutomatica();
        }
    }

    private void mostrarAvisoCierreProximo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aviso de Jornada");
        alert.setHeaderText("La jornada cerrará a las " + HORA_CIERRE);
        alert.setContentText("Por favor registra tu salida antes de las 21:20");
        alert.showAndWait();
    }

    void verificarEstadoJornada() {
        if (docenteActual == null) {
            mostrarMensajeError("No se ha asignado docente.");
            return;
        }

        String query = "SELECT hora_entrada, hora_salida FROM entrada_salida WHERE docente_id = ? AND fecha = CURDATE()";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, docenteActual.getDocument());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Time entrada = rs.getTime("hora_entrada");
                Time salida = rs.getTime("hora_salida");
                actualizarInterfazSegunEstado(entrada, salida);
            } else {
                setEstadoInactivo();
            }
        } catch (SQLException e) {
            mostrarMensajeError("Error al verificar estado de jornada: " + e.getMessage());
        }
    }

    private void actualizarInterfazSegunEstado(Time entrada, Time salida) {
        if (entrada != null && salida == null) {
            setEstadoActivo(entrada);
        } else if (entrada != null && salida != null) {
            setEstadoCompletado(entrada, salida);
        } else {
            setEstadoInactivo();
        }
    }

    private void setEstadoActivo(Time entrada) {
        estadoActualLabel.setText("JORNADA ACTIVA");
        inicioRegistradoLabel.setText("Inicio: " + entrada.toString());
        jornadaActiva = true;
        btnRegistrarInicio.setDisable(true);
        btnRegistrarFin.setDisable(false);
    }

    private void setEstadoCompletado(Time entrada, Time salida) {
        estadoActualLabel.setText("JORNADA COMPLETADA");
        inicioRegistradoLabel.setText("Inicio: " + entrada.toString());
        finRegistradoLabel.setText("Fin: " + salida.toString());
        jornadaActiva = false;
        btnRegistrarInicio.setDisable(true);
        btnRegistrarFin.setDisable(true);
    }

    private void setEstadoInactivo() {
        estadoActualLabel.setText("SIN JORNADA INICIADA");
        jornadaActiva = false;
        btnRegistrarInicio.setDisable(false);
        btnRegistrarFin.setDisable(true);
    }

    @FXML
    private void registrarInicioJornada() {
        if (docenteActual == null) return;

        String query = "INSERT INTO entrada_salida (docente_id, fecha, hora_entrada) " +
                "VALUES (?, CURDATE(), CURTIME()) " +
                "ON DUPLICATE KEY UPDATE hora_entrada = IF(hora_entrada IS NULL, CURTIME(), hora_entrada)";

        ejecutarOperacionBD(query, "Jornada iniciada correctamente", "Error al iniciar jornada");
    }

    @FXML
    private void registrarFinJornada() {
        if (docenteActual == null || !jornadaActiva) return;

        String query = "UPDATE entrada_salida SET hora_salida = CURTIME() " +
                "WHERE docente_id = ? AND fecha = CURDATE()";

        ejecutarOperacionBD(query, "Jornada finalizada correctamente", "Error al finalizar jornada");
    }

  

    private void registrarSalidaAutomatica() {
        if (docenteActual == null || !jornadaActiva) return;

        String query = "UPDATE entrada_salida SET hora_salida = '21:20:00' " +
                "WHERE docente_id = ? AND fecha = CURDATE() AND hora_salida IS NULL";

        ejecutarOperacionBD(query, "Jornada finalizada automáticamente", "Error al finalizar jornada automáticamente");
    }

    private void ejecutarOperacionBD(String query, String mensajeExito, String mensajeError) {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, docenteActual.getDocument());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                mostrarMensajeExito(mensajeExito);
                verificarEstadoJornada();
            } else {
                mostrarMensajeError(mensajeError);
            }
        } catch (SQLException e) {
            mostrarMensajeError(mensajeError + ": " + e.getMessage());
        }
    }

    private void mostrarMensajeExito(String mensaje) {
        mensajeLabel.setText(mensaje);
        mensajeLabel.setStyle("-fx-text-fill: #2E7D32;");
        mensajeContainer.setVisible(true);
        ocultarMensajeDespues(5000);
    }

    private void mostrarMensajeError(String mensaje) {
        mensajeLabel.setText(mensaje);
        mensajeLabel.setStyle("-fx-text-fill: #D32F2F;");
        mensajeContainer.setVisible(true);
        ocultarMensajeDespues(5000);
    }

    private void ocultarMensajeDespues(int milisegundos) {
        new java.util.Timer().schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> mensajeContainer.setVisible(false));
                }
            },
            milisegundos
        );
    }
}
