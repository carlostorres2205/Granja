package ni.edu.uam.granja.Service;
import ni.edu.uam.granja.Guardado.EventoSanitarioRepository;
import ni.edu.uam.granja.model.Animal;
import ni.edu.uam.granja.model.EventosSanitarios;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SaludService {
    private final EventoSanitarioRepository repository = new EventoSanitarioRepository();
    public List<EventosSanitarios> listar(long animalId) throws SQLException { return repository.listar(animalId); }
    public List<EventosSanitarios> proximos() throws SQLException { return repository.proximos(); }
    public void registrar(Animal animal,String tipo,LocalDate fecha,String descripcion,String dosis,
                         LocalDate proxima,LocalDate retiro,String notas,Long insumoId,BigDecimal cantidad) throws SQLException {
        if(animal==null) throw new IllegalArgumentException("Selecciona un animal.");
        if(!"ACTIVO".equals(animal.estado())) throw new IllegalArgumentException("Selecciona un animal activo.");
        if(fecha==null || fecha.isBefore(animal.fechaIngreso()) || fecha.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La fecha debe estar entre el ingreso del animal y hoy.");
        if(descripcion==null || descripcion.isBlank()) throw new IllegalArgumentException("Escribe el motivo o la descripción.");
        if(descripcion.length()>10000) throw new IllegalArgumentException("La descripción es demasiado larga.");
        if(proxima!=null && proxima.isBefore(fecha)) throw new IllegalArgumentException("La próxima revisión no puede ser anterior al evento.");
        if(retiro!=null && retiro.isBefore(fecha)) throw new IllegalArgumentException("El fin del retiro no puede ser anterior al evento.");
        if(insumoId==null && cantidad!=null || insumoId!=null && (cantidad==null||cantidad.signum()<=0
            ||cantidad.stripTrailingZeros().scale()>3))
            throw new IllegalArgumentException("Indica la cantidad del medicamento con máximo tres decimales.");
        repository.guardar(new EventosSanitarios(0,animal.id(),tipo,fecha,descripcion.trim(),null,
            limpio(dosis),proxima,retiro,limpio(notas)),insumoId,cantidad);
    }
    private String limpio(String valor) { return valor==null || valor.isBlank()?null:valor.trim(); }
}
