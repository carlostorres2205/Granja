package ni.edu.uam.granja.model;
import java.time.LocalDate;

public record EventosSanitarios(long id, long animalId, String tipo, LocalDate fecha,
    String descripcion, String producto, String dosis, LocalDate fechaProxima,
    LocalDate finRetiro, String observaciones) {}
