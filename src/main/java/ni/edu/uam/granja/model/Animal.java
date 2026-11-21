package ni.edu.uam.granja.model;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Animal(long id, String arete, String nombre, String sexo, String raza,
                     LocalDate fechaNacimiento, LocalDate fechaIngreso, String origen,
                     String estado, BigDecimal pesoInicialKg) {}
