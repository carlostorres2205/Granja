package ni.edu.uam.granja.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import ni.edu.uam.granja.GranjaApplication;
import ni.edu.uam.granja.Service.Permisos;
import java.io.IOException;

public class PrincipalController {
    @FXML private Label titulo;
    @FXML private Label descripcion;
    @FXML private VBox inicio;
    @FXML private VBox modulo;
    @FXML private Label nombreModulo;
    @FXML private Label detalleModulo;
    @FXML private VBox animalContainer;
    @FXML private VBox pesajeContainer;
    @FXML private VBox reproduccionContainer;
    @FXML private VBox saludContainer;
    @FXML private VBox organizacionContainer;
    @FXML private VBox inventarioContainer;
    @FXML private VBox finanzasContainer;
    @FXML private VBox reporteContainer;
    @FXML private VBox usuariosContainer;
    @FXML private Button usuariosBoton;
    @FXML private Button reportesBoton;
    private boolean animalesCargados;
    private PesajeController pesajeController;
    private ReproduccionController reproduccionController;
    private SaludController saludController;
    private OrganizacionController organizacionController;
    private InventarioController inventarioController;
    private FinanzasController finanzasController;
    private ReporteController reporteController;
    private UsuariosController usuariosController;
    private AnimalController animalController;
    @FXML private void initialize() {
        boolean admin=Permisos.esAdministrador();
        usuariosBoton.setVisible(admin);usuariosBoton.setManaged(admin);
        reportesBoton.setVisible(Permisos.puedeReportes());
        reportesBoton.setManaged(Permisos.puedeReportes());
    }
    @FXML private void navegar(javafx.event.ActionEvent evento) {
        String nombre = ((Button) evento.getSource()).getUserData().toString();
        if(nombre.equals("Usuarios") && !Permisos.esAdministrador()) return;
        if(nombre.equals("Reportes") && !Permisos.puedeReportes()) return;
        boolean esInicio = nombre.equals("Inicio");
        titulo.setText(esInicio ? "Bienvenido a tu granja" : nombre);
        descripcion.setText(esInicio ? "Todo lo importante, en un solo lugar." : "Consulta y administra la información de " + nombre.toLowerCase() + ".");
        inicio.setVisible(esInicio); inicio.setManaged(esInicio);
        boolean esAnimales = nombre.equals("Animales y lotes");
        boolean esPesajes = nombre.equals("Pesajes y engorde");
        boolean esReproduccion = nombre.equals("Reproducción");
        boolean esSalud = nombre.equals("Salud");
        boolean esOrganizacion = nombre.equals("Potreros y corrales");
        boolean esInventario = nombre.equals("Inventario y alimentación");
        boolean esFinanzas = nombre.equals("Finanzas y ventas");
        boolean esReportes = nombre.equals("Reportes");
        boolean esUsuarios = nombre.equals("Usuarios");
        animalContainer.setVisible(esAnimales); animalContainer.setManaged(esAnimales);
        pesajeContainer.setVisible(esPesajes); pesajeContainer.setManaged(esPesajes);
        reproduccionContainer.setVisible(esReproduccion); reproduccionContainer.setManaged(esReproduccion);
        saludContainer.setVisible(esSalud); saludContainer.setManaged(esSalud);
        organizacionContainer.setVisible(esOrganizacion); organizacionContainer.setManaged(esOrganizacion);
        inventarioContainer.setVisible(esInventario); inventarioContainer.setManaged(esInventario);
        finanzasContainer.setVisible(esFinanzas); finanzasContainer.setManaged(esFinanzas);
        reporteContainer.setVisible(esReportes); reporteContainer.setManaged(esReportes);
        usuariosContainer.setVisible(esUsuarios); usuariosContainer.setManaged(esUsuarios);
        modulo.setVisible(!esInicio && !esAnimales && !esPesajes && !esReproduccion && !esSalud && !esOrganizacion && !esInventario && !esFinanzas && !esReportes && !esUsuarios);
        modulo.setManaged(!esInicio && !esAnimales && !esPesajes && !esReproduccion && !esSalud && !esOrganizacion && !esInventario && !esFinanzas && !esReportes && !esUsuarios);
        if(esUsuarios) {
            if(usuariosController==null) {
                try {
                    FXMLLoader loader=new FXMLLoader(GranjaApplication.class.getResource("Usuarios-view.fxml"));
                    Parent vista=loader.load();usuariosController=loader.getController();
                    usuariosContainer.getChildren().setAll(vista);
                } catch(IOException ex) {
                    ex.printStackTrace();usuariosContainer.getChildren().setAll(new Label("No se pudo abrir usuarios."));
                }
            } else usuariosController.actualizar();
        }
        if (esReportes) {
            if (reporteController == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Reporte-view.fxml"));
                    Parent vista = loader.load();
                    reporteController = loader.getController();
                    reporteContainer.getChildren().setAll(vista);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    reporteContainer.getChildren().setAll(new Label("No se pudieron abrir los reportes."));
                }
            } else reporteController.actualizar();
        }
        if (esFinanzas) {
            if (finanzasController == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Finanzas-view.fxml"));
                    Parent vista = loader.load();
                    finanzasController = loader.getController();
                    finanzasContainer.getChildren().setAll(vista);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    finanzasContainer.getChildren().setAll(new Label("No se pudo abrir finanzas y ventas."));
                }
            } else finanzasController.actualizar();
        }
        if (esInventario) {
            if (inventarioController == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Inventario-view.fxml"));
                    Parent vista = loader.load();
                    inventarioController = loader.getController();
                    inventarioContainer.getChildren().setAll(vista);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    inventarioContainer.getChildren().setAll(new Label("No se pudo abrir inventario y alimentación."));
                }
            } else inventarioController.actualizar();
        }
        if (esOrganizacion) {
            if (organizacionController == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Organizacion-view.fxml"));
                    Parent vista = loader.load();
                    organizacionController = loader.getController();
                    organizacionContainer.getChildren().setAll(vista);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    organizacionContainer.getChildren().setAll(new Label("No se pudo abrir potreros y corrales."));
                }
            } else organizacionController.actualizar();
        }
        if (esSalud) {
            if (saludController == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Salud-view.fxml"));
                    Parent vista = loader.load();
                    saludController = loader.getController();
                    saludContainer.getChildren().setAll(vista);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    saludContainer.getChildren().setAll(new Label("No se pudo abrir el módulo de salud."));
                }
            } else saludController.actualizarAnimales();
        }
        if (esReproduccion) {
            if (reproduccionController == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Reproduccion-view.fxml"));
                    Parent vista = loader.load();
                    reproduccionController = loader.getController();
                    reproduccionContainer.getChildren().setAll(vista);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    reproduccionContainer.getChildren().setAll(new Label("No se pudo abrir el módulo de reproducción."));
                }
            } else reproduccionController.actualizarAnimales();
        }
        if (esPesajes) {
            if (pesajeController == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Pesaje-view.fxml"));
                    Parent vista = loader.load();
                    pesajeController = loader.getController();
                    pesajeContainer.getChildren().setAll(vista);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    pesajeContainer.getChildren().setAll(new Label("No se pudo abrir el módulo de pesajes."));
                }
            } else {
                pesajeController.actualizarAnimales();
            }
        }
        if (esAnimales && !animalesCargados) {
            try {
                FXMLLoader loader = new FXMLLoader(GranjaApplication.class.getResource("Animal-view.fxml"));
                Parent vista = loader.load();
                animalController = loader.getController();
                animalContainer.getChildren().setAll(vista);
                animalesCargados = true;
            } catch (IOException ex) {
                ex.printStackTrace();
                animalContainer.getChildren().setAll(new Label("No se pudo abrir el módulo de animales."));
            }
        } else if (esAnimales) animalController.actualizar();
        nombreModulo.setText(nombre);
        detalleModulo.setText("Este espacio está preparado para " + nombre.toLowerCase() + ". Sus registros y operaciones estarán disponibles en una próxima etapa.");
    }
    @FXML private void cerrarSesion() throws IOException {
        GranjaApplication.cerrarSesion();
        GranjaApplication.mostrar((Stage) titulo.getScene().getWindow(), "Login-view.fxml", "Granja | Bienvenido", 1000, 680);
    }
}
