package controlador;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import utilidades.LoggerUtil;
import modelo.Usuario;
import databases.DatabaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class LoginController {

    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Button loginBtn;
    private static final int TIEMPO_ESPERA_MINUTOS = 10;
    private Timer timerInicioAutomatico;
    private Usuario usuarioActual;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public void registrarLogin() {
        if (usuarioActual != null) {
            LoggerUtil.logLogin(usuarioActual.getDocument());
            programarInicioAutomatico();
        }
    }

    public void registrarLogout() {
        cancelarTimer();
        if (usuarioActual != null) {
            LoggerUtil.logLogout(usuarioActual.getDocument());
        }
    }

    private void programarInicioAutomatico() {
        cancelarTimer();  
        
        timerInicioAutomatico = new Timer();
        timerInicioAutomatico.schedule(new TimerTask() {
            public void run() {
                if (!jornadaIniciadaHoy()) {
                    registrarInicioAutomatico();
                }
            }
        }, TIEMPO_ESPERA_MINUTOS * 60 * 1000);
    }

    private boolean jornadaIniciadaHoy() {
        String sql = "SELECT hora_entrada FROM entrada_salida WHERE docente_id = ? AND fecha = CURDATE()";
        
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioActual.getDocument());
            ResultSet rs = stmt.executeQuery();
            
            return rs.next() && rs.getTime("hora_entrada") != null;
        } catch (SQLException e) {
            System.err.println("Error al verificar jornada: " + e.getMessage());
            return true;
        }
    }

    private void registrarInicioAutomatico() {
        String sql = "INSERT INTO entrada_salida (docente_id, fecha, hora_entrada) " +
                     "VALUES (?, CURDATE(), ADDTIME(CURTIME(), '00:10:00')) " +
                     "ON DUPLICATE KEY UPDATE hora_entrada = IF(hora_entrada IS NULL, ADDTIME(CURTIME(), '00:10:00'), hora_entrada)";
        
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioActual.getDocument());
            stmt.executeUpdate();
            
            Platform.runLater(() -> {
                if (mainController != null) {
                    mainController.actualizarEstadoJornada();
                }
                mostrarNotificacion("Jornada Automática", 
                    "Se registró automáticamente su inicio de jornada");
            });
        } catch (SQLException e) {
            System.err.println("Error al registrar inicio automático: " + e.getMessage());
        }
    }

    private void mostrarNotificacion(String titulo, String mensaje) {
        Platform.runLater(() -> {
            
            System.out.println(titulo + ": " + mensaje);
        });
    }

    private void cancelarTimer() {
        if (timerInicioAutomatico != null) {
            timerInicioAutomatico.cancel();
            timerInicioAutomatico = null;
        }
    }
    @FXML
    public void initialize() {
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String documento = userField.getText().trim();
        String contrasena = passField.getText().trim();

        if (documento.isEmpty() || contrasena.isEmpty()) {
            mostrarAlerta("Campos vacíos", "Por favor ingrese su documento y contraseña", Alert.AlertType.WARNING);
            return;
        }

        try {
            Usuario usuario = autenticarUsuario(documento, contrasena);

            if (usuario != null) {
                LoggerUtil.logLogin(usuario.getDocument());
                abrirInterfazPrincipal(usuario);
            } else {
                mostrarAlerta("Acceso denegado", "Documento o contraseña incorrectos", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            mostrarAlerta("Error del sistema", "No se pudo verificar las credenciales: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private Usuario autenticarUsuario(String documento, String contrasena) throws SQLException {
        String sql = "SELECT d.document, d.nom, d.cognom1, d.cognom2, " +
                     "d.tipo_doc, d.sexe, d.data_ingres, d.hores_lloc, " +
                     "d.hores_dedicades, d.data_naix, d.ensenyament, d.organisme, " +
                     "l.mail, l.es_admin " +
                     "FROM login l " +
                     "JOIN docent d ON l.docente_id = d.document " +
                     "WHERE l.docente_id = ? AND l.password = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, documento);
            stmt.setString(2, contrasena);

            try (ResultSet rs = stmt.executeQuery()) {
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
                        rs.getBoolean("ensenyament"),
                        rs.getBoolean("organisme"),
                        rs.getString("mail"),
                        rs.getBoolean("es_admin")
                    );
                }
            }
        }
        return null;
    }
   

    private void abrirInterfazPrincipal(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/Main.fxml"));
            Parent root = loader.load();

            MainController controlador = loader.getController();
            controlador.setUsuarioActual(usuario);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema de Gestión Docente - " + usuario.getNombreCompleto());
            stage.setMaximized(true);

            Stage loginStage = (Stage) loginBtn.getScene().getWindow();
            loginStage.close();

            stage.show();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo cargar la interfaz principal: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
    
}