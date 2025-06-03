package controlador;

import databases.DatabaseConnector;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import modelo.Guardia;
import modelo.Usuario;

public class GuardiasController {

    @FXML private TableView<Guardia> tvGuardias;
    @FXML private TableColumn<Guardia, LocalDate> colFecha;
    @FXML private TableColumn<Guardia, LocalTime> colHoraInicio;
    @FXML private TableColumn<Guardia, LocalTime> colHoraFin;
    @FXML private TableColumn<Guardia, String> colAula;
    @FXML private TableColumn<Guardia, String> colDocente;
    @FXML private TableColumn<Guardia, String> colAccion;
    @FXML private DatePicker dpFechaFiltro;
    @FXML private Button btnLimpiarFiltro;
    @FXML private Button btnFiltrar;
    @FXML private Button btnRefrescar;
    @FXML private Label lblMensaje;

    private ObservableList<Guardia> guardiasDisponibles = FXCollections.observableArrayList();
    private Usuario docenteActual;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        configurarColumnasTabla();
        configurarFiltros();
    }

    private void configurarColumnasTabla() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHoraInicio.setCellValueFactory(new PropertyValueFactory<>("horaInicio"));
        colHoraFin.setCellValueFactory(new PropertyValueFactory<>("horaFin"));
        colAula.setCellValueFactory(new PropertyValueFactory<>("aula"));
        colDocente.setCellValueFactory(new PropertyValueFactory<>("docenteNombreCompleto"));

         colFecha.setCellFactory(col -> new TableCell<Guardia, LocalDate>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                setText(empty || fecha == null ? null : dateFormatter.format(fecha));
            }
        });
        
        colHoraInicio.setCellFactory(col -> new TableCell<Guardia, LocalTime>() {
            @Override
            protected void updateItem(LocalTime hora, boolean empty) {
                super.updateItem(hora, empty);
                setText(empty || hora == null ? null : timeFormatter.format(hora));
            }
        });
        
        colHoraFin.setCellFactory(col -> new TableCell<Guardia, LocalTime>() {
            @Override
            protected void updateItem(LocalTime hora, boolean empty) {
                super.updateItem(hora, empty);
                setText(empty || hora == null ? null : timeFormatter.format(hora));
            }
        });

         colAccion.setCellFactory(col -> new TableCell<Guardia, String>() {
            private final Button btnAsignar = new Button("Asignar");

            {
                btnAsignar.getStyleClass().add("btn-asignar");
                btnAsignar.setOnAction(e -> {
                    Guardia guardia = getTableView().getItems().get(getIndex());
                    asignarGuardia(guardia);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnAsignar);
            }
        });

        tvGuardias.setItems(guardiasDisponibles);
    }

    private void configurarFiltros() {
        dpFechaFiltro.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    public void setDocenteActual(Usuario usuario) {
        this.docenteActual = usuario;
        cargarGuardiasDisponibles();
    }

    @FXML
    private void cargarGuardiasDisponibles() {
        guardiasDisponibles.clear();

        boolean hayFiltroFecha = dpFechaFiltro.getValue() != null;

        String sql = "SELECT g.id, g.docente_id, d.nom, d.cognom1, d.cognom2, g.fecha, " +
                     "g.hora_inici, g.hora_fi, g.aula, g.disponible " +
                     "FROM guardia g " +
                     "JOIN docent d ON g.docente_id = d.document " +
                     "WHERE g.disponible = true " +
                     "AND g.docente_id != ? " +
                     "AND (g.docente_sustituto IS NULL OR g.docente_sustituto != ?)";

        if (hayFiltroFecha) {
            sql += " AND g.fecha = ?";
        } else {
            sql += " AND g.fecha >= ?";
        }

        sql += " ORDER BY g.fecha, g.hora_inici";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, docenteActual.getDocument());
            stmt.setString(2, docenteActual.getDocument());

            if (hayFiltroFecha) {
                stmt.setDate(3, Date.valueOf(dpFechaFiltro.getValue()));
            } else {
                stmt.setDate(3, Date.valueOf(LocalDate.now()));
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Guardia guardia = new Guardia();
                guardia.setId(rs.getInt("id"));
                guardia.setDocenteId(rs.getString("docente_id"));

                String nombreCompleto = rs.getString("nom") + " " + rs.getString("cognom1") +
                                      (rs.getString("cognom2") != null ? " " + rs.getString("cognom2") : "");
                guardia.setDocenteNombreCompleto(nombreCompleto.trim());

                guardia.setFecha(rs.getDate("fecha").toLocalDate());
                guardia.setHoraInicio(rs.getTime("hora_inici").toLocalTime());
                guardia.setHoraFin(rs.getTime("hora_fi").toLocalTime());
                guardia.setAula(rs.getString("aula"));
                guardia.setDisponible(rs.getBoolean("disponible"));

                guardiasDisponibles.add(guardia);
            }

            Platform.runLater(() -> {
                lblMensaje.setVisible(guardiasDisponibles.isEmpty());
                lblMensaje.setText(guardiasDisponibles.isEmpty() ?
                    "No hay guardias disponibles para la fecha seleccionada" : "");
            });

        } catch (SQLException e) {
            mostrarError("Error al cargar guardias: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void asignarGuardia(Guardia guardia) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Asignación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Asignarse esta guardia el " + 
                guardia.getFecha().format(dateFormatter) + " de " + 
                guardia.getHoraInicio().format(timeFormatter) + " a " + 
                guardia.getHoraFin().format(timeFormatter) + "?");

        Optional<ButtonType> resultado = confirm.showAndWait();

        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnector.getConnection()) {
                String sql = "UPDATE guardia SET docente_sustituto = ?, disponible = false WHERE id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, docenteActual.getDocument());
                    stmt.setInt(2, guardia.getId());
                    
                    if (stmt.executeUpdate() > 0) {
                        Platform.runLater(() -> {
                            guardiasDisponibles.remove(guardia);
                            lblMensaje.setText("Guardia asignada correctamente");
                        });
                    } else {
                        mostrarError("No se pudo asignar la guardia");
                    }
                }
            } catch (SQLException e) {
                mostrarError("Error al asignar guardia: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void filtrarGuardias() {
        cargarGuardiasDisponibles();
    }

    @FXML
    private void limpiarFiltro() {
        dpFechaFiltro.setValue(null);
        cargarGuardiasDisponibles();
    }

    @FXML
    private void refrescarTabla() {
        cargarGuardiasDisponibles();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}