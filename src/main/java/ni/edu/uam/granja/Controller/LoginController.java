package ni.edu.uam.granja.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.granja.GranjaApplication;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import ni.edu.uam.granja.Service.AutenticacionService;

public class LoginController {
    @FXML private TextField usuario;
    @FXML private PasswordField contrasena;
    @FXML private Label mensaje;
    @FXML private Label tituloLogin;
    @FXML private Label ayudaLogin;
    @FXML private TextField nombre;
    @FXML private PasswordField confirmar;
    @FXML private javafx.scene.layout.VBox configuracionInicial;
    @FXML private javafx.scene.layout.VBox confirmacionInicial;
    @FXML private javafx.scene.control.Button botonEntrar;
    private final AutenticacionService autenticacion=new AutenticacionService();
    private boolean primeraCuenta;
    @FXML private void initialize() {
        try {
            primeraCuenta=autenticacion.requierePrimerAdministrador();
            configuracionInicial.setManaged(primeraCuenta);
            configuracionInicial.setVisible(primeraCuenta);
            confirmacionInicial.setManaged(primeraCuenta);
            confirmacionInicial.setVisible(primeraCuenta);
            if(primeraCuenta) {
                tituloLogin.setText("Configura el acceso");
                ayudaLogin.setText("Crea la primera cuenta administradora de la granja.");
                botonEntrar.setText("Crear cuenta y entrar  →");
            }
        } catch (SQLException e) {
            mensaje.setText("No se pudo conectar a PostgreSQL. Revisa GRANJA_DB_PASSWORD, el servidor y la base Granja.");
        }
    }
    @FXML private void entrar() {
        char[] clave=contrasena.getText().toCharArray();
        char[] repetida=confirmar.getText().toCharArray();
        try {
            var cuenta=primeraCuenta
                ? autenticacion.crearPrimerAdministrador(nombre.getText(),usuario.getText(),clave,repetida)
                : autenticacion.entrar(usuario.getText(),clave);
            GranjaApplication.iniciarSesion(cuenta);
            GranjaApplication.mostrar((Stage) usuario.getScene().getWindow(),"Granja-App.fxml","Granja | Panel principal",1200,760);
        } catch(IllegalArgumentException | IllegalStateException e) {
            mensaje.setText(e.getMessage());
        } catch(SQLException e) {
            mensaje.setText("No se pudo consultar PostgreSQL. Revisa la conexión y los datos.");
        } catch(IOException e) {
            GranjaApplication.cerrarSesion();
            mensaje.setText("No se pudo abrir el panel principal.");
        } finally {
            Arrays.fill(clave,'\0'); Arrays.fill(repetida,'\0');
            contrasena.clear(); confirmar.clear();
        }
    }
}
