package ni.edu.uam.granja;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import ni.edu.uam.granja.model.Usuario;

public class GranjaApplication extends Application {
    private static Usuario sesion;
    public static Usuario sesion() { return sesion; }
    public static void iniciarSesion(Usuario usuario) { sesion=usuario; }
    public static void cerrarSesion() { sesion=null; }
    @Override public void start(Stage stage) throws IOException {
        mostrar(stage, "Login-view.fxml", "Granja | Bienvenido", 1000, 680);
        stage.setMinWidth(900); stage.setMinHeight(620); stage.show();
    }
    public static void mostrar(Stage stage, String archivo, String titulo, double ancho, double alto) throws IOException {
        if ("Granja-App.fxml".equals(archivo) && sesion==null)
            throw new IllegalStateException("Debes iniciar sesión.");
        Scene scene = new Scene(new FXMLLoader(GranjaApplication.class.getResource(archivo)).load(), ancho, alto);
        scene.getStylesheets().add(GranjaApplication.class.getResource("granja.css").toExternalForm());
        stage.setTitle(titulo); stage.setScene(scene);
    }
    public static void main(String[] args) { launch(args); }
}
