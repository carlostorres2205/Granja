package ni.edu.uam.granja.Controller;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.granja.Guardado.InventarioRepository;
import ni.edu.uam.granja.Guardado.InventarioRepository.Insumo;
import ni.edu.uam.granja.Guardado.OrganizacionRepository;
import ni.edu.uam.granja.Guardado.OrganizacionRepository.Opcion;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class InventarioController {
    @FXML private TextField nombre,unidad,minimo,cantidad,cantidadAlimento;
    @FXML private TextField existenciaNueva,motivoAjuste;
    @FXML private ComboBox<String> categoria,tipoMovimiento;
    @FXML private ComboBox<Insumo> insumo,alimento;
    @FXML private ComboBox<Opcion> lote;
    @FXML private DatePicker vencimiento,fechaAlimento;
    @FXML private Label mensaje,alertas;
    @FXML private TableView<Insumo> tabla;
    @FXML private TableColumn<Insumo,String> colNombre,colCategoria,colExistencia,colMinimo,colVencimiento;
    private final InventarioRepository repo=new InventarioRepository();
    private final OrganizacionRepository organizacion=new OrganizacionRepository();
    @FXML private void initialize() {
        categoria.getItems().setAll("Alimento","Medicamento","Suplemento","Otro"); categoria.getSelectionModel().selectFirst();
        tipoMovimiento.getItems().setAll("Entrada","Salida"); tipoMovimiento.getSelectionModel().selectFirst();
        fechaAlimento.setValue(LocalDate.now());
        colNombre.setCellValueFactory(v->new SimpleStringProperty(v.getValue().nombre()));
        colCategoria.setCellValueFactory(v->new SimpleStringProperty(v.getValue().categoria()));
        colExistencia.setCellValueFactory(v->new SimpleStringProperty(v.getValue().existencia().toPlainString()+" "+v.getValue().unidad()));
        colMinimo.setCellValueFactory(v->new SimpleStringProperty(v.getValue().minimo().toPlainString()));
        colVencimiento.setCellValueFactory(v->new SimpleStringProperty(v.getValue().vencimiento()==null?"":v.getValue().vencimiento().toString()));
        actualizar();
    }
    public void actualizar() {
        try {
            List<Insumo> lista=repo.listar();
            tabla.setItems(FXCollections.observableArrayList(lista));
            insumo.setItems(FXCollections.observableArrayList(lista));
            alimento.setItems(FXCollections.observableArrayList(lista.stream().filter(i->i.categoria().equals("ALIMENTO")||i.categoria().equals("SUPLEMENTO")).toList()));
            lote.setItems(FXCollections.observableArrayList(organizacion.lotes()));
            long bajos=lista.stream().filter(i->i.existencia().compareTo(i.minimo())<=0).count();
            long vencidos=lista.stream().filter(i->i.vencimiento()!=null && !i.vencimiento().isAfter(LocalDate.now())).count();
            alertas.setText("Existencias bajas: "+bajos+" · Vencidos: "+vencidos);
        } catch(SQLException e) { mensaje.setText("No se pudo consultar inventario. Revisa PostgreSQL."); }
    }
    @FXML private void crear() {
        try {
            String n=nombre.getText().trim(),u=unidad.getText().trim();
            if(n.isEmpty()||n.length()>120||u.isEmpty()||u.length()>20) throw new IllegalArgumentException("Escribe nombre y unidad válidos.");
            BigDecimal m=minimo.getText().isBlank()?BigDecimal.ZERO:numero(minimo.getText(),true);
            repo.crear(n,switch(categoria.getValue()) {
                case "Medicamento"->"MEDICAMENTO"; case "Suplemento"->"SUPLEMENTO"; case "Otro"->"OTRO"; default->"ALIMENTO";
            },u,m,vencimiento.getValue());
            nombre.clear();unidad.clear();minimo.clear();vencimiento.setValue(null);actualizar();mensaje.setText("Insumo creado.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("23505".equals(e.getSQLState())?"Ya existe ese insumo con la misma unidad.":"No se pudo crear el insumo."); }
    }
    @FXML private void mover() {
        if(insumo.getValue()==null) { mensaje.setText("Selecciona un insumo.");return; }
        try {
            repo.mover(insumo.getValue().id(),"Entrada".equals(tipoMovimiento.getValue())?"ENTRADA":"SALIDA",numero(cantidad.getText(),false));
            cantidad.clear();actualizar();mensaje.setText("Movimiento de inventario guardado.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo guardar el movimiento."); }
    }
    @FXML private void alimentar() {
        if(lote.getValue()==null || alimento.getValue()==null) { mensaje.setText("Selecciona un lote y un alimento.");return; }
        try {
            LocalDate f=fechaAlimento.getValue();
            if(f==null || f.isAfter(LocalDate.now())) throw new IllegalArgumentException("Selecciona una fecha válida.");
            repo.alimentar(lote.getValue().id(),alimento.getValue().id(),f,numero(cantidadAlimento.getText(),false));
            cantidadAlimento.clear();actualizar();mensaje.setText("Alimentación guardada y existencia descontada.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo guardar la alimentación."); }
    }
    @FXML private void ajustar() {
        if(insumo.getValue()==null) { mensaje.setText("Selecciona el insumo que deseas corregir.");return; }
        if(motivoAjuste.getText().isBlank()) { mensaje.setText("Escribe el motivo de la corrección.");return; }
        try {
            repo.ajustar(insumo.getValue().id(),numero(existenciaNueva.getText(),true),motivoAjuste.getText().trim());
            existenciaNueva.clear();motivoAjuste.clear();actualizar();mensaje.setText("Existencia corregida; el ajuste quedó registrado.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo corregir la existencia."); }
    }
    private static BigDecimal numero(String texto,boolean cero) {
        try {
            BigDecimal n=new BigDecimal(texto.trim().replace(',','.'));
            if(n.scale()>3 || n.compareTo(new BigDecimal("99999999999.999"))>0 || (cero?n.signum()<0:n.signum()<=0))
                throw new NumberFormatException();
            return n;
        } catch(Exception e) { throw new IllegalArgumentException("Escribe una cantidad válida, con máximo tres decimales."); }
    }
}
