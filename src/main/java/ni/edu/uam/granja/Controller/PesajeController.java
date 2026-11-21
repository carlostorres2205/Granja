package ni.edu.uam.granja.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ni.edu.uam.granja.Guardado.AnimalRepository;
import ni.edu.uam.granja.Service.PesajeService;
import ni.edu.uam.granja.model.Animal;
import ni.edu.uam.granja.model.Pesaje;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PesajeController {
    @FXML private ComboBox<Animal> animal;
    @FXML private DatePicker fecha;
    @FXML private TextField peso;
    @FXML private TextArea observaciones;
    @FXML private Label mensaje;
    @FXML private Label resumen;
    @FXML private TableView<Pesaje> tabla;
    @FXML private TableColumn<Pesaje, String> colFecha;
    @FXML private TableColumn<Pesaje, String> colPeso;
    @FXML private TableColumn<Pesaje, String> colObservaciones;
    private final AnimalRepository animales = new AnimalRepository();
    private final PesajeService service = new PesajeService();

    @FXML private void initialize() {
        fecha.setValue(LocalDate.now());
        animal.setConverter(new StringConverter<>() {
            @Override public String toString(Animal a) {
                return a == null ? "" : a.arete() + " · " + (a.nombre() == null ? "Sin tipo" : a.nombre()) + " · " + a.estado();
            }
            @Override public Animal fromString(String texto) { return null; }
        });
        colFecha.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().fecha().toString()));
        colPeso.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().pesoKg().toPlainString()));
        colObservaciones.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().observaciones() == null ? "" : v.getValue().observaciones()));
        animal.valueProperty().addListener((obs, anterior, seleccionado) -> cargarHistorial());
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{
            if(b!=null) { fecha.setValue(b.fecha());peso.setText(b.pesoKg().toPlainString());
                observaciones.setText(b.observaciones()==null?"":b.observaciones()); }
        });
        actualizarAnimales();
    }

    @FXML public void actualizarAnimales() {
        Animal anterior = animal.getValue();
        try {
            List<Animal> lista = animales.listar();
            animal.setItems(FXCollections.observableArrayList(lista));
            if (anterior != null) lista.stream().filter(a -> a.id() == anterior.id()).findFirst()
                .ifPresent(a -> animal.getSelectionModel().select(a));
            mensaje.setText(lista.isEmpty() ? "Primero registra un animal en Animales y lotes." : "");
        } catch (SQLException e) {
            mensaje.setText("No se pudieron cargar los animales. Revisa la conexión a PostgreSQL.");
        }
    }

    @FXML private void guardar() {
        try {
            BigDecimal valor = peso.getText().isBlank() ? null : new BigDecimal(peso.getText().trim().replace(',', '.'));
            service.registrar(animal.getValue(), fecha.getValue(), valor, observaciones.getText());
            peso.clear(); observaciones.clear();
            if (cargarHistorial()) mensaje.setText("Pesaje guardado correctamente.");
            else mensaje.setText("Pesaje guardado, pero no se pudo actualizar el historial. Pulsa Actualizar historial.");
        } catch (NumberFormatException e) {
            mensaje.setText("Escribe un peso válido en kilogramos.");
        } catch (IllegalArgumentException e) {
            mensaje.setText(e.getMessage());
        } catch (SQLException e) {
            mensaje.setText("23505".equals(e.getSQLState())
                ? "Ya existe un pesaje para este animal en esa fecha."
                : "No se pudo guardar el pesaje. Revisa la conexión a PostgreSQL.");
        }
    }

    @FXML private void actualizarHistorial() {
        if (cargarHistorial()) mensaje.setText("");
    }
    @FXML private void corregir() {
        Pesaje elegido=tabla.getSelectionModel().getSelectedItem();
        try {
            BigDecimal valor=peso.getText().isBlank()?null:new BigDecimal(peso.getText().trim().replace(',','.'));
            service.corregir(elegido,animal.getValue(),fecha.getValue(),valor,observaciones.getText());
            tabla.getSelectionModel().clearSelection();peso.clear();observaciones.clear();
            cargarHistorial();mensaje.setText("Pesaje corregido.");
        } catch(NumberFormatException e) { mensaje.setText("Escribe un peso válido."); }
        catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("23505".equals(e.getSQLState())
            ?"Ya existe un pesaje en esa fecha.":"No se pudo corregir el pesaje."); }
    }

    private boolean cargarHistorial() {
        Animal seleccionado = animal.getValue();
        tabla.getItems().clear();
        resumen.setText("Selecciona un animal para consultar sus pesajes.");
        if (seleccionado == null) return true;
        try {
            List<Pesaje> registros = service.listar(seleccionado.id());
            tabla.setItems(FXCollections.observableArrayList(registros));
            if (registros.size() < 2) {
                resumen.setText("Se necesitan al menos dos pesajes para calcular la ganancia diaria.");
            } else {
                Pesaje reciente = registros.get(0), anterior = registros.get(1);
                BigDecimal diferencia = reciente.pesoKg().subtract(anterior.pesoKg());
                resumen.setText("Últimos dos pesajes (" + anterior.fecha() + " a " + reciente.fecha()
                    + "): variación " + diferencia.toPlainString() + " kg · promedio "
                    + PesajeService.gananciaDiaria(anterior, reciente).toPlainString() + " kg/día");
            }
            return true;
        } catch (SQLException e) {
            resumen.setText("No se pudo cargar el historial. Revisa la conexión a PostgreSQL.");
            return false;
        }
    }
}
