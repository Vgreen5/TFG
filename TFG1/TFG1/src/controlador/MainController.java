package controlador;

import javafx.animation.Animation; 
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty; 
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import modelo.Absencia;
import modelo.Guardia;
import utilidades.LoggerUtil;
import modelo.Usuario;
import databases.DatabaseConnector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

     
    @FXML private Label welcomeLabel;
    @FXML private Label dniLabel;
    @FXML private Label emailLabel;
    @FXML private Label rolLabel;
    @FXML private Label jornadaStatusLabel;
    
    
    @FXML private Tab reportsTab;
  
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private CheckBox cbFaltas;
    @FXML private CheckBox cbGuardias;
    
    @FXML private Label lblUsuarioInforme;
    @FXML private Label lblPeriodoInforme;
    @FXML private Label lblTotalFaltas;
    @FXML private Label lblTotalGuardias;
    
    @FXML private TableView<Absencia> tablaFaltas;
    @FXML private TableColumn<Absencia, String> colFechaFalta;
    @FXML private TableColumn<Absencia, String> colHoraFalta;
    @FXML private TableColumn<Absencia, String> colMotivoFalta;
    @FXML private TableColumn<Absencia, String> colJustificadaFalta;
    @FXML private TextField txtDocenteId;

    @FXML private TableView<Guardia> tablaGuardiasInforme;
    @FXML private TableColumn<Guardia, String> colFechaGuardia;
    @FXML private TableColumn<Guardia, String> colHoraInicioGuardia;
    @FXML private TableColumn<Guardia, String> colHoraFinGuardia;
    @FXML private TableColumn<Guardia, String> colAulaGuardia;
    @FXML private TableColumn<Guardia, String> colDocenteGuardia;
    
    @FXML private Menu adminMenu;
    @FXML private VBox guardList;
    @FXML private VBox absencesList;
 
    private Usuario usuarioActual;
    private JornadaController jornadaController;

    private LoginController loggerController;
    
    // Inicializadores
   //=========================================================================================
   
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTablasInformes();
        configurarFiltrosPorDefecto();
        iniciarRecordatorioJornada();
    }
    
    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
       
        if (loggerController == null) {
            loggerController = new LoginController();
            loggerController.setMainController(this);
        }
        loggerController.setUsuarioActual(usuario);
        loggerController.registrarLogin();  
        
       
        actualizarDatosUsuario();
        configurarVisibilidadSegunRol();
        cargarGuardiasDisponibles();
       
        if (jornadaController != null) {
            jornadaController.setDocenteActual(usuario);
        }
    }
    public void actualizarEstadoJornada() {
       
        if (jornadaController != null) {
            jornadaController.verificarEstadoJornada();
        }
    }
    
    private void actualizarDatosUsuario() {
        if (usuarioActual != null) {
            welcomeLabel.setText("Bienvenido, " + usuarioActual.getNom());
            dniLabel.setText("DNI: " + usuarioActual.getDocument());
            emailLabel.setText("Email: " + usuarioActual.getMail());
            rolLabel.setText("Rol: " + (usuarioActual.isEsAdmin() ? "Administrador" : "Docente"));
        }
    }
    private void configurarVisibilidadSegunRol() {
        boolean esAdmin = usuarioActual != null && usuarioActual.isEsAdmin();
        TabPane tabPane = reportsTab.getTabPane();
        
       
        if (esAdmin && !tabPane.getTabs().contains(reportsTab)) {
            tabPane.getTabs().add(reportsTab);
        } else if (!esAdmin) {
            tabPane.getTabs().remove(reportsTab);
        }
        
        adminMenu.setVisible(esAdmin);
    }
    
  //Fin Inicializador 
 //===============================================================================================


    
 //Guardias
//================================================================================================
    private void cargarGuardiasDisponibles() {
        guardList.getChildren().clear();
        List<Guardia> guardias = obtenerGuardiasDisponibles();
        
        for (Guardia g : guardias) {
            HBox item = new HBox(10);
            item.getStyleClass().add("guardia-item");
            
            String texto = String.format("Docente: %s, Aula: %s, Hora: %s - %s, Fecha: %s",
                g.getDocenteId(), g.getAula(),
                g.getHoraInicio() != null ? g.getHoraInicio().toString() : "-",
                g.getHoraFin() != null ? g.getHoraFin().toString() : "-",
                g.getFecha() != null ? g.getFecha().toString() : "-");
            
            Label label = new Label(texto);
            Button reservarBtn = new Button("Reservar");

            reservarBtn.getStyleClass().add("accept-button");
            
            reservarBtn.setOnAction(e -> reservarGuardia(g.getId()));
            
            item.getChildren().addAll(label, reservarBtn);
            guardList.getChildren().add(item);
        }
    }

    
    private List<Guardia> obtenerGuardiasDisponibles() {
        List<Guardia> guardias = new ArrayList<>();
        String sql = "SELECT g.id, g.docente_id, g.fecha, g.hora_inici, g.hora_fi, g.aula, " +
                     "CONCAT(d.nom, ' ', d.cognom1, ' ', d.cognom2) as nombre_docente " +
                     "FROM guardia g JOIN docent d ON g.docente_id = d.document " +
                     "WHERE g.disponible = 1 AND g.fecha >= ? " + 
                     "ORDER BY g.fecha, g.hora_inici";  
        
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
             stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Guardia g = new Guardia();
                    g.setId(rs.getInt("id"));
                    g.setDocenteId(rs.getString("docente_id"));
                    g.setDocenteNombreCompleto(rs.getString("nombre_docente"));
                    g.setFecha(rs.getDate("fecha").toLocalDate());
                    g.setHoraInicio(rs.getTime("hora_inici").toLocalTime());
                    g.setHoraFin(rs.getTime("hora_fi").toLocalTime());
                    g.setAula(rs.getString("aula"));
                    guardias.add(g);
                }
            }
        } catch (SQLException e) {
            mostrarError("Error al obtener guardias: " + e.getMessage());
        }
        return guardias;
    }
    private void reservarGuardia(int idGuardia) {
        String sql = "UPDATE guardia SET disponible = 0, docente_sustituto = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioActual.getDocument());
            stmt.setInt(2, idGuardia);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                mostrarAlerta("Éxito", "Guardia reservada correctamente");
                cargarGuardiasDisponibles();
            } else {
                mostrarError("No se pudo reservar la guardia");
            }
        } catch (SQLException e) {
            mostrarError("Error al reservar guardia: " + e.getMessage());
        }
    }

    // Fin Guardias
   //================================================================================================
    
    
    
    
  //Informes
 //======================================================================================================
    @FXML
    private void limpiarFiltrosInforme() {
        txtDocenteId.clear();
        dpFechaInicio.setValue(null);
        dpFechaFin.setValue(null);
        cbFaltas.setSelected(true);
        cbGuardias.setSelected(true);

        tablaFaltas.getItems().clear();
        tablaGuardiasInforme.getItems().clear();

        lblUsuarioInforme.setText("");
        lblPeriodoInforme.setText("");
        lblTotalFaltas.setText("0");
        lblTotalGuardias.setText("0");
       
    }
   
    private boolean validarDocenteExiste(String docenteId) {
        String sql = "SELECT COUNT(*) FROM docent WHERE document = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, docenteId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            mostrarError("Error al validar docente: " + e.getMessage());
        }
        return false;
    }


    @FXML
    private void generarInforme() {
        if (!validarFiltrosInforme()) {
            return;
        }

        String docenteId = txtDocenteId.getText().trim();
        if (!docenteId.isEmpty() && !validarDocenteExiste(docenteId)) {
            mostrarError("El docente con ID ingresado no existe.");
            return;
        }

        LocalDate fechaInicio = dpFechaInicio.getValue();
        LocalDate fechaFin = dpFechaFin.getValue();

        lblUsuarioInforme.setText(!docenteId.isEmpty() ? docenteId : "Todos los usuarios");
        lblPeriodoInforme.setText(fechaInicio + " - " + fechaFin);

        if (cbFaltas.isSelected()) {
            List<Absencia> faltas = obtenerFaltasParaInforme(
                    !docenteId.isEmpty() ? docenteId : null,
                    fechaInicio, fechaFin
            );
            tablaFaltas.setItems(FXCollections.observableArrayList(faltas));
            lblTotalFaltas.setText(String.valueOf(faltas.size()));
        } else {
            tablaFaltas.getItems().clear();
            lblTotalFaltas.setText("0");
        }

        if (cbGuardias.isSelected()) {
            List<Guardia> guardias = obtenerGuardiasParaInforme(
                    !docenteId.isEmpty() ? docenteId : null,
                    fechaInicio, fechaFin
            );
            tablaGuardiasInforme.setItems(FXCollections.observableArrayList(guardias));
            lblTotalGuardias.setText(String.valueOf(guardias.size()));
        } else {
            tablaGuardiasInforme.getItems().clear();
            lblTotalGuardias.setText("0");
        }

        
    }

    @FXML
    private void exportarInformeTXT() {
        if (!validarFiltrosInforme()) {
            return;
        }

         
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar informe como TXT");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos de texto", "*.txt")
        );
        fileChooser.setInitialFileName("informe_" + LocalDate.now().toString() + ".txt");
        
        File file = fileChooser.showSaveDialog(tablaFaltas.getScene().getWindow());
        
        if (file == null) {
            return; 
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            
            writer.write("INFORME DOCENTE\n");
            writer.write("================\n\n");
            writer.write("Usuario: " + lblUsuarioInforme.getText() + "\n");
            writer.write("Periodo: " + lblPeriodoInforme.getText() + "\n\n");

            
            if (cbFaltas.isSelected() && !tablaFaltas.getItems().isEmpty()) {
                writer.write("FALTAS (" + lblTotalFaltas.getText() + ")\n");
                writer.write("----------------------------------------\n");
                writer.write(String.format("%-10s %-12s %-20s %-10s\n", 
                    "Fecha", "Horario", "Motivo", "Justificada"));
                writer.write("----------------------------------------\n");
                
                for (Absencia falta : tablaFaltas.getItems()) {
                    writer.write(String.format("%-10s %-12s %-20s %-10s\n",
                        falta.getFecha().toString(),
                        (falta.getHoraInicio() != null ? falta.getHoraInicio().toString() : "Completo") + 
                        (falta.getHoraFin() != null ? " - " + falta.getHoraFin().toString() : ""),
                        falta.getTipoAbsencia(),
                        falta.isJustificada() ? "Sí" : "No"));
                }
                writer.write("\n");
            }
 
            if (cbGuardias.isSelected() && !tablaGuardiasInforme.getItems().isEmpty()) {
                writer.write("GUARDIAS (" + lblTotalGuardias.getText() + ")\n");
                writer.write("--------------------------------------------------\n");
                writer.write(String.format("%-10s %-12s %-8s %-20s\n", 
                    "Fecha", "Horario", "Aula", "Estado"));
                writer.write("--------------------------------------------------\n");
                
                for (Guardia guardia : tablaGuardiasInforme.getItems()) {
                    writer.write(String.format("%-10s %-12s %-8s %-20s\n",
                        guardia.getFecha().toString(),
                        guardia.getHoraInicio().toString() + " - " + guardia.getHoraFin().toString(),
                        guardia.getAula(),
                        guardia.getEstado()));
                }
                writer.write("\n");
            }

        
            mostrarAlerta("Exportación exitosa", 
                "El informe se ha guardado correctamente en:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            mostrarError("Error al exportar el informe: " + e.getMessage());
        }
    }

    private boolean validarFiltrosInforme() {
        if (dpFechaInicio.getValue() == null || dpFechaFin.getValue() == null) {
            mostrarError("Debe seleccionar un rango de fechas.");
            return false;
        }
        if (dpFechaInicio.getValue().isAfter(dpFechaFin.getValue())) {
            mostrarError("La fecha de inicio no puede ser posterior a la fecha fin.");
            return false;
        }
        if (!cbFaltas.isSelected() && !cbGuardias.isSelected()) {
            mostrarError("Debe seleccionar al menos un tipo de informe (Faltas o Guardias).");
            return false;
        }
        return true;
    }


    private void configurarTablasInformes() {
        // Tabla faltas
        colFechaFalta.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFecha().toString()));
        colHoraFalta.setCellValueFactory(cellData -> {
            Absencia abs = cellData.getValue();
            return new SimpleStringProperty(
                (abs.getHoraInicio() != null ? abs.getHoraInicio().toString() : "-") + " / " +
                (abs.getHoraFin() != null ? abs.getHoraFin().toString() : "-")
            );
        });
        colMotivoFalta.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTipoAbsencia()));
        colJustificadaFalta.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isJustificada() ? "Sí" : "No"));

        // Tabla guardias
        colFechaGuardia.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFecha().toString()));
        colHoraInicioGuardia.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getHoraInicio().toString()));
        colHoraFinGuardia.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getHoraFin().toString()));
        colAulaGuardia.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAula()));
        colDocenteGuardia.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDocenteNombreCompleto()));
    }

    private List<Absencia> obtenerFaltasParaInforme(String docenteId, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Absencia> faltas = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.id, a.docente_id, a.fecha, a.tipo_absencia, a.hora_inici, a.hora_fi, a.justificada, " +
            "CONCAT(d.nom, ' ', d.cognom1, ' ', d.cognom2) as nombre_docente " +
            "FROM absencia a JOIN docent d ON a.docente_id = d.document " +
            "WHERE a.fecha BETWEEN ? AND ?"
        );

        if (docenteId != null) {
            sql.append(" AND a.docente_id = ?");
        }
        sql.append(" ORDER BY a.fecha DESC");

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            stmt.setDate(1, Date.valueOf(fechaInicio));
            stmt.setDate(2, Date.valueOf(fechaFin));
            
            if (docenteId != null) {
                stmt.setString(3, docenteId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Absencia abs = new Absencia();
                abs.setId(rs.getInt("id"));
                abs.setDocenteId(rs.getString("docente_id"));
                abs.setFecha(rs.getDate("fecha").toLocalDate());
                abs.setTipoAbsencia(rs.getString("tipo_absencia"));
                abs.setHoraInicio(rs.getTime("hora_inici").toLocalTime());
                abs.setHoraFin(rs.getTime("hora_fi").toLocalTime());
                abs.setJustificada(rs.getBoolean("justificada"));
                faltas.add(abs);
            }
        } catch (SQLException e) {
            mostrarError("Error al obtener faltas: " + e.getMessage());
        }
        return faltas;
    }

    private List<Guardia> obtenerGuardiasParaInforme(String docenteId, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Guardia> guardias = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT g.id, g.docente_id, g.fecha, g.hora_inici, g.hora_fi, g.aula, g.docente_sustituto, " +
            "CONCAT(d1.nom, ' ', d1.cognom1, ' ', d1.cognom2) as nombre_docente, " +
            "CONCAT(d2.nom, ' ', d2.cognom1, ' ', d2.cognom2) as nombre_sustituto " +
            "FROM guardia g " +
            "LEFT JOIN docent d1 ON g.docente_id = d1.document " +
            "LEFT JOIN docent d2 ON g.docente_sustituto = d2.document " +
            "WHERE g.disponible = 0 AND g.fecha BETWEEN ? AND ?"
        );

        if (docenteId != null) {
            sql.append(" AND (g.docente_id = ? OR g.docente_sustituto = ?)");
        }
        sql.append(" ORDER BY g.fecha DESC");

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            stmt.setDate(1, Date.valueOf(fechaInicio));
            stmt.setDate(2, Date.valueOf(fechaFin));
            
            if (docenteId != null) {
                stmt.setString(3, docenteId);
                stmt.setString(4, docenteId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Guardia g = new Guardia();
                g.setId(rs.getInt("id"));
                g.setDocenteId(rs.getString("docente_id"));
                g.setFecha(rs.getDate("fecha").toLocalDate());
                g.setHoraInicio(rs.getTime("hora_inici").toLocalTime());
                g.setHoraFin(rs.getTime("hora_fi").toLocalTime());
                g.setAula(rs.getString("aula"));
                g.setDocenteNombreCompleto(rs.getString("nombre_docente"));
                
                String sustituto = rs.getString("nombre_sustituto");
                if (sustituto != null) {
                    g.setEstado("Cubierta por " + sustituto);
                } else {
                    g.setEstado("Pendiente de cubrir");
                }
                
                guardias.add(g);
            }
        } catch (SQLException e) {
            mostrarError("Error al obtener guardias: " + e.getMessage());
        }
        return guardias;
    }

    

    private void configurarFiltrosPorDefecto() {
        dpFechaInicio.setValue(LocalDate.now().minusMonths(1));
        dpFechaFin.setValue(LocalDate.now());
        cbFaltas.setSelected(true);
        cbGuardias.setSelected(true);
    }

    
    
   //Fin Informes
  //=====================================================================================================================
    
    
    
    
  //Menu
 //=============================================================================================================}

    @FXML
    private void cerrarSesion() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar cierre de sesión");
        confirmacion.setHeaderText("¿Estás seguro de que deseas cerrar sesión?");
        confirmacion.setContentText("Se perderán los cambios no guardados.");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                 LoggerUtil.logLogout(usuarioActual.getDocument());
                
                Stage stage = (Stage) welcomeLabel.getScene().getWindow();
                stage.close();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/logger.fxml"));
                Stage loginStage = new Stage();
                loginStage.setWidth(800);
                loginStage.setHeight(600);
                loginStage.setScene(new Scene(loader.load()));
                loginStage.setTitle("Inicio de Sesión");
                loginStage.show();
            } catch (Exception e) {
                mostrarError("Error al cerrar sesión: " + e.getMessage());
            }
        }
    }

    @FXML
    private void manejarJornada() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/EntradaSalida.fxml"));
            Parent root = loader.load();
            
            JornadaController controller = loader.getController();
            controller.setDocenteActual(usuarioActual);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Jornada Laboral");
            stage.show();
        } catch (Exception e) {
            mostrarError("Error al abrir gestión de jornada: " + e.getMessage());
        }
    }

    @FXML
    private void verGuardiasDisponibles() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/Guardias.fxml"));
            Parent root = loader.load();

            GuardiasController controller = loader.getController();
            controller.setDocenteActual(usuarioActual);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Guardias Disponibles");
            stage.show();
        } catch (Exception e) {
            mostrarError("Error al abrir gestión de guardias: " + e.getMessage());
        }
    }

    @FXML
    private void verMisGuardias() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/misGuardias.fxml"));
            Parent root = loader.load();

            MiGuardiaController controller = loader.getController();
            controller.setDocenteActual(usuarioActual);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Mis Guardias Asignadas");
            stage.show();
        } catch (Exception e) {
            mostrarError("Error al abrir gestión de guardias: " + e.getMessage());
        }
    }

  @FXML
  private void GestionarUsuario() {
	  try {
		    FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/Gestion.fxml"));
		    Parent root = loader.load();


		    Stage stage = new Stage();
		    stage.setTitle("Gestión de Usuarios");
		    stage.setScene(new Scene(root));
		    stage.initModality(Modality.APPLICATION_MODAL);
		    stage.showAndWait();
		} catch (Exception e) {
		    mostrarError("Error al abrir gestión: " + e.getMessage());
		}

  }
 
  @FXML
  private void InasistenciaAdmin() {
	  try {
		    FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/faltaAdmin.fxml"));
		    Parent root = loader.load();
		    

		    Stage stage = new Stage();
		    stage.setTitle("Gestión de Usuarios");
		    stage.setScene(new Scene(root));
		    stage.initModality(Modality.APPLICATION_MODAL);
		    stage.showAndWait();
		} catch (Exception e) {
		    mostrarError("Error al abrir gestión: " + e.getMessage());
		}

  }
 
  
  //Fin Menu
 //==================================================================================================================================
    
    
    
  private void iniciarRecordatorioJornada() {
	    Timeline timeline = new Timeline(
	        new KeyFrame(Duration.minutes(1), 
	        event -> verificarHoraSalida()
	    ));
	    timeline.setCycleCount(Animation.INDEFINITE);
	    timeline.play();
	    
 	    verificarHoraSalida();
	}

	private void verificarHoraSalida() {
	    LocalTime ahora = LocalTime.now();
	    
	    if ((ahora.getHour() == 14 && ahora.getMinute() == 45) || 
	        (ahora.getHour() == 21 && ahora.getMinute() == 35)) {
	        
	        mostrarNotificacionSalida();
	    }
	}

	private void mostrarNotificacionSalida() {
	    Platform.runLater(() -> {
	        Alert alert = new Alert(Alert.AlertType.INFORMATION);
	        alert.setTitle("Recordatorio de finalización de jornada");
	        alert.setHeaderText(null);
	        alert.setContentText("¡Recuerda finalizar tu jornada! Quedan 5 minutos para terminar.");
	        alert.show();
	    });
	}
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
   
}