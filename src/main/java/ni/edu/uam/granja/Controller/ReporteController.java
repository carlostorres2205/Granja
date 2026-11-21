package ni.edu.uam.granja.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.granja.Guardado.ReporteRepository;
import ni.edu.uam.granja.Guardado.ReporteRepository.Fila;
import ni.edu.uam.granja.Service.Permisos;
import javafx.stage.FileChooser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReporteController {
    @FXML private DatePicker desde,hasta;
    @FXML private Label resumen,conteos,mensaje;
    @FXML private TabPane pestañas;
    private final ReporteRepository repo=new ReporteRepository();
    private final TableView<Fila> finanzas=tabla();
    private final TableView<Fila> animales=tabla();
    private final TableView<Fila> traslados=tabla();
    private final TableView<Fila> consumo=tabla();
    private final TableView<Fila> salud=tabla();

    @FXML private void initialize() {
        desde.setValue(LocalDate.now().withDayOfMonth(1));
        hasta.setValue(LocalDate.now());
        pestañas.getTabs().add(pestaña("Ingresos y gastos",finanzas));
        pestañas.getTabs().add(pestaña("Entradas y ventas",animales));
        pestañas.getTabs().add(pestaña("Traslados",traslados));
        pestañas.getTabs().add(pestaña("Consumo de insumos",consumo));
        pestañas.getTabs().add(pestaña("Salud",salud));
        actualizar();
    }
    public void actualizar() {
        cargar();
    }
    private boolean cargar() {
        LocalDate inicio=desde.getValue(),fin=hasta.getValue();
        if(inicio==null || fin==null || inicio.isAfter(fin)) {
            mensaje.setText("Selecciona un rango de fechas válido.");return false;
        }
        try {
            ReporteRepository.Finanzas datos=repo.finanzas(inicio,fin);
            List<Fila> entradas=repo.entradasSalidas(inicio,fin);
            List<Fila> cambios=repo.traslados(inicio,fin);
            List<Fila> insumos=repo.consumo(inicio,fin);
            List<Fila> eventos=repo.salud(inicio,fin);
            finanzas.setItems(FXCollections.observableArrayList(datos.filas()));
            animales.setItems(FXCollections.observableArrayList(entradas));
            traslados.setItems(FXCollections.observableArrayList(cambios));
            consumo.setItems(FXCollections.observableArrayList(insumos));
            salud.setItems(FXCollections.observableArrayList(eventos));
            BigDecimal diferencia=datos.ingresos().subtract(datos.gastos());
            resumen.setText("Ingresos: "+datos.ingresos()+"   ·   Gastos: "+datos.gastos()
                +"   ·   Diferencia: "+diferencia);
            long ventas=entradas.stream().filter(f->"Venta".equals(f.tipo())).count();
            long bajas=entradas.stream().filter(f->"Fallecimiento".equals(f.tipo())).count();
            long ingresos=entradas.size()-ventas-bajas;
            conteos.setText("Entradas de animales: "+ingresos+"   ·   Vendidos: "+ventas
                +"   ·   Fallecidos: "+bajas+"   ·   Traslados: "+cambios.size()
                +"   ·   Registros de consumo: "+insumos.size());
            mensaje.setText("");
            return true;
        } catch(SQLException | IllegalStateException e) {
            mensaje.setText("No se pudieron cargar los reportes. Revisa la conexión a PostgreSQL.");
            return false;
        }
    }
    @FXML private void guardarCsv() {
        if (!Permisos.puedeReportes()) {
            mensaje.setText("Tu cuenta no tiene permiso para guardar reportes.");
            return;
        }
        if (!cargar()) return;
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte de la granja");
        selector.setInitialFileName("reporte_granja_" + desde.getValue() + "_" + hasta.getValue() + ".csv");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
        java.io.File destino = selector.showSaveDialog(pestañas.getScene().getWindow());
        if (destino == null) return;
        StringBuilder salida = new StringBuilder("\uFEFF");
        linea(salida, "Reporte de granja", desde.getValue().toString(), hasta.getValue().toString());
        linea(salida, resumen.getText());
        linea(salida, conteos.getText());
        agregar(salida, "Ingresos y gastos", finanzas);
        agregar(salida, "Entradas y ventas", animales);
        agregar(salida, "Traslados", traslados);
        agregar(salida, "Consumo de insumos", consumo);
        agregar(salida, "Salud", salud);
        try {
            Files.writeString(Path.of(destino.toURI()), salida.toString(), StandardCharsets.UTF_8);
            mensaje.setText("Reporte guardado en " + destino.getName());
        } catch (IOException e) {
            mensaje.setText("No se pudo guardar el reporte. Comprueba la carpeta de destino.");
        }
    }
    private static void agregar(StringBuilder salida, String titulo, TableView<Fila> tabla) {
        salida.append("\r\n");
        linea(salida, titulo);
        linea(salida, "Fecha", "Tipo", "Detalle", "Cantidad", "Importe");
        for (Fila fila : tabla.getItems())
            linea(salida, String.valueOf(fila.fecha()), fila.tipo(), fila.detalle(), fila.cantidad(), fila.importe());
    }
    private static void linea(StringBuilder salida, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) salida.append(',');
            String valor = valores[i] == null ? "" : valores[i];
            if (!valor.isEmpty() && "=+-@".indexOf(valor.charAt(0)) >= 0) valor = "'" + valor;
            salida.append('"').append(valor.replace("\"", "\"\"")).append('"');
        }
        salida.append("\r\n");
    }
    private static Tab pestaña(String nombre,TableView<Fila> tabla) {
        Tab t=new Tab(nombre,tabla);t.setClosable(false);return t;
    }
    private static TableView<Fila> tabla() {
        TableView<Fila> tabla=new TableView<>();
        tabla.setPrefHeight(480);
        tabla.setPlaceholder(new Label("No hay registros en el período seleccionado."));
        tabla.getColumns().add(columna("Fecha",105,Fila::fecha));
        tabla.getColumns().add(columna("Tipo",130,Fila::tipo));
        tabla.getColumns().add(columna("Detalle",370,Fila::detalle));
        tabla.getColumns().add(columna("Cantidad",135,Fila::cantidad));
        tabla.getColumns().add(columna("Importe",115,Fila::importe));
        return tabla;
    }
    private static TableColumn<Fila,String> columna(String titulo,int ancho,java.util.function.Function<Fila,Object> valor) {
        TableColumn<Fila,String> columna=new TableColumn<>(titulo);columna.setPrefWidth(ancho);
        columna.setCellValueFactory(v->new SimpleStringProperty(
            valor.apply(v.getValue())==null?"":valor.apply(v.getValue()).toString()));
        return columna;
    }
}
