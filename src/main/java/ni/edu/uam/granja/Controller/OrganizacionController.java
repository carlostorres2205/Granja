package ni.edu.uam.granja.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.granja.Guardado.OrganizacionRepository;
import ni.edu.uam.granja.Guardado.OrganizacionRepository.FilaAnimal;
import ni.edu.uam.granja.Guardado.OrganizacionRepository.Opcion;
import java.sql.SQLException;
import java.util.List;

public class OrganizacionController {
    @FXML private TextField nombreLote,nombreUbicacion,capacidad;
    @FXML private ComboBox<String> proposito,tipoUbicacion;
    @FXML private ComboBox<FilaAnimal> animal;
    @FXML private ComboBox<Opcion> lote,ubicacion;
    @FXML private Label mensaje;
    @FXML private TableView<FilaAnimal> tabla;
    @FXML private TableColumn<FilaAnimal,String> colArete,colTipo,colLote,colUbicacion;
    private final OrganizacionRepository repo=new OrganizacionRepository();

    @FXML private void initialize() {
        proposito.getItems().setAll("Engorde","Reproducción","Crianza","Otro");
        tipoUbicacion.getItems().setAll("Potrero","Corral","Enfermería","Otro");
        proposito.getSelectionModel().selectFirst(); tipoUbicacion.getSelectionModel().selectFirst();
        animal.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(FilaAnimal a) { return a==null?"":a.arete()+" · "+valor(a.tipo()); }
            @Override public FilaAnimal fromString(String s) { return null; }
        });
        animal.valueProperty().addListener((o,a,b)->seleccionarActual(b));
        colArete.setCellValueFactory(v->new SimpleStringProperty(v.getValue().arete()));
        colTipo.setCellValueFactory(v->new SimpleStringProperty(valor(v.getValue().tipo())));
        colLote.setCellValueFactory(v->new SimpleStringProperty(valor(v.getValue().lote())));
        colUbicacion.setCellValueFactory(v->new SimpleStringProperty(valor(v.getValue().ubicacion())));
        actualizar();
    }
    public void actualizar() {
        try {
            Long anterior=animal.getValue()==null?null:animal.getValue().id();
            List<Opcion> lotes=repo.lotes(), ubicaciones=repo.ubicaciones();
            List<FilaAnimal> animales=repo.animales();
            lote.setItems(FXCollections.observableArrayList(lotes));
            ubicacion.setItems(FXCollections.observableArrayList(ubicaciones));
            animal.setItems(FXCollections.observableArrayList(animales));
            tabla.setItems(FXCollections.observableArrayList(animales));
            if(anterior!=null) animales.stream().filter(a->a.id()==anterior).findFirst().ifPresent(animal::setValue);
            mensaje.setText("");
        } catch(SQLException e) { mensaje.setText("No se pudieron cargar los datos. Revisa PostgreSQL."); }
    }
    @FXML private void crearLote() {
        try {
            String nombre=validarNombre(nombreLote.getText());
            repo.crearLote(nombre,switch(proposito.getValue()) {
                case "Reproducción"->"REPRODUCCION"; case "Crianza"->"CRIANZA"; case "Otro"->"OTRO"; default->"ENGORDE";
            });
            nombreLote.clear(); actualizar(); mensaje.setText("Lote creado.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { error(e); }
    }
    @FXML private void crearUbicacion() {
        try {
            String nombre=validarNombre(nombreUbicacion.getText());
            Integer limite=null;
            if(!capacidad.getText().isBlank()) {
                try { limite=Integer.valueOf(capacidad.getText().trim()); }
                catch(NumberFormatException e) { throw new IllegalArgumentException("La capacidad debe ser un número entero."); }
                if(limite<=0) throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");
            }
            repo.crearUbicacion(nombre,switch(tipoUbicacion.getValue()) {
                case "Corral"->"CORRAL"; case "Enfermería"->"ENFERMERIA"; case "Otro"->"OTRO"; default->"POTRERO";
            },limite);
            nombreUbicacion.clear(); capacidad.clear(); actualizar(); mensaje.setText("Ubicación creada.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { error(e); }
    }
    @FXML private void asignar() {
        if(animal.getValue()==null) { mensaje.setText("Selecciona un animal."); return; }
        if(lote.getValue()==null && ubicacion.getValue()==null) {
            mensaje.setText("Selecciona al menos un lote o una ubicación."); return;
        }
        try {
            repo.asignar(animal.getValue().id(),id(lote.getValue()),id(ubicacion.getValue()));
            actualizar(); mensaje.setText("Asignación guardada.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo asignar el animal. Revisa los datos y PostgreSQL."); }
    }
    private void seleccionarActual(FilaAnimal a) {
        if(a==null) return;
        lote.getItems().stream().filter(x->x.nombre().equals(a.lote())).findFirst().ifPresentOrElse(lote::setValue,()->lote.setValue(null));
        ubicacion.getItems().stream().filter(x->x.nombre().equals(a.ubicacion())).findFirst().ifPresentOrElse(ubicacion::setValue,()->ubicacion.setValue(null));
    }
    private static Long id(Opcion o) { return o==null?null:o.id(); }
    private static String valor(String s) { return s==null?"":s; }
    private static String validarNombre(String s) {
        if(s==null || s.isBlank() || s.trim().length()>100)
            throw new IllegalArgumentException("Escribe un nombre de hasta 100 caracteres.");
        return s.trim();
    }
    private void error(SQLException e) {
        mensaje.setText("23505".equals(e.getSQLState())?"Ese nombre ya existe.":"No se pudo guardar. Revisa PostgreSQL.");
    }
}
