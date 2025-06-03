package controlador;

import databases.DatabaseConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert.AlertType;
import modelo.Login;

import java.sql.*;

public class GestionController {

    @FXML private TextField txtBuscarId;
    @FXML private TableView<Login> tablaUsuarios;
    @FXML private TableColumn<Login, String> colDocenteId;
    @FXML private TableColumn<Login, String> colPassword;
    @FXML private TableColumn<Login, Boolean> colAdmin;

  
    @FXML private Button btnBuscar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnCambiarPassword;
    @FXML private Button btnToggleAdmin;

    private ObservableList<Login> usuarios;

    @FXML
    public void initialize() {
        colDocenteId.setCellValueFactory(new PropertyValueFactory<>("docenteId"));
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colAdmin.setCellValueFactory(new PropertyValueFactory<>("esAdmin"));

        tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS); // JavaFX 21
        tablaUsuarios.setStyle("-fx-font-size: 13px;");

        cargarUsuarios(null);
    }

    private void cargarUsuarios(String filtroDocenteId) {
        usuarios = FXCollections.observableArrayList();

        String sql = "SELECT docente_id, password, es_admin FROM login";
        if (filtroDocenteId != null && !filtroDocenteId.isEmpty()) {
            sql += " WHERE docente_id LIKE ?";
        }

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filtroDocenteId != null && !filtroDocenteId.isEmpty()) {
                ps.setString(1, "%" + filtroDocenteId + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                usuarios.add(new Login(
                    rs.getString("docente_id"),
                    rs.getString("password"),
                    rs.getBoolean("es_admin")
                ));
            }

            tablaUsuarios.setItems(usuarios);

        } catch (SQLException e) {
            mostrarError("Error al cargar usuarios: " + e.getMessage());
        }
    }

    @FXML
    private void buscarUsuario() {
        cargarUsuarios(txtBuscarId.getText().trim());
    }

    @FXML
    private void limpiarBusqueda() {
        txtBuscarId.clear();
        cargarUsuarios(null);
    }

    @FXML
    private void cambiarPassword() {
        Login seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Debe seleccionar un usuario.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cambiar Contraseña");
        dialog.setHeaderText("Nueva contraseña para: " + seleccionado.getDocenteId());
        dialog.setContentText("Ingrese la nueva contraseña:");

        dialog.showAndWait().ifPresent(nuevaPass -> {
            if (nuevaPass.trim().isEmpty()) {
                mostrarAlerta("La contraseña no puede estar vacía.");
            } else {
                if (actualizarPassword(seleccionado.getDocenteId(), nuevaPass.trim())) {
                    mostrarAlerta("Contraseña actualizada correctamente.");
                    cargarUsuarios(txtBuscarId.getText().trim()); // Recargar datos
                }
            }
        });
    }

    private boolean actualizarPassword(String docenteId, String nuevaPass) {
        String sql = "UPDATE login SET password = ? WHERE docente_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevaPass);
            ps.setString(2, docenteId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            mostrarError("Error al actualizar contraseña: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void toggleAdmin() {
        Login seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Debe seleccionar un usuario.");
            return;
        }

        boolean nuevoAdmin = !seleccionado.isEsAdmin();
        String sql = "UPDATE login SET es_admin = ? WHERE docente_id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, nuevoAdmin);
            ps.setString(2, seleccionado.getDocenteId());

            if (ps.executeUpdate() > 0) {
                seleccionado.setEsAdmin(nuevoAdmin);
                tablaUsuarios.refresh();
                mostrarAlerta("Rol de administrador actualizado.");
            }

        } catch (SQLException e) {
            mostrarError("Error al cambiar rol: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
