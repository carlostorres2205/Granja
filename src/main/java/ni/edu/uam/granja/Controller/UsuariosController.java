package ni.edu.uam.granja.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.granja.Service.AutenticacionService;
import ni.edu.uam.granja.model.Usuario;
import ni.edu.uam.granja.Guardado.AuditoriaRepository;
import ni.edu.uam.granja.Guardado.AuditoriaRepository.Cambio;
import java.time.format.DateTimeFormatter;
import java.sql.SQLException;
import java.util.Arrays;

public class UsuariosController {
    @FXML private TextField nombre,usuario;
    @FXML private PasswordField clave,confirmar;
    @FXML private ComboBox<String> rol;
    @FXML private CheckBox activo;
    @FXML private Label mensaje;
    @FXML private TableView<Usuario> tabla;
    @FXML private TableColumn<Usuario,String> colNombre,colUsuario,colRol,colEstado;
    @FXML private TableView<Cambio> auditoria;
    @FXML private TableColumn<Cambio,String> colFechaCambio,colActor,colTabla,colRegistro,colOperacion;
    @FXML private Label mensajeAuditoria;
    private final AutenticacionService servicio=new AutenticacionService();
    private final AuditoriaRepository historial=new AuditoriaRepository();
    @FXML private void initialize() {
        rol.getItems().setAll("ADMINISTRADOR","ENCARGADO","TRABAJADOR");
        rol.getSelectionModel().selectFirst();activo.setSelected(true);
        colNombre.setCellValueFactory(v->new SimpleStringProperty(v.getValue().nombre()));
        colUsuario.setCellValueFactory(v->new SimpleStringProperty(v.getValue().nombreUsuario()));
        colRol.setCellValueFactory(v->new SimpleStringProperty(v.getValue().rol()));
        colEstado.setCellValueFactory(v->new SimpleStringProperty(v.getValue().activo()?"Activo":"Inactivo"));
        colFechaCambio.setCellValueFactory(v->new SimpleStringProperty(
            v.getValue().fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colActor.setCellValueFactory(v->new SimpleStringProperty(
            v.getValue().usuario()==null?"Sin sesión":v.getValue().usuario()));
        colTabla.setCellValueFactory(v->new SimpleStringProperty(v.getValue().tabla()));
        colRegistro.setCellValueFactory(v->new SimpleStringProperty(v.getValue().registro()));
        colOperacion.setCellValueFactory(v->new SimpleStringProperty(v.getValue().operacion()));
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{
            if(b==null)return;
            nombre.setText(b.nombre());usuario.setText(b.nombreUsuario());rol.setValue(b.rol());
            activo.setSelected(b.activo());clave.clear();confirmar.clear();
        });
        actualizar();
    }
    public void actualizar() {
        try { tabla.setItems(FXCollections.observableArrayList(servicio.listarUsuarios())); }
        catch(SQLException | IllegalStateException e) { mensaje.setText("No se pudo consultar usuarios o no tienes permiso."); }
        actualizarAuditoria();
    }
    @FXML private void actualizarAuditoria() {
        try {
            auditoria.setItems(FXCollections.observableArrayList(historial.recientes()));
            mensajeAuditoria.setText("");
        } catch (SQLException e) {
            mensajeAuditoria.setText("Para activar el historial, ejecuta database/03_auditoria_roles.sql en la base Granja.");
        } catch (IllegalStateException e) {
            mensajeAuditoria.setText(e.getMessage());
        }
    }
    @FXML private void crear() { guardar(true); }
    @FXML private void modificar() { guardar(false); }
    private void guardar(boolean nuevo) {
        char[] contrasena=clave.getText().toCharArray(), confirmacion=confirmar.getText().toCharArray();
        try {
            if(nuevo) servicio.crearUsuario(nombre.getText(),usuario.getText(),rol.getValue(),contrasena,confirmacion);
            else {
                Usuario elegido=tabla.getSelectionModel().getSelectedItem();
                if(elegido==null) throw new IllegalArgumentException("Selecciona un usuario de la tabla.");
                servicio.actualizarUsuario(elegido.id(),nombre.getText(),usuario.getText(),rol.getValue(),
                    activo.isSelected(),contrasena,confirmacion);
            }
            limpiar();actualizar();mensaje.setText(nuevo?"Usuario creado.":"Usuario actualizado.");
        } catch(IllegalArgumentException | IllegalStateException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("23505".equals(e.getSQLState())?"Ese nombre de usuario ya existe.":"No se pudo guardar el usuario."); }
        finally { Arrays.fill(contrasena,'\0');Arrays.fill(confirmacion,'\0');clave.clear();confirmar.clear(); }
    }
    @FXML private void limpiar() {
        tabla.getSelectionModel().clearSelection();nombre.clear();usuario.clear();clave.clear();confirmar.clear();
        rol.getSelectionModel().selectFirst();activo.setSelected(true);mensaje.setText("");
    }
}
