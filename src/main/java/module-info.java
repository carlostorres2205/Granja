module ni.edu.uam.granja {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens ni.edu.uam.granja to javafx.fxml;
    opens ni.edu.uam.granja.Controller to javafx.fxml;
    exports ni.edu.uam.granja;
}
