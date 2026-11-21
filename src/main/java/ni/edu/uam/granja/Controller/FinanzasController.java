package ni.edu.uam.granja.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ni.edu.uam.granja.GranjaApplication;
import ni.edu.uam.granja.Guardado.AnimalRepository;
import ni.edu.uam.granja.Guardado.FinanzasRepository.Comprador;
import ni.edu.uam.granja.Guardado.FinanzasRepository.LineaVenta;
import ni.edu.uam.granja.Guardado.FinanzasRepository.VentaResumen;
import ni.edu.uam.granja.Service.FinanzasService;
import ni.edu.uam.granja.model.Animal;
import ni.edu.uam.granja.model.MovimientoFinanciero;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class FinanzasController {
    @FXML private DatePicker fechaVenta,desde,hasta,fechaMovimiento;
    @FXML private ComboBox<Animal> animal;
    @FXML private ComboBox<Comprador> comprador;
    @FXML private ComboBox<String> tipoMovimiento,categoria;
    @FXML private TextField pesoVenta,precio,concepto,importe,nombreComprador;
    @FXML private Label mensaje,totalVenta,resumen;
    @FXML private TableView<LineaVenta> tablaVenta;
    @FXML private TableColumn<LineaVenta,String> colAreteVenta,colPesoVenta,colPrecioVenta;
    @FXML private TableView<MovimientoFinanciero> tablaMovimientos;
    @FXML private TableView<VentaResumen> tablaVentas;
    @FXML private TableColumn<VentaResumen,String> colVentaId,colVentaFecha,colComprador,colAnimales,colVentaTotal;
    @FXML private TableColumn<MovimientoFinanciero,String> colFecha,colTipo,colCategoria,colConcepto,colImporte;
    private final FinanzasService service=new FinanzasService();
    private final AnimalRepository animales=new AnimalRepository();
    private final ObservableList<LineaVenta> lineas=FXCollections.observableArrayList();

    @FXML private void initialize() {
        fechaVenta.setValue(LocalDate.now());fechaMovimiento.setValue(LocalDate.now());
        desde.setValue(LocalDate.now().withDayOfMonth(1));hasta.setValue(LocalDate.now());
        tipoMovimiento.getItems().setAll("Gasto","Ingreso");tipoMovimiento.getSelectionModel().selectFirst();
        categoria.getItems().setAll("Compra de ganado","Alimentación","Salud","Mantenimiento","Servicios","Otro");
        categoria.getSelectionModel().selectFirst();
        animal.setConverter(new StringConverter<>() {
            @Override public String toString(Animal a) { return a==null?"":a.arete()+" · "+(a.nombre()==null?"Sin tipo":a.nombre()); }
            @Override public Animal fromString(String s) { return null; }
        });
        colAreteVenta.setCellValueFactory(v->new SimpleStringProperty(v.getValue().arete()));
        colPesoVenta.setCellValueFactory(v->new SimpleStringProperty(v.getValue().pesoKg()==null?"":v.getValue().pesoKg().toPlainString()));
        colPrecioVenta.setCellValueFactory(v->new SimpleStringProperty(v.getValue().precio().toPlainString()));
        tablaVenta.setItems(lineas);
        tablaMovimientos.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{
            if(b==null || b.ventaId()!=null)return;
            fechaMovimiento.setValue(b.fecha());tipoMovimiento.setValue("GASTO".equals(b.tipo())?"Gasto":"Ingreso");
            categoria.setValue(switch(b.categoria()) {
                case "COMPRA_GANADO"->"Compra de ganado";case "ALIMENTACION"->"Alimentación";
                case "SALUD"->"Salud";case "MANTENIMIENTO"->"Mantenimiento";
                case "SERVICIOS"->"Servicios";default->"Otro";
            });
            concepto.setText(b.concepto());importe.setText(b.importe().toPlainString());
        });
        colFecha.setCellValueFactory(v->new SimpleStringProperty(v.getValue().fecha().toString()));
        colTipo.setCellValueFactory(v->new SimpleStringProperty(v.getValue().tipo()));
        colCategoria.setCellValueFactory(v->new SimpleStringProperty(v.getValue().categoria()));
        colConcepto.setCellValueFactory(v->new SimpleStringProperty(v.getValue().concepto()));
        colImporte.setCellValueFactory(v->new SimpleStringProperty(v.getValue().importe().toPlainString()));
        colVentaId.setCellValueFactory(v->new SimpleStringProperty(Long.toString(v.getValue().id())));
        colVentaFecha.setCellValueFactory(v->new SimpleStringProperty(v.getValue().fecha().toString()));
        colComprador.setCellValueFactory(v->new SimpleStringProperty(v.getValue().comprador()));
        colAnimales.setCellValueFactory(v->new SimpleStringProperty(Integer.toString(v.getValue().animales())));
        colVentaTotal.setCellValueFactory(v->new SimpleStringProperty(v.getValue().total().toPlainString()));
        actualizar();
    }
    public void actualizar() {
        try {
            List<Animal> disponibles=animales.listar().stream().filter(a->"ACTIVO".equals(a.estado())
                && lineas.stream().noneMatch(l->l.animalId()==a.id())).toList();
            animal.setItems(FXCollections.observableArrayList(disponibles));
            comprador.setItems(FXCollections.observableArrayList(service.repositorio().compradores()));
            actualizarMovimientos();
        } catch(SQLException e) { mensaje.setText("No se pudieron cargar los datos. Revisa PostgreSQL."); }
    }
    @FXML private void crearComprador() {
        String nombre=nombreComprador.getText().trim();
        if(nombre.isEmpty()||nombre.length()>150) { mensaje.setText("Escribe un nombre de comprador de hasta 150 caracteres.");return; }
        try {
            Comprador nuevo=service.repositorio().crearComprador(nombre);
            nombreComprador.clear();actualizar();comprador.setValue(nuevo);mensaje.setText("Comprador creado.");
        } catch(SQLException e) { mensaje.setText("No se pudo crear el comprador."); }
    }
    @FXML private void agregarAnimal() {
        if(animal.getValue()==null) { mensaje.setText("Selecciona un animal activo.");return; }
        try {
            BigDecimal kg=pesoVenta.getText().isBlank()?null:numero(pesoVenta.getText());
            BigDecimal valor=numero(precio.getText());
            lineas.add(new LineaVenta(animal.getValue().id(),animal.getValue().arete(),kg,valor));
            animal.setValue(null);pesoVenta.clear();precio.clear();actualizarTotal();actualizarAnimalesDisponibles();
            mensaje.setText("Animal agregado a la venta.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
    }
    @FXML private void quitarAnimal() {
        lineas.remove(tablaVenta.getSelectionModel().getSelectedItem());
        actualizarTotal();actualizarAnimalesDisponibles();
    }
    @FXML private void guardarVenta() {
        try {
            long id=service.vender(fechaVenta.getValue(),comprador.getValue()==null?null:comprador.getValue().id(),
                List.copyOf(lineas),GranjaApplication.sesion().id());
            lineas.clear();actualizarTotal();actualizar();mensaje.setText("Venta #"+id+" guardada con todos sus animales.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo guardar la venta. No se aplicó ningún cambio."); }
    }
    @FXML private void guardarMovimiento() {
        try {
            service.guardarMovimiento(fechaMovimiento.getValue(),
                "Gasto".equals(tipoMovimiento.getValue())?"GASTO":"INGRESO",
                switch(categoria.getValue()) {
                    case "Compra de ganado"->"COMPRA_GANADO";case "Alimentación"->"ALIMENTACION";
                    case "Salud"->"SALUD";case "Mantenimiento"->"MANTENIMIENTO";
                    case "Servicios"->"SERVICIOS";default->"OTRO";
                },concepto.getText(),numero(importe.getText()),GranjaApplication.sesion().id());
            concepto.clear();importe.clear();actualizarMovimientos();mensaje.setText("Movimiento guardado.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo guardar el movimiento."); }
    }
    @FXML private void corregirMovimiento() {
        MovimientoFinanciero elegido=tablaMovimientos.getSelectionModel().getSelectedItem();
        if(elegido==null) { mensaje.setText("Selecciona un movimiento manual en la tabla.");return; }
        try {
            service.corregirMovimiento(elegido.id(),fechaMovimiento.getValue(),
                "Gasto".equals(tipoMovimiento.getValue())?"GASTO":"INGRESO",
                switch(categoria.getValue()) {
                    case "Compra de ganado"->"COMPRA_GANADO";case "Alimentación"->"ALIMENTACION";
                    case "Salud"->"SALUD";case "Mantenimiento"->"MANTENIMIENTO";
                    case "Servicios"->"SERVICIOS";default->"OTRO";
                },concepto.getText(),numero(importe.getText()));
            tablaMovimientos.getSelectionModel().clearSelection();concepto.clear();importe.clear();
            actualizarMovimientos();mensaje.setText("Movimiento corregido.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo corregir el movimiento."); }
    }
    @FXML private void actualizarMovimientos() {
        try {
            List<MovimientoFinanciero> lista=service.movimientos(desde.getValue(),hasta.getValue());
            tablaMovimientos.setItems(FXCollections.observableArrayList(lista));
            tablaVentas.setItems(FXCollections.observableArrayList(service.repositorio().ventas(desde.getValue(),hasta.getValue())));
            BigDecimal ingresos=lista.stream().filter(m->m.tipo().equals("INGRESO")).map(MovimientoFinanciero::importe)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal gastos=lista.stream().filter(m->m.tipo().equals("GASTO")).map(MovimientoFinanciero::importe)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
            resumen.setText("Ingresos: "+ingresos+" · Gastos: "+gastos+" · Diferencia: "+ingresos.subtract(gastos));
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudieron consultar los movimientos."); }
    }
    private void actualizarTotal() {
        BigDecimal total=lineas.stream().map(LineaVenta::precio).reduce(BigDecimal.ZERO,BigDecimal::add);
        totalVenta.setText("Total de la venta: "+total);
    }
    private void actualizarAnimalesDisponibles() {
        try {
            animal.setItems(FXCollections.observableArrayList(animales.listar().stream()
                .filter(a->"ACTIVO".equals(a.estado())&&lineas.stream().noneMatch(l->l.animalId()==a.id())).toList()));
        } catch(SQLException e) { mensaje.setText("No se pudo actualizar la lista de animales."); }
    }
    private static BigDecimal numero(String texto) {
        try { return new BigDecimal(texto.trim().replace(',','.')); }
        catch(Exception e) { throw new IllegalArgumentException("Escribe un importe o peso válido."); }
    }
}
