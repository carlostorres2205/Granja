package ni.edu.uam.granja.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import ni.edu.uam.granja.Guardado.AnimalRepository;
import ni.edu.uam.granja.Guardado.ReproduccionRepository;
import ni.edu.uam.granja.Guardado.ReproduccionRepository.Cria;
import ni.edu.uam.granja.Guardado.ReproduccionRepository.Evento;
import ni.edu.uam.granja.Guardado.ReproduccionRepository.Servicio;
import ni.edu.uam.granja.Service.ReproduccionService;
import ni.edu.uam.granja.model.Animal;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;

public class ReproduccionController {
    @FXML private VBox contenido;
    private final AnimalRepository animales = new AnimalRepository();
    private final ReproduccionRepository repo = new ReproduccionRepository();
    private final ReproduccionService service = new ReproduccionService();
    private final ComboBox<Animal> hembra = new ComboBox<>();
    private final ComboBox<Animal> macho = new ComboBox<>();
    private final ComboBox<Servicio> servicioDiagnostico = new ComboBox<>();
    private final ComboBox<Servicio> servicioParto = new ComboBox<>();
    private final DatePicker fechaServicio = new DatePicker(LocalDate.now());
    private final DatePicker fechaDiagnostico = new DatePicker(LocalDate.now());
    private final DatePicker fechaParto = new DatePicker(LocalDate.now());
    private final DatePicker fechaEstimada = new DatePicker();
    private final ComboBox<String> tipo = opciones("Monta", "Inseminación");
    private final ComboBox<String> resultado = opciones("Positivo", "Negativo", "Indeterminado");
    private final ComboBox<String> dificultad = opciones("Normal", "Asistido", "Complicado");
    private final TextField semen = new TextField();
    private final TextField notaServicio = new TextField();
    private final TextField notaDiagnostico = new TextField();
    private final TextField notaParto = new TextField();
    private final TextField arete = new TextField();
    private final TextField peso = new TextField();
    private final ComboBox<String> sexo = opciones("Hembra", "Macho");
    private final ComboBox<String> estado = opciones("Viva", "Fallecida");
    private final ObservableList<Cria> crias = FXCollections.observableArrayList();
    private Long madreDeCrias;
    private final ListView<Cria> listaCrias = new ListView<>(crias);
    private final Label mensaje = new Label();
    private final TableView<Evento> historial = new TableView<>();
    private List<Animal> todos = List.of();

    @FXML private void initialize() {
        StringConverter<Animal> conversor = new StringConverter<>() {
            @Override public String toString(Animal a) {
                return a == null ? "" : a.arete() + " · " + (a.nombre() == null ? "Tipo sin registrar" : a.nombre());
            }
            @Override public Animal fromString(String s) { return null; }
        };
        hembra.setConverter(conversor); macho.setConverter(conversor);
        hembra.setPromptText("Selecciona una hembra activa");
        macho.setPromptText("Opcional");
        hembra.setMaxWidth(Double.MAX_VALUE); macho.setMaxWidth(Double.MAX_VALUE);
        servicioDiagnostico.setPromptText("Sin servicio asociado"); servicioParto.setPromptText("Sin servicio asociado");
        arete.setPromptText("Arete de la cría");
        peso.setPromptText("Peso al nacer en kg (opcional)");
        semen.setPromptText("Identificación del semen (opcional)");
        servicioDiagnostico.setMaxWidth(Double.MAX_VALUE); servicioParto.setMaxWidth(Double.MAX_VALUE);
        hembra.valueProperty().addListener((o, a, b) -> actualizarSeleccion());
        tipo.valueProperty().addListener((o, a, b) -> actualizarTipo());
        listaCrias.setPrefHeight(130);
        listaCrias.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(Cria c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.arete() + " · " + (c.sexo().equals("H") ? "Hembra" : "Macho")
                    + " · " + (c.viva() ? "Viva" : "Fallecida") + (c.peso() == null ? "" : " · " + c.peso() + " kg"));
            }
        });
        TableColumn<Evento,String> f = new TableColumn<>("Fecha");
        f.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().fecha().toString())); f.setPrefWidth(115);
        TableColumn<Evento,String> c = new TableColumn<>("Evento");
        c.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().categoria())); c.setPrefWidth(120);
        TableColumn<Evento,String> d = new TableColumn<>("Detalle");
        d.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().detalle())); d.setPrefWidth(420);
        historial.getColumns().add(f); historial.getColumns().add(c); historial.getColumns().add(d);
        historial.setPlaceholder(new Label("No hay eventos para esta hembra.")); historial.setPrefHeight(220);
        mensaje.setWrapText(true); mensaje.getStyleClass().add("muted");
        VBox seleccion = tarjeta(new Label("Hembra"), hembra, mensaje);
        TabPane tabs = new TabPane();
        tabs.getTabs().add(tab("Servicio", tarjeta(
            new Label("Fecha del servicio *"), fechaServicio, new Label("Tipo *"), tipo,
            new Label("Macho (opcional, para monta)"), macho,
            new Label("Identificación del semen (opcional, para inseminación)"), semen,
            new Label("Observaciones"), notaServicio, boton("Guardar servicio", this::guardarServicio))));
        tabs.getTabs().add(tab("Diagnóstico", tarjeta(
            new Label("Fecha del diagnóstico *"), fechaDiagnostico, new Label("Servicio asociado (opcional)"), servicioDiagnostico,
            botonSecundario("Quitar servicio", () -> servicioDiagnostico.getSelectionModel().clearSelection()),
            new Label("Resultado *"), resultado, new Label("Fecha estimada de parto (opcional)"), fechaEstimada,
            new Label("Observaciones"), notaDiagnostico, boton("Guardar diagnóstico", this::guardarDiagnostico))));
        tabs.getTabs().add(tab("Parto y crías", tarjeta(
            new Label("Fecha del parto *"), fechaParto, new Label("Servicio asociado (opcional)"), servicioParto,
            botonSecundario("Quitar servicio", () -> servicioParto.getSelectionModel().clearSelection()),
            new Label("Dificultad"), dificultad, new Label("Observaciones"), notaParto,
            new Label("Agregar crías: arete, sexo, estado y peso opcional en kg"),
            arete, sexo, estado, peso, botonSecundario("Agregar cría a la lista", this::agregarCria),
            listaCrias, botonSecundario("Quitar cría seleccionada", () -> {
                crias.remove(listaCrias.getSelectionModel().getSelectedItem());
                if (crias.isEmpty()) madreDeCrias = null;
            }),
            boton("Guardar parto y crías", this::guardarParto))));
        VBox eventos = tarjeta(new Label("Historial reproductivo"), historial,
            botonSecundario("Actualizar historial", this::actualizarSeleccion));
        contenido.getChildren().setAll(seleccion, tabs, eventos);
        actualizarTipo();
        actualizarAnimales();
    }

    public void actualizarAnimales() {
        try {
            Animal anterior = hembra.getValue();
            todos = animales.listar();
            List<Animal> hembras = todos.stream().filter(a -> "H".equals(a.sexo()) && "ACTIVO".equals(a.estado())).toList();
            hembra.setItems(FXCollections.observableArrayList(hembras));
            if (anterior != null) hembras.stream().filter(a -> a.id() == anterior.id()).findFirst().ifPresent(hembra::setValue);
            if (hembras.isEmpty()) mensaje.setText("Registra una hembra activa para comenzar.");
            else if (hembra.getValue() != null) actualizarSeleccion();
        } catch (SQLException e) { mensaje.setText("No se pudieron cargar los animales. Revisa la conexión a PostgreSQL."); }
    }
    private void actualizarSeleccion() {
        Animal madre = hembra.getValue();
        macho.getItems().setAll(todos.stream().filter(a -> "M".equals(a.sexo()) && "ACTIVO".equals(a.estado())
            && madre != null && madre.nombre() != null && madre.nombre().equalsIgnoreCase(a.nombre())).toList());
        macho.getSelectionModel().clearSelection();
        servicioDiagnostico.getItems().clear(); servicioParto.getItems().clear(); historial.getItems().clear();
        if (madre == null) return;
        try {
            List<Servicio> servicios = repo.servicios(madre.id());
            servicioDiagnostico.getItems().setAll(servicios); servicioParto.getItems().setAll(servicios);
            historial.getItems().setAll(repo.historial(madre.id()));
            mensaje.setText(madre.nombre() == null ? "El tipo de esta hembra no está definido; registra el macho solo si conoces su especie." : "");
        } catch (SQLException e) { mensaje.setText("No se pudo cargar el historial reproductivo."); }
    }
    private void actualizarTipo() {
        boolean monta = "Monta".equals(tipo.getValue());
        macho.setDisable(!monta); semen.setDisable(monta);
        if (monta) semen.clear(); else macho.getSelectionModel().clearSelection();
    }
    private void guardarServicio() {
        try {
            service.validar(hembra.getValue(), fechaServicio.getValue(), null);
            repo.guardarServicio(hembra.getValue().id(), fechaServicio.getValue(),
                "Monta".equals(tipo.getValue()) ? "MONTA" : "INSEMINACION",
                macho.getValue() == null ? null : macho.getValue().id(), vacio(semen), vacio(notaServicio));
            notaServicio.clear(); semen.clear(); actualizarSeleccion(); mensaje.setText("Servicio guardado.");
        } catch (IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch (SQLException e) { error(e); }
    }
    private void guardarDiagnostico() {
        try {
            service.validar(hembra.getValue(), fechaDiagnostico.getValue(), servicioDiagnostico.getValue());
            if (fechaEstimada.getValue() != null && (!"Positivo".equals(resultado.getValue()) || fechaEstimada.getValue().isBefore(fechaDiagnostico.getValue())))
                throw new IllegalArgumentException("La fecha estimada corresponde a un diagnóstico positivo y no puede ser anterior al diagnóstico.");
            repo.guardarDiagnostico(hembra.getValue().id(), id(servicioDiagnostico.getValue()), fechaDiagnostico.getValue(),
                switch (resultado.getValue()) { case "Positivo" -> "POSITIVO"; case "Negativo" -> "NEGATIVO"; default -> "INDETERMINADO"; },
                fechaEstimada.getValue(), vacio(notaDiagnostico));
            fechaEstimada.setValue(null); notaDiagnostico.clear(); actualizarSeleccion(); mensaje.setText("Diagnóstico guardado.");
        } catch (IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch (SQLException e) { error(e); }
    }
    private void agregarCria() {
        try {
            if (hembra.getValue() == null) throw new IllegalArgumentException("Selecciona primero la madre.");
            if (madreDeCrias != null && madreDeCrias.longValue() != hembra.getValue().id())
                throw new IllegalArgumentException("Las crías de la lista pertenecen a otra madre. Quítalas antes de continuar.");
            BigDecimal kg = peso.getText().isBlank() ? null : new BigDecimal(peso.getText().trim().replace(',', '.'));
            Cria cria = new Cria(arete.getText().trim(), "Hembra".equals(sexo.getValue()) ? "H" : "M", kg, "Viva".equals(estado.getValue()));
            List<Cria> prueba = new java.util.ArrayList<>(crias); prueba.add(cria); service.validarCrias(prueba);
            crias.add(cria); madreDeCrias = hembra.getValue().id(); arete.clear(); peso.clear(); mensaje.setText("Cría agregada. Guarda el parto para registrar la lista.");
        } catch (NumberFormatException e) { mensaje.setText("Escribe un peso válido."); }
        catch (IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
    }
    private void guardarParto() {
        try {
            service.validar(hembra.getValue(), fechaParto.getValue(), servicioParto.getValue());
            service.validarCrias(crias);
            if (madreDeCrias == null || madreDeCrias.longValue() != hembra.getValue().id())
                throw new IllegalArgumentException("Las crías de la lista pertenecen a otra madre. Selecciona la madre original o quita las crías.");
            repo.guardarParto(hembra.getValue().id(), id(servicioParto.getValue()), fechaParto.getValue(),
                switch (dificultad.getValue()) { case "Asistido" -> "ASISTIDO"; case "Complicado" -> "COMPLICADO"; default -> "NORMAL"; },
                vacio(notaParto), List.copyOf(crias));
            crias.clear(); madreDeCrias = null; notaParto.clear();
            try { actualizarSeleccion(); mensaje.setText("Parto y crías guardados."); }
            catch (RuntimeException e) { mensaje.setText("Parto guardado; actualiza el historial para consultarlo."); }
        } catch (IllegalArgumentException e) { mensaje.setText(e.getMessage()); }
        catch (SQLException e) { error(e); }
    }
    private void error(SQLException e) {
        mensaje.setText("23505".equals(e.getSQLState()) ? "Ya existe una cría con ese arete. Revisa la lista; no se guardó el parto."
            : "No se pudo guardar. Comprueba la conexión a PostgreSQL y los datos.");
    }
    private static Long id(Servicio s) { return s == null ? null : s.id(); }
    private static String vacio(TextField t) { return t.getText().isBlank() ? null : t.getText().trim(); }
    private static ComboBox<String> opciones(String... valores) {
        ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList(valores)); c.getSelectionModel().selectFirst(); c.setMaxWidth(Double.MAX_VALUE); return c;
    }
    private static VBox tarjeta(javafx.scene.Node... nodos) {
        VBox caja = new VBox(10, nodos); caja.getStyleClass().add("info-card"); caja.setPadding(new Insets(20)); return caja;
    }
    private static Tab tab(String titulo, VBox contenido) {
        Tab t = new Tab(titulo, contenido); t.setClosable(false); return t;
    }
    private static Button boton(String texto, Runnable accion) {
        Button b = new Button(texto); b.getStyleClass().add("primary-button"); b.setOnAction(e -> accion.run()); return b;
    }
    private static Button botonSecundario(String texto, Runnable accion) {
        Button b = new Button(texto); b.getStyleClass().add("secondary-button"); b.setOnAction(e -> accion.run()); return b;
    }
}
