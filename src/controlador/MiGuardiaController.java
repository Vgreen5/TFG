package controlador;

import databases.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import modelo.Guardia;
import modelo.Usuario;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MiGuardiaController {

    @FXML private TableView<Guardia> tablaGuardias;
    @FXML private TableColumn<Guardia, Integer> colId;
    @FXML private TableColumn<Guardia, String> colDocente;
    @FXML private TableColumn<Guardia, LocalDate> colFecha;
    @FXML private TableColumn<Guardia, LocalTime> colHoraInicio;
    @FXML private TableColumn<Guardia, LocalTime> colHoraFin;
    @FXML private TableColumn<Guardia, String> colAula;
    @FXML private TableColumn<Guardia, String> colEstado;

    private Usuario docenteActual;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public void initialize() {
        configurarColumnas();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDocente.setCellValueFactory(new PropertyValueFactory<>("docenteNombreCompleto"));

        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateFormatter.format(item));
            }
        });

        colHoraInicio.setCellValueFactory(new PropertyValueFactory<>("horaInicio"));
        colHoraInicio.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : timeFormatter.format(item));
            }
        });

        colHoraFin.setCellValueFactory(new PropertyValueFactory<>("horaFin"));
        colHoraFin.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : timeFormatter.format(item));
            }
        });

        colAula.setCellValueFactory(new PropertyValueFactory<>("aula"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    public void setDocenteActual(Usuario docente) {
        this.docenteActual = docente;
        if (docenteActual != null) {
            cargarGuardias();
        }
    }

    private void cargarGuardias() {
        ObservableList<Guardia> guardias = FXCollections.observableArrayList();
        String sql = "SELECT g.id, g.docente_id, g.fecha, g.hora_inici, g.hora_fi, g.aula, " +
                "d.nom, d.cognom1, d.cognom2, g.disponible, " +
                "CASE WHEN g.disponible THEN 'Pendiente' ELSE 'Asignada' END as estado " +
                "FROM guardia g " +
                "JOIN docent d ON g.docente_id = d.document " +
                "WHERE g.docente_sustituto = ? AND g.fecha >= CURRENT_DATE() " +  
                "ORDER BY g.fecha, g.hora_inici";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, docenteActual.getDocument());
            ResultSet rs = ps.executeQuery();

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
                guardia.setEstado(rs.getString("estado")); // solo si agregaste el campo en la clase Guardia

                guardias.add(guardia);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar las guardias", e.getMessage());
        }

        tablaGuardias.setItems(guardias);
    }

    private void mostrarAlerta(String titulo, String cabecera, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecera);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
