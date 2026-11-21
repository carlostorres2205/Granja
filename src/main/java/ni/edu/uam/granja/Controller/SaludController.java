package ni.edu.uam.granja.Controller;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import ni.edu.uam.granja.Guardado.AnimalRepository;
import ni.edu.uam.granja.Guardado.InventarioRepository;
import ni.edu.uam.granja.Guardado.InventarioRepository.Insumo;
import ni.edu.uam.granja.Service.SaludService;
import ni.edu.uam.granja.model.Animal;
import ni.edu.uam.granja.model.EventosSanitarios;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public class SaludController {
    @FXML private ComboBox<Animal> animal;
    @FXML private ComboBox<String> tipo;
    @FXML private DatePicker fecha, proxima, retiro;
    @FXML private TextField descripcion, producto, dosis;
    @FXML private TextField cantidadMedicamento;
    @FXML private ComboBox<Insumo> medicamento;
    @FXML private TextArea notas;
    @FXML private Label mensaje, aviso;
    @FXML private TableView<EventosSanitarios> tabla;
    @FXML private TableColumn<EventosSanitarios,String> colFecha,colTipo,colDescripcion,colDosis,colProxima,colRetiro;
    private final AnimalRepository animales=new AnimalRepository();
    private final SaludService service=new SaludService();
    private final InventarioRepository inventario=new InventarioRepository();

    @FXML private void initialize() {
        tipo.getItems().setAll("Vacuna","Desparasitación","Enfermedad","Tratamiento","Revisión","Otro");
        tipo.getSelectionModel().selectFirst(); fecha.setValue(LocalDate.now());
        animal.setConverter(new StringConverter<>() {
            @Override public String toString(Animal a) { return a==null?"":a.arete()+" · "+(a.nombre()==null?"Sin tipo":a.nombre()); }
            @Override public Animal fromString(String s) { return null; }
        });
        animal.valueProperty().addListener((o,a,b)->actualizarHistorial());
        colFecha.setCellValueFactory(v->new SimpleStringProperty(v.getValue().fecha().toString()));
        colTipo.setCellValueFactory(v->new SimpleStringProperty(v.getValue().tipo()));
        colDescripcion.setCellValueFactory(v->new SimpleStringProperty(v.getValue().descripcion()));
        colDosis.setCellValueFactory(v->new SimpleStringProperty(valor(v.getValue().dosis())));
        colProxima.setCellValueFactory(v->new SimpleStringProperty(fechaTexto(v.getValue().fechaProxima())));
        colRetiro.setCellValueFactory(v->new SimpleStringProperty(fechaTexto(v.getValue().finRetiro())));
        actualizarAnimales();
        actualizarMedicamentos();
    }
    public void actualizarAnimales() {
        Animal seleccionado=animal.getValue();
        try {
            List<Animal> lista=animales.listar().stream().filter(a->"ACTIVO".equals(a.estado())).toList();
            animal.setItems(FXCollections.observableArrayList(lista));
            if(seleccionado!=null) lista.stream().filter(a->a.id()==seleccionado.id()).findFirst().ifPresent(animal::setValue);
            mensaje.setText(lista.isEmpty()?"Primero registra un animal activo.":"");
            actualizarAvisos();
        } catch(SQLException e) { mensaje.setText("No se pudieron cargar los animales. Revisa PostgreSQL."); }
    }
    @FXML private void guardar() {
        try {
            if(descripcion.getText().isBlank()) throw new IllegalArgumentException("Escribe el motivo o la descripción.");
            String detalle=descripcion.getText().trim();
            if(!producto.getText().isBlank()) detalle=detalle+" · Producto: "+producto.getText().trim();
            String codigo=switch(tipo.getValue()) {
                case "Vacuna"->"VACUNA"; case "Desparasitación"->"DESPARASITACION";
                case "Enfermedad"->"ENFERMEDAD"; case "Tratamiento"->"TRATAMIENTO";
                case "Revisión"->"REVISION"; default->"OTRO";
            };
            service.registrar(animal.getValue(),codigo,fecha.getValue(),detalle,dosis.getText(),
                proxima.getValue(),retiro.getValue(),notas.getText(),
                medicamento.getValue()==null?null:medicamento.getValue().id(),
                cantidadMedicamento.getText().isBlank()?null:new BigDecimal(cantidadMedicamento.getText().trim().replace(',','.')));
            descripcion.clear(); producto.clear(); dosis.clear(); notas.clear();
            cantidadMedicamento.clear();medicamento.setValue(null);
            proxima.setValue(null); retiro.setValue(null);
            actualizarHistorial(); actualizarAvisos(); actualizarMedicamentos(); mensaje.setText("Evento sanitario guardado.");
        } catch(NumberFormatException e) { mensaje.setText("Escribe una cantidad válida de medicamento."); }
        catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo guardar el evento. Revisa PostgreSQL y los datos."); }
    }
    @FXML private void actualizarHistorial() {
        tabla.getItems().clear();
        if(animal.getValue()==null) return;
        try { tabla.setItems(FXCollections.observableArrayList(service.listar(animal.getValue().id()))); }
        catch(SQLException e) { mensaje.setText("No se pudo cargar el historial sanitario."); }
    }
    @FXML private void actualizarAvisos() {
        try {
            List<EventosSanitarios> eventos=service.proximos();
            aviso.setText(eventos.isEmpty()?"No hay revisiones programadas para los próximos 30 días."
                : "Próximas revisiones (30 días): "+eventos.size()+". Consulta el historial de cada animal.");
        } catch(SQLException e) { aviso.setText("No se pudieron consultar las próximas revisiones."); }
    }
    @FXML private void actualizarMedicamentos() {
        try {
            medicamento.setItems(FXCollections.observableArrayList(inventario.listar().stream()
                .filter(i->"MEDICAMENTO".equals(i.categoria())).toList()));
        } catch(SQLException e) { mensaje.setText("No se pudo cargar la lista de medicamentos."); }
    }
    private static String valor(String s) { return s==null?"":s; }
    private static String fechaTexto(LocalDate f) { return f==null?"":f.toString(); }
}
