package ni.edu.uam.granja.Service;

import ni.edu.uam.granja.Guardado.PesajeRepository;
import ni.edu.uam.granja.model.Animal;
import ni.edu.uam.granja.model.Pesaje;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class PesajeService {
    private final PesajeRepository repository = new PesajeRepository();

    public List<Pesaje> listar(long animalId) throws SQLException {
        return repository.listarPorAnimal(animalId);
    }

    public void registrar(Animal animal, LocalDate fecha, BigDecimal peso, String observaciones) throws SQLException {
        validar(animal,fecha,peso);
        repository.guardar(new Pesaje(0, animal.id(), fecha, peso,
            observaciones == null || observaciones.isBlank() ? null : observaciones.trim()));
    }
    public void corregir(Pesaje existente,Animal animal,LocalDate fecha,BigDecimal peso,String observaciones) throws SQLException {
        if(existente==null || animal==null || existente.animalId()!=animal.id())
            throw new IllegalArgumentException("Selecciona un pesaje del animal actual.");
        validar(animal,fecha,peso);
        repository.corregir(new Pesaje(existente.id(),animal.id(),fecha,peso,
            observaciones==null||observaciones.isBlank()?null:observaciones.trim()));
    }
    private void validar(Animal animal, LocalDate fecha, BigDecimal peso) {
        if (animal == null) throw new IllegalArgumentException("Selecciona un animal.");
        if (!"ACTIVO".equals(animal.estado())) throw new IllegalArgumentException("Solo se pueden registrar pesajes para animales activos.");
        if (fecha == null) throw new IllegalArgumentException("Selecciona la fecha del pesaje.");
        if (fecha.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha no puede ser futura.");
        if (fecha.isBefore(animal.fechaIngreso())) throw new IllegalArgumentException("La fecha no puede ser anterior al ingreso del animal.");
        if (peso == null || peso.signum() <= 0 || peso.compareTo(new BigDecimal("999999.99")) > 0 || peso.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Indica un peso mayor que cero, hasta 999999.99 kg y con máximo dos decimales.");
    }

    public static BigDecimal gananciaDiaria(Pesaje anterior, Pesaje reciente) {
        long dias = ChronoUnit.DAYS.between(anterior.fecha(), reciente.fecha());
        if (dias <= 0 || anterior.animalId() != reciente.animalId())
            throw new IllegalArgumentException("Los pesajes deben pertenecer al mismo animal y tener fechas distintas.");
        return reciente.pesoKg().subtract(anterior.pesoKg())
            .divide(BigDecimal.valueOf(dias), 2, RoundingMode.HALF_UP);
    }
}
