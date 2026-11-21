package ni.edu.uam.granja.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.granja.Service.AnimalService;
import ni.edu.uam.granja.model.Animal;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class AnimalController {
    @FXML private TextField arete;
    @FXML private ComboBox<String> tipoAnimal;
    @FXML private TextField raza;
    @FXML private TextField peso;
    @FXML private ComboBox<String> sexo;
    @FXML private ComboBox<String> origen;
    @FXML private DatePicker nacimiento;
    @FXML private DatePicker ingreso;
    @FXML private Label mensaje;
    @FXML private DatePicker fechaBaja;
    @FXML private TextField motivoBaja;
    @FXML private TableView<Animal> tabla;
    @FXML private TableColumn<Animal, String> colArete;
    @FXML private TableColumn<Animal, String> colNombre;
    @FXML private TableColumn<Animal, String> colSexo;
    @FXML private TableColumn<Animal, String> colRaza;
    @FXML private TableColumn<Animal, String> colIngreso;
    @FXML private TableColumn<Animal, String> colEstado;
    private final AnimalService service = new AnimalService();

    @FXML private void initialize() {
        tipoAnimal.getItems().setAll("Vaca", "Cerdo");
        tipoAnimal.getSelectionModel().selectFirst();
        sexo.getItems().setAll("Hembra", "Macho");
        origen.getItems().setAll("Nacido en la granja", "Comprado", "Otro");
        sexo.getSelectionModel().selectFirst();
        origen.getSelectionModel().selectFirst();
        ingreso.setValue(LocalDate.now());
        fechaBaja.setValue(LocalDate.now());
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{
            if(b==null)return;
            arete.setText(b.arete());tipoAnimal.setValue(b.nombre());raza.setText(valor(b.raza()));
            peso.setText(b.pesoInicialKg()==null?"":b.pesoInicialKg().toPlainString());
            sexo.setValue("H".equals(b.sexo())?"Hembra":"Macho");
            origen.setValue(switch(b.origen()) {case "COMPRADO"->"Comprado";case "OTRO"->"Otro";default->"Nacido en la granja";});
            nacimiento.setValue(b.fechaNacimiento());ingreso.setValue(b.fechaIngreso());
        });
        colArete.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().arete()));
        colNombre.setCellValueFactory(v -> new SimpleStringProperty(valor(v.getValue().nombre())));
        colSexo.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().sexo().equals("H") ? "Hembra" : "Macho"));
        colRaza.setCellValueFactory(v -> new SimpleStringProperty(valor(v.getValue().raza())));
        colIngreso.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().fechaIngreso().toString()));
        colEstado.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().estado()));
        actualizar();
    }

    @FXML private void guardar() {
        try {
            BigDecimal pesoInicial = peso.getText().isBlank() ? null : new BigDecimal(peso.getText().trim().replace(",", "."));
            service.registrar(arete.getText(), tipoAnimal.getValue(),
                sexo.getValue().equals("Hembra") ? "H" : "M", raza.getText(),
                nacimiento.getValue(), ingreso.getValue(),
                switch (origen.getValue()) {
                    case "Comprado" -> "COMPRADO";
                    case "Otro" -> "OTRO";
                    default -> "NACIDO";
                }, pesoInicial);
            limpiar();
            actualizar();
            mensaje.setText("Animal guardado correctamente.");
        } catch (NumberFormatException e) {
            mensaje.setText("El peso debe ser un número válido.");
        } catch (IllegalArgumentException e) {
            mensaje.setText(e.getMessage());
        } catch (SQLException e) {
            mensaje.setText(e.getSQLState() != null && e.getSQLState().equals("23505")
                ? "Ya existe un animal con ese número de arete."
                : "No se pudo guardar. Revisa la conexión a PostgreSQL y la tabla animales.");
        }
    }

    @FXML public void actualizar() {
        try {
            tabla.setItems(FXCollections.observableArrayList(service.listar()));
            mensaje.setText("");
        } catch (SQLException e) {
            mensaje.setText(e.getMessage() != null && e.getMessage().contains("GRANJA_DB_PASSWORD")
                ? e.getMessage()
                : "No se pudo consultar PostgreSQL. Revisa el servidor, la base Granja y las credenciales.");
        }
    }
    @FXML private void corregir() {
        Animal elegido=tabla.getSelectionModel().getSelectedItem();
        if(elegido==null) { mensaje.setText("Selecciona un animal de la tabla.");return; }
        try {
            BigDecimal kg=peso.getText().isBlank()?null:new BigDecimal(peso.getText().trim().replace(',','.'));
            service.corregir(elegido.id(),arete.getText(),tipoAnimal.getValue(),
                "Hembra".equals(sexo.getValue())?"H":"M",raza.getText(),nacimiento.getValue(),
                ingreso.getValue(),switch(origen.getValue()) {
                    case "Comprado"->"COMPRADO";case "Otro"->"OTRO";default->"NACIDO";
                },kg);
            tabla.getSelectionModel().clearSelection();limpiar();actualizar();mensaje.setText("Animal corregido.");
        } catch(NumberFormatException e) { mensaje.setText("El peso debe ser un número válido."); }
        catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("23505".equals(e.getSQLState())?"Ese arete ya existe.":"No se pudo corregir el animal."); }
    }
    @FXML private void registrarBaja() {
        Animal elegido=tabla.getSelectionModel().getSelectedItem();
        try {
            service.baja(elegido,fechaBaja.getValue(),motivoBaja.getText());
            tabla.getSelectionModel().clearSelection();limpiar();motivoBaja.clear();
            actualizar();mensaje.setText("Baja registrada. El animal figura como fallecido.");
        } catch(IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch(SQLException e) { mensaje.setText("No se pudo registrar la baja. Ejecuta primero database/02_bajas_animales.sql en pgAdmin."); }
    }

    private void limpiar() {
        arete.clear(); tipoAnimal.getSelectionModel().selectFirst(); raza.clear(); peso.clear();
        nacimiento.setValue(null); ingreso.setValue(LocalDate.now());
    }
    @FXML private void nuevo() { tabla.getSelectionModel().clearSelection();limpiar();mensaje.setText(""); }
    private static String valor(String texto) { return texto == null ? "" : texto; }
}
