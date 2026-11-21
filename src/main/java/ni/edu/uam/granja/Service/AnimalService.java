package ni.edu.uam.granja.Service;

import ni.edu.uam.granja.Guardado.AnimalRepository;
import ni.edu.uam.granja.model.Animal;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AnimalService {
    private final AnimalRepository repository = new AnimalRepository();
    public List<Animal> listar() throws SQLException { return repository.listar(); }
    public Animal registrar(String arete, String nombre, String sexo, String raza,
                            LocalDate nacimiento, LocalDate ingreso, String origen,
                            BigDecimal pesoInicial) throws SQLException {
        validar(arete,ingreso,nacimiento,pesoInicial);
        return repository.guardar(new Animal(0, arete.trim(), normalizar(nombre), sexo, normalizar(raza),
            nacimiento, ingreso, origen, "ACTIVO", pesoInicial));
    }
    public void corregir(long id,String arete,String tipo,String sexo,String raza,LocalDate nacimiento,
                         LocalDate ingreso,String origen,BigDecimal peso) throws SQLException {
        validar(arete,ingreso,nacimiento,peso);
        repository.actualizar(new Animal(id,arete.trim(),tipo,sexo,normalizar(raza),
            nacimiento,ingreso,origen,"ACTIVO",peso));
    }
    public void baja(Animal animal,LocalDate fecha,String motivo) throws SQLException {
        if(animal==null || !"ACTIVO".equals(animal.estado())) throw new IllegalArgumentException("Selecciona un animal activo.");
        if(fecha==null || fecha.isAfter(LocalDate.now())) throw new IllegalArgumentException("Selecciona una fecha de baja válida.");
        if(motivo==null || motivo.isBlank()) throw new IllegalArgumentException("Escribe el motivo de la baja.");
        repository.registrarBaja(animal.id(),fecha,motivo.trim());
    }
    private static void validar(String arete,LocalDate ingreso,LocalDate nacimiento,BigDecimal peso) {
        if(arete==null || arete.isBlank() || arete.trim().length()>50) throw new IllegalArgumentException("Escribe un arete de hasta 50 caracteres.");
        if(ingreso==null || ingreso.isAfter(LocalDate.now())) throw new IllegalArgumentException("Selecciona una fecha de ingreso válida.");
        if(nacimiento!=null && nacimiento.isAfter(ingreso)) throw new IllegalArgumentException("El nacimiento no puede ser posterior al ingreso.");
        if(peso!=null && (peso.signum()<=0 || peso.compareTo(new BigDecimal("999999.99"))>0 || peso.stripTrailingZeros().scale()>2))
            throw new IllegalArgumentException("El peso inicial debe ser positivo y tener hasta dos decimales.");
    }
    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
