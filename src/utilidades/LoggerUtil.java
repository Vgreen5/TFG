package utilidades;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {
    private static final String LOG_FILE = "session_logs.txt";  
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logLogin(String docenteId) {
        logAction(docenteId, "LOGIN");
    }

    public static void logLogout(String docenteId) {
        logAction(docenteId, "LOGOUT");
    }

    private static void logAction(String docenteId, String action) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("[%s] Usuario: %s - Fecha/Hora: %s%n", 
                                      action, docenteId, timestamp);
        
        writeToLog(logEntry);
    }

    private static void writeToLog(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(message);
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo de log: " + e.getMessage());
        }
    }
}