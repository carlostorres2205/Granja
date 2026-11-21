package ni.edu.uam.granja.Service;

import ni.edu.uam.granja.Guardado.ReproduccionRepository.Cria;
import ni.edu.uam.granja.Guardado.ReproduccionRepository.Servicio;
import ni.edu.uam.granja.model.Animal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReproduccionService {
    public void validar(Animal madre, LocalDate fecha, Servicio servicio) {
        if (madre == null || !"H".equals(madre.sexo()) || !"ACTIVO".equals(madre.estado()))
            throw new IllegalArgumentException("Selecciona una hembra activa.");
        if (fecha == null || fecha.isAfter(LocalDate.now()) || fecha.isBefore(madre.fechaIngreso()))
            throw new IllegalArgumentException("La fecha debe estar entre el ingreso de la hembra y hoy.");
        if (servicio != null && (servicio.hembraId() != madre.id() || servicio.fecha().isAfter(fecha)))
            throw new IllegalArgumentException("El servicio debe pertenecer a la hembra y ser anterior al evento.");
    }
    public void validarCrias(List<Cria> crias) {
        if (crias.isEmpty()) throw new IllegalArgumentException("Agrega al menos una cría.");
        Set<String> aretes = new HashSet<>();
        for (Cria c : crias) {
            if (c.arete() == null || c.arete().isBlank() || c.arete().length() > 50)
                throw new IllegalArgumentException("Cada cría necesita un arete de hasta 50 caracteres.");
            if (!aretes.add(c.arete().trim())) throw new IllegalArgumentException("Hay aretes repetidos en la lista de crías.");
            if (!List.of("H", "M").contains(c.sexo())) throw new IllegalArgumentException("Selecciona el sexo de cada cría.");
            BigDecimal peso = c.peso();
            if (peso != null && (peso.signum() <= 0 || peso.compareTo(new BigDecimal("999999.99")) > 0 || peso.stripTrailingZeros().scale() > 2))
                throw new IllegalArgumentException("El peso debe ser mayor que cero y tener hasta dos decimales.");
        }
    }
}
