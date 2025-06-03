package utilidades;

import javafx.scene.control.Alert;
import java.time.LocalTime;

public class Notificacion {
    
    public static void verificarHoraSalida() {
        LocalTime ahora = LocalTime.now();
        
         if ((ahora.getHour() == 14 && ahora.getMinute() == 45) || 
            (ahora.getHour() == 21 && ahora.getMinute() == 35)) {
            
            mostrarNotificacionSalida();
        }
    }
    
    private static void mostrarNotificacionSalida() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Recordatorio de finalización de jornada");
        alert.setHeaderText(null);
        alert.setContentText("¡Recuerda finalizar tu jornada! Quedan 5 minutos para terminar.");
        alert.show();
    }
}