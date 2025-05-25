module TFG1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires javafx.base;

    opens application to javafx.graphics, javafx.fxml;
    opens controlador to javafx.fxml, javafx.base;
    opens modelo to javafx.base;
}
